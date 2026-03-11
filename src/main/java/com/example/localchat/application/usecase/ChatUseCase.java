package com.example.localchat.application.usecase;

import com.example.localchat.adapters.rest.dto.request.ChatRequest;
import com.example.localchat.adapters.rest.dto.request.ChatMessageRequest;
import com.example.localchat.adapters.rest.dto.response.ChatResponse;

/**
 * Use case for processing AI chat messages.
 */
public interface ChatUseCase {
    ChatResponse chat(ChatRequest request);
    ChatResponse processChatMessage(ChatMessageRequest request);
}
