package com.example.localchat.adapters.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Response DTO: Outbound response from the chat endpoint.
 */
@Schema(description = "Chat API response")
public record ChatResponse(

        @Schema(description = "Session identifier echoed back", example = "test123")
        String sessionId,

        @Schema(description = "AI-generated reply text")
        String response,

        @Schema(description = "UTC timestamp of the response")
        Instant timestamp,

        @Schema(description = "Suggested incident severity based on analysis", example = "HIGH")
        String suggestedSeverity,

        @Schema(description = "Suggested department for routing", example = "MAINTENANCE")
        String suggestedDepartment,

        @Schema(description = "AI confidence score in the diagnosis", example = "0.85")
        Double confidenceScore

) {}
