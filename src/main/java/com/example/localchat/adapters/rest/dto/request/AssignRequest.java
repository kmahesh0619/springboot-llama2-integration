package com.example.localchat.adapters.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO: Assign a ticket to a worker.
 */
@Schema(description = "Request body to assign a ticket")
public record AssignRequest(

        @NotBlank(message = "Assigned user ID is required")
        @Schema(description = "The worker ID to assign the ticket to", example = "EMP001")
        String assignedUserId
) {}
