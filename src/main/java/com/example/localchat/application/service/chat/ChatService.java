package com.example.localchat.application.service.chat;

import com.example.localchat.application.ai.IncidentClassificationService;
import com.example.localchat.domain.dto.IncidentClassificationDto;
import com.example.localchat.infrastructure.service.LlamaChatService;
import com.example.localchat.application.usecase.ChatUseCase;
import com.example.localchat.adapters.rest.dto.request.ChatRequest;
import com.example.localchat.adapters.rest.dto.request.ChatMessageRequest;
import com.example.localchat.adapters.rest.dto.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService implements ChatUseCase {

    private final LlamaChatService llamaChatService;
    private final IncidentClassificationService classificationService;

    @Override
    public ChatResponse chat(ChatRequest request) {
        log.info("Generating diagnostic chat response for sessionId={}", request.sessionId());
        
        // 1. Generate conversational response
        String aiResponse = llamaChatService.generateResponse(request.message());

        // 2. Generate classification suggestions
        IncidentClassificationDto suggestion = classificationService.classifyIncident(request.message());
        
        return new ChatResponse(
                request.sessionId(),
                aiResponse,
                Instant.now(),
                suggestion.severity(),
                suggestion.department(),
                suggestion.confidenceScore()
        );
    }

    @Override
    public ChatResponse processChatMessage(ChatMessageRequest request) {
        log.info("Processing AI diagnostic message (sessionId={})", request.sessionId());
        
        // 1. Generate conversational response
        String aiResponse = llamaChatService.generateResponse(request.message());
        
        // 2. Generate classification suggestions (replacing automated ticket creation)
        IncidentClassificationDto suggestion = classificationService.classifyIncident(request.message());
        
        return new ChatResponse(
                request.sessionId(),
                aiResponse,
                Instant.now(),
                suggestion.severity(),
                suggestion.department(),
                suggestion.confidenceScore()
        );
    }
}
