package ma.darsma.backend.review;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "booking_id", nullable = false, updatable = false)
    private UUID bookingId;

    @Column(name = "reviewer_id", nullable = false, updatable = false)
    private UUID reviewerId;

    @Column(nullable = false, updatable = false)
    private short rating;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Review() {
    }

    private Review(Builder b) {
        this.bookingId = b.bookingId;
        this.reviewerId = b.reviewerId;
        this.rating = b.rating;
        this.comment = b.comment;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() { return id; }
    public UUID getBookingId() { return bookingId; }
    public UUID getReviewerId() { return reviewerId; }
    public short getRating() { return rating; }
    public String getComment() { return comment; }
    public Instant getCreatedAt() { return createdAt; }

    public static final class Builder {
        private UUID bookingId;
        private UUID reviewerId;
        private short rating;
        private String comment;

        public Builder bookingId(UUID bookingId) { this.bookingId = bookingId; return this; }
        public Builder reviewerId(UUID reviewerId) { this.reviewerId = reviewerId; return this; }
        public Builder rating(short rating) { this.rating = rating; return this; }
        public Builder comment(String comment) { this.comment = comment; return this; }

        public Review build() { return new Review(this); }
    }
}
