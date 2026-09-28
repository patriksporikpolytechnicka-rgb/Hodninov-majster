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
    CurrentUser current = mock(CurrentUser.class);
    CatalogService subject = new CatalogService(categories, services, providers, current);
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
    @Test void inactiveServiceIsHidden() {
        Service service = new Service(); service.setActive(false);
        when(services.findById(3L)).thenReturn(Optional.of(service));
        assertThatThrownBy(() -> subject.service(3L)).isInstanceOf(ApiException.class).hasMessage("Service not found");
    }
}
