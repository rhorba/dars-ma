package ma.darsma.backend.review.dto;

import ma.darsma.backend.review.Review;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID bookingId,
        UUID reviewerId,
        short rating,
        String comment,
        Instant createdAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getBookingId(),
                review.getReviewerId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt());
    }
}
