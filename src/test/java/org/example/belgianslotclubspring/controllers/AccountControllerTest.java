package org.example.belgianslotclubspring.controllers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountControllerTest {

    @Test
    void safeNextRejectsOpenRedirects() {
        assertEquals("/compte", AccountController.safeNext(null));
        assertEquals("/compte", AccountController.safeNext("//evil.test"));
        assertEquals("/compte", AccountController.safeNext("https://evil.test"));
        assertEquals("/marketplace", AccountController.safeNext("/marketplace"));
        assertEquals("/marketplace/chat/abc", AccountController.safeNext("/marketplace/chat/abc"));
    }
}
