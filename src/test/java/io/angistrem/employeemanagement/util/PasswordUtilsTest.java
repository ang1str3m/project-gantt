package io.angistrem.employeemanagement.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilsTest {

    @Test
    void hashShouldUsePbkdf2Format() {
        String hash = PasswordUtils.hash("secret123");

        assertTrue(hash.startsWith("pbkdf2$"));
        assertTrue(PasswordUtils.matches("secret123", hash));
    }

    @Test
    void matchesShouldSupportLegacySha256Hash() throws Exception {
        String legacyHash = sha256Hex("legacy-pass");

        assertTrue(PasswordUtils.matches("legacy-pass", legacyHash));
        assertTrue(PasswordUtils.needsRehash(legacyHash));
    }

    @Test
    void hashShouldIncludeRandomSalt() {
        String hash1 = PasswordUtils.hash("same-password");
        String hash2 = PasswordUtils.hash("same-password");

        assertNotEquals(hash1, hash2);
        assertTrue(PasswordUtils.matches("same-password", hash1));
        assertTrue(PasswordUtils.matches("same-password", hash2));
    }

    @Test
    void matchesShouldRejectInvalidValue() {
        assertFalse(PasswordUtils.matches("password", null));
        assertFalse(PasswordUtils.matches("password", ""));
        assertFalse(PasswordUtils.matches("password", "pbkdf2$bad"));
    }

    private String sha256Hex(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
