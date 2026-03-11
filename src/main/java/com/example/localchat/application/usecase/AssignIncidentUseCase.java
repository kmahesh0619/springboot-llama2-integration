package com.example.localchat.application.usecase;

import com.example.localchat.adapters.rest.dto.request.AssignRequest;
import com.example.localchat.adapters.rest.dto.response.IncidentSummaryResponse;

/**
 * Use case for assigning a ticket to a worker.
 */
public interface AssignIncidentUseCase {
    IncidentSummaryResponse assignTicket(String ticketNumber, AssignRequest request);
}
