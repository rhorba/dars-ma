package ma.darsma.backend.matching;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "match_suggestions")
public class MatchSuggestion {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "gig_request_id", nullable = false, updatable = false)
    private UUID gigRequestId;

    @Column(name = "tutor_user_id", nullable = false, updatable = false)
    private UUID tutorUserId;

    @Column(name = "similarity_score", nullable = false)
    private BigDecimal similarityScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected MatchSuggestion() {
    }

    public MatchSuggestion(UUID gigRequestId, UUID tutorUserId, BigDecimal similarityScore) {
        this.gigRequestId = gigRequestId;
        this.tutorUserId = tutorUserId;
        this.similarityScore = similarityScore;
    }

    public UUID getId() { return id; }
    public UUID getGigRequestId() { return gigRequestId; }
    public UUID getTutorUserId() { return tutorUserId; }
    public BigDecimal getSimilarityScore() { return similarityScore; }
    public Instant getCreatedAt() { return createdAt; }
}
