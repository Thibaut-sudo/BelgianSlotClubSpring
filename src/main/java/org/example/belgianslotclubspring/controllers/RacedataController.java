package org.example.belgianslotclubspring.controllers;

import org.example.belgianslotclubspring.services.QualifService;
import org.example.belgianslotclubspring.services.RaceResultService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * Contrôleur gérant le traitement des résultats de course pour une date donnée.
 * Il permet de récupérer et afficher les résultats des qualifications et des courses
 * pour une date sélectionnée.
 */
@Controller
public class RacedataController {

    private final RaceResultService raceResultService;
    private final QualifService qualifService;

    /**
     * Constructeur injectant les services nécessaires au traitement des résultats de course.
     *
     * @param raceResultService Service permettant la récupération des résultats de courses.
     * @param qualifService     Service permettant la récupération des résultats de qualifications.
     */
    public RacedataController(RaceResultService raceResultService, QualifService qualifService) {
        this.raceResultService = raceResultService;
        this.qualifService = qualifService;
    }

    /**
     * Endpoint permettant de traiter une date de course sélectionnée et d'afficher les résultats correspondants.
     *
     * @param raceDate La date de la course sélectionnée sous forme de chaîne de caractères.
     * @param model    L'objet Model permettant de passer les données à la vue.
     * @return La page Thymeleaf "raceResult.html" affichant les résultats de la course et des qualifications.
     */
    @PostMapping("/processRaceDate")
    public String processRaceDate(@RequestParam("raceDate") String raceDate, Model model) {
        // Vérifie si la date fournie est valide
        if (raceDate != null && !raceDate.isEmpty()) {
            // Conversion de la date en objet LocalDate
            LocalDate selectedDate = LocalDate.parse(raceDate);

            // Affichage des résultats de qualification dans la console (debug)
            System.out.println(qualifService.getQualifByDate(selectedDate));
            qualifService.getQualifByDate(selectedDate).forEach(qualif ->
                    System.out.println(qualif.getBestTime())
            );

            // Ajout des résultats de course et de qualification au modèle pour la vue
            model.addAttribute("raceResultDate", raceResultService.getRaceResultByDate(selectedDate));
            model.addAttribute("qualiResult", qualifService.getQualifByDate(selectedDate));
        }

        // Retourne la vue Thymeleaf affichant les résultats de la course
        return "pages/raceResult.html";
    }
}
