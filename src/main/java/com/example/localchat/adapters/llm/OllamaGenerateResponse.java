package com.example.localchat.adapters.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * JSON response from Ollama's {@code /api/generate} endpoint.
 * Unknown fields are silently ignored for forward compatibility.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaGenerateResponse(
        String model,
        String response,
        boolean done,
        String error
) {
    public boolean hasError() {
        return error != null && !error.isBlank();
    }
}
