package org.example.belgianslotclubspring.controllers;

import org.example.belgianslotclubspring.services.QualifService;
import org.example.belgianslotclubspring.services.RaceResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
public class RacedataController {


    private final RaceResultService raceResultService;


    private final QualifService qualifService;

    public RacedataController (RaceResultService raceResultService, QualifService qualifService) {
        this.raceResultService = raceResultService;
        this.qualifService = qualifService;
    }

    @PostMapping("/processRaceDate")
    public String processRaceDate(@RequestParam("raceDate") String raceDate, Model model) {
        if (raceDate != null && !raceDate.isEmpty()) {
            LocalDate selectedDate = LocalDate.parse(raceDate);

            System.out.println(qualifService.getQualifByDate(selectedDate));

            for (int i = 0; i < qualifService.getQualifByDate(selectedDate).size(); i++) {
                System.out.println(qualifService.getQualifByDate(selectedDate).get(i).getBestTime());
            }

            model.addAttribute("raceResultDate", raceResultService.getRaceResultByDate(selectedDate));
            model.addAttribute("qualiResult", qualifService.getQualifByDate(selectedDate));

        }
        return "pages/raceResult.html";
    }
}
