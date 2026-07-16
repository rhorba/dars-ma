package ma.darsma.backend.messaging;

import jakarta.validation.Valid;
import ma.darsma.backend.messaging.dto.MessageCreateRequest;
import ma.darsma.backend.messaging.dto.MessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/messages")
public class MessageController {

    private final MessagingService messagingService;

    public MessageController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse send(Authentication authentication, @PathVariable UUID bookingId,
                                 @Valid @RequestBody MessageCreateRequest request) {
        UUID senderId = UUID.fromString(authentication.getName());
        return MessageResponse.from(messagingService.sendMessage(bookingId, senderId, request.body()));
    }

    @GetMapping
    public List<MessageResponse> list(Authentication authentication, @PathVariable UUID bookingId) {
        UUID requesterId = UUID.fromString(authentication.getName());
        return messagingService.listMessages(bookingId, requesterId).stream().map(MessageResponse::from).toList();
    }
}
