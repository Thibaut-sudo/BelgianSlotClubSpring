package org.example.belgianslotclubspring.controllers;

import org.example.belgianslotclubspring.entities.ClubCalendarEvent;
import org.example.belgianslotclubspring.models.CalendarCategory;
import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.services.ClubCalendarService;
import org.example.belgianslotclubspring.services.ImportAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/calendrier")
public class ClubCalendarEventController {

    private final ClubCalendarService clubCalendarService;
    private final ImportAuthService importAuthService;

    public ClubCalendarEventController(ClubCalendarService clubCalendarService,
                                       ImportAuthService importAuthService) {
        this.clubCalendarService = clubCalendarService;
        this.importAuthService = importAuthService;
    }

    @PostMapping("/{club}/events")
    public ResponseEntity<Map<String, Object>> save(@PathVariable String club,
                                                    @RequestBody Map<String, String> body) {
        Optional<Club> parsed = Club.fromCode(club);
        if (parsed.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!importAuthService.matches(body == null ? null : body.get("password"))) {
            return error(HttpStatus.UNAUTHORIZED, "Mot de passe incorrect.");
        }
        try {
            LocalDate date = parseDate(body.get("date"));
            ClubCalendarEvent saved = clubCalendarService.upsert(
                    parsed.get(), date, body.get("name"), body.get("color"));
            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("success", true);
            ok.put("date", saved.getEventDate().toString());
            ok.put("name", saved.getName());
            return ResponseEntity.ok(ok);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/events")
    public ResponseEntity<Map<String, Object>> saveAll(@RequestBody Map<String, String> body) {
        if (!importAuthService.matches(body == null ? null : body.get("password"))) {
            return error(HttpStatus.UNAUTHORIZED, "Mot de passe incorrect.");
        }
        try {
            LocalDate date = parseDate(body.get("date"));
            clubCalendarService.upsertAllClubs(date, body.get("name"), body.get("color"));
            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("success", true);
            ok.put("date", date.toString());
            ok.put("name", body.get("name"));
            return ResponseEntity.ok(ok);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/{club}/events/delete")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String club,
                                                      @RequestBody Map<String, String> body) {
        Optional<Club> parsed = Club.fromCode(club);
        if (parsed.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!importAuthService.matches(body == null ? null : body.get("password"))) {
            return error(HttpStatus.UNAUTHORIZED, "Mot de passe incorrect.");
        }
        try {
            LocalDate date = parseDate(body.get("date"));
            boolean removed = clubCalendarService.deleteCustom(parsed.get(), date);
            if (!removed) {
                return error(HttpStatus.NOT_FOUND, "Aucun événement ajouté à supprimer pour ce jour.");
            }
            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("success", true);
            ok.put("date", date.toString());
            return ResponseEntity.ok(ok);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/events/delete")
    public ResponseEntity<Map<String, Object>> deleteAll(@RequestBody Map<String, String> body) {
        if (!importAuthService.matches(body == null ? null : body.get("password"))) {
            return error(HttpStatus.UNAUTHORIZED, "Mot de passe incorrect.");
        }
        try {
            LocalDate date = parseDate(body.get("date"));
            boolean removed = clubCalendarService.deleteCustomAllClubs(date);
            if (!removed) {
                return error(HttpStatus.NOT_FOUND, "Aucun événement ajouté à supprimer pour ce jour.");
            }
            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("success", true);
            ok.put("date", date.toString());
            return ResponseEntity.ok(ok);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/{club}/categories")
    public ResponseEntity<Map<String, Object>> saveCategory(@PathVariable String club,
                                                            @RequestBody Map<String, String> body) {
        Optional<Club> parsed = Club.fromCode(club);
        if (parsed.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!importAuthService.matches(body == null ? null : body.get("password"))) {
            return error(HttpStatus.UNAUTHORIZED, "Mot de passe incorrect.");
        }
        try {
            CalendarCategory saved = clubCalendarService.upsertCategory(
                    parsed.get(),
                    body.get("name"),
                    body.get("color"));
            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("success", true);
            ok.put("name", saved.name());
            ok.put("color", saved.color());
            return ResponseEntity.ok(ok);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/{club}/categories/delete")
    public ResponseEntity<Map<String, Object>> deleteCategory(@PathVariable String club,
                                                              @RequestBody Map<String, String> body) {
        Optional<Club> parsed = Club.fromCode(club);
        if (parsed.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!importAuthService.matches(body == null ? null : body.get("password"))) {
            return error(HttpStatus.UNAUTHORIZED, "Mot de passe incorrect.");
        }
        try {
            boolean removed = clubCalendarService.deleteCustomCategory(parsed.get(), body.get("name"));
            if (!removed) {
                return error(HttpStatus.NOT_FOUND, "Catégorie introuvable.");
            }
            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("success", true);
            ok.put("name", body.get("name"));
            return ResponseEntity.ok(ok);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Date obligatoire.");
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Date invalide.");
        }
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }
}
