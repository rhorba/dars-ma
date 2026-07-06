package ma.darsma.backend.notification;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload = Map.of();

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Notification() {
    }

    private Notification(Builder b) {
        this.userId = b.userId;
        this.type = b.type;
        this.payload = b.payload;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getType() { return type; }
    public Map<String, Object> getPayload() { return payload; }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }

    public static final class Builder {
        private UUID userId;
        private String type;
        private Map<String, Object> payload = Map.of();

        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder payload(Map<String, Object> payload) { this.payload = payload; return this; }

        public Notification build() { return new Notification(this); }
    }
}
