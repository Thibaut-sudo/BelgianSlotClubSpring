package org.example.belgianslotclubspring.controllers;

import org.example.belgianslotclubspring.entities.Qualif;
import org.example.belgianslotclubspring.entities.RaceResult;
import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.services.QualifService;
import org.example.belgianslotclubspring.services.RaceDayRecapService;
import org.example.belgianslotclubspring.services.RaceResultService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

/**
 * Contrôleur gérant le traitement des résultats de course pour une date et un club donnés.
 */
@Controller
public class RacedataController {

    private final RaceResultService raceResultService;
    private final QualifService qualifService;
    private final RaceDayRecapService raceDayRecapService;

    public RacedataController(
            RaceResultService raceResultService,
            QualifService qualifService,
            RaceDayRecapService raceDayRecapService
    ) {
        this.raceResultService = raceResultService;
        this.qualifService = qualifService;
        this.raceDayRecapService = raceDayRecapService;
    }

    @PostMapping("/processRaceDate")
    public String processRaceDatePost(
            @RequestParam("raceDate") String raceDate,
            @RequestParam("club") String club,
            @RequestParam(value = "category", required = false) String category,
            Model model
    ) {
        return showRaceDay(raceDate, club, category, model);
    }

    @GetMapping("/processRaceDate")
    public String processRaceDateGet(
            @RequestParam("raceDate") String raceDate,
            @RequestParam("club") String club,
            @RequestParam(value = "category", required = false) String category,
            Model model
    ) {
        return showRaceDay(raceDate, club, category, model);
    }

    private String showRaceDay(String raceDate, String club, String category, Model model) {
        String clubCode = Club.requireCode(club);
        model.addAttribute("club", clubCode);
        model.addAttribute("clubDisplayName",
                Club.fromCode(clubCode).map(Club::getDisplayName).orElse(clubCode));

        if (raceDate != null && !raceDate.isEmpty()) {
            LocalDate selectedDate = LocalDate.parse(raceDate);
            List<RaceResult> raceResults = (category != null && !category.isBlank())
                    ? raceResultService.getRaceResultByDateClubAndCategory(selectedDate, clubCode, category)
                    : raceResultService.getRaceResultByDateAndClub(selectedDate, clubCode);
            List<Qualif> qualiResults = qualifService.getQualifByDateAndClub(selectedDate, clubCode);

            model.addAttribute("raceResultDate", raceResults);
            model.addAttribute("qualiResult", qualiResults);
            model.addAttribute("raceDate", selectedDate);
            model.addAttribute("raceCategory", category);
            model.addAttribute("recap", raceDayRecapService.build(qualiResults, raceResults));

            if (!raceResults.isEmpty()) {
                model.addAttribute("winnerName", raceResults.get(0).getNom());
            }
        }

        return "pages/raceResult.html";
    }
}
