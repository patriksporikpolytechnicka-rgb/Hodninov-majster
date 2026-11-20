package sk.kasv.fx.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sk.kasv.fx.entity.OrderMessage;
import java.util.List;

public interface OrderMessageRepository extends JpaRepository<OrderMessage, Long> {
    /** Messages newer than afterId, oldest first, at most 100 (the client asks again with the last id). */
    List<OrderMessage> findTop100ByOrderIdAndIdGreaterThanOrderByIdAsc(Long orderId, Long afterId);
    List<OrderMessage> findAllBySenderIdOrderByIdAsc(Long senderId);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update OrderMessage m set m.text = :text where m.sender.id = :senderId")
    int replaceTexts(@org.springframework.data.repository.query.Param("senderId") Long senderId, @org.springframework.data.repository.query.Param("text") String text);
}
