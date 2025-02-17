package org.example.belgianslotclubspring.controllers;

import org.example.belgianslotclubspring.services.QualifService;
import org.example.belgianslotclubspring.services.RaceResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
public class ChampionnatController {

    private final RaceResultService raceResultService;


    public ChampionnatController(RaceResultService raceResultService) {
        this.raceResultService = raceResultService;

    }

    @GetMapping("/championnat")
    public String getChampionnat(@RequestParam(value = "club", required = false) String club,
                                 @RequestParam(value = "categorie", required = false) String category,
                                 Model model) {

        // Récupération des catégories disponibles
        List<String> categories = raceResultService.getAllCategoriesClub(club);
        model.addAttribute("categories", categories);

        // Sélection de la première catégorie par défaut si aucune sélectionnée
        if (category == null && !categories.isEmpty()) {
            category = categories.get(0);
        }
        model.addAttribute("selectedCategory", category);

        // Récupération des dates des courses pour la catégorie sélectionnée
        List<String> raceDates = raceResultService.getRaceDates(category);
        model.addAttribute("raceDates", raceDates);

        // Récupération des résultats du championnat
        Map<LocalDate, Map<String, Double>> raceResults = raceResultService.getChampionshipResults(category);
        model.addAttribute("raceResults", raceResults);

        return "pages/championnat"; // Redirige vers la vue Thymeleaf "championnat.html"
    }
}
