package ma.darsma.backend.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification create(UUID userId, String type, Map<String, Object> payload) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .payload(payload)
                .build();
        return notificationRepository.save(notification);
    }
}
