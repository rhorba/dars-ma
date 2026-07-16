package ma.darsma.backend.review;

import ma.darsma.backend.booking.Booking;
import ma.darsma.backend.booking.BookingRepository;
import ma.darsma.backend.booking.BookingStatus;
import ma.darsma.backend.notification.NotificationService;
import ma.darsma.backend.profile.TutorProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final TutorProfileService tutorProfileService;
    private final NotificationService notificationService;

    public ReviewService(ReviewRepository reviewRepository, BookingRepository bookingRepository,
                          TutorProfileService tutorProfileService, NotificationService notificationService) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.tutorProfileService = tutorProfileService;
        this.notificationService = notificationService;
    }

    @Transactional
    public Review submit(UUID bookingId, UUID reviewerId, short rating, String comment) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Booking not found"));
        if (!booking.isParty(reviewerId)) {
            throw new ResponseStatusException(FORBIDDEN, "Not a party to this booking");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new ResponseStatusException(CONFLICT, "Booking is not completed");
        }
        if (reviewRepository.existsByBookingIdAndReviewerId(bookingId, reviewerId)) {
            throw new ResponseStatusException(CONFLICT, "You have already reviewed this booking");
        }

        UUID revieweeId = reviewerId.equals(booking.getStudentUserId())
                ? booking.getTutorUserId()
                : booking.getStudentUserId();

        Review saved = reviewRepository.save(Review.builder()
                .bookingId(bookingId)
                .reviewerId(reviewerId)
                .rating(rating)
                .comment(comment)
                .build());

        if (revieweeId.equals(booking.getTutorUserId())) {
            tutorProfileService.updateAvgRating(booking.getTutorUserId(),
                    reviewRepository.avgRatingForTutor(booking.getTutorUserId()));
        }

        notificationService.create(revieweeId, "REVIEW_RECEIVED", Map.of("bookingId", bookingId.toString()));

        return saved;
    }

    public List<Review> getForBooking(UUID bookingId, UUID requesterId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Booking not found"));
        if (!booking.isParty(requesterId)) {
            throw new ResponseStatusException(FORBIDDEN, "Not a party to this booking");
        }
        return reviewRepository.findByBookingId(bookingId);
    }
}
