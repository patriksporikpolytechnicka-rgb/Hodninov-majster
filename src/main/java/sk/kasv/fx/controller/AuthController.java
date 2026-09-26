package sk.kasv.fx.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import sk.kasv.fx.dto.ApiMapper;
import sk.kasv.fx.security.CurrentUser;
import sk.kasv.fx.service.AuthService;
import static sk.kasv.fx.dto.ApiDtos.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final CurrentUser current;
    public AuthController(AuthService auth, CurrentUser current) { this.auth = auth; this.current = current; }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) { return auth.register(request); }
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) { return auth.login(request); }
    @GetMapping("/me")
    public UserResponse me() { return ApiMapper.user(current.require()); }
}
