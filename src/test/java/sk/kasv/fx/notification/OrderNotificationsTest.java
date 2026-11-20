package sk.kasv.fx.notification;

import org.junit.jupiter.api.Test;
import sk.kasv.fx.entity.OrderStatus;
import sk.kasv.fx.entity.Role;
import sk.kasv.fx.service.MailService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderNotificationsTest {
    OrderEvent event(OrderStatus status, Role by) {
        return new OrderEvent(5L, status, by, "customer@x.sk", "provider@x.sk", "Montáž",
                LocalDateTime.of(2026, 12, 1, 9, 30), new BigDecimal("51.00"), "Hlavná 1, Košice", "Zvoňte dvakrát");
    }
    @Test void newOrderGoesToHandyman() {
        var m = OrderNotifications.message(event(OrderStatus.PENDING, null));
        assertThat(m.to()).isEqualTo("provider@x.sk");
        assertThat(m.text()).contains("1. 12. 2026 o 09:30").contains("Hlavná 1, Košice").contains("Zvoňte dvakrát").contains("51.00 €");
    }
    @Test void statusChangesGoToCustomer() {
        for (var s : new OrderStatus[]{OrderStatus.ACCEPTED, OrderStatus.REJECTED, OrderStatus.IN_PROGRESS, OrderStatus.COMPLETED})
            assertThat(OrderNotifications.message(event(s, Role.PROVIDER)).to()).isEqualTo("customer@x.sk");
    }
    @Test void cancellationGoesToTheOtherSide() {
        assertThat(OrderNotifications.message(event(OrderStatus.CANCELLED, Role.CUSTOMER)).to()).isEqualTo("provider@x.sk");
        assertThat(OrderNotifications.message(event(OrderStatus.CANCELLED, Role.PROVIDER)).to()).isEqualTo("customer@x.sk");
    }
    @Test void mailContainsLinkToOrder() {
        MailService mail = mock(MailService.class);
        new OrderNotifications(mail, "https://hm.example").onOrderEvent(event(OrderStatus.ACCEPTED, Role.PROVIDER));
        verify(mail).send(eq("customer@x.sk"), contains("#5"), contains("https://hm.example/objednavky/5"));
    }
}
