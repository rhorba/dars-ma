package ma.darsma.backend.booking;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "escrow_transactions")
public class EscrowTransaction {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "booking_id", nullable = false, updatable = false)
    private UUID bookingId;

    @Column(name = "cmi_reference")
    private String cmiReference;

    @Column(name = "amount_mad", nullable = false, updatable = false)
    private BigDecimal amountMad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EscrowStatus status;

    @Column(name = "held_at")
    private Instant heldAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected EscrowTransaction() {
    }

    private EscrowTransaction(Builder b) {
        this.bookingId = b.bookingId;
        this.cmiReference = b.cmiReference;
        this.amountMad = b.amountMad;
        this.status = b.status;
        this.heldAt = b.heldAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() { return id; }
    public UUID getBookingId() { return bookingId; }
    public String getCmiReference() { return cmiReference; }
    public BigDecimal getAmountMad() { return amountMad; }
    public EscrowStatus getStatus() { return status; }
    public Instant getHeldAt() { return heldAt; }
    public Instant getReleasedAt() { return releasedAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(EscrowStatus status) { this.status = status; }
    public void setReleasedAt(Instant releasedAt) { this.releasedAt = releasedAt; }

    public static final class Builder {
        private UUID bookingId;
        private String cmiReference;
        private BigDecimal amountMad;
        private EscrowStatus status;
        private Instant heldAt;

        public Builder bookingId(UUID bookingId) { this.bookingId = bookingId; return this; }
        public Builder cmiReference(String cmiReference) { this.cmiReference = cmiReference; return this; }
        public Builder amountMad(BigDecimal amountMad) { this.amountMad = amountMad; return this; }
        public Builder status(EscrowStatus status) { this.status = status; return this; }
        public Builder heldAt(Instant heldAt) { this.heldAt = heldAt; return this; }

        public EscrowTransaction build() { return new EscrowTransaction(this); }
    }
}
