package com.example.localchat.adapters.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO: User profile information.
 */
@Schema(description = "User profile details")
public record UserResponse(
        Long id,
        String workerId,
        String fullName,
        String email,
        String role,
        String departmentName,
        boolean isActive
) {}
