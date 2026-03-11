package com.example.localchat.adapters.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO: Report a new industrial incident.
 */
@Schema(description = "Incident report request body")
public record IncidentReportRequest(

        @NotBlank(message = "Incident title is required")
        @Size(max = 200, message = "Title is too long")
        @Schema(description = "Short title for the incident", example = "Conveyor Belt Motor Overheating")
        String title,

        @NotBlank(message = "Incident description is required")
        @Schema(description = "Detailed description of the problem", example = "Motor on line 3 is emitting smoke and burning smell.")
        String description,

        @Schema(description = "Reference ID for the reporting worker", example = "EMP042")
        String workerId,

        @Schema(description = "Optional department tag", example = "MAINTENANCE")
        String department
) {}
