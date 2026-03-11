package com.example.localchat.domain.entity;

import com.example.localchat.domain.value.SessionId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate root: an ordered list of messages in one session.
 *
 * <p>Immutable – mutating operations return new instances.
 */
public record Conversation(SessionId sessionId, List<ChatMessage> messages) {

    public Conversation(SessionId sessionId, List<ChatMessage> messages) {
        this.sessionId = sessionId;
        this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
    }

    public static Conversation empty(SessionId sessionId) {
        return new Conversation(sessionId, List.of());
    }

    public Conversation addMessage(ChatMessage message) {
        var updated = new ArrayList<>(this.messages);
        updated.add(message);
        return new Conversation(this.sessionId, updated);
    }

    /**
     * Returns a new conversation that retains only the last {@code n} messages.
     */
    public Conversation trimToLast(int n) {
        if (messages.size() <= n) return this;
        return new Conversation(sessionId, messages.subList(messages.size() - n, messages.size()));
    }

    public int size() { return messages.size(); }
}
