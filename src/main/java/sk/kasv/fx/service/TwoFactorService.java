package sk.kasv.fx.service;

import org.springframework.transaction.annotation.Transactional;
import sk.kasv.fx.entity.User;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.UserRepository;
import sk.kasv.fx.security.Totp;
import java.time.Clock;
import static sk.kasv.fx.dto.ApiDtos.*;

/** Turning two-factor login (authenticator app) on and off for the logged-in user. */
@org.springframework.stereotype.Service
@Transactional
public class TwoFactorService {
    static final String ISSUER = "Hodinový majster";
    private final UserRepository users;
    private final Clock clock;
    public TwoFactorService(UserRepository users, Clock clock) { this.users = users; this.clock = clock; }

    /** Step 1: a new secret to scan. 2FA is not active until enable() confirms a code. */
    public TwoFactorSetupResponse setup(User user) {
        if (user.isTotpEnabled()) throw new ApiException(409, "Two-factor login is already enabled");
        String secret = Totp.newSecret();
        user.setTotpSecret(secret);
        users.saveAndFlush(user);
        return new TwoFactorSetupResponse(secret, Totp.uri(ISSUER, user.getEmail(), secret));
    }
    /** Step 2: the first code from the app proves the secret was scanned correctly. */
    public void enable(User user, TwoFactorCodeRequest request) {
        if (user.isTotpEnabled()) throw new ApiException(409, "Two-factor login is already enabled");
        if (user.getTotpSecret() == null) throw new ApiException(409, "Start the setup first");
        user.setTotpLastStep(checkCode(user, request.code()));
        user.setTotpEnabled(true);
        users.saveAndFlush(user);
    }
    /** Needs a current code, so a stolen session alone cannot turn 2FA off. */
    public void disable(User user, TwoFactorCodeRequest request) {
        if (!user.isTotpEnabled()) throw new ApiException(409, "Two-factor login is not enabled");
        checkCode(user, request.code());
        clear(user);
        users.saveAndFlush(user);
    }
    static void clear(User user) {
        user.setTotpEnabled(false); user.setTotpSecret(null); user.setTotpLastStep(0);
    }
    private long checkCode(User user, String code) {
        long step = Totp.verify(user.getTotpSecret(), code, clock.instant(), user.getTotpLastStep());
        if (step < 0) throw new ApiException(400, "Invalid two-factor code");
        return step;
    }
}
