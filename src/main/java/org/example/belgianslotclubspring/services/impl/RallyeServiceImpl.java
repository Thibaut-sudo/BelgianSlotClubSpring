package org.example.belgianslotclubspring.services.impl;

import org.apache.poi.ss.usermodel.*;
import org.example.belgianslotclubspring.entities.Rallye;
import org.example.belgianslotclubspring.entities.RallyeGroupAssignment;
import org.example.belgianslotclubspring.entities.RallyePilot;
import org.example.belgianslotclubspring.entities.RallyeStageTime;
import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.models.RallyeBoucleGrid;
import org.example.belgianslotclubspring.models.RallyeBoucleRecap;
import org.example.belgianslotclubspring.models.RallyeFinalRecap;
import org.example.belgianslotclubspring.models.RallyeGroupEsBlock;
import org.example.belgianslotclubspring.models.RallyeGroupSheet;
import org.example.belgianslotclubspring.models.RallyeGridPilot;
import org.example.belgianslotclubspring.models.RallyeImportResult;
import org.example.belgianslotclubspring.models.RallyeRecaps;
import org.example.belgianslotclubspring.models.RallyeScratchTally;
import org.example.belgianslotclubspring.models.RallyeStageHighlight;
import org.example.belgianslotclubspring.models.RallyeStandingRow;
import org.example.belgianslotclubspring.repo.RallyeGroupAssignmentRepo;
import org.example.belgianslotclubspring.repo.RallyePilotRepo;
import org.example.belgianslotclubspring.repo.RallyeRepo;
import org.example.belgianslotclubspring.services.ClubCalendarService;
import org.example.belgianslotclubspring.services.RallyeService;
import org.example.belgianslotclubspring.utils.RallyeSheetQr;
import org.example.belgianslotclubspring.utils.RallyeTimeFormat;
import org.example.belgianslotclubspring.utils.RecapPhrases;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class RallyeServiceImpl implements RallyeService {

    private static final Logger log = LoggerFactory.getLogger(RallyeServiceImpl.class);

    private final RallyeRepo rallyeRepo;
    private final RallyePilotRepo pilotRepo;
    private final RallyeGroupAssignmentRepo groupAssignmentRepo;
    private final ClubCalendarService clubCalendarService;

    public RallyeServiceImpl(
            RallyeRepo rallyeRepo,
            RallyePilotRepo pilotRepo,
            RallyeGroupAssignmentRepo groupAssignmentRepo,
            ClubCalendarService clubCalendarService
    ) {
        this.rallyeRepo = rallyeRepo;
        this.pilotRepo = pilotRepo;
        this.groupAssignmentRepo = groupAssignmentRepo;
        this.clubCalendarService = clubCalendarService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Rallye> listByClub(String club) {
        List<Rallye> list = new ArrayList<>(rallyeRepo.findByClubNameWithPilots(Club.requireCode(club)));
        list.sort(Comparator.comparing(Rallye::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return list;
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
        Rallye saved = rallyeRepo.save(rallye);
        addToCalendar(saved);
        return saved;
    }

    @Override
    public void delete(Long id) {
        Rallye rallye = rallyeRepo.findById(id).orElse(null);
        rallyeRepo.deleteById(id);
        if (rallye != null) {
            clubCalendarService.deleteIfMatchesRallye(rallye);
        }
    }

    @Override
    public Rallye finish(Long id) {
        Rallye rallye = get(id);
        rallye.setFinished(true);
        return rallye;
    }

    @Override
    public RallyePilot addPilot(Long rallyeId, String name, String car, String category) {
        Rallye rallye = requireEditable(rallyeId);
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
        assertEditable(pilot.getRallye());
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
        assertEditable(pilot.getRallye());
        Rallye rallye = pilot.getRallye();
        rallye.getPilots().remove(pilot);
        pilotRepo.delete(pilot);
    }

    @Override
    public void saveBoucleTimes(Long rallyeId, int boucle, Map<Long, Map<Integer, String>> timesByPilot) {
        patchBoucleTimes(rallyeId, boucle, timesByPilot);
    }

    @Override
    public int patchBoucleTimes(Long rallyeId, int boucle, Map<Long, Map<Integer, String>> timesByPilot) {
        Rallye rallye = requireEditable(rallyeId);
        if (boucle < 1 || boucle > rallye.getBoucleCount()) {
            throw new IllegalArgumentException("Boucle invalide: " + boucle);
        }
        if (timesByPilot == null || timesByPilot.isEmpty()) {
            return 0;
        }

        int written = 0;
        for (Map.Entry<Long, Map<Integer, String>> entry : timesByPilot.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
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
                written++;
            }
        }
        return written;
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
        Double previousTotal = null;
        for (StandingAccumulator a : rows) {
            Double gap = (a.total != null && leader != null) ? a.total - leader : null;
            Double gapPrev = (a.total != null && previousTotal != null) ? a.total - previousTotal : null;
            result.add(new RallyeStandingRow(
                    a.total == null ? 0 : pos,
                    a.pilot.getId(),
                    a.pilot.getName(),
                    a.pilot.getCar(),
                    a.pilot.getCategory(),
                    a.total,
                    gap,
                    gapPrev,
                    a.total == null ? "—" : RallyeTimeFormat.format(a.total),
                    (a.total == null || gap == null || gap == 0)
                            ? "—"
                            : RallyeTimeFormat.formatGap(gap),
                    (a.total == null || gapPrev == null || gapPrev == 0)
                            ? "—"
                            : RallyeTimeFormat.formatGap(gapPrev),
                    a.completed,
                    a.expected
            ));
            if (a.total != null) {
                previousTotal = a.total;
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

        List<Map.Entry<Long, Double>> ranking = boucleTotals.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .toList();

        Long leaderId = ranking.isEmpty() ? null : ranking.get(0).getKey();
        String leaderName = leaderId != null ? pilotNames.get(leaderId) : null;
        String leaderTime = leaderId != null
                ? RallyeTimeFormat.format(boucleTotals.get(leaderId))
                : null;

        List<String> headlines = buildBoucleHeadlines(
                boucle,
                finished,
                stages,
                stagesWithTimes,
                leaderName,
                leaderTime,
                ranking,
                pilotNames,
                scratchLeaders,
                highlights,
                hardestName,
                hardestEs,
                hardestGap,
                rallye
        );

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

    private List<String> buildBoucleHeadlines(
            int boucle,
            boolean finished,
            int stages,
            int stagesWithTimes,
            String leaderName,
            String leaderTime,
            List<Map.Entry<Long, Double>> ranking,
            Map<Long, String> pilotNames,
            List<RallyeScratchTally> scratchLeaders,
            List<RallyeStageHighlight> highlights,
            String hardestName,
            String hardestEs,
            String hardestGap,
            Rallye rallye
    ) {
        List<String> headlines = new ArrayList<>();
        String boucleLabel = "boucle " + boucle;
        String boucleTitle = "Boucle " + boucle;
        int pilotsTimed = ranking.size();
        int base = RecapPhrases.seed("boucle-v2", boucle, leaderName, pilotsTimed, finished, stagesWithTimes);

        if (finished) {
            String timed = pilotsTimed + " pilote" + (pilotsTimed > 1 ? "s ont" : " a")
                    + " passé la ligne chronométrée";
            headlines.add(RecapPhrases.pick(base,
                    "La " + boucleLabel + " est bouclée : " + timed + ".",
                    "Fin de la " + boucleLabel + " — " + timed + ".",
                    "Voilà, la " + boucleLabel + " est dans la boîte : " + timed + ".",
                    "Chronos définitifs pour la " + boucleLabel + " : " + timed + "."
            ));
        } else {
            headlines.add(RecapPhrases.pick(base,
                    boucleTitle + " encore en cours — " + stagesWithTimes
                            + " ES chronométrée" + (stagesWithTimes > 1 ? "s" : "")
                            + " sur " + stages + ". L’histoire s’écrit encore…",
                    "La " + boucleLabel + " se construit : " + stagesWithTimes + "/"
                            + stages + " ES déjà chronométrées. Rien n’est figé.",
                    boucleTitle + " à mi-chemin — " + stagesWithTimes
                            + " spéciale" + (stagesWithTimes > 1 ? "s" : "")
                            + " au compteur. Place à la suite.",
                    "On avance en " + boucleLabel + " (" + stagesWithTimes + " ES sur "
                            + stages + "). Le classement peut encore basculer."
            ));
        }

        Long leaderId = ranking.isEmpty() ? null : ranking.get(0).getKey();
        String leaderCar = carOfPilot(rallye, leaderId);
        String leaderLabel = labelled(leaderName, leaderCar);

        if (leaderName != null && leaderTime != null) {
            if (finished) {
                headlines.add(RecapPhrases.pick(base + 1,
                        "Et c’est " + leaderLabel + " qui s’offre la " + boucleLabel
                                + " en " + leaderTime + " — une belle opération au général.",
                        leaderLabel + " remporte la " + boucleLabel + " (" + leaderTime
                                + "). Une opération nette au classement.",
                        "Victoire de boucle pour " + leaderLabel + " en " + leaderTime
                                + ". Ça fait du bien au général.",
                        leaderLabel + " signe la " + boucleLabel + " en " + leaderTime
                                + " — autorité et régularité.",
                        leaderCar != null
                                ? leaderName + " a su tirer le meilleur de sa " + leaderCar
                                + " : victoire de " + boucleLabel + " en " + leaderTime + "."
                                : leaderName + " signe la " + boucleLabel + " en " + leaderTime + "."
                ));
            } else {
                headlines.add(RecapPhrases.pick(base + 1,
                        "Pour l’instant, " + leaderLabel + " mène la danse en "
                                + leaderTime + ". La pression monte pour la suite.",
                        "Leader provisoire : " + leaderLabel + " (" + leaderTime
                                + "). Les prochaines ES vont parler.",
                        leaderLabel + " tient la tête de la " + boucleLabel + " en "
                                + leaderTime + " — pour combien de temps ?",
                        "En tête à ce stade : " + leaderLabel + " chronométré en "
                                + leaderTime + "."
                ));
            }

            if (ranking.size() >= 2) {
                Map.Entry<Long, Double> second = ranking.get(1);
                double gap = second.getValue() - ranking.get(0).getValue();
                String secondName = pilotNames.get(second.getKey());
                String secondCar = carOfPilot(rallye, second.getKey());
                String secondLabel = labelled(secondName, secondCar);
                String gapFmt = RallyeTimeFormat.formatGap(gap);
                if (gap < 1.0) {
                    headlines.add(RecapPhrases.pick(base + 2,
                            "Suspense ! " + secondLabel + " n’est qu’à " + gapFmt
                                    + " — une poignée de dixièmes sépare le duo.",
                            "Écart microscopique : " + secondLabel + " à " + gapFmt
                                    + " de " + leaderName + ". Du grand sport.",
                            secondLabel + " colle à " + gapFmt
                                    + ". La " + boucleLabel + " se joue sur un fil.",
                            "Presque rien entre les deux : " + secondLabel + " à seulement "
                                    + gapFmt + "."
                    ));
                } else if (gap < 5.0) {
                    headlines.add(RecapPhrases.pick(base + 2,
                            secondLabel + " reste dans le match, à seulement " + gapFmt
                                    + " du leader. Rien n’est joué.",
                            "À " + gapFmt + ", " + secondLabel
                                    + " garde une vraie chance de revenir.",
                            secondLabel + " pointe à " + gapFmt
                                    + " — assez près pour croire à un retournement.",
                            "Deuxième chronométré, " + secondLabel + " (" + gapFmt
                                    + ") n’a pas dit son dernier mot."
                    ));
                } else {
                    headlines.add(RecapPhrases.pick(base + 2,
                            secondLabel + " pointe à " + gapFmt + " : " + leaderName
                                    + " a su créer un vrai écart.",
                            leaderName + " a creusé : " + secondLabel + " est déjà à "
                                    + gapFmt + ".",
                            "Écart net pour " + secondLabel + " (" + gapFmt
                                    + ") — la " + boucleLabel + " penche clairement.",
                            secondLabel + " doit digérer " + gapFmt
                                    + " de retard sur " + leaderName + "."
                    ));
                }
                if (leaderCar != null && secondCar != null && leaderCar.equalsIgnoreCase(secondCar)) {
                    headlines.add(RecapPhrases.pick(base + 21,
                            "Duel interne chez les " + leaderCar + " : " + leaderName
                                    + " et " + secondName + " se tiennent à " + gapFmt + ".",
                            "Même arme, même combat : deux " + leaderCar
                                    + " occupent la tête (" + leaderName + " devant " + secondName + ").",
                            leaderName + " et " + secondName + " se livrent un mano a mano en "
                                    + leaderCar + " (" + gapFmt + ")."
                    ));
                } else if (leaderCar != null && secondCar != null) {
                    headlines.add(RecapPhrases.pick(base + 21,
                            "Choc des machines : la " + leaderCar + " de " + leaderName
                                    + " face à la " + secondCar + " de " + secondName + ".",
                            leaderName + " en " + leaderCar + ", " + secondName + " en "
                                    + secondCar + " — deux recettes différentes pour la " + boucleLabel + ".",
                            "La " + leaderCar + " mène, la " + secondCar
                                    + " répond : le match technique est lancé."
                    ));
                }
            }

            if (ranking.size() >= 3) {
                Map.Entry<Long, Double> third = ranking.get(2);
                String thirdName = pilotNames.get(third.getKey());
                String thirdLabel = labelled(thirdName, carOfPilot(rallye, third.getKey()));
                String thirdTime = RallyeTimeFormat.format(third.getValue());
                double gap23 = third.getValue() - ranking.get(1).getValue();
                String gap23Fmt = RallyeTimeFormat.formatGap(gap23);
                headlines.add(RecapPhrases.pick(base + 3,
                        "Sur la troisième marche de la " + boucleLabel + " : "
                                + thirdLabel + " en " + thirdTime + ", à " + gap23Fmt
                                + " du deuxième.",
                        "P3 de la " + boucleLabel + " pour " + thirdLabel
                                + " (" + thirdTime + ", " + gap23Fmt + " de P2).",
                        thirdLabel + " glisse sur le podium de la " + boucleLabel
                                + " en " + thirdTime + " — " + gap23Fmt + " derrière le deuxième.",
                        "Troisième chrono : " + thirdLabel + " — " + thirdTime
                                + " (" + gap23Fmt + " de P2)."
                ));
            }

            if (ranking.size() >= 4) {
                Map.Entry<Long, Double> p4 = ranking.get(3);
                String p4Label = labelled(pilotNames.get(p4.getKey()), carOfPilot(rallye, p4.getKey()));
                double gap34 = p4.getValue() - ranking.get(2).getValue();
                headlines.add(RecapPhrases.pick(base + 22,
                        "Juste hors podium, " + p4Label + " concède "
                                + RallyeTimeFormat.formatGap(gap34) + " à la troisième place.",
                        p4Label + " termine 4e de la " + boucleLabel + " en "
                                + RallyeTimeFormat.format(p4.getValue())
                                + " — " + RallyeTimeFormat.formatGap(gap34) + " de P3.",
                        "La 4e place revient à " + p4Label + " ("
                                + RallyeTimeFormat.format(p4.getValue()) + ")."
                ));
            }
        }

        List<ConsecutiveGap> boucleMid = midfieldGaps(consecutiveGapsFromRanking(ranking, pilotNames));
        if (!boucleMid.isEmpty()) {
            headlines.add(duelLine(base + 30, boucleMid.getFirst(), "en " + boucleLabel));
            if (boucleMid.size() >= 2) {
                String extra = extraDuelsLine(base + 31, boucleMid.subList(1, Math.min(3, boucleMid.size())),
                        "en " + boucleLabel);
                if (extra != null) {
                    headlines.add(extra);
                }
            }
        }
        String bouclePeloton = pelotonLineFromRanking(base + 32, ranking, pilotNames, "en " + boucleLabel);
        if (bouclePeloton != null) {
            headlines.add(bouclePeloton);
        }

        if (!scratchLeaders.isEmpty()) {
            RallyeScratchTally top = scratchLeaders.get(0);
            String stagesLabel = top.stageNumbers().stream()
                    .map(n -> "ES " + n)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            int scratchSeed = RecapPhrases.seed("b-scratch", boucle, top.pilotName(), top.count());
            if (top.count() >= 3) {
                headlines.add(RecapPhrases.pick(scratchSeed,
                        "Quel festival pour " + labelledByName(rallye, top.pilotName()) + " ! " + top.count()
                                + " scratchs en " + boucleLabel + " (" + stagesLabel
                                + "). Le genre de journée dont on se souvient.",
                        labelledByName(rallye, top.pilotName()) + " a maraudé " + top.count() + " scratchs en "
                                + boucleLabel + " (" + stagesLabel + "). Démonstration.",
                        "Vitesse pure en " + boucleLabel + " : " + labelledByName(rallye, top.pilotName())
                                + " signe " + top.count() + " ES (" + stagesLabel + ").",
                        top.count() + " spéciales au nom de " + labelledByName(rallye, top.pilotName())
                                + " (" + stagesLabel + ") — difficile de faire plus clair."
                ));
            } else if (top.count() == 2) {
                headlines.add(RecapPhrases.pick(scratchSeed,
                        labelledByName(rallye, top.pilotName()) + " a mis deux fois tout le monde d’accord"
                                + " (" + stagesLabel + "). Une vraie pointe de vitesse.",
                        "Double scratch pour " + labelledByName(rallye, top.pilotName()) + " (" + stagesLabel
                                + ") en " + boucleLabel + ".",
                        labelledByName(rallye, top.pilotName()) + " s’offre un duo gagnant sur " + stagesLabel + ".",
                        "Deux ES, deux scratchs : " + labelledByName(rallye, top.pilotName())
                                + " a trouvé le rythme (" + stagesLabel + ")."
                ));
            } else {
                headlines.add(RecapPhrases.pick(scratchSeed,
                        "Le scratch du jour en " + boucleLabel + " revient à "
                                + labelledByName(rallye, top.pilotName()) + " sur " + stagesLabel
                                + " — un chrono qui fait mal aux adversaires.",
                        "Meilleur temps de référence en " + boucleLabel + " : "
                                + labelledByName(rallye, top.pilotName()) + " sur " + stagesLabel + ".",
                        labelledByName(rallye, top.pilotName()) + " s’adjuge le scratch de " + stagesLabel
                                + " en " + boucleLabel + ".",
                        "Sur " + stagesLabel + ", personne n’a fait mieux que "
                                + labelledByName(rallye, top.pilotName()) + "."
                ));
            }
            if (scratchLeaders.size() > 1) {
                RallyeScratchTally second = scratchLeaders.get(1);
                headlines.add(RecapPhrases.pick(scratchSeed + 1,
                        "Pas loin derrière au rayon scratchs : " + labelledByName(rallye, second.pilotName())
                                + " avec " + second.count() + " ES gagnée"
                                + (second.count() > 1 ? "s" : "") + ". La concurrence est réelle.",
                        labelledByName(rallye, second.pilotName()) + " répond aussi présent : " + second.count()
                                + " ES au compteur.",
                        "Autre homme fort des spéciales : " + labelledByName(rallye, second.pilotName())
                                + " (" + second.count() + ").",
                        "Dans le sillage, " + labelledByName(rallye, second.pilotName()) + " glane "
                                + second.count() + " ES."
                ));
            }
        }

        RallyeStageHighlight extremeStage = null;
        double extremeGap = -1;
        for (RallyeStageHighlight st : highlights) {
            if (st.worstGapToScratch() == null || "—".equals(st.worstGapToScratch())) {
                continue;
            }
            try {
                String g = st.worstGapToScratch().startsWith("+")
                        ? st.worstGapToScratch().substring(1)
                        : st.worstGapToScratch();
                Double gap = RallyeTimeFormat.parse(g);
                if (gap != null && gap > extremeGap) {
                    extremeGap = gap;
                    extremeStage = st;
                }
            } catch (Exception ignored) {
                // ignore
            }
        }
        if (extremeStage != null && extremeGap > 0 && extremeGap < 2.0 && pilotsTimed >= 3) {
            int s = RecapPhrases.seed("b-tight", boucle, extremeStage.esNumber());
            String gapFmt = RallyeTimeFormat.formatGap(extremeGap);
            headlines.add(RecapPhrases.pick(s,
                    "Sur l’ES " + extremeStage.esNumber() + ", le plateau était ultra-serré : "
                            + "scratch pour " + extremeStage.scratchPilotName()
                            + ", seulement " + gapFmt + " d’écart dans le peloton. Du grand sport.",
                    "ES " + extremeStage.esNumber() + " : tout le monde dans un mouchoir ("
                            + gapFmt + "). Scratch pour " + extremeStage.scratchPilotName() + ".",
                    "L’ES " + extremeStage.esNumber() + " a livré un vrai festival de précision"
                            + " — seulement " + gapFmt + " d’écart max.",
                    "Écarts microscopiques sur l’ES " + extremeStage.esNumber()
                            + " : " + extremeStage.scratchPilotName() + " devant, peloton collé."
            ));
        } else if (extremeStage != null && extremeGap >= 8.0) {
            int s = RecapPhrases.seed("b-blow", boucle, extremeStage.esNumber());
            headlines.add(RecapPhrases.pick(s,
                    "L’ES " + extremeStage.esNumber() + " a étiré le peloton : "
                            + extremeStage.scratchPilotName() + " plante un "
                            + extremeStage.scratchTime() + " de référence.",
                    "Gros écarts sur l’ES " + extremeStage.esNumber() + " : "
                            + extremeStage.scratchPilotName() + " signe un "
                            + extremeStage.scratchTime() + ".",
                    "Sur l’ES " + extremeStage.esNumber() + ", "
                            + extremeStage.scratchPilotName()
                            + " prend le scratch (" + extremeStage.scratchTime() + ").",
                    "L’ES " + extremeStage.esNumber() + " a séparé le plateau : "
                            + extremeStage.scratchPilotName() + " au sommet ("
                            + extremeStage.scratchTime() + ")."
            ));
        }

        String lastBoucleName = ranking.isEmpty()
                ? null
                : pilotNames.get(ranking.get(ranking.size() - 1).getKey());
        String secondLastName = ranking.size() >= 2
                ? pilotNames.get(ranking.get(ranking.size() - 2).getKey())
                : null;
        boolean hardestIsBack = hardestName != null
                && (hardestName.equals(lastBoucleName) || hardestName.equals(secondLastName));
        if (hardestName != null && hardestGap != null && hardestEs != null && !hardestIsBack) {
            int s = RecapPhrases.seed("b-hard", boucle, hardestName, hardestEs);
            String hardestLabel = labelledByName(rallye, hardestName);
            headlines.add(RecapPhrases.pick(s,
                    "Sur " + hardestEs + " de la " + boucleLabel + ", l’écart se creuse pour "
                            + hardestLabel + " (" + hardestGap + " au scratch).",
                    hardestLabel + " lâche " + hardestGap + " sur " + hardestEs
                            + " — un passage qui pèse dans la " + boucleLabel + ".",
                    "L’" + hardestEs + " fait la différence pour " + hardestLabel
                            + " : " + hardestGap + " face au scratch."
            ));
        }

        Map<String, Integer> carCounts = new LinkedHashMap<>();
        Map<String, List<String>> namesByCar = new LinkedHashMap<>();
        for (Map.Entry<Long, Double> e : ranking) {
            String car = carOfPilot(rallye, e.getKey());
            if (car == null) {
                continue;
            }
            carCounts.merge(car, 1, Integer::sum);
            namesByCar.computeIfAbsent(car, k -> new ArrayList<>()).add(pilotNames.get(e.getKey()));
        }
        carCounts.entrySet().stream()
                .filter(e -> e.getValue() >= 3)
                .max(Map.Entry.comparingByValue())
                .ifPresent(e -> {
                    List<String> names = namesByCar.getOrDefault(e.getKey(), List.of());
                    String sample = names.stream().limit(3).reduce((a, b) -> a + ", " + b).orElse("");
                    headlines.add(RecapPhrases.pick(base + 23,
                            "Les " + e.getKey() + " étaient nombreuses en " + boucleLabel
                                    + " (" + e.getValue() + " au chrono"
                                    + (sample.isEmpty() ? "" : " : " + sample)
                                    + (names.size() > 3 ? "…" : "") + ").",
                            e.getValue() + " " + e.getKey() + " au chronomètre — "
                                    + "c’est le modèle le plus représenté de la " + boucleLabel + ".",
                            "Côté parc fermé, la " + e.getKey() + " domine le plateau ("
                                    + e.getValue() + " voitures, dont " + sample + ")."
                    ));
                });

        if (highlights.size() >= 2) {
            RallyeStageHighlight firstEs = highlights.get(0);
            RallyeStageHighlight lastEs = highlights.get(highlights.size() - 1);
            headlines.add(RecapPhrases.pick(base + 24,
                    "Ouverture de la " + boucleLabel + " : " + labelledByName(rallye, firstEs.scratchPilotName())
                            + " scratch l’ES " + firstEs.esNumber() + " en " + firstEs.scratchTime() + ".",
                    "Premier chronométrage, ES " + firstEs.esNumber() + " : scratch pour "
                            + labelledByName(rallye, firstEs.scratchPilotName())
                            + " (" + firstEs.scratchTime() + ")."
            ));
            if (lastEs.esNumber() != firstEs.esNumber()) {
                headlines.add(RecapPhrases.pick(base + 25,
                        "Pour clore la " + boucleLabel + ", l’ES " + lastEs.esNumber()
                                + " revient à " + labelledByName(rallye, lastEs.scratchPilotName())
                                + " en " + lastEs.scratchTime() + ".",
                        "Dernière spéciale de la " + boucleLabel + " (ES " + lastEs.esNumber()
                                + ") : " + labelledByName(rallye, lastEs.scratchPilotName())
                                + " plante le meilleur chrono (" + lastEs.scratchTime() + ")."
                ));
            }
        }

        List<String> penos = new ArrayList<>();
        for (RallyePilot p : rallye.getPilots()) {
            Double peno = p.getPenaltySeconds(boucle);
            if (peno != null && peno > 0) {
                penos.add(p.getName() + " (+" + RallyeTimeFormat.format(peno) + ")");
            }
        }
        if (!penos.isEmpty()) {
            String list = String.join(", ", penos);
            headlines.add(RecapPhrases.pick(base + 8,
                    "Attention aux pénalités en " + boucleLabel + " : " + list
                            + ". Autant de secondes qui ne se rattrapent pas facilement.",
                    "Pénalités en " + boucleLabel + " pour " + list
                            + " — chaque dixième compte.",
                    "Le chronomètre a aussi puni : " + list + " en " + boucleLabel + ".",
                    "Secondes de pénalité en " + boucleLabel + " : " + list + "."
            ));
        }

        if (finished && leaderName != null) {
            headlines.add(RecapPhrases.pick(base + 9,
                    "Bilan de la " + boucleLabel + " : " + leaderName
                            + " repart avec les honneurs. Place à la suite !",
                    "Pour résumer la " + boucleLabel + " : journée de " + leaderName
                            + ". On enchaîne.",
                    "Fin de chapitre pour la " + boucleLabel + " — " + leaderName
                            + " au sommet.",
                    leaderName + " a écrit le dénouement de la " + boucleLabel
                            + ". Clap de fin… jusqu’à la prochaine."
            ));
        }

        return headlines;
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

        int totalEs = Math.max(1, rallye.totalStages());
        int finalSeed = RecapPhrases.seed(
                "final-v4",
                championName,
                championTime,
                ranked.size(),
                scratchLeaders.isEmpty() ? 0 : scratchLeaders.get(0).count(),
                boucleRecaps.size()
        );

        Map<String, String> beats = buildFinalBeats(
                finalSeed, ranked, championName, championTime,
                scratchLeaders, boucleRecaps, totalEs
        );

        // Canevas différents : ordre + sélection des sujets changent selon le seed.
        String[] template = RecapPhrases.pickTemplate(finalSeed,
                new String[]{"intro", "winner", "cars", "runner_up", "interval", "p3", "peloton", "duels", "scratch_king", "scratch_second", "same_car", "es_detail", "outro"},
                new String[]{"intro", "field", "cars", "podium_pack", "winner", "peloton", "duels", "midfield", "scratch_king", "interval", "tightest", "es_detail", "p4", "outro"},
                new String[]{"intro", "boucle_dom", "winner", "cars", "scratch_king", "runner_up", "same_car", "peloton", "midfield", "duels", "p4", "outro"},
                new String[]{"intro", "winner", "speed_vs_title", "cars", "runner_up", "interval", "tightest", "field", "peloton", "duels", "es_detail", "outro"},
                new String[]{"intro", "field", "winner", "cars", "scratch_king", "scratch_pack", "podium_pack", "same_car", "peloton", "midfield", "interval", "outro"},
                new String[]{"intro", "scratch_king", "winner", "cars", "boucle_dom", "runner_up", "comeback", "peloton", "duels", "es_detail", "outro"},
                new String[]{"intro", "field", "cars", "winner", "interval", "peloton", "midfield", "duels", "scratch_second", "same_car", "p4", "outro"},
                new String[]{"intro", "podium_pack", "cars", "boucle_dom", "winner", "speed_vs_title", "interval", "peloton", "duels", "tightest", "p4", "outro"}
        );

        List<String> headlines = new ArrayList<>();
        Set<String> used = new LinkedHashSet<>();
        for (String key : template) {
            String line = beats.get(key);
            if (line != null && !line.isBlank() && used.add(key)) {
                headlines.add(line);
            }
        }
        // Filet de sécurité : au moins vainqueur + outro
        if (beats.get("winner") != null && !used.contains("winner")) {
            if (headlines.isEmpty()) {
                headlines.add(beats.get("winner"));
            } else {
                headlines.add(Math.min(1, headlines.size()), beats.get("winner"));
            }
            used.add("winner");
        }
        for (String extra : List.of(
                "peloton", "duels", "midfield", "scratch_pack",
                "cars", "same_car", "interval", "p4", "es_detail",
                "runner_up", "p3", "scratch_second"
        )) {
            String line = beats.get(extra);
            if (line != null && !line.isBlank() && used.add(extra)) {
                int outroAt = headlines.isEmpty() ? 0 : headlines.size() - (used.contains("outro") ? 1 : 0);
                headlines.add(Math.max(0, outroAt), line);
            }
        }
        if (beats.get("outro") != null && !used.contains("outro")) {
            headlines.add(beats.get("outro"));
        }
        if (headlines.isEmpty()) {
            headlines.add("Résumé du rallye indisponible pour le moment.");
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

    private Map<String, String> buildFinalBeats(
            int seed,
            List<RallyeStandingRow> ranked,
            String championName,
            String championTime,
            List<RallyeScratchTally> scratchLeaders,
            List<RallyeBoucleRecap> boucleRecaps,
            int totalEs
    ) {
        Map<String, String> beats = new LinkedHashMap<>();

        beats.put("intro", RecapPhrases.pick(seed,
                "Le rideau tombe sur ce rallye. Voici ce qu’il faudra retenir.",
                "Fin de rallye — place au bilan de cette édition.",
                "Les chronos sont figés. Retour sur une journée bien remplie.",
                "Clap de fin. Quelques angles pour raconter cette édition.",
                "Bilan express : ce qui a fait basculer le général.",
                "Spéciales terminées. On décortique le classement et les faits marquants.",
                "Dernière ES bouclée — voici l’histoire de ce rallye en quelques lignes."
        ));

        if (championName != null) {
            RallyeStandingRow champ = ranked.get(0);
            String champLabel = labelled(champ);
            String champCar = blankToNull(champ.car());
            beats.put("winner", RecapPhrases.pick(seed + 1,
                    "Chapeau bas à " + champLabel + " : vainqueur en " + championTime
                            + ". Une performance complète, du premier chrono au dernier.",
                    champLabel + " s’impose en " + championTime
                            + " — une victoire construite spéciale après spéciale.",
                    "Vainqueur du rallye : " + champLabel + " (" + championTime
                            + "). Autorité et régularité.",
                    "C’est " + champLabel + " qui ramène l’épreuve en " + championTime
                            + ". Mission accomplie.",
                    champCar != null
                            ? championName + " a fait parler sa " + champCar
                            + " du début à la fin : général en " + championTime + "."
                            : championName + " ramène le général en " + championTime + ".",
                    "Titre pour " + champLabel + " (" + championTime
                            + "). Le chronomètre a parlé."
            ));
        }

        if (ranked.size() >= 2) {
            RallyeStandingRow second = ranked.get(1);
            String secondLabel = labelled(second);
            String gap = second.gapFormatted();
            String gapPrev = second.gapToPreviousFormatted();
            Double gapSec = parseGapSeconds(gap);
            if (gap != null && !"—".equals(gap)) {
                if (gapSec != null && gapSec < 5.0) {
                    beats.put("runner_up", RecapPhrases.pick(seed + 2,
                            "Quel thriller jusqu’au bout ! " + secondLabel
                                    + " termine à seulement " + gap
                                    + " du vainqueur — on a frôlé le coup de théâtre.",
                            "Presque ! " + secondLabel + " n’a manqué le titre que de "
                                    + gap + ". Un souffle.",
                            secondLabel + " finit à " + gap
                                    + " : du grand spectacle jusqu’au damier.",
                            "Écart de " + gap + " pour " + secondLabel
                                    + " — la décision s’est jouée sur un détail.",
                            "À " + gap + " seulement, " + secondLabel
                                    + " a rendu le final irrespirable."
                    ));
                } else {
                    beats.put("runner_up", RecapPhrases.pick(seed + 2,
                            secondLabel + " monte sur la deuxième marche (" + gap
                                    + " du leader"
                                    + (gapPrev != null && !"—".equals(gapPrev) ? ", " + gapPrev + " de P1" : "")
                                    + "). Une belle course malgré tout.",
                            "P2 pour " + secondLabel + " (" + gap
                                    + ") : solide jusqu’au bout.",
                            secondLabel + " prend la deuxième place avec " + gap
                                    + " de retard au général.",
                            "Deuxième du général : " + secondLabel + " (" + gap + ").",
                            secondLabel + " consolide le podium en P2 (" + gap + ")."
                    ));
                }
            }
            if (gapPrev != null && !"—".equals(gapPrev) && ranked.size() >= 2) {
                beats.put("interval", RecapPhrases.pick(seed + 16,
                        "Entre le 1er et le 2e, l’écart se chiffre à " + gapPrev
                                + " — c’est là que le rallye s’est joué.",
                        "Intervalle de tête : " + gapPrev + " entre "
                                + labelled(ranked.get(0)) + " et " + secondLabel + ".",
                        secondLabel + " a lâché " + gapPrev
                                + " sur le vainqueur, spéciale après spéciale."
                ));
            }
        }

        if (ranked.size() >= 3) {
            RallyeStandingRow third = ranked.get(2);
            String thirdLabel = labelled(third);
            String gap3 = third.gapFormatted();
            String gap3prev = third.gapToPreviousFormatted();
            beats.put("p3", RecapPhrases.pick(seed + 3,
                    "Et " + thirdLabel + " complète le podium"
                            + (gap3prev != null && !"—".equals(gap3prev)
                            ? " à " + gap3prev + " du deuxième" : "")
                            + " : la récompense d’une journée bien menée.",
                    "Troisième marche pour " + thirdLabel
                            + (gap3 != null && !"—".equals(gap3) ? " (" + gap3 + " du leader)" : "")
                            + " — bien mérité.",
                    thirdLabel + " glisse sur le podium final"
                            + (gap3prev != null && !"—".equals(gap3prev) ? " (" + gap3prev + " de P2)" : "") + ".",
                    "P3 acquis pour " + thirdLabel + ".",
                    gap3 != null && !"—".equals(gap3)
                            ? thirdLabel + " verrouille le podium (" + gap3 + " au 1er"
                            + (gap3prev != null && !"—".equals(gap3prev) ? ", " + gap3prev + " de P2" : "")
                            + ")."
                            : thirdLabel + " prend la dernière marche du podium."
            ));
        }

        if (ranked.size() >= 3) {
            RallyeStandingRow a = ranked.get(0);
            RallyeStandingRow b = ranked.get(1);
            RallyeStandingRow c = ranked.get(2);
            beats.put("podium_pack", RecapPhrases.pick(seed + 4,
                    "Podium final : " + labelled(a) + ", " + labelled(b) + " et " + labelled(c)
                            + ". Trois lectures différentes d’une même journée.",
                    "Le trio de tête — " + labelled(a) + " / " + labelled(b) + " / " + labelled(c)
                            + " — résume bien l’intensité du rallye.",
                    "Sur le podium : " + labelled(a) + " devant " + labelled(b)
                            + ", " + labelled(c) + " complète le tableau.",
                    labelled(a) + " domine, " + labelled(b) + " répond, " + labelled(c)
                            + " accroche le bronze. Classique… et efficace."
            ));
        }

        if (!scratchLeaders.isEmpty()) {
            RallyeScratchTally top = scratchLeaders.get(0);
            int pct = (int) Math.round(100.0 * top.count() / (double) totalEs);
            if (top.count() >= 5) {
                beats.put("scratch_king", RecapPhrases.pick(seed + 5,
                        "Mention spéciale vitesse pure : " + top.pilotName()
                                + " raflé " + top.count()
                                + " scratchs. Quand ça marche, ça marche vraiment.",
                        top.pilotName() + " a maraudé " + top.count()
                                + " scratchs — une démonstration de pointe.",
                        "Festival de scratchs pour " + top.pilotName() + " : "
                                + top.count() + " ES gagnées.",
                        top.count() + " meilleurs temps au nom de " + top.pilotName()
                                + " (" + pct + "% des ES). Difficile de faire plus clair.",
                        "Stat qui claque : " + top.pilotName() + " gagne " + top.count()
                                + " ES sur " + totalEs + "."
                ));
            } else {
                beats.put("scratch_king", RecapPhrases.pick(seed + 5,
                        "Au rayon des plus rapides sur une ES, " + top.pilotName()
                                + " sort du lot avec " + top.count()
                                + " scratch" + (top.count() > 1 ? "s" : "") + ".",
                        "Meilleurs tours : " + top.pilotName() + " mène avec "
                                + top.count() + " scratch" + (top.count() > 1 ? "s" : "") + ".",
                        top.pilotName() + " s’adjuge " + top.count()
                                + " scratch" + (top.count() > 1 ? "s" : "")
                                + " sur l’ensemble du rallye.",
                        "Côté vitesse pure, " + top.pilotName() + " sort du lot ("
                                + top.count() + ")."
                ));
            }
            if (scratchLeaders.size() > 1) {
                RallyeScratchTally second = scratchLeaders.get(1);
                beats.put("scratch_second", RecapPhrases.pick(seed + 6,
                        second.pilotName() + " n’était pas en reste non plus ("
                                + second.count() + " ES gagnée"
                                + (second.count() > 1 ? "s" : "") + ").",
                        "Dans le sillage, " + second.pilotName() + " glane "
                                + second.count() + " ES.",
                        "Autre homme fort des spéciales : " + second.pilotName()
                                + " (" + second.count() + ").",
                        second.pilotName() + " répond présent avec " + second.count()
                                + " scratch" + (second.count() > 1 ? "s" : "") + ".",
                        "Relais scratchs : " + second.pilotName() + " en prend "
                                + second.count() + " aussi."
                ));
            }
            if (scratchLeaders.size() >= 3) {
                String pack = scratchLeaders.stream()
                        .limit(5)
                        .map(s -> s.pilotName() + " (" + s.count() + ")")
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                beats.put("scratch_pack", RecapPhrases.pick(seed + 21,
                        "Les scratchs se sont partagés : " + pack + ".",
                        "Plusieurs pilotes ont mis leur nom sur une ES : " + pack + ".",
                        "Au-delà du podium, les meilleurs temps tournent aussi : " + pack + "."
                ));
            }
        }

        // Vitesse pure ≠ titre
        if (championName != null && !scratchLeaders.isEmpty()) {
            RallyeScratchTally top = scratchLeaders.get(0);
            int champScratches = scratchLeaders.stream()
                    .filter(s -> championName.equals(s.pilotName()))
                    .mapToInt(RallyeScratchTally::count)
                    .findFirst()
                    .orElse(0);
            if (!championName.equals(top.pilotName())) {
                beats.put("speed_vs_title", RecapPhrases.pick(seed + 7,
                        "Paradoxe intéressant : " + top.pilotName() + " mène aux scratchs ("
                                + top.count() + "), mais c’est " + championName
                                + " qui gagne le général. La régularité a tranché.",
                        "Le plus rapide sur une ES n’est pas forcément le vainqueur : "
                                + top.pilotName() + " (" + top.count() + " scratchs) s’incline au général face à "
                                + championName + ".",
                        championName + " titré avec « seulement » " + champScratches
                                + " scratch" + (champScratches > 1 ? "s" : "")
                                + " — preuve que le classement se construit aussi sans tout gagner.",
                        "Leçon du jour : " + championName + " gagne le rallye, "
                                + top.pilotName() + " gagne les chronos flash. Deux combats différents."
                ));
            } else if (champScratches >= Math.max(3, totalEs / 2)) {
                beats.put("speed_vs_title", RecapPhrases.pick(seed + 7,
                        "Doublé parfait pour " + championName + " : général + "
                                + champScratches + " scratchs. Domination nette.",
                        championName + " n’a pas seulement gagné : il a aussi été le plus souvent le plus rapide ("
                                + champScratches + " ES).",
                        "Titre et vitesse : " + championName + " empoche les deux volets ("
                                + champScratches + " scratchs)."
                ));
            }
        }

        // Dominance / signature de boucle
        Map<String, List<Integer>> bouclesByPilot = new LinkedHashMap<>();
        for (RallyeBoucleRecap b : boucleRecaps) {
            if (b.hasData() && b.leaderName() != null) {
                bouclesByPilot.computeIfAbsent(b.leaderName(), k -> new ArrayList<>()).add(b.boucle());
            }
        }
        long bouclesWithData = boucleRecaps.stream().filter(RallyeBoucleRecap::hasData).count();
        bouclesByPilot.entrySet().stream()
                .max(Comparator.comparingInt(e -> e.getValue().size()))
                .ifPresent(e -> {
                    List<Integer> won = e.getValue();
                    if (won.size() >= 2) {
                        String list = won.stream().map(n -> "boucle " + n)
                                .reduce((a, b) -> a + ", " + b).orElse("");
                        beats.put("boucle_dom", RecapPhrases.pick(seed + 8,
                                e.getKey() + " a dominé le rythme des boucles (" + list
                                        + "). Régularité et efficacité.",
                                "Maître des boucles : " + e.getKey() + " (" + list + ").",
                                e.getKey() + " a enchaîné sur " + list
                                        + " — une vraie constance.",
                                "Sur " + list + ", c’est " + e.getKey()
                                        + " qui a dicté le tempo.",
                                e.getKey() + " s’offre " + won.size()
                                        + " boucles : le tempo du rallye lui appartenait."
                        ));
                    } else if (won.size() == 1 && bouclesWithData > 1) {
                        beats.put("boucle_dom", RecapPhrases.pick(seed + 8,
                                "Sur une boucle, " + e.getKey()
                                        + " a été intouchable (boucle " + won.get(0) + ").",
                                e.getKey() + " a marqué la boucle " + won.get(0)
                                        + " de son empreinte.",
                                "Intouchable sur la boucle " + won.get(0) + " : "
                                        + e.getKey() + ".",
                                "Une boucle sous contrôle pour " + e.getKey()
                                        + " (boucle " + won.get(0) + ")."
                        ));
                    }
                });

        // Comeback : vainqueur qui n'a pas mené la 1re boucle
        if (championName != null && boucleRecaps.size() >= 2) {
            RallyeBoucleRecap first = boucleRecaps.get(0);
            if (first.hasData() && first.leaderName() != null
                    && !championName.equals(first.leaderName())) {
                beats.put("comeback", RecapPhrases.pick(seed + 9,
                        "Remontée notée : après la boucle 1 derrière "
                                + first.leaderName() + ", " + championName
                                + " inverse la tendance et gagne le rallye.",
                        championName + " n’a pas mené d’entrée ("
                                + first.leaderName() + " en boucle 1) — la suite a basculé.",
                        "Scénario en deux temps : " + first.leaderName()
                                + " lance, " + championName + " termine devant au général.",
                        "Comeback réussi pour " + championName
                                + ", qui reprend le dessus après une boucle 1 à remonter."
                ));
            }
        }

        // Plateaux / densite
        if (ranked.size() >= 4) {
            int nameTo = Math.min(ranked.size(), 8);
            String midNames = ranked.subList(3, nameTo).stream()
                    .map(RallyeStandingRow::name)
                    .filter(n -> n != null && !n.isBlank())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            beats.put("field", RecapPhrases.pick(seed + 10,
                    ranked.size() + " pilotes classés, " + totalEs
                            + " ES au programme"
                            + (midNames.isEmpty() ? "." : " — dans le peloton on retrouve " + midNames
                            + (ranked.size() > nameTo ? "…" : "") + "."),
                    "Densité du jour : " + ranked.size()
                            + " concurrents au général"
                            + (midNames.isEmpty() ? "." : ", dont " + midNames
                            + (ranked.size() > nameTo ? " et d’autres" : "") + "."),
                    "Avec " + ranked.size() + " chronos au général, le plateau va du podium jusqu’à "
                            + (midNames.isEmpty() ? "un vrai peloton." : midNames
                            + (ranked.size() > nameTo ? "…" : "") + "."),
                    "Édition bien fournie — " + ranked.size()
                            + " noms au classement, parmi eux " + midNames
                            + (ranked.size() > nameTo ? "…" : "") + "."
            ));
        }

        Map<String, List<RallyeStandingRow>> byCar = new LinkedHashMap<>();
        for (RallyeStandingRow r : ranked) {
            String car = blankToNull(r.car());
            if (car != null) {
                byCar.computeIfAbsent(car, k -> new ArrayList<>()).add(r);
            }
        }
        if (!byCar.isEmpty()) {
            Map.Entry<String, List<RallyeStandingRow>> topCar = byCar.entrySet().stream()
                    .max(Comparator.comparingInt(e -> e.getValue().size()))
                    .orElse(null);
            if (topCar != null && topCar.getValue().size() >= 2) {
                List<String> carNames = topCar.getValue().stream()
                        .limit(4)
                        .map(RallyeStandingRow::name)
                        .toList();
                String sample = String.join(", ", carNames);
                beats.put("cars", RecapPhrases.pick(seed + 17,
                        "Côté voitures, la " + topCar.getKey() + " était la plus vue ("
                                + topCar.getValue().size() + " au général, dont " + sample
                                + (topCar.getValue().size() > 4 ? "…" : "") + ").",
                        "Le parc était marqué par les " + topCar.getKey()
                                + " : " + topCar.getValue().size() + " pilotes alignés, "
                                + sample + " en tête de liste.",
                        topCar.getValue().size() + " " + topCar.getKey()
                                + " au classement — le modèle star de cette édition."
                ));
                List<RallyeStandingRow> same = topCar.getValue();
                if (same.size() >= 2) {
                    RallyeStandingRow ca = same.get(0);
                    RallyeStandingRow cb = same.get(1);
                    String intra = (ca.totalSeconds() != null && cb.totalSeconds() != null)
                            ? RallyeTimeFormat.formatGap(cb.totalSeconds() - ca.totalSeconds())
                            : null;
                    beats.put("same_car", RecapPhrases.pick(seed + 18,
                            "Meilleure " + topCar.getKey() + " du jour : " + labelled(ca)
                                    + (intra != null && !"—".equals(intra)
                                    ? ", devant " + cb.name() + " (" + intra + ")."
                                    : "."),
                            "Match " + topCar.getKey() + " : " + ca.name() + " prend l’avantage sur "
                                    + cb.name()
                                    + (intra != null ? " (" + intra + ")" : "") + ".",
                            "Chez les " + topCar.getKey() + ", " + ca.name()
                                    + " sort du lot devant " + cb.name() + "."
                    ));
                }
            } else if (ranked.get(0).car() != null) {
                Set<String> models = byCar.keySet();
                beats.put("cars", RecapPhrases.pick(seed + 17,
                        "Le vainqueur " + labelled(ranked.get(0))
                                + " a su faire la différence sur un plateau varié ("
                                + models.size() + " modèles différents).",
                        "Diversité au parc : " + models.size()
                                + " voitures différentes, et c’est la "
                                + ranked.get(0).car() + " qui ramène le général."
                ));
            }
        }

        if (ranked.size() >= 4) {
            RallyeStandingRow p4row = ranked.get(3);
            String gapP4 = p4row.gapToPreviousFormatted();
            beats.put("p4", RecapPhrases.pick(seed + 19,
                    labelled(p4row) + " termine au pied du podium"
                            + (gapP4 != null && !"—".equals(gapP4) ? " à " + gapP4 + " de P3" : "")
                            + " — une 4e place qui se joue souvent à peu.",
                    "Hors podium : " + labelled(p4row) + " en "
                            + p4row.totalFormatted()
                            + (p4row.gapFormatted() != null && !"—".equals(p4row.gapFormatted())
                            ? " (" + p4row.gapFormatted() + " du leader)" : "") + ".",
                    labelled(p4row) + " accroche la 4e place, juste derrière le bronze."
            ));
        }

        // Batailles de mi-peloton : écarts entre voisins, beaucoup de noms
        List<ConsecutiveGap> midGaps = midfieldGaps(consecutiveGaps(ranked));
        if (!midGaps.isEmpty()) {
            beats.put("midfield", duelLine(seed + 11, midGaps.getFirst(), null));
            if (midGaps.size() >= 2) {
                String extra = extraDuelsLine(
                        seed + 22,
                        midGaps.subList(1, Math.min(4, midGaps.size())),
                        null
                );
                if (extra != null) {
                    beats.put("duels", extra);
                }
            }
        }
        String peloton = pelotonLineFromRows(seed + 23, ranked);
        if (peloton != null) {
            beats.put("peloton", peloton);
        }

        // ES la plus serrée (sans pointer le plus lent)
        RallyeStageHighlight tightest = null;
        double tightestGap = Double.MAX_VALUE;
        RallyeStageHighlight otherStage = null;
        for (RallyeBoucleRecap b : boucleRecaps) {
            if (b.stages() == null) {
                continue;
            }
            for (RallyeStageHighlight st : b.stages()) {
                otherStage = st;
                Double g = parseGapSeconds(st.worstGapToScratch());
                if (g == null) {
                    continue;
                }
                if (g > 0 && g < tightestGap) {
                    tightestGap = g;
                    tightest = st;
                }
            }
        }
        if (tightest != null && tightestGap < 1.5) {
            beats.put("tightest", RecapPhrases.pick(seed + 12,
                    "ES la plus sniper : la ES " + tightest.esNumber()
                            + " — le plateau était ultra-serré (scratch "
                            + labelledFromRanked(ranked, tightest.scratchPilotName())
                            + " en " + tightest.scratchTime() + ").",
                    "Chronos collés sur la ES " + tightest.esNumber()
                            + ". " + labelledFromRanked(ranked, tightest.scratchPilotName())
                            + " sort du lot d’un rien (" + tightest.scratchTime() + ").",
                    "Le suspense d’une ES : n°" + tightest.esNumber()
                            + ", où " + labelledFromRanked(ranked, tightest.scratchPilotName())
                            + " arrache le scratch dans un mouchoir (" + tightest.scratchTime() + ")."
            ));
        }

        RallyeStageHighlight zoom = tightest != null ? tightest : otherStage;
        if (zoom != null) {
            beats.put("es_detail", RecapPhrases.pick(seed + 20,
                    "Zoom sur une spéciale : ES " + zoom.esNumber()
                            + ", scratch de " + labelledFromRanked(ranked, zoom.scratchPilotName())
                            + " en " + zoom.scratchTime() + ".",
                    labelledFromRanked(ranked, zoom.scratchPilotName())
                            + " a planté un " + zoom.scratchTime()
                            + " de référence sur l’ES " + zoom.esNumber() + ".",
                    "L’ES " + zoom.esNumber() + " restera : "
                            + labelledFromRanked(ranked, zoom.scratchPilotName())
                            + " devant (" + zoom.scratchTime() + ")."
            ));
        }

        beats.put("outro", RecapPhrases.pick(seed + 15,
                "Merci à tous les pilotes — rendez-vous pour la prochaine spéciale.",
                "Bravo à tout le plateau. À bientôt sur une nouvelle ES.",
                "Fin de chapitre — merci aux pilotes, place à la suite du calendrier.",
                "Clap de fin. On se retrouve pour la prochaine édition.",
                "Rideau. Les chronos resteront — l’envie de remettre ça aussi.",
                "À la prochaine spéciale. Le plateau a livré."
        ));

        return beats;
    }

    private static String carOfPilot(Rallye rallye, Long id) {
        if (id == null) {
            return null;
        }
        for (RallyePilot p : rallye.getPilots()) {
            if (id.equals(p.getId())) {
                return blankToNull(p.getCar());
            }
        }
        return null;
    }

    private static String labelled(String name, String car) {
        return name == null ? "" : name;
    }

    private static String labelled(RallyeStandingRow row) {
        return labelled(row.name(), row.car());
    }

    private static String labelledFromRanked(List<RallyeStandingRow> ranked, String name) {
        if (name == null) {
            return "";
        }
        for (RallyeStandingRow r : ranked) {
            if (name.equals(r.name())) {
                return labelled(r);
            }
        }
        return name;
    }

    private String labelledByName(Rallye rallye, String name) {
        return labelled(name, carOfPilot(rallye, findPilotIdByName(rallye, name)));
    }

    private static Double parseGapSeconds(String gap) {
        if (gap == null || gap.isBlank() || "—".equals(gap) || "-".equals(gap)) {
            return null;
        }
        String raw = gap.trim();
        if (raw.startsWith("+")) {
            raw = raw.substring(1);
        }
        try {
            return RallyeTimeFormat.parse(raw);
        } catch (Exception e) {
            return null;
        }
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
    public RallyeImportResult importPilotsFromExcel(Long rallyeId, String filePath) {
        Rallye rallye = requireEditable(rallyeId);
        Map<String, RallyePilot> byName = new HashMap<>();
        for (RallyePilot p : rallye.getPilots()) {
            byName.put(normalizeName(p.getName()), p);
        }

        int importedPilots = 0;
        int importedTimes = 0;
        int bouclesDetected = 0;
        int stagesDetected = 0;

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = findPilotesSheet(workbook);
            if (sheet == null) {
                throw new IllegalArgumentException("Feuille « Pilotes » introuvable dans le fichier");
            }

            Row header = findPilotesHeaderRow(sheet);
            if (header == null) {
                throw new IllegalArgumentException("En-tête « Pilotes » introuvable");
            }
            List<StageColumn> stageColumns = parseStageColumns(header);
            bouclesDetected = stageColumns.stream().mapToInt(StageColumn::boucle).max().orElse(0);
            stagesDetected = stageColumns.stream()
                    .filter(c -> c.stage() > 0)
                    .mapToInt(StageColumn::stage)
                    .max()
                    .orElse(0);

            if (bouclesDetected > rallye.getBoucleCount()) {
                rallye.setBoucleCount(bouclesDetected);
            }
            if (stagesDetected > rallye.getStagesPerBoucle()) {
                rallye.setStagesPerBoucle(stagesDetected);
            }

            int headerRowIdx = header.getRowNum();
            for (int i = headerRowIdx + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String name = cellAsString(row.getCell(1));
                if (name.isEmpty() || isPlaceholderName(name)) {
                    continue;
                }

                String key = normalizeName(name);
                RallyePilot pilot = byName.get(key);
                if (pilot == null) {
                    String car = cellAsString(row.getCell(2));
                    String category = cellAsString(row.getCell(3));
                    Integer startNumber = cellAsInteger(row.getCell(0));
                    pilot = new RallyePilot(
                            name.trim(),
                            blankToNull(car),
                            blankToNull(category),
                            startNumber != null ? startNumber : (byName.size() + 1)
                    );
                    rallye.addPilot(pilot);
                    byName.put(key, pilot);
                    importedPilots++;
                } else {
                    // Complète voiture / catégorie si vides
                    if (blankToNull(pilot.getCar()) == null) {
                        pilot.setCar(blankToNull(cellAsString(row.getCell(2))));
                    }
                    if (blankToNull(pilot.getCategory()) == null) {
                        pilot.setCategory(blankToNull(cellAsString(row.getCell(3))));
                    }
                    Integer startNumber = cellAsInteger(row.getCell(0));
                    if (pilot.getStartNumber() == null && startNumber != null) {
                        pilot.setStartNumber(startNumber);
                    }
                }

                for (StageColumn col : stageColumns) {
                    if (col.boucle() > rallye.getBoucleCount()) {
                        continue;
                    }
                    if (col.stage() > 0 && col.stage() > rallye.getStagesPerBoucle()) {
                        continue;
                    }
                    Double seconds = cellAsTimeSeconds(row.getCell(col.colIndex()));
                    if (seconds == null) {
                        continue;
                    }
                    RallyeStageTime time = pilot.getOrCreateTime(col.boucle(), col.stage());
                    Double previous = time.getTimeSeconds();
                    time.setTimeSeconds(seconds);
                    if (previous == null || Double.compare(previous, seconds) != 0) {
                        importedTimes++;
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Impossible de lire le fichier Excel: " + e.getMessage());
        }

        return new RallyeImportResult(importedPilots, importedTimes, bouclesDetected, stagesDetected);
    }

    /**
     * Colonnes de temps sur la feuille Pilotes :
     * pour chaque boucle → ES 1, 2, 3, PENO, 4, 5 (séparées par « Après… » / « Boucle… »).
     */
    private static List<StageColumn> parseStageColumns(Row header) {
        List<StageColumn> columns = new ArrayList<>();
        int boucle = 0;
        int lastCell = header.getLastCellNum() > 0 ? header.getLastCellNum() : 0;

        for (int c = 5; c < lastCell; c++) {
            Cell cell = header.getCell(c);
            if (cell == null) {
                continue;
            }

            Integer stageNum = cellAsStageNumber(cell);
            if (stageNum != null) {
                if (stageNum == 1) {
                    boucle++;
                } else if (boucle == 0) {
                    boucle = 1;
                }
                columns.add(new StageColumn(boucle, stageNum, c));
                continue;
            }

            String label = cellAsString(cell).toLowerCase(Locale.ROOT);
            if (label.isEmpty()) {
                continue;
            }
            if (label.contains("peno") || label.contains("péno")) {
                if (boucle == 0) {
                    boucle = 1;
                }
                columns.add(new StageColumn(boucle, RallyeStageTime.PENALTY_STAGE, c));
            }
            // « Après… », « Boucle… », totaux → ignorés
        }
        return columns;
    }

    private static Row findPilotesHeaderRow(Sheet sheet) {
        int max = Math.min(sheet.getLastRowNum(), 10);
        for (int i = 0; i <= max; i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            String b = cellAsString(row.getCell(1)).toLowerCase(Locale.ROOT);
            if (b.startsWith("pilot")) {
                return row;
            }
        }
        return sheet.getRow(1);
    }

    private static Integer cellAsStageNumber(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            double v = cell.getNumericCellValue();
            if (v == Math.rint(v)) {
                int n = (int) v;
                if (n >= 1 && n <= 12) {
                    return n;
                }
            }
            return null;
        }
        String s = cellAsString(cell);
        if (s.matches("[1-9]|1[0-2]")) {
            return Integer.parseInt(s);
        }
        return null;
    }

    private static Double cellAsTimeSeconds(Cell cell) {
        if (cell == null) {
            return null;
        }
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        if (type == CellType.NUMERIC) {
            double v = cell.getNumericCellValue();
            if (v <= 0) {
                return null;
            }
            return v;
        }
        if (type == CellType.STRING) {
            String raw = cell.getStringCellValue();
            if (raw == null || raw.isBlank() || "-".equals(raw.trim()) || "—".equals(raw.trim())) {
                return null;
            }
            try {
                Double parsed = RallyeTimeFormat.parse(raw);
                if (parsed != null && parsed <= 0) {
                    return null;
                }
                return parsed;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private record StageColumn(int boucle, int stage, int colIndex) {
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
        Rallye rallye = requireEditable(rallyeId);
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
        Rallye rallye = requireEditable(rallyeId);
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
        Rallye rallye = requireEditable(rallyeId);
        if (rallye.getBoucleCount() >= 12) {
            throw new IllegalArgumentException("Maximum 12 boucles");
        }
        rallye.setBoucleCount(rallye.getBoucleCount() + 1);
        return rallye;
    }

    @Override
    public Rallye addStage(Long rallyeId) {
        Rallye rallye = requireEditable(rallyeId);
        if (rallye.getStagesPerBoucle() >= 12) {
            throw new IllegalArgumentException("Maximum 12 ES");
        }
        rallye.setStagesPerBoucle(rallye.getStagesPerBoucle() + 1);
        return rallye;
    }

    @Override
    public Rallye removeBoucle(Long rallyeId) {
        Rallye rallye = requireEditable(rallyeId);
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
        Rallye rallye = requireEditable(rallyeId);
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

    private void addToCalendar(Rallye rallye) {
        try {
            if (!clubCalendarService.upsertFromRallye(rallye)) {
                log.warn("Calendrier : rallye {} non ajouté (date ou nom ignoré).", rallye.getName());
            }
        } catch (RuntimeException e) {
            log.warn("Calendrier : impossible d’ajouter le rallye {} : {}", rallye.getName(), e.getMessage());
        }
    }

    private Rallye requireEditable(Long rallyeId) {
        Rallye rallye = get(rallyeId);
        assertEditable(rallye);
        return rallye;
    }

    private static void assertEditable(Rallye rallye) {
        if (rallye != null && rallye.isFinished()) {
            throw new IllegalStateException(
                    "Ce rallye est terminé : les informations ne peuvent plus être modifiées.");
        }
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

    private record ConsecutiveGap(
            String aheadName,
            String behindName,
            int aheadPos,
            int behindPos,
            double gapSeconds
    ) {
        String gapFmt() {
            return RallyeTimeFormat.formatGap(gapSeconds);
        }
    }

    private static String posLabel(int p) {
        if (p <= 0) {
            return "";
        }
        return p == 1 ? "1er" : p + "e";
    }

    private static List<ConsecutiveGap> consecutiveGaps(List<RallyeStandingRow> ranked) {
        List<ConsecutiveGap> out = new ArrayList<>();
        for (int i = 1; i < ranked.size(); i++) {
            RallyeStandingRow a = ranked.get(i - 1);
            RallyeStandingRow b = ranked.get(i);
            if (a.totalSeconds() == null || b.totalSeconds() == null
                    || a.name() == null || b.name() == null) {
                continue;
            }
            out.add(new ConsecutiveGap(
                    a.name(),
                    b.name(),
                    a.position(),
                    b.position(),
                    b.totalSeconds() - a.totalSeconds()
            ));
        }
        return out;
    }

    private static List<ConsecutiveGap> consecutiveGapsFromRanking(
            List<Map.Entry<Long, Double>> ranking,
            Map<Long, String> names
    ) {
        List<ConsecutiveGap> out = new ArrayList<>();
        for (int i = 1; i < ranking.size(); i++) {
            Map.Entry<Long, Double> a = ranking.get(i - 1);
            Map.Entry<Long, Double> b = ranking.get(i);
            if (a.getValue() == null || b.getValue() == null) {
                continue;
            }
            String ahead = names.get(a.getKey());
            String behind = names.get(b.getKey());
            if (ahead == null || behind == null) {
                continue;
            }
            out.add(new ConsecutiveGap(ahead, behind, i, i + 1, b.getValue() - a.getValue()));
        }
        return out;
    }

    /** Duels à partir de la 3e place (milieu / fin de peloton, du plus serré au plus large). */
    private static List<ConsecutiveGap> midfieldGaps(List<ConsecutiveGap> all) {
        return all.stream()
                .filter(g -> g.aheadPos() >= 3)
                .sorted(Comparator.comparingDouble(ConsecutiveGap::gapSeconds))
                .toList();
    }

    private static String duelLine(int seed, ConsecutiveGap g, String where) {
        String whereBit = where == null || where.isBlank() ? "" : " " + where;
        String gap = g.gapFmt();
        String a = g.aheadName() + " (" + posLabel(g.aheadPos()) + ")";
        String b = g.behindName() + " (" + posLabel(g.behindPos()) + ")";
        if (g.gapSeconds() < 1.0) {
            return RecapPhrases.pick(seed,
                    "Dans le peloton" + whereBit + ", " + a + " et " + b
                            + " ne se tiennent qu’à " + gap + " — un vrai match dans le match.",
                    "Écart microscopique" + whereBit + " entre " + a + " et " + b
                            + " : " + gap + ".",
                    a + " et " + b + " se collent" + whereBit + " (" + gap + ")."
            );
        }
        if (g.gapSeconds() < 5.0) {
            return RecapPhrases.pick(seed,
                    "Combat serré" + whereBit + " : " + a + " garde " + b + " à " + gap + ".",
                    "Entre " + a + " et " + b + whereBit + ", seulement " + gap + ".",
                    a + " et " + b + " se tiennent à " + gap + whereBit + "."
            );
        }
        return RecapPhrases.pick(seed,
                "Plus loin" + whereBit + ", " + a + " et " + b
                        + " sont séparés de " + gap + ".",
                "Écart de " + gap + " entre " + a + " et " + b + whereBit + "."
        );
    }

    private static String extraDuelsLine(int seed, List<ConsecutiveGap> extra, String where) {
        if (extra == null || extra.isEmpty()) {
            return null;
        }
        List<String> bits = new ArrayList<>();
        for (ConsecutiveGap g : extra) {
            bits.add(g.aheadName() + " / " + g.behindName()
                    + " (" + posLabel(g.aheadPos()) + "–" + posLabel(g.behindPos())
                    + ", " + g.gapFmt() + ")");
        }
        String joined = String.join(", ", bits);
        String whereBit = where == null || where.isBlank() ? "" : " " + where;
        return RecapPhrases.pick(seed,
                "D’autres écarts à suivre" + whereBit + " : " + joined + ".",
                "Le milieu se joue aussi sur des duels" + whereBit + " : " + joined + ".",
                "À surveiller dans le peloton" + whereBit + " : " + joined + "."
        );
    }

    private static String pelotonLineFromRows(int seed, List<RallyeStandingRow> ranked) {
        if (ranked.size() < 5) {
            return null;
        }
        List<String> bits = new ArrayList<>();
        for (int i = 3; i < ranked.size(); i++) {
            RallyeStandingRow r = ranked.get(i);
            if (r.name() == null || r.name().isBlank()) {
                continue;
            }
            String bit = r.name() + " " + posLabel(r.position());
            if (r.gapToPrevious() != null && r.gapToPrevious() > 0 && r.gapToPrevious() < 3.0) {
                bit += " (" + RallyeTimeFormat.formatGap(r.gapToPrevious()) + " du précédent)";
            }
            bits.add(bit);
        }
        return formatPeloton(seed, bits, ranked.size() - 3, null);
    }

    private static String pelotonLineFromRanking(
            int seed,
            List<Map.Entry<Long, Double>> ranking,
            Map<Long, String> names,
            String where
    ) {
        if (ranking.size() < 5) {
            return null;
        }
        List<String> bits = new ArrayList<>();
        for (int i = 3; i < ranking.size(); i++) {
            String name = names.get(ranking.get(i).getKey());
            if (name == null || name.isBlank()) {
                continue;
            }
            String bit = name + " " + posLabel(i + 1);
            double prev = ranking.get(i).getValue() - ranking.get(i - 1).getValue();
            if (prev > 0 && prev < 3.0) {
                bit += " (" + RallyeTimeFormat.formatGap(prev) + " du précédent)";
            }
            bits.add(bit);
        }
        return formatPeloton(seed, bits, ranking.size() - 3, where);
    }

    private static String formatPeloton(int seed, List<String> bits, int totalMid, String where) {
        if (bits.isEmpty()) {
            return null;
        }
        int cap = 8;
        List<String> shown = bits.size() > cap ? bits.subList(0, cap) : bits;
        String joined = String.join(", ", shown);
        if (bits.size() > cap) {
            joined += " et " + (bits.size() - cap) + " autres";
        }
        String whereBit = where == null || where.isBlank() ? "" : " " + where;
        return RecapPhrases.pick(seed,
                "Le milieu de peloton" + whereBit + " a aussi son mot à dire : " + joined + ".",
                "Derrière la tête, le peloton est dense" + whereBit + " : " + joined + ".",
                "On retrouve notamment" + whereBit + " " + joined + ".",
                totalMid + " pilotes encore dans le ventre du classement"
                        + whereBit + ", parmi eux " + joined + "."
        );
    }

    private record StandingAccumulator(RallyePilot pilot, Double total, int completed, int expected) {
    }
}
