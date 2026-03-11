package com.example.localchat.adapters.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO: Authenticate using workerId and password.
 */
@Schema(description = "Login request structure")
public record LoginRequest(
    @NotBlank(message = "Worker ID is required")
    @Schema(description = "Unique worker identifying number", example = "EMP001")
    String workerId,

    @NotBlank(message = "Password is required")
    @Schema(description = "User password")
    String password
) {}
