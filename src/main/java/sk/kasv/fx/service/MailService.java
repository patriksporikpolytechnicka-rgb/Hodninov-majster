package sk.kasv.fx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends plain-text e-mails. Without a mail server (spring.mail.host) the message is only
 * written to the log when app.mail.log-without-server=true (demo and local profiles).
 * A failed send is logged and never breaks the request that caused it.
 */
@Component
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private final ObjectProvider<JavaMailSender> mail;
    private final String from;
    private final boolean logWithoutServer;
    public MailService(ObjectProvider<JavaMailSender> mail,
                       @Value("${app.mail.from:no-reply@hodinovy-majster.local}") String from,
                       @Value("${app.mail.log-without-server:false}") boolean logWithoutServer) {
        this.mail = mail; this.from = from; this.logWithoutServer = logWithoutServer;
    }
    public void send(String to, String subject, String text) {
        JavaMailSender sender = mail.getIfAvailable();
        if (sender == null) {
            if (logWithoutServer) log.info("E-mail (no mail server configured) to {}: {}\n{}", to, subject, text);
            else log.warn("E-mail to {} not sent: no mail server configured (spring.mail.host)", to);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(to); message.setSubject(subject); message.setText(text);
        try { sender.send(message); }
        catch (MailException e) { log.error("E-mail to {} failed: {}", to, subject, e); }
    }
}
