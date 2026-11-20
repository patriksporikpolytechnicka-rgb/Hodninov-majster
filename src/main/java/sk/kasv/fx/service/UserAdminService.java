package sk.kasv.fx.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import sk.kasv.fx.dto.ApiMapper;
import sk.kasv.fx.entity.Role;
import sk.kasv.fx.entity.User;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.OrderRepository;
import sk.kasv.fx.repository.ProviderProfileRepository;
import sk.kasv.fx.repository.UserRepository;
import sk.kasv.fx.security.CurrentUser;
import java.util.Locale;
import static sk.kasv.fx.dto.ApiDtos.*;

/** User management for administrators: list, block/unblock and change role. */
@org.springframework.stereotype.Service
@Transactional(readOnly = true)
public class UserAdminService {
    private final UserRepository users;
    private final ProviderProfileRepository providers;
    private final OrderRepository orders;
    private final CurrentUser current;
    public UserAdminService(UserRepository users, ProviderProfileRepository providers, OrderRepository orders, CurrentUser current) {
        this.users = users; this.providers = providers; this.orders = orders; this.current = current;
    }

    public PageResponse<AdminUserResponse> list(String q, Role role, Boolean blocked, int page, int size) {
        requireAdmin();
        Specification<User> spec = (root, query, cb) -> cb.conjunction();
        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.strip().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            spec = spec.and((r, query, cb) -> cb.like(cb.lower(r.get("email")), pattern, '!'));
        }
        if (role != null) spec = spec.and((r, query, cb) -> cb.equal(r.get("role"), role));
        if (blocked != null) spec = spec.and((r, query, cb) -> cb.equal(r.get("blocked"), blocked));
        return PageResponse.from(users.findAll(spec, PageRequest.of(page, size, Sort.by("id"))).map(ApiMapper::adminUser));
    }

    /**
     * Admins cannot change their own account. A role can change only on an account without
     * history (no provider profile, no orders); otherwise block it instead.
     */
    @Transactional
    public AdminUserResponse update(Long id, AdminUserRequest request) {
        User admin = requireAdmin();
        if (request.role() == null && request.blocked() == null && !Boolean.TRUE.equals(request.disableTwoFactor())) throw new ApiException(400, "Nothing to change");
        User user = users.findById(id).orElseThrow(() -> new ApiException(404, "User not found"));
        if (user.getId().equals(admin.getId())) throw new ApiException(409, "You cannot change your own account");
        if (request.role() != null && request.role() != user.getRole()) {
            if (providers.findByUserId(id).isPresent() || orders.existsByCustomerId(id))
                throw new ApiException(409, "Account has history; block it instead of changing its role");
            user.setRole(request.role());
        }
        if (request.blocked() != null) user.setBlocked(request.blocked());
        if (Boolean.TRUE.equals(request.disableTwoFactor())) TwoFactorService.clear(user);
        return ApiMapper.adminUser(users.saveAndFlush(user));
    }

    private User requireAdmin() {
        User user = current.require();
        if (user.getRole() != Role.ADMIN) throw new ApiException(403, "Role ADMIN required");
        return user;
    }
}
