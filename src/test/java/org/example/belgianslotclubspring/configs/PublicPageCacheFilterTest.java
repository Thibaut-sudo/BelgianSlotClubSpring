package org.example.belgianslotclubspring.configs;

import org.example.belgianslotclubspring.services.AccountService;
import org.example.belgianslotclubspring.services.AccountService.AccountView;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicPageCacheFilterTest {

    @Test
    void loggedInSessionSkipsPublicCache() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        AccountService.login(session, new AccountView(1L, "Thibaut", "thibaut.lenertz@gmail.com", true));
        request.setSession(session);

        assertTrue(PublicPageCacheFilter.isLoggedIn(request));
        assertFalse(PublicPageCacheFilter.isLoggedIn(new MockHttpServletRequest()));
    }
}
