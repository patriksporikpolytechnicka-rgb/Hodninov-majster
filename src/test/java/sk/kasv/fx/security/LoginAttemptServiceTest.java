package sk.kasv.fx.security;

import org.junit.jupiter.api.Test;
import sk.kasv.fx.exception.ApiException;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

class LoginAttemptServiceTest {
    MutableClock clock = new MutableClock(Instant.parse("2026-11-20T10:00:00Z"));
    LoginAttemptService subject = new LoginAttemptService(clock);

    @Test void blocksAfterFiveFailuresAndUnblocksAfterWindow() {
        for (int i = 0; i < 5; i++) { subject.check("login", "a@x.sk", "1.1.1.1"); subject.failed("login", "a@x.sk", "1.1.1.1"); }
        assertThatThrownBy(() -> subject.check("login", "a@x.sk", "2.2.2.2"))
                .isInstanceOf(ApiException.class).hasMessageContaining("Too many attempts");
        subject.check("login", "b@x.sk", "1.1.1.1");
        clock.now = clock.now.plus(Duration.ofMinutes(15));
        subject.check("login", "a@x.sk", "1.1.1.1");
    }
    @Test void successClearsEmailCounter() {
        for (int i = 0; i < 4; i++) subject.failed("login", "a@x.sk", "1.1.1.1");
        subject.succeeded("login", "a@x.sk");
        subject.failed("login", "a@x.sk", "1.1.1.1");
        subject.check("login", "a@x.sk", "1.1.1.1");
    }
    @Test void ipIsLimitedAcrossEmails() {
        for (int i = 0; i < 30; i++) subject.failed("login", "user" + i + "@x.sk", "3.3.3.3");
        assertThatThrownBy(() -> subject.check("login", "new@x.sk", "3.3.3.3")).isInstanceOf(ApiException.class);
    }
    @Test void actionsAreCountedSeparately() {
        for (int i = 0; i < 5; i++) subject.failed("reset", "a@x.sk", "1.1.1.1");
        subject.check("login", "a@x.sk", "1.1.1.1");
    }

    static class MutableClock extends Clock {
        Instant now;
        MutableClock(Instant now) { this.now = now; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
