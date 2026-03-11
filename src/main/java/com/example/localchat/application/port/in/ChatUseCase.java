package com.example.localchat.application.port.in;

import com.example.localchat.adapters.rest.dto.request.ChatRequest;
import com.example.localchat.adapters.rest.dto.response.ChatResponse;

/**
 * Primary (driving) port: the only entry point into the core use-case
 * from external adapters (REST controller, etc.).
 */
public interface ChatUseCase {

    /**
     * Process a user message and return the AI-generated reply.
     *
     * @param request validated chat request
     * @return response containing the session ID and AI reply
     */
    ChatResponse chat(ChatRequest request);
}
