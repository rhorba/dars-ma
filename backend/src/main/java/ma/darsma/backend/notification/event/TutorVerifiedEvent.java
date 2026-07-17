package ma.darsma.backend.notification.event;

import java.util.UUID;

public record TutorVerifiedEvent(UUID tutorUserId, UUID documentId) {
}
