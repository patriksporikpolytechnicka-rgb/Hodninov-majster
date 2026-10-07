package sk.kasv.fx.service;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sk.kasv.fx.entity.ProviderProfile;
import sk.kasv.fx.entity.Role;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.ProviderProfileRepository;
import sk.kasv.fx.security.CurrentUser;
@Service
@Transactional(readOnly = true)
public class SubscriptionService {
    private final ProviderProfileRepository providers;
    private final CurrentUser current;
    private final Clock clock;
    private final boolean demo;
    public SubscriptionService(ProviderProfileRepository providers, CurrentUser current, Clock clock,
                               @Value("${app.demo:false}") boolean demo) {
        this.providers = providers; this.current = current; this.clock = clock; this.demo = demo;
    }
    public record SubscriptionResponse(String status, Instant subscriptionStart, Instant subscriptionEnd,
                                       BigDecimal monthlyFee, String currency, boolean canReceiveOrders, boolean demoPayment) {}
    public SubscriptionResponse mine() { return response(profile()); }
    @Transactional
    public SubscriptionResponse demoPay() {
        if (!demo) throw new ApiException(403, "Demo payments are disabled");
        ProviderProfile p = profile();
        if (p.hasSubscription(clock.instant())) throw new ApiException(409, "Subscription is still valid");
        p.activatePaidMonth(clock.instant());
        providers.saveAndFlush(p);
        return response(p);
    }
    private ProviderProfile profile() {
        var user = current.require();
        if (user.getRole() != Role.PROVIDER) throw new ApiException(403, "Provider role required");
        return providers.findByUserId(user.getId()).orElseThrow(() -> new ApiException(404, "Create your provider profile first"));
    }
    private SubscriptionResponse response(ProviderProfile p) {
        boolean valid = p.hasSubscription(clock.instant());
        return new SubscriptionResponse(valid ? (p.isSubscriptionPaid() ? "ACTIVE" : "TRIAL") : "EXPIRED",
                p.getSubscriptionStart(), p.getSubscriptionEnd(), new BigDecimal("10.00"), "EUR", valid, demo);
    }
}
