package org.example.belgianslotclubspring.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SellerPasswordsTest {

    @Test
    void hashesAreUniqueAndMatchOriginal() {
        String first = SellerPasswords.hash("secret4");
        String second = SellerPasswords.hash("secret4");
        assertNotEquals(first, second);
        assertTrue(SellerPasswords.matches(first, "secret4"));
        assertTrue(SellerPasswords.matches(second, "secret4"));
        assertFalse(SellerPasswords.matches(first, "secret5"));
        assertFalse(SellerPasswords.matches(first, null));
        assertFalse(SellerPasswords.matches(null, "secret4"));
        assertFalse(SellerPasswords.matches("", "secret4"));
        assertFalse(SellerPasswords.matches("not-a-hash", "secret4"));
    }

    @Test
    void requireMatchRejectsShortOrDifferentPasswords() {
        assertThrows(IllegalArgumentException.class, () -> SellerPasswords.require("abc"));
        assertThrows(IllegalArgumentException.class, () -> SellerPasswords.requireMatch("secret4", "other4"));
        SellerPasswords.requireMatch("secret4", "secret4");
    }
}
