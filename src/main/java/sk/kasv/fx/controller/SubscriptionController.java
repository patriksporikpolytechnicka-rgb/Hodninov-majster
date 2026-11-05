package sk.kasv.fx.controller;
import org.springframework.web.bind.annotation.*;
import sk.kasv.fx.service.SubscriptionService;
import sk.kasv.fx.service.SubscriptionService.SubscriptionResponse;
@RestController
@RequestMapping("/api/providers/me/subscription")
public class SubscriptionController {
    private final SubscriptionService subscriptions;
    public SubscriptionController(SubscriptionService subscriptions) { this.subscriptions = subscriptions; }
    @GetMapping public SubscriptionResponse get() { return subscriptions.mine(); }
    @PostMapping("/demo-payment") public SubscriptionResponse pay() { return subscriptions.demoPay(); }
}
