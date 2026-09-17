package org.example.belgianslotclubspring.services;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountCookiesTest {

    @Test
    void writesHttpOnlyRememberCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AccountCookies.write(request, response, "12.aabbccddeeff0011");

        var cookie = response.getCookie(AccountService.REMEMBER_COOKIE);
        assertEquals("12.aabbccddeeff0011", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertEquals("/", cookie.getPath());
        assertEquals(AccountService.REMEMBER_MAX_AGE, cookie.getMaxAge());
        assertEquals("Lax", cookie.getAttribute("SameSite"));
        assertFalse(cookie.getSecure());
    }

    @Test
    void httpsForwardedProtoMarksCookieSecure() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-Proto", "https");
        assertTrue(AccountCookies.isHttps(request));
    }
}
