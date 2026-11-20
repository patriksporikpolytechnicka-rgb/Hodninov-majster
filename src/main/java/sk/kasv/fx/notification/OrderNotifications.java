package sk.kasv.fx.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import sk.kasv.fx.entity.OrderStatus;
import sk.kasv.fx.entity.Role;
import sk.kasv.fx.service.MailService;
import java.time.format.DateTimeFormatter;

/**
 * E-mails about orders: the handyman hears about new orders, the customer about every status change,
 * and the other side about a cancellation. Sent only after the change is saved, outside the request thread.
 */
@Component
public class OrderNotifications {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d. M. yyyy 'o' HH:mm");
    private final MailService mail;
    private final String frontendUrl;
    public OrderNotifications(MailService mail, @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.mail = mail; this.frontendUrl = frontendUrl;
    }

    @Async
    @TransactionalEventListener
    public void onOrderEvent(OrderEvent e) {
        Message m = message(e);
        if (m != null) mail.send(m.to(), m.subject(), m.text() + "\n\nDetail: " + frontendUrl + "/objednavky/" + e.orderId());
    }

    record Message(String to, String subject, String text) {}

    /** Who gets what; null when nobody needs to know. */
    static Message message(OrderEvent e) {
        String order = "Objednávka #" + e.orderId() + " – " + e.serviceName();
        String when = "Termín: " + DATE.format(e.scheduledAt()) + "\nAdresa: " + e.address();
        if (e.changedBy() == null) {
            return new Message(e.providerEmail(), "Nová objednávka #" + e.orderId(),
                    "Máte novú objednávku: " + e.serviceName() + "\n" + when + "\nCena: " + e.totalPrice() + " €"
                            + (e.note() == null ? "" : "\nPoznámka: " + e.note())
                            + "\n\nPrijmite alebo odmietnite ju v časti Objednávky.");
        }
        return switch (e.status()) {
            case ACCEPTED -> new Message(e.customerEmail(), order + " bola prijatá", "Majster prijal vašu objednávku.\n" + when);
            case REJECTED -> new Message(e.customerEmail(), order + " bola odmietnutá",
                    "Majster vašu objednávku odmietol. Skúste iný termín alebo iného majstra.");
            case IN_PROGRESS -> new Message(e.customerEmail(), order + " – práca začala", "Majster začal pracovať na vašej objednávke.");
            case COMPLETED -> new Message(e.customerEmail(), order + " je dokončená",
                    "Majster označil prácu za dokončenú. Budeme radi, ak ho ohodnotíte.");
            case CANCELLED -> e.changedBy() == Role.CUSTOMER
                    ? new Message(e.providerEmail(), order + " bola zrušená", "Zákazník zrušil objednávku.\n" + when)
                    : new Message(e.customerEmail(), order + " bola zrušená", "Majster zrušil objednávku.\n" + when);
            case PENDING -> null;
        };
    }
}
