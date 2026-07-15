package ma.darsma.backend.admin;

import ma.darsma.backend.booking.*;
import ma.darsma.backend.notification.NotificationService;
import ma.darsma.backend.shared.audit.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminBookingDisputeServiceTest {

    private BookingRepository bookingRepository;
    private EscrowTransactionRepository escrowTransactionRepository;
    private EscrowPaymentProvider escrowPaymentProvider;
    private NotificationService notificationService;
    private AuditLogService auditLogService;
    private AdminBookingDisputeService service;

    private final UUID studentId = UUID.randomUUID();
    private final UUID tutorId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        escrowTransactionRepository = mock(EscrowTransactionRepository.class);
        escrowPaymentProvider = mock(EscrowPaymentProvider.class);
        notificationService = mock(NotificationService.class);
        auditLogService = mock(AuditLogService.class);
        service = new AdminBookingDisputeService(bookingRepository, escrowTransactionRepository,
                escrowPaymentProvider, notificationService, auditLogService);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Booking disputedBooking() {
        Booking booking = Booking.builder().studentUserId(studentId).tutorUserId(tutorId)
                .gigRequestId(UUID.randomUUID()).agreedPriceMad(new BigDecimal("200.00"))
                .status(BookingStatus.DISPUTED).build();
        try {
            var field = Booking.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(booking, bookingId);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return booking;
    }

    private EscrowTransaction heldEscrow() {
        return EscrowTransaction.builder().bookingId(bookingId).cmiReference("mock-ref")
                .amountMad(new BigDecimal("200.00")).status(EscrowStatus.HELD).heldAt(Instant.now()).build();
    }

    @Test
    void disputedQueue_delegatesToRepository() {
        Booking booking = disputedBooking();
        when(bookingRepository.findByStatus(BookingStatus.DISPUTED)).thenReturn(List.of(booking));

        assertThat(service.disputedQueue()).containsExactly(booking);
    }

    @Test
    void resolve_release_transitionsToCompletedAndReleasesEscrow() {
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(disputedBooking()));
        EscrowTransaction escrow = heldEscrow();
        when(escrowTransactionRepository.findByBookingId(bookingId)).thenReturn(Optional.of(escrow));

        Booking result = service.resolve(bookingId, adminId, DisputeResolution.RELEASE);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(escrow.getStatus()).isEqualTo(EscrowStatus.RELEASED);
        verify(escrowPaymentProvider).release("mock-ref");
        verify(escrowPaymentProvider, never()).refund(any());
        verify(notificationService).create(eq(studentId), eq("DISPUTE_RESOLVED"), any());
        verify(notificationService).create(eq(tutorId), eq("DISPUTE_RESOLVED"), any());
        verify(auditLogService).record(eq(adminId), eq("DISPUTE_RESOLVED"), eq("bookings"), eq(bookingId), any());
    }

    @Test
    void resolve_refund_transitionsToRefundedAndRefundsEscrow() {
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(disputedBooking()));
        EscrowTransaction escrow = heldEscrow();
        when(escrowTransactionRepository.findByBookingId(bookingId)).thenReturn(Optional.of(escrow));

        Booking result = service.resolve(bookingId, adminId, DisputeResolution.REFUND);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.REFUNDED);
        assertThat(escrow.getStatus()).isEqualTo(EscrowStatus.REFUNDED);
        verify(escrowPaymentProvider).refund("mock-ref");
        verify(escrowPaymentProvider, never()).release(any());
    }

    @Test
    void resolve_notDisputed_throwsConflict() {
        Booking booking = disputedBooking();
        booking.setStatus(BookingStatus.ESCROW_HELD);
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.resolve(bookingId, adminId, DisputeResolution.RELEASE))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not under dispute");
        verifyNoInteractions(escrowPaymentProvider);
    }

    @Test
    void resolve_notFound_throwsNotFound() {
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve(bookingId, adminId, DisputeResolution.RELEASE))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }
}
