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

class ChatServiceTest {
    OrderMessageRepository messages = mock(OrderMessageRepository.class);
    OrderRepository orders = mock(OrderRepository.class);
    CurrentUser current = mock(CurrentUser.class);
    ChatService subject = new ChatService(messages, orders, current, Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneOffset.UTC));
    User customer = new User("c@x.sk", "h", Role.CUSTOMER), providerUser = new User("p@x.sk", "h", Role.PROVIDER),
            stranger = new User("s@x.sk", "h", Role.CUSTOMER), admin = new User("a@x.sk", "h", Role.ADMIN);
    Order order = new Order();

    @BeforeEach void setup() {
        customer.setId(1L); providerUser.setId(2L); stranger.setId(3L); admin.setId(4L);
        ProviderProfile provider = new ProviderProfile(); provider.setUser(providerUser);
        order.setId(5L); order.setCustomer(customer); order.setProvider(provider);
        when(orders.findById(5L)).thenReturn(Optional.of(order));
        when(messages.saveAndFlush(any())).thenAnswer(i -> { OrderMessage m = i.getArgument(0); m.setId(9L); return m; });
    }
    @Test void bothSidesCanWriteAndSideIsShown() {
        when(current.require()).thenReturn(customer);
        assertThat(subject.send(5L, new MessageRequest("  Dobrý deň  ")).senderRole()).isEqualTo(Role.CUSTOMER);
        when(current.require()).thenReturn(providerUser);
        var reply = subject.send(5L, new MessageRequest("Prídem o 9:00"));
        assertThat(reply.senderRole()).isEqualTo(Role.PROVIDER);
        assertThat(reply.text()).isEqualTo("Prídem o 9:00");
    }
    @Test void strangerCannotReadOrWrite() {
        when(current.require()).thenReturn(stranger);
        assertThatThrownBy(() -> subject.list(5L, 0)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> subject.send(5L, new MessageRequest("hi"))).isInstanceOf(ApiException.class);
    }
    @Test void adminReadsButCannotWrite() {
        when(current.require()).thenReturn(admin);
        when(messages.findTop100ByOrderIdAndIdGreaterThanOrderByIdAsc(5L, 0L)).thenReturn(List.of());
        assertThat(subject.list(5L, 0)).isEmpty();
        assertThatThrownBy(() -> subject.send(5L, new MessageRequest("hi"))).isInstanceOf(ApiException.class);
    }
    @Test void blankMessageIsRejected() {
        when(current.require()).thenReturn(customer);
        assertThatThrownBy(() -> subject.send(5L, new MessageRequest("   "))).isInstanceOf(ApiException.class);
        verify(messages, never()).saveAndFlush(any());
    }
}
