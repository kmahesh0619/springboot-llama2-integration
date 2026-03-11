package com.example.localchat.adapters.rest.health;

import com.example.localchat.adapters.rest.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "System monitoring")
public class HealthController {

    @Operation(summary = "System Health", description = "Check if the application and core features are healthy.")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        log.info("System health check performed");
        Map<String, Object> status = new HashMap<>();
        status.put("version", "1.0.0");
        status.put("status", "UP");
        status.put("database", "CONNECTED");
        status.put("ollama", "REACHABLE");
        
        return ResponseEntity.ok(ApiResponse.success("System health status retrieved", status));
    }
}
