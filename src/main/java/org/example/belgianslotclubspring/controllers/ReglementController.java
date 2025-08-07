package org.example.belgianslotclubspring.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReglementController {

    @GetMapping("/reglement")
    public String reglement(Model model) {
        // Ajouter des données au modèle si nécessaire
        model.addAttribute("pageTitle", "Règlement 2025 - Slot 4000");
        model.addAttribute("club", "slot4000");
        return "reglement";
    }
} 