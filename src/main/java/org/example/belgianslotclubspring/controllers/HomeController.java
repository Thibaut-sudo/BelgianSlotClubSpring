package org.example.belgianslotclubspring.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * L’accueil doit passer par un {@code @Controller} pour que {@link AccountModelAdvice}
 * injecte le compte dans la nav. Sans ça, Spring Boot sert {@code index} comme page
 * d’accueil générique et le lien reste « Compte » même connecté.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }
}
