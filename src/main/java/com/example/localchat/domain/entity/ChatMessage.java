package com.example.localchat.domain.entity;

import java.time.Instant;

/**
 * Domain entity: a single turn in a conversation.
 *
 * @param role      who sent this message
 * @param content   text content
 * @param timestamp creation time (UTC)
 */
public record ChatMessage(Role role, String content, Instant timestamp) {

    public enum Role { USER, ASSISTANT, SYSTEM }

    public static ChatMessage userMessage(String content) {
        return new ChatMessage(Role.USER, content, Instant.now());
    }

    public static ChatMessage assistantMessage(String content) {
        return new ChatMessage(Role.ASSISTANT, content, Instant.now());
    }

    public static ChatMessage systemMessage(String content) {
        return new ChatMessage(Role.SYSTEM, content, Instant.now());
    }
}
