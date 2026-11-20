package sk.kasv.fx.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import sk.kasv.fx.dto.ApiMapper;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.security.CurrentUser;
import sk.kasv.fx.security.LoginAttemptService;
import sk.kasv.fx.service.AuthService;
import java.util.Locale;
import static sk.kasv.fx.dto.ApiDtos.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final CurrentUser current;
    private final LoginAttemptService attempts;
    public AuthController(AuthService auth, CurrentUser current, LoginAttemptService attempts) {
        this.auth = auth; this.current = current; this.attempts = attempts;
    }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) { return auth.register(request); }

    /** At most 5 wrong passwords per e-mail (30 per IP) in 15 minutes, then 429. */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        attempts.check("login", email, http.getRemoteAddr());
        try {
            AuthResponse response = auth.login(request);
            attempts.succeeded("login", email);
            return response;
        } catch (ApiException e) {
            if (e.getStatus() == 401) attempts.failed("login", email, http.getRemoteAddr());
            throw e;
        }
    }
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) { return auth.refresh(request); }
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) { auth.logout(request); }
    @PostMapping("/logout-all") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll() { auth.logoutEverywhere(current.require()); }

    /** Always 202, whether the e-mail exists or not. Limited like login. */
    @PostMapping("/password-reset/request") @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request, HttpServletRequest http) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        attempts.check("reset", email, http.getRemoteAddr());
        attempts.failed("reset", email, http.getRemoteAddr());
        auth.requestPasswordReset(request);
    }
    @PostMapping("/password-reset/confirm") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody PasswordResetConfirm request) { auth.resetPassword(request); }

    @GetMapping("/me")
    public UserResponse me() { return ApiMapper.user(current.require()); }
}
