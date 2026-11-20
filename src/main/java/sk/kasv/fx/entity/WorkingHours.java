package sk.kasv.fx.entity;

import jakarta.persistence.*;
import java.time.LocalTime;

/** When a handyman works on one weekday (1 = Monday ... 7 = Sunday). */
@Entity
@Table(name = "provider_working_hours", uniqueConstraints = @UniqueConstraint(columnNames = {"provider_id", "day_of_week"}))
public class WorkingHours {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderProfile provider;

    @Column(nullable = false)
    private int dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    public WorkingHours() {}
    public WorkingHours(ProviderProfile provider, int dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.provider = provider; this.dayOfWeek = dayOfWeek; this.startTime = startTime; this.endTime = endTime;
    }
    public Long getId() { return id; }
    public ProviderProfile getProvider() { return provider; }
    public int getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
}
