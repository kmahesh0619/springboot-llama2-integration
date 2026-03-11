package com.example.localchat.application.usecase;

import com.example.localchat.adapters.rest.dto.response.IncidentSummaryResponse;

/**
 * Use case for resolving and closing tickets.
 */
public interface CloseTicketUseCase {
    IncidentSummaryResponse resolveTicket(String ticketNumber);
    IncidentSummaryResponse closeTicket(String ticketNumber);
}
