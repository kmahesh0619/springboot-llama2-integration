package com.example.localchat.application.sla;

import com.example.localchat.application.repository.TicketRepository;
import com.example.localchat.application.repository.UserRepository;
import com.example.localchat.application.service.incident.IncidentManagementService;
import com.example.localchat.application.service.mapper.TicketEventMapper;
import com.example.localchat.domain.dto.IncidentEventDTO;
import com.example.localchat.domain.entity.Ticket;
import com.example.localchat.domain.entity.User;
import com.example.localchat.webSocket.IncidentEventPublisher;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlaEscalationScheduler {

    private final TicketRepository ticketRepository;
    private final SlaEngineService slaEngineService;
    private final IncidentManagementService incidentManagementService;
    private final UserRepository userRepository;
    private final IncidentEventPublisher eventPublisher;

    /**
     * Runs every 30 seconds and checks SLA status for all active tickets.
     * Uses ShedLock to avoid concurrent execution in multi-node environments.
     */
    @Scheduled(fixedDelay = 30_000)
    @SchedulerLock(name = "SlaEscalationTask", lockAtMostFor = "10m", lockAtLeastFor = "10s")
    @Transactional
    public void runEscalationCheck() {
        log.info("Running Enterprise SLA escalation scheduler at {}", Instant.now());

        List<Ticket> activeTickets = ticketRepository.findActiveTicketsForEscalationCheck();

        if (activeTickets.isEmpty()) {
            log.debug("No active tickets found for SLA evaluation.");
            return;
        }

        for (Ticket ticket : activeTickets) {
            handleSingleTicket(ticket);
        }
    }

    private void handleSingleTicket(Ticket ticket) {
        Instant deadline = ticket.getSlaDeadline();
        if (deadline == null) {
            log.error("Ticket {} has NULL SLA deadline", ticket.getTicketNumber());
            return;
        }

        // 1. SLA BREACH (Final state)
        if (slaEngineService.isSlaBreached(deadline)) {
            transitionToSlaBreached(ticket);
            return;
        }

        // 2. TIERED ESCALATION / Warning Logic
        if (slaEngineService.shouldEscalate(ticket.getSeverity(), deadline)) {
            performTieredEscalation(ticket);
        }
    }

    private void transitionToSlaBreached(Ticket ticket) {
        if ("SLA_BREACHED".equals(ticket.getStatus())) return;

        String oldStatus = ticket.getStatus();
        ticket.setStatus("SLA_BREACHED");
        ticket.setUpdatedAt(Instant.now());
        
        ticketRepository.save(ticket);
        incidentManagementService.recordHistory(ticket, "SLA_BREACHED", oldStatus, "SLA_BREACHED", null);
        
        // Notify Admin of Breach
        eventPublisher.notifyAdmin(TicketEventMapper.toEventDto(ticket, "SLA_BREACHED"));
        eventPublisher.pushDashboardUpdate(incidentManagementService.getEnterpriseStats());
        
        log.warn("SLA BREACHED | ticket={} severity={} deadline={}", 
                ticket.getTicketNumber(), ticket.getSeverity(), deadlineAsString(ticket));
    }

    private void performTieredEscalation(Ticket ticket) {
        User currentAssignee = ticket.getAssignedTo();
        String currentRole = (currentAssignee != null) ? currentAssignee.getRole() : "UNKNOWN";
        
        String fromRole = currentRole;
        String toRole;
        User nextAssignee = null;
        String nextStatus = "ESCALATED";

        if ("SUPERVISOR".equals(currentRole)) {
            toRole = "MANAGER";
            nextAssignee = userRepository.findFirstByRoleAndDepartment_DepartmentId("MANAGER", ticket.getDepartment().getDepartmentId())
                    .orElseGet(() -> {
                        log.warn("No MANAGER found for department {}, falling back to ADMIN", ticket.getDepartment().getDepartmentCode());
                        return userRepository.findFirstByRole("ADMIN").orElse(null);
                    });
        } else if ("MANAGER".equals(currentRole) || "ESCALATED".equals(ticket.getStatus())) {
            toRole = "ADMIN";
            nextAssignee = userRepository.findFirstByRole("ADMIN").orElse(null);
        } else if ("ADMIN".equals(currentRole)) {
            toRole = "CRITICAL_ALERT";
            nextStatus = "CRITICAL";
        } else {
            toRole = "ADMIN";
            nextAssignee = userRepository.findFirstByRole("ADMIN").orElse(null);
        }

        // Guard against duplicate audit entries
        if (nextStatus.equals(ticket.getStatus()) && (nextAssignee == null || nextAssignee.equals(ticket.getAssignedTo()))) {
            // Even if we don't change state, we might want to send a "Warning" notification if we are in the escalation window
            sendSlaWarning(ticket);
            return;
        }

        String oldStatus = ticket.getStatus();
        ticket.setStatus(nextStatus);
        if (nextAssignee != null) {
            ticket.setAssignedTo(nextAssignee);
        }
        ticket.setUpdatedAt(Instant.now());
        ticketRepository.save(ticket);

        incidentManagementService.recordHistory(ticket, "ESCALATED", oldStatus, nextStatus, null, fromRole, toRole);
        
        // Granular Routing based on target role
        broadcastEscalation(ticket, toRole);
        eventPublisher.pushDashboardUpdate(incidentManagementService.getEnterpriseStats());

        log.info("TIERED ESCALATION | ticket={} fromRole={} toRole={} status={} assignedTo={}",
                ticket.getTicketNumber(), fromRole, toRole, nextStatus, 
                (nextAssignee != null) ? nextAssignee.getWorkerId() : "NONE");
    }

    private void sendSlaWarning(Ticket ticket) {
        // Notify the current role that their time is almost up
        User current = ticket.getAssignedTo();
        if (current == null) return;

        IncidentEventDTO event = TicketEventMapper.toEventDto(ticket, "SLA_WARNING");
        
        if ("SUPERVISOR".equals(current.getRole())) {
            eventPublisher.notifySupervisor(ticket.getDepartment().getDepartmentCode(), event);
        } else if ("MANAGER".equals(current.getRole())) {
            eventPublisher.notifyManager(event);
        } else {
            eventPublisher.notifyAdmin(event);
        }
    }

    private void broadcastEscalation(Ticket ticket, String targetRole) {
        IncidentEventDTO event = TicketEventMapper.toEventDto(ticket, "ESCALATED");
        switch (targetRole) {
            case "MANAGER" -> eventPublisher.notifyManager(event);
            case "ADMIN", "CRITICAL_ALERT" -> eventPublisher.notifyAdmin(event);
            default -> eventPublisher.notifyAdmin(event);
        }
    }

    private String deadlineAsString(Ticket ticket) {
        return ticket.getSlaDeadline() != null ? ticket.getSlaDeadline().toString() : "NULL";
    }
}