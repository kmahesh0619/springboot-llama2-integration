package com.example.localchat.adapters.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO: Register a new factory user.
 */
@Schema(description = "Registration request structure")
public record RegisterRequest(
    @Schema(description = "Optional worker ID (will be auto-generated if empty)", example = "EMP001")
    String workerId,

    @NotBlank(message = "Full name is required")
    @Schema(description = "Full legal name of the employee", example = "John Doe")
    String fullName,

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Schema(description = "Official company email", example = "john.doe@factory.com")
    String email,

    @Schema(description = "Contact phone number")
    String phone,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "Secret password")
    String password,

    @NotBlank(message = "Role is required")
    @Schema(description = "Assigned system role", example = "WORKER")
    String role,

    @JsonProperty("department")
    @NotNull(message = "Department ID is required")
    @Schema(description = "Database ID of the assigned department", example = "1")
    Long departmentId
) {}
