package com.example.localchat.adapters.rest.dto.response;

import lombok.Builder;
import java.time.Instant;

@Builder
public record UserDetailResponse(
    Long userId,
    String workerId,
    String fullName,
    String email,
    String phone,
    String role,
    String departmentName,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt,
    Long assignedTicketCount
) {}
