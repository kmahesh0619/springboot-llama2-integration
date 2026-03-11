package com.example.localchat.application.service.incident;

import com.example.localchat.application.repository.IncidentHistoryRepository;
import com.example.localchat.application.repository.TicketCommentRepository;
import com.example.localchat.application.repository.TicketRepository;
import com.example.localchat.application.repository.UserRepository;
import com.example.localchat.application.service.mapper.TicketEventMapper;
import com.example.localchat.application.sla.SlaEngineService;
import com.example.localchat.config.security.SecurityUtils;
import com.example.localchat.webSocket.IncidentEventPublisher;
import com.example.localchat.adapters.rest.dto.response.HistoryEventResponse;
import com.example.localchat.adapters.rest.dto.response.IncidentSummaryResponse;
import com.example.localchat.domain.entity.IncidentHistory;
import com.example.localchat.domain.entity.Ticket;
import com.example.localchat.domain.entity.TicketComment;
import com.example.localchat.domain.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

import com.example.localchat.application.usecase.IncidentQueryUseCase;
import com.example.localchat.application.usecase.AssignIncidentUseCase;
import com.example.localchat.application.usecase.CloseTicketUseCase;
import com.example.localchat.adapters.rest.dto.request.AssignRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentManagementService implements IncidentQueryUseCase, AssignIncidentUseCase, CloseTicketUseCase {

    private final TicketRepository ticketRepository;
    private final IncidentHistoryRepository incidentHistoryRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final SlaEngineService slaEngineService;
    private final IncidentEventPublisher eventPublisher;

    // ─────────────────────────────────────────────────────────────────────
    // PHASE 2 — VIEW TICKETS
    // ─────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<IncidentSummaryResponse> getMyTickets(Pageable pageable) {
        User currentUser = securityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("Authentication required"));

        log.info("Fetching tickets for user: {} role: {}", currentUser.getWorkerId(), currentUser.getRole());

        Page<Ticket> page;
        if ("WORKER".equals(currentUser.getRole())) {
            page = ticketRepository.findByCreatedByUserId(currentUser.getUserId(), pageable);
        } else {
            // Supervisors/Managers see assigned tickets
            page = ticketRepository.findByAssignedToUserId(currentUser.getUserId(), pageable);
        }

        return page.map(this::toSummaryDto);
    }

    @Transactional(readOnly = true)
    public Page<IncidentSummaryResponse> listIncidents(String status, String severity, String departmentCode,
                                                   Pageable pageable) {

        User currentUser = securityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("Authentication required"));

        log.info("User {} listing incidents - role={}", currentUser.getWorkerId(), currentUser.getRole());

        // Role-Based Filtering
        if ("SUPERVISOR".equals(currentUser.getRole())) {
            if (currentUser.getDepartment() == null) {
                throw new RuntimeException("Supervisor has no department assigned");
            }
            departmentCode = currentUser.getDepartment().getDepartmentCode();
            log.info("Restricting supervisor {} to department {}", currentUser.getWorkerId(), departmentCode);
        }

        Page<Ticket> page = ticketRepository.findByFilters(status, severity, departmentCode, pageable);
        return page.map(this::toSummaryDto);
    }

    @Transactional(readOnly = true)
    public IncidentSummaryResponse getTicketSummary(String ticketNumber) {
        User currentUser = securityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("Authentication required"));
        
        Ticket ticket = findTicketByNumberOrThrow(ticketNumber);
        validateAccess(currentUser, ticket);
        
        return toSummaryDto(ticket);
    }

    // ─────────────────────────────────────────────────────────────────────
    // PHASE 2 — UPDATE STATUS
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    @Override
    public IncidentSummaryResponse resolveTicket(String ticketNumber) {
        return updateStatus(ticketNumber, "RESOLVED");
    }

    @Transactional
    @Override
    public IncidentSummaryResponse closeTicket(String ticketNumber) {
        return updateStatus(ticketNumber, "CLOSED");
    }

    @Transactional
    public IncidentSummaryResponse updateStatus(String ticketNumber, String newStatus) {
        User currentUser = securityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("Authentication required"));
        
        Ticket ticket = findTicketByNumberOrThrow(ticketNumber);
        String oldStatus = ticket.getStatus();

        // 1. Ownership/Department Validation
        validateAccess(currentUser, ticket);

        // 2. Status Transition Guard
        validateStatusTransition(currentUser, newStatus);

        // 3. Reporter/Fixer Paradox Fix
        if ("RESOLVED".equals(newStatus)) {
            if (ticket.getCreatedBy().getUserId().equals(currentUser.getUserId())) {
                throw new RuntimeException("Access denied: You cannot resolve an incident you reported. Independent verification required.");
            }
        }

        setupMdc(ticket);

        try {
            log.info("Status transition: ticket={} user={} {} → {}", ticket.getTicketNumber(), currentUser.getWorkerId(), oldStatus, newStatus);

            ticket.setStatus(newStatus);
            ticket.setUpdatedAt(Instant.now());

            if ("RESOLVED".equals(newStatus) || "CLOSED".equals(newStatus)) {
                ticket.setResolvedAt(Instant.now());
            }

            ticketRepository.save(ticket);
            recordHistory(ticket, "STATUS_CHANGE", oldStatus, newStatus, currentUser);

            broadcastStatusChange(ticket);
            return toSummaryDto(ticket);
        } finally {
            MDC.clear();
        }
    }

    @Transactional
    public IncidentSummaryResponse withdrawTicket(String ticketNumber) {
        User currentUser = securityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("Authentication required"));
        
        Ticket ticket = findTicketByNumberOrThrow(ticketNumber);
        String oldStatus = ticket.getStatus();

        if (!ticket.getCreatedBy().getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("Access denied: Only the reporter can withdraw this incident.");
        }

        if ("CLOSED".equals(oldStatus) || "RESOLVED".equals(oldStatus)) {
            throw new RuntimeException("Access denied: Cannot withdraw a " + oldStatus + " incident.");
        }

        setupMdc(ticket);

        try {
            log.info("Ticket withdrawal: ticket={} user={}", ticket.getTicketNumber(), currentUser.getWorkerId());

            ticket.setStatus("WITHDRAWN");
            ticket.setUpdatedAt(Instant.now());
            ticketRepository.save(ticket);

            recordHistory(ticket, "WITHDRAWN", oldStatus, "WITHDRAWN", currentUser);
            broadcastStatusChange(ticket);
            return toSummaryDto(ticket);

        } finally {
            MDC.clear();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // PHASE 2 — ASSIGN USER
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    @Override
    public IncidentSummaryResponse assignTicket(String ticketNumber, AssignRequest request) {
        return assignUser(ticketNumber, request.assignedUserId());
    }

    @Transactional
    public IncidentSummaryResponse assignUser(String ticketNumber, String workerIdString) {
        User currentUser = securityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("Authentication required"));
        
        Ticket ticket = findTicketByNumberOrThrow(ticketNumber);
        setupMdc(ticket);

        try {
            User assignee = userRepository.findByWorkerId(workerIdString)
                    .orElseThrow(() -> new RuntimeException("User not found with workerId: " + workerIdString));

            String previousAssigneeName = ticket.getAssignedTo() != null
                    ? ticket.getAssignedTo().getFullName()
                    : "Unassigned";

            ticket.setAssignedTo(assignee);
            ticket.setUpdatedAt(Instant.now());
            ticketRepository.save(ticket);

            recordHistory(ticket, "ASSIGNED", previousAssigneeName, assignee.getFullName(), currentUser);
            saveComment(ticket, currentUser, "Ticket assigned to " + assignee.getFullName(), "ASSIGNMENT");

            eventPublisher.notifyWorker(assignee.getUserId(), TicketEventMapper.toEventDto(ticket, "ASSIGNED"));
            eventPublisher.pushDashboardUpdate(getEnterpriseStats());

            return toSummaryDto(ticket);
        } finally {
            MDC.clear();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // PHASE 2 — ADD COMMENT
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    public TicketComment addComment(String ticketNumber, String commentText) {
        User currentUser = securityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("Authentication required"));
        
        Ticket ticket = findTicketByNumberOrThrow(ticketNumber);
        
        if ("WORKER".equals(currentUser.getRole()) && !ticket.getCreatedBy().getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("Access denied: You can only comment on your own incidents.");
        }

        setupMdc(ticket);

        try {
            TicketComment comment = saveComment(ticket, currentUser, commentText, "NOTE");
            log.info("COMMENT_ADDED: ticket={} user={}", ticket.getTicketNumber(), currentUser.getWorkerId());
            return comment;
        } finally {
            MDC.clear();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // PHASE 2 — GET HISTORY TIMELINE
    // ─────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<HistoryEventResponse> getHistory(String ticketNumber) {
        User currentUser = securityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("Auth required"));
        
        Ticket ticket = findTicketByNumberOrThrow(ticketNumber);
        validateAccess(currentUser, ticket);

        List<HistoryEventResponse> timeline = new ArrayList<>();

        // 1. Audit History
        List<IncidentHistory> historyEntries =
                incidentHistoryRepository.findByTicketTicketIdOrderByEventTimestampDesc(ticket.getTicketId());

        for (IncidentHistory h : historyEntries) {
            String actorName = h.getChangedByUser() != null ? h.getChangedByUser().getFullName() : "System";
            String description = buildHistoryDescription(h);
            String actorRole = h.getChangedByUserRole() != null ? h.getChangedByUserRole() : "SYSTEM";

            timeline.add(new HistoryEventResponse(
                    h.getEventType(), description, h.getOldValue(), h.getNewValue(),
                    actorName, actorRole, h.getFromRole(), h.getToRole(), h.getEventTimestamp()
            ));
        }

        // 2. Comments
        List<TicketComment> comments =
                ticketCommentRepository.findByTicketTicketIdOrderByCreatedAtDesc(ticket.getTicketId());

        for (TicketComment c : comments) {
            if ("ASSIGNMENT".equals(c.getCommentType())) continue;
            String actorName = c.getUser() != null ? c.getUser().getFullName() : "System";

            timeline.add(new HistoryEventResponse(
                    "COMMENT", c.getCommentText(), null, null,
                    actorName, c.getUser() != null ? c.getUser().getRole() : "SYSTEM",
                    null, null, c.getCreatedAt()
            ));
        }

        timeline.sort(Comparator.comparing(HistoryEventResponse::eventTimestamp));
        return timeline;
    }

    @Transactional
    public IncidentSummaryResponse reopenTicket(String ticketNumber, String reason) {
        User user = securityUtils.getCurrentUser().orElseThrow();
        Ticket ticket = findTicketByNumberOrThrow(ticketNumber);
        
        if (!ticket.getCreatedBy().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("Access denied: Only reporter can reopen.");
        }
        
        if (!"RESOLVED".equals(ticket.getStatus())) {
            throw new RuntimeException("Cannot reopen " + ticket.getStatus() + " incident.");
        }
        
        String oldStatus = ticket.getStatus();
        ticket.setStatus("OPEN");
        ticket.setUpdatedAt(Instant.now());
        ticketRepository.save(ticket);
        
        recordHistory(ticket, "REOPENED", oldStatus, "OPEN", user);
        saveComment(ticket, user, "Reopened: " + reason, "REOPEN");
        
        if (ticket.getDepartment() != null) {
            eventPublisher.notifySupervisor(ticket.getDepartment().getDepartmentCode(), 
                    TicketEventMapper.toEventDto(ticket, "REOPENED"));
        }
        eventPublisher.pushDashboardUpdate(getEnterpriseStats());

        return toSummaryDto(ticket);
    }

    public Map<String, Object> getEnterpriseStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTickets", ticketRepository.count());
        stats.put("openTickets", ticketRepository.countByStatusNotIn(List.of("CLOSED", "RESOLVED", "WITHDRAWN")));
        stats.put("resolvedTickets", ticketRepository.countByStatus("RESOLVED"));
        stats.put("slaBreachRate", "0.0%"); 
        stats.put("avgResolutionTime", "N/A");
        return stats;
    }

    // ─────────────────────────────────────────────────────────────────────
    // INTERNAL HELPERS
    // ─────────────────────────────────────────────────────────────────────

    public void recordHistory(Ticket ticket, String eventType, String oldValue, String newValue, User changedBy) {
        recordHistory(ticket, eventType, oldValue, newValue, changedBy, null, null);
    }

    public void recordHistory(Ticket ticket, String eventType, String oldValue, String newValue, User changedBy, String fromRole, String toRole) {
        IncidentHistory history = IncidentHistory.builder()
                .ticket(ticket).eventType(eventType).oldValue(oldValue).newValue(newValue)
                .changedByUser(changedBy).changedByUserRole(changedBy != null ? changedBy.getRole() : "SYSTEM")
                .fromRole(fromRole).toRole(toRole).eventTimestamp(Instant.now()).build();
        incidentHistoryRepository.save(history);
    }

    private TicketComment saveComment(Ticket ticket, User author, String text, String commentType) {
        TicketComment comment = TicketComment.builder()
                .ticket(ticket).user(author).commentText(text).commentType(commentType)
                .createdAt(Instant.now()).build();
        return ticketCommentRepository.save(comment);
    }

    private Ticket findTicketByNumberOrThrow(String ticketNumber) {
        return ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketNumber));
    }

    private Ticket findTicketOrThrow(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + ticketId));
    }

    private void setupMdc(Ticket ticket) {
        MDC.put("ticketNumber", ticket.getTicketNumber());
        MDC.put("severity", ticket.getSeverity());
    }

    private String buildHistoryDescription(IncidentHistory h) {
        return switch (h.getEventType()) {
            case "CREATED"      -> "Incident reported and classified";
            case "STATUS_CHANGE" -> "Status transition to " + h.getNewValue();
            case "ASSIGNED"     -> "Assigned to " + h.getNewValue();
            case "ESCALATED"    -> "Escalated to " + h.getToRole();
            case "SLA_BREACHED" -> "SLA Breached!";
            case "REOPENED"     -> "Ticket reopened";
            case "RESOLVED"     -> "Resolved";
            case "WITHDRAWN"    -> "Withdrawn by reporter";
            default             -> h.getEventType();
        };
    }

    private void validateAccess(User user, Ticket ticket) {
        String role = user.getRole();
        if ("ADMIN".equals(role) || "MANAGER".equals(role)) return;
        if ("SUPERVISOR".equals(role)) {
            if (ticket.getDepartment() == null || user.getDepartment() == null ||
                !ticket.getDepartment().getDepartmentId().equals(user.getDepartment().getDepartmentId())) {
                throw new RuntimeException("Access denied: Outside department");
            }
            return;
        }
        if ("WORKER".equals(role) && !ticket.getCreatedBy().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("Access denied: Not your incident");
        }
    }

    private void validateStatusTransition(User user, String status) {
        if (status == null) return;
        String role = user.getRole();
        if ("ADMIN".equals(role)) return;
        
        List<String> supervisorAllowed = List.of("IN_PROGRESS", "RESOLVED");
        List<String> managerAllowed = List.of("IN_PROGRESS", "RESOLVED", "ESCALATED");

        if ("SUPERVISOR".equals(role) && !supervisorAllowed.contains(status)) {
            throw new RuntimeException("Supervisor cannot set status to " + status);
        } else if ("MANAGER".equals(role) && !managerAllowed.contains(status)) {
            throw new RuntimeException("Manager cannot set status to " + status);
        } else if ("WORKER".equals(role) && !"WITHDRAWN".equals(status)) {
            throw new RuntimeException("Worker cannot set status to " + status);
        }
    }

    public IncidentSummaryResponse toSummaryDto(Ticket ticket) {
        Long remaining = ticket.getSlaDeadline() != null ? slaEngineService.getMinutesUntilSla(ticket.getSlaDeadline()) : null;
        return new IncidentSummaryResponse(
                ticket.getTicketId(), ticket.getTicketNumber(),
                ticket.getCreatedBy() != null ? ticket.getCreatedBy().getFullName() : "Unknown",
                ticket.getIncidentType() != null ? ticket.getIncidentType().getTypeCode() : "N/A",
                ticket.getSeverity(), ticket.getStatus(), ticket.getPriority(),
                ticket.getDepartment() != null ? ticket.getDepartment().getDepartmentName() : "N/A",
                ticket.getAssignedTo() != null ? ticket.getAssignedTo().getFullName() : null,
                ticket.getSlaTargetMinutes(), ticket.getSlaDeadline(), remaining, ticket.getCreatedAt()
        );
    }

    private void broadcastStatusChange(Ticket ticket) {
        // Notify Worker if Resolved
        if ("RESOLVED".equals(ticket.getStatus()) || "CLOSED".equals(ticket.getStatus())) {
            eventPublisher.notifyWorker(ticket.getCreatedBy().getUserId(), 
                    TicketEventMapper.toEventDto(ticket, "STATUS_UPDATE"));
        }
        
        // Notify Supervisor
        if (ticket.getDepartment() != null) {
            eventPublisher.notifySupervisor(ticket.getDepartment().getDepartmentCode(), 
                    TicketEventMapper.toEventDto(ticket, "STATUS_UPDATE"));
        }
        
        eventPublisher.pushDashboardUpdate(getEnterpriseStats());
    }
}
