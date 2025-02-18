package org.example.belgianslotclubspring.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

/**
 * Contrôleur REST gérant la vérification d'un mot de passe sécurisé.
 * Il permet d'effectuer une vérification simple en comparant un mot de passe fourni
 * avec un mot de passe préconfiguré côté serveur.
 */
@RestController
@RequestMapping("/api")
public class SecurityController {

    // Mot de passe sécurisé défini en dur (idéalement à externaliser dans un fichier de configuration)
    private static final String SECRET_PASSWORD = "MonMotDePasseSecurity";

    /**
     * Vérifie si le mot de passe fourni correspond au mot de passe sécurisé.
     *
     * @param request Une requête contenant un champ "password" envoyé en JSON.
     * @return Une réponse JSON indiquant si l'authentification a réussi ou non.
     *         - 200 OK si le mot de passe est correct.
     *         - 401 Unauthorized si le mot de passe est incorrect.
     */
    @PostMapping("/verify-password")
    public ResponseEntity<Map<String, Boolean>> verifyPassword(@RequestBody Map<String, String> request) {
        // Récupération du mot de passe fourni dans la requête
        String password = request.get("password");

        // Vérification du mot de passe
        if (SECRET_PASSWORD.equals(password)) {
            return ResponseEntity.ok(Collections.singletonMap("success", true));
        }

        // Renvoie une réponse Unauthorized si le mot de passe est incorrect
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("success", false));
    }
}
