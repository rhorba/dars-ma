package ma.darsma.backend.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByBookingId(UUID bookingId);

    boolean existsByBookingIdAndReviewerId(UUID bookingId, UUID reviewerId);

    @Query(value = """
            SELECT AVG(r.rating) FROM reviews r
            JOIN bookings b ON b.id = r.booking_id
            WHERE b.tutor_user_id = :tutorUserId AND r.reviewer_id = b.student_user_id
            """, nativeQuery = true)
    BigDecimal avgRatingForTutor(@Param("tutorUserId") UUID tutorUserId);
}
