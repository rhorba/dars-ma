package ma.darsma.backend.messaging;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "message_threads")
public class MessageThread {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "booking_id", updatable = false)
    private UUID bookingId;

    @Column(name = "gig_request_id", updatable = false)
    private UUID gigRequestId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected MessageThread() {
    }

    private MessageThread(Builder b) {
        this.bookingId = b.bookingId;
        this.gigRequestId = b.gigRequestId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() { return id; }
    public UUID getBookingId() { return bookingId; }
    public UUID getGigRequestId() { return gigRequestId; }
    public Instant getCreatedAt() { return createdAt; }

    public static final class Builder {
        private UUID bookingId;
        private UUID gigRequestId;

        public Builder bookingId(UUID bookingId) { this.bookingId = bookingId; return this; }
        public Builder gigRequestId(UUID gigRequestId) { this.gigRequestId = gigRequestId; return this; }

        public MessageThread build() { return new MessageThread(this); }
    }
}
