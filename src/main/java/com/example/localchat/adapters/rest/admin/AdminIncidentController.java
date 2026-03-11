package com.example.localchat.adapters.rest.admin;

import com.example.localchat.adapters.rest.dto.ApiResponse;
import com.example.localchat.adapters.rest.dto.request.AssignRequest;
import com.example.localchat.adapters.rest.dto.response.HistoryEventResponse;
import com.example.localchat.adapters.rest.dto.response.IncidentSummaryResponse;
import com.example.localchat.application.service.incident.IncidentManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ADMIN Role Controller - Incident Management
 * Provides full visibility and control over all system tickets.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/incidents")
@RequiredArgsConstructor
@Tag(name = "Admin Incident Operations", description = "Global ticket oversight and lifecycle management")
@PreAuthorize("hasRole('ADMIN')")
public class AdminIncidentController {

    private final IncidentManagementService incidentService;

    @Operation(summary = "List All Incidents", description = "Admin view of all tickets with global filters.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<IncidentSummaryResponse>>> getAllIncidents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String department,
            Pageable pageable) {
        log.info("Admin listing all incidents");
        Page<IncidentSummaryResponse> list = incidentService.listIncidents(status, severity, department, pageable);
        return ResponseEntity.ok(ApiResponse.success("Global incident list retrieved", list));
    }

    @Operation(summary = "Get Ticket Details", description = "Fetch complete details of any ticket in the system.")
    @GetMapping("/{ticketNumber}")
    public ResponseEntity<ApiResponse<IncidentSummaryResponse>> getTicketDetails(@PathVariable String ticketNumber) {
        log.info("Admin fetching details for ticket: {}", ticketNumber);
        IncidentSummaryResponse detail = incidentService.getTicketSummary(ticketNumber);
        return ResponseEntity.ok(ApiResponse.success("Ticket details retrieved", detail));
    }

    @Operation(summary = "Change Ticket Status", description = "Bypass standard transitions to force a status change.")
    @PostMapping("/{ticketNumber}/status")
    public ResponseEntity<ApiResponse<IncidentSummaryResponse>> updateTicketStatus(
            @PathVariable String ticketNumber,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        log.info("Admin forcing status change for {}: -> {}", ticketNumber, status);
        IncidentSummaryResponse updated = incidentService.updateStatus(ticketNumber, status);
        return ResponseEntity.ok(ApiResponse.success("Ticket status updated by admin", updated));
    }

    @Operation(summary = "Assign Ticket", description = "Assign any ticket to any valid system user.")
    @PostMapping("/{ticketNumber}/assign")
    public ResponseEntity<ApiResponse<IncidentSummaryResponse>> assignTicket(
            @PathVariable String ticketNumber,
            @RequestBody AssignRequest request) {
        log.info("Admin assigning ticket {} to user {}", ticketNumber, request.assignedUserId());
        IncidentSummaryResponse updated = incidentService.assignTicket(ticketNumber, request);
        return ResponseEntity.ok(ApiResponse.success("Ticket assigned by admin", updated));
    }

    @Operation(summary = "Withdraw Ticket", description = "Administrative withdrawal of a ticket.")
    @PostMapping("/{ticketNumber}/withdraw")
    public ResponseEntity<ApiResponse<IncidentSummaryResponse>> withdrawTicket(@PathVariable String ticketNumber) {
        log.info("Admin withdrawing ticket: {}", ticketNumber);
        IncidentSummaryResponse withdrawn = incidentService.withdrawTicket(ticketNumber);
        return ResponseEntity.ok(ApiResponse.success("Ticket withdrawn by admin", withdrawn));
    }

    @Operation(summary = "View Ticket History", description = "Full audit trail of all ticket events.")
    @GetMapping("/{ticketNumber}/history")
    public ResponseEntity<ApiResponse<List<HistoryEventResponse>>> getTicketHistory(@PathVariable String ticketNumber) {
        log.info("Admin viewing history for ticket: {}", ticketNumber);
        List<HistoryEventResponse> history = incidentService.getHistory(ticketNumber);
        return ResponseEntity.ok(ApiResponse.success("Audit history retrieved", history));
    }
}
