package ma.darsma.backend.review;

import ma.darsma.backend.booking.Booking;
import ma.darsma.backend.booking.BookingRepository;
import ma.darsma.backend.booking.BookingStatus;
import ma.darsma.backend.notification.event.ReviewSubmittedEvent;
import ma.darsma.backend.profile.TutorProfileService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final TutorProfileService tutorProfileService;
    private final ApplicationEventPublisher eventPublisher;

    public ReviewService(ReviewRepository reviewRepository, BookingRepository bookingRepository,
                          TutorProfileService tutorProfileService, ApplicationEventPublisher eventPublisher) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.tutorProfileService = tutorProfileService;
        this.eventPublisher = eventPublisher;
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

        eventPublisher.publishEvent(new ReviewSubmittedEvent(revieweeId, bookingId));

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
