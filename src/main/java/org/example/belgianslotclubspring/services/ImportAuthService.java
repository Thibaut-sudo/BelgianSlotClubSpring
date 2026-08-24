package org.example.belgianslotclubspring.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Vérifie le mot de passe requis pour importer un fichier (Excel courses / rallye).
 */
@Service
public class ImportAuthService {

    private final String password;

    public ImportAuthService(@Value("${app.import.password:Test1234=}") String password) {
        this.password = password == null ? "" : password.trim();
    }

    public boolean matches(String submitted) {
        if (password.isEmpty()) {
            return false;
        }
        String value = submitted == null ? "" : submitted;
        return MessageDigest.isEqual(
                password.getBytes(StandardCharsets.UTF_8),
                value.getBytes(StandardCharsets.UTF_8)
        );
    }
}
