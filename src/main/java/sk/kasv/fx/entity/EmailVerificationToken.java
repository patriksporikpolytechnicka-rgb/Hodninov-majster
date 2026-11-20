package sk.kasv.fx.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** One-time e-mail verification token. Only its SHA-256 hash is stored. */
@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant usedAt;

    public EmailVerificationToken() {}
    public EmailVerificationToken(User user, String tokenHash, Instant createdAt, Instant expiresAt) {
        this.user = user; this.tokenHash = tokenHash; this.createdAt = createdAt; this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }
}
