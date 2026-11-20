package sk.kasv.fx.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** Who did what to whom: admin and security actions, kept for the administrator. */
@Entity
@Table(name = "audit_log")
public class AuditEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long actorId;

    /** E-mail at the time of the action, so the entry stays readable if the account changes later. */
    @Column(length = 254)
    private String actorEmail;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 30)
    private String targetType;

    private Long targetId;

    @Column(length = 500)
    private String details;

    @Column(nullable = false)
    private Instant createdAt;

    public AuditEntry() {}
    public AuditEntry(Long actorId, String actorEmail, String action, String targetType, Long targetId, String details, Instant createdAt) {
        this.actorId = actorId; this.actorEmail = actorEmail; this.action = action; this.targetType = targetType;
        this.targetId = targetId; this.details = details; this.createdAt = createdAt;
    }
    public Long getId() { return id; }
    public Long getActorId() { return actorId; }
    public String getActorEmail() { return actorEmail; }
    public String getAction() { return action; }
    public String getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public String getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
