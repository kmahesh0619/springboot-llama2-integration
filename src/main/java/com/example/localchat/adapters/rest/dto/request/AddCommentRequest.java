package com.example.localchat.adapters.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO: Add a comment to an incident ticket.
 */
@Schema(description = "Request body to add a comment to an incident")
public record AddCommentRequest(

        @NotBlank(message = "Comment text is required")
        @Size(min = 3, max = 2000, message = "Comment must be between 3 and 2000 characters")
        @Schema(
                description = "Comment text to add to the incident timeline",
                example = "Technician dispatched to machine line 3"
        )
        String comment
) {}
