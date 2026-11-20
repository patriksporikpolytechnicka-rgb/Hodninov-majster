package sk.kasv.fx.security;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

class TotpTest {
    // RFC 6238 test secret (SHA1) and its expected codes, last 6 digits.
    String secret = Totp.base32Encode("12345678901234567890".getBytes(StandardCharsets.US_ASCII));

    @Test void matchesRfc6238TestVectors() {
        assertThat(Totp.verify(secret, "287082", Instant.ofEpochSecond(59), 0)).isEqualTo(1);
        assertThat(Totp.verify(secret, "081804", Instant.ofEpochSecond(1111111109L), 0)).isEqualTo(37037036L);
        assertThat(Totp.verify(secret, "005924", Instant.ofEpochSecond(1234567890L), 0)).isEqualTo(41152263L);
    }
    @Test void acceptsOneStepOfClockDriftButNotMore() {
        assertThat(Totp.verify(secret, "081804", Instant.ofEpochSecond(1111111109L + 30), 0)).isPositive();
        assertThat(Totp.verify(secret, "081804", Instant.ofEpochSecond(1111111109L + 90), 0)).isEqualTo(-1);
    }
    @Test void usedCodeCannotBeReused() {
        assertThat(Totp.verify(secret, "081804", Instant.ofEpochSecond(1111111109L), 37037036L)).isEqualTo(-1);
    }
    @Test void rejectsMalformedCodes() {
        assertThat(Totp.verify(secret, "12345", Instant.now(), 0)).isEqualTo(-1);
        assertThat(Totp.verify(secret, "abcdef", Instant.now(), 0)).isEqualTo(-1);
        assertThat(Totp.verify(null, "123456", Instant.now(), 0)).isEqualTo(-1);
    }
    @Test void base32RoundTripAndUri() {
        String s = Totp.newSecret();
        assertThat(s).hasSize(32).matches("[A-Z2-7]+");
        assertThat(Totp.base32Encode(Totp.base32Decode(s))).isEqualTo(s);
        assertThat(Totp.uri("Hodinový majster", "a@b.sk", "ABC"))
                .startsWith("otpauth://totp/Hodinov%C3%BD%20majster:a%40b.sk?secret=ABC&issuer=Hodinov%C3%BD%20majster")
                .contains("digits=6").contains("period=30");
    }
}
