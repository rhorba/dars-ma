package ma.darsma.backend.booking;

import ma.darsma.backend.notification.NotificationService;
import ma.darsma.backend.shared.audit.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class BookingDisputeService {

    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public BookingDisputeService(BookingRepository bookingRepository,
                                  NotificationService notificationService,
                                  AuditLogService auditLogService) {
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Booking raise(UUID bookingId, UUID requesterId) {
        // Same lock as completion: a dispute and a mutual-completion race on the same booking
        // must resolve to exactly one outcome, not both.
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Booking not found"));
        if (!booking.isParty(requesterId)) {
            throw new ResponseStatusException(FORBIDDEN, "Not a party to this booking");
        }
        if (booking.getStatus() == BookingStatus.DISPUTED) {
            return booking;
        }
        if (booking.getStatus() != BookingStatus.ESCROW_HELD) {
            throw new ResponseStatusException(CONFLICT, "Booking is not eligible for dispute (status: " + booking.getStatus() + ")");
        }

        booking.setStatus(BookingStatus.DISPUTED);
        booking = bookingRepository.save(booking);

        UUID otherParty = booking.getStudentUserId().equals(requesterId)
                ? booking.getTutorUserId()
                : booking.getStudentUserId();
        notificationService.create(otherParty, "BOOKING_DISPUTED", Map.of("bookingId", bookingId.toString()));
        auditLogService.record(requesterId, "BOOKING_DISPUTED", "bookings", bookingId, Map.of());

        return booking;
    }
}
