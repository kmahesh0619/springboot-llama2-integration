package com.example.localchat.adapters.rest.worker;

import com.example.localchat.application.usecase.ReportIncidentUseCase;
import com.example.localchat.application.usecase.IncidentQueryUseCase;
import com.example.localchat.application.usecase.GetTicketDetailUseCase;
import com.example.localchat.application.service.incident.IncidentManagementService;
import com.example.localchat.adapters.rest.dto.response.IncidentCreatedResponse;
import com.example.localchat.adapters.rest.dto.request.IncidentReportRequest;
import com.example.localchat.adapters.rest.dto.response.IncidentSummaryResponse;
import com.example.localchat.adapters.rest.dto.response.TicketDetailResponse;
import com.example.localchat.domain.entity.Ticket;
import com.example.localchat.adapters.rest.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * WORKER Role Controller
 * Responsible for reporting incidents, viewing own incidents/tickets, and withdrawing reports.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/worker")
@RequiredArgsConstructor
@Tag(name = "Worker Operations", description = "Endpoints for factory workers to report and track their incidents")
@PreAuthorize("hasRole('WORKER')")
public class WorkerIncidentController {

    private final ReportIncidentUseCase reportIncidentUseCase;
    private final IncidentQueryUseCase incidentQueryUseCase;
    private final GetTicketDetailUseCase getTicketDetailUseCase;
    
    // Using incidentManagementService directly for withdraw/reopen as they are specific to domain service or
    // we could create new use cases for them. Let's keep IncidentManagementService for now for these specific methods.
    private final IncidentManagementService incidentManagementService;

    @Operation(summary = "Report an incident", description = "Report a factory incident in natural language.")
    @PostMapping(path = "/incidents/report", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<IncidentCreatedResponse>> reportIncident(
            @Valid @RequestBody IncidentReportRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String workerId = userDetails.getUsername(); 
        MDC.put("sessionId", workerId);
        
        try {
            log.info("Worker reporting incident: {}", workerId);
            IncidentReportRequest fullRequest = new IncidentReportRequest(request.title(), request.description(), workerId, request.department());
            IncidentCreatedResponse response = reportIncidentUseCase.reportIncident(fullRequest);
            return ResponseEntity.ok(ApiResponse.success("Incident reported successfully", response));
        } finally {
            MDC.remove("sessionId");
        }
    }

    @Operation(summary = "Get my reported incidents", description = "Returns list of incidents reported by the logged-in worker.")
    @GetMapping("/incidents/my")
    public ResponseEntity<ApiResponse<Page<IncidentSummaryResponse>>> getMyIncidents(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        
        log.info("Fetching incidents for worker: {}", userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("My incidents retrieved successfully", incidentQueryUseCase.getMyTickets(pageable)));
    }

    @Operation(summary = "Withdraw own incident", description = "Withdraw a recently reported incident.")
    @PostMapping("/incidents/{id}/withdraw")
    public ResponseEntity<ApiResponse<IncidentSummaryResponse>> withdrawIncident(@PathVariable String id) {
        log.info("Worker requesting to withdraw incident: {}", id);
        IncidentSummaryResponse result = incidentManagementService.withdrawTicket(id);
        return ResponseEntity.ok(ApiResponse.success("Incident withdrawn successfully", result));
    }

    @Operation(summary = "Get my tickets (Assigned or Created)", description = "View tickets relevant to the worker.")
    @GetMapping("/tickets/my-tickets")
    public ResponseEntity<ApiResponse<Page<IncidentSummaryResponse>>> getMyTickets(Pageable pageable) {
        log.info("Fetching my tickets for worker");
        return ResponseEntity.ok(ApiResponse.success("My tickets retrieved successfully", incidentQueryUseCase.getMyTickets(pageable)));
    }

    @Operation(summary = "Reopen a ticket", description = "Reopen a resolved ticket if still not satisfied.")
    @PostMapping("/tickets/{ticketNumber}/reopen")
    public ResponseEntity<ApiResponse<IncidentSummaryResponse>> reopenTicket(
            @PathVariable String ticketNumber,
            @RequestBody ReopenRequest request) {
        log.info("Worker reopening ticket: {}", ticketNumber);
        return ResponseEntity.ok(ApiResponse.success("Ticket reopened successfully", incidentManagementService.reopenTicket(ticketNumber, request.reason())));
    }

    @Operation(summary = "Get ticket details", description = "Fetch complete ticket details by ticket number.")
    @GetMapping("/tickets/{ticketNumber}")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> getTicketDetail(
            @PathVariable String ticketNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Worker {} fetching ticket details for: {}", userDetails.getUsername(), ticketNumber);
        TicketDetailResponse response = getTicketDetailUseCase.getTicketDetail(ticketNumber);
        return ResponseEntity.ok(ApiResponse.success("Ticket retrieved successfully", response));
    }

    public record ReopenRequest(String reason) {}
}
