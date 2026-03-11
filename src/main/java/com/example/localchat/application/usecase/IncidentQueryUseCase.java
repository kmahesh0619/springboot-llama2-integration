package com.example.localchat.application.usecase;

import com.example.localchat.adapters.rest.dto.response.HistoryEventResponse;
import com.example.localchat.adapters.rest.dto.response.IncidentSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;

/**
 * Use case for querying incidents and stats.
 */
public interface IncidentQueryUseCase {
    IncidentSummaryResponse getTicketSummary(String ticketNumber);
    List<HistoryEventResponse> getHistory(String ticketNumber);
    Page<IncidentSummaryResponse> listIncidents(String status, String severity, String departmentCode, Pageable pageable);
    Page<IncidentSummaryResponse> getMyTickets(Pageable pageable);
    Map<String, Object> getEnterpriseStats();
}
