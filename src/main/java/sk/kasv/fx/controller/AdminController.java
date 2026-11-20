package sk.kasv.fx.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import sk.kasv.fx.entity.Role;
import sk.kasv.fx.service.UserAdminService;
import static sk.kasv.fx.dto.ApiDtos.*;

@RestController
@Validated
@RequestMapping("/api/admin/users")
public class AdminController {
    private final UserAdminService admin;
    public AdminController(UserAdminService admin) { this.admin = admin; }

    @GetMapping
    public PageResponse<AdminUserResponse> list(@RequestParam(required = false) @Size(max = 254) String q,
            @RequestParam(required = false) Role role, @RequestParam(required = false) Boolean blocked,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return admin.list(q, role, blocked, page, size);
    }
    @PatchMapping("/{id}")
    public AdminUserResponse update(@PathVariable @Positive Long id, @Valid @RequestBody AdminUserRequest request) {
        return admin.update(id, request);
    }
}
