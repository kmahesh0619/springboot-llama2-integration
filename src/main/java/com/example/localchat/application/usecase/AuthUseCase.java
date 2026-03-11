package com.example.localchat.application.usecase;

import com.example.localchat.adapters.rest.dto.response.AuthResponse;
import com.example.localchat.adapters.rest.dto.request.LoginRequest;
import com.example.localchat.adapters.rest.dto.request.RegisterRequest;

/**
 * Use case for user authentication.
 */
public interface AuthUseCase {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
