package org.example.belgianslotclubspring.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Mot de passe d'annonce marketplace : sel aléatoire + SHA-256, jamais en clair.
 */
public final class SellerPasswords {

    public static final int MIN_LENGTH = 4;
    public static final int MAX_LENGTH = 80;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private SellerPasswords() {
    }

    public static String require(String raw) {
        String value = raw == null ? "" : raw;
        if (value.length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                    "Le mot de passe de l’annonce doit faire au moins " + MIN_LENGTH + " caractères.");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Le mot de passe de l’annonce est trop long.");
        }
        return value;
    }

    public static void requireMatch(String password, String confirm) {
        String value = require(password);
        if (!value.equals(confirm == null ? "" : confirm)) {
            throw new IllegalArgumentException("Les deux mots de passe ne correspondent pas.");
        }
    }

    public static String hash(String raw) {
        String password = require(raw);
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return HEX.formatHex(salt) + ":" + digest(salt, password);
    }

    public static boolean matches(String stored, String raw) {
        if (stored == null || stored.isBlank() || raw == null) {
            return false;
        }
        int sep = stored.indexOf(':');
        if (sep <= 0 || sep == stored.length() - 1) {
            return false;
        }
        try {
            byte[] salt = HEX.parseHex(stored.substring(0, sep));
            String expected = stored.substring(sep + 1);
            String actual = digest(salt, raw);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.US_ASCII),
                    actual.getBytes(StandardCharsets.US_ASCII)
            );
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String digest(byte[] salt, String password) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(salt);
            sha.update(password.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(sha.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible.", e);
        }
    }
}
