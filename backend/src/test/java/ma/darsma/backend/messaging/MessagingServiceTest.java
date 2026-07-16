package ma.darsma.backend.messaging;

import ma.darsma.backend.booking.Booking;
import ma.darsma.backend.booking.BookingRepository;
import ma.darsma.backend.booking.BookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MessagingServiceTest {

    private MessageThreadRepository messageThreadRepository;
    private MessageRepository messageRepository;
    private BookingRepository bookingRepository;
    private MessagingService messagingService;

    private final UUID studentId = UUID.randomUUID();
    private final UUID tutorId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();
    private final UUID threadId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        messageThreadRepository = mock(MessageThreadRepository.class);
        messageRepository = mock(MessageRepository.class);
        bookingRepository = mock(BookingRepository.class);
        messagingService = new MessagingService(messageThreadRepository, messageRepository, bookingRepository);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Booking booking() {
        return Booking.builder().studentUserId(studentId).tutorUserId(tutorId)
                .gigRequestId(UUID.randomUUID()).agreedPriceMad(new BigDecimal("200.00"))
                .status(BookingStatus.ESCROW_HELD).build();
    }

    private MessageThread existingThread() {
        return MessageThread.builder().bookingId(bookingId).build();
    }

    @Test
    void sendMessage_createsThreadLazilyOnFirstMessage() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking()));
        when(messageThreadRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(messageThreadRepository.save(any(MessageThread.class))).thenAnswer(inv -> inv.getArgument(0));

        Message result = messagingService.sendMessage(bookingId, studentId, "Hello");

        assertThat(result.getSenderId()).isEqualTo(studentId);
        assertThat(result.getBody()).isEqualTo("Hello");
        verify(messageThreadRepository).save(any(MessageThread.class));
    }

    @Test
    void sendMessage_reusesExistingThread() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking()));
        when(messageThreadRepository.findByBookingId(bookingId)).thenReturn(Optional.of(existingThread()));

        messagingService.sendMessage(bookingId, tutorId, "Hi back");

        verify(messageThreadRepository, never()).save(any());
    }

    @Test
    void sendMessage_nonParty_throwsForbidden() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking()));

        assertThatThrownBy(() -> messagingService.sendMessage(bookingId, UUID.randomUUID(), "Hi"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not a party");
        verifyNoInteractions(messageRepository);
    }

    @Test
    void sendMessage_bookingNotFound_throwsNotFound() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messagingService.sendMessage(bookingId, studentId, "Hi"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void listMessages_returnsThreadMessagesForParty() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking()));
        when(messageThreadRepository.findByBookingId(bookingId)).thenReturn(Optional.of(existingThread()));
        Message message = Message.builder().threadId(threadId).senderId(studentId).body("Hi").build();
        when(messageRepository.findByThreadIdOrderByCreatedAtAsc(any())).thenReturn(List.of(message));

        List<Message> result = messagingService.listMessages(bookingId, tutorId);

        assertThat(result).containsExactly(message);
    }

    @Test
    void listMessages_noThreadYet_returnsEmptyList() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking()));
        when(messageThreadRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());

        List<Message> result = messagingService.listMessages(bookingId, studentId);

        assertThat(result).isEmpty();
    }

    @Test
    void listMessages_nonParty_throwsForbidden() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking()));

        assertThatThrownBy(() -> messagingService.listMessages(bookingId, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not a party");
    }
}
