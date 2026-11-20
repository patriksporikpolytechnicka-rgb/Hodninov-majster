package sk.kasv.fx.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM for small secrets in the database (2FA secrets). Stored as "v1:" + Base64(iv + ciphertext + tag).
 * Key: TOTP_ENCRYPTION_KEY (Base64, 32 bytes) if set; otherwise derived from JWT_SECRET; otherwise random
 * (demo only, data is lost on restart anyway). Values without the prefix are old plain text and are read as they are.
 */
@Component
public class SecretCipher {
    private static final Logger log = LoggerFactory.getLogger(SecretCipher.class);
    private static final String PREFIX = "v1:";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static volatile SecretCipher instance;
    private final SecretKeySpec key;

    public SecretCipher(@Value("${app.totp.encryption-key:}") String configuredKey, @Value("${JWT_SECRET:}") String jwtSecret) {
        this.key = new SecretKeySpec(keyBytes(configuredKey, jwtSecret), "AES");
    }
    @PostConstruct void register() { instance = this; }
    /** For the JPA converter, which Hibernate may create without Spring. */
    static SecretCipher instance() {
        if (instance == null) throw new IllegalStateException("SecretCipher is not initialized");
        return instance;
    }

    public String encrypt(String plain) {
        if (plain == null) return null;
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (GeneralSecurityException e) { throw new IllegalStateException("Encryption failed", e); }
    }

    /** Returns null (and logs) when the value cannot be decrypted, e.g. after the key changed. */
    public String decrypt(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) return stored;
        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, all, 0, 12));
            return new String(cipher.doFinal(all, 12, all.length - 12), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.error("A stored secret could not be decrypted (was TOTP_ENCRYPTION_KEY or JWT_SECRET changed?)");
            return null;
        }
    }

    private static byte[] keyBytes(String configuredKey, String jwtSecret) {
        if (!configuredKey.isBlank()) {
            byte[] k = Base64.getDecoder().decode(configuredKey.strip());
            if (k.length != 32) throw new IllegalStateException("TOTP_ENCRYPTION_KEY must be 32 bytes in Base64");
            return k;
        }
        if (!jwtSecret.isBlank()) {
            log.warn("TOTP_ENCRYPTION_KEY is not set; the 2FA encryption key is derived from JWT_SECRET");
            try { return MessageDigest.getInstance("SHA-256").digest(("hodinovy-majster-totp:" + jwtSecret).getBytes(StandardCharsets.UTF_8)); }
            catch (GeneralSecurityException e) { throw new IllegalStateException(e); }
        }
        byte[] k = new byte[32];
        RANDOM.nextBytes(k);
        return k;
    }
}
