package com.example.localchat.config;

import com.example.localchat.domain.entity.Conversation;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Caffeine cache configuration for in-memory conversation history.
 *
 * <p>Settings:
 * <ul>
 *   <li>Max 500 concurrent sessions</li>
 *   <li>30-minute write TTL (idle sessions evicted automatically)</li>
 *   <li>Stats recording for Actuator metrics</li>
 * </ul>
 */
@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, Conversation> conversationCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(30))
                .recordStats()
                .build();
    }
}
