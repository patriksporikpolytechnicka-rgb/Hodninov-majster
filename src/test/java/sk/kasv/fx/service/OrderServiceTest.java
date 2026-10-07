package sk.kasv.fx.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sk.kasv.fx.entity.*;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.*;
import sk.kasv.fx.security.CurrentUser;
import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static sk.kasv.fx.dto.ApiDtos.*;

class OrderServiceTest {
    OrderRepository orders = mock(OrderRepository.class);
    ServiceRepository services = mock(ServiceRepository.class);
    CurrentUser current = mock(CurrentUser.class);
    Clock clock = Clock.fixed(Instant.parse("2026-09-26T10:00:00Z"), ZoneId.of("Europe/Bratislava"));
    OrderService subject = new OrderService(orders, services, current, clock);
    User customer = new User("customer@example.invalid", "hash", Role.CUSTOMER);
    User providerUser = new User("provider@example.invalid", "hash", Role.PROVIDER);
    ProviderProfile provider = new ProviderProfile();
    Service service = new Service();
    Order order = new Order();
    @BeforeEach void setup() { provider.startTrial(clock.instant());
        customer.setId(1L); providerUser.setId(2L); provider.setId(3L); provider.setUser(providerUser); provider.setAvailability(true);
        service.setId(4L); service.setProvider(provider); service.setActive(true); service.setName("Assembly");
        service.setPrice(new BigDecimal("25.50")); service.setPriceUnit(PriceUnit.HOUR);
        when(current.require()).thenReturn(customer);
        when(services.findById(4L)).thenReturn(Optional.of(service));
        when(orders.saveAndFlush(any())).thenAnswer(i -> { Order o = i.getArgument(0); o.setId(5L); return o; });
        order.setId(5L); order.setCustomer(customer); order.setProvider(provider); order.setService(service); order.setStatus(OrderStatus.PENDING);
        when(orders.findById(5L)).thenReturn(Optional.of(order));
    }
    OrderRequest request(String quantity) {
        return new OrderRequest(4L, LocalDateTime.now(clock).plusDays(1), new BigDecimal(quantity), "Test address", null);
    }
    @Test void snapshotsPriceAndComputesTotal() {
        var response = subject.create(request("1.25"));
        assertThat(response.totalPrice()).isEqualByComparingTo("31.88");
        assertThat(response.unitPrice()).isEqualByComparingTo("25.50");
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.customerId()).isEqualTo(1L);
        service.setPrice(new BigDecimal("99.00"));
        assertThat(response.unitPrice()).isEqualByComparingTo("25.50");
    }
    @Test void expiredSubscriptionBlocksNewOrdersAndAcceptanceButAllowsCompletion() {
        Clock expiredClock = Clock.offset(clock, Duration.ofDays(30));
        OrderService expired = new OrderService(orders, services, current, expiredClock);
        var future = new OrderRequest(4L, LocalDateTime.now(expiredClock).plusDays(1), BigDecimal.ONE, "Address", null);
        assertThatThrownBy(() -> expired.create(future)).isInstanceOf(ApiException.class).hasMessageContaining("subscription expired");
        when(current.require()).thenReturn(providerUser);
        assertThatThrownBy(() -> expired.changeStatus(5L, new StatusRequest(OrderStatus.ACCEPTED)))
                .isInstanceOf(ApiException.class).hasMessageContaining("subscription expired");
        order.setStatus(OrderStatus.IN_PROGRESS);
        assertThat(expired.changeStatus(5L, new StatusRequest(OrderStatus.COMPLETED)).status()).isEqualTo(OrderStatus.COMPLETED);
    }
    @Test void refusesUnavailableProvider() {
        provider.setAvailability(false);
        assertThatThrownBy(() -> subject.create(request("1"))).isInstanceOf(ApiException.class).hasMessageContaining("unavailable");
        verify(orders, never()).saveAndFlush(any());
    }
    @Test void refusesFractionalJobs() {
        service.setPriceUnit(PriceUnit.JOB);
        assertThatThrownBy(() -> subject.create(request("1.5"))).isInstanceOf(ApiException.class).hasMessageContaining("whole number");
    }
    @Test void refusesPastAppointment() {
        var r = new OrderRequest(4L, LocalDateTime.now(clock).minusMinutes(1), BigDecimal.ONE, "Address", null);
        assertThatThrownBy(() -> subject.create(r)).isInstanceOf(ApiException.class).hasMessageContaining("future");
    }
    @Test void strangerCannotReadOrChangeOrder() {
        User stranger = new User("stranger@example.invalid", "hash", Role.CUSTOMER); stranger.setId(99L);
        when(current.require()).thenReturn(stranger);
        assertThatThrownBy(() -> subject.get(5L)).isInstanceOf(ApiException.class).hasMessageContaining("another user");
        assertThatThrownBy(() -> subject.changeStatus(5L, new StatusRequest(OrderStatus.CANCELLED))).isInstanceOf(ApiException.class);
        verify(orders, never()).saveAndFlush(any());
    }
    @Test void customerCannotAcceptOwnOrder() {
        assertThatThrownBy(() -> subject.changeStatus(5L, new StatusRequest(OrderStatus.ACCEPTED)))
                .isInstanceOf(ApiException.class).hasMessageContaining("transition");
    }
    @Test void providerCanAcceptStartAndCompleteButCannotReopen() {
        when(current.require()).thenReturn(providerUser);
        for (OrderStatus target : new OrderStatus[]{OrderStatus.ACCEPTED, OrderStatus.IN_PROGRESS, OrderStatus.COMPLETED})
            assertThat(subject.changeStatus(5L, new StatusRequest(target)).status()).isEqualTo(target);
        assertThatThrownBy(() -> subject.changeStatus(5L, new StatusRequest(OrderStatus.ACCEPTED))).isInstanceOf(ApiException.class);
    }
    @Test void customerCanCancelPendingOrder() {
        assertThat(subject.changeStatus(5L, new StatusRequest(OrderStatus.CANCELLED)).status()).isEqualTo(OrderStatus.CANCELLED);
    }
    @Test void providerCannotPlaceOrder() {
        when(current.require()).thenReturn(providerUser);
        assertThatThrownBy(() -> subject.create(request("1"))).isInstanceOf(ApiException.class).hasMessageContaining("Only customers");
    }
}
