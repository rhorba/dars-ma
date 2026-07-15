package ma.darsma.backend.booking;

import ma.darsma.backend.booking.dto.BookingCreateRequest;
import ma.darsma.backend.gig.GigRequest;
import ma.darsma.backend.gig.GigRequestService;
import ma.darsma.backend.gig.GigStatus;
import ma.darsma.backend.notification.NotificationService;
import ma.darsma.backend.profile.TutorProfile;
import ma.darsma.backend.profile.TutorProfileService;
import ma.darsma.backend.profile.VerificationGuard;
import ma.darsma.backend.profile.VerificationStatus;
import ma.darsma.backend.shared.audit.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookingServiceTest {

    private BookingRepository bookingRepository;
    private EscrowTransactionRepository escrowTransactionRepository;
    private EscrowPaymentProvider escrowPaymentProvider;
    private GigRequestService gigRequestService;
    private TutorProfileService tutorProfileService;
    private VerificationGuard verificationGuard;
    private NotificationService notificationService;
    private AuditLogService auditLogService;
    private BookingService bookingService;

    private final UUID studentId = UUID.randomUUID();
    private final UUID tutorId = UUID.randomUUID();
    private final UUID gigId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        escrowTransactionRepository = mock(EscrowTransactionRepository.class);
        escrowPaymentProvider = mock(EscrowPaymentProvider.class);
        gigRequestService = mock(GigRequestService.class);
        tutorProfileService = mock(TutorProfileService.class);
        verificationGuard = new VerificationGuard();
        notificationService = mock(NotificationService.class);
        auditLogService = mock(AuditLogService.class);
        bookingService = new BookingService(bookingRepository, escrowTransactionRepository, escrowPaymentProvider,
                gigRequestService, tutorProfileService, verificationGuard, notificationService, auditLogService);
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

    private GigRequest openGig() {
        return GigRequest.builder().studentUserId(studentId).subject("Math").level("HS").description("desc")
                .status(GigStatus.OPEN).build();
    }

    private TutorProfile verifiedTutor(BigDecimal rate) {
        return TutorProfile.builder().userId(tutorId).subjects(new String[]{"Math"})
                .hourlyRateMad(rate).verificationStatus(VerificationStatus.VERIFIED).build();
    }

    @Test
    void create_happyPath_holdsEscrowAndMarksGigMatched() {
        GigRequest gig = openGig();
        when(gigRequestService.getForOwner(gigId, studentId)).thenReturn(gig);
        when(tutorProfileService.getByUserId(tutorId)).thenReturn(verifiedTutor(new BigDecimal("100.00")));
        when(bookingRepository.findByGigRequestId(gigId)).thenReturn(Optional.empty());
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            setId(b, UUID.randomUUID());
            return b;
        });
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(escrowPaymentProvider.hold(any(), eq(new BigDecimal("200.00"))))
                .thenReturn(EscrowHoldResult.success("mock-ref"));

        BookingCreateRequest request = new BookingCreateRequest(gigId, tutorId, new BigDecimal("2"));
        Booking result = bookingService.create(studentId, request);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.ESCROW_HELD);
        assertThat(result.getAgreedPriceMad()).isEqualByComparingTo("200.00");
        verify(escrowTransactionRepository).save(argThat(tx ->
                tx.getStatus() == EscrowStatus.HELD && tx.getCmiReference().equals("mock-ref")));
        verify(gigRequestService).markMatched(gig);
        verify(notificationService).create(eq(tutorId), eq("BOOKING_CREATED"), any());
        verify(auditLogService).record(eq(studentId), eq("BOOKING_CREATED"), eq("bookings"), any(), any());
    }

    @Test
    void create_paymentFails_leavesPendingPaymentAndNoEscrowRow() {
        GigRequest gig = openGig();
        when(gigRequestService.getForOwner(gigId, studentId)).thenReturn(gig);
        when(tutorProfileService.getByUserId(tutorId)).thenReturn(verifiedTutor(MockEscrowPaymentProvider.FAILURE_TRIGGER_AMOUNT_MAD));
        when(bookingRepository.findByGigRequestId(gigId)).thenReturn(Optional.empty());
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(escrowPaymentProvider.hold(any(), any())).thenReturn(EscrowHoldResult.failure());

        BookingCreateRequest request = new BookingCreateRequest(gigId, tutorId, BigDecimal.ONE);
        Booking result = bookingService.create(studentId, request);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        verify(escrowTransactionRepository, never()).save(any());
        verify(gigRequestService, never()).markMatched(any());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void create_gigNotOpen_throwsConflict() {
        GigRequest gig = GigRequest.builder().studentUserId(studentId).subject("Math").level("HS").description("d")
                .status(GigStatus.MATCHED).build();
        when(gigRequestService.getForOwner(gigId, studentId)).thenReturn(gig);

        BookingCreateRequest request = new BookingCreateRequest(gigId, tutorId, BigDecimal.ONE);

        assertThatThrownBy(() -> bookingService.create(studentId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no longer open");
        verifyNoInteractions(tutorProfileService, escrowPaymentProvider);
    }

    @Test
    void create_unverifiedTutor_throwsForbidden() {
        when(gigRequestService.getForOwner(gigId, studentId)).thenReturn(openGig());
        TutorProfile pending = TutorProfile.builder().userId(tutorId).subjects(new String[]{"Math"})
                .hourlyRateMad(new BigDecimal("100.00")).verificationStatus(VerificationStatus.PENDING).build();
        when(tutorProfileService.getByUserId(tutorId)).thenReturn(pending);

        BookingCreateRequest request = new BookingCreateRequest(gigId, tutorId, BigDecimal.ONE);

        assertThatThrownBy(() -> bookingService.create(studentId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("VERIFIED");
        verifyNoInteractions(escrowPaymentProvider);
    }

    @Test
    void create_bookingAlreadyExistsForGig_throwsConflict() {
        when(gigRequestService.getForOwner(gigId, studentId)).thenReturn(openGig());
        when(tutorProfileService.getByUserId(tutorId)).thenReturn(verifiedTutor(new BigDecimal("100.00")));
        when(bookingRepository.findByGigRequestId(gigId)).thenReturn(Optional.of(Booking.builder().build()));

        BookingCreateRequest request = new BookingCreateRequest(gigId, tutorId, BigDecimal.ONE);

        assertThatThrownBy(() -> bookingService.create(studentId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");
        verifyNoInteractions(escrowPaymentProvider);
    }

    @Test
    void create_concurrentDuplicateInsert_isCaughtAndConvertedToConflict() {
        when(gigRequestService.getForOwner(gigId, studentId)).thenReturn(openGig());
        when(tutorProfileService.getByUserId(tutorId)).thenReturn(verifiedTutor(new BigDecimal("100.00")));
        when(bookingRepository.findByGigRequestId(gigId)).thenReturn(Optional.empty());
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenThrow(new DataIntegrityViolationException("dup"));

        BookingCreateRequest request = new BookingCreateRequest(gigId, tutorId, BigDecimal.ONE);

        assertThatThrownBy(() -> bookingService.create(studentId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");
        verifyNoInteractions(escrowPaymentProvider);
    }

    @Test
    void getForParty_throwsNotFoundWhenMissing() {
        UUID bookingId = UUID.randomUUID();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getForParty(bookingId, studentId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void getForParty_throwsForbiddenForNonParty() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder().studentUserId(studentId).tutorUserId(tutorId)
                .gigRequestId(gigId).agreedPriceMad(new BigDecimal("100.00")).build();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.getForParty(bookingId, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not a party");
    }

    @Test
    void getForParty_succeedsForStudentOrTutor() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder().studentUserId(studentId).tutorUserId(tutorId)
                .gigRequestId(gigId).agreedPriceMad(new BigDecimal("100.00")).build();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThat(bookingService.getForParty(bookingId, studentId)).isEqualTo(booking);
        assertThat(bookingService.getForParty(bookingId, tutorId)).isEqualTo(booking);
    }
}
