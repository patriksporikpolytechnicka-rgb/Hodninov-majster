package sk.kasv.fx.service;

import org.springframework.transaction.annotation.Transactional;
import sk.kasv.fx.dto.ApiMapper;
import sk.kasv.fx.entity.*;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.OrderRepository;
import sk.kasv.fx.repository.ProviderProfileRepository;
import sk.kasv.fx.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import static sk.kasv.fx.dto.ApiDtos.*;

@org.springframework.stereotype.Service
@Transactional(readOnly = true)
public class ProviderStatsService {
    private final OrderRepository orders;
    private final ProviderProfileRepository providers;
    private final CurrentUser current;
    private final Clock clock;
    public ProviderStatsService(OrderRepository orders, ProviderProfileRepository providers, CurrentUser current, Clock clock) {
        this.orders = orders; this.providers = providers; this.current = current; this.clock = clock;
    }

    public ProviderStatsResponse mine() {
        User user = current.require();
        if (user.getRole() != Role.PROVIDER) throw new ApiException(403, "Role PROVIDER required");
        ProviderProfile provider = providers.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(404, "Create your provider profile first"));
        Long id = provider.getId();
        Map<OrderStatus, Long> byStatus = new EnumMap<>(OrderStatus.class);
        for (OrderStatus s : OrderStatus.values()) byStatus.put(s, 0L);
        for (Object[] row : orders.countByStatus(id)) byStatus.put((OrderStatus) row[0], (Long) row[1]);
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime monthStart = LocalDate.now(clock).withDayOfMonth(1).atStartOfDay();
        Object[] month = single(orders.completedBetween(id, monthStart, monthStart.plusMonths(1)));
        Object[] all = single(orders.completedBetween(id, LocalDateTime.of(1970, 1, 1, 0, 0), LocalDateTime.of(9999, 1, 1, 0, 0)));
        long upcoming = orders.countByProviderIdAndStatusInAndScheduledAtGreaterThanEqual(id,
                List.of(OrderStatus.ACCEPTED, OrderStatus.IN_PROGRESS), now);
        ProviderResponse p = ApiMapper.provider(provider);
        return new ProviderStatsResponse(total, byStatus, upcoming, (Long) month[0], money(month[1]),
                (Long) all[0], money(all[1]), p.rating(), p.reviewCount());
    }
    private static Object[] single(List<Object[]> rows) { return rows.isEmpty() ? new Object[]{0L, BigDecimal.ZERO} : rows.get(0); }
    private static BigDecimal money(Object value) {
        return (value instanceof BigDecimal b ? b : new BigDecimal(value.toString())).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
