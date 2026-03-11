package com.example.localchat.adapters.rest.chat;

import com.example.localchat.application.usecase.ChatUseCase;
import com.example.localchat.adapters.rest.dto.request.ChatRequest;
import com.example.localchat.adapters.rest.dto.request.ChatMessageRequest;
import com.example.localchat.adapters.rest.dto.response.ChatResponse;
import com.example.localchat.adapters.rest.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "AI Chat", description = "Incident diagnostic support via LLM")
public class ChatController {

    private final ChatUseCase chatUseCase;

    @Operation(summary = "Diagnostic Chat", description = "Ask AI for diagnostic advice on machine incidents.")
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Diagnostic chat request received");
        ChatResponse response = chatUseCase.chat(request);
        return ResponseEntity.ok(ApiResponse.success("Diagnostic response generated", response));
    }

    @Operation(summary = "AI Incident Reporting", description = "Report an incident using natural language chat.")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/message")
    public ResponseEntity<ApiResponse<ChatResponse>> reportViaChat(@Valid @RequestBody ChatMessageRequest request) {
        log.info("AI incident reporting chat request received");
        ChatResponse response = chatUseCase.processChatMessage(request);
        return ResponseEntity.ok(ApiResponse.success("Incident processed via chat", response));
    }
}
