package sk.kasv.fx.dto;

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
    public record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password) {}
    public record UserResponse(Long id, String email, Role role) {}
    public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {}
    public record CategoryRequest(@NotBlank @Size(max = 100) String name) {}
    public record CategoryResponse(Long id, String name) {}
    public record ProviderRequest(@NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 1000) String description,
            @NotBlank @Size(max = 150) String location,
            @NotBlank @Size(max = 40) String phone,
            @Size(max = 255) String profileImage, @NotNull Boolean availability) {}
    public record ProviderResponse(Long id, String name, String description, String location,
            String phone, String profileImage, boolean availability) {}
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
    public record ErrorResponse(int status, String message, Map<String, String> fieldErrors) {}
}
