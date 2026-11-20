-- Encrypted 2FA secrets are longer than the plain Base32 text. Old plain values stay readable.
ALTER TABLE users ALTER COLUMN totp_secret TYPE varchar(255);
