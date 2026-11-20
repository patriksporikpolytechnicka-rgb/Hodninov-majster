package sk.kasv.fx.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sk.kasv.fx.service.AccountService;
import static sk.kasv.fx.dto.ApiDtos.*;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    private final AccountService account;
    public AccountController(AccountService account) { this.account = account; }

    /** All data about the logged-in user as a JSON download. */
    @GetMapping("/export")
    public ResponseEntity<AccountExport> export() {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"hodinovy-majster-export.json\"")
                .body(account.export());
    }
    /** POST rather than DELETE with a body, which some clients and proxies drop. */
    @PostMapping("/delete") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Valid @RequestBody DeleteAccountRequest request) { account.delete(request); }
}
