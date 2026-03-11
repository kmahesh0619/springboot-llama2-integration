package com.example.localchat.adapters.rest.admin;

import com.example.localchat.adapters.rest.dto.ApiResponse;
import com.example.localchat.adapters.rest.dto.request.UserUpdateRequest;
import com.example.localchat.adapters.rest.dto.response.UserDetailResponse;
import com.example.localchat.application.service.auth.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * ADMIN Role Controller - User Management
 * Responsible for user creation, roles administration, and access management.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin User Operations", description = "Administrative controls for user management")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "List All Users", description = "Fetch a paginated list of all system users.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserDetailResponse>>> getAllUsers(Pageable pageable) {
        log.info("Admin listing all users");
        Page<UserDetailResponse> users = adminUserService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success("User list retrieved", users));
    }

    @Operation(summary = "Get User Details", description = "Fetch detailed information for a specific user.")
    @GetMapping("/{workerId}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserDetails(@PathVariable String workerId) {
        log.info("Admin fetching details for user: {}", workerId);
        UserDetailResponse user = adminUserService.getUserDetails(workerId);
        return ResponseEntity.ok(ApiResponse.success("User details retrieved", user));
    }

    @Operation(summary = "Update User", description = "Update user roles, department, and status.")
    @PutMapping("/{workerId}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateUser(
            @PathVariable String workerId,
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("Admin updating user: {}", workerId);
        UserDetailResponse updated = adminUserService.updateUser(workerId, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", updated));
    }

    @Operation(summary = "Deactivate User", description = "Soft-delete a user by setting isActive to false.")
    @DeleteMapping("/{workerId}")
    public ResponseEntity<ApiResponse<String>> deactivateUser(@PathVariable String workerId) {
        log.info("Admin deactivating user: {}", workerId);
        adminUserService.deactivateUser(workerId);
        return ResponseEntity.ok(ApiResponse.success("User deactivated successfully", workerId));
    }
}
