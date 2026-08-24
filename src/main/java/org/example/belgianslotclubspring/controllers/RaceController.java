package org.example.belgianslotclubspring.controllers;

import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.models.ClubCalendar;
import org.example.belgianslotclubspring.models.RaceSummary;
import org.example.belgianslotclubspring.services.RaceResultService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        String clubCode = Club.requireCode(club);
        Club clubEnum = Club.fromCode(clubCode).orElseThrow();

        List<RaceSummary> raceSummaries = raceResultService.getRaceSummariesByClub(clubCode);
        // Dérivé des résumés (évite un 2e passage DB + normalize)
        Map<LocalDate, String> raceResultDate = raceSummaries.stream()
                .collect(java.util.stream.Collectors.toMap(
                        RaceSummary::date,
                        RaceSummary::category,
                        (existing, replacement) -> replacement,
                        java.util.LinkedHashMap::new
                ));

        List<String> listeCategorie = raceResultService.getAllCategoriesClub(clubCode);
        List<String> listeAnnees = raceResultService.getAllYearsClub(clubCode);

        java.util.Map<String, String> events2025 = ClubCalendar.eventsFor(clubEnum);

        // Prochain événement du calendrier du club (jamais le fallback d'un autre club)
        LocalDate today = LocalDate.now();
        String nextEventDate = null;
        String nextEventName = null;
        long daysUntilNext = 0;
        boolean hasUpcomingEvent = false;

        for (java.util.Map.Entry<String, String> entry : events2025.entrySet()) {
            LocalDate eventDate = LocalDate.parse(entry.getKey());
            if (!eventDate.isBefore(today)) {
                nextEventDate = entry.getKey();
                nextEventName = entry.getValue();
                daysUntilNext = java.time.temporal.ChronoUnit.DAYS.between(today, eventDate);
                hasUpcomingEvent = true;
                break;
            }
        }

        // Saison terminée : dernier événement du calendrier de CE club
        if (nextEventDate == null && !events2025.isEmpty()) {
            java.util.Map.Entry<String, String> last = null;
            for (java.util.Map.Entry<String, String> entry : events2025.entrySet()) {
                last = entry;
            }
            if (last != null) {
                nextEventDate = last.getKey();
                nextEventName = last.getValue();
                daysUntilNext = 0;
            }
        }

        java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.FRENCH);
        String formattedDate = nextEventDate != null
                ? LocalDate.parse(nextEventDate).format(formatter)
                : "—";

        String eventType = nextEventName != null
                ? nextEventName.split(",")[0].trim().toLowerCase().replaceAll("[^a-z0-9]", "")
                : "";

        model.addAttribute("club", clubCode);
        model.addAttribute("clubDisplayName", clubEnum.getDisplayName());
        model.addAttribute("listeCategorie", listeCategorie);
        model.addAttribute("raceResultDate", raceResultDate);
        model.addAttribute("raceSummaries", raceSummaries);
        model.addAttribute("listeAnnees", listeAnnees);

        model.addAttribute("nextEventDate", formattedDate);
        model.addAttribute("nextEventName", nextEventName != null ? nextEventName : "Aucun événement");
        model.addAttribute("daysUntilNext", daysUntilNext);
        model.addAttribute("hasUpcomingEvent", hasUpcomingEvent);
        model.addAttribute("eventType", eventType);

        return "pages/selectRace.html";
    }

    @PostMapping("/{club}/delete")
    public String deleteRace(
            @PathVariable String club,
            @RequestParam LocalDate raceDate,
            @RequestParam String category,
            RedirectAttributes redirectAttributes
    ) {
        String clubCode = Club.requireCode(club);
        try {
            raceResultService.deleteRace(raceDate, clubCode, category);
            redirectAttributes.addFlashAttribute("success",
                    "Course supprimée : " + category + " du " + raceDate + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    e.getMessage() != null ? e.getMessage() : "Impossible de supprimer la course.");
        }
        return "redirect:/selectRace/" + clubCode;
    }
}
