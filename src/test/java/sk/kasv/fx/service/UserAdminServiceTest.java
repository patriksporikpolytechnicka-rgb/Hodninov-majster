package sk.kasv.fx.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sk.kasv.fx.entity.*;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.*;
import sk.kasv.fx.security.CurrentUser;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static sk.kasv.fx.dto.ApiDtos.*;

class UserAdminServiceTest {
    UserRepository users = mock(UserRepository.class);
    ProviderProfileRepository providers = mock(ProviderProfileRepository.class);
    OrderRepository orders = mock(OrderRepository.class);
    CurrentUser current = mock(CurrentUser.class);
    UserAdminService subject = new UserAdminService(users, providers, orders, current);
    User admin = new User("admin@example.invalid", "hash", Role.ADMIN);
    User customer = new User("customer@example.invalid", "hash", Role.CUSTOMER);

    @BeforeEach void setup() {
        admin.setId(1L); customer.setId(2L);
        when(current.require()).thenReturn(admin);
        when(users.findById(1L)).thenReturn(Optional.of(admin));
        when(users.findById(2L)).thenReturn(Optional.of(customer));
        when(users.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test void adminBlocksAndUnblocksUser() {
        assertThat(subject.update(2L, new AdminUserRequest(null, true)).blocked()).isTrue();
        assertThat(customer.isBlocked()).isTrue();
        assertThat(subject.update(2L, new AdminUserRequest(null, false)).blocked()).isFalse();
    }
    @Test void roleChangesOnlyWithoutHistory() {
        assertThat(subject.update(2L, new AdminUserRequest(Role.PROVIDER, null)).role()).isEqualTo(Role.PROVIDER);
        customer.setRole(Role.CUSTOMER);
        when(orders.existsByCustomerId(2L)).thenReturn(true);
        assertThatThrownBy(() -> subject.update(2L, new AdminUserRequest(Role.PROVIDER, null)))
                .isInstanceOf(ApiException.class).hasMessageContaining("block it instead");
        assertThat(customer.getRole()).isEqualTo(Role.CUSTOMER);
    }
    @Test void adminCannotChangeOwnAccount() {
        assertThatThrownBy(() -> subject.update(1L, new AdminUserRequest(null, true)))
                .isInstanceOf(ApiException.class).hasMessageContaining("own account");
        assertThat(admin.isBlocked()).isFalse();
    }
    @Test void adminTurnsOffTwoFactorForLockedOutUser() {
        customer.setTotpSecret("SECRET"); customer.setTotpEnabled(true);
        assertThat(subject.update(2L, new AdminUserRequest(null, null, true)).twoFactorEnabled()).isFalse();
        assertThat(customer.getTotpSecret()).isNull();
    }
    @Test void emptyChangeIsRejected() {
        assertThatThrownBy(() -> subject.update(2L, new AdminUserRequest(null, null)))
                .isInstanceOf(ApiException.class).hasMessageContaining("Nothing to change");
    }
    @Test void onlyAdminsManageUsers() {
        when(current.require()).thenReturn(customer);
        assertThatThrownBy(() -> subject.list(null, null, null, 0, 20)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> subject.update(1L, new AdminUserRequest(null, true))).isInstanceOf(ApiException.class);
        verify(users, never()).saveAndFlush(any());
    }
}
