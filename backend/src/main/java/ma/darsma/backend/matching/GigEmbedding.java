package ma.darsma.backend.matching;

import jakarta.persistence.*;
import ma.darsma.backend.shared.persistence.VectorType;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gig_embeddings")
public class GigEmbedding {

    @Id
    @Column(name = "gig_request_id")
    private UUID gigRequestId;

    @Type(VectorType.class)
    @Column(columnDefinition = "vector(384)", nullable = false)
    private float[] embedding;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected GigEmbedding() {
    }

    public GigEmbedding(UUID gigRequestId, float[] embedding) {
        this.gigRequestId = gigRequestId;
        this.embedding = embedding;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getGigRequestId() { return gigRequestId; }
    public float[] getEmbedding() { return embedding; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setEmbedding(float[] embedding) { this.embedding = embedding; }
}
