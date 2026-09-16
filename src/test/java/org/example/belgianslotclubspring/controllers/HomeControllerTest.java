package org.example.belgianslotclubspring.controllers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeControllerTest {

    @Test
    void homeUsesTheIndexTemplate() {
        assertEquals("index", new HomeController().home());
    }
}
