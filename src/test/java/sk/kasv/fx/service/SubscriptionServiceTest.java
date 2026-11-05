package sk.kasv.fx.service;
import org.junit.jupiter.api.Test;
import sk.kasv.fx.entity.*;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.ProviderProfileRepository;
import sk.kasv.fx.security.CurrentUser;
import java.time.*;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class SubscriptionServiceTest {
    Clock clock = Clock.fixed(Instant.parse("2026-10-07T12:00:00Z"), ZoneId.of("Europe/Bratislava"));
    ProviderProfileRepository profiles = mock(ProviderProfileRepository.class);
    CurrentUser current = mock(CurrentUser.class);
    ProviderProfile profile = new ProviderProfile();
    SubscriptionService subject = new SubscriptionService(profiles, current, clock, true);
    SubscriptionServiceTest() {
        User user = new User("provider@example.invalid", "hash", Role.PROVIDER); user.setId(1L);
        when(current.require()).thenReturn(user);
        when(profiles.findByUserId(1L)).thenReturn(Optional.of(profile));
    }
    @Test void trialLastsExactlyThirtyDaysAndCannotBeRestarted() {
        profile.startTrial(clock.instant());
        Instant end = profile.getSubscriptionEnd();
        profile.startTrial(clock.instant().plus(Duration.ofDays(5)));
        assertThat(profile.getSubscriptionEnd()).isEqualTo(end);
        assertThat(end).isEqualTo(clock.instant().plus(Duration.ofDays(30)));
        assertThat(profile.hasSubscription(end.minusSeconds(1))).isTrue();
        assertThat(profile.hasSubscription(end)).isFalse();
        assertThat(subject.mine().status()).isEqualTo("TRIAL");
        assertThat(subject.mine().monthlyFee()).isEqualByComparingTo("10.00");
    }
    @Test void expiredTrialRequiresPaymentAndPaidMonthHasCalendarEnd() {
        profile.startTrial(clock.instant().minus(Duration.ofDays(30)));
        assertThat(subject.mine().status()).isEqualTo("EXPIRED");
        var result = subject.demoPay();
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.subscriptionEnd()).isEqualTo(clock.instant().atZone(clock.getZone()).plusMonths(1).toInstant());
        assertThatThrownBy(subject::demoPay).isInstanceOf(ApiException.class).hasMessageContaining("still valid");
        verify(profiles, times(1)).saveAndFlush(profile);
    }
    @Test void productionCannotSimulatePayment() {
        var production = new SubscriptionService(profiles, current, clock, false);
        assertThatThrownBy(production::demoPay).isInstanceOf(ApiException.class).hasMessageContaining("disabled");
        verify(profiles, never()).saveAndFlush(any());
    }
    @Test void customerCannotReadSubscription() {
        when(current.require()).thenReturn(new User("customer@example.invalid", "hash", Role.CUSTOMER));
        assertThatThrownBy(subject::mine).isInstanceOf(ApiException.class).hasMessageContaining("Provider");
    }
}
