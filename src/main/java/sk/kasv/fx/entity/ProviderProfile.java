package sk.kasv.fx.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "provider_profiles")
public class ProviderProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String name;

    @Column(length = 1000)
    private String description;

    private String location;

    private String phone;

    private String profileImage;

    private boolean availability;
    private java.time.Instant subscriptionStart;
    private java.time.Instant subscriptionEnd;
    private boolean subscriptionPaid;

    /** Average of all reviews (0 when there are none) and their number; updated with every new review. */
    @Column(nullable = false)
    private double ratingAverage;

    @Column(nullable = false)
    private int ratingCount;

    public double getRatingAverage() { return ratingAverage; }
    public int getRatingCount() { return ratingCount; }
    public void setRating(double average, int count) { this.ratingAverage = average; this.ratingCount = count; }
    public java.time.Instant getSubscriptionStart() { return subscriptionStart; }
    public java.time.Instant getSubscriptionEnd() { return subscriptionEnd; }
    public boolean isSubscriptionPaid() { return subscriptionPaid; }
    public void startTrial(java.time.Instant now) {
        if (subscriptionStart != null) return;
        subscriptionStart = now;
        subscriptionEnd = now.plus(java.time.Duration.ofDays(30));
        subscriptionPaid = false;
    }
    public boolean hasSubscription(java.time.Instant now) {
        return subscriptionStart != null && !now.isBefore(subscriptionStart)
                && subscriptionEnd != null && now.isBefore(subscriptionEnd);
    }
    public void activatePaidMonth(java.time.Instant now) {
        subscriptionStart = now;
        subscriptionEnd = now.atZone(java.time.ZoneId.of("Europe/Bratislava")).plusMonths(1).toInstant();
        subscriptionPaid = true;
    }

    public ProviderProfile() {
    }

    public ProviderProfile(User user, String name, String description,
                           String location, String phone,
                           String profileImage, boolean availability) {
        this.user = user;
        this.name = name;
        this.description = description;
        this.location = location;
        this.phone = phone;
        this.profileImage = profileImage;
        this.availability = availability;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public boolean isAvailability() {
        return availability;
    }

    public void setAvailability(boolean availability) {
        this.availability = availability;
    }
}