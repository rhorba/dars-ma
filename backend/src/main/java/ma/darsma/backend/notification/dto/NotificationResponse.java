package ma.darsma.backend.notification.dto;

import ma.darsma.backend.notification.Notification;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        Map<String, Object> payload,
        Instant readAt,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getPayload(),
                notification.getReadAt(), notification.getCreatedAt());
    }
}
