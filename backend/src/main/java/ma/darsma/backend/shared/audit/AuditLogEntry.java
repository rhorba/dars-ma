package ma.darsma.backend.shared.audit;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_log_entries")
public class AuditLogEntry {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(nullable = false)
    private String action;

    @Column(name = "target_type", nullable = false)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = Map.of();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AuditLogEntry() {
    }

    private AuditLogEntry(Builder b) {
        this.actorId = b.actorId;
        this.action = b.action;
        this.targetType = b.targetType;
        this.targetId = b.targetId;
        this.metadata = b.metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() { return id; }
    public UUID getActorId() { return actorId; }
    public String getAction() { return action; }
    public String getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
    public Map<String, Object> getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }

    public static final class Builder {
        private UUID actorId;
        private String action;
        private String targetType;
        private UUID targetId;
        private Map<String, Object> metadata = Map.of();

        public Builder actorId(UUID actorId) { this.actorId = actorId; return this; }
        public Builder action(String action) { this.action = action; return this; }
        public Builder targetType(String targetType) { this.targetType = targetType; return this; }
        public Builder targetId(UUID targetId) { this.targetId = targetId; return this; }
        public Builder metadata(Map<String, Object> metadata) { this.metadata = metadata; return this; }

        public AuditLogEntry build() { return new AuditLogEntry(this); }
    }
}
