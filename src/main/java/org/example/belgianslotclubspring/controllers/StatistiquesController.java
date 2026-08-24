package org.example.belgianslotclubspring.controllers;

import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.models.ClubRaceStats;
import org.example.belgianslotclubspring.services.ClubRaceStatsService;
import org.example.belgianslotclubspring.services.RaceResultService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class StatistiquesController {

    private final ClubRaceStatsService clubRaceStatsService;
    private final RaceResultService raceResultService;

    public StatistiquesController(
            ClubRaceStatsService clubRaceStatsService,
            RaceResultService raceResultService
    ) {
        this.clubRaceStatsService = clubRaceStatsService;
        this.raceResultService = raceResultService;
    }

    @GetMapping("/statistiques")
    public String statistiquesRedirect(
            @RequestParam(value = "club", required = false) String club,
            @RequestParam(value = "year", required = false) Integer year,
            RedirectAttributes redirectAttributes
    ) {
        if (club == null || club.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Sélectionnez d'abord un club.");
            return "redirect:/#clubs";
        }
        String clubCode = Club.requireCode(club);
        if (year != null) {
            return "redirect:/statistiques/" + clubCode + "?year=" + year;
        }
        return "redirect:/statistiques/" + clubCode;
    }

    @GetMapping("/statistiques/{club}")
    public String statistiques(
            @PathVariable String club,
            @RequestParam(value = "year", required = false) Integer year,
            Model model
    ) {
        String clubCode = Club.requireCode(club);
        Club clubEnum = Club.fromCode(clubCode).orElseThrow();

        List<Integer> years = raceResultService.getAvailableYears(clubCode);
        ClubRaceStats stats = clubRaceStatsService.build(clubCode, year);

        model.addAttribute("club", clubCode);
        model.addAttribute("clubDisplayName", clubEnum.getDisplayName());
        model.addAttribute("years", years);
        model.addAttribute("selectedYear", year);
        model.addAttribute("stats", stats);

        return "pages/statistiques";
    }
}
