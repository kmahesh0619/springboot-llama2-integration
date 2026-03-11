package com.example.localchat.config;

import com.example.localchat.adapters.llm.OllamaRestClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configures the Ollama {@link RestClient} bean and exposes
 * strongly-typed {@link OllamaProperties} via {@code @ConfigurationProperties}.
 * 
 * Spring AI ChatClient is auto-configured based on the spring.ai.ollama.* 
 * properties in application.yml when spring-ai-ollama-spring-boot-starter is present.
 */
@Configuration
public class OllamaConfig {

    /**
     * Ollama connection settings, bound from the {@code ollama.*} YAML namespace.
     *
     * @param baseUrl        Ollama base URL (default: http://localhost:11434)
     * @param model          model name     (default: llama2)
     * @param timeoutSeconds HTTP timeout   (default: 90)
     */
    @ConfigurationProperties(prefix = "ollama")
    public record OllamaProperties(
            @DefaultValue("http://localhost:11434") String baseUrl,
            @DefaultValue("llama2")                 String model,
            @DefaultValue("90")                     int timeoutSeconds
    ) {}

    /**
     * Shared {@link RestClient} pre-configured with the Ollama base URL.
     * All Ollama calls share this single instance.
     */
    @Bean
    public RestClient ollamaRestClient(OllamaProperties props) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept",       "application/json")
                .build();
    }

    /**
     * Registers {@link OllamaRestClient} as a managed bean.
     */
    @Bean
    public OllamaRestClient ollamaGenerationPort(RestClient ollamaRestClient) {
        return new OllamaRestClient(ollamaRestClient);
    }
}
