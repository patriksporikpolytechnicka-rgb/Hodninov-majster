package sk.kasv.fx.service;

import org.junit.jupiter.api.Test;
import sk.kasv.fx.entity.*;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.UserRepository;
import sk.kasv.fx.security.Totp;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static sk.kasv.fx.dto.ApiDtos.*;

class TwoFactorServiceTest {
    UserRepository users = mock(UserRepository.class);
    Clock clock = Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneOffset.UTC);
    TwoFactorService subject = new TwoFactorService(users, clock, mock(AuditService.class));
    User user = new User("a@example.invalid", "hash", Role.CUSTOMER);

    /** The code an authenticator app would show right now. */
    static String currentCode(String secret, Clock clock) { return Totp.codeAt(secret, clock.instant()); }

    @Test void setupThenEnableWithValidCode() {
        var setup = subject.setup(user);
        assertThat(setup.otpauthUri()).contains("secret=" + setup.secret());
        assertThat(user.isTotpEnabled()).isFalse();
        subject.enable(user, new TwoFactorCodeRequest(currentCode(setup.secret(), clock)));
        assertThat(user.isTotpEnabled()).isTrue();
        assertThat(user.getTotpLastStep()).isEqualTo(Totp.step(clock.instant()));
    }
    @Test void wrongCodeDoesNotEnable() {
        String secret = subject.setup(user).secret();
        String wrong = currentCode(secret, clock).equals("000000") ? "111111" : "000000";
        assertThatThrownBy(() -> subject.enable(user, new TwoFactorCodeRequest(wrong)))
                .isInstanceOf(ApiException.class).hasMessageContaining("Invalid two-factor code");
        assertThat(user.isTotpEnabled()).isFalse();
    }
    @Test void disableNeedsValidCodeAndClearsSecret() {
        String secret = subject.setup(user).secret();
        user.setTotpEnabled(true);
        subject.disable(user, new TwoFactorCodeRequest(currentCode(secret, clock)));
        assertThat(user.isTotpEnabled()).isFalse();
        assertThat(user.getTotpSecret()).isNull();
    }
    @Test void setupIsRefusedWhenAlreadyEnabled() {
        user.setTotpEnabled(true);
        assertThatThrownBy(() -> subject.setup(user)).isInstanceOf(ApiException.class).hasMessageContaining("already enabled");
    }
}
