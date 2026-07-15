package ma.darsma.backend.booking;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "gig_request_id", nullable = false, updatable = false)
    private UUID gigRequestId;

    @Column(name = "student_user_id", nullable = false, updatable = false)
    private UUID studentUserId;

    @Column(name = "tutor_user_id", nullable = false, updatable = false)
    private UUID tutorUserId;

    @Column(name = "agreed_price_mad", nullable = false)
    private BigDecimal agreedPriceMad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING_PAYMENT;

    @Column(name = "student_confirmed_at")
    private Instant studentConfirmedAt;

    @Column(name = "tutor_confirmed_at")
    private Instant tutorConfirmedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Booking() {
    }

    private Booking(Builder b) {
        this.gigRequestId = b.gigRequestId;
        this.studentUserId = b.studentUserId;
        this.tutorUserId = b.tutorUserId;
        this.agreedPriceMad = b.agreedPriceMad;
        this.status = b.status;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() { return id; }
    public UUID getGigRequestId() { return gigRequestId; }
    public UUID getStudentUserId() { return studentUserId; }
    public UUID getTutorUserId() { return tutorUserId; }
    public BigDecimal getAgreedPriceMad() { return agreedPriceMad; }
    public BookingStatus getStatus() { return status; }
    public Instant getStudentConfirmedAt() { return studentConfirmedAt; }
    public Instant getTutorConfirmedAt() { return tutorConfirmedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setStatus(BookingStatus status) { this.status = status; }
    public void setStudentConfirmedAt(Instant studentConfirmedAt) { this.studentConfirmedAt = studentConfirmedAt; }
    public void setTutorConfirmedAt(Instant tutorConfirmedAt) { this.tutorConfirmedAt = tutorConfirmedAt; }

    public boolean isParty(UUID userId) {
        return studentUserId.equals(userId) || tutorUserId.equals(userId);
    }

    public static final class Builder {
        private UUID gigRequestId;
        private UUID studentUserId;
        private UUID tutorUserId;
        private BigDecimal agreedPriceMad;
        private BookingStatus status = BookingStatus.PENDING_PAYMENT;

        public Builder gigRequestId(UUID gigRequestId) { this.gigRequestId = gigRequestId; return this; }
        public Builder studentUserId(UUID studentUserId) { this.studentUserId = studentUserId; return this; }
        public Builder tutorUserId(UUID tutorUserId) { this.tutorUserId = tutorUserId; return this; }
        public Builder agreedPriceMad(BigDecimal agreedPriceMad) { this.agreedPriceMad = agreedPriceMad; return this; }
        public Builder status(BookingStatus status) { this.status = status; return this; }

        public Booking build() { return new Booking(this); }
    }
}
