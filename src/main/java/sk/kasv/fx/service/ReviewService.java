package sk.kasv.fx.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import sk.kasv.fx.dto.ApiMapper;
import sk.kasv.fx.entity.*;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.OrderRepository;
import sk.kasv.fx.repository.ProviderProfileRepository;
import sk.kasv.fx.repository.ReviewRepository;
import sk.kasv.fx.security.CurrentUser;
import java.time.Clock;
import java.time.LocalDateTime;
import static sk.kasv.fx.dto.ApiDtos.*;

@org.springframework.stereotype.Service
@Transactional(readOnly = true)
public class ReviewService {
    private final ReviewRepository reviews;
    private final OrderRepository orders;
    private final ProviderProfileRepository providers;
    private final CurrentUser current;
    private final Clock clock;
    public ReviewService(ReviewRepository reviews, OrderRepository orders, ProviderProfileRepository providers,
                         CurrentUser current, Clock clock) {
        this.reviews = reviews; this.orders = orders; this.providers = providers; this.current = current; this.clock = clock;
    }

    /** Only the customer of a completed order can review it, and only once. */
    @Transactional
    public ReviewResponse create(Long orderId, ReviewRequest request) {
        User user = current.require();
        if (user.getRole() != Role.CUSTOMER) throw new ApiException(403, "Only customers can review orders");
        Order order = findOrder(orderId);
        if (!order.getCustomer().getId().equals(user.getId())) throw new ApiException(403, "Order belongs to another user");
        if (order.getStatus() != OrderStatus.COMPLETED) throw new ApiException(409, "Only completed orders can be reviewed");
        if (reviews.existsByOrderId(orderId)) throw new ApiException(409, "Order already reviewed");
        Review review = new Review();
        review.setOrder(order); review.setProvider(order.getProvider()); review.setCustomer(user);
        review.setRating(request.rating());
        String comment = request.comment() == null ? null : request.comment().strip();
        review.setComment(comment == null || comment.isEmpty() ? null : comment);
        review.setCreatedAt(LocalDateTime.now(clock));
        review = reviews.saveAndFlush(review);
        ProviderProfile provider = order.getProvider();
        Double average = reviews.averageRating(provider.getId());
        provider.setRating(average == null ? 0 : average, (int) reviews.countByProviderId(provider.getId()));
        providers.save(provider);
        return ApiMapper.review(review);
    }

    /** The review of one order, for its customer, provider or an admin. */
    public ReviewResponse forOrder(Long orderId) {
        Order order = findOrder(orderId); User user = current.require();
        if (user.getRole() != Role.ADMIN && !order.getCustomer().getId().equals(user.getId())
                && !order.getProvider().getUser().getId().equals(user.getId())) throw new ApiException(403, "Order belongs to another user");
        return reviews.findByOrderId(orderId).map(ApiMapper::review).orElseThrow(() -> new ApiException(404, "Review not found"));
    }

    public PageResponse<ReviewResponse> forProvider(Long providerId, int page, int size) {
        requireProvider(providerId);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return PageResponse.from(reviews.findByProviderId(providerId, pageable).map(ApiMapper::review));
    }

    /** Average rounded to one decimal; null when the provider has no reviews yet. */
    public RatingResponse rating(Long providerId) {
        requireProvider(providerId);
        long count = reviews.countByProviderId(providerId);
        Double average = count == 0 ? null : reviews.averageRating(providerId);
        return new RatingResponse(providerId, average == null ? null : Math.round(average * 10) / 10.0, count);
    }

    private Order findOrder(Long id) { return orders.findById(id).orElseThrow(() -> new ApiException(404, "Order not found")); }
    private void requireProvider(Long id) { if (!providers.existsById(id)) throw new ApiException(404, "Provider not found"); }
}
