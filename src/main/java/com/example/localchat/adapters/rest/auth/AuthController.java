package com.example.localchat.adapters.rest.auth;

import com.example.localchat.application.usecase.AuthUseCase;
import com.example.localchat.adapters.rest.dto.response.AuthResponse;
import com.example.localchat.adapters.rest.dto.request.LoginRequest;
import com.example.localchat.adapters.rest.dto.request.RegisterRequest;
import com.example.localchat.adapters.rest.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register and Login endpoints")
public class AuthController {

    private final AuthUseCase authUseCase;

    @Operation(summary = "User Registration", description = "Register a new factory worker with workerId, role, and department.")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration attempt for worker: {}", request.workerId());
        AuthResponse response = authUseCase.register(request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
    }

    @Operation(summary = "User Login", description = "Authenticate using workerId and password to receive a JWT token.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for worker: {}", request.workerId());
        AuthResponse response = authUseCase.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}
