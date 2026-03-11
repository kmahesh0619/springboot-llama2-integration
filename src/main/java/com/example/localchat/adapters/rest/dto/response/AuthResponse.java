package com.example.localchat.adapters.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Response DTO: Authentication response containing the JWT token.
 */
@Builder
@Schema(description = "Authentication response with JWT token")
public record AuthResponse(
    @Schema(description = "JWT access token")
    String token,
    
    @Schema(description = "Database ID of the user")
    Long userId,
    
    @Schema(description = "Worker identifier", example = "EMP001")
    String workerId,
    
    @Schema(description = "Full name of the user")
    String fullName,
    
    @Schema(description = "Assigned user role")
    String role,
    
    @Schema(description = "Optional status message")
    String message
) {}
