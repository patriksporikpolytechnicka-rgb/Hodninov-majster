package sk.kasv.fx.controller;

import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import sk.kasv.fx.service.AuditService;
import static sk.kasv.fx.dto.ApiDtos.*;

@RestController
@Validated
@RequestMapping("/api/admin/audit")
public class AuditController {
    private final AuditService audit;
    public AuditController(AuditService audit) { this.audit = audit; }

    @GetMapping
    public PageResponse<AuditResponse> list(@RequestParam(required = false) @Size(max = 50) String action,
            @RequestParam(required = false) @Positive Long actorId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return audit.list(action, actorId, page, size);
    }
}
