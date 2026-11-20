package sk.kasv.fx.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import sk.kasv.fx.entity.*;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.*;
import sk.kasv.fx.security.CurrentUser;
import java.time.*;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static sk.kasv.fx.dto.ApiDtos.*;

class AccountServiceTest {
    UserRepository users = mock(UserRepository.class);
    ProviderProfileRepository providers = mock(ProviderProfileRepository.class);
    ServiceRepository services = mock(ServiceRepository.class);
    OrderRepository orders = mock(OrderRepository.class);
    ReviewRepository reviews = mock(ReviewRepository.class);
    OrderMessageRepository messages = mock(OrderMessageRepository.class);
    WorkingHoursRepository hours = mock(WorkingHoursRepository.class);
    DayOffRepository daysOff = mock(DayOffRepository.class);
    ProviderPhotoRepository photos = mock(ProviderPhotoRepository.class);
    RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    AuditService audit = mock(AuditService.class);
    CurrentUser current = mock(CurrentUser.class);
    BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(4);
    AccountService subject = new AccountService(users, providers, services, orders, reviews, messages, hours, daysOff, photos,
            refreshTokens, mock(PasswordResetTokenRepository.class), mock(EmailVerificationTokenRepository.class),
            mock(AvailabilityService.class), audit, passwords, current, Clock.systemUTC());
    User user = new User("peter@x.sk", passwords.encode("secret-password"), Role.PROVIDER);
    ProviderProfile profile = new ProviderProfile();

    @BeforeEach void setup() {
        user.setId(2L); user.setEmailVerified(true);
        profile.setId(3L); profile.setUser(user); profile.setName("Peter Novák"); profile.setPhone("0900 123 456");
        when(current.require()).thenReturn(user);
        when(providers.findByUserId(2L)).thenReturn(Optional.of(profile));
    }
    @Test void deletionAnonymizesEverything() {
        subject.delete(new DeleteAccountRequest("secret-password", null));
        assertThat(user.getEmail()).isEqualTo("deleted-2@deleted.invalid");
        assertThat(user.isBlocked()).isTrue();
        assertThat(passwords.matches("secret-password", user.getPassword())).isFalse();
        assertThat(user.getTokenVersion()).isEqualTo(1);
        assertThat(profile.getName()).isEqualTo("Zmazaný účet");
        assertThat(profile.getPhone()).isEmpty();
        verify(services).deleteAllForProvider(3L);
        verify(orders).anonymizeCustomer(2L, "[zmazané]");
        verify(reviews).removeComments(2L);
        verify(messages).replaceTexts(eq(2L), any());
        verify(refreshTokens).revokeAllForUser(eq(2L), any());
        verify(audit).record(user, AuditService.ACCOUNT_DELETED, "USER", 2L, null);
    }
    @Test void wrongPasswordKeepsAccount() {
        assertThatThrownBy(() -> subject.delete(new DeleteAccountRequest("wrong-password", null)))
                .isInstanceOf(ApiException.class).hasMessage("Wrong password");
        assertThat(user.getEmail()).isEqualTo("peter@x.sk");
    }
    @Test void openOrdersBlockDeletion() {
        when(orders.existsByProviderUserIdAndStatusIn(eq(2L), any())).thenReturn(true);
        assertThatThrownBy(() -> subject.delete(new DeleteAccountRequest("secret-password", null)))
                .isInstanceOf(ApiException.class).hasMessageContaining("open orders");
        assertThat(user.isBlocked()).isFalse();
    }
    @Test void twoFactorCodeIsRequiredWhenEnabled() {
        user.setTotpSecret(sk.kasv.fx.security.Totp.newSecret()); user.setTotpEnabled(true);
        assertThatThrownBy(() -> subject.delete(new DeleteAccountRequest("secret-password", null)))
                .isInstanceOf(ApiException.class).hasMessageContaining("two-factor");
        subject.delete(new DeleteAccountRequest("secret-password", sk.kasv.fx.security.Totp.codeAt(user.getTotpSecret(), Instant.now())));
        assertThat(user.isBlocked()).isTrue();
    }
    @Test void adminCannotDeleteHere() {
        user.setRole(Role.ADMIN);
        assertThatThrownBy(() -> subject.delete(new DeleteAccountRequest("secret-password", null))).isInstanceOf(ApiException.class);
    }
}
