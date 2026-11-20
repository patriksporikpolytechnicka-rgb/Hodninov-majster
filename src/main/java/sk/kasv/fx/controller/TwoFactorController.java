package sk.kasv.fx.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import sk.kasv.fx.security.CurrentUser;
import sk.kasv.fx.service.TwoFactorService;
import static sk.kasv.fx.dto.ApiDtos.*;

@RestController
@RequestMapping("/api/auth/2fa")
public class TwoFactorController {
    private final TwoFactorService twoFactor;
    private final CurrentUser current;
    public TwoFactorController(TwoFactorService twoFactor, CurrentUser current) { this.twoFactor = twoFactor; this.current = current; }

    @PostMapping("/setup")
    public TwoFactorSetupResponse setup() { return twoFactor.setup(current.require()); }
    @PostMapping("/enable") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enable(@Valid @RequestBody TwoFactorCodeRequest request) { twoFactor.enable(current.require(), request); }
    @PostMapping("/disable") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@Valid @RequestBody TwoFactorCodeRequest request) { twoFactor.disable(current.require(), request); }
}
