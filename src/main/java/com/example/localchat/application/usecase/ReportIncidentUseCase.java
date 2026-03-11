package com.example.localchat.application.usecase;

import com.example.localchat.adapters.rest.dto.request.IncidentReportRequest;
import com.example.localchat.adapters.rest.dto.response.IncidentCreatedResponse;

/**
 * Use case for reporting a new factory incident.
 */
public interface ReportIncidentUseCase {
    IncidentCreatedResponse reportIncident(IncidentReportRequest request);
    IncidentCreatedResponse createTicketFromMessage(String sessionId, String message);
}
