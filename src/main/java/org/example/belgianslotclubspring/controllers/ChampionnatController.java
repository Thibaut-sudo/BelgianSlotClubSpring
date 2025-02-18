package org.example.belgianslotclubspring.controllers;

import org.example.belgianslotclubspring.services.RaceResultService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur gérant l'affichage du classement du championnat.
 * Il récupère les informations depuis le service RaceResultService et les transmet à la vue Thymeleaf.
 */
@Controller
public class ChampionnatController {

    private final RaceResultService raceResultService;

    /**
     * Constructeur du contrôleur injectant le service de gestion des résultats de course.
     *
     * @param raceResultService Service permettant de récupérer les résultats de course.
     */
    public ChampionnatController(RaceResultService raceResultService) {
        this.raceResultService = raceResultService;
    }

    /**
     * Gère la requête GET pour afficher le classement du championnat.
     *
     * @param club     Nom du club sélectionné (optionnel).
     * @param category Catégorie sélectionnée (optionnelle).
     * @param model    Modèle pour transmettre les données à la vue.
     * @return Le nom de la vue Thymeleaf "championnat.html".
     */
    @GetMapping("/championnat")
    public String getChampionnat(@RequestParam(value = "club", required = false) String club,
                                 @RequestParam(value = "categorie", required = false) String category,
                                 Model model) {

        // Récupération des catégories disponibles pour le club donné
        List<String> categories = raceResultService.getAllCategoriesClub(club);
        model.addAttribute("categories", categories);

        // Sélection de la première catégorie par défaut si aucune n'est spécifiée
        if (category == null && !categories.isEmpty()) {
            category = categories.get(0);
        }
        model.addAttribute("selectedCategory", category);

        // Récupération des dates des courses pour la catégorie sélectionnée
        List<String> raceDates = raceResultService.getRaceDates(category);
        model.addAttribute("raceDates", raceDates);

        // Récupération des résultats du championnat sous forme de Map
        // Clé : Date de course | Valeur : Map contenant les pilotes et leurs scores
        Map<LocalDate, Map<String, Double>> raceResults = raceResultService.getChampionshipResults(category, club);
        model.addAttribute("raceResults", raceResults);

        // Redirige vers la page Thymeleaf "championnat.html"
        return "pages/championnat";
    }
}
