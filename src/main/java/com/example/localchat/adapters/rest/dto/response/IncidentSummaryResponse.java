package com.example.localchat.adapters.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Response DTO: A summary of an incident ticket.
 */
@Schema(description = "Summary details for an incident")
public record IncidentSummaryResponse(
        Long ticketId,
        String ticketNumber,
        String reportedBy,
        String incidentType,
        String severity,
        String status,
        String priority,
        String department,
        String assignedTo,
        Integer slaTargetMinutes,
        Instant slaDeadline,
        Long slaRemainingMinutes,
        Instant createdAt
) {}
