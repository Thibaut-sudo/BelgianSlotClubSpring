package org.example.belgianslotclubspring.controllers;

import org.example.belgianslotclubspring.entities.Rallye;
import org.example.belgianslotclubspring.entities.RallyePilot;
import org.example.belgianslotclubspring.entities.RallyeStageTime;
import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.models.RallyeStandingRow;
import org.example.belgianslotclubspring.services.ImportAuthService;
import org.example.belgianslotclubspring.services.RallyeService;
import org.example.belgianslotclubspring.utils.RallyeSheetQr;
import org.example.belgianslotclubspring.utils.RallyeTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/rallye")
public class RallyeController {

    private final RallyeService rallyeService;
    private final ImportAuthService importAuthService;

    public RallyeController(RallyeService rallyeService, ImportAuthService importAuthService) {
        this.rallyeService = rallyeService;
        this.importAuthService = importAuthService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String club, Model model) {
        if (club == null || club.isBlank()) {
            return "redirect:/?goto=rallye#clubs";
        }
        String clubCode = Club.requireCode(club);
        Club clubEnum = Club.fromCode(clubCode).orElseThrow();

        model.addAttribute("club", clubCode);
        model.addAttribute("clubDisplayName", clubEnum.getDisplayName());
        model.addAttribute("rallyOnly", clubEnum.isRallyOnly());
        model.addAttribute("rallyes", rallyeService.listByClub(clubCode));
        return "pages/rallyeList";
    }

    @PostMapping("/create")
    public String create(
            @RequestParam String club,
            @RequestParam String name,
            @RequestParam String date,
            @RequestParam(required = false, defaultValue = "4") int boucles,
            @RequestParam(required = false, defaultValue = "5") int stagesPerBoucle,
            RedirectAttributes redirectAttributes
    ) {
        String clubCode = Club.requireCode(club);
        try {
            Rallye rallye = rallyeService.create(
                    name,
                    LocalDate.parse(date),
                    clubCode,
                    boucles,
                    stagesPerBoucle
            );
            redirectAttributes.addFlashAttribute("success", "Rallye créé.");
            return "redirect:/rallye/" + rallye.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/rallye?club=" + clubCode;
        }
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable Long id,
            @RequestParam(required = false) Integer boucle,
            @RequestParam(required = false) Integer after,
            @RequestParam(required = false) String category,
            Model model
    ) {
        Rallye rallye = rallyeService.get(id);
        int activeBoucle = boucle != null ? boucle : 1;
        if (activeBoucle < 1) {
            activeBoucle = 1;
        }
        if (activeBoucle > rallye.getBoucleCount()) {
            activeBoucle = rallye.getBoucleCount();
        }

        int afterStages = after != null ? after : rallye.totalStages();
        List<RallyeStandingRow> standings = rallyeService.standings(id, afterStages, category);

        Set<String> categories = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (RallyePilot p : rallye.getPilots()) {
            if (p.getCategory() != null && !p.getCategory().isBlank()) {
                categories.add(p.getCategory().trim());
            }
        }

        // Clés String "pilotId_stage" : SpEL/Thymeleaf casse souvent Map<Integer,*> (Long ≠ Integer).
        Map<String, String> timeDisplay = new LinkedHashMap<>();
        for (RallyePilot pilot : rallye.getPilots()) {
            for (int s = 1; s <= rallye.getStagesPerBoucle(); s++) {
                timeDisplay.put(pilot.getId() + "_" + s,
                        RallyeTimeFormat.format(pilot.getStageSeconds(activeBoucle, s)));
            }
            timeDisplay.put(pilot.getId() + "_" + RallyeStageTime.PENALTY_STAGE,
                    RallyeTimeFormat.format(pilot.getPenaltySeconds(activeBoucle)));
        }

        List<Integer> checkpoints = new ArrayList<>();
        for (int b = 1; b <= rallye.getBoucleCount(); b++) {
            checkpoints.add(b * rallye.getStagesPerBoucle());
        }

        model.addAttribute("rallye", rallye);
        model.addAttribute("club", rallye.getClubName());
        model.addAttribute("clubDisplayName",
                Club.fromCode(rallye.getClubName()).map(Club::getDisplayName).orElse(rallye.getClubName()));
        model.addAttribute("activeBoucle", activeBoucle);
        model.addAttribute("afterStages", afterStages);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("categories", categories);
        model.addAttribute("checkpoints", checkpoints);
        model.addAttribute("standings", standings);
        model.addAttribute("timeDisplay", timeDisplay);
        model.addAttribute("recaps", rallyeService.buildRecaps(id));
        return "pages/rallyeDetail";
    }

    @PostMapping("/{id}/pilots")
    public String addPilot(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String car,
            @RequestParam(required = false) String category,
            RedirectAttributes redirectAttributes
    ) {
        try {
            rallyeService.addPilot(id, name, car, category);
            redirectAttributes.addFlashAttribute("success", "Pilote ajouté.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/rallye/" + id + "#pilotes";
    }

    @PostMapping("/{id}/pilots/{pilotId}/update")
    public String updatePilot(
            @PathVariable Long id,
            @PathVariable Long pilotId,
            @RequestParam String name,
            @RequestParam(required = false) String car,
            @RequestParam(required = false) String category,
            RedirectAttributes redirectAttributes
    ) {
        try {
            rallyeService.updatePilot(id, pilotId, name, car, category);
            redirectAttributes.addFlashAttribute("success", "Pilote mis à jour.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/rallye/" + id + "#pilotes";
    }

    @PostMapping("/{id}/pilots/{pilotId}/delete")
    public String deletePilot(
            @PathVariable Long id,
            @PathVariable Long pilotId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            rallyeService.removePilot(id, pilotId);
            redirectAttributes.addFlashAttribute("success", "Pilote supprimé.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/rallye/" + id + "#pilotes";
    }

    @PostMapping("/{id}/times")
    public String saveTimes(
            @PathVariable Long id,
            @RequestParam int boucle,
            @RequestParam(required = false, defaultValue = "saisie") String returnTo,
            @RequestParam Map<String, String> allParams,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Map<Long, Map<Integer, String>> times = parseTimeParams(allParams);
            int n = rallyeService.patchBoucleTimes(id, boucle, times);
            redirectAttributes.addFlashAttribute("success",
                    n == 0 ? "Aucun temps modifié."
                            : (n == 1 ? "1 temps enregistré." : n + " temps enregistrés."));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        if ("grilles".equalsIgnoreCase(returnTo)) {
            return "redirect:/rallye/" + id + "/grilles?boucle=" + boucle;
        }
        return "redirect:/rallye/" + id + "?boucle=" + boucle + "#saisie";
    }

    /**
     * Enregistrement partiel (AJAX) : seules les cases envoyées sont écrites.
     * Permet à plusieurs personnes de saisir sans s’écraser.
     */
    @PostMapping("/{id}/api/times")
    @ResponseBody
    public Map<String, Object> patchTimesApi(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        try {
            Object boucleRaw = body.get("boucle");
            if (boucleRaw == null) {
                throw new IllegalArgumentException("boucle obligatoire");
            }
            int boucle = boucleRaw instanceof Number
                    ? ((Number) boucleRaw).intValue()
                    : Integer.parseInt(String.valueOf(boucleRaw));

            @SuppressWarnings("unchecked")
            Map<String, Object> rawTimes = body.get("times") instanceof Map
                    ? (Map<String, Object>) body.get("times")
                    : Map.of();

            Map<String, String> flat = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : rawTimes.entrySet()) {
                flat.put(e.getKey(), e.getValue() == null ? "" : String.valueOf(e.getValue()));
            }
            Map<Long, Map<Integer, String>> times = parseTimeParams(flat);
            for (Map.Entry<String, String> e : flat.entrySet()) {
                String key = e.getKey();
                if (key.startsWith("time_")) {
                    continue;
                }
                String[] parts = key.split("_");
                if (parts.length != 2) {
                    continue;
                }
                try {
                    Long pilotId = Long.parseLong(parts[0]);
                    int stage = Integer.parseInt(parts[1]);
                    times.computeIfAbsent(pilotId, k -> new HashMap<>()).put(stage, e.getValue());
                } catch (NumberFormatException ignored) {
                    // ignore
                }
            }

            int n = rallyeService.patchBoucleTimes(id, boucle, times);
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("ok", true);
            res.put("saved", n);
            res.put("boucle", boucle);
            return res;
        } catch (Exception e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("ok", false);
            err.put("error", e.getMessage() != null ? e.getMessage() : "Erreur");
            return err;
        }
    }

    private static Map<Long, Map<Integer, String>> parseTimeParams(Map<String, String> allParams) {
        Map<Long, Map<Integer, String>> times = new HashMap<>();
        if (allParams == null) {
            return times;
        }
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("time_")) {
                continue;
            }
            String[] parts = key.split("_");
            if (parts.length != 3) {
                continue;
            }
            try {
                Long pilotId = Long.parseLong(parts[1]);
                int stage = Integer.parseInt(parts[2]);
                times.computeIfAbsent(pilotId, k -> new HashMap<>()).put(stage, entry.getValue());
            } catch (NumberFormatException ignored) {
                // ignore
            }
        }
        return times;
    }

    @PostMapping("/{id}/import-pilots")
    public String importPilots(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "password", required = false) String password,
            RedirectAttributes redirectAttributes
    ) {
        if (!importAuthService.matches(password)) {
            redirectAttributes.addFlashAttribute("error",
                    "Mot de passe incorrect. L'import a été refusé.");
            return "redirect:/rallye/" + id + "#pilotes";
        }
        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Choisissez un fichier Excel.");
            return "redirect:/rallye/" + id + "#pilotes";
        }
        try {
            Path temp = Files.createTempFile("rallye-import-", ".xlsx");
            file.transferTo(temp.toFile());
            var result = rallyeService.importPilotsFromExcel(id, temp.toString());
            Files.deleteIfExists(temp);
            redirectAttributes.addFlashAttribute("success", result.successMessage());
            if (result.timesImported() > 0) {
                return "redirect:/rallye/" + id + "#classement";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/rallye/" + id + "#pilotes";
    }

    @PostMapping("/{id}/add-boucle")
    public String addBoucle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Rallye rallye = rallyeService.addBoucle(id);
            redirectAttributes.addFlashAttribute("success",
                    "Boucle " + rallye.getBoucleCount() + " ajoutée.");
            return "redirect:/rallye/" + id + "?boucle=" + rallye.getBoucleCount() + "#saisie";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/rallye/" + id + "#saisie";
        }
    }

    @PostMapping("/{id}/add-stage")
    public String addStage(
            @PathVariable Long id,
            @RequestParam(required = false) Integer boucle,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Rallye rallye = rallyeService.addStage(id);
            int b = boucle != null ? boucle : 1;
            redirectAttributes.addFlashAttribute("success",
                    "ES " + rallye.getStagesPerBoucle() + " ajoutée.");
            return "redirect:/rallye/" + id + "?boucle=" + b + "#saisie";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/rallye/" + id + "#saisie";
        }
    }

    @PostMapping("/{id}/remove-boucle")
    public String removeBoucle(
            @PathVariable Long id,
            @RequestParam(required = false) Integer boucle,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Rallye before = rallyeService.get(id);
            int removed = before.getBoucleCount();
            Rallye rallye = rallyeService.removeBoucle(id);
            int stayOn = boucle != null && boucle <= rallye.getBoucleCount()
                    ? boucle
                    : rallye.getBoucleCount();
            redirectAttributes.addFlashAttribute("success",
                    "Boucle " + removed + " supprimée (temps associés effacés).");
            return "redirect:/rallye/" + id + "?boucle=" + stayOn + "#saisie";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/rallye/" + id + "#saisie";
        }
    }

    @PostMapping("/{id}/remove-stage")
    public String removeStage(
            @PathVariable Long id,
            @RequestParam(required = false) Integer boucle,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Rallye before = rallyeService.get(id);
            int removed = before.getStagesPerBoucle();
            rallyeService.removeStage(id);
            int b = boucle != null ? boucle : 1;
            redirectAttributes.addFlashAttribute("success",
                    "ES " + removed + " supprimée (temps associés effacés).");
            return "redirect:/rallye/" + id + "?boucle=" + b + "#saisie";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/rallye/" + id + "#saisie";
        }
    }

    @PostMapping("/{id}/finish")
    public String finish(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            rallyeService.finish(id);
            redirectAttributes.addFlashAttribute("success",
                    "Rallye terminé. Les informations ne peuvent plus être modifiées.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/rallye/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Rallye rallye = rallyeService.get(id);
        String club = rallye.getClubName();
        rallyeService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Rallye supprimé.");
        return "redirect:/rallye?club=" + club;
    }

    /**
     * Grilles de groupes imprimables (équivalent feuilles B1–B4 Excel).
     */
    @GetMapping("/{id}/grilles")
    public String grilles(
            @PathVariable Long id,
            @RequestParam(required = false) Integer boucle,
            Model model
    ) {
        Rallye rallye = rallyeService.get(id);
        int activeBoucle = boucle != null ? boucle : 1;
        if (activeBoucle < 1) {
            activeBoucle = 1;
        }
        if (activeBoucle > rallye.getBoucleCount()) {
            activeBoucle = rallye.getBoucleCount();
        }

        model.addAttribute("rallye", rallye);
        model.addAttribute("club", rallye.getClubName());
        model.addAttribute("clubDisplayName",
                Club.fromCode(rallye.getClubName()).map(Club::getDisplayName).orElse(rallye.getClubName()));
        model.addAttribute("activeBoucle", activeBoucle);
        model.addAttribute("grid", rallyeService.buildGroupGrid(id, activeBoucle));

        Map<String, String> timeDisplay = new LinkedHashMap<>();
        for (RallyePilot pilot : rallye.getPilots()) {
            for (int s = 1; s <= rallye.getStagesPerBoucle(); s++) {
                timeDisplay.put(pilot.getId() + "_" + s,
                        RallyeTimeFormat.format(pilot.getStageSeconds(activeBoucle, s)));
            }
        }
        model.addAttribute("timeDisplay", timeDisplay);
        return "pages/rallyeGrilles";
    }

    @PostMapping("/{id}/groups")
    public String saveGroups(
            @PathVariable Long id,
            @RequestParam int boucle,
            @RequestParam Map<String, String> allParams,
            RedirectAttributes redirectAttributes
    ) {
        try {
            List<List<Long>> groups = parseGroupsFromParams(allParams);
            rallyeService.saveGroupAssignments(id, boucle, groups);
            redirectAttributes.addFlashAttribute("success", "Composition des groupes enregistrée.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/rallye/" + id + "/grilles?boucle=" + boucle;
    }

    @PostMapping("/{id}/groups/reset")
    public String resetGroups(
            @PathVariable Long id,
            @RequestParam int boucle,
            RedirectAttributes redirectAttributes
    ) {
        try {
            rallyeService.clearGroupAssignments(id, boucle);
            redirectAttributes.addFlashAttribute("success", "Groupes remis sur la répartition automatique.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/rallye/" + id + "/grilles?boucle=" + boucle;
    }

    /** Params group_1=3,7 & group_2=4 … (ids séparés par virgule, ordre = ordre dans le groupe). */
    private static List<List<Long>> parseGroupsFromParams(Map<String, String> allParams) {
        TreeMap<Integer, List<Long>> byGroup = new TreeMap<>();
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("group_")) {
                continue;
            }
            int groupNum = Integer.parseInt(key.substring("group_".length()));
            List<Long> ids = new ArrayList<>();
            String raw = entry.getValue() == null ? "" : entry.getValue().trim();
            if (!raw.isEmpty()) {
                for (String part : raw.split(",")) {
                    if (!part.isBlank()) {
                        ids.add(Long.parseLong(part.trim()));
                    }
                }
            }
            byGroup.put(groupNum, ids);
        }
        if (byGroup.isEmpty()) {
            throw new IllegalArgumentException("Aucun groupe reçu.");
        }
        int max = byGroup.lastKey();
        List<List<Long>> groups = new ArrayList<>();
        for (int g = 1; g <= max; g++) {
            groups.add(byGroup.getOrDefault(g, List.of()));
        }
        return groups;
    }

    /**
     * Scan photo d'une feuille groupe (QR + OCR conservateur + validation).
     */
    @GetMapping("/{id}/scan")
    public String scan(
            @PathVariable Long id,
            @RequestParam(required = false) Integer boucle,
            Model model
    ) {
        Rallye rallye = rallyeService.get(id);
        int activeBoucle = boucle != null ? boucle : 1;
        if (activeBoucle < 1) {
            activeBoucle = 1;
        }
        if (activeBoucle > rallye.getBoucleCount()) {
            activeBoucle = rallye.getBoucleCount();
        }
        model.addAttribute("rallye", rallye);
        model.addAttribute("club", rallye.getClubName());
        model.addAttribute("clubDisplayName",
                Club.fromCode(rallye.getClubName()).map(Club::getDisplayName).orElse(rallye.getClubName()));
        model.addAttribute("activeBoucle", activeBoucle);
        return "pages/rallyeScan";
    }

    /**
     * Structure d'une feuille groupe pour le review après scan QR.
     */
    @GetMapping("/{id}/api/group-sheet")
    @ResponseBody
    public Map<String, Object> groupSheetApi(
            @PathVariable Long id,
            @RequestParam int boucle,
            @RequestParam int group
    ) {
        Rallye rallye = rallyeService.get(id);
        var grid = rallyeService.buildGroupGrid(id, boucle);
        var sheet = grid.groupSheets().stream()
                .filter(s -> s.groupNumber() == group)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Groupe introuvable: " + group));

        List<Map<String, Object>> stages = new ArrayList<>();
        for (var st : sheet.stages()) {
            List<Map<String, Object>> pilots = new ArrayList<>();
            for (var p : st.pilots()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", p.id());
                row.put("name", p.name());
                row.put("car", p.car());
                row.put("startNumber", p.startNumber());
                row.put("previousRank", p.previousRank());
                row.put("anchor", RallyeSheetQr.cellAnchor(p.id(), st.esNumber()));
                row.put("time", RallyeTimeFormat.format(
                        rallye.getPilots().stream()
                                .filter(rp -> rp.getId().equals(p.id()))
                                .findFirst()
                                .map(rp -> rp.getStageSeconds(boucle, st.esNumber()))
                                .orElse(null)
                ));
                pilots.add(row);
            }
            Map<String, Object> stage = new LinkedHashMap<>();
            stage.put("esNumber", st.esNumber());
            stage.put("passageOrder", st.passageOrder());
            stage.put("pilots", pilots);
            stages.add(stage);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("rallyeId", id);
        body.put("boucle", boucle);
        body.put("groupNumber", sheet.groupNumber());
        body.put("qrPayload", sheet.qrPayload());
        body.put("stages", stages);
        return body;
    }
}
