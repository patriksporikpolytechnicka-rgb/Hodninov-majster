package sk.kasv.fx.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sk.kasv.fx.dto.ApiMapper;
import sk.kasv.fx.entity.*;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.UserRepository;
import sk.kasv.fx.security.SecurityConfig;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import static sk.kasv.fx.dto.ApiDtos.*;

@Service
@Transactional
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtEncoder tokens;
    private final Clock clock;
    private final String dummyHash;
    public AuthService(UserRepository users, PasswordEncoder passwords, JwtEncoder tokens, Clock clock) {
        this.users = users; this.passwords = passwords; this.tokens = tokens; this.clock = clock;
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
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        validatePasswordLength(request.password());
        User user = users.findByEmail(normalize(request.email())).orElse(null);
        boolean matches = passwords.matches(request.password(), user == null ? dummyHash : user.getPassword());
        if (user == null || !matches) throw new ApiException(401, "Invalid email or password");
        return token(user);
    }
    private AuthResponse token(User user) {
        var now = clock.instant();
        var claims = JwtClaimsSet.builder().issuer(SecurityConfig.ISSUER).subject(user.getId().toString())
                .issuedAt(now).expiresAt(now.plusSeconds(3600)).claim("role", user.getRole().name()).build();
        String value = tokens.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AuthResponse(value, "Bearer", 3600, ApiMapper.user(user));
    }
    private String normalize(String email) { return email.strip().toLowerCase(Locale.ROOT); }
    private void validatePasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ApiException(400, "Password must not exceed 72 UTF-8 bytes");
    }
}
