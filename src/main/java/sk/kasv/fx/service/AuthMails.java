package sk.kasv.fx.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** E-mails with one-time links for account security. Links end with the token. */
@Component
public class AuthMails {
    private final MailService mail;
    private final String resetLink;
    private final String verifyLink;
    public AuthMails(MailService mail,
                     @Value("${app.password-reset.link:http://localhost:4200/obnova-hesla?token=}") String resetLink,
                     @Value("${app.email-verification.link:http://localhost:4200/overenie-emailu?token=}") String verifyLink) {
        this.mail = mail; this.resetLink = resetLink; this.verifyLink = verifyLink;
    }
    public void sendPasswordReset(String email, String token) {
        mail.send(email, "Hodinový majster – obnovenie hesla",
                "Heslo si obnovíte na tomto odkaze (platí 30 minút):\n" + resetLink + token
                        + "\n\nAk ste o obnovenie nežiadali, správu ignorujte.");
    }
    public void sendVerification(String email, String token) {
        mail.send(email, "Hodinový majster – overenie e-mailu",
                "Vitajte! Svoj e-mail overíte na tomto odkaze (platí 24 hodín):\n" + verifyLink + token
                        + "\n\nAk ste sa neregistrovali, správu ignorujte.");
    }
}
