package com.example.localchat.adapters.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO: Minimal ticket info.
 */
@Schema(description = "Basic ticket response")
public record TicketResponse(
        String ticketNumber,
        String title,
        String status
) {}
