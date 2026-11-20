package sk.kasv.fx.repository;

import sk.kasv.fx.entity.Order;
import sk.kasv.fx.entity.OrderStatus;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);
    Page<Order> findByProviderUserId(Long userId, Pageable pageable);
    boolean existsByServiceId(Long serviceId);
    boolean existsByServiceIdAndStatusIn(Long serviceId, Collection<OrderStatus> statuses);
    boolean existsByCustomerId(Long customerId);
}
