package com.example.localchat.adapters.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO: Update the status of an incident.
 */
@Schema(description = "Status update request body")
public record UpdateStatusRequest(

        @NotBlank(message = "New status is required")
        @Pattern(
                regexp = "OPEN|IN_PROGRESS|RESOLVED|CLOSED|WITHDRAWN",
                message = "Invalid status. Must be OPEN, IN_PROGRESS, RESOLVED, CLOSED, or WITHDRAWN"
        )
        @Schema(description = "The target status for the ticket", example = "IN_PROGRESS")
        String status,

        @Schema(description = "Optional reason or comment for the status change", example = "Starting investigation of motor housing.")
        String comment
) {}
