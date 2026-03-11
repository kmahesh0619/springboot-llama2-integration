package com.example.localchat.adapters.llm;

import com.example.localchat.application.port.out.LlmGenerationPort;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Ollama HTTP adapter – implements {@link LlmGenerationPort} by calling
 * {@code POST /api/generate} on the local Ollama server.
 *
 * <p>Uses Spring 6's {@link RestClient} (modern replacement for RestTemplate).
 * Resilience4j {@code @Retry} retries on network-level failures (see {@code application.yml}).
 */
@Slf4j
public class OllamaRestClient implements LlmGenerationPort {

    private final RestClient restClient;

    /** Spring injects the RestClient bean configured in {@link com.example.localchat.config.OllamaConfig}. */
    public OllamaRestClient(RestClient ollamaRestClient) {
        this.restClient = ollamaRestClient;
    }

    @Override
    @Retry(name = "ollama", fallbackMethod = "generateFallback")
    public String generate(String model, String prompt, double temperature) {
        log.debug("Calling Ollama model={} temperature={} promptChars={}", model, temperature, prompt.length());

        try {
            OllamaGenerateResponse response = restClient
                    .post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(OllamaGenerateRequest.of(model, prompt, temperature))
                    .retrieve()
                    .body(OllamaGenerateResponse.class);

            if (response == null) {
                throw new OllamaException("Ollama returned an empty response body", -1);
            }
            if (response.hasError()) {
                log.error("Ollama application error: {}", response.error());
                throw new OllamaException("Ollama model error: " + response.error(), 200);
            }
            if (response.response() == null || response.response().isBlank()) {
                throw new OllamaException("Ollama returned blank response text", 200);
            }

            log.debug("Ollama OK: {} chars, done={}", response.response().length(), response.done());
            return response.response();

        } catch (RestClientResponseException ex) {
            log.error("Ollama HTTP {} – {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new OllamaException(
                    "Ollama returned HTTP " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString(),
                    ex.getStatusCode().value(), ex);

        } catch (ResourceAccessException ex) {
            log.error("Ollama connection failure: {}", ex.getMessage());
            throw new OllamaException(
                    "Cannot connect to Ollama. Is 'ollama serve' running? " + ex.getMessage(), -1, ex);
        }
    }

    /** Called after all retry attempts are exhausted. */
    @SuppressWarnings("unused")
    private String generateFallback(String model, String prompt, double temperature, Exception ex) {
        log.error("All Ollama retries exhausted for model={}: {}", model, ex.getMessage());
        if (ex instanceof OllamaException oe) throw oe;
        throw new OllamaException("Ollama unavailable after retries: " + ex.getMessage(), -1, ex);
    }
}
