package ma.darsma.backend.matching;

import jakarta.persistence.*;
import ma.darsma.backend.shared.persistence.VectorType;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tutor_embeddings")
public class TutorEmbedding {

    @Id
    @Column(name = "tutor_user_id")
    private UUID tutorUserId;

    @Type(VectorType.class)
    @Column(columnDefinition = "vector(384)", nullable = false)
    private float[] embedding;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected TutorEmbedding() {
    }

    public TutorEmbedding(UUID tutorUserId, float[] embedding) {
        this.tutorUserId = tutorUserId;
        this.embedding = embedding;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getTutorUserId() { return tutorUserId; }
    public float[] getEmbedding() { return embedding; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setEmbedding(float[] embedding) { this.embedding = embedding; }
}
