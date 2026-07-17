package ma.darsma.backend.notification.event;

import ma.darsma.backend.auth.Role;
import ma.darsma.backend.auth.User;
import ma.darsma.backend.auth.UserRepository;
import ma.darsma.backend.notification.NotificationService;
import ma.darsma.backend.notification.email.EmailProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationEventListenerTest {

    private NotificationService notificationService;
    private EmailProvider emailProvider;
    private UserRepository userRepository;
    private NotificationEventListener listener;

    private final UUID studentId = UUID.randomUUID();
    private final UUID tutorId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        emailProvider = mock(EmailProvider.class);
        userRepository = mock(UserRepository.class);
        listener = new NotificationEventListener(notificationService, emailProvider, userRepository);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(userWithEmail(studentId, "student@example.com")));
        when(userRepository.findById(tutorId)).thenReturn(Optional.of(userWithEmail(tutorId, "tutor@example.com")));
    }

    private User userWithEmail(UUID id, String email) {
        return User.builder().id(id).email(email).passwordHash("h").role(Role.STUDENT).fullName("Test User").build();
    }

    @Test
    void onBookingCompleted_notifiesAndEmailsBothParties() {
        listener.onBookingCompleted(new BookingCompletedEvent(bookingId, studentId, tutorId));

        verify(notificationService).create(eq(studentId), eq("BOOKING_COMPLETED"), any());
        verify(notificationService).create(eq(tutorId), eq("BOOKING_COMPLETED"), any());
        verify(emailProvider).send(eq("student@example.com"), anyString(), anyString());
        verify(emailProvider).send(eq("tutor@example.com"), anyString(), anyString());
    }

    @Test
    void onEscrowReleased_notifiesAndEmailsBothParties() {
        listener.onEscrowReleased(new EscrowReleasedEvent(bookingId, studentId, tutorId));

        verify(notificationService).create(eq(studentId), eq("ESCROW_RELEASED"), any());
        verify(notificationService).create(eq(tutorId), eq("ESCROW_RELEASED"), any());
        verify(emailProvider).send(eq("student@example.com"), anyString(), anyString());
        verify(emailProvider).send(eq("tutor@example.com"), anyString(), anyString());
    }

    @Test
    void onTutorVerified_notifiesAndEmailsTutorOnly() {
        UUID documentId = UUID.randomUUID();

        listener.onTutorVerified(new TutorVerifiedEvent(tutorId, documentId));

        verify(notificationService).create(eq(tutorId), eq("TUTOR_VERIFIED"), any());
        verify(emailProvider).send(eq("tutor@example.com"), anyString(), anyString());
        verify(notificationService, never()).create(eq(studentId), any(), any());
    }

    @Test
    void onReviewSubmitted_notifiesAndEmailsReviewee() {
        listener.onReviewSubmitted(new ReviewSubmittedEvent(studentId, bookingId));

        verify(notificationService).create(eq(studentId), eq("REVIEW_RECEIVED"), any());
        verify(emailProvider).send(eq("student@example.com"), anyString(), anyString());
    }

    @Test
    void notify_skipsEmailWhenUserMissing() {
        UUID unknownUser = UUID.randomUUID();
        when(userRepository.findById(unknownUser)).thenReturn(Optional.empty());

        listener.onReviewSubmitted(new ReviewSubmittedEvent(unknownUser, bookingId));

        verify(notificationService).create(eq(unknownUser), eq("REVIEW_RECEIVED"), any());
        verify(emailProvider, never()).send(any(), any(), any());
    }
}
