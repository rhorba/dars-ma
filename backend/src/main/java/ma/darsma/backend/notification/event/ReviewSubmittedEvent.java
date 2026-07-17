package ma.darsma.backend.notification.event;

import java.util.UUID;

public record ReviewSubmittedEvent(UUID revieweeId, UUID bookingId) {
}
