package ma.darsma.backend.gig;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gig_requests")
public class GigRequest {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "student_user_id", nullable = false, updatable = false)
    private UUID studentUserId;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String level;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "budget_min_mad")
    private BigDecimal budgetMinMad;

    @Column(name = "budget_max_mad")
    private BigDecimal budgetMaxMad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GigStatus status = GigStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected GigRequest() {
    }

    private GigRequest(Builder b) {
        this.studentUserId = b.studentUserId;
        this.subject = b.subject;
        this.level = b.level;
        this.description = b.description;
        this.budgetMinMad = b.budgetMinMad;
        this.budgetMaxMad = b.budgetMaxMad;
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
    public UUID getStudentUserId() { return studentUserId; }
    public String getSubject() { return subject; }
    public String getLevel() { return level; }
    public String getDescription() { return description; }
    public BigDecimal getBudgetMinMad() { return budgetMinMad; }
    public BigDecimal getBudgetMaxMad() { return budgetMaxMad; }
    public GigStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setStatus(GigStatus status) { this.status = status; }

    public static final class Builder {
        private UUID studentUserId;
        private String subject;
        private String level;
        private String description;
        private BigDecimal budgetMinMad;
        private BigDecimal budgetMaxMad;
        private GigStatus status = GigStatus.OPEN;

        public Builder studentUserId(UUID studentUserId) { this.studentUserId = studentUserId; return this; }
        public Builder subject(String subject) { this.subject = subject; return this; }
        public Builder level(String level) { this.level = level; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder budgetMinMad(BigDecimal budgetMinMad) { this.budgetMinMad = budgetMinMad; return this; }
        public Builder budgetMaxMad(BigDecimal budgetMaxMad) { this.budgetMaxMad = budgetMaxMad; return this; }
        public Builder status(GigStatus status) { this.status = status; return this; }

        public GigRequest build() { return new GigRequest(this); }
    }
}
