package ma.darsma.backend.profile;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_documents")
public class VerificationDocument {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tutor_user_id", nullable = false)
    private UUID tutorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false)
    private DocType docType;

    @Column(name = "encrypted_blob", nullable = false)
    private byte[] encryptedBlob;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected VerificationDocument() {
    }

    private VerificationDocument(Builder b) {
        this.id = b.id;
        this.tutorUserId = b.tutorUserId;
        this.docType = b.docType;
        this.encryptedBlob = b.encryptedBlob;
        this.mimeType = b.mimeType;
        this.originalFilename = b.originalFilename;
        this.reviewedBy = b.reviewedBy;
        this.reviewedAt = b.reviewedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() { return id; }
    public UUID getTutorUserId() { return tutorUserId; }
    public DocType getDocType() { return docType; }
    public byte[] getEncryptedBlob() { return encryptedBlob; }
    public String getMimeType() { return mimeType; }
    public String getOriginalFilename() { return originalFilename; }
    public UUID getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void setReviewedBy(UUID reviewedBy) { this.reviewedBy = reviewedBy; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }

    public static final class Builder {
        private UUID id;
        private UUID tutorUserId;
        private DocType docType;
        private byte[] encryptedBlob;
        private String mimeType;
        private String originalFilename;
        private UUID reviewedBy;
        private Instant reviewedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder tutorUserId(UUID tutorUserId) { this.tutorUserId = tutorUserId; return this; }
        public Builder docType(DocType docType) { this.docType = docType; return this; }
        public Builder encryptedBlob(byte[] encryptedBlob) { this.encryptedBlob = encryptedBlob; return this; }
        public Builder mimeType(String mimeType) { this.mimeType = mimeType; return this; }
        public Builder originalFilename(String originalFilename) { this.originalFilename = originalFilename; return this; }
        public Builder reviewedBy(UUID reviewedBy) { this.reviewedBy = reviewedBy; return this; }
        public Builder reviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; return this; }

        public VerificationDocument build() { return new VerificationDocument(this); }
    }
}
