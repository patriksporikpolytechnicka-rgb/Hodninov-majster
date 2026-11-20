package sk.kasv.fx.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import sk.kasv.fx.dto.ApiMapper;
import sk.kasv.fx.entity.*;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.*;
import sk.kasv.fx.security.CurrentUser;
import sk.kasv.fx.security.Tokens;
import sk.kasv.fx.security.Totp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import static sk.kasv.fx.dto.ApiDtos.*;

/**
 * GDPR: a user can download all their data and delete their account.
 * Deleting anonymizes instead of removing rows, because orders, reviews and messages of the other
 * side must stay consistent (and orders are business records).
 */
@org.springframework.stereotype.Service
@Transactional(readOnly = true)
public class AccountService {
    static final List<OrderStatus> OPEN = List.of(OrderStatus.PENDING, OrderStatus.ACCEPTED, OrderStatus.IN_PROGRESS);
    private final UserRepository users;
    private final ProviderProfileRepository providers;
    private final ServiceRepository services;
    private final OrderRepository orders;
    private final ReviewRepository reviews;
    private final OrderMessageRepository messages;
    private final WorkingHoursRepository hours;
    private final DayOffRepository daysOff;
    private final ProviderPhotoRepository photos;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final EmailVerificationTokenRepository verifyTokens;
    private final AvailabilityService availability;
    private final AuditService audit;
    private final PasswordEncoder passwords;
    private final CurrentUser current;
    private final Clock clock;
    public AccountService(UserRepository users, ProviderProfileRepository providers, ServiceRepository services,
                          OrderRepository orders, ReviewRepository reviews, OrderMessageRepository messages,
                          WorkingHoursRepository hours, DayOffRepository daysOff, ProviderPhotoRepository photos,
                          RefreshTokenRepository refreshTokens, PasswordResetTokenRepository resetTokens,
                          EmailVerificationTokenRepository verifyTokens, AvailabilityService availability, AuditService audit,
                          PasswordEncoder passwords, CurrentUser current, Clock clock) {
        this.users = users; this.providers = providers; this.services = services; this.orders = orders;
        this.reviews = reviews; this.messages = messages; this.hours = hours; this.daysOff = daysOff; this.photos = photos;
        this.refreshTokens = refreshTokens; this.resetTokens = resetTokens; this.verifyTokens = verifyTokens;
        this.availability = availability; this.audit = audit; this.passwords = passwords; this.current = current; this.clock = clock;
    }

    /** Everything stored about the logged-in user (passwords, secrets and tokens are never included). */
    public AccountExport export() {
        User user = current.require();
        ProviderProfile profile = providers.findByUserId(user.getId()).orElse(null);
        return new AccountExport(clock.instant(), ApiMapper.adminUser(user),
                profile == null ? null : ApiMapper.provider(profile),
                profile == null ? List.of() : services.findAllByProviderIdAndDeletedFalseOrderByIdAsc(profile.getId()).stream().map(ApiMapper::service).toList(),
                orders.findAllByCustomerIdOrderByIdAsc(user.getId()).stream().map(ApiMapper::order).toList(),
                orders.findAllByProviderUserIdOrderByIdAsc(user.getId()).stream().map(ApiMapper::order).toList(),
                reviews.findAllByCustomerIdOrderByIdAsc(user.getId()).stream().map(ApiMapper::review).toList(),
                messages.findAllBySenderIdOrderByIdAsc(user.getId()).stream().map(ApiMapper::message).toList(),
                profile == null ? null : availability.get(profile.getId()));
    }

    /** Needs the password (and the 2FA code when enabled). Open orders must be finished or cancelled first. */
    @Transactional
    public void delete(DeleteAccountRequest request) {
        User user = current.require();
        if (user.getRole() == Role.ADMIN) throw new ApiException(409, "Administrators cannot delete their account here");
        if (!passwords.matches(request.password(), user.getPassword())) throw new ApiException(400, "Wrong password");
        if (user.isTotpEnabled()) {
            long step = Totp.verify(user.getTotpSecret(), request.code(), clock.instant(), user.getTotpLastStep());
            if (step < 0) throw new ApiException(400, "Invalid two-factor code");
        }
        if (orders.existsByCustomerIdAndStatusIn(user.getId(), OPEN) || orders.existsByProviderUserIdAndStatusIn(user.getId(), OPEN))
            throw new ApiException(409, "Finish or cancel your open orders first");
        Instant now = clock.instant();
        Long id = user.getId();

        providers.findByUserId(id).ifPresent(profile -> {
            profile.setName("Zmazaný účet"); profile.setDescription(""); profile.setLocation(""); profile.setPhone("");
            profile.setProfileImage(null); profile.setAvailability(false);
            providers.save(profile);
            services.deleteAllForProvider(profile.getId());
            hours.deleteAllForProvider(profile.getId());
            daysOff.deleteAllForProvider(profile.getId());
            if (photos.existsById(profile.getId())) photos.deleteById(profile.getId());
        });
        orders.anonymizeCustomer(id, "[zmazané]");
        reviews.removeComments(id);
        messages.replaceTexts(id, "[správa zmazaná]");
        refreshTokens.revokeAllForUser(id, now);
        resetTokens.invalidateAllForUser(id, now);
        verifyTokens.invalidateAllForUser(id, now);

        user.setEmail("deleted-" + id + "@deleted.invalid");
        user.setPassword(passwords.encode(Tokens.newToken()));
        user.setBlocked(true);
        user.setEmailVerified(false);
        TwoFactorService.clear(user);
        user.setTokenVersion(user.getTokenVersion() + 1);
        users.saveAndFlush(user);
        audit.record(user, AuditService.ACCOUNT_DELETED, "USER", id, null);
    }
}
