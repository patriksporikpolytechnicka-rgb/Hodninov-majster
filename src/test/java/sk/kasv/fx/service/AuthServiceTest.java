package sk.kasv.fx.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import sk.kasv.fx.entity.*;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.UserRepository;
import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static sk.kasv.fx.dto.ApiDtos.*;

class AuthServiceTest {
    UserRepository users = mock(UserRepository.class);
    BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(4);
    JwtEncoder encoder = mock(JwtEncoder.class);
    AuthService subject = new AuthService(users, passwords, encoder, Clock.systemUTC());
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
    @Test void rejectsPasswordsOverBcryptByteLimit() {
        assertThatThrownBy(() -> subject.register(new RegisterRequest("test@example.invalid", "á".repeat(40), Role.CUSTOMER)))
                .isInstanceOf(ApiException.class).hasMessageContaining("72 UTF-8 bytes");
    }
}
