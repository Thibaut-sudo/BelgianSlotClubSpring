package org.example.belgianslotclubspring.controllers;

import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.services.QualifService;
import org.example.belgianslotclubspring.services.RaceResultService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * Contrôleur gérant le traitement des résultats de course pour une date et un club donnés.
 */
@Controller
public class RacedataController {

    private final RaceResultService raceResultService;
    private final QualifService qualifService;

    public RacedataController(RaceResultService raceResultService, QualifService qualifService) {
        this.raceResultService = raceResultService;
        this.qualifService = qualifService;
    }

    @PostMapping("/processRaceDate")
    public String processRaceDate(
            @RequestParam("raceDate") String raceDate,
            @RequestParam("club") String club,
            Model model
    ) {
        String clubCode = Club.requireCode(club);
        model.addAttribute("club", clubCode);

        if (raceDate != null && !raceDate.isEmpty()) {
            LocalDate selectedDate = LocalDate.parse(raceDate);

            model.addAttribute("raceResultDate", raceResultService.getRaceResultByDateAndClub(selectedDate, clubCode));
            model.addAttribute("qualiResult", qualifService.getQualifByDateAndClub(selectedDate, clubCode));
            model.addAttribute("raceDate", selectedDate);
        }

        return "pages/raceResult.html";
    }
}
