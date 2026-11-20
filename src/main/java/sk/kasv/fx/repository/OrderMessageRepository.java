package sk.kasv.fx.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sk.kasv.fx.entity.OrderMessage;
import java.util.List;

public interface OrderMessageRepository extends JpaRepository<OrderMessage, Long> {
    /** Messages newer than afterId, oldest first, at most 100 (the client asks again with the last id). */
    List<OrderMessage> findTop100ByOrderIdAndIdGreaterThanOrderByIdAsc(Long orderId, Long afterId);
}
