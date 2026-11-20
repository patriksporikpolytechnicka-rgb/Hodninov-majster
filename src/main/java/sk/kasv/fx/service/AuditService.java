package sk.kasv.fx.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import sk.kasv.fx.entity.AuditEntry;
import sk.kasv.fx.entity.Role;
import sk.kasv.fx.entity.User;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.AuditRepository;
import sk.kasv.fx.security.CurrentUser;
import java.time.Clock;
import static sk.kasv.fx.dto.ApiDtos.*;

/** Writes audit entries in the same transaction as the action, so an entry exists exactly when the action happened. */
@org.springframework.stereotype.Service
@Transactional(readOnly = true)
public class AuditService {
    public static final String USER_BLOCKED = "USER_BLOCKED", USER_UNBLOCKED = "USER_UNBLOCKED",
            USER_ROLE_CHANGED = "USER_ROLE_CHANGED", USER_2FA_RESET = "USER_2FA_RESET", USER_EMAIL_VERIFIED = "USER_EMAIL_VERIFIED",
            CATEGORY_CREATED = "CATEGORY_CREATED", CATEGORY_RENAMED = "CATEGORY_RENAMED", CATEGORY_DELETED = "CATEGORY_DELETED",
            SERVICE_DELETED = "SERVICE_DELETED", PASSWORD_RESET = "PASSWORD_RESET", TWO_FACTOR_ENABLED = "2FA_ENABLED",
            TWO_FACTOR_DISABLED = "2FA_DISABLED", TOKEN_REUSE_DETECTED = "TOKEN_REUSE_DETECTED", ACCOUNT_DELETED = "ACCOUNT_DELETED";
    private final AuditRepository entries;
    private final CurrentUser current;
    private final Clock clock;
    public AuditService(AuditRepository entries, CurrentUser current, Clock clock) {
        this.entries = entries; this.current = current; this.clock = clock;
    }

    @Transactional
    public void record(User actor, String action, String targetType, Long targetId, String details) {
        String text = details == null ? null : details.length() > 500 ? details.substring(0, 500) : details;
        entries.save(new AuditEntry(actor == null ? null : actor.getId(), actor == null ? null : actor.getEmail(),
                action, targetType, targetId, text, clock.instant()));
    }

    public PageResponse<AuditResponse> list(String action, Long actorId, int page, int size) {
        if (current.require().getRole() != Role.ADMIN) throw new ApiException(403, "Role ADMIN required");
        Specification<AuditEntry> spec = (root, query, cb) -> cb.conjunction();
        if (action != null && !action.isBlank()) spec = spec.and((r, q, cb) -> cb.equal(r.get("action"), action.strip()));
        if (actorId != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("actorId"), actorId));
        return PageResponse.from(entries.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")))
                .map(e -> new AuditResponse(e.getId(), e.getActorId(), e.getActorEmail(), e.getAction(), e.getTargetType(),
                        e.getTargetId(), e.getDetails(), e.getCreatedAt())));
    }
}
