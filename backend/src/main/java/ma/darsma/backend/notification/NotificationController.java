package ma.darsma.backend.notification;

import ma.darsma.backend.notification.dto.NotificationResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> list(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return notificationService.listForUser(userId).stream().map(NotificationResponse::from).toList();
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markRead(Authentication authentication, @PathVariable UUID notificationId) {
        UUID userId = UUID.fromString(authentication.getName());
        return NotificationResponse.from(notificationService.markRead(notificationId, userId));
    }
}
