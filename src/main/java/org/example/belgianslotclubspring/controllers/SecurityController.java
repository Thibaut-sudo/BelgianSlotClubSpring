package org.example.belgianslotclubspring.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SecurityController {

    private static final String SECRET_PASSWORD = "MonMotDePasseSecurise";

    @PostMapping("/verify-password")
    public ResponseEntity<Map<String, Boolean>> verifyPassword(@RequestBody Map<String, String> request) {
        String password = request.get("password");

        if (SECRET_PASSWORD.equals(password)) {
            return ResponseEntity.ok(Collections.singletonMap("success", true));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("success", false));
    }
}
