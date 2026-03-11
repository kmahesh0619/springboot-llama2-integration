package com.example.localchat.infrastructure.service;

import com.example.localchat.adapters.llm.OllamaRestClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlamaChatService {

    private final OllamaRestClient ollamaRestClient;

    @Value("${incident.ai.temperature:0.7}")
    private double temperature;

    public String generateResponse(String prompt) {
        log.debug("Generating AI response for prompt: {} chars", prompt.length());
        try {
            return ollamaRestClient.generate("llama2", prompt, temperature);
        } catch (Exception ex) {
            log.error("AI response generation failed: {}", ex.getMessage());
            return "I'm sorry, I'm having trouble connecting to the diagnostic engine right now. Please try again later.";
        }
    }
}
