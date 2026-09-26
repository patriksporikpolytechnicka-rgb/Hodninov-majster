package sk.kasv.fx.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import sk.kasv.fx.service.OrderService;
import static sk.kasv.fx.dto.ApiDtos.*;

@RestController
@Validated
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orders;
    public OrderController(OrderService orders) { this.orders = orders; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody OrderRequest request) { return orders.create(request); }
    @GetMapping("/my")
    public PageResponse<OrderResponse> mine(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) { return orders.mine(page, size); }
    @GetMapping("/{id}") public OrderResponse get(@PathVariable @Positive Long id) { return orders.get(id); }
    @PatchMapping("/{id}/status")
    public OrderResponse status(@PathVariable @Positive Long id, @Valid @RequestBody StatusRequest request) {
        return orders.changeStatus(id, request);
    }
}
