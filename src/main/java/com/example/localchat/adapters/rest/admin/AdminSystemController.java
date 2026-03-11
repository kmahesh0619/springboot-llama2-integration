package com.example.localchat.adapters.rest.admin;

import com.example.localchat.adapters.rest.dto.ApiResponse;
import com.example.localchat.application.service.incident.IncidentManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ADMIN Role Controller - System Management
 * Responsible for system-wide configuration, health, and audit.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/system")
@RequiredArgsConstructor
@Tag(name = "Admin System Operations", description = "System management and audit controls")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSystemController {

    private final IncidentManagementService incidentService;
    private final HealthEndpoint healthEndpoint;

    @Operation(summary = "Enterprise Dashboard Stats", description = "Get real-time global statistics for the enterprise dashboard.")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEnterpriseStats() {
        log.info("Admin fetching enterprise dashboard stats");
        Map<String, Object> stats = incidentService.getEnterpriseStats();
        return ResponseEntity.ok(ApiResponse.success("Enterprise stats retrieved", stats));
    }

    @Operation(summary = "System Health Detailed", description = "Get detailed system health status from actuator.")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Object>> getDetailedHealth() {
        log.info("Admin checking detailed system health");
        return ResponseEntity.ok(ApiResponse.success("System health retrieved", healthEndpoint.health()));
    }

    @Operation(summary = "System Status", description = "Simple health check for admin routes.")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<String>> getSystemStatus() {
        log.info("Admin checking system status");
        return ResponseEntity.ok(ApiResponse.success("Admin system active", "OK"));
    }
}
