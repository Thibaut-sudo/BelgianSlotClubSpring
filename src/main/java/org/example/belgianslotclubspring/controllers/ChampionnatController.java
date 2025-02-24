package org.example.belgianslotclubspring.controllers;

import org.example.belgianslotclubspring.services.RaceResultService;
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
                                 @RequestParam(value = "year", required = false) Integer year,
                                 Model model) {

        // Liste des années disponibles
        List<Integer> years = raceResultService.getAvailableYears(club);
        model.addAttribute("years", years);

        // Sélection de l'année actuelle si aucune n'est spécifiée
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        model.addAttribute("selectedYear", year);

        // Récupération des catégories disponibles pour le club donné
        List<String> categories = raceResultService.getAllCategoriesClub(club);
        model.addAttribute("categories", categories);

        if (category == null && !categories.isEmpty()) {
            category = categories.getFirst();
        }
        model.addAttribute("selectedCategory", category);

        // Récupération des résultats pour l'année et la catégorie choisies
        Map<LocalDate, Map<String, Double>> raceResults = raceResultService.getChampionshipResults(category, club, year);
        model.addAttribute("raceResults", raceResults);

        return "pages/championnat";
    }
}
