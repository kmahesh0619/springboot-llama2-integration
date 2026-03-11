package com.example.localchat.adapters.rest.supervisor;

import com.example.localchat.application.usecase.AssignIncidentUseCase;
import com.example.localchat.application.usecase.IncidentQueryUseCase;
import com.example.localchat.application.service.incident.IncidentManagementService;
import com.example.localchat.adapters.rest.dto.request.*;
import com.example.localchat.adapters.rest.dto.response.*;
import com.example.localchat.adapters.rest.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SUPERVISOR Role Controller
 * Responsible for managing incidents, assigning to workers, updating status, and adding comments.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/supervisor")
@RequiredArgsConstructor
@Tag(name = "Supervisor Operations", description = "Endpoints for supervisors to manage and assign incidents")
@PreAuthorize("hasRole('SUPERVISOR')")
public class SupervisorIncidentController {

    private final IncidentQueryUseCase incidentQueryUseCase;
    private final AssignIncidentUseCase assignIncidentUseCase;
    private final IncidentManagementService incidentManagementService;

    @Operation(summary = "List all incidents", description = "Paginated list with filters for status, severity, and department.")
    @GetMapping(path = "/incidents", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Page<IncidentSummaryResponse>>> listIncidents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Supervisor listing incidents: status={} severity={} dept={}", status, severity, department);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<IncidentSummaryResponse> result = incidentQueryUseCase.listIncidents(status, severity, department, pageable);
        return ResponseEntity.ok(ApiResponse.success("Incidents retrieved successfully", result));
    }

    @Operation(summary = "Update incident status", description = "Transition ticket status (e.g., OPEN -> IN_PROGRESS).")
    @PatchMapping(path = "/incidents/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<IncidentSummaryResponse>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        log.info("Supervisor updating status: incidentId={} newStatus={}", id, request.status());
        IncidentSummaryResponse updated = incidentManagementService.updateStatus(id, request.status());
        return ResponseEntity.ok(ApiResponse.success("Status updated successfully", updated));
    }

    @Operation(summary = "Assign incident", description = "Assign incident to a specific workerId.")
    @PutMapping(path = "/incidents/{id}/assign", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<IncidentSummaryResponse>> assignIncident(
            @PathVariable String id,
            @Valid @RequestBody AssignRequest request
    ) {
        log.info("Supervisor assigning incident: id={} assignedUserId={}", id, request.assignedUserId());
        IncidentSummaryResponse updated = assignIncidentUseCase.assignTicket(id, request);
        return ResponseEntity.ok(ApiResponse.success("Incident assigned successfully", updated));
    }

    @Operation(summary = "Add comment", description = "Add a progress note to the incident.")
    @PostMapping(path = "/incidents/{id}/comments", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> addComment(
            @PathVariable String id,
            @Valid @RequestBody AddCommentRequest request
    ) {
        log.info("Supervisor adding comment to incident: {}", id);
        incidentManagementService.addComment(id, request.comment());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment added successfully", null));
    }

    @Operation(summary = "Get incident history", description = "Full audit timeline of the incident.")
    @GetMapping(path = "/incidents/{id}/history", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<HistoryEventResponse>>> getHistory(@PathVariable String id) {
        log.info("Supervisor fetching history for incident: {}", id);
        List<HistoryEventResponse> timeline = incidentQueryUseCase.getHistory(id);
        return ResponseEntity.ok(ApiResponse.success("History timeline retrieved successfully", timeline));
    }
}
