package ma.darsma.backend.booking;

import ma.darsma.backend.notification.NotificationService;
import ma.darsma.backend.shared.audit.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookingDisputeServiceTest {

    private BookingRepository bookingRepository;
    private NotificationService notificationService;
    private AuditLogService auditLogService;
    private BookingDisputeService disputeService;

    private final UUID studentId = UUID.randomUUID();
    private final UUID tutorId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        notificationService = mock(NotificationService.class);
        auditLogService = mock(AuditLogService.class);
        disputeService = new BookingDisputeService(bookingRepository, notificationService, auditLogService);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Booking escrowHeldBooking() {
        return Booking.builder().studentUserId(studentId).tutorUserId(tutorId)
                .gigRequestId(UUID.randomUUID()).agreedPriceMad(new BigDecimal("200.00"))
                .status(BookingStatus.ESCROW_HELD).build();
    }

    @Test
    void raise_byStudent_transitionsToDisputedAndNotifiesTutor() {
        Booking booking = escrowHeldBooking();
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));

        Booking result = disputeService.raise(bookingId, studentId);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.DISPUTED);
        verify(notificationService).create(eq(tutorId), eq("BOOKING_DISPUTED"), any());
        verify(auditLogService).record(eq(studentId), eq("BOOKING_DISPUTED"), eq("bookings"), eq(bookingId), any());
    }

    @Test
    void raise_byTutor_notifiesStudent() {
        Booking booking = escrowHeldBooking();
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));

        disputeService.raise(bookingId, tutorId);

        verify(notificationService).create(eq(studentId), eq("BOOKING_DISPUTED"), any());
    }

    @Test
    void raise_nonParty_throwsForbidden() {
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(escrowHeldBooking()));

        assertThatThrownBy(() -> disputeService.raise(bookingId, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not a party");
    }

    @Test
    void raise_alreadyDisputed_isIdempotentNoOp() {
        Booking booking = escrowHeldBooking();
        booking.setStatus(BookingStatus.DISPUTED);
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));

        Booking result = disputeService.raise(bookingId, studentId);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.DISPUTED);
        verifyNoInteractions(notificationService, auditLogService);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void raise_notEscrowHeld_throwsConflict() {
        Booking booking = escrowHeldBooking();
        booking.setStatus(BookingStatus.COMPLETED);
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> disputeService.raise(bookingId, studentId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not eligible for dispute");
    }

    @Test
    void raise_notFound_throwsNotFound() {
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> disputeService.raise(bookingId, studentId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }
}
