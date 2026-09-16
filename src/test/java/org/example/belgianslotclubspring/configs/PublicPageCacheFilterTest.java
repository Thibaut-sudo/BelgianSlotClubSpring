package org.example.belgianslotclubspring.configs;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicPageCacheFilterTest {

    @Test
    void htmlPagesAreNotPubliclyCached() {
        MockHttpServletRequest home = new MockHttpServletRequest("GET", "/");
        home.setRequestURI("/");
        assertTrue(PublicPageCacheFilter.shouldNoStore(home));

        MockHttpServletRequest contact = new MockHttpServletRequest("GET", "/contact");
        contact.setRequestURI("/contact");
        assertTrue(PublicPageCacheFilter.shouldNoStore(contact));

        MockHttpServletRequest css = new MockHttpServletRequest("GET", "/css/main.css");
        css.setRequestURI("/css/main.css");
        assertFalse(PublicPageCacheFilter.shouldNoStore(css));
    }

    @Test
    void noStoreSurvivesACommittedResponse() throws Exception {
        PublicPageCacheFilter filter = new PublicPageCacheFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setRequestURI("/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            res.getWriter().write("<html>accueil</html>");
            res.flushBuffer();
        });

        assertEquals("private, no-store", response.getHeader("Cache-Control"));
        assertEquals("no-store", response.getHeader("CDN-Cache-Control"));
    }
}
