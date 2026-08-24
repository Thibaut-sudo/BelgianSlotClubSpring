package org.example.belgianslotclubspring.controllers;

import org.example.belgianslotclubspring.services.ImportAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

/**
 * Vérifie le mot de passe d'import (courses Excel, rallye Excel).
 */
@RestController
@RequestMapping("/api")
public class SecurityController {

    private final ImportAuthService importAuthService;

    public SecurityController(ImportAuthService importAuthService) {
        this.importAuthService = importAuthService;
    }

    /**
     * Vérifie si le mot de passe fourni autorise un import.
     *
     * @return 200 si correct, 401 sinon.
     */
    @PostMapping("/verify-password")
    public ResponseEntity<Map<String, Boolean>> verifyPassword(@RequestBody Map<String, String> request) {
        String password = request == null ? null : request.get("password");
        if (importAuthService.matches(password)) {
            return ResponseEntity.ok(Collections.singletonMap("success", true));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("success", false));
    }
}
