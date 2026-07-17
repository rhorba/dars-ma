package ma.darsma.backend.notification.event;

import java.util.UUID;

public record EscrowReleasedEvent(UUID bookingId, UUID studentUserId, UUID tutorUserId) {
}
