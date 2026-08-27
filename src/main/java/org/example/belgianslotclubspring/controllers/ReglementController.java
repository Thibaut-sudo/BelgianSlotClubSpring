package org.example.belgianslotclubspring.controllers;

import org.example.belgianslotclubspring.models.Club;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ReglementController {

    @GetMapping("/reglement")
    public String reglement(
            @RequestParam(value = "club", required = false) String club,
            Model model
    ) {
        return show(club, model);
    }

    @GetMapping("/reglement/{club}")
    public String reglementClub(@PathVariable String club, Model model) {
        return show(club, model);
    }

    private String show(String club, Model model) {
        Club resolved = Club.fromCode(club).orElse(Club.SLOT4000);
        if (resolved.isRallyOnly()) {
            model.addAttribute("club", resolved.getCode());
            model.addAttribute("clubDisplayName", resolved.getDisplayName());
            return "pages/reglementRallye";
        }
        if (club != null && !club.isBlank() && !resolved.isSlot4000()) {
            return "redirect:/selectRace/" + resolved.getCode();
        }
        model.addAttribute("pageTitle", "Règlement 2026 - " + Club.SLOT4000.getDisplayName());
        model.addAttribute("club", Club.SLOT4000.getCode());
        model.addAttribute("clubDisplayName", Club.SLOT4000.getDisplayName());
        return "reglement";
    }
}
