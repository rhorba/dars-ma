package ma.darsma.backend.booking;

import ma.darsma.backend.notification.event.BookingCompletedEvent;
import ma.darsma.backend.notification.event.EscrowReleasedEvent;
import ma.darsma.backend.shared.audit.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookingCompletionServiceTest {

    private BookingRepository bookingRepository;
    private EscrowTransactionRepository escrowTransactionRepository;
    private EscrowPaymentProvider escrowPaymentProvider;
    private ApplicationEventPublisher eventPublisher;
    private AuditLogService auditLogService;
    private BookingCompletionService completionService;

    private final UUID studentId = UUID.randomUUID();
    private final UUID tutorId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        escrowTransactionRepository = mock(EscrowTransactionRepository.class);
        escrowPaymentProvider = mock(EscrowPaymentProvider.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        auditLogService = mock(AuditLogService.class);
        completionService = new BookingCompletionService(bookingRepository, escrowTransactionRepository,
                escrowPaymentProvider, eventPublisher, auditLogService);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Booking escrowHeldBooking() {
        Booking booking = Booking.builder().studentUserId(studentId).tutorUserId(tutorId)
                .gigRequestId(UUID.randomUUID()).agreedPriceMad(new BigDecimal("200.00"))
                .status(BookingStatus.ESCROW_HELD).build();
        setId(booking, bookingId);
        return booking;
    }

    private static void setId(Booking booking, UUID id) {
        try {
            var field = Booking.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(booking, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void confirmCompletion_onlyStudentConfirms_staysEscrowHeld() {
        Booking booking = escrowHeldBooking();
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));

        Booking result = completionService.confirmCompletion(bookingId, studentId);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.ESCROW_HELD);
        assertThat(result.getStudentConfirmedAt()).isNotNull();
        assertThat(result.getTutorConfirmedAt()).isNull();
        verifyNoInteractions(escrowPaymentProvider);
    }

    @Test
    void confirmCompletion_bothConfirm_transitionsToCompletedAndReleasesEscrow() {
        Booking booking = escrowHeldBooking();
        booking.setStudentConfirmedAt(Instant.now());
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        EscrowTransaction escrowTransaction = EscrowTransaction.builder()
                .bookingId(bookingId).cmiReference("mock-ref").amountMad(new BigDecimal("200.00"))
                .status(EscrowStatus.HELD).heldAt(Instant.now()).build();
        when(escrowTransactionRepository.findByBookingId(bookingId)).thenReturn(Optional.of(escrowTransaction));

        Booking result = completionService.confirmCompletion(bookingId, tutorId);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(result.getTutorConfirmedAt()).isNotNull();
        verify(escrowPaymentProvider).release("mock-ref");
        assertThat(escrowTransaction.getStatus()).isEqualTo(EscrowStatus.RELEASED);
        verify(escrowTransactionRepository).save(escrowTransaction);
        verify(eventPublisher).publishEvent(new BookingCompletedEvent(bookingId, studentId, tutorId));
        verify(eventPublisher).publishEvent(new EscrowReleasedEvent(bookingId, studentId, tutorId));
        verify(auditLogService).record(eq(tutorId), eq("BOOKING_COMPLETED"), eq("bookings"), eq(bookingId), any());
    }

    @Test
    void confirmCompletion_nonParty_throwsForbidden() {
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(escrowHeldBooking()));

        assertThatThrownBy(() -> completionService.confirmCompletion(bookingId, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not a party");
        verifyNoInteractions(escrowPaymentProvider);
    }

    @Test
    void confirmCompletion_alreadyCompleted_isIdempotentNoOp() {
        Booking booking = escrowHeldBooking();
        booking.setStatus(BookingStatus.COMPLETED);
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));

        Booking result = completionService.confirmCompletion(bookingId, studentId);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        verifyNoInteractions(escrowPaymentProvider, eventPublisher, auditLogService);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void confirmCompletion_notFound_throwsNotFound() {
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> completionService.confirmCompletion(bookingId, studentId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void confirmCompletion_wrongStatus_throwsConflict() {
        Booking booking = escrowHeldBooking();
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> completionService.confirmCompletion(bookingId, studentId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not awaiting completion");
    }

    @Test
    void confirmCompletion_sameStudentDoubleSubmit_doesNotChangeTimestamp() {
        Booking booking = escrowHeldBooking();
        Instant firstConfirm = Instant.now().minusSeconds(60);
        booking.setStudentConfirmedAt(firstConfirm);
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));

        Booking result = completionService.confirmCompletion(bookingId, studentId);

        assertThat(result.getStudentConfirmedAt()).isEqualTo(firstConfirm);
        assertThat(result.getStatus()).isEqualTo(BookingStatus.ESCROW_HELD);
    }
}
