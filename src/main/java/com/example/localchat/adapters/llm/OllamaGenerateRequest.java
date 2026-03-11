package com.example.localchat.adapters.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * JSON request body sent to {@code POST /api/generate} on the Ollama server.
 */
public record OllamaGenerateRequest(
        String model,
        String prompt,
        boolean stream,
        Options options
) {
    public record Options(double temperature) {}

    public static OllamaGenerateRequest of(String model, String prompt, double temperature) {
        return new OllamaGenerateRequest(model, prompt, false, new Options(temperature));
    }
}
