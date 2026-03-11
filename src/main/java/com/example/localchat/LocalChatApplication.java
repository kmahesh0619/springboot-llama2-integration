package com.example.localchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Local AI Chat Backend – entry point.
 *
 * <p>Swagger UI: <a href="http://localhost:8081/swagger-ui.html">http://localhost:8081/swagger-ui.html</a>
 *
 * <p>Architecture: Clean / Hexagonal
 * <pre>
 *   domain      ← DTOs, entities, value objects (zero Spring deps)
 *   application ← use-case port interfaces + ChatService
 *   adapters    ← REST controllers, Ollama HTTP client, Caffeine cache
 *   config      ← Spring beans: Swagger, Ollama RestClient, Cache
 * </pre>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
@EnableScheduling
public class LocalChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(LocalChatApplication.class, args);
    }
}
