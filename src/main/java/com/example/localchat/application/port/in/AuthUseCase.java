package com.example.localchat.application.port.in;

import com.example.localchat.adapters.rest.dto.response.AuthResponse;
import com.example.localchat.adapters.rest.dto.request.LoginRequest;
import com.example.localchat.adapters.rest.dto.request.RegisterRequest;

public interface AuthUseCase {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
