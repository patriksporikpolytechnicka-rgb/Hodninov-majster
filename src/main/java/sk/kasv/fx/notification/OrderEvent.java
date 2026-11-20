package sk.kasv.fx.notification;

import sk.kasv.fx.entity.Order;
import sk.kasv.fx.entity.OrderStatus;
import sk.kasv.fx.entity.Role;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Something happened to an order. Carries plain values (no entities), because it is handled
 * after the transaction commits, when lazy relations can no longer be loaded.
 * changedBy: CUSTOMER or PROVIDER who caused the change; null for a new order.
 */
public record OrderEvent(Long orderId, OrderStatus status, Role changedBy, String customerEmail, String providerEmail,
                         String serviceName, LocalDateTime scheduledAt, BigDecimal totalPrice, String address, String note) {
    public static OrderEvent of(Order order, Role changedBy) {
        return new OrderEvent(order.getId(), order.getStatus(), changedBy, order.getCustomer().getEmail(),
                order.getProvider().getUser().getEmail(), order.getServiceName(), order.getScheduledAt(),
                order.getTotalPrice(), order.getAddress(), order.getNote());
    }
}
