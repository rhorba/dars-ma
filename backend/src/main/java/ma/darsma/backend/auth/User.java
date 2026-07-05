package ma.darsma.backend.auth;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String phone;

    @Column(name = "preferred_lang", nullable = false)
    private String preferredLang = "fr";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected User() {
    }

    private User(Builder b) {
        this.id = b.id;
        this.email = b.email;
        this.passwordHash = b.passwordHash;
        this.role = b.role;
        this.fullName = b.fullName;
        this.phone = b.phone;
        this.preferredLang = b.preferredLang;
        this.active = b.active;
        this.failedLoginAttempts = b.failedLoginAttempts;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getPreferredLang() { return preferredLang; }
    public boolean isActive() { return active; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }
    public void setPreferredLang(String preferredLang) { this.preferredLang = preferredLang; }
    public void setActive(boolean active) { this.active = active; }

    public static final class Builder {
        private UUID id;
        private String email;
        private String passwordHash;
        private Role role;
        private String fullName;
        private String phone;
        private String preferredLang = "fr";
        private boolean active = true;
        private int failedLoginAttempts = 0;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public Builder role(Role role) { this.role = role; return this; }
        public Builder fullName(String fullName) { this.fullName = fullName; return this; }
        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder preferredLang(String preferredLang) { this.preferredLang = preferredLang; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        public Builder failedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; return this; }

        public User build() { return new User(this); }
    }
}
