package com.example.localchat.adapters.rest.dto.request;

import com.example.localchat.adapters.rest.dto.request.MessageDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request DTO: Send a diagnostic message to the AI.
 */
@Schema(description = "Chat API request structure")
public record ChatRequest(

        @NotBlank(message = "Session ID is required")
        @Schema(description = "Unique session or ticket number for context tracking", example = "TIC-2024-001")
        String sessionId,

        @NotBlank(message = "Message cannot be empty")
        @Schema(description = "The question or update from the worker", example = "How do I recalibrate the temperature sensor?")
        String message,

        @Schema(description = "Current role of the user (affects AI persona)", example = "WORKER")
        String role,

        @Valid
        @Schema(description = "Optional conversation history for context-aware replies")
        List<MessageDto> history
) {}
