package org.example.belgianslotclubspring.controllers;

import lombok.RequiredArgsConstructor;
import org.example.belgianslotclubspring.services.RaceResultService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import java.util.List;
import java.util.Map;

@Controller

@RequestMapping("/selectRace")
public class RaceController {
    private final RaceResultService raceResultService;

    public RaceController(RaceResultService raceResultService
    ) {
        this.raceResultService = raceResultService;
    }




    @GetMapping("/{club}")
    public String selectRace(@PathVariable String club, Model model) {



        Map<LocalDate, String> raceResultDate = raceResultService.getRaceResultDateByClub(club);



        List<String> listeCategorie = raceResultService.getAllCategoriesClub(club);
        List<String>listeAnnees = raceResultService.getAllYearsClub(club);

        model.addAttribute("club", club);
        model.addAttribute("listeCategorie", listeCategorie);
        model.addAttribute("raceResultDate", raceResultDate);
        model.addAttribute("listeAnnees", listeAnnees);

        return "pages/selectRace.html"; // Retourne la page Thymeleaf "selectRace.html"
    }
}
