package sk.kasv.fx.service;

import org.springframework.transaction.annotation.Transactional;
import sk.kasv.fx.dto.ApiMapper;
import sk.kasv.fx.entity.*;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.OrderMessageRepository;
import sk.kasv.fx.repository.OrderRepository;
import sk.kasv.fx.security.CurrentUser;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import static sk.kasv.fx.dto.ApiDtos.*;

/** Chat attached to an order. Customer and handyman write; an admin can only read. */
@org.springframework.stereotype.Service
@Transactional(readOnly = true)
public class ChatService {
    private final OrderMessageRepository messages;
    private final OrderRepository orders;
    private final CurrentUser current;
    private final Clock clock;
    public ChatService(OrderMessageRepository messages, OrderRepository orders, CurrentUser current, Clock clock) {
        this.messages = messages; this.orders = orders; this.current = current; this.clock = clock;
    }

    public List<MessageResponse> list(Long orderId, long afterId) {
        Order order = findOrder(orderId);
        User user = current.require();
        if (user.getRole() != Role.ADMIN && !isParticipant(order, user)) throw new ApiException(403, "Order belongs to another user");
        return messages.findTop100ByOrderIdAndIdGreaterThanOrderByIdAsc(orderId, afterId).stream().map(ApiMapper::message).toList();
    }

    @Transactional
    public MessageResponse send(Long orderId, MessageRequest request) {
        Order order = findOrder(orderId);
        User user = current.require();
        if (!isParticipant(order, user)) throw new ApiException(403, "Only the customer and the handyman can write");
        String text = request.text().strip();
        if (text.isEmpty()) throw new ApiException(400, "Message is empty");
        return ApiMapper.message(messages.saveAndFlush(new OrderMessage(order, user, text, LocalDateTime.now(clock))));
    }

    private Order findOrder(Long id) { return orders.findById(id).orElseThrow(() -> new ApiException(404, "Order not found")); }
    private static boolean isParticipant(Order order, User user) {
        return order.getCustomer().getId().equals(user.getId()) || order.getProvider().getUser().getId().equals(user.getId());
    }
}
