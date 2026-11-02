package io.angistrem.employeemanagement.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordUtils {
    private static final String LEGACY_ALGO = "SHA-256";
    private static final String PBKDF2_ALGO = "PBKDF2WithHmacSHA256";
    private static final String PBKDF2_PREFIX = "pbkdf2";
    private static final int SALT_SIZE = 16;
    private static final int HASH_SIZE = 32;
    private static final int ITERATIONS = 120_000;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtils() {
    }

    public static String hash(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password must not be null");
        }

        try {
            byte[] salt = new byte[SALT_SIZE];
            SECURE_RANDOM.nextBytes(salt);
            byte[] hash = pbkdf2(rawPassword.toCharArray(), salt, ITERATIONS, HASH_SIZE);

            return PBKDF2_PREFIX + "$"
                    + ITERATIONS + "$"
                    + Base64.getEncoder().encodeToString(salt) + "$"
                    + Base64.getEncoder().encodeToString(hash);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 is not supported", e);
        }
    }

    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }

        if (storedHash.startsWith(PBKDF2_PREFIX + "$")) {
            return verifyPbkdf2(rawPassword, storedHash);
        }

        return verifyLegacySha256(rawPassword, storedHash);
    }

    public static boolean needsRehash(String storedHash) {
        return storedHash == null || !storedHash.startsWith(PBKDF2_PREFIX + "$");
    }

    private static boolean verifyPbkdf2(String rawPassword, String storedHash) {
        String[] parts = storedHash.split("\\$");
        if (parts.length != 4) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(rawPassword.toCharArray(), salt, iterations, expected.length);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException | GeneralSecurityException e) {
            return false;
        }
    }

    private static boolean verifyLegacySha256(String rawPassword, String storedHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance(LEGACY_ALGO);
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return MessageDigest.isEqual(
                    hex.toString().getBytes(StandardCharsets.UTF_8),
                    storedHash.getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not supported", e);
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBytes)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBytes * 8);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGO);
        return factory.generateSecret(spec).getEncoded();
    }
}
