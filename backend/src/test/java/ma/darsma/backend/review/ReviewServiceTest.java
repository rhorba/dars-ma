package ma.darsma.backend.review;

import ma.darsma.backend.booking.Booking;
import ma.darsma.backend.booking.BookingRepository;
import ma.darsma.backend.booking.BookingStatus;
import ma.darsma.backend.notification.NotificationService;
import ma.darsma.backend.profile.TutorProfileService;
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

class ReviewServiceTest {

    private ReviewRepository reviewRepository;
    private BookingRepository bookingRepository;
    private TutorProfileService tutorProfileService;
    private NotificationService notificationService;
    private ReviewService reviewService;

    private final UUID studentId = UUID.randomUUID();
    private final UUID tutorId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        reviewRepository = mock(ReviewRepository.class);
        bookingRepository = mock(BookingRepository.class);
        tutorProfileService = mock(TutorProfileService.class);
        notificationService = mock(NotificationService.class);
        reviewService = new ReviewService(reviewRepository, bookingRepository, tutorProfileService, notificationService);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Booking completedBooking() {
        return Booking.builder().studentUserId(studentId).tutorUserId(tutorId)
                .gigRequestId(UUID.randomUUID()).agreedPriceMad(new BigDecimal("200.00"))
                .status(BookingStatus.COMPLETED).build();
    }

    @Test
    void submit_byStudent_derivesTutorAsRevieweeAndUpdatesAvgRating() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(completedBooking()));
        when(reviewRepository.existsByBookingIdAndReviewerId(bookingId, studentId)).thenReturn(false);
        when(reviewRepository.avgRatingForTutor(tutorId)).thenReturn(new BigDecimal("4.50"));

        Review result = reviewService.submit(bookingId, studentId, (short) 5, "Great tutor");

        assertThat(result.getRating()).isEqualTo((short) 5);
        assertThat(result.getReviewerId()).isEqualTo(studentId);
        verify(tutorProfileService).updateAvgRating(tutorId, new BigDecimal("4.50"));
        verify(notificationService).create(eq(tutorId), eq("REVIEW_RECEIVED"), any());
    }

    @Test
    void submit_byTutor_derivesStudentAsRevieweeAndDoesNotTouchAvgRating() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(completedBooking()));
        when(reviewRepository.existsByBookingIdAndReviewerId(bookingId, tutorId)).thenReturn(false);

        reviewService.submit(bookingId, tutorId, (short) 4, null);

        verifyNoInteractions(tutorProfileService);
        verify(notificationService).create(eq(studentId), eq("REVIEW_RECEIVED"), any());
    }

    @Test
    void submit_nonParty_throwsForbidden() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(completedBooking()));

        assertThatThrownBy(() -> reviewService.submit(bookingId, UUID.randomUUID(), (short) 3, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not a party");
    }

    @Test
    void submit_bookingNotCompleted_throwsConflict() {
        Booking booking = completedBooking();
        booking.setStatus(BookingStatus.ESCROW_HELD);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> reviewService.submit(bookingId, studentId, (short) 3, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not completed");
    }

    @Test
    void submit_duplicateReview_throwsConflict() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(completedBooking()));
        when(reviewRepository.existsByBookingIdAndReviewerId(bookingId, studentId)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.submit(bookingId, studentId, (short) 3, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already reviewed");
    }

    @Test
    void submit_bookingNotFound_throwsNotFound() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.submit(bookingId, studentId, (short) 3, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void getForBooking_nonParty_throwsForbidden() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(completedBooking()));

        assertThatThrownBy(() -> reviewService.getForBooking(bookingId, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not a party");
    }
}
