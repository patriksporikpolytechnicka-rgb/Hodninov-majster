package sk.kasv.fx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Sends the reset link by e-mail; without a mail server it can only log it (demo/local). */
@Component
public class PasswordResetMailer {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailer.class);
    private final ObjectProvider<JavaMailSender> mail;
    private final String link;
    private final String from;
    private final boolean logLinks;
    public PasswordResetMailer(ObjectProvider<JavaMailSender> mail,
                               @Value("${app.password-reset.link:http://localhost:4200/obnova-hesla?token=}") String link,
                               @Value("${app.mail.from:no-reply@hodinovy-majster.local}") String from,
                               @Value("${app.password-reset.log-links:false}") boolean logLinks) {
        this.mail = mail; this.link = link; this.from = from; this.logLinks = logLinks;
    }
    public void send(String email, String token) {
        String url = link + token;
        JavaMailSender sender = mail.getIfAvailable();
        if (sender != null) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from); message.setTo(email);
            message.setSubject("Hodinový majster – obnovenie hesla");
            message.setText("Heslo si obnovíte na tomto odkaze (platí 30 minút):\n" + url
                    + "\n\nAk ste o obnovenie nežiadali, správu ignorujte.");
            try { sender.send(message); }
            catch (org.springframework.mail.MailException e) { log.error("Password reset e-mail to {} failed", email, e); }
        } else if (logLinks) {
            log.info("Password reset link for {}: {}", email, url);
        } else {
            log.warn("Password reset requested but no mail server is configured (spring.mail.host)");
        }
    }
}
