package sk.kasv.fx.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sk.kasv.fx.entity.*;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.*;
import sk.kasv.fx.security.CurrentUser;
import java.time.*;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static sk.kasv.fx.dto.ApiDtos.*;

class ReviewServiceTest {
    ReviewRepository reviews = mock(ReviewRepository.class);
    OrderRepository orders = mock(OrderRepository.class);
    ProviderProfileRepository providers = mock(ProviderProfileRepository.class);
    CurrentUser current = mock(CurrentUser.class);
    Clock clock = Clock.fixed(Instant.parse("2026-11-19T10:00:00Z"), ZoneId.of("Europe/Bratislava"));
    ReviewService subject = new ReviewService(reviews, orders, providers, current, clock);
    User customer = new User("customer@example.invalid", "hash", Role.CUSTOMER);
    User otherCustomer = new User("other@example.invalid", "hash", Role.CUSTOMER);
    User providerUser = new User("provider@example.invalid", "hash", Role.PROVIDER);
    ProviderProfile provider = new ProviderProfile();
    Order order = new Order();

    @BeforeEach void setup() {
        customer.setId(1L); otherCustomer.setId(9L); providerUser.setId(2L);
        provider.setId(3L); provider.setUser(providerUser);
        order.setId(5L); order.setCustomer(customer); order.setProvider(provider);
        order.setServiceName("Assembly"); order.setStatus(OrderStatus.COMPLETED);
        when(orders.findById(5L)).thenReturn(Optional.of(order));
        when(current.require()).thenReturn(customer);
        when(reviews.saveAndFlush(any())).thenAnswer(i -> { Review r = i.getArgument(0); r.setId(7L); return r; });
        when(providers.existsById(3L)).thenReturn(true);
    }

    @Test void customerReviewsCompletedOrder() {
        var response = subject.create(5L, new ReviewRequest(5, "  Great job  "));
        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.comment()).isEqualTo("Great job");
        assertThat(response.providerId()).isEqualTo(3L);
        assertThat(response.serviceName()).isEqualTo("Assembly");
    }
    @Test void newReviewUpdatesHandymansRating() {
        when(reviews.averageRating(3L)).thenReturn(4.5);
        when(reviews.countByProviderId(3L)).thenReturn(2L);
        subject.create(5L, new ReviewRequest(4, null));
        assertThat(provider.getRatingAverage()).isEqualTo(4.5);
        assertThat(provider.getRatingCount()).isEqualTo(2);
        verify(providers).save(provider);
    }
    @Test void blankCommentIsStoredAsNull() {
        assertThat(subject.create(5L, new ReviewRequest(4, "   ")).comment()).isNull();
    }
    @Test void onlyCompletedOrdersCanBeReviewed() {
        order.setStatus(OrderStatus.IN_PROGRESS);
        assertThatThrownBy(() -> subject.create(5L, new ReviewRequest(5, null)))
                .isInstanceOf(ApiException.class).hasMessageContaining("completed");
        verify(reviews, never()).saveAndFlush(any());
    }
    @Test void orderCanBeReviewedOnlyOnce() {
        when(reviews.existsByOrderId(5L)).thenReturn(true);
        assertThatThrownBy(() -> subject.create(5L, new ReviewRequest(5, null)))
                .isInstanceOf(ApiException.class).hasMessageContaining("already reviewed");
    }
    @Test void onlyTheOrdersCustomerCanReview() {
        when(current.require()).thenReturn(otherCustomer);
        assertThatThrownBy(() -> subject.create(5L, new ReviewRequest(5, null))).isInstanceOf(ApiException.class);
        when(current.require()).thenReturn(providerUser);
        assertThatThrownBy(() -> subject.create(5L, new ReviewRequest(5, null)))
                .isInstanceOf(ApiException.class).hasMessageContaining("Only customers");
        verify(reviews, never()).saveAndFlush(any());
    }
    @Test void ratingIsRoundedAndEmptyWithoutReviews() {
        when(reviews.countByProviderId(3L)).thenReturn(3L);
        when(reviews.averageRating(3L)).thenReturn(4.333333);
        assertThat(subject.rating(3L)).isEqualTo(new RatingResponse(3L, 4.3, 3));
        when(reviews.countByProviderId(3L)).thenReturn(0L);
        assertThat(subject.rating(3L)).isEqualTo(new RatingResponse(3L, null, 0));
    }
    @Test void unknownProviderIsNotFound() {
        assertThatThrownBy(() -> subject.rating(99L)).isInstanceOf(ApiException.class).hasMessageContaining("Provider not found");
    }
}
