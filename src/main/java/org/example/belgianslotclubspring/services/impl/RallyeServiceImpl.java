package org.example.belgianslotclubspring.services.impl;

import org.apache.poi.ss.usermodel.*;
import org.example.belgianslotclubspring.entities.Rallye;
import org.example.belgianslotclubspring.entities.RallyeGroupAssignment;
import org.example.belgianslotclubspring.entities.RallyePilot;
import org.example.belgianslotclubspring.entities.RallyeStageTime;
import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.models.RallyeBoucleGrid;
import org.example.belgianslotclubspring.models.RallyeGroupEsBlock;
import org.example.belgianslotclubspring.models.RallyeGroupSheet;
import org.example.belgianslotclubspring.models.RallyeGridPilot;
import org.example.belgianslotclubspring.models.RallyeBoucleRecap;
import org.example.belgianslotclubspring.models.RallyeFinalRecap;
import org.example.belgianslotclubspring.models.RallyeRecaps;
import org.example.belgianslotclubspring.models.RallyeScratchTally;
import org.example.belgianslotclubspring.models.RallyeStageHighlight;
import org.example.belgianslotclubspring.models.RallyeStandingRow;
import org.example.belgianslotclubspring.repo.RallyeGroupAssignmentRepo;
import org.example.belgianslotclubspring.repo.RallyePilotRepo;
import org.example.belgianslotclubspring.repo.RallyeRepo;
import org.example.belgianslotclubspring.services.RallyeService;
import org.example.belgianslotclubspring.utils.RallyeSheetQr;
import org.example.belgianslotclubspring.utils.RallyeTimeFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class RallyeServiceImpl implements RallyeService {

    private final RallyeRepo rallyeRepo;
    private final RallyePilotRepo pilotRepo;
    private final RallyeGroupAssignmentRepo groupAssignmentRepo;

    public RallyeServiceImpl(
            RallyeRepo rallyeRepo,
            RallyePilotRepo pilotRepo,
            RallyeGroupAssignmentRepo groupAssignmentRepo
    ) {
        this.rallyeRepo = rallyeRepo;
        this.pilotRepo = pilotRepo;
        this.groupAssignmentRepo = groupAssignmentRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Rallye> listByClub(String club) {
        return rallyeRepo.findByClubNameOrderByDateDesc(Club.requireCode(club));
    }

    @Override
    @Transactional(readOnly = true)
    public Rallye get(Long id) {
        // Ne pas remplacer pilots (Set) : avec orphanRemoval, setPilots(new …)
        // provoque "collection with orphan deletion was no longer referenced".
        // L'ordre vient de @OrderBy sur Rallye.pilots.
        return rallyeRepo.findDetailedById(id)
                .orElseThrow(() -> new NoSuchElementException("Rallye introuvable: " + id));
    }

    @Override
    public Rallye create(String name, LocalDate date, String club, Integer boucles, Integer stagesPerBoucle) {
        Rallye rallye = new Rallye(name.trim(), date, club);
        if (boucles != null && boucles > 0) {
            rallye.setBoucleCount(boucles);
        }
        if (stagesPerBoucle != null && stagesPerBoucle > 0) {
            rallye.setStagesPerBoucle(stagesPerBoucle);
        }
        return rallyeRepo.save(rallye);
    }

    @Override
    public void delete(Long id) {
        rallyeRepo.deleteById(id);
    }

    @Override
    public RallyePilot addPilot(Long rallyeId, String name, String car, String category) {
        Rallye rallye = get(rallyeId);
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException("Le nom du pilote est obligatoire");
        }
        int nextNumber = rallye.getPilots().stream()
                .map(RallyePilot::getStartNumber)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        RallyePilot pilot = new RallyePilot(
                cleanName,
                blankToNull(car),
                blankToNull(category),
                nextNumber
        );
        rallye.addPilot(pilot);
        return pilotRepo.save(pilot);
    }

    @Override
    public void updatePilot(Long rallyeId, Long pilotId, String name, String car, String category) {
        RallyePilot pilot = requirePilot(rallyeId, pilotId);
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException("Le nom du pilote est obligatoire");
        }
        pilot.setName(cleanName);
        pilot.setCar(blankToNull(car));
        pilot.setCategory(blankToNull(category));
    }

    @Override
    public void removePilot(Long rallyeId, Long pilotId) {
        RallyePilot pilot = requirePilot(rallyeId, pilotId);
        Rallye rallye = pilot.getRallye();
        rallye.getPilots().remove(pilot);
        pilotRepo.delete(pilot);
    }

    @Override
    public void saveBoucleTimes(Long rallyeId, int boucle, Map<Long, Map<Integer, String>> timesByPilot) {
        Rallye rallye = get(rallyeId);
        if (boucle < 1 || boucle > rallye.getBoucleCount()) {
            throw new IllegalArgumentException("Boucle invalide: " + boucle);
        }

        for (Map.Entry<Long, Map<Integer, String>> entry : timesByPilot.entrySet()) {
            RallyePilot pilot = requirePilot(rallyeId, entry.getKey());
            for (Map.Entry<Integer, String> stageEntry : entry.getValue().entrySet()) {
                int stage = stageEntry.getKey();
                if (stage != RallyeStageTime.PENALTY_STAGE
                        && (stage < 1 || stage > rallye.getStagesPerBoucle())) {
                    continue;
                }
                Double seconds;
                try {
                    seconds = RallyeTimeFormat.parse(stageEntry.getValue());
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(
                            "Temps invalide pour " + pilot.getName() + " (ES " + stage + "): "
                                    + stageEntry.getValue()
                    );
                }
                RallyeStageTime time = pilot.getOrCreateTime(boucle, stage);
                time.setTimeSeconds(seconds);
                if (seconds == null && time.getId() != null) {
                    // garder la ligne vide pour réaffichage, ou supprimer
                    time.setTimeSeconds(null);
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RallyeStandingRow> standings(Long rallyeId, Integer afterStages, String categoryFilter) {
        Rallye rallye = get(rallyeId);
        int stagesPerBoucle = rallye.getStagesPerBoucle();
        int expected = afterStages != null
                ? afterStages
                : rallye.totalStages();

        String category = blankToNull(categoryFilter);

        List<StandingAccumulator> rows = new ArrayList<>();
        for (RallyePilot pilot : rallye.getPilots()) {
            if (category != null) {
                String pilotCat = pilot.getCategory() == null ? "" : pilot.getCategory().trim();
                if (!category.equalsIgnoreCase(pilotCat)) {
                    continue;
                }
            }

            double total = 0;
            int completed = 0;
            boolean hasAny = false;

            outer:
            for (int b = 1; b <= rallye.getBoucleCount(); b++) {
                for (int s = 1; s <= stagesPerBoucle; s++) {
                    int globalIndex = (b - 1) * stagesPerBoucle + s;
                    if (globalIndex > expected) {
                        break outer;
                    }
                    Double t = pilot.getStageSeconds(b, s);
                    if (t != null) {
                        total += t;
                        completed++;
                        hasAny = true;
                    }
                }
                // PENO de la boucle si au moins une ES de la boucle est dans le périmètre
                int lastStageOfBoucle = b * stagesPerBoucle;
                if (lastStageOfBoucle <= expected || (b - 1) * stagesPerBoucle < expected) {
                    Double peno = pilot.getPenaltySeconds(b);
                    if (peno != null) {
                        total += peno;
                        hasAny = true;
                    }
                }
            }

            if (!hasAny) {
                rows.add(new StandingAccumulator(pilot, null, completed, expected));
            } else {
                rows.add(new StandingAccumulator(pilot, total, completed, expected));
            }
        }

        rows.sort(Comparator
                .comparing((StandingAccumulator a) -> a.total == null)
                .thenComparing(a -> a.total == null ? Double.MAX_VALUE : a.total)
                .thenComparing(a -> a.pilot.getStartNumber() == null ? Integer.MAX_VALUE : a.pilot.getStartNumber())
        );

        Double leader = rows.stream()
                .map(a -> a.total)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        List<RallyeStandingRow> result = new ArrayList<>();
        int pos = 1;
        for (StandingAccumulator a : rows) {
            Double gap = (a.total != null && leader != null) ? a.total - leader : null;
            result.add(new RallyeStandingRow(
                    a.total == null ? 0 : pos,
                    a.pilot.getId(),
                    a.pilot.getName(),
                    a.pilot.getCar(),
                    a.pilot.getCategory(),
                    a.total,
                    gap,
                    a.total == null ? "—" : RallyeTimeFormat.format(a.total),
                    (a.total == null || gap == null || gap == 0)
                            ? "—"
                            : RallyeTimeFormat.formatGap(gap),
                    a.completed,
                    a.expected
            ));
            if (a.total != null) {
                pos++;
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public RallyeRecaps buildRecaps(Long rallyeId) {
        Rallye rallye = get(rallyeId);
        List<RallyeBoucleRecap> boucles = new ArrayList<>();
        Map<Long, Integer> totalScratches = new HashMap<>();
        Map<Long, String> names = new HashMap<>();
        Map<Long, List<Integer>> scratchStagesGlobal = new HashMap<>();

        for (RallyePilot p : rallye.getPilots()) {
            names.put(p.getId(), p.getName());
            totalScratches.put(p.getId(), 0);
            scratchStagesGlobal.put(p.getId(), new ArrayList<>());
        }

        boolean allFinished = true;
        for (int b = 1; b <= rallye.getBoucleCount(); b++) {
            RallyeBoucleRecap recap = buildBoucleRecap(rallye, b);
            if (!recap.finished()) {
                allFinished = false;
            }
            // Uniquement boucles en cours ou terminées (au moins un temps)
            if (!recap.hasData()) {
                continue;
            }
            boucles.add(recap);
            for (RallyeScratchTally t : recap.scratchLeaders()) {
                Long id = findPilotIdByName(rallye, t.pilotName());
                if (id == null) {
                    continue;
                }
                totalScratches.merge(id, t.count(), Integer::sum);
                for (Integer es : t.stageNumbers()) {
                    scratchStagesGlobal.get(id).add((b - 1) * rallye.getStagesPerBoucle() + es);
                }
            }
        }

        // Résumé final seulement quand toutes les boucles sont terminées
        RallyeFinalRecap finale = null;
        if (allFinished && !boucles.isEmpty()) {
            finale = buildFinalRecap(rallye, totalScratches, names, scratchStagesGlobal, boucles);
        }
        return new RallyeRecaps(boucles, finale);
    }

    private RallyeBoucleRecap buildBoucleRecap(Rallye rallye, int boucle) {
        int stages = rallye.getStagesPerBoucle();
        List<RallyeStageHighlight> highlights = new ArrayList<>();
        Map<Long, Integer> scratchCount = new HashMap<>();
        Map<Long, List<Integer>> scratchEs = new HashMap<>();
        Map<Long, Double> boucleTotals = new HashMap<>();
        Map<Long, String> pilotNames = new HashMap<>();

        for (RallyePilot p : rallye.getPilots()) {
            pilotNames.put(p.getId(), p.getName());
            scratchCount.put(p.getId(), 0);
            scratchEs.put(p.getId(), new ArrayList<>());
            Double total = null;
            for (int s = 1; s <= stages; s++) {
                Double t = p.getStageSeconds(boucle, s);
                if (t != null) {
                    total = (total == null ? 0 : total) + t;
                }
            }
            Double peno = p.getPenaltySeconds(boucle);
            if (peno != null) {
                total = (total == null ? 0 : total) + peno;
            }
            if (total != null) {
                boucleTotals.put(p.getId(), total);
            }
        }

        int stagesWithTimes = 0;
        String hardestName = null;
        String hardestEs = null;
        String hardestGap = null;
        double hardestGapSec = -1;

        for (int s = 1; s <= stages; s++) {
            Long bestId = null;
            Double best = null;
            Long worstId = null;
            Double worst = null;

            for (RallyePilot p : rallye.getPilots()) {
                Double t = p.getStageSeconds(boucle, s);
                if (t == null) {
                    continue;
                }
                if (best == null || t < best) {
                    best = t;
                    bestId = p.getId();
                }
                if (worst == null || t > worst) {
                    worst = t;
                    worstId = p.getId();
                }
            }

            if (bestId == null) {
                continue;
            }
            stagesWithTimes++;
            scratchCount.merge(bestId, 1, Integer::sum);
            scratchEs.get(bestId).add(s);

            String worstGapFmt = "—";
            if (worstId != null && worst != null && best != null && !worstId.equals(bestId)) {
                double gap = worst - best;
                worstGapFmt = RallyeTimeFormat.formatGap(gap);
                if (gap > hardestGapSec) {
                    hardestGapSec = gap;
                    hardestName = pilotNames.get(worstId);
                    hardestEs = "ES " + s;
                    hardestGap = worstGapFmt;
                }
            }

            highlights.add(new RallyeStageHighlight(
                    s,
                    pilotNames.get(bestId),
                    RallyeTimeFormat.format(best),
                    worstId != null ? pilotNames.get(worstId) : "—",
                    worst != null ? RallyeTimeFormat.format(worst) : "—",
                    worstGapFmt
            ));
        }

        if (stagesWithTimes == 0) {
            return new RallyeBoucleRecap(
                    boucle, false, false, 0, null, null, List.of(), List.of(),
                    List.of("Pas encore de temps saisis pour la boucle " + boucle + ".")
            );
        }

        boolean finished = stagesWithTimes >= stages;

        List<RallyeScratchTally> scratchLeaders = scratchCount.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> {
                    int c = Integer.compare(b.getValue(), a.getValue());
                    if (c != 0) {
                        return c;
                    }
                    return pilotNames.getOrDefault(a.getKey(), "").compareToIgnoreCase(
                            pilotNames.getOrDefault(b.getKey(), ""));
                })
                .map(e -> new RallyeScratchTally(
                        pilotNames.get(e.getKey()),
                        e.getValue(),
                        List.copyOf(scratchEs.get(e.getKey()))
                ))
                .toList();

        Long leaderId = boucleTotals.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        String leaderName = leaderId != null ? pilotNames.get(leaderId) : null;
        String leaderTime = leaderId != null
                ? RallyeTimeFormat.format(boucleTotals.get(leaderId))
                : null;

        List<String> headlines = new ArrayList<>();
        if (leaderName != null) {
            if (finished) {
                headlines.add(leaderName + " remporte la boucle " + boucle + " en " + leaderTime + ".");
            } else {
                headlines.add(leaderName + " mène la boucle " + boucle + " en " + leaderTime
                        + " (" + stagesWithTimes + "/" + stages + " ES).");
            }
        }
        if (!scratchLeaders.isEmpty()) {
            RallyeScratchTally top = scratchLeaders.get(0);
            String stagesLabel = top.stageNumbers().stream()
                    .map(n -> "ES " + n)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            if (top.count() == 1) {
                headlines.add(top.pilotName() + " : 1 scratch (" + stagesLabel + ").");
            } else {
                headlines.add(top.pilotName() + " domine les scratchs : "
                        + top.count() + " (" + stagesLabel + ").");
            }
            if (scratchLeaders.size() > 1) {
                RallyeScratchTally second = scratchLeaders.get(1);
                headlines.add("Aussi en forme : " + second.pilotName()
                        + " (" + second.count() + " scratch"
                        + (second.count() > 1 ? "s" : "") + ").");
            }
        }
        if (hardestName != null && hardestGap != null) {
            headlines.add("Coup dur pour " + hardestName + " sur " + hardestEs
                    + " (" + hardestGap + " face au scratch).");
        }

        // Pénalités
        List<String> penos = new ArrayList<>();
        for (RallyePilot p : rallye.getPilots()) {
            Double peno = p.getPenaltySeconds(boucle);
            if (peno != null && peno > 0) {
                penos.add(p.getName() + " +" + RallyeTimeFormat.format(peno));
            }
        }
        if (!penos.isEmpty()) {
            headlines.add("Pénalités : " + String.join(", ", penos) + ".");
        }

        return new RallyeBoucleRecap(
                boucle,
                true,
                finished,
                stagesWithTimes,
                leaderName,
                leaderTime,
                scratchLeaders,
                highlights,
                headlines
        );
    }

    private RallyeFinalRecap buildFinalRecap(
            Rallye rallye,
            Map<Long, Integer> totalScratches,
            Map<Long, String> names,
            Map<Long, List<Integer>> scratchStagesGlobal,
            List<RallyeBoucleRecap> boucleRecaps
    ) {
        List<RallyeStandingRow> overall = standings(rallye.getId(), rallye.totalStages(), null);
        boolean hasData = overall.stream().anyMatch(r -> r.totalSeconds() != null);
        if (!hasData) {
            return new RallyeFinalRecap(
                    false, null, null, List.of(), List.of(),
                    List.of("Le résumé final apparaîtra dès que des temps seront enregistrés.")
            );
        }

        List<RallyeStandingRow> ranked = overall.stream()
                .filter(r -> r.totalSeconds() != null)
                .toList();

        String championName = ranked.isEmpty() ? null : ranked.get(0).name();
        String championTime = ranked.isEmpty() ? null : ranked.get(0).totalFormatted();

        List<String> podium = new ArrayList<>();
        for (int i = 0; i < Math.min(3, ranked.size()); i++) {
            RallyeStandingRow r = ranked.get(i);
            podium.add((i + 1) + ". " + r.name() + " — " + r.totalFormatted()
                    + (r.gapFormatted() != null && !"—".equals(r.gapFormatted())
                    ? " (" + r.gapFormatted() + ")" : ""));
        }

        List<RallyeScratchTally> scratchLeaders = totalScratches.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> {
                    int c = Integer.compare(b.getValue(), a.getValue());
                    if (c != 0) {
                        return c;
                    }
                    return names.getOrDefault(a.getKey(), "").compareToIgnoreCase(
                            names.getOrDefault(b.getKey(), ""));
                })
                .map(e -> new RallyeScratchTally(
                        names.get(e.getKey()),
                        e.getValue(),
                        List.copyOf(scratchStagesGlobal.getOrDefault(e.getKey(), List.of()))
                ))
                .toList();

        List<String> headlines = new ArrayList<>();
        if (championName != null) {
            headlines.add("Vainqueur : " + championName + " en " + championTime + ".");
        }
        if (podium.size() >= 2) {
            headlines.add("Podium : " + String.join(" · ", podium) + ".");
        }
        if (!scratchLeaders.isEmpty()) {
            RallyeScratchTally top = scratchLeaders.get(0);
            headlines.add("Roi des scratchs : " + top.pilotName()
                    + " avec " + top.count() + " ES gagnée"
                    + (top.count() > 1 ? "s" : "") + ".");
        }

        // Plus de boucles menées
        Map<String, Integer> boucleWins = new HashMap<>();
        for (RallyeBoucleRecap b : boucleRecaps) {
            if (b.hasData() && b.leaderName() != null) {
                boucleWins.merge(b.leaderName(), 1, Integer::sum);
            }
        }
        boucleWins.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(e -> {
                    if (e.getValue() >= 2) {
                        headlines.add(e.getKey() + " a remporté " + e.getValue()
                                + " boucles au chronomètre cumulé.");
                    } else if (e.getValue() == 1 && boucleRecaps.stream().filter(RallyeBoucleRecap::hasData).count() == 1) {
                        // single boucle with data — skip redundant
                    } else if (e.getValue() == 1) {
                        headlines.add("Meilleure boucle chronométrée : " + e.getKey() + ".");
                    }
                });

        // Plus gros écart final (dernier classé)
        if (ranked.size() >= 2) {
            RallyeStandingRow last = ranked.get(ranked.size() - 1);
            if (last.gapFormatted() != null && !"—".equals(last.gapFormatted())) {
                headlines.add("Plus gros retard final : " + last.name()
                        + " (" + last.gapFormatted() + ").");
            }
        }

        // Coup dur le plus marquant parmi les boucles
        String worstBlow = null;
        for (RallyeBoucleRecap b : boucleRecaps) {
            for (String h : b.headlines()) {
                if (h.startsWith("Coup dur")) {
                    worstBlow = h.replace("Coup dur", "Coup dur du rallye");
                }
            }
        }
        if (worstBlow != null) {
            headlines.add(worstBlow);
        }

        return new RallyeFinalRecap(
                true,
                championName,
                championTime,
                podium,
                scratchLeaders,
                headlines
        );
    }

    private static Long findPilotIdByName(Rallye rallye, String name) {
        if (name == null) {
            return null;
        }
        for (RallyePilot p : rallye.getPilots()) {
            if (name.equals(p.getName())) {
                return p.getId();
            }
        }
        return null;
    }

    @Override
    public int importPilotsFromExcel(Long rallyeId, String filePath) {
        Rallye rallye = get(rallyeId);
        Set<String> existing = new HashSet<>();
        for (RallyePilot p : rallye.getPilots()) {
            existing.add(normalizeName(p.getName()));
        }

        int imported = 0;
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = findPilotesSheet(workbook);
            if (sheet == null) {
                throw new IllegalArgumentException("Feuille « Pilotes » introuvable dans le fichier");
            }

            for (int i = 3; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String name = cellAsString(row.getCell(1));
                if (name.isEmpty() || isPlaceholderName(name)) {
                    continue;
                }
                if (existing.contains(normalizeName(name))) {
                    continue;
                }

                String car = cellAsString(row.getCell(2));
                String category = cellAsString(row.getCell(3));
                Integer startNumber = cellAsInteger(row.getCell(0));

                RallyePilot pilot = new RallyePilot(
                        name.trim(),
                        blankToNull(car),
                        blankToNull(category),
                        startNumber != null ? startNumber : (imported + 1)
                );
                rallye.addPilot(pilot);
                existing.add(normalizeName(name));
                imported++;
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Impossible de lire le fichier Excel: " + e.getMessage());
        }

        return imported;
    }

    @Override
    @Transactional(readOnly = true)
    public RallyeBoucleGrid buildGroupGrid(Long rallyeId, int boucle) {
        Rallye rallye = get(rallyeId);
        if (boucle < 1 || boucle > rallye.getBoucleCount()) {
            throw new IllegalArgumentException("Boucle invalide: " + boucle);
        }
        int groupCount = rallye.getStagesPerBoucle();
        if (groupCount < 1) {
            throw new IllegalArgumentException("Nombre d'ES invalide");
        }
        if (rallye.getPilots().isEmpty()) {
            return new RallyeBoucleGrid(boucle, groupCount, List.of(), List.of(), false, null, 0);
        }

        SeedingOrder seeding = orderPilotsForBoucle(rallye, boucle);
        boolean manual = groupAssignmentRepo.existsByRallyeIdAndBoucle(rallyeId, boucle);
        List<List<RallyeGridPilot>> baseGroups = manual
                ? groupsFromAssignments(rallyeId, boucle, groupCount, seeding.pilots())
                : dealRoundRobin(seeding.pilots(), groupCount);

        // Une feuille par groupe : toutes les ES de la boucle, dans l'ordre de parcours.
        // Groupe g démarre sur ES g, puis ES+1, ES+2… jusqu'à avoir tout fait.
        // Ordre des pilotes dans le groupe : composition figée (classement / manuel),
        // mais rotation du départ à chaque ES pour que chacun mène une spéciale si possible.
        List<RallyeGroupSheet> groupSheets = new ArrayList<>();
        for (int groupIndex = 0; groupIndex < groupCount; groupIndex++) {
            List<RallyeGridPilot> pilots = List.copyOf(baseGroups.get(groupIndex));
            List<RallyeGroupEsBlock> stages = new ArrayList<>();
            List<Integer> esOrder = new ArrayList<>();
            for (int step = 0; step < groupCount; step++) {
                int es = ((groupIndex + step) % groupCount) + 1;
                int passageOrder = Math.floorMod(es - groupIndex - 1, groupCount) + 1;
                stages.add(new RallyeGroupEsBlock(es, passageOrder, rotateList(pilots, step)));
                esOrder.add(es);
            }
            List<Long> pilotIds = pilots.stream().map(RallyeGridPilot::id).toList();
            String qr = RallyeSheetQr.encode(rallye.getId(), boucle, groupIndex + 1, pilotIds, esOrder);
            groupSheets.add(new RallyeGroupSheet(groupIndex + 1, pilots, stages, qr));
        }

        return new RallyeBoucleGrid(
                boucle,
                groupCount,
                baseGroups,
                groupSheets,
                seeding.seededFromResults(),
                seeding.sourceBoucle(),
                seeding.pilotsRanked(),
                manual
        );
    }

    @Override
    public void saveGroupAssignments(Long rallyeId, int boucle, List<List<Long>> groups) {
        Rallye rallye = get(rallyeId);
        if (boucle < 1 || boucle > rallye.getBoucleCount()) {
            throw new IllegalArgumentException("Boucle invalide: " + boucle);
        }
        int groupCount = rallye.getStagesPerBoucle();
        if (groups == null || groups.size() != groupCount) {
            throw new IllegalArgumentException("Nombre de groupes attendu: " + groupCount);
        }

        Map<Long, RallyePilot> pilotsById = new HashMap<>();
        for (RallyePilot p : rallye.getPilots()) {
            pilotsById.put(p.getId(), p);
        }

        Set<Long> seen = new HashSet<>();
        List<RallyeGroupAssignment> toSave = new ArrayList<>();
        for (int g = 0; g < groups.size(); g++) {
            List<Long> pilotIds = groups.get(g) != null ? groups.get(g) : List.of();
            int pos = 0;
            for (Long pilotId : pilotIds) {
                if (pilotId == null) {
                    continue;
                }
                RallyePilot pilot = pilotsById.get(pilotId);
                if (pilot == null) {
                    throw new IllegalArgumentException("Pilote inconnu: " + pilotId);
                }
                if (!seen.add(pilotId)) {
                    throw new IllegalArgumentException("Pilote en double dans les groupes: " + pilot.getName());
                }
                toSave.add(new RallyeGroupAssignment(rallye, boucle, pilot, g + 1, pos++));
            }
        }
        if (seen.size() != pilotsById.size()) {
            throw new IllegalArgumentException("Tous les pilotes doivent être placés dans un groupe.");
        }

        groupAssignmentRepo.deleteByRallyeIdAndBoucle(rallyeId, boucle);
        groupAssignmentRepo.flush();
        groupAssignmentRepo.saveAll(toSave);
    }

    @Override
    public void clearGroupAssignments(Long rallyeId, int boucle) {
        Rallye rallye = get(rallyeId);
        if (boucle < 1 || boucle > rallye.getBoucleCount()) {
            throw new IllegalArgumentException("Boucle invalide: " + boucle);
        }
        groupAssignmentRepo.deleteByRallyeIdAndBoucle(rallyeId, boucle);
    }

    private List<List<RallyeGridPilot>> groupsFromAssignments(
            Long rallyeId,
            int boucle,
            int groupCount,
            List<RallyeGridPilot> seededPilots
    ) {
        Map<Long, RallyeGridPilot> byId = new LinkedHashMap<>();
        for (RallyeGridPilot p : seededPilots) {
            byId.put(p.id(), p);
        }

        List<List<RallyeGridPilot>> groups = new ArrayList<>();
        for (int i = 0; i < groupCount; i++) {
            groups.add(new ArrayList<>());
        }

        Set<Long> placed = new HashSet<>();
        for (RallyeGroupAssignment a : groupAssignmentRepo
                .findByRallyeIdAndBoucleOrderByGroupNumberAscPositionInGroupAsc(rallyeId, boucle)) {
            int g = a.getGroupNumber() - 1;
            if (g < 0 || g >= groupCount) {
                continue;
            }
            RallyeGridPilot pilot = byId.get(a.getPilot().getId());
            if (pilot == null) {
                continue;
            }
            groups.get(g).add(pilot);
            placed.add(pilot.id());
        }

        // Pilotes non assignés → groupe le moins chargé
        for (RallyeGridPilot p : seededPilots) {
            if (placed.contains(p.id())) {
                continue;
            }
            int best = 0;
            for (int i = 1; i < groupCount; i++) {
                if (groups.get(i).size() < groups.get(best).size()) {
                    best = i;
                }
            }
            groups.get(best).add(p);
        }
        return groups;
    }

    /**
     * Boucle 1 : ordre d'inscription (#).
     * Boucles suivantes : ordre du classement cumulé après la boucle précédente (meilleur temps en tête),
     * puis redistribution en round-robin (1→G1, 2→G2, …, puis on recommence).
     */
    private SeedingOrder orderPilotsForBoucle(Rallye rallye, int boucle) {
        if (boucle <= 1) {
            List<RallyeGridPilot> ordered = new ArrayList<>();
            for (RallyePilot p : rallye.getPilots()) {
                ordered.add(new RallyeGridPilot(p.getId(), p.getStartNumber(), p.getName(), p.getCar()));
            }
            return new SeedingOrder(ordered, false, null, 0);
        }

        int sourceBoucle = boucle - 1;
        int afterStages = sourceBoucle * rallye.getStagesPerBoucle();
        List<RallyeStandingRow> rows = standings(rallye.getId(), afterStages, null);

        int ranked = 0;
        for (RallyeStandingRow row : rows) {
            if (row.totalSeconds() != null) {
                ranked++;
            }
        }

        if (ranked == 0) {
            // Pas encore de temps sur la boucle précédente → reste sur l'ordre d'inscription
            List<RallyeGridPilot> fallback = new ArrayList<>();
            for (RallyePilot p : rallye.getPilots()) {
                fallback.add(new RallyeGridPilot(p.getId(), p.getStartNumber(), p.getName(), p.getCar()));
            }
            return new SeedingOrder(fallback, false, sourceBoucle, 0);
        }

        List<RallyeGridPilot> ordered = new ArrayList<>();
        for (RallyeStandingRow row : rows) {
            Integer prevRank = row.totalSeconds() == null ? null : row.position();
            ordered.add(new RallyeGridPilot(
                    row.pilotId(),
                    null,
                    row.name(),
                    row.car(),
                    prevRank
            ));
        }
        // Réinjecter le n° de départ pour l'affichage
        Map<Long, Integer> startById = new HashMap<>();
        for (RallyePilot p : rallye.getPilots()) {
            startById.put(p.getId(), p.getStartNumber());
        }
        List<RallyeGridPilot> withNumbers = new ArrayList<>();
        for (RallyeGridPilot g : ordered) {
            withNumbers.add(new RallyeGridPilot(
                    g.id(),
                    startById.get(g.id()),
                    g.name(),
                    g.car(),
                    g.previousRank()
            ));
        }
        return new SeedingOrder(withNumbers, true, sourceBoucle, ranked);
    }

    @Override
    public Rallye addBoucle(Long rallyeId) {
        Rallye rallye = get(rallyeId);
        if (rallye.getBoucleCount() >= 12) {
            throw new IllegalArgumentException("Maximum 12 boucles");
        }
        rallye.setBoucleCount(rallye.getBoucleCount() + 1);
        return rallye;
    }

    @Override
    public Rallye addStage(Long rallyeId) {
        Rallye rallye = get(rallyeId);
        if (rallye.getStagesPerBoucle() >= 12) {
            throw new IllegalArgumentException("Maximum 12 ES");
        }
        rallye.setStagesPerBoucle(rallye.getStagesPerBoucle() + 1);
        return rallye;
    }

    @Override
    public Rallye removeBoucle(Long rallyeId) {
        Rallye rallye = get(rallyeId);
        if (rallye.getBoucleCount() <= 1) {
            throw new IllegalArgumentException("Il faut au moins une boucle");
        }
        int removed = rallye.getBoucleCount();
        for (RallyePilot pilot : rallye.getPilots()) {
            pilot.getStageTimes().removeIf(t -> t.getBoucle() == removed);
        }
        rallye.setBoucleCount(removed - 1);
        return rallye;
    }

    @Override
    public Rallye removeStage(Long rallyeId) {
        Rallye rallye = get(rallyeId);
        if (rallye.getStagesPerBoucle() <= 1) {
            throw new IllegalArgumentException("Il faut au moins une ES");
        }
        int removed = rallye.getStagesPerBoucle();
        for (RallyePilot pilot : rallye.getPilots()) {
            pilot.getStageTimes().removeIf(t -> t.getStage() == removed);
        }
        rallye.setStagesPerBoucle(removed - 1);
        return rallye;
    }

    /**
     * Distribution round-robin : pilote 1→G1, 2→G2, …, N→G1, N+1→G2, etc.
     * Ex. 5 pilotes / 4 groupes → G1:[1,5] G2:[2] G3:[3] G4:[4]
     */
    static List<List<RallyeGridPilot>> dealRoundRobin(List<RallyeGridPilot> pilots, int groupCount) {
        List<List<RallyeGridPilot>> groups = new ArrayList<>();
        for (int i = 0; i < groupCount; i++) {
            groups.add(new ArrayList<>());
        }
        for (int i = 0; i < pilots.size(); i++) {
            groups.get(i % groupCount).add(pilots.get(i));
        }
        return groups;
    }

    static <T> List<T> rotateList(List<T> source, int offset) {
        if (source.isEmpty()) {
            return List.of();
        }
        int n = source.size();
        int o = ((offset % n) + n) % n;
        if (o == 0) {
            return new ArrayList<>(source);
        }
        List<T> out = new ArrayList<>(n);
        out.addAll(source.subList(o, n));
        out.addAll(source.subList(0, o));
        return out;
    }

    private record SeedingOrder(
            List<RallyeGridPilot> pilots,
            boolean seededFromResults,
            Integer sourceBoucle,
            int pilotsRanked
    ) {
    }

    private RallyePilot requirePilot(Long rallyeId, Long pilotId) {
        RallyePilot pilot = pilotRepo.findById(pilotId)
                .orElseThrow(() -> new NoSuchElementException("Pilote introuvable: " + pilotId));
        if (!pilot.getRallye().getId().equals(rallyeId)) {
            throw new IllegalArgumentException("Le pilote n'appartient pas à ce rallye");
        }
        return pilot;
    }

    private static Sheet findPilotesSheet(Workbook workbook) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (sheet.getSheetName() != null
                    && sheet.getSheetName().trim().equalsIgnoreCase("Pilotes")) {
                return sheet;
            }
        }
        return workbook.getNumberOfSheets() > 1 ? workbook.getSheetAt(1) : null;
    }

    private static String cellAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                if (v == Math.rint(v)) {
                    yield String.valueOf((long) v);
                }
                yield String.valueOf(v);
            }
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue().trim();
                } catch (Exception e) {
                    yield "";
                }
            }
            default -> "";
        };
    }

    private static Integer cellAsInteger(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        String s = cellAsString(cell);
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isPlaceholderName(String name) {
        String t = name.trim();
        return t.matches("\\d+") || "0".equals(t);
    }

    private static String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private record StandingAccumulator(RallyePilot pilot, Double total, int completed, int expected) {
    }
}
