package org.example.belgianslotclubspring.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountServiceTest {

    @Test
    void normalizeEmailRejectsInvalidAddresses() {
        assertEquals("thibaut@example.com", AccountService.normalizeEmail("  Thibaut@Example.com "));
        assertThrows(IllegalArgumentException.class, () -> AccountService.normalizeEmail("pas-un-email"));
        assertThrows(IllegalArgumentException.class, () -> AccountService.normalizeEmail(""));
    }

    @Test
    void accountPasswordMustBeConfirmedAndLongEnough() {
        assertThrows(IllegalArgumentException.class,
                () -> AccountService.requireAccountPassword("12345", "12345"));
        assertThrows(IllegalArgumentException.class,
                () -> AccountService.requireAccountPassword("secret6", "other6"));
        AccountService.requireAccountPassword("secret6", "secret6");
    }

    @Test
    void gmailAdminEmailIsRecognized() {
        AccountService service = new AccountService(null, "thibaut.lenertz@gmail.com");
        assertTrue(service.isAdminEmail("Thibaut.Lenertz@gmail.com"));
        assertTrue(service.isAdminEmail("thibaut.lenertz@gmail.com"));
        assertFalse(service.isAdminEmail("autre@example.com"));
        assertFalse(service.isAdminEmail(null));
    }

    @Test
    void rememberCookieParsesAccountIdAndToken() {
        AccountService.RememberCookie parsed = AccountService.parseRememberCookie("12.aabbccddeeff0011");
        assertEquals(12L, parsed.accountId());
        assertEquals("aabbccddeeff0011", parsed.token());
        assertEquals(null, AccountService.parseRememberCookie(null));
        assertEquals(null, AccountService.parseRememberCookie("abc"));
        assertEquals(null, AccountService.parseRememberCookie("0.aabbccddeeff0011"));
        assertEquals(null, AccountService.parseRememberCookie("12.short"));
    }
}
