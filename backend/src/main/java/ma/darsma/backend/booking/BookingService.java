package ma.darsma.backend.booking;

import ma.darsma.backend.booking.dto.BookingCreateRequest;
import ma.darsma.backend.gig.GigRequest;
import ma.darsma.backend.gig.GigRequestService;
import ma.darsma.backend.gig.GigStatus;
import ma.darsma.backend.notification.NotificationService;
import ma.darsma.backend.profile.TutorProfile;
import ma.darsma.backend.profile.TutorProfileService;
import ma.darsma.backend.profile.VerificationGuard;
import ma.darsma.backend.shared.audit.AuditLogService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EscrowTransactionRepository escrowTransactionRepository;
    private final EscrowPaymentProvider escrowPaymentProvider;
    private final GigRequestService gigRequestService;
    private final TutorProfileService tutorProfileService;
    private final VerificationGuard verificationGuard;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public BookingService(BookingRepository bookingRepository,
                           EscrowTransactionRepository escrowTransactionRepository,
                           EscrowPaymentProvider escrowPaymentProvider,
                           GigRequestService gigRequestService,
                           TutorProfileService tutorProfileService,
                           VerificationGuard verificationGuard,
                           NotificationService notificationService,
                           AuditLogService auditLogService) {
        this.bookingRepository = bookingRepository;
        this.escrowTransactionRepository = escrowTransactionRepository;
        this.escrowPaymentProvider = escrowPaymentProvider;
        this.gigRequestService = gigRequestService;
        this.tutorProfileService = tutorProfileService;
        this.verificationGuard = verificationGuard;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Booking create(UUID studentUserId, BookingCreateRequest request) {
        GigRequest gigRequest = gigRequestService.getForOwner(request.gigRequestId(), studentUserId);
        if (gigRequest.getStatus() != GigStatus.OPEN) {
            throw new ResponseStatusException(CONFLICT, "Gig request is no longer open");
        }

        TutorProfile tutorProfile = tutorProfileService.getByUserId(request.tutorUserId());
        verificationGuard.assertVerified(tutorProfile);

        if (bookingRepository.findByGigRequestId(request.gigRequestId()).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "A booking already exists for this gig request");
        }

        BigDecimal agreedPrice = tutorProfile.getHourlyRateMad()
                .multiply(request.durationHours())
                .setScale(2, RoundingMode.HALF_UP);

        Booking booking = Booking.builder()
                .gigRequestId(gigRequest.getId())
                .studentUserId(studentUserId)
                .tutorUserId(request.tutorUserId())
                .agreedPriceMad(agreedPrice)
                .status(BookingStatus.PENDING_PAYMENT)
                .build();
        try {
            // saveAndFlush forces the INSERT (and its unique constraint check) to happen now,
            // so a race with a concurrent booking on the same gig surfaces here, not at commit time.
            booking = bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(CONFLICT, "A booking already exists for this gig request");
        }

        EscrowHoldResult holdResult = escrowPaymentProvider.hold(booking.getId(), agreedPrice);
        if (!holdResult.success()) {
            return booking;
        }

        booking.setStatus(BookingStatus.ESCROW_HELD);
        booking = bookingRepository.save(booking);

        EscrowTransaction escrowTransaction = EscrowTransaction.builder()
                .bookingId(booking.getId())
                .cmiReference(holdResult.cmiReference())
                .amountMad(agreedPrice)
                .status(EscrowStatus.HELD)
                .heldAt(Instant.now())
                .build();
        escrowTransactionRepository.save(escrowTransaction);

        gigRequestService.markMatched(gigRequest);

        notificationService.create(request.tutorUserId(), "BOOKING_CREATED",
                Map.of("bookingId", booking.getId().toString()));
        auditLogService.record(studentUserId, "BOOKING_CREATED", "bookings", booking.getId(),
                Map.of("tutorUserId", request.tutorUserId().toString(), "agreedPriceMad", agreedPrice.toString()));

        return booking;
    }

    public Booking getForParty(UUID bookingId, UUID requesterId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Booking not found"));
        if (!booking.isParty(requesterId)) {
            throw new ResponseStatusException(FORBIDDEN, "Not a party to this booking");
        }
        return booking;
    }
}
