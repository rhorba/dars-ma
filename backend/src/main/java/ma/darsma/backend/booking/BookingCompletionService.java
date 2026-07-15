package ma.darsma.backend.booking;

import ma.darsma.backend.notification.NotificationService;
import ma.darsma.backend.shared.audit.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class BookingCompletionService {

    private final BookingRepository bookingRepository;
    private final EscrowTransactionRepository escrowTransactionRepository;
    private final EscrowPaymentProvider escrowPaymentProvider;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public BookingCompletionService(BookingRepository bookingRepository,
                                     EscrowTransactionRepository escrowTransactionRepository,
                                     EscrowPaymentProvider escrowPaymentProvider,
                                     NotificationService notificationService,
                                     AuditLogService auditLogService) {
        this.bookingRepository = bookingRepository;
        this.escrowTransactionRepository = escrowTransactionRepository;
        this.escrowPaymentProvider = escrowPaymentProvider;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Booking confirmCompletion(UUID bookingId, UUID requesterId) {
        // Pessimistic lock: serializes concurrent completions on the same booking so exactly one
        // request performs the ESCROW_HELD -> COMPLETED transition and exactly one escrow release.
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Booking not found"));
        if (!booking.isParty(requesterId)) {
            throw new ResponseStatusException(FORBIDDEN, "Not a party to this booking");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            return booking;
        }
        if (booking.getStatus() != BookingStatus.ESCROW_HELD) {
            throw new ResponseStatusException(CONFLICT, "Booking is not awaiting completion (status: " + booking.getStatus() + ")");
        }

        boolean isStudent = booking.getStudentUserId().equals(requesterId);
        if (isStudent) {
            if (booking.getStudentConfirmedAt() == null) {
                booking.setStudentConfirmedAt(Instant.now());
            }
        } else if (booking.getTutorConfirmedAt() == null) {
            booking.setTutorConfirmedAt(Instant.now());
        }

        if (booking.getStudentConfirmedAt() != null && booking.getTutorConfirmedAt() != null) {
            booking.setStatus(BookingStatus.COMPLETED);
            releaseEscrow(booking);
            notificationService.create(booking.getStudentUserId(), "BOOKING_COMPLETED", Map.of("bookingId", bookingId.toString()));
            notificationService.create(booking.getTutorUserId(), "BOOKING_COMPLETED", Map.of("bookingId", bookingId.toString()));
            auditLogService.record(requesterId, "BOOKING_COMPLETED", "bookings", bookingId, Map.of());
        }

        return bookingRepository.save(booking);
    }

    private void releaseEscrow(Booking booking) {
        EscrowTransaction escrowTransaction = escrowTransactionRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new IllegalStateException("Escrow transaction missing for booking " + booking.getId()));
        escrowPaymentProvider.release(escrowTransaction.getCmiReference());
        escrowTransaction.setStatus(EscrowStatus.RELEASED);
        escrowTransaction.setReleasedAt(Instant.now());
        escrowTransactionRepository.save(escrowTransaction);
    }
}
