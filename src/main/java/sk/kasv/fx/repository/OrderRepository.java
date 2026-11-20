package sk.kasv.fx.repository;

import sk.kasv.fx.entity.Order;
import sk.kasv.fx.entity.OrderStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);
    Page<Order> findByProviderUserId(Long userId, Pageable pageable);
    boolean existsByServiceId(Long serviceId);
    boolean existsByServiceIdAndStatusIn(Long serviceId, Collection<OrderStatus> statuses);
    boolean existsByCustomerId(Long customerId);
    boolean existsByCustomerIdAndStatusIn(Long customerId, Collection<OrderStatus> statuses);
    boolean existsByProviderUserIdAndStatusIn(Long userId, Collection<OrderStatus> statuses);
    List<Order> findAllByCustomerIdOrderByIdAsc(Long customerId);
    List<Order> findAllByProviderUserIdOrderByIdAsc(Long userId);
    /** Account deletion: the customer's address and note are personal data. */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Order o set o.address = :address, o.note = null where o.customer.id = :customerId")
    int anonymizeCustomer(@org.springframework.data.repository.query.Param("customerId") Long customerId, @org.springframework.data.repository.query.Param("address") String address);

    @org.springframework.data.jpa.repository.Query("select o.status, count(o) from Order o where o.provider.id = :providerId group by o.status")
    List<Object[]> countByStatus(@org.springframework.data.repository.query.Param("providerId") Long providerId);
    /** [count, sum of totalPrice] of completed orders scheduled in [from, to). */
    @org.springframework.data.jpa.repository.Query("select count(o), coalesce(sum(o.totalPrice), 0) from Order o where o.provider.id = :providerId"
            + " and o.status = sk.kasv.fx.entity.OrderStatus.COMPLETED and o.scheduledAt >= :from and o.scheduledAt < :to")
    List<Object[]> completedBetween(@org.springframework.data.repository.query.Param("providerId") Long providerId,
                                    @org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
                                    @org.springframework.data.repository.query.Param("to") java.time.LocalDateTime to);
    long countByProviderIdAndStatusInAndScheduledAtGreaterThanEqual(Long providerId, Collection<OrderStatus> statuses,
                                                                     java.time.LocalDateTime from);
}
