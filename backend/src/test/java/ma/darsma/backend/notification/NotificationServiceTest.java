package ma.darsma.backend.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private NotificationService notificationService;

    private final UUID userId = UUID.randomUUID();
    private final UUID notificationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        notificationService = new NotificationService(notificationRepository);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void create_savesNotificationWithGivenFields() {
        Notification result = notificationService.create(userId, "BOOKING_COMPLETED", Map.of("bookingId", "abc"));

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getType()).isEqualTo("BOOKING_COMPLETED");
        assertThat(result.getPayload()).containsEntry("bookingId", "abc");
    }

    @Test
    void listForUser_returnsRepositoryResultOrderedByCreatedAtDesc() {
        Notification notification = Notification.builder().userId(userId).type("BOOKING_COMPLETED").build();
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(notification));

        List<Notification> result = notificationService.listForUser(userId);

        assertThat(result).containsExactly(notification);
    }

    @Test
    void markRead_owner_setsReadAt() {
        Notification notification = Notification.builder().userId(userId).type("BOOKING_COMPLETED").build();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        Notification result = notificationService.markRead(notificationId, userId);

        assertThat(result.getReadAt()).isNotNull();
    }

    @Test
    void markRead_nonOwner_throwsForbidden() {
        Notification notification = Notification.builder().userId(userId).type("BOOKING_COMPLETED").build();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markRead(notificationId, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not the notification owner");
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markRead_notFound_throwsNotFound() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markRead(notificationId, userId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }
}
