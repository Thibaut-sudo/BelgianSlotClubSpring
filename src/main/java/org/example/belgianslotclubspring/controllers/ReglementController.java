package org.example.belgianslotclubspring.controllers;

import org.example.belgianslotclubspring.models.Club;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReglementController {

    @GetMapping("/reglement")
    public String reglement(Model model) {
        // Règlement officiel actuellement disponible pour Slot 4000 uniquement
        Club club = Club.SLOT4000;
        model.addAttribute("pageTitle", "Règlement 2025 - " + club.getDisplayName());
        model.addAttribute("club", club.getCode());
        model.addAttribute("clubDisplayName", club.getDisplayName());
        return "reglement";
    }
} 