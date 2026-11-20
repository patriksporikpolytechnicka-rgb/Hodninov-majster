package sk.kasv.fx.security;

import org.springframework.stereotype.Component;
import sk.kasv.fx.exception.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limits failed attempts per e-mail and per IP address within a time window.
 * Kept in memory: it resets on restart and is per server instance, which is enough for one pod.
 */
@Component
public class LoginAttemptService {
    static final int MAX_PER_EMAIL = 5;
    static final int MAX_PER_IP = 30;
    static final Duration WINDOW = Duration.ofMinutes(15);
    private final Clock clock;
    private final Map<String, Window> attempts = new ConcurrentHashMap<>();
    public LoginAttemptService(Clock clock) { this.clock = clock; }

    /** Throws 429 when this e-mail or IP already used up its attempts for the given action. */
    public void check(String action, String email, String ip) {
        Instant now = clock.instant();
        if (count(action + ":email:" + email, now) >= MAX_PER_EMAIL || count(action + ":ip:" + ip, now) >= MAX_PER_IP)
            throw new ApiException(429, "Too many attempts; try again in 15 minutes");
    }
    public void failed(String action, String email, String ip) {
        Instant now = clock.instant();
        add(action + ":email:" + email, now);
        add(action + ":ip:" + ip, now);
        if (attempts.size() > 10_000) attempts.values().removeIf(w -> w.expired(now));
    }
    public void succeeded(String action, String email) { attempts.remove(action + ":email:" + email); }

    private int count(String key, Instant now) {
        Window w = attempts.get(key);
        return w == null || w.expired(now) ? 0 : w.count;
    }
    private void add(String key, Instant now) {
        attempts.compute(key, (k, w) -> w == null || w.expired(now) ? new Window(now, 1) : new Window(w.start, w.count + 1));
    }
    private record Window(Instant start, int count) {
        boolean expired(Instant now) { return !now.isBefore(start.plus(WINDOW)); }
    }
}
