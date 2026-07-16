package ma.darsma.backend.messaging;

import ma.darsma.backend.booking.Booking;
import ma.darsma.backend.booking.BookingRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class MessagingService {

    private final MessageThreadRepository messageThreadRepository;
    private final MessageRepository messageRepository;
    private final BookingRepository bookingRepository;

    public MessagingService(MessageThreadRepository messageThreadRepository, MessageRepository messageRepository,
                             BookingRepository bookingRepository) {
        this.messageThreadRepository = messageThreadRepository;
        this.messageRepository = messageRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public Message sendMessage(UUID bookingId, UUID senderId, String body) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Booking not found"));
        if (!booking.isParty(senderId)) {
            throw new ResponseStatusException(FORBIDDEN, "Not a party to this booking");
        }

        MessageThread thread = getOrCreateThread(bookingId);
        return messageRepository.save(Message.builder()
                .threadId(thread.getId())
                .senderId(senderId)
                .body(body)
                .build());
    }

    public List<Message> listMessages(UUID bookingId, UUID requesterId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Booking not found"));
        if (!booking.isParty(requesterId)) {
            throw new ResponseStatusException(FORBIDDEN, "Not a party to this booking");
        }
        return messageThreadRepository.findByBookingId(bookingId)
                .map(thread -> messageRepository.findByThreadIdOrderByCreatedAtAsc(thread.getId()))
                .orElseGet(List::of);
    }

    private MessageThread getOrCreateThread(UUID bookingId) {
        return messageThreadRepository.findByBookingId(bookingId).orElseGet(() -> {
            try {
                return messageThreadRepository.save(MessageThread.builder().bookingId(bookingId).build());
            } catch (DataIntegrityViolationException e) {
                return messageThreadRepository.findByBookingId(bookingId).orElseThrow(() -> e);
            }
        });
    }
}
