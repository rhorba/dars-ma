package ma.darsma.backend.notification.event;

import ma.darsma.backend.auth.User;
import ma.darsma.backend.auth.UserRepository;
import ma.darsma.backend.notification.NotificationService;
import ma.darsma.backend.notification.email.EmailProvider;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final EmailProvider emailProvider;
    private final UserRepository userRepository;

    public NotificationEventListener(NotificationService notificationService,
                                      EmailProvider emailProvider,
                                      UserRepository userRepository) {
        this.notificationService = notificationService;
        this.emailProvider = emailProvider;
        this.userRepository = userRepository;
    }

    @Async
    @EventListener
    public void onBookingCompleted(BookingCompletedEvent event) {
        Map<String, Object> payload = Map.of("bookingId", event.bookingId().toString());
        notify(event.studentUserId(), "BOOKING_COMPLETED", payload,
                "Your booking is complete", "Your booking has been marked as completed.");
        notify(event.tutorUserId(), "BOOKING_COMPLETED", payload,
                "Your booking is complete", "Your booking has been marked as completed.");
    }

    @Async
    @EventListener
    public void onEscrowReleased(EscrowReleasedEvent event) {
        Map<String, Object> payload = Map.of("bookingId", event.bookingId().toString());
        notify(event.studentUserId(), "ESCROW_RELEASED", payload,
                "Escrow released", "The escrow payment for your booking has been released.");
        notify(event.tutorUserId(), "ESCROW_RELEASED", payload,
                "Escrow released", "The escrow payment for your booking has been released.");
    }

    @Async
    @EventListener
    public void onTutorVerified(TutorVerifiedEvent event) {
        notify(event.tutorUserId(), "TUTOR_VERIFIED",
                Map.of("documentId", event.documentId().toString()),
                "You're verified", "Your tutor profile has been verified.");
    }

    @Async
    @EventListener
    public void onReviewSubmitted(ReviewSubmittedEvent event) {
        notify(event.revieweeId(), "REVIEW_RECEIVED",
                Map.of("bookingId", event.bookingId().toString()),
                "You received a review", "You have received a new review.");
    }

    private void notify(UUID userId, String type, Map<String, Object> payload, String subject, String body) {
        notificationService.create(userId, type, payload);
        userRepository.findById(userId).map(User::getEmail)
                .ifPresent(email -> emailProvider.send(email, subject, body));
    }
}
