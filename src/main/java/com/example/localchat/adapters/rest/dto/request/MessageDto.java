package com.example.localchat.adapters.rest.dto.request;

import com.example.localchat.domain.entity.ChatMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Lightweight projection of a {@link ChatMessage} used in API responses or
 * conversation history listings.
 */
@Schema(description = "A single message in the conversation history")
public record MessageDto(

        @Schema(description = "Message role", example = "user")
        String role,

        @Schema(description = "Message content", example = "What is Spring Boot?")
        String content,

        @Schema(description = "UTC timestamp when this message was created")
        Instant timestamp

) {
    /** Maps a domain entity to its DTO projection. */
    public static MessageDto from(ChatMessage msg) {
        return new MessageDto(
                msg.role().name().toLowerCase(),
                msg.content(),
                msg.timestamp()
        );
    }
}
