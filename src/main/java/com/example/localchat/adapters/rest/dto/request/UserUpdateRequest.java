package com.example.localchat.adapters.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UserUpdateRequest(
    @NotBlank(message = "Full name is required")
    String fullName,
    
    @Email(message = "Invalid email format")
    String email,
    
    String phone,
    
    @NotBlank(message = "Role is required")
    String role,
    
    Long departmentId,
    
    Boolean isActive
) {}
