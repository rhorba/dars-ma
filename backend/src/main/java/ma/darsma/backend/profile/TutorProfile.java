package ma.darsma.backend.profile;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tutor_profiles")
public class TutorProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]", nullable = false)
    private String[] subjects = new String[0];

    @Column(name = "hourly_rate_mad", nullable = false)
    private BigDecimal hourlyRateMad;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "avg_rating")
    private BigDecimal avgRating;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected TutorProfile() {
    }

    private TutorProfile(Builder b) {
        this.userId = b.userId;
        this.bio = b.bio;
        this.subjects = b.subjects;
        this.hourlyRateMad = b.hourlyRateMad;
        this.verificationStatus = b.verificationStatus;
        this.avgRating = b.avgRating;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getUserId() { return userId; }
    public String getBio() { return bio; }
    public String[] getSubjects() { return subjects; }
    public BigDecimal getHourlyRateMad() { return hourlyRateMad; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public BigDecimal getAvgRating() { return avgRating; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setBio(String bio) { this.bio = bio; }
    public void setSubjects(String[] subjects) { this.subjects = subjects; }
    public void setHourlyRateMad(BigDecimal hourlyRateMad) { this.hourlyRateMad = hourlyRateMad; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }
    public void setAvgRating(BigDecimal avgRating) { this.avgRating = avgRating; }

    public static final class Builder {
        private UUID userId;
        private String bio;
        private String[] subjects = new String[0];
        private BigDecimal hourlyRateMad;
        private VerificationStatus verificationStatus = VerificationStatus.PENDING;
        private BigDecimal avgRating;

        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder bio(String bio) { this.bio = bio; return this; }
        public Builder subjects(String[] subjects) { this.subjects = subjects; return this; }
        public Builder hourlyRateMad(BigDecimal hourlyRateMad) { this.hourlyRateMad = hourlyRateMad; return this; }
        public Builder verificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; return this; }
        public Builder avgRating(BigDecimal avgRating) { this.avgRating = avgRating; return this; }

        public TutorProfile build() { return new TutorProfile(this); }
    }
}
