package com.example.localchat.application.service.mapper;

import com.example.localchat.domain.dto.IncidentEventDTO;
import com.example.localchat.domain.entity.Ticket;

public class TicketEventMapper {

    private TicketEventMapper() {}

    public static IncidentEventDTO toEventDto(Ticket ticket, String eventType) {

        // Guard against null lazy-loaded associations (never let Hibernate proxies crash the event
        // publisher — the worst case is a missing field in the WebSocket event, not a 500).
        String departmentName = (ticket.getDepartment() != null)
                ? ticket.getDepartment().getDepartmentName()
                : "UNKNOWN";

        String workerName = (ticket.getCreatedBy() != null)
                ? ticket.getCreatedBy().getFullName()
                : "UNKNOWN";

        String assignedTo = (ticket.getAssignedTo() != null)
                ? ticket.getAssignedTo().getFullName()
                : null;

        return IncidentEventDTO.builder()
                .ticketId(ticket.getTicketId())
                .ticketNumber(ticket.getTicketNumber())
                .status(ticket.getStatus())
                .severity(ticket.getSeverity())
                .priority(ticket.getPriority())
                .department(departmentName)
                .assignedTo(assignedTo)
                .worker(workerName)
                .createdAt(ticket.getCreatedAt())
                .slaDeadline(ticket.getSlaDeadline())
                .eventType(eventType)
                .build();
    }
}