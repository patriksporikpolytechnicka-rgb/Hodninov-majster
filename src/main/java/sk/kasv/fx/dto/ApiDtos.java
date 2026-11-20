package sk.kasv.fx.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import sk.kasv.fx.entity.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class ApiDtos {
    private ApiDtos() {}

    public record RegisterRequest(@NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 10, max = 72) String password, @NotNull Role role) {}
    /** code: 6-digit authenticator code, needed only when the account has two-factor login on. */
    public record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password, @Pattern(regexp = "\\d{6}") String code) {
        public LoginRequest(String email, String password) { this(email, password, null); }
    }
    public record UserResponse(Long id, String email, Role role, boolean twoFactorEnabled, boolean emailVerified) {}
    public record VerifyEmailRequest(@NotBlank @Size(max = 200) String token) {}
    public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user, String refreshToken) {}
    public record RefreshRequest(@NotBlank @Size(max = 200) String refreshToken) {}
    public record PasswordResetRequest(@NotBlank @Email @Size(max = 254) String email) {}
    public record PasswordResetConfirm(@NotBlank @Size(max = 200) String token,
            @NotBlank @Size(min = 10, max = 72) String newPassword) {}
    public record CategoryRequest(@NotBlank @Size(max = 100) String name) {}
    public record CategoryResponse(Long id, String name) {}
    public record ProviderRequest(@NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 1000) String description,
            @NotBlank @Size(max = 150) String location,
            @NotBlank @Size(max = 40) String phone,
            @Size(max = 255) String profileImage, @NotNull Boolean availability) {}
    /** rating: average of reviews rounded to one decimal, null when there are no reviews yet. */
    public record ProviderResponse(Long id, String name, String description, String location,
            String phone, String profileImage, boolean availability, Double rating, int reviewCount) {}
    public record ServiceRequest(@NotNull @Positive Long categoryId,
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 2000) String description,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
            @NotNull PriceUnit priceUnit, @NotNull Boolean active) {}
    public record ServiceResponse(Long id, ProviderResponse provider, CategoryResponse category,
            String name, String description, BigDecimal price, PriceUnit priceUnit, boolean active) {}
    public record OrderRequest(@NotNull @Positive Long serviceId,
            @NotNull LocalDateTime scheduledAt,
            @NotNull @DecimalMin("0.01") @Digits(integer = 6, fraction = 2) BigDecimal quantity,
            @NotBlank @Size(max = 300) String address, @Size(max = 2000) String note) {}
    public record StatusRequest(@NotNull OrderStatus status) {}
    public record OrderResponse(Long id, Long customerId, Long providerId, Long serviceId,
            String serviceName, LocalDateTime scheduledAt, OrderStatus status,
            BigDecimal unitPrice, PriceUnit priceUnit, BigDecimal quantity, BigDecimal totalPrice,
            String address, String note, LocalDateTime createdAt) {}
    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <T> PageResponse<T> from(org.springframework.data.domain.Page<T> page) {
            return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages());
        }
    }
    public record ReviewRequest(@NotNull @Min(1) @Max(5) Integer rating, @Size(max = 1000) String comment) {}
    public record ReviewResponse(Long id, Long orderId, Long providerId, String serviceName,
            int rating, String comment, LocalDateTime createdAt) {}
    public record RatingResponse(Long providerId, Double average, long count) {}
    /**
     * disableTwoFactor: true turns off 2FA for a user who lost their phone.
     * markEmailVerified: true confirms the e-mail by hand (e.g. no mail server yet).
     */
    public record AdminUserRequest(Role role, Boolean blocked, Boolean disableTwoFactor, Boolean markEmailVerified) {
        public AdminUserRequest(Role role, Boolean blocked) { this(role, blocked, null, null); }
        public AdminUserRequest(Role role, Boolean blocked, Boolean disableTwoFactor) { this(role, blocked, disableTwoFactor, null); }
    }
    public record AdminUserResponse(Long id, String email, Role role, boolean blocked, boolean twoFactorEnabled,
            boolean emailVerified, LocalDateTime createdAt) {}
    public record TwoFactorSetupResponse(String secret, String otpauthUri) {}
    public record TwoFactorCodeRequest(@NotBlank @Pattern(regexp = "\\d{6}") String code) {}
    public record MessageRequest(@NotBlank @Size(max = 2000) String text) {}
    /** senderRole: CUSTOMER or PROVIDER, i.e. which side of the order wrote it. */
    public record MessageResponse(Long id, Long orderId, Long senderId, Role senderRole, String text, LocalDateTime createdAt) {}
    /** dayOfWeek: 1 = Monday ... 7 = Sunday; times like "08:00". An order may start from start up to (not including) end. */
    public record WorkingHoursEntry(@NotNull @Min(1) @Max(7) Integer dayOfWeek, @NotNull java.time.LocalTime start,
            @NotNull java.time.LocalTime end) {}
    public record WorkingHoursRequest(@NotNull @Size(max = 7) List<@Valid @NotNull WorkingHoursEntry> hours) {}
    public record DayOffRequest(@NotNull java.time.LocalDate date) {}
    public record AvailabilityResponse(List<WorkingHoursEntry> workingHours, List<java.time.LocalDate> daysOff) {}
    public record ErrorResponse(int status, String message, Map<String, String> fieldErrors) {}
}
