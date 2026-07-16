package ma.darsma.backend.messaging.dto;

import ma.darsma.backend.messaging.Message;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID senderId,
        String body,
        Instant createdAt
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(message.getId(), message.getSenderId(), message.getBody(), message.getCreatedAt());
    }
}
