package com.example.localchat.adapters.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Response DTO: Audit trail event for an incident.
 */
@Schema(description = "An event in the incident history")
public record HistoryEventResponse(
        @Schema(description = "Type of change (STATUS_CHANGE, ASSIGNMENT, etc.)")
        String eventType,
        
        @Schema(description = "Human-readable description of the event")
        String description,
        
        @Schema(description = "Value before the change")
        String oldValue,
        
        @Schema(description = "Value after the change")
        String newValue,
        
        @Schema(description = "Who made the change")
        String changedBy,

        @Schema(description = "Role of who made the change")
        String changedByRole,

        @Schema(description = "From Role")
        String fromRole,

        @Schema(description = "To Role")
        String toRole,
        
        @Schema(description = "UTC timestamp of the event")
        Instant eventTimestamp
) {}
