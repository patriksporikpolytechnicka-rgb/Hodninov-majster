package sk.kasv.fx.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sk.kasv.fx.dto.ApiMapper;
import sk.kasv.fx.entity.*;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.PasswordResetTokenRepository;
import sk.kasv.fx.repository.RefreshTokenRepository;
import sk.kasv.fx.repository.UserRepository;
import sk.kasv.fx.security.SecurityConfig;
import sk.kasv.fx.security.Tokens;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;
import static sk.kasv.fx.dto.ApiDtos.*;

@Service
@Transactional
public class AuthService {
    static final long ACCESS_SECONDS = 900;
    static final Duration REFRESH_LIFETIME = Duration.ofDays(14);
    static final Duration RESET_LIFETIME = Duration.ofMinutes(30);
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtEncoder tokens;
    private final Clock clock;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordResetMailer mailer;
    private final String dummyHash;
    public AuthService(UserRepository users, PasswordEncoder passwords, JwtEncoder tokens, Clock clock,
                       RefreshTokenRepository refreshTokens, PasswordResetTokenRepository resetTokens, PasswordResetMailer mailer) {
        this.users = users; this.passwords = passwords; this.tokens = tokens; this.clock = clock;
        this.refreshTokens = refreshTokens; this.resetTokens = resetTokens; this.mailer = mailer;
        this.dummyHash = passwords.encode("not-a-real-account-password");
    }
    public AuthResponse register(RegisterRequest request) {
        if (request.role() != Role.CUSTOMER && request.role() != Role.PROVIDER)
            throw new ApiException(400, "Only CUSTOMER or PROVIDER can register");
        validatePasswordLength(request.password());
        String email = normalize(request.email());
        if (users.existsByEmail(email)) throw new ApiException(409, "Email already registered");
        User user = new User(email, passwords.encode(request.password()), request.role());
        user.setCreatedAt(LocalDateTime.now(clock));
        return token(users.saveAndFlush(user));
    }
    public AuthResponse login(LoginRequest request) {
        validatePasswordLength(request.password());
        User user = users.findByEmail(normalize(request.email())).orElse(null);
        boolean matches = passwords.matches(request.password(), user == null ? dummyHash : user.getPassword());
        if (user == null || !matches) throw new ApiException(401, "Invalid email or password");
        if (user.isBlocked()) throw new ApiException(403, "Account is blocked");
        return token(user);
    }

    /**
     * Swaps a refresh token for a new access + refresh token (rotation). Using a token that was
     * already swapped means it leaked, so every session of that user is ended.
     * The revocation is kept even though the request fails (noRollbackFor).
     */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse refresh(RefreshRequest request) {
        Instant now = clock.instant();
        RefreshToken stored = refreshTokens.findByTokenHash(Tokens.hash(request.refreshToken()))
                .orElseThrow(() -> new ApiException(401, "Invalid refresh token"));
        User user = stored.getUser();
        if (stored.getRevokedAt() != null) {
            endAllSessions(user, now);
            throw new ApiException(401, "Invalid refresh token");
        }
        if (!now.isBefore(stored.getExpiresAt())) throw new ApiException(401, "Refresh token expired");
        if (user.isBlocked()) throw new ApiException(403, "Account is blocked");
        stored.setRevokedAt(now);
        return token(user);
    }
    /** Ends one session. Unknown or already revoked tokens are ignored. */
    public void logout(RefreshRequest request) {
        refreshTokens.findByTokenHash(Tokens.hash(request.refreshToken()))
                .filter(t -> t.getRevokedAt() == null).ifPresent(t -> t.setRevokedAt(clock.instant()));
    }
    /** Ends every session of the user, including access tokens that are still valid. */
    public void logoutEverywhere(User user) { endAllSessions(user, clock.instant()); }

    /** Always succeeds, so nobody can find out which e-mails are registered. */
    public void requestPasswordReset(PasswordResetRequest request) {
        User user = users.findByEmail(normalize(request.email())).orElse(null);
        if (user == null || user.isBlocked()) return;
        Instant now = clock.instant();
        resetTokens.invalidateAllForUser(user.getId(), now);
        String token = Tokens.newToken();
        resetTokens.save(new PasswordResetToken(user, Tokens.hash(token), now, now.plus(RESET_LIFETIME)));
        mailer.send(user.getEmail(), token);
    }
    /** Sets the new password and logs the user out everywhere. */
    public void resetPassword(PasswordResetConfirm request) {
        validatePasswordLength(request.newPassword());
        Instant now = clock.instant();
        PasswordResetToken stored = resetTokens.findByTokenHash(Tokens.hash(request.token()))
                .filter(t -> t.getUsedAt() == null && now.isBefore(t.getExpiresAt()))
                .orElseThrow(() -> new ApiException(400, "Invalid or expired reset token"));
        stored.setUsedAt(now);
        User user = stored.getUser();
        user.setPassword(passwords.encode(request.newPassword()));
        endAllSessions(user, now);
    }

    private void endAllSessions(User user, Instant now) {
        refreshTokens.revokeAllForUser(user.getId(), now);
        user.setTokenVersion(user.getTokenVersion() + 1);
        users.saveAndFlush(user);
    }
    private AuthResponse token(User user) {
        var now = clock.instant();
        var claims = JwtClaimsSet.builder().issuer(SecurityConfig.ISSUER).subject(user.getId().toString())
                .issuedAt(now).expiresAt(now.plusSeconds(ACCESS_SECONDS)).claim("role", user.getRole().name())
                .claim("tv", user.getTokenVersion()).build();
        String value = tokens.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        String refresh = Tokens.newToken();
        refreshTokens.save(new RefreshToken(user, Tokens.hash(refresh), now, now.plus(REFRESH_LIFETIME)));
        return new AuthResponse(value, "Bearer", ACCESS_SECONDS, ApiMapper.user(user), refresh);
    }
    private String normalize(String email) { return email.strip().toLowerCase(Locale.ROOT); }
    private void validatePasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ApiException(400, "Password must not exceed 72 UTF-8 bytes");
    }
}
