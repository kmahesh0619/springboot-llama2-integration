package com.example.localchat.adapters.rest.manager;

import com.example.localchat.application.usecase.AssignIncidentUseCase;
import com.example.localchat.application.usecase.CloseTicketUseCase;
import com.example.localchat.application.usecase.IncidentQueryUseCase;
import com.example.localchat.adapters.rest.dto.request.AssignRequest;
import com.example.localchat.adapters.rest.dto.response.HistoryEventResponse;
import com.example.localchat.adapters.rest.dto.response.IncidentSummaryResponse;
import com.example.localchat.adapters.rest.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MANAGER Role Controller
 * Responsible for resolution/closure, enterprise stats, and escalated assignments.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/manager")
@RequiredArgsConstructor
@Tag(name = "Manager Operations", description = "Endpoints for managers to resolve, close, and monitor ticket stats")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerIncidentController {

    private final IncidentQueryUseCase incidentQueryUseCase;
    private final CloseTicketUseCase closeTicketUseCase;
    private final AssignIncidentUseCase assignIncidentUseCase;

    @Operation(summary = "Get ticket details", description = "View full ticket details including history.")
    @GetMapping("/tickets/{ticketNumber}")
    public ResponseEntity<ApiResponse<TicketDetailsResponse>> getTicketDetails(@PathVariable String ticketNumber) {
        log.info("Manager fetching ticket details: {}", ticketNumber);
        IncidentSummaryResponse summary = incidentQueryUseCase.getTicketSummary(ticketNumber);
        List<HistoryEventResponse> history = incidentQueryUseCase.getHistory(ticketNumber);
        return ResponseEntity.ok(ApiResponse.success("Ticket details retrieved successfully", new TicketDetailsResponse(summary, history)));
    }

    @Operation(summary = "Resolve a ticket", description = "Final state transition for resolved incidents.")
    @PutMapping("/tickets/{ticketNumber}/resolve")
    public ResponseEntity<ApiResponse<IncidentSummaryResponse>> resolveTicket(@PathVariable String ticketNumber) {
        log.info("Manager resolving ticket: {}", ticketNumber);
        return ResponseEntity.ok(ApiResponse.success("Ticket resolved successfully", closeTicketUseCase.resolveTicket(ticketNumber)));
    }

    @Operation(summary = "Close a ticket", description = "Administrative closure of tickets.")
    @PutMapping("/tickets/{ticketNumber}/close")
    public ResponseEntity<ApiResponse<IncidentSummaryResponse>> closeTicket(@PathVariable String ticketNumber) {
        log.info("Manager closing ticket: {}", ticketNumber);
        return ResponseEntity.ok(ApiResponse.success("Ticket closed successfully", closeTicketUseCase.closeTicket(ticketNumber)));
    }

    @Operation(summary = "Enterprise Stats", description = "Dashboard metrics for managers.")
    @GetMapping("/tickets/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        log.info("Manager fetching enterprise stats");
        return ResponseEntity.ok(ApiResponse.success("Enterprise stats retrieved successfully", incidentQueryUseCase.getEnterpriseStats()));
    }

    @Operation(summary = "Assign ticket", description = "Manager-level assignment (e.g., for escalations).")
    @PutMapping("/tickets/{ticketNumber}/assign")
    public ResponseEntity<ApiResponse<IncidentSummaryResponse>> assignTicket(
            @PathVariable String ticketNumber,
            @RequestBody AssignRequest request) {
        log.info("Manager assigning ticket {} to {}", ticketNumber, request.assignedUserId());
        return ResponseEntity.ok(ApiResponse.success("Ticket assigned successfully", assignIncidentUseCase.assignTicket(ticketNumber, request)));
    }

    public record TicketDetailsResponse(IncidentSummaryResponse details, List<HistoryEventResponse> history) {}
}
