package sk.kasv.fx.security;

import org.junit.jupiter.api.Test;
import java.util.Base64;
import static org.assertj.core.api.Assertions.*;

class SecretCipherTest {
    SecretCipher cipher = new SecretCipher("", "test-jwt-secret-at-least-thirty-two-bytes");

    @Test void roundTripWithFreshIvEachTime() {
        String a = cipher.encrypt("JBSWY3DPEHPK3PXP"), b = cipher.encrypt("JBSWY3DPEHPK3PXP");
        assertThat(a).startsWith("v1:").isNotEqualTo(b).doesNotContain("JBSWY3DPEHPK3PXP");
        assertThat(cipher.decrypt(a)).isEqualTo("JBSWY3DPEHPK3PXP");
        assertThat(a.length()).isLessThanOrEqualTo(255);
    }
    @Test void oldPlainTextValuesAreReadAsTheyAre() {
        assertThat(cipher.decrypt("JBSWY3DPEHPK3PXP")).isEqualTo("JBSWY3DPEHPK3PXP");
        assertThat(cipher.decrypt(null)).isNull();
    }
    @Test void tamperedOrForeignValuesAreNotAccepted() {
        String stored = cipher.encrypt("SECRET");
        char last = stored.charAt(stored.length() - 3);
        String tampered = stored.substring(0, stored.length() - 3) + (last == 'A' ? 'B' : 'A') + stored.substring(stored.length() - 2);
        assertThat(cipher.decrypt(tampered)).isNull();
        assertThat(new SecretCipher("", "another-jwt-secret-also-thirty-two-bytes").decrypt(stored)).isNull();
    }
    @Test void configuredKeyMustBe32Bytes() {
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        assertThat(new SecretCipher(key, "").decrypt(new SecretCipher(key, "x").encrypt("S"))).isEqualTo("S");
        assertThatThrownBy(() -> new SecretCipher(Base64.getEncoder().encodeToString(new byte[16]), "")).isInstanceOf(IllegalStateException.class);
    }
}
