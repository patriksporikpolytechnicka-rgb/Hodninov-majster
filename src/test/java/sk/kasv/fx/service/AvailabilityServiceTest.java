package sk.kasv.fx.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sk.kasv.fx.entity.*;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.*;
import sk.kasv.fx.security.CurrentUser;
import java.time.*;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static sk.kasv.fx.dto.ApiDtos.*;

class AvailabilityServiceTest {
    WorkingHoursRepository hours = mock(WorkingHoursRepository.class);
    DayOffRepository daysOff = mock(DayOffRepository.class);
    ProviderProfileRepository providers = mock(ProviderProfileRepository.class);
    CurrentUser current = mock(CurrentUser.class);
    Clock clock = Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneId.of("Europe/Bratislava"));
    AvailabilityService subject = new AvailabilityService(hours, daysOff, providers, current, clock);
    User providerUser = new User("p@x.sk", "h", Role.PROVIDER);
    ProviderProfile provider = new ProviderProfile();
    // 2026-11-23 is a Monday.
    LocalDateTime mondayAt(int hour) { return LocalDateTime.of(2026, 11, 23, hour, 0); }

    @BeforeEach void setup() {
        providerUser.setId(2L); provider.setId(3L); provider.setUser(providerUser);
        when(current.require()).thenReturn(providerUser);
        when(providers.findByUserId(2L)).thenReturn(Optional.of(provider));
    }
    @Test void withoutWorkingHoursAnyTimeIsFine() {
        subject.requireAvailable(provider, mondayAt(23));
    }
    @Test void orderMustStartInsideWorkingHours() {
        when(hours.findByProviderIdOrderByDayOfWeekAsc(3L)).thenReturn(List.of(new WorkingHours(provider, 1, LocalTime.of(8, 0), LocalTime.of(16, 0))));
        subject.requireAvailable(provider, mondayAt(8));
        assertThatThrownBy(() -> subject.requireAvailable(provider, mondayAt(16))).isInstanceOf(ApiException.class).hasMessageContaining("working hours");
        assertThatThrownBy(() -> subject.requireAvailable(provider, mondayAt(7))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> subject.requireAvailable(provider, mondayAt(10).plusDays(1))).isInstanceOf(ApiException.class);
    }
    @Test void dayOffBlocksTheWholeDay() {
        when(daysOff.existsByProviderIdAndDay(3L, LocalDate.of(2026, 11, 23))).thenReturn(true);
        assertThatThrownBy(() -> subject.requireAvailable(provider, mondayAt(10))).isInstanceOf(ApiException.class).hasMessageContaining("not available");
    }
    @Test void workingHoursAreValidated() {
        var twice = new WorkingHoursRequest(List.of(new WorkingHoursEntry(1, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                new WorkingHoursEntry(1, LocalTime.of(13, 0), LocalTime.of(16, 0))));
        assertThatThrownBy(() -> subject.setWorkingHours(twice)).isInstanceOf(ApiException.class).hasMessageContaining("only once");
        var backwards = new WorkingHoursRequest(List.of(new WorkingHoursEntry(2, LocalTime.of(16, 0), LocalTime.of(8, 0))));
        assertThatThrownBy(() -> subject.setWorkingHours(backwards)).isInstanceOf(ApiException.class).hasMessageContaining("before end");
        verify(hours, never()).deleteAllForProvider(any());
    }
    @Test void pastDayOffIsRejected() {
        assertThatThrownBy(() -> subject.addDayOff(new DayOffRequest(LocalDate.of(2026, 11, 19)))).isInstanceOf(ApiException.class);
    }
    @Test void onlyHandymenEditAvailability() {
        when(current.require()).thenReturn(new User("c@x.sk", "h", Role.CUSTOMER));
        assertThatThrownBy(() -> subject.addDayOff(new DayOffRequest(LocalDate.of(2026, 12, 24)))).isInstanceOf(ApiException.class);
    }
}
