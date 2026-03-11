package com.example.localchat.domain.value;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object wrapping a session identifier. Never blank.
 */
public record SessionId(String value) {

    public SessionId {
        Objects.requireNonNull(value, "sessionId must not be null");
        if (value.isBlank()) throw new IllegalArgumentException("sessionId must not be blank");
    }

    public static SessionId of(String raw) { return new SessionId(raw); }

    public static SessionId generate() { return new SessionId(UUID.randomUUID().toString()); }

    @Override
    public String toString() { return value; }
}
