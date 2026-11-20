package sk.kasv.fx.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Encrypts a String column with SecretCipher on write and decrypts it on read. */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    @Override public String convertToDatabaseColumn(String value) { return value == null ? null : SecretCipher.instance().encrypt(value); }
    @Override public String convertToEntityAttribute(String value) { return value == null ? null : SecretCipher.instance().decrypt(value); }
}
