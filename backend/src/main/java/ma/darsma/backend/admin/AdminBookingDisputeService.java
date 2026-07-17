package ma.darsma.backend.admin;

import ma.darsma.backend.booking.Booking;
import ma.darsma.backend.booking.BookingRepository;
import ma.darsma.backend.booking.BookingStatus;
import ma.darsma.backend.booking.DisputeResolution;
import ma.darsma.backend.booking.EscrowPaymentProvider;
import ma.darsma.backend.booking.EscrowStatus;
import ma.darsma.backend.booking.EscrowTransaction;
import ma.darsma.backend.booking.EscrowTransactionRepository;
import ma.darsma.backend.notification.NotificationService;
import ma.darsma.backend.notification.event.BookingCompletedEvent;
import ma.darsma.backend.notification.event.EscrowReleasedEvent;
import ma.darsma.backend.shared.audit.AuditLogService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AdminBookingDisputeService {

    private final BookingRepository bookingRepository;
    private final EscrowTransactionRepository escrowTransactionRepository;
    private final EscrowPaymentProvider escrowPaymentProvider;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogService auditLogService;

    public AdminBookingDisputeService(BookingRepository bookingRepository,
                                       EscrowTransactionRepository escrowTransactionRepository,
                                       EscrowPaymentProvider escrowPaymentProvider,
                                       NotificationService notificationService,
                                       ApplicationEventPublisher eventPublisher,
                                       AuditLogService auditLogService) {
        this.bookingRepository = bookingRepository;
        this.escrowTransactionRepository = escrowTransactionRepository;
        this.escrowPaymentProvider = escrowPaymentProvider;
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
        this.auditLogService = auditLogService;
    }

    public List<Booking> disputedQueue() {
        return bookingRepository.findByStatus(BookingStatus.DISPUTED);
    }

    @Transactional
    public Booking resolve(UUID bookingId, UUID adminId, DisputeResolution resolution) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Booking not found"));
        if (booking.getStatus() != BookingStatus.DISPUTED) {
            throw new ResponseStatusException(CONFLICT, "Booking is not under dispute (status: " + booking.getStatus() + ")");
        }
        EscrowTransaction escrowTransaction = escrowTransactionRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalStateException("Escrow transaction missing for booking " + bookingId));

        if (resolution == DisputeResolution.RELEASE) {
            escrowPaymentProvider.release(escrowTransaction.getCmiReference());
            escrowTransaction.setStatus(EscrowStatus.RELEASED);
            escrowTransaction.setReleasedAt(Instant.now());
            booking.setStatus(BookingStatus.COMPLETED);
            eventPublisher.publishEvent(new BookingCompletedEvent(bookingId, booking.getStudentUserId(), booking.getTutorUserId()));
            eventPublisher.publishEvent(new EscrowReleasedEvent(bookingId, booking.getStudentUserId(), booking.getTutorUserId()));
        } else {
            escrowPaymentProvider.refund(escrowTransaction.getCmiReference());
            escrowTransaction.setStatus(EscrowStatus.REFUNDED);
            booking.setStatus(BookingStatus.REFUNDED);
        }
        escrowTransactionRepository.save(escrowTransaction);
        booking = bookingRepository.save(booking);

        notificationService.create(booking.getStudentUserId(), "DISPUTE_RESOLVED",
                Map.of("bookingId", bookingId.toString(), "resolution", resolution.toString()));
        notificationService.create(booking.getTutorUserId(), "DISPUTE_RESOLVED",
                Map.of("bookingId", bookingId.toString(), "resolution", resolution.toString()));
        auditLogService.record(adminId, "DISPUTE_RESOLVED", "bookings", bookingId,
                Map.of("resolution", resolution.toString()));

        return booking;
    }
}
