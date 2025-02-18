package org.example.belgianslotclubspring.controllers;

import org.example.belgianslotclubspring.services.RaceResultService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur gérant la sélection des courses pour un club donné.
 * Ce contrôleur permet de récupérer les courses enregistrées et d'afficher
 * les catégories et années disponibles pour un club.
 */
@Controller
@RequestMapping("/selectRace")
public class RaceController {

    private final RaceResultService raceResultService;

    /**
     * Constructeur injectant le service de gestion des résultats de courses.
     *
     * @param raceResultService Service permettant la récupération des résultats des courses.
     */
    public RaceController(RaceResultService raceResultService) {
        this.raceResultService = raceResultService;
    }

    /**
     * Endpoint permettant d'afficher la liste des courses disponibles pour un club donné.
     *
     * @param club  Le nom du club pour lequel les courses doivent être affichées.
     * @param model L'objet Model permettant de passer les données à la vue.
     * @return La page Thymeleaf "selectRace.html" affichant la liste des courses disponibles.
     */
    @GetMapping("/{club}")
    public String selectRace(@PathVariable String club, Model model) {
        // Récupération des dates des courses disponibles pour le club
        Map<LocalDate, String> raceResultDate = raceResultService.getRaceResultDateByClub(club);

        // Récupération des catégories disponibles pour le club
        List<String> listeCategorie = raceResultService.getAllCategoriesClub(club);

        // Récupération des années de course disponibles pour le club
        List<String> listeAnnees = raceResultService.getAllYearsClub(club);

        // Ajout des données au modèle pour les passer à la vue
        model.addAttribute("club", club);
        model.addAttribute("listeCategorie", listeCategorie);
        model.addAttribute("raceResultDate", raceResultDate);
        model.addAttribute("listeAnnees", listeAnnees);

        // Retourne la vue Thymeleaf "selectRace.html" pour affichage
        return "pages/selectRace.html";
    }
}
