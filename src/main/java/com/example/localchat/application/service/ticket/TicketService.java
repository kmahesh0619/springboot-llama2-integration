package com.example.localchat.application.service.ticket;

import com.example.localchat.application.ai.IncidentClassificationService;
import com.example.localchat.application.repository.*;
import com.example.localchat.application.service.mapper.TicketEventMapper;
import com.example.localchat.application.sla.SlaEngineService;
import com.example.localchat.domain.dto.IncidentClassificationDto;
import com.example.localchat.adapters.rest.dto.response.IncidentCreatedResponse;
import com.example.localchat.domain.entity.*;
import com.example.localchat.webSocket.IncidentEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.localchat.application.usecase.ReportIncidentUseCase;
import com.example.localchat.application.usecase.GetTicketDetailUseCase;
import com.example.localchat.adapters.rest.dto.request.IncidentReportRequest;
import com.example.localchat.adapters.rest.dto.response.TicketDetailResponse;
import com.example.localchat.domain.exception.TicketNotFoundException;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TicketService implements ReportIncidentUseCase, GetTicketDetailUseCase {

    private final IncidentClassificationService classificationService;
    private final SlaEngineService slaEngineService;

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final IncidentTypeRepository incidentTypeRepository;
    private final IncidentHistoryRepository incidentHistoryRepository;
    private final TicketCommentRepository ticketCommentRepository;

    private final IncidentEventPublisher eventPublisher;

    @Override
    public IncidentCreatedResponse reportIncident(IncidentReportRequest request) {
        String workerId = request.workerId();
        if (workerId == null || workerId.isBlank()) {
            workerId = getCurrentWorkerId();
        }
        // Backward compatibility rule: originalMessage = title + " : " + description
        String message = request.title() + " : " + request.description();
        return createTicketFromMessage(workerId, message);
    }

    public IncidentCreatedResponse createTicketFromMessage(String sessionId, String message) {
        log.info("Creating ticket sessionId={} messageLength={}", sessionId, message.length());
        try {
            User worker = resolveWorker(sessionId);
            return createTicketForUser(worker, message);
        } catch (Exception ex) {
            log.error("Ticket creation failed", ex);
            throw new RuntimeException("Ticket creation failed: " + ex.getMessage(), ex);
        }
    }

    public IncidentCreatedResponse createTicketForUser(User worker, String message) {
        log.info("Creating ticket for user workerId={} messageLength={}", worker.getWorkerId(), message.length());
        try {
            IncidentClassificationDto classification = classificationService.classifyIncident(message);

            IncidentType incidentType = resolveIncidentType(classification.incidentType());
            Department department = resolveDepartment(classification.department());

            Integer slaMinutes = slaEngineService.calculateSlaMinutes(classification.severity());
            Instant slaDeadline = slaEngineService.calculateSlaDeadline(classification.severity());

            User supervisor = userRepository.findFirstByRoleAndDepartment_DepartmentId("SUPERVISOR", department.getDepartmentId())
                    .orElse(null);

            Ticket ticket = Ticket.builder()
                    .ticketNumber(generateTicketNumber())
                    .createdBy(worker)
                    .assignedTo(supervisor)
                    .originalMessage(message)
                    .incidentType(incidentType)
                    .severity(classification.severity())
                    .priority(classification.priority())
                    .department(department)
                    .status("OPEN")
                    .slaTargetMinutes(slaMinutes)
                    .slaDeadline(slaDeadline)
                    .suggestedActions(classification.suggestedActions())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            Ticket savedTicket = ticketRepository.save(ticket);

            recordIncidentHistory(savedTicket, "CREATED", null, "OPEN", null);
            
            if (supervisor != null) {
                recordIncidentHistory(savedTicket, "ASSIGNED", "UNASSIGNED", supervisor.getFullName(), null);
            }

            addTicketComment(savedTicket, "Ticket auto-created and assigned to supervisor", "SYSTEM");

            if (supervisor != null) {
                eventPublisher.notifySupervisor(department.getDepartmentCode(), 
                        TicketEventMapper.toEventDto(savedTicket, "CREATED"));
            } else {
                eventPublisher.notifyAdmin(TicketEventMapper.toEventDto(savedTicket, "CREATED"));
            }

            return buildResponse(savedTicket, classification);
        } catch (Exception ex) {
            log.error("Ticket creation for user failed", ex);
            throw new RuntimeException("Ticket creation failed: " + ex.getMessage(), ex);
        }
    }

    private User resolveWorker(String workerId) {
        return userRepository.findByWorkerId(workerId)
                .orElseThrow(() -> new RuntimeException("Worker not found in system: " + workerId + 
                        ". Please ensure the worker account exists before reporting incidents."));
    }

    private String getCurrentWorkerId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return principal.toString();
    }

    @Override
    public TicketDetailResponse getTicketDetail(String ticketNumber) {
        log.info("Fetching ticket details for number: {}", ticketNumber);
        
        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new TicketNotFoundException(ticketNumber));
        
        String currentWorkerId = getCurrentWorkerId();
        
        // Security check: Only creator or assignee can view
        boolean isCreator = ticket.getCreatedBy() != null && ticket.getCreatedBy().getWorkerId().equals(currentWorkerId);
        boolean isAssignee = ticket.getAssignedTo() != null && ticket.getAssignedTo().getWorkerId().equals(currentWorkerId);
        
        if (!isCreator && !isAssignee) {
            log.warn("Access denied for worker {} to ticket {}", currentWorkerId, ticketNumber);
            throw new AccessDeniedException("You do not have permission to view this ticket");
        }
        
        return mapToDetailResponse(ticket);
    }

    private TicketDetailResponse mapToDetailResponse(Ticket ticket) {
        String originalMessage = ticket.getOriginalMessage();
        String title = "Incident Report";
        String description = originalMessage;
        
        if (originalMessage != null && originalMessage.contains(" : ")) {
            String[] parts = originalMessage.split(" : ", 2);
            title = parts[0];
            description = parts[1];
        }
        
        return TicketDetailResponse.builder()
                .ticketNumber(ticket.getTicketNumber())
                .title(title)
                .description(description)
                .status(ticket.getStatus())
                .severity(ticket.getSeverity())
                .priority(ticket.getPriority())
                .department(ticket.getDepartment() != null ? ticket.getDepartment().getDepartmentName() : null)
                .createdAt(ticket.getCreatedAt())
                .assignedTo(ticket.getAssignedTo() != null ? ticket.getAssignedTo().getFullName() : "Unassigned")
                .build();
    }

    private IncidentType resolveIncidentType(String typeCode) {
        return incidentTypeRepository.findByTypeCode(typeCode)
                .orElseGet(() -> incidentTypeRepository.findByTypeCode("MAINTENANCE_REQUEST").orElseThrow());
    }

    private Department resolveDepartment(String departmentName) {
        return departmentRepository.findByDepartmentCode(departmentName)
                .or(() -> departmentRepository.findByDepartmentName(departmentName))
                .orElseGet(() -> departmentRepository.findByDepartmentCode("PROD").orElseThrow());
    }

    private String generateTicketNumber() {
        long nextId = ticketRepository.count() + 1001;
        return "INC-" + nextId;
    }

    private void recordIncidentHistory(Ticket ticket, String eventType, String oldValue, String newValue, User changedBy) {
        recordIncidentHistory(ticket, eventType, oldValue, newValue, changedBy, null, null);
    }

    private void recordIncidentHistory(Ticket ticket, String eventType, String oldValue, String newValue,
                                       User changedBy, String fromRole, String toRole) {
        IncidentHistory history = IncidentHistory.builder()
                .ticket(ticket)
                .eventType(eventType)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedByUser(changedBy)
                .changedByUserRole(changedBy != null ? changedBy.getRole() : "SYSTEM")
                .fromRole(fromRole)
                .toRole(toRole)
                .eventTimestamp(Instant.now())
                .build();
        incidentHistoryRepository.save(history);
    }

    private void addTicketComment(Ticket ticket, String comment, String type) {
        TicketComment tc = TicketComment.builder()
                .ticket(ticket)
                .commentText(comment)
                .commentType(type)
                .user(ticket.getCreatedBy())
                .createdAt(Instant.now())
                .build();
        ticketCommentRepository.save(tc);
        eventPublisher.publishStatusUpdate(TicketEventMapper.toEventDto(ticket, "UPDATED"));
    }

    private IncidentCreatedResponse buildResponse(Ticket ticket, IncidentClassificationDto classification) {
        String createdBy = ticket.getCreatedBy() != null ? ticket.getCreatedBy().getWorkerId() : null;
        String assignedTo = ticket.getAssignedTo() != null ? ticket.getAssignedTo().getWorkerId() : null;
        String assignedRole = ticket.getAssignedTo() != null ? ticket.getAssignedTo().getRole() : null;

        return new IncidentCreatedResponse(
                ticket.getTicketNumber(),
                classification.incidentType(),
                classification.severity(),
                ticket.getDepartment() != null ? ticket.getDepartment().getDepartmentName() : null,
                classification.priority(),
                ticket.getSlaTargetMinutes(),
                ticket.getSlaDeadline(),
                ticket.getStatus(),
                classification.suggestedActions(),
                createdBy,
                assignedTo,
                assignedRole,
                ticket.getCreatedAt()
        );
    }

    public Ticket getTicketByNumber(String ticketNumber) {
        return ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketNumber));
    }

    public void closeTicket(String ticketNumber, String resolutionNotes) {
        Ticket ticket = getTicketByNumber(ticketNumber);
        ticket.setStatus("CLOSED");
        ticket.setResolutionNotes(resolutionNotes);
        ticket.setResolvedAt(Instant.now());
        ticket.setUpdatedAt(Instant.now());
        ticketRepository.save(ticket);
        eventPublisher.publishStatusUpdate(TicketEventMapper.toEventDto(ticket, "CLOSED"));
    }

    public void assignTicket(String ticketNumber, Long userId) {
        Ticket ticket = getTicketByNumber(ticketNumber);
        User assignee = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        User previousAssignee = ticket.getAssignedTo();
        ticket.setAssignedTo(assignee);
        ticket.setUpdatedAt(Instant.now());
        ticketRepository.save(ticket);
        recordIncidentHistory(ticket, "ASSIGNED",
                previousAssignee != null ? previousAssignee.getFullName() : "UNASSIGNED",
                assignee.getFullName(), null);
        if (assignee != null) {
            eventPublisher.notifyWorker(assignee.getUserId(), TicketEventMapper.toEventDto(ticket, "ASSIGNED"));
        }
    }

    public Page<Ticket> getTicketsByWorkerId(String workerId, Pageable pageable) {
        User worker = userRepository.findByWorkerId(workerId)
                .orElseThrow(() -> new RuntimeException("User not found: " + workerId));
        return ticketRepository.findByCreatedByUserId(worker.getUserId(), pageable);
    }
}
