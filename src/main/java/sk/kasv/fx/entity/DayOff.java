package sk.kasv.fx.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/** A whole day when the handyman cannot be booked (holiday, sick day...). */
@Entity
@Table(name = "provider_days_off", uniqueConstraints = @UniqueConstraint(columnNames = {"provider_id", "off_day"}))
public class DayOff {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderProfile provider;

    // "day" is a reserved word in H2 (demo database).
    @Column(name = "off_day", nullable = false)
    private LocalDate day;

    public DayOff() {}
    public DayOff(ProviderProfile provider, LocalDate day) { this.provider = provider; this.day = day; }
    public Long getId() { return id; }
    public ProviderProfile getProvider() { return provider; }
    public LocalDate getDay() { return day; }
}
