package sk.kasv.fx.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** Profile photo of a handyman, stored in the database (pods in Kubernetes have no lasting disk). */
@Entity
@Table(name = "provider_photos")
public class ProviderPhoto {
    @Id
    private Long providerId;

    @Column(nullable = false, length = 30)
    private String contentType;

    // bytea on PostgreSQL and on the H2 demo database (PostgreSQL mode); Hibernate would otherwise pick BLOB.
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARBINARY)
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] data;

    @Column(nullable = false)
    private Instant updatedAt;

    public ProviderPhoto() {}
    public ProviderPhoto(Long providerId, String contentType, byte[] data, Instant updatedAt) {
        this.providerId = providerId; this.contentType = contentType; this.data = data; this.updatedAt = updatedAt;
    }
    public Long getProviderId() { return providerId; }
    public String getContentType() { return contentType; }
    public byte[] getData() { return data; }
    public Instant getUpdatedAt() { return updatedAt; }
}
