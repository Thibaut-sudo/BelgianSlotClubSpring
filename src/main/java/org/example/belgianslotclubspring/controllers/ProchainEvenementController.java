package org.example.belgianslotclubspring.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Controller
public class ProchainEvenementController {

    @GetMapping("/prochain-evenement")
    public String prochainEvenement(@RequestParam(defaultValue = "slot4000") String club, Model model) {
        // Données des événements selon le club
        Map<String, String> events2025 = new TreeMap<>();
        
        if ("slot4000".equals(club)) {
            // Données des événements SLOT4000 2025 (corrigées selon le calendrier officiel)
            events2025.put("2025-01-10", "GT32");
            events2025.put("2025-01-24", "GR5");
            events2025.put("2025-01-31", "PROTO24");
            events2025.put("2025-02-07", "PROTO32, GT32");
            events2025.put("2025-02-14", "TCR-SCALE, PROTO24");
            events2025.put("2025-02-21", "SLOT.IT, SCALEAUTO");
            events2025.put("2025-02-22", "BPC");
            events2025.put("2025-02-23", "BPC");
            events2025.put("2025-02-28", "GT24");
            events2025.put("2025-03-07", "GT32");
            events2025.put("2025-03-21", "GR5");
            events2025.put("2025-03-27", "1000KMS");
            events2025.put("2025-03-28", "1000KMS");
            events2025.put("2025-03-29", "1000KMS");
            events2025.put("2025-04-04", "PROTO32");
            events2025.put("2025-04-11", "GT24");
            events2025.put("2025-04-18", "SLOT.IT");
            events2025.put("2025-04-25", "PROTO24");
            events2025.put("2025-05-02", "GT32");
            events2025.put("2025-05-09", "TCR-SCALE");
            events2025.put("2025-05-16", "GR5");
            events2025.put("2025-05-30", "PROTO32");
            events2025.put("2025-06-06", "PROTO24");
            events2025.put("2025-06-13", "SLOT.IT");
            events2025.put("2025-06-20", "TCR-SCALE");
            events2025.put("2025-06-27", "GT32");
            events2025.put("2025-09-05", "GR5");
            events2025.put("2025-09-12", "PROTO24");
            events2025.put("2025-09-19", "PROTO32");
            events2025.put("2025-09-26", "GT24");
            events2025.put("2025-10-03", "SLOT.IT");
            events2025.put("2025-10-10", "TCR-SCALE");
            events2025.put("2025-10-17", "GT32");
            events2025.put("2025-10-24", "PROTO24");
            events2025.put("2025-10-31", "GR5");
            events2025.put("2025-11-07", "GT24");
            events2025.put("2025-11-14", "PROTO24");
            events2025.put("2025-11-21", "TCR-SCALE");
            events2025.put("2025-11-27", "GT24, BEL LMS");
            events2025.put("2025-12-05", "PROTO24");
            events2025.put("2025-12-12", "SLOT.IT");
            events2025.put("2025-12-19", "GT24");
            events2025.put("2025-12-26", "PROTO32");
        } else if ("srcs".equals(club)) {
            // Données des événements SRCS 2025 (selon le calendrier officiel SRCS)
            events2025.put("2025-01-04", "Revoslot");
            events2025.put("2025-01-11", "BEL-LMS S.R.C.S");
            events2025.put("2025-01-18", "BPC D.S.C.A");
            events2025.put("2025-01-21", "GT24");
            events2025.put("2025-01-25", "GT 24");
            events2025.put("2025-01-28", "BRM");
            events2025.put("2025-02-08", "BEL-LMS Chimay");
            events2025.put("2025-02-15", "Revo S4all");
            events2025.put("2025-02-16", "BPC Chimay");
            events2025.put("2025-02-18", "Scaleauto");
            events2025.put("2025-02-23", "BPC Slot 4000");
            events2025.put("2025-02-25", "BRM");
            events2025.put("2025-03-04", "BPC");
            events2025.put("2025-03-11", "proto 24");
            events2025.put("2025-03-18", "Scaleauto");
            events2025.put("2025-03-22", "Scaleauto");
            events2025.put("2025-03-29", "BPC");
            events2025.put("2025-04-01", "Revoslot");
            events2025.put("2025-04-08", "GT 24");
            events2025.put("2025-04-12", "BEL-LMS fastlane");
            events2025.put("2025-04-15", "Proto 24");
            events2025.put("2025-04-25", "Proto 24");
            events2025.put("2025-05-06", "BRM");
            events2025.put("2025-05-11", "BPC Dulmen");
            events2025.put("2025-05-13", "Revoslot");
            events2025.put("2025-05-20", "GT 24");
            events2025.put("2025-05-24", "BEL-LMS D.S.C.A");
            events2025.put("2025-05-27", "Proto 24");
            events2025.put("2025-05-29", "BPC Stolberg");
            events2025.put("2025-06-03", "Revoslot");
            events2025.put("2025-06-07", "Revo Diepenbeek");
            events2025.put("2025-06-10", "BRM");
            events2025.put("2025-06-17", "Deph'one F1");
            events2025.put("2025-06-29", "BPC Stolberg");
            events2025.put("2025-09-01", "BPC");
            events2025.put("2025-09-07", "BPC S.R.C.S");
            events2025.put("2025-09-09", "Deph'one F1");
            events2025.put("2025-09-16", "Revoslot");
            events2025.put("2025-09-20", "Revo S.R.C.S");
            events2025.put("2025-09-23", "BRM");
            events2025.put("2025-09-25", "Chall. E.Pirotte");
            events2025.put("2025-09-28", "BPC");
            events2025.put("2025-09-30", "GT 24");
            events2025.put("2025-10-02", "BPC M.R.T.U");
            events2025.put("2025-10-04", "BRM");
            events2025.put("2025-10-07", "Proto 24");
            events2025.put("2025-10-08", "Revo Eindhoven");
            events2025.put("2025-10-11", "Revoslot");
            events2025.put("2025-10-14", "GT 24");
            events2025.put("2025-10-18", "Deph'one F1");
            events2025.put("2025-10-25", "Chall. E.Pirotte");
            events2025.put("2025-10-26", "Chall. E. Pirotte");
            events2025.put("2025-10-28", "BPC");
            events2025.put("2025-11-01", "BPC M.R.T.U");
            events2025.put("2025-11-08", "Revo Eindhoven");
            events2025.put("2025-11-11", "Revoslot");
            events2025.put("2025-11-13", "Revo DSCA");
            events2025.put("2025-11-16", "BRM");
            events2025.put("2025-11-18", "Deph'one F1");
            events2025.put("2025-11-25", "Scaleauto");
            events2025.put("2025-11-26", "Scaleauto S.R.C.S");
            events2025.put("2025-12-02", "BPC");
            events2025.put("2025-12-09", "Revoslot");
            events2025.put("2025-12-13", "Revo DSCA");
            events2025.put("2025-12-16", "BRM");
            events2025.put("2025-12-20", "Revo S.R.C.S");
            events2025.put("2025-12-23", "BRM");
            events2025.put("2025-12-27", "BRM");
            events2025.put("2025-12-30", "GT 24");
        }

        // Trouver le prochain événement
        LocalDate today = LocalDate.now();
        String nextEventDate = null;
        String nextEventName = null;
        long daysUntilNext = 0;

        for (Map.Entry<String, String> entry : events2025.entrySet()) {
            LocalDate eventDate = LocalDate.parse(entry.getKey());
            if (eventDate.isAfter(today) || eventDate.isEqual(today)) {
                nextEventDate = entry.getKey();
                nextEventName = entry.getValue();
                daysUntilNext = java.time.temporal.ChronoUnit.DAYS.between(today, eventDate);
                break;
            }
        }

        // Si aucun événement futur trouvé, prendre le premier de l'année
        if (nextEventDate == null) {
            nextEventDate = "2025-01-10";
            nextEventName = "GT32";
            daysUntilNext = java.time.temporal.ChronoUnit.DAYS.between(today, LocalDate.parse(nextEventDate));
        }

        // Formater la date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.FRENCH);
        LocalDate eventDate = LocalDate.parse(nextEventDate);
        String formattedDate = eventDate.format(formatter);

        // Déterminer le type d'événement pour la couleur
        String eventType = nextEventName.split(",")[0].trim().toLowerCase().replaceAll("[^a-z0-9]", "");

        model.addAttribute("club", club);
        model.addAttribute("nextEventDate", formattedDate);
        model.addAttribute("nextEventName", nextEventName);
        model.addAttribute("daysUntilNext", daysUntilNext);
        model.addAttribute("eventType", eventType);
        model.addAttribute("allEvents", events2025);

        return "pages/prochainEvenement";
    }
} 