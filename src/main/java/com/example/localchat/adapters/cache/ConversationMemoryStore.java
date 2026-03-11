package com.example.localchat.adapters.cache;

import com.example.localchat.domain.entity.Conversation;
import com.example.localchat.domain.value.SessionId;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * In-memory conversation store backed by a Caffeine cache.
 *
 * <p>Sessions expire 30 minutes after last write; at most 500 concurrent sessions
 * are kept in memory. Both settings are configured in {@link com.example.localchat.config.CacheConfig}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationMemoryStore {

    private final Cache<String, Conversation> conversationCache;

    /**
     * Return the existing conversation for this session, or an empty one
     * if the session is new.
     */
    public Conversation getOrCreate(SessionId sessionId) {
        Conversation existing = conversationCache.getIfPresent(sessionId.value());
        if (existing != null) {
            log.debug("Cache HIT sessionId={} messages={}", sessionId, existing.size());
            return existing;
        }
        log.debug("Cache MISS sessionId={} – creating new conversation", sessionId);
        return Conversation.empty(sessionId);
    }

    /** Persist (overwrite) the conversation for its session. */
    public void save(Conversation conversation) {
        conversationCache.put(conversation.sessionId().value(), conversation);
        log.debug("Saved sessionId={} messages={}", conversation.sessionId(), conversation.size());
    }

    /** Invalidate a session's conversation (useful for testing / reset). */
    public void delete(SessionId sessionId) {
        conversationCache.invalidate(sessionId.value());
    }

    /** Number of live sessions in the cache. */
    public long size() {
        return conversationCache.estimatedSize();
    }
}
