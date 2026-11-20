package sk.kasv.fx.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import sk.kasv.fx.entity.*;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.*;
import sk.kasv.fx.security.Tokens;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static sk.kasv.fx.dto.ApiDtos.*;

class AuthServiceTest {
    UserRepository users = mock(UserRepository.class);
    BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(4);
    JwtEncoder encoder = mock(JwtEncoder.class);
    RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    PasswordResetTokenRepository resetTokens = mock(PasswordResetTokenRepository.class);
    PasswordResetMailer mailer = mock(PasswordResetMailer.class);
    AuthService subject = new AuthService(users, passwords, encoder, Clock.systemUTC(), refreshTokens, resetTokens, mailer);
    User existing(String password) {
        User u = new User("test@example.invalid", passwords.encode(password), Role.CUSTOMER); u.setId(7L);
        return u;
    }
    void signs() {
        when(encoder.encode(any())).thenReturn(Jwt.withTokenValue("signed-token").header("alg", "HS256").subject("7").build());
    }
    @Test void cannotRegisterAdministrator() {
        assertThatThrownBy(() -> subject.register(new RegisterRequest("a@example.invalid", "long-password", Role.ADMIN)))
                .isInstanceOf(ApiException.class).hasMessageContaining("CUSTOMER or PROVIDER");
        verify(users, never()).saveAndFlush(any());
    }
    @Test void registrationNormalizesEmailAndHashesPassword() {
        AtomicReference<User> saved = new AtomicReference<>();
        when(users.saveAndFlush(any())).thenAnswer(i -> { User u = i.getArgument(0); u.setId(7L); saved.set(u); return u; });
        when(encoder.encode(any())).thenReturn(Jwt.withTokenValue("signed-token").header("alg", "HS256").subject("7").build());
        var response = subject.register(new RegisterRequest("TEST@Example.invalid", "long-password", Role.PROVIDER));
        assertThat(saved.get().getEmail()).isEqualTo("test@example.invalid");
        assertThat(passwords.matches("long-password", saved.get().getPassword())).isTrue();
        assertThat(response.accessToken()).isEqualTo("signed-token");
        assertThat(response.user().role()).isEqualTo(Role.PROVIDER);
    }
    @Test void duplicateEmailIsRejected() {
        when(users.existsByEmail("test@example.invalid")).thenReturn(true);
        assertThatThrownBy(() -> subject.register(new RegisterRequest("TEST@example.invalid", "long-password", Role.CUSTOMER)))
                .isInstanceOf(ApiException.class).hasMessageContaining("already registered");
    }
    @Test void wrongPasswordAndMissingUserHaveSameError() {
        User u = new User("test@example.invalid", passwords.encode("correct-password"), Role.CUSTOMER);
        when(users.findByEmail("test@example.invalid")).thenReturn(Optional.of(u));
        assertThatThrownBy(() -> subject.login(new LoginRequest("test@example.invalid", "wrong-password")))
                .isInstanceOf(ApiException.class).hasMessage("Invalid email or password");
        assertThatThrownBy(() -> subject.login(new LoginRequest("missing@example.invalid", "wrong-password")))
                .isInstanceOf(ApiException.class).hasMessage("Invalid email or password");
    }
    @Test void loginReturnsRefreshTokenAndStoresOnlyItsHash() {
        signs();
        when(users.findByEmail("test@example.invalid")).thenReturn(Optional.of(existing("correct-password")));
        AtomicReference<RefreshToken> saved = new AtomicReference<>();
        when(refreshTokens.save(any())).thenAnswer(i -> { saved.set(i.getArgument(0)); return i.getArgument(0); });
        var response = subject.login(new LoginRequest("test@example.invalid", "correct-password"));
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(saved.get().getTokenHash()).isEqualTo(Tokens.hash(response.refreshToken())).isNotEqualTo(response.refreshToken());
    }
    @Test void blockedUserCannotLogIn() {
        User u = existing("correct-password"); u.setBlocked(true);
        when(users.findByEmail("test@example.invalid")).thenReturn(Optional.of(u));
        assertThatThrownBy(() -> subject.login(new LoginRequest("test@example.invalid", "correct-password")))
                .isInstanceOf(ApiException.class).hasMessageContaining("blocked");
    }
    @Test void refreshRotatesToken() {
        signs();
        User u = existing("pw-123456789");
        RefreshToken stored = new RefreshToken(u, Tokens.hash("old"), Instant.now(), Instant.now().plusSeconds(600));
        when(refreshTokens.findByTokenHash(Tokens.hash("old"))).thenReturn(Optional.of(stored));
        var response = subject.refresh(new RefreshRequest("old"));
        assertThat(stored.getRevokedAt()).isNotNull();
        assertThat(response.refreshToken()).isNotEqualTo("old");
    }
    @Test void reusedRefreshTokenEndsAllSessions() {
        User u = existing("pw-123456789");
        RefreshToken stored = new RefreshToken(u, Tokens.hash("old"), Instant.now(), Instant.now().plusSeconds(600));
        stored.setRevokedAt(Instant.now());
        when(refreshTokens.findByTokenHash(Tokens.hash("old"))).thenReturn(Optional.of(stored));
        assertThatThrownBy(() -> subject.refresh(new RefreshRequest("old"))).isInstanceOf(ApiException.class);
        verify(refreshTokens).revokeAllForUser(eq(7L), any());
        assertThat(u.getTokenVersion()).isEqualTo(1);
    }
    @Test void expiredRefreshTokenIsRejected() {
        User u = existing("pw-123456789");
        RefreshToken stored = new RefreshToken(u, Tokens.hash("old"), Instant.now().minusSeconds(900), Instant.now().minusSeconds(1));
        when(refreshTokens.findByTokenHash(Tokens.hash("old"))).thenReturn(Optional.of(stored));
        assertThatThrownBy(() -> subject.refresh(new RefreshRequest("old"))).isInstanceOf(ApiException.class).hasMessageContaining("expired");
    }
    @Test void resetRequestForUnknownEmailSendsNothing() {
        subject.requestPasswordReset(new PasswordResetRequest("missing@example.invalid"));
        verifyNoInteractions(mailer, resetTokens);
    }
    @Test void resetRequestMailsTokenAndStoresOnlyItsHash() {
        when(users.findByEmail("test@example.invalid")).thenReturn(Optional.of(existing("pw-123456789")));
        AtomicReference<PasswordResetToken> saved = new AtomicReference<>();
        when(resetTokens.save(any())).thenAnswer(i -> { saved.set(i.getArgument(0)); return i.getArgument(0); });
        subject.requestPasswordReset(new PasswordResetRequest("Test@example.invalid"));
        var token = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(mailer).send(eq("test@example.invalid"), token.capture());
        assertThat(saved.get().getTokenHash()).isEqualTo(Tokens.hash(token.getValue()));
    }
    @Test void passwordResetChangesPasswordOnceAndEndsSessions() {
        User u = existing("old-password-1");
        PasswordResetToken stored = new PasswordResetToken(u, Tokens.hash("reset"), Instant.now(), Instant.now().plusSeconds(600));
        when(resetTokens.findByTokenHash(Tokens.hash("reset"))).thenReturn(Optional.of(stored));
        subject.resetPassword(new PasswordResetConfirm("reset", "new-password-1"));
        assertThat(passwords.matches("new-password-1", u.getPassword())).isTrue();
        assertThat(u.getTokenVersion()).isEqualTo(1);
        verify(refreshTokens).revokeAllForUser(eq(7L), any());
        assertThatThrownBy(() -> subject.resetPassword(new PasswordResetConfirm("reset", "other-password-1")))
                .isInstanceOf(ApiException.class).hasMessageContaining("Invalid or expired");
    }
    @Test void rejectsPasswordsOverBcryptByteLimit() {
        assertThatThrownBy(() -> subject.register(new RegisterRequest("test@example.invalid", "á".repeat(40), Role.CUSTOMER)))
                .isInstanceOf(ApiException.class).hasMessageContaining("72 UTF-8 bytes");
    }
}
