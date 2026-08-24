package org.example.belgianslotclubspring.controllers;

import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.models.ClubCalendar;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Controller
public class ProchainEvenementController {

    @GetMapping("/prochain-evenement")
    public String prochainEvenement(
            @RequestParam(required = false) String club,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (club == null || club.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Sélectionnez d'abord un club.");
            return "redirect:/#clubs";
        }

        String clubCode = Club.requireCode(club);
        Club clubEnum = Club.fromCode(clubCode).orElseThrow();

        Map<String, String> events2025 = ClubCalendar.eventsFor(clubEnum);

        LocalDate today = LocalDate.now();
        String nextEventDate = null;
        String nextEventName = null;
        long daysUntilNext = 0;
        boolean hasUpcomingEvent = false;

        for (Map.Entry<String, String> entry : events2025.entrySet()) {
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
            Map.Entry<String, String> last = null;
            for (Map.Entry<String, String> entry : events2025.entrySet()) {
                last = entry;
            }
            if (last != null) {
                nextEventDate = last.getKey();
                nextEventName = last.getValue();
                daysUntilNext = 0;
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.FRENCH);
        String formattedDate = nextEventDate != null
                ? LocalDate.parse(nextEventDate).format(formatter)
                : "—";

        String eventType = nextEventName != null
                ? nextEventName.split(",")[0].trim().toLowerCase().replaceAll("[^a-z0-9]", "")
                : "";

        model.addAttribute("club", clubCode);
        model.addAttribute("clubDisplayName", clubEnum.getDisplayName());
        model.addAttribute("nextEventDate", formattedDate);
        model.addAttribute("nextEventName", nextEventName != null ? nextEventName : "Aucun événement");
        model.addAttribute("daysUntilNext", daysUntilNext);
        model.addAttribute("hasUpcomingEvent", hasUpcomingEvent);
        model.addAttribute("eventType", eventType);
        model.addAttribute("allEvents", events2025);

        return "pages/prochainEvenement";
    }
} 