package org.example.belgianslotclubspring.controllers;

import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.models.RallyeChampionshipTable;
import org.example.belgianslotclubspring.services.RaceResultService;
import org.example.belgianslotclubspring.services.RallyeChampionshipService;
import org.example.belgianslotclubspring.utils.CategoryNames;
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
    private final RallyeChampionshipService rallyeChampionshipService;

    public ChampionnatController(
            RaceResultService raceResultService,
            RallyeChampionshipService rallyeChampionshipService
    ) {
        this.raceResultService = raceResultService;
        this.rallyeChampionshipService = rallyeChampionshipService;
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
        if (clubEnum.isRallyOnly()) {
            RallyeChampionshipTable table = rallyeChampionshipService.build(clubCode, year, category);
            model.addAttribute("club", clubCode);
            model.addAttribute("clubDisplayName", clubEnum.getDisplayName());
            model.addAttribute("rallyOnly", true);
            model.addAttribute("table", table);
            model.addAttribute("selectedYear", table.year());
            model.addAttribute("selectedCategory", table.selectedCategory());
            model.addAttribute("categories", table.categories());
            return "pages/championnatRallye";
        }

        model.addAttribute("club", clubCode);
        model.addAttribute("clubDisplayName", clubEnum.getDisplayName());

        List<Integer> years = raceResultService.getAvailableYears(clubCode);
        model.addAttribute("years", years);

        if (year == null) {
            year = years.isEmpty() ? LocalDate.now().getYear() : years.get(years.size() - 1);
        }
        model.addAttribute("selectedYear", year);

        List<String> categories = raceResultService.getAllCategoriesClub(clubCode, year);
        model.addAttribute("categories", categories);

        String requestedCategory = category;
        String selectedCategory = categories.stream()
                .filter(c -> CategoryNames.same(c, requestedCategory))
                .findFirst()
                .orElse(categories.isEmpty() ? null : categories.getFirst());
        model.addAttribute("selectedCategory", selectedCategory);

        Map<LocalDate, Map<String, Double>> raceResults =
                (selectedCategory == null)
                        ? Map.of()
                        : raceResultService.getChampionshipResults(selectedCategory, clubCode, year);
        model.addAttribute("raceResults", raceResults);

        return "pages/championnat";
    }
}
