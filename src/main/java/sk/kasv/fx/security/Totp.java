package sk.kasv.fx.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;

/**
 * Time-based one-time codes (RFC 6238) as used by Google Authenticator:
 * HMAC-SHA1, 6 digits, a new code every 30 seconds.
 */
public final class Totp {
    public static final int PERIOD_SECONDS = 30;
    public static final int DIGITS = 6;
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom RANDOM = new SecureRandom();
    private Totp() {}

    /** A new 160-bit secret in Base32, the format authenticator apps expect. */
    public static String newSecret() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /** otpauth:// link; the frontend shows it as a QR code to scan. */
    public static String uri(String issuer, String account, String secret) {
        String label = encode(issuer) + ":" + encode(account);
        return "otpauth://totp/" + label + "?secret=" + secret + "&issuer=" + encode(issuer)
                + "&algorithm=SHA1&digits=" + DIGITS + "&period=" + PERIOD_SECONDS;
    }

    public static long step(Instant time) { return Math.floorDiv(time.getEpochSecond(), PERIOD_SECONDS); }

    /**
     * Checks the code for the current step and one step before and after (clock drift).
     * Returns the matching step, or -1. Steps up to lastUsedStep are refused, so a code works only once.
     */
    public static long verify(String secret, String code, Instant now, long lastUsedStep) {
        if (secret == null || code == null || !code.matches("\\d{" + DIGITS + "}")) return -1;
        byte[] key = base32Decode(secret);
        long current = step(now);
        for (long step = current - 1; step <= current + 1; step++) {
            if (step <= lastUsedStep) continue;
            byte[] expected = format(code(key, step)).getBytes(StandardCharsets.US_ASCII);
            if (MessageDigest.isEqual(expected, code.getBytes(StandardCharsets.US_ASCII))) return step;
        }
        return -1;
    }

    /** The code an authenticator app shows at the given time (used by tests and tools). */
    public static String codeAt(String secret, Instant time) { return format(code(base32Decode(secret), step(time))); }

    /** HOTP value (RFC 4226) for one time step. */
    static int code(byte[] key, long step) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(java.nio.ByteBuffer.allocate(8).putLong(step).array());
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);
            return binary % 1_000_000;
        } catch (GeneralSecurityException e) { throw new IllegalStateException(e); }
    }
    static String format(int code) { return String.format(Locale.ROOT, "%06d", code); }

    static String base32Encode(byte[] data) {
        StringBuilder out = new StringBuilder();
        int buffer = 0, bits = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff); bits += 8;
            while (bits >= 5) { out.append(ALPHABET.charAt((buffer >> (bits - 5)) & 31)); bits -= 5; }
        }
        if (bits > 0) out.append(ALPHABET.charAt((buffer << (5 - bits)) & 31));
        return out.toString();
    }
    static byte[] base32Decode(String text) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int buffer = 0, bits = 0;
        for (char c : text.toUpperCase(Locale.ROOT).replace("=", "").replace(" ", "").toCharArray()) {
            int value = ALPHABET.indexOf(c);
            if (value < 0) throw new IllegalArgumentException("Invalid Base32 secret");
            buffer = (buffer << 5) | value; bits += 5;
            if (bits >= 8) { out.write((buffer >> (bits - 8)) & 0xff); bits -= 8; }
        }
        return out.toByteArray();
    }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); }
}
