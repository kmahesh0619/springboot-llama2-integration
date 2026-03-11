package com.example.localchat.webSocket;

import com.example.localchat.domain.dto.IncidentEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Enterprise Event Publisher
 * Responsible for routing system events to specific WebSocket topics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notify a specific worker about their ticket.
     */
    public void notifyWorker(Long userId, IncidentEventDTO event) {
        String destination = "/topic/worker/" + userId;
        log.debug("Publishing to worker {}: {}", userId, event.getEventType());
        messagingTemplate.convertAndSend(destination, event);
    }

    /**
     * Notify all supervisors in a specific department.
     */
    public void notifySupervisor(String departmentCode, IncidentEventDTO event) {
        String destination = "/topic/supervisor/" + departmentCode;
        log.debug("Publishing to department {}: {}", departmentCode, event.getEventType());
        messagingTemplate.convertAndSend(destination, event);
    }

    /**
     * Notify all Managers.
     */
    public void notifyManager(IncidentEventDTO event) {
        log.debug("Publishing escalation to managers: {}", event.getTicketNumber());
        messagingTemplate.convertAndSend("/topic/manager", event);
    }

    /**
     * Notify all Admins.
     */
    public void notifyAdmin(IncidentEventDTO event) {
        log.debug("Publishing system alert to admins: {}", event.getTicketNumber());
        messagingTemplate.convertAndSend("/topic/admin", event);
    }

    /**
     * Broadcast a global dashboard update (e.g. for real-time counters).
     */
    public void pushDashboardUpdate(Map<String, Object> stats) {
        log.debug("Pushing dashboard stats update");
        messagingTemplate.convertAndSend("/topic/dashboard", stats);
    }

    // --- Legacy / Generic Methods ---

    public void publishIncidentCreated(IncidentEventDTO event) {
        messagingTemplate.convertAndSend("/topic/incidents", event);
    }

    public void publishStatusUpdate(IncidentEventDTO event) {
        messagingTemplate.convertAndSend("/topic/status", event);
    }

    public void publishAssignment(IncidentEventDTO event) {
        messagingTemplate.convertAndSend("/topic/assignments", event);
    }

    public void publishEscalation(IncidentEventDTO event) {
        messagingTemplate.convertAndSend("/topic/escalations", event);
    }
}