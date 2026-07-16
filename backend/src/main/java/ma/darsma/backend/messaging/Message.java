package ma.darsma.backend.messaging;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "thread_id", nullable = false, updatable = false)
    private UUID threadId;

    @Column(name = "sender_id", nullable = false, updatable = false)
    private UUID senderId;

    @Column(columnDefinition = "TEXT", nullable = false, updatable = false)
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Message() {
    }

    private Message(Builder b) {
        this.threadId = b.threadId;
        this.senderId = b.senderId;
        this.body = b.body;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() { return id; }
    public UUID getThreadId() { return threadId; }
    public UUID getSenderId() { return senderId; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }

    public static final class Builder {
        private UUID threadId;
        private UUID senderId;
        private String body;

        public Builder threadId(UUID threadId) { this.threadId = threadId; return this; }
        public Builder senderId(UUID senderId) { this.senderId = senderId; return this; }
        public Builder body(String body) { this.body = body; return this; }

        public Message build() { return new Message(this); }
    }
}
