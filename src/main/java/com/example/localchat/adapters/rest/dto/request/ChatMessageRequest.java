package com.example.localchat.adapters.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO: Specialized for chat-based incident reporting.
 */
@Schema(description = "Request for reporting incident via chat")
public record ChatMessageRequest(
        @NotBlank(message = "Message is required")
        @Schema(description = "The incident description message from the worker", example = "Motor on line 3 is overheating")
        String message,

        @NotBlank(message = "Session ID is required")
        @Schema(description = "Client session identifier", example = "abc123")
        String sessionId
) {}
