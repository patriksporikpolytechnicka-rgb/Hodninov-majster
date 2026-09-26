package sk.kasv.fx.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import sk.kasv.fx.dto.ApiMapper;
import sk.kasv.fx.entity.*;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.OrderRepository;
import sk.kasv.fx.repository.ServiceRepository;
import sk.kasv.fx.security.CurrentUser;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import static sk.kasv.fx.dto.ApiDtos.*;

@org.springframework.stereotype.Service
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orders;
    private final ServiceRepository services;
    private final CurrentUser current;
    private final Clock clock;
    public OrderService(OrderRepository orders, ServiceRepository services, CurrentUser current, Clock clock) {
        this.orders = orders; this.services = services; this.current = current; this.clock = clock;
    }
    @Transactional
    public OrderResponse create(OrderRequest request) {
        User customer = current.require();
        if (customer.getRole() != Role.CUSTOMER) throw new ApiException(403, "Only customers can place orders");
        if (!request.scheduledAt().isAfter(LocalDateTime.now(clock))) throw new ApiException(400, "scheduledAt must be in the future (Europe/Bratislava)");
        Service service = services.findById(request.serviceId()).orElseThrow(() -> new ApiException(404, "Service not found"));
        if (!service.isActive() || !service.getProvider().isAvailability()) throw new ApiException(409, "Service or provider is unavailable");
        if (service.getPriceUnit() == PriceUnit.JOB && request.quantity().stripTrailingZeros().scale() > 0)
            throw new ApiException(400, "JOB quantity must be a whole number");
        Order order = new Order();
        order.setCustomer(customer); order.setProvider(service.getProvider()); order.setService(service);
        order.setServiceName(service.getName()); order.setUnitPrice(service.getPrice()); order.setPriceUnit(service.getPriceUnit());
        order.setQuantity(request.quantity());
        order.setTotalPrice(service.getPrice().multiply(request.quantity()).setScale(2, RoundingMode.HALF_UP));
        order.setScheduledAt(request.scheduledAt()); order.setStatus(OrderStatus.PENDING);
        order.setAddress(request.address().strip()); order.setNote(request.note()); order.setCreatedAt(LocalDateTime.now(clock));
        return ApiMapper.order(orders.saveAndFlush(order));
    }
    public PageResponse<OrderResponse> mine(int page, int size) {
        User user = current.require();
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        var result = switch (user.getRole()) {
            case CUSTOMER -> orders.findByCustomerId(user.getId(), pageable);
            case PROVIDER -> orders.findByProviderUserId(user.getId(), pageable);
            case ADMIN -> orders.findAll(pageable);
        };
        return PageResponse.from(result.map(ApiMapper::order));
    }
    public OrderResponse get(Long id) {
        Order order = find(id); checkParticipant(order, current.require());
        return ApiMapper.order(order);
    }
    @Transactional
    public OrderResponse changeStatus(Long id, StatusRequest request) {
        Order order = find(id); User user = current.require(); checkParticipant(order, user);
        boolean provider = user.getRole() == Role.PROVIDER && order.getProvider().getUser().getId().equals(user.getId());
        boolean customer = user.getRole() == Role.CUSTOMER && order.getCustomer().getId().equals(user.getId());
        if (!provider && !customer) throw new ApiException(403, "Only the customer or provider can change status");
        OrderStatus target = request.status();
        boolean allowed = switch (order.getStatus()) {
            case PENDING -> (provider && (target == OrderStatus.ACCEPTED || target == OrderStatus.REJECTED))
                    || (customer && target == OrderStatus.CANCELLED);
            case ACCEPTED -> (provider && target == OrderStatus.IN_PROGRESS) || target == OrderStatus.CANCELLED;
            case IN_PROGRESS -> provider && target == OrderStatus.COMPLETED;
            default -> false;
        };
        if (!allowed) throw new ApiException(409, "Status transition not allowed");
        order.setStatus(target);
        return ApiMapper.order(orders.saveAndFlush(order));
    }
    private Order find(Long id) { return orders.findById(id).orElseThrow(() -> new ApiException(404, "Order not found")); }
    private void checkParticipant(Order order, User user) {
        if (user.getRole() != Role.ADMIN && !order.getCustomer().getId().equals(user.getId())
                && !order.getProvider().getUser().getId().equals(user.getId())) throw new ApiException(403, "Order belongs to another user");
    }
}
