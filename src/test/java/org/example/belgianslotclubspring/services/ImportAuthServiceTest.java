package org.example.belgianslotclubspring.services;

import org.junit.jupiter.api.Test;

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
}
