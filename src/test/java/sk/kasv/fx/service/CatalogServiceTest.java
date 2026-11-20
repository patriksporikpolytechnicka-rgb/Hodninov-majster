package sk.kasv.fx.service;

import org.junit.jupiter.api.Test;
import sk.kasv.fx.entity.*;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.*;
import sk.kasv.fx.security.CurrentUser;
import java.math.BigDecimal;
import java.util.Optional;
import static sk.kasv.fx.dto.ApiDtos.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogServiceTest {
    CategoryRepository categories = mock(CategoryRepository.class);
    ServiceRepository services = mock(ServiceRepository.class);
    ProviderProfileRepository providers = mock(ProviderProfileRepository.class);
    OrderRepository orders = mock(OrderRepository.class);
    CurrentUser current = mock(CurrentUser.class);
    CatalogService subject = new CatalogService(categories, services, providers, orders, current);
    @Test void providerCannotModifyAnotherProvidersService() {
        User actor = new User("a@example.invalid", "hash", Role.PROVIDER); actor.setId(1L);
        User owner = new User("b@example.invalid", "hash", Role.PROVIDER); owner.setId(2L);
        ProviderProfile profile = new ProviderProfile(); profile.setUser(owner);
        Service service = new Service(); service.setProvider(profile);
        when(current.require()).thenReturn(actor); when(services.findById(3L)).thenReturn(Optional.of(service));
        var request = new ServiceRequest(1L, "Name", "Description", BigDecimal.TEN, PriceUnit.HOUR, true);
        assertThatThrownBy(() -> subject.updateService(3L, request)).isInstanceOf(ApiException.class).hasMessageContaining("another provider");
        verify(services, never()).saveAndFlush(any());
    }
    @Test void onlyAdministratorCreatesCategories() {
        when(current.require()).thenReturn(new User("a@example.invalid", "hash", Role.CUSTOMER));
        assertThatThrownBy(() -> subject.createCategory(new CategoryRequest("Garden"))).isInstanceOf(ApiException.class);
        verify(categories, never()).saveAndFlush(any());
    }
    Service ownedService(User owner) {
        ProviderProfile profile = new ProviderProfile(); profile.setUser(owner);
        Service service = new Service(); service.setId(3L); service.setProvider(profile); service.setActive(true);
        when(services.findById(3L)).thenReturn(Optional.of(service));
        return service;
    }
    @Test void serviceWithoutOrdersIsRemoved() {
        User owner = new User("b@example.invalid", "hash", Role.PROVIDER); owner.setId(2L);
        Service service = ownedService(owner);
        when(current.require()).thenReturn(owner);
        subject.deleteService(3L);
        verify(services).delete(service);
    }
    @Test void serviceWithPastOrdersIsOnlyMarkedDeleted() {
        User owner = new User("b@example.invalid", "hash", Role.PROVIDER); owner.setId(2L);
        Service service = ownedService(owner);
        when(current.require()).thenReturn(owner);
        when(orders.existsByServiceId(3L)).thenReturn(true);
        subject.deleteService(3L);
        assertThat(service.isDeleted()).isTrue();
        assertThat(service.isActive()).isFalse();
        verify(services, never()).delete(any(Service.class));
    }
    @Test void serviceWithOpenOrdersCannotBeDeleted() {
        User owner = new User("b@example.invalid", "hash", Role.PROVIDER); owner.setId(2L);
        ownedService(owner);
        when(current.require()).thenReturn(owner);
        when(orders.existsByServiceIdAndStatusIn(eq(3L), any())).thenReturn(true);
        assertThatThrownBy(() -> subject.deleteService(3L)).isInstanceOf(ApiException.class).hasMessageContaining("open orders");
        verify(services, never()).delete(any(Service.class));
    }
    @Test void onlyOwnerOrAdminDeletesService() {
        User owner = new User("b@example.invalid", "hash", Role.PROVIDER); owner.setId(2L);
        User other = new User("c@example.invalid", "hash", Role.PROVIDER); other.setId(4L);
        Service service = ownedService(owner);
        when(current.require()).thenReturn(other);
        assertThatThrownBy(() -> subject.deleteService(3L)).isInstanceOf(ApiException.class).hasMessageContaining("another provider");
        User admin = new User("admin@example.invalid", "hash", Role.ADMIN); admin.setId(9L);
        when(current.require()).thenReturn(admin);
        subject.deleteService(3L);
        verify(services).delete(service);
    }
    @Test void categoryInUseCannotBeDeleted() {
        when(current.require()).thenReturn(new User("admin@example.invalid", "hash", Role.ADMIN));
        Category category = new Category(); category.setName("Garden");
        when(categories.findById(1L)).thenReturn(Optional.of(category));
        when(services.existsByCategoryId(1L)).thenReturn(true);
        assertThatThrownBy(() -> subject.deleteCategory(1L)).isInstanceOf(ApiException.class).hasMessageContaining("used by services");
        verify(categories, never()).delete(any());
    }
    @Test void categoryRenameKeepsNamesUnique() {
        when(current.require()).thenReturn(new User("admin@example.invalid", "hash", Role.ADMIN));
        when(categories.findById(1L)).thenReturn(Optional.of(new Category()));
        when(categories.existsByNameIgnoreCaseAndIdNot("Garden", 1L)).thenReturn(true);
        assertThatThrownBy(() -> subject.updateCategory(1L, new CategoryRequest(" Garden ")))
                .isInstanceOf(ApiException.class).hasMessageContaining("already exists");
    }
    @Test void blockedProvidersServiceIsHidden() {
        User owner = new User("b@example.invalid", "hash", Role.PROVIDER); owner.setBlocked(true);
        ownedService(owner);
        assertThatThrownBy(() -> subject.service(3L)).isInstanceOf(ApiException.class).hasMessage("Service not found");
    }
    @Test void inactiveServiceIsHidden() {
        Service service = new Service(); service.setActive(false);
        when(services.findById(3L)).thenReturn(Optional.of(service));
        assertThatThrownBy(() -> subject.service(3L)).isInstanceOf(ApiException.class).hasMessage("Service not found");
    }
}
