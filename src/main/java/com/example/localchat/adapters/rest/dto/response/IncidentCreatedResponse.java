package com.example.localchat.adapters.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

import java.time.Instant;

/**
 * Response DTO: Confirmation of a reported incident.
 */
@Schema(description = "Response containing the newly created incident details")
public record IncidentCreatedResponse(
        @Schema(description = "Assigned incident ticket number", example = "TIC-2024-001")
        String ticketNumber,

        @Schema(description = "AI-predicted incident category", example = "MACHINE_FAILURE")
        String incidentType,

        @Schema(description = "AI-suggested severity level", example = "HIGH")
        String severity,

        @Schema(description = "Department assigned by the system", example = "MAINTENANCE")
        String department,

        @Schema(description = "Ticket priority", example = "HIGH")
        String priority,

        @Schema(description = "Target SLA in minutes")
        Integer slaTargetMinutes,

        @Schema(description = "SLA Deadline")
        Instant slaDeadline,

        @Schema(description = "Ticket status")
        String status,

        @Schema(description = "Immediate actions suggested by the AI")
        List<String> suggestedActions,

        @Schema(description = "Worker who created the ticket")
        String createdBy,

        @Schema(description = "Worker assigned to the ticket")
        String assignedTo,

        @Schema(description = "Role of assigned worker")
        String assignedRole,

        @Schema(description = "Ticket creation time")
        Instant createdAt
) {}
