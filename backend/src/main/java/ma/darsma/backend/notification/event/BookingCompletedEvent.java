package ma.darsma.backend.notification.event;

import java.util.UUID;

public record BookingCompletedEvent(UUID bookingId, UUID studentUserId, UUID tutorUserId) {
}
