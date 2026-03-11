package com.example.localchat.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * DTO: AI Classification Result (Internal)
 * Represents the structured output from Llama2 classification
 */
@Schema(description = "AI classification result for incident")
public record IncidentClassificationDto(
    
    @Schema(description = "Type of incident", example = "MACHINE_FAILURE")
    String incidentType,
    
    @Schema(description = "Severity assessment", example = "HIGH")
    String severity,
    
    @Schema(description = "Responsible department", example = "MAINTENANCE")
    String department,
    
    @Schema(description = "Priority ranking", example = "P2")
    String priority,
    
    @Schema(description = "Suggested mitigation actions")
    List<String> suggestedActions,
    
    @Schema(description = "Confidence score 0.0-1.0", example = "0.95")
    Double confidenceScore
) {}
