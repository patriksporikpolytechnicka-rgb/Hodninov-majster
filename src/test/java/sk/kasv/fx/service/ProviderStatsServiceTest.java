package sk.kasv.fx.service;

import org.junit.jupiter.api.Test;
import sk.kasv.fx.entity.*;
import sk.kasv.fx.repository.*;
import sk.kasv.fx.security.CurrentUser;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProviderStatsServiceTest {
    OrderRepository orders = mock(OrderRepository.class);
    ProviderProfileRepository providers = mock(ProviderProfileRepository.class);
    CurrentUser current = mock(CurrentUser.class);
    Clock clock = Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneId.of("Europe/Bratislava"));
    ProviderStatsService subject = new ProviderStatsService(orders, providers, current, clock);

    @Test void summarizesOrdersAndEarnings() {
        User u = new User("p@x.sk", "h", Role.PROVIDER); u.setId(2L);
        ProviderProfile p = new ProviderProfile(); p.setId(3L); p.setUser(u); p.setRating(4.25, 4);
        when(current.require()).thenReturn(u);
        when(providers.findByUserId(2L)).thenReturn(Optional.of(p));
        when(orders.countByStatus(3L)).thenReturn(List.of(new Object[]{OrderStatus.COMPLETED, 3L}, new Object[]{OrderStatus.PENDING, 1L}));
        LocalDateTime nov = LocalDateTime.of(2026, 11, 1, 0, 0);
        when(orders.completedBetween(3L, nov, nov.plusMonths(1))).thenReturn(List.<Object[]>of(new Object[]{2L, new BigDecimal("80")}));
        when(orders.completedBetween(eq(3L), eq(LocalDateTime.of(1970, 1, 1, 0, 0)), any())).thenReturn(List.<Object[]>of(new Object[]{3L, new BigDecimal("120.5")}));
        var stats = subject.mine();
        assertThat(stats.ordersTotal()).isEqualTo(4);
        assertThat(stats.ordersByStatus()).containsEntry(OrderStatus.CANCELLED, 0L).containsEntry(OrderStatus.COMPLETED, 3L);
        assertThat(stats.earningsThisMonth()).isEqualByComparingTo("80.00");
        assertThat(stats.earningsTotal()).isEqualByComparingTo("120.50");
        assertThat(stats.rating()).isEqualTo(4.3);
    }
}
