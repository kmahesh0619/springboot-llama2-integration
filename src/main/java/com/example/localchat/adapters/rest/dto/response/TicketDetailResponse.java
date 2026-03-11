package com.example.localchat.adapters.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.time.Instant;

/**
 * Response DTO: Complete details of a ticket.
 */
@Builder
@Schema(description = "Detailed response for a specific ticket")
public record TicketDetailResponse(
        @Schema(description = "Unique ticket reference number", example = "INC-2026-0045")
        String ticketNumber,

        @Schema(description = "Parsed title of the incident", example = "Machine overheating")
        String title,

        @Schema(description = "Parsed or raw description of the incident", example = "Motor temperature is very high")
        String description,

        @Schema(description = "Current status of the ticket", example = "OPEN")
        String status,

        @Schema(description = "Categorized severity level", example = "HIGH")
        String severity,

        @Schema(description = "Assigned priority level", example = "P2")
        String priority,

        @Schema(description = "Associated factory department", example = "PRODUCTION")
        String department,

        @Schema(description = "UTC timestamp of ticket creation")
        Instant createdAt,

        @Schema(description = "Name or ID of the assigned staff member", example = "maintenance-team")
        String assignedTo
) {}
