package org.example.belgianslotclubspring.controllers;

import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.services.RaceResultService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String getChampionnat(
            @RequestParam(value = "club", required = false) String club,
            @RequestParam(value = "categorie", required = false) String category,
            @RequestParam(value = "year", required = false) Integer year,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (club == null || club.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Sélectionnez d'abord un club.");
            return "redirect:/#clubs";
        }

        String clubCode = Club.requireCode(club);
        Club clubEnum = Club.fromCode(clubCode).orElseThrow();

        model.addAttribute("club", clubCode);
        model.addAttribute("clubDisplayName", clubEnum.getDisplayName());

        List<Integer> years = raceResultService.getAvailableYears(clubCode);
        model.addAttribute("years", years);

        if (year == null) {
            year = years.isEmpty() ? LocalDate.now().getYear() : years.get(years.size() - 1);
        }
        model.addAttribute("selectedYear", year);

        List<String> categories = raceResultService.getAllCategoriesClub(clubCode);
        model.addAttribute("categories", categories);

        if (category == null && !categories.isEmpty()) {
            category = categories.getFirst();
        }
        model.addAttribute("selectedCategory", category);

        Map<LocalDate, Map<String, Double>> raceResults =
                (category == null)
                        ? Map.of()
                        : raceResultService.getChampionshipResults(category, clubCode, year);
        model.addAttribute("raceResults", raceResults);

        return "pages/championnat";
    }
}
