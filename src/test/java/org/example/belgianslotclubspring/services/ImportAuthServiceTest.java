package org.example.belgianslotclubspring.services;

import org.example.belgianslotclubspring.services.AccountService.AccountView;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportAuthServiceTest {

    @Test
    void acceptsConfiguredPassword() {
        ImportAuthService service = new ImportAuthService("club-secret");
        assertTrue(service.matches("club-secret"));
    }

    @Test
    void rejectsWrongOrEmptyPassword() {
        ImportAuthService service = new ImportAuthService("club-secret");
        assertFalse(service.matches("wrong"));
        assertFalse(service.matches(""));
        assertFalse(service.matches(null));
    }

    @Test
    void rejectsEverythingWhenPasswordNotConfigured() {
        ImportAuthService service = new ImportAuthService("");
        assertFalse(service.matches(""));
        assertFalse(service.matches("anything"));
        assertFalse(new ImportAuthService("   ").matches("anything"));
    }

    @Test
    void authorizedAcceptsAdminSessionWithoutPassword() {
        ImportAuthService service = new ImportAuthService("club-secret");
        MockHttpSession session = new MockHttpSession();
        AccountService.login(session, new AccountView(1L, "Thibaut", "thibaut.lenertz@gmail.com", true));
        assertTrue(service.authorized(session, null));
        assertFalse(service.authorized(new MockHttpSession(), null));
        assertTrue(service.authorized(new MockHttpSession(), "club-secret"));
    }
}
