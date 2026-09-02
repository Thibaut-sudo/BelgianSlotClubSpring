package org.example.belgianslotclubspring.services;

import org.example.belgianslotclubspring.entities.BestTime;
import org.example.belgianslotclubspring.entities.LapsPerTrack;
import org.example.belgianslotclubspring.entities.Qualif;
import org.example.belgianslotclubspring.entities.RaceResult;
import org.example.belgianslotclubspring.models.RaceDayRecap;
import org.example.belgianslotclubspring.models.RaceTrackScratch;
import org.example.belgianslotclubspring.utils.RallyeTimeFormat;
import org.example.belgianslotclubspring.utils.RecapPhrases;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class RaceDayRecapService {

    public RaceDayRecap build(List<Qualif> qualifications, List<RaceResult> raceResults) {
        List<Qualif> quals = qualifications == null ? List.of() : qualifications;
        List<RaceResult> results = raceResults == null ? List.of() : raceResults;

        List<String> qualiHeadlines = buildQualiHeadlines(quals);
        List<String> raceHeadlines = new ArrayList<>();
        List<String> podium = new ArrayList<>();
        List<RaceTrackScratch> trackScratches = new ArrayList<>();
        List<String> scratchLeaders = new ArrayList<>();

        if (!results.isEmpty()) {
            buildRaceNarrative(quals, results, raceHeadlines, podium, trackScratches, scratchLeaders);
        }

        return new RaceDayRecap(
                !quals.isEmpty(),
                !results.isEmpty(),
                qualiHeadlines,
                raceHeadlines,
                podium,
                trackScratches,
                scratchLeaders
        );
    }

    private List<String> buildQualiHeadlines(List<Qualif> rawQuals) {
        List<Qualif> quals = rawQuals.stream()
                .filter(q -> q != null && q.getPilotName() != null && !q.getPilotName().isBlank())
                .sorted(Comparator.comparingDouble(Qualif::getBestTime))
                .toList();
        if (quals.isEmpty()) {
            return List.of();
        }

        Qualif pole = quals.get(0);
        String poleName = pole.getPilotName();
        String poleTime = formatTime(pole.getBestTime());
        int n = quals.size();
        int seed = RecapPhrases.seed("quali-v3", poleName, poleTime, n);

        Map<String, String> beats = new LinkedHashMap<>();
        if (quals.size() == 1) {
            beats.put("pole", poleName + " prend la pole en " + poleTime + " (seul chrono).");
            return List.of(beats.get("pole"));
        }

        Qualif second = quals.get(1);
        double gap2 = second.getBestTime() - pole.getBestTime();
        String secondName = second.getPilotName();
        String gap2Fmt = formatGap(gap2);

        if (gap2 < 0.08) {
            beats.put("pole", RecapPhrases.pick(seed + 1,
                    n + " pilotes en qualifs. Pole pour " + poleName + " en " + poleTime
                            + " — " + secondName + " n’est qu’à " + gap2Fmt + ".",
                    poleName + " arrache la pole en " + poleTime + ", " + secondName
                            + " collé à " + gap2Fmt + " (" + n + " pilotes).",
                    "Pole au finish : " + poleName + " (" + poleTime + ") devant "
                            + secondName + " à " + gap2Fmt + "."
            ));
        } else if (gap2 < 0.25) {
            beats.put("pole", RecapPhrases.pick(seed + 1,
                    n + " pilotes en qualifs. " + poleName + " en pole (" + poleTime
                            + "), " + secondName + " à " + gap2Fmt + ".",
                    "Pole pour " + poleName + " en " + poleTime + ", juste devant "
                            + secondName + " (" + gap2Fmt + ").",
                    poleName + " s’élance en tête (" + poleTime + "). "
                            + secondName + " suit à " + gap2Fmt + "."
            ));
        } else {
            beats.put("pole", RecapPhrases.pick(seed + 1,
                    n + " pilotes en qualifs. " + poleName + " creuse déjà en pole : "
                            + poleTime + ", " + gap2Fmt + " sur " + secondName + ".",
                    "Pole nette pour " + poleName + " (" + poleTime + "). "
                            + secondName + " est à " + gap2Fmt + ".",
                    poleName + " prend la pole en " + poleTime + " avec "
                            + gap2Fmt + " d’avance sur " + secondName + "."
            ));
        }

        if (quals.size() >= 3) {
            Qualif third = quals.get(2);
            double gap3 = third.getBestTime() - pole.getBestTime();
            double gap23 = third.getBestTime() - second.getBestTime();
            String thirdName = third.getPilotName();
            if (gap3 < 0.30) {
                beats.put("top3", RecapPhrases.pick(seed + 3,
                        "Les trois premiers tiennent en " + formatGap(gap3)
                                + " : " + poleName + ", " + secondName + ", " + thirdName + ".",
                        thirdName + " n’est qu’à " + formatGap(gap23)
                                + " du 2e — le haut de grille est compact.",
                        "P3 pour " + thirdName + " à " + formatGap(gap3) + " de la pole."
                ));
            } else {
                beats.put("top3", RecapPhrases.pick(seed + 3,
                        thirdName + " prend la 3e place en " + formatTime(third.getBestTime())
                                + " (" + formatGap(gap3) + " de la pole).",
                        "Derrière, " + thirdName + " signe un "
                                + formatTime(third.getBestTime()) + "."
                ));
            }
        }

        if (quals.size() >= 5) {
            Qualif closestMid = null;
            double closestGap = Double.MAX_VALUE;
            for (int i = 3; i < quals.size(); i++) {
                double g = quals.get(i).getBestTime() - quals.get(i - 1).getBestTime();
                if (g < closestGap) {
                    closestGap = g;
                    closestMid = quals.get(i);
                }
            }
            if (closestMid != null && closestGap < 0.15) {
                int pos = quals.indexOf(closestMid) + 1;
                beats.put("pack", RecapPhrases.pick(seed + 4,
                        "Plus loin, " + quals.get(pos - 2).getPilotName() + " et "
                                + closestMid.getPilotName() + " se tiennent à "
                                + formatGap(closestGap) + " ("
                                + posLabel(pos - 1) + "–" + posLabel(pos) + ").",
                        "Duel au milieu de grille : "
                                + quals.get(pos - 2).getPilotName() + " / "
                                + closestMid.getPilotName() + " ("
                                + formatGap(closestGap) + ")."
                ));
            }
            Qualif last = quals.get(quals.size() - 1);
            double spread = last.getBestTime() - pole.getBestTime();
            beats.put("grid_tail", RecapPhrases.pick(seed + 5,
                    last.getPilotName() + " ferme la grille à " + formatGap(spread)
                            + " de la pole.",
                    "Fond de grille : " + last.getPilotName() + " à "
                            + formatGap(spread) + "."
            ));
        }

        return assemble(new String[]{"pole", "top3", "pack", "grid_tail"}, beats, List.of("pole", "top3"), 5);
    }

    private void buildRaceNarrative(
            List<Qualif> quals,
            List<RaceResult> results,
            List<String> headlines,
            List<String> podium,
            List<RaceTrackScratch> trackScratches,
            List<String> scratchLeaders
    ) {
        List<RaceResult> ranked = results.stream()
                .filter(r -> r != null && r.getNom() != null && !r.getNom().isBlank())
                .sorted(Comparator.comparingDouble(RaceResult::getTotalTours).reversed())
                .toList();
        if (ranked.isEmpty()) {
            return;
        }

        RaceResult winner = ranked.get(0);
        String winnerName = winner.getNom();
        String winnerTours = formatLaps(winner.getTotalTours());
        int n = ranked.size();

        for (int i = 0; i < Math.min(3, ranked.size()); i++) {
            RaceResult r = ranked.get(i);
            podium.add((i + 1) + ". " + r.getNom() + " — " + formatLaps(r.getTotalTours()) + " tours");
        }

        List<TrackMark> marks = computeTrackMarks(ranked);
        for (TrackMark mark : marks) {
            trackScratches.add(new RaceTrackScratch(
                    mark.track,
                    mark.bestPilot,
                    formatTime(mark.bestLap),
                    mark.worstPilot != null ? mark.worstPilot : "—",
                    mark.worstPilot != null && mark.worstLap > mark.bestLap
                            ? formatGap(mark.worstLap - mark.bestLap) : "—"
            ));
        }

        Map<String, Integer> scratchCount = new HashMap<>();
        Map<String, List<Integer>> scratchTracks = new HashMap<>();
        for (TrackMark mark : marks) {
            scratchCount.merge(mark.bestPilot, 1, Integer::sum);
            scratchTracks.computeIfAbsent(mark.bestPilot, k -> new ArrayList<>()).add(mark.track);
        }
        List<Map.Entry<String, Integer>> leaders = scratchCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .toList();
        for (Map.Entry<String, Integer> e : leaders) {
            List<Integer> tracks = scratchTracks.getOrDefault(e.getKey(), List.of());
            scratchLeaders.add(e.getKey() + " — " + e.getValue()
                    + " piste" + (e.getValue() > 1 ? "s" : "")
                    + (tracks.isEmpty() ? "" : " (" + pistesShort(tracks) + ")"));
        }

        int seed = RecapPhrases.seed(
                "race-v4",
                winnerName,
                winnerTours,
                n,
                leaders.isEmpty() ? 0 : leaders.get(0).getValue()
        );

        Map<String, String> beats = buildRaceBeats(
                seed, ranked, quals, marks, leaders, scratchTracks, winnerName, winnerTours, n
        );

        String[] template = RecapPhrases.pickTemplate(seed,
                new String[]{"winner", "podium", "p4", "scratch_king", "scratch_rest", "fastest_lap", "midfield", "duel", "mover", "tight_track", "spread", "last"},
                new String[]{"winner", "podium", "scratch_king", "fastest_lap", "pace_vs_peak", "p4", "midfield", "drop", "wide_track", "last", "spread"},
                new String[]{"winner", "pole", "podium", "p4", "scratch_king", "scratch_rest", "fastest_lap", "duel", "mover", "tight_track", "spread"},
                new String[]{"winner", "podium", "scratch_king", "p4", "midfield", "fastest_lap", "pace_vs_peak", "drop", "wide_track", "last"}
        );

        headlines.addAll(assemble(template, beats, List.of(
                "winner", "podium", "scratch_king", "fastest_lap", "p4", "midfield"
        ), List.of(
                "scratch_rest", "pace_vs_peak", "pole", "mover", "drop",
                "duel", "tight_track", "wide_track", "spread", "last"
        ), 12));
    }

    private Map<String, String> buildRaceBeats(
            int seed,
            List<RaceResult> ranked,
            List<Qualif> quals,
            List<TrackMark> marks,
            List<Map.Entry<String, Integer>> leaders,
            Map<String, List<Integer>> scratchTracks,
            String winnerName,
            String winnerTours,
            int n
    ) {
        Map<String, String> beats = new LinkedHashMap<>();
        double gap12 = ranked.size() >= 2
                ? ranked.get(0).getTotalTours() - ranked.get(1).getTotalTours() : -1;
        double gap23 = ranked.size() >= 3
                ? ranked.get(1).getTotalTours() - ranked.get(2).getTotalTours() : -1;

        Integer winnerStart = qualiPosition(quals, winnerName);
        String poleName = poleSitter(quals);
        boolean poleConverted = winnerStart != null && winnerStart == 1;
        boolean foldedPole = poleConverted || (winnerStart != null && winnerStart == 2);

        String secondName = ranked.size() >= 2 ? ranked.get(1).getNom() : null;
        beats.put("winner", winnerLine(
                seed, winnerName, winnerTours, secondName, gap12, winnerStart, poleName, n
        ));

        if (ranked.size() >= 3 && gap23 < 1.0) {
            RaceResult third = ranked.get(2);
            String thirdName = third.getNom();
            beats.put("podium", RecapPhrases.pick(seed + 2,
                    "Derrière, " + secondName + " et " + thirdName
                            + " se tiennent à " + lapsPhrase(gap23) + " pour la 2e place.",
                    secondName + " prend la 2e place, " + thirdName
                            + " n’est qu’à " + lapsPhrase(gap23) + ".",
                    secondName + " (" + formatLaps(ranked.get(1).getTotalTours())
                            + ") et " + thirdName + " ("
                            + formatLaps(third.getTotalTours())
                            + ") ne se tiennent qu’à " + lapsPhrase(gap23) + "."
            ));
        } else if (ranked.size() >= 3) {
            RaceResult third = ranked.get(2);
            beats.put("podium", RecapPhrases.pick(seed + 2,
                    third.getNom() + " complète le podium avec "
                            + formatLaps(third.getTotalTours()) + " tours, à "
                            + lapsPhrase(Math.max(0, gap23)) + " de " + secondName + ".",
                    "P3 pour " + third.getNom() + " ("
                            + formatLaps(third.getTotalTours()) + " tours, "
                            + lapsPhrase(ranked.get(0).getTotalTours() - third.getTotalTours())
                            + " du vainqueur)."
            ));
        }

        if (ranked.size() >= 4) {
            RaceResult p4 = ranked.get(3);
            double gapP4 = ranked.get(2).getTotalTours() - p4.getTotalTours();
            beats.put("p4", RecapPhrases.pick(seed + 19,
                    p4.getNom() + " termine 4e avec "
                            + formatLaps(p4.getTotalTours()) + " tours, à "
                            + lapsPhrase(gapP4) + " du podium.",
                    "Au pied du podium : " + p4.getNom() + " ("
                            + formatLaps(p4.getTotalTours()) + " tours), "
                            + lapsPhrase(gapP4) + " derrière " + ranked.get(2).getNom() + "."
            ));
        }

        if (ranked.size() >= 6) {
            int to = Math.min(ranked.size() - 1, 8);
            List<String> bits = new ArrayList<>();
            for (int i = 4; i < to; i++) {
                RaceResult r = ranked.get(i);
                double g = ranked.get(i - 1).getTotalTours() - r.getTotalTours();
                String bit = r.getNom() + " " + posLabel(i + 1)
                        + " (" + formatLaps(r.getTotalTours()) + ")";
                if (g > 0 && g < 1.5) {
                    bit += ", " + lapsPhrase(g) + " du précédent";
                }
                bits.add(bit);
            }
            if (!bits.isEmpty()) {
                beats.put("midfield", RecapPhrases.pick(seed + 23,
                        "Milieu de peloton : " + String.join(" · ", bits) + ".",
                        "Derrière, ça reste dense : " + String.join(", ", bits) + ".",
                        "On retrouve ensuite " + String.join(", ", bits) + "."
                ));
            }
        }

        buildSpeedBeats(beats, seed, ranked, marks, leaders, scratchTracks, winnerName);
        addTrackStories(beats, seed, marks, n);

        if (!foldedPole && winnerStart != null && winnerStart >= 3) {
            beats.put("pole", RecapPhrases.pick(seed + 9,
                    winnerName + " s’élançait " + posLabel(winnerStart)
                            + " sur la grille"
                            + (poleName != null ? " (pole : " + poleName + ")" : "")
                            + " — et gagne quand même.",
                    "Remontée : " + winnerName + " était " + posLabel(winnerStart)
                            + " des qualifs, premier au damier.",
                    "La pole de " + poleName + " ne résiste pas. "
                            + winnerName + " remonte depuis la "
                            + posLabel(winnerStart) + " place."
            ));
        }

        addQualiMoves(beats, seed, quals, ranked, winnerName);

        Consecutive closest = closestFrom(ranked, 5);
        if (closest != null && closest.gapLaps < 1.5) {
            beats.put("duel", RecapPhrases.pick(seed + 11,
                    "Dans le peloton, " + closest.ahead + " et " + closest.behind
                            + " se tiennent à " + lapsPhrase(closest.gapLaps)
                            + " (" + posLabel(closest.aheadPos) + "–"
                            + posLabel(closest.behindPos) + ").",
                    closest.ahead + " (" + posLabel(closest.aheadPos) + ") garde "
                            + closest.behind + " à " + lapsPhrase(closest.gapLaps) + "."
            ));
        }

        if (n >= 4) {
            RaceResult last = ranked.get(n - 1);
            double gapLast = ranked.get(0).getTotalTours() - last.getTotalTours();
            beats.put("spread", RecapPhrases.pick(seed + 16,
                    "De la tête à la queue : " + lapsPhrase(gapLast)
                            + " entre " + winnerName + " et " + last.getNom() + ".",
                    last.getNom() + " ferme la marche à " + lapsPhrase(gapLast)
                            + " du vainqueur (" + formatLaps(last.getTotalTours()) + " tours)."
            ));
            int lastWorst = 0;
            for (TrackMark m : marks) {
                if (last.getNom().equals(m.worstPilot) && !last.getNom().equals(m.bestPilot)) {
                    lastWorst++;
                }
            }
            if (lastWorst >= 4) {
                String worstBit = lastWorst >= 6
                        ? "plus lent sur les six pistes"
                        : "plus lent sur " + lastWorst + " pistes";
                beats.put("last", RecapPhrases.pick(seed + 8,
                        last.getNom() + " a enchaîné les galères : " + worstBit + ".",
                        "Journée compliquée pour " + last.getNom()
                                + " — " + worstBit + "."
                ));
            }
        }

        return beats;
    }

    private static String winnerLine(
            int seed,
            String winnerName,
            String winnerTours,
            String secondName,
            double gap12,
            Integer winnerStart,
            String poleName,
            int n
    ) {
        String field = n >= 8 ? " (" + n + " pilotes)" : "";
        String start;
        if (winnerStart != null && winnerStart == 1) {
            start = RecapPhrases.pick(seed + 1,
                    "Parti en pole, " + winnerName,
                    winnerName + ", élancé en tête de grille,"
            );
        } else if (winnerStart != null && winnerStart == 2) {
            start = RecapPhrases.pick(seed + 1,
                    "Élancé 2e, " + winnerName,
                    winnerName + " passe devant " + (poleName != null ? poleName : "la pole")
                            + " et"
            );
        } else {
            start = winnerName;
        }

        if (gap12 >= 0 && gap12 < 0.4) {
            return RecapPhrases.pick(seed + 4,
                    start + " gagne au finish" + field + " : " + winnerTours
                            + " tours, seulement " + lapsPhrase(gap12) + " devant " + secondName + ".",
                    start + " s’impose de justesse" + field + " (" + winnerTours
                            + " tours), " + lapsPhrase(gap12) + " d’avance sur " + secondName + "."
            );
        }
        if (gap12 >= 0 && gap12 < 2.0) {
            return RecapPhrases.pick(seed + 4,
                    start + " s’impose" + field + " avec " + winnerTours + " tours, "
                            + lapsPhrase(gap12) + " devant " + secondName + ".",
                    start + " gagne" + field + " avec " + winnerTours + " tours, "
                            + lapsPhrase(gap12) + " d’avance sur " + secondName + "."
            );
        }
        if (gap12 >= 2.0) {
            return RecapPhrases.pick(seed + 4,
                    start + " s’impose" + field + " avec " + winnerTours + " tours, "
                            + lapsPhrase(gap12) + " devant " + secondName + ".",
                    start + " gagne" + field + " avec " + winnerTours + " tours, "
                            + lapsPhrase(gap12) + " d’avance sur " + secondName + "."
            );
        }
        return start + " s’impose" + field + " avec " + winnerTours + " tours.";
    }

    private static void buildSpeedBeats(
            Map<String, String> beats,
            int seed,
            List<RaceResult> ranked,
            List<TrackMark> marks,
            List<Map.Entry<String, Integer>> leaders,
            Map<String, List<Integer>> scratchTracks,
            String winnerName
    ) {
        if (leaders.isEmpty() && marks.isEmpty()) {
            return;
        }
        TrackMark fastest = marks.stream()
                .min(Comparator.comparingDouble(m -> m.bestLap))
                .orElse(null);

        boolean tie = leaders.size() >= 2
                && leaders.get(0).getValue().equals(leaders.get(1).getValue())
                && leaders.get(0).getValue() >= 2;

        if (tie) {
            Map.Entry<String, Integer> a = leaders.get(0);
            Map.Entry<String, Integer> b = leaders.get(1);
            String pistesA = pistesLabel(scratchTracks.getOrDefault(a.getKey(), List.of()));
            String pistesB = pistesLabel(scratchTracks.getOrDefault(b.getKey(), List.of()));
            StringBuilder line = new StringBuilder();
            line.append(RecapPhrases.pick(seed + 5,
                    "Match nul aux scratchs : " + a.getKey() + " et " + b.getKey()
                            + " à " + a.getValue() + " partout ("
                            + a.getKey() + " sur " + pistesA + ", "
                            + b.getKey() + " sur " + pistesB + ").",
                    a.getKey() + " et " + b.getKey() + " se partagent les meilleurs tours, "
                            + a.getValue() + " chacun — "
                            + a.getKey() + " sur " + pistesA + ", "
                            + b.getKey() + " sur " + pistesB + ".",
                    "Les scratchs se coupent en deux : " + a.getKey()
                            + " (" + pistesA + ") et " + b.getKey()
                            + " (" + pistesB + ")."
            ));
            if (!a.getKey().equals(winnerName) && !b.getKey().equals(winnerName)) {
                line.append(" ").append(winnerName)
                        .append(" gagne pourtant au tours.");
            } else if (!a.getKey().equals(winnerName) || !b.getKey().equals(winnerName)) {
                String other = a.getKey().equals(winnerName) ? b.getKey() : a.getKey();
                line.append(" ").append(winnerName)
                        .append(" gagne le total, ").append(other)
                        .append(" répond à la pointe.");
            }
            beats.put("scratch_king", line.toString().replace("  ", " ").trim());
            addScratchRest(beats, seed, leaders, scratchTracks, 2);
            addFastestAndPace(beats, seed, ranked, marks, fastest);
            return;
        }

        if (!leaders.isEmpty() && leaders.get(0).getValue() >= 2) {
            Map.Entry<String, Integer> top = leaders.get(0);
            String pistes = pistesLabel(scratchTracks.getOrDefault(top.getKey(), List.of()));
            if (leaders.size() >= 2 && leaders.get(1).getValue() >= 2) {
                Map.Entry<String, Integer> second = leaders.get(1);
                beats.put("scratch_king", RecapPhrases.pick(seed + 5,
                        top.getKey() + " mène aux scratchs (" + top.getValue()
                                + " sur " + pistes + "), devant " + second.getKey()
                                + " (" + second.getValue() + " sur "
                                + pistesLabel(scratchTracks.getOrDefault(second.getKey(), List.of()))
                                + ").",
                        "Meilleurs tours : " + top.getKey() + " " + top.getValue()
                                + "–" + second.getValue() + " " + second.getKey()
                                + " (" + top.getKey() + " sur " + pistes + ")."
                ));
                addScratchRest(beats, seed, leaders, scratchTracks, 2);
            } else if (top.getKey().equals(winnerName)) {
                beats.put("scratch_king", RecapPhrases.pick(seed + 5,
                        top.getKey() + " ajoute " + top.getValue()
                                + " scratchs sur " + pistes + ".",
                        "Et la pointe suit : " + top.getValue()
                                + " meilleurs tours pour " + top.getKey()
                                + " (" + pistes + ")."
                ));
                addScratchRest(beats, seed, leaders, scratchTracks, 1);
            } else {
                beats.put("scratch_king", RecapPhrases.pick(seed + 5,
                        top.getKey() + " est le plus souvent le plus rapide ("
                                + top.getValue() + " scratchs, " + pistes
                                + "), mais " + winnerName + " gagne au tours.",
                        "Les scratchs vont à " + top.getKey() + " ("
                                + pistes + "). Le classement, lui, reste à "
                                + winnerName + "."
                ));
                addScratchRest(beats, seed, leaders, scratchTracks, 1);
            }
            addFastestAndPace(beats, seed, ranked, marks, fastest);
            return;
        }

        addScratchRest(beats, seed, leaders, scratchTracks, 0);
        addFastestAndPace(beats, seed, ranked, marks, fastest);
    }

    private static void addScratchRest(
            Map<String, String> beats,
            int seed,
            List<Map.Entry<String, Integer>> leaders,
            Map<String, List<Integer>> scratchTracks,
            int skip
    ) {
        if (leaders.size() <= skip) {
            return;
        }
        List<String> bits = new ArrayList<>();
        for (int i = skip; i < leaders.size(); i++) {
            Map.Entry<String, Integer> e = leaders.get(i);
            bits.add(e.getKey() + " (" + pistesShort(scratchTracks.getOrDefault(e.getKey(), List.of())) + ")");
        }
        if (bits.isEmpty()) {
            return;
        }
        beats.put("scratch_rest", RecapPhrases.pick(seed + 21,
                "Autres scratchs : " + String.join(", ", bits) + ".",
                "Aussi un meilleur tour pour " + String.join(", ", bits) + ".",
                "Le reste des pistes : " + String.join(", ", bits) + "."
        ));
    }

    private static void addFastestAndPace(
            Map<String, String> beats,
            int seed,
            List<RaceResult> ranked,
            List<TrackMark> marks,
            TrackMark fastest
    ) {
        if (fastest == null) {
            return;
        }
        List<PacePeak> paces = findAllPaceVsPeak(ranked, marks);
        PacePeak onFastest = paces.stream()
                .filter(p -> p.track == fastest.track)
                .findFirst()
                .orElse(null);
        beats.put("fastest_lap", fastestBit(fastest, onFastest, seed));
        PacePeak other = paces.stream()
                .filter(p -> p.track != fastest.track)
                .findFirst()
                .orElse(null);
        if (other != null) {
            beats.put("pace_vs_peak", paceLine(other, seed));
        }
    }

    private static void addTrackStories(
            Map<String, String> beats,
            int seed,
            List<TrackMark> marks,
            int n
    ) {
        TrackMark tightest = null;
        TrackMark widest = null;
        double minSpread = Double.MAX_VALUE;
        double maxSpread = -1;
        for (TrackMark m : marks) {
            if (m.worstPilot == null || m.worstPilot.equals(m.bestPilot)) {
                continue;
            }
            double spread = m.worstLap - m.bestLap;
            if (spread > 0 && spread < minSpread) {
                minSpread = spread;
                tightest = m;
            }
            if (spread > maxSpread) {
                maxSpread = spread;
                widest = m;
            }
        }
        if (tightest != null && minSpread < 0.70 && n >= 4) {
            beats.put("tight_track", RecapPhrases.pick(seed + 12,
                    "Piste la plus serrée : la " + tightest.track
                            + " — scratch " + tightest.bestPilot + " en "
                            + formatTime(tightest.bestLap) + ", seulement "
                            + formatGap(minSpread) + " jusqu’au plus lent.",
                    "Sur la piste " + tightest.track + ", le plateau est dans un mouchoir ("
                            + formatGap(minSpread) + "). "
                            + tightest.bestPilot + " sort du lot en "
                            + formatTime(tightest.bestLap) + "."
            ));
        }
        if (widest != null && maxSpread >= 0.80
                && (tightest == null || widest.track != tightest.track)) {
            beats.put("wide_track", RecapPhrases.pick(seed + 13,
                    "Plus gros écart chrono : piste " + widest.track
                            + ", scratch " + widest.bestPilot + " en "
                            + formatTime(widest.bestLap) + " ("
                            + formatGap(maxSpread) + " jusqu’au fond).",
                    "La piste " + widest.track + " a le plus écarté le plateau : "
                            + widest.bestPilot + " en " + formatTime(widest.bestLap)
                            + ", " + formatGap(maxSpread) + " derrière."
            ));
        }
    }

    private static String fastestBit(TrackMark fastest, PacePeak pace, int seed) {
        if (pace != null && pace.track == fastest.track) {
            return RecapPhrases.pick(seed + 6,
                    "Meilleur tour du jour : " + fastest.bestPilot + " en "
                            + formatTime(fastest.bestLap) + " sur la piste " + fastest.track
                            + ", mais " + pace.lapsPilot + " y aligne plus de tours ("
                            + pace.laps + ").",
                    fastest.bestPilot + " plante un " + formatTime(fastest.bestLap)
                            + " sur la piste " + fastest.track + " — "
                            + pace.lapsPilot + " y tourne davantage (" + pace.laps + " tours)."
            );
        }
        return RecapPhrases.pick(seed + 6,
                "Meilleur tour du jour : " + fastest.bestPilot + " en "
                        + formatTime(fastest.bestLap) + " sur la piste " + fastest.track + ".",
                fastest.bestPilot + " signe le chrono de référence : "
                        + formatTime(fastest.bestLap) + " (piste " + fastest.track + ")."
        );
    }

    private static String paceLine(PacePeak pace, int seed) {
        return RecapPhrases.pick(seed + 18,
                "Sur la piste " + pace.track + ", " + pace.scratchPilot
                        + " a le scratch mais " + pace.lapsPilot
                        + " tourne plus (" + pace.laps + " tours).",
                pace.scratchPilot + " est le plus rapide sur la piste " + pace.track
                        + ", " + pace.lapsPilot + " y aligne le plus de tours ("
                        + pace.laps + ")."
        );
    }

    private static List<PacePeak> findAllPaceVsPeak(
            List<RaceResult> ranked,
            List<TrackMark> marks
    ) {
        List<PacePeak> out = new ArrayList<>();
        for (TrackMark mark : marks) {
            String mostLapsPilot = null;
            int mostLaps = -1;
            for (RaceResult r : ranked) {
                Integer laps = lapsOnTrack(r, mark.track);
                if (laps == null || laps <= 0) {
                    continue;
                }
                if (laps > mostLaps) {
                    mostLaps = laps;
                    mostLapsPilot = r.getNom();
                }
            }
            if (mostLapsPilot == null || mostLapsPilot.equals(mark.bestPilot)) {
                continue;
            }
            out.add(new PacePeak(mark.track, mark.bestPilot, mostLapsPilot, mostLaps));
        }
        return out;
    }

    private static void addQualiMoves(
            Map<String, String> beats,
            int seed,
            List<Qualif> quals,
            List<RaceResult> ranked,
            String winnerName
    ) {
        if (quals == null || quals.isEmpty() || ranked.size() < 3) {
            return;
        }
        Map<String, Integer> startPos = qualiPositions(quals);
        int bestGain = 0;
        String bestMover = null;
        int from = 0;
        int to = 0;
        int bestDrop = 0;
        String bestDropper = null;
        int dropFrom = 0;
        int dropTo = 0;
        for (int i = 0; i < ranked.size(); i++) {
            Integer start = startPos.get(norm(ranked.get(i).getNom()));
            if (start == null) {
                continue;
            }
            int gain = start - (i + 1);
            if (gain > bestGain) {
                bestGain = gain;
                bestMover = ranked.get(i).getNom();
                from = start;
                to = i + 1;
            }
            if (gain < bestDrop) {
                bestDrop = gain;
                bestDropper = ranked.get(i).getNom();
                dropFrom = start;
                dropTo = i + 1;
            }
        }
        if (bestMover != null && bestGain >= 2 && !bestMover.equals(winnerName)) {
            beats.put("mover", RecapPhrases.pick(seed + 14,
                    bestMover + " remonte " + bestGain + " places ("
                            + posLabel(from) + " → " + posLabel(to) + ").",
                    "Belle remontée de " + bestMover + " : "
                            + posLabel(from) + " des qualifs, " + posLabel(to) + " à l’arrivée."
            ));
        }
        if (bestDropper != null && bestDrop <= -2 && !bestDropper.equals(bestMover)) {
            beats.put("drop", RecapPhrases.pick(seed + 15,
                    bestDropper + " recule de " + Math.abs(bestDrop) + " places ("
                            + posLabel(dropFrom) + " → " + posLabel(dropTo) + ").",
                    "À l’inverse, " + bestDropper + " lâche du terrain : "
                            + posLabel(dropFrom) + " des qualifs, " + posLabel(dropTo)
                            + " au damier."
            ));
        }
    }

    private static List<String> assemble(
            String[] template,
            Map<String, String> beats,
            List<String> required,
            int cap
    ) {
        return assemble(template, beats, required, List.of(), cap);
    }

    private static List<String> assemble(
            String[] template,
            Map<String, String> beats,
            List<String> required,
            List<String> extras,
            int cap
    ) {
        List<String> headlines = new ArrayList<>();
        Set<String> used = new LinkedHashSet<>();
        for (String key : template) {
            addBeat(headlines, used, beats, key);
        }
        for (String key : required) {
            addBeat(headlines, used, beats, key);
        }
        for (String key : extras) {
            if (headlines.size() >= cap) {
                break;
            }
            addBeat(headlines, used, beats, key);
        }
        if (headlines.size() <= cap) {
            return headlines;
        }
        return new ArrayList<>(headlines.subList(0, cap));
    }

    private static void addBeat(
            List<String> headlines,
            Set<String> used,
            Map<String, String> beats,
            String key
    ) {
        String line = beats.get(key);
        if (line == null || line.isBlank() || !used.add(key)) {
            return;
        }
        headlines.add(line);
    }

    private static List<TrackMark> computeTrackMarks(List<RaceResult> ranked) {
        List<TrackMark> marks = new ArrayList<>();
        for (int track = 1; track <= 6; track++) {
            String bestPilot = null;
            double bestLap = Double.MAX_VALUE;
            String worstPilot = null;
            double worstLap = -1;
            for (RaceResult r : ranked) {
                Double lap = bestLapOnTrack(r, track);
                if (lap == null || lap <= 0) {
                    continue;
                }
                if (lap < bestLap) {
                    bestLap = lap;
                    bestPilot = r.getNom();
                }
                if (lap > worstLap) {
                    worstLap = lap;
                    worstPilot = r.getNom();
                }
            }
            if (bestPilot == null) {
                continue;
            }
            marks.add(new TrackMark(track, bestPilot, bestLap, worstPilot, worstLap));
        }
        return marks;
    }

    private static Consecutive closestFrom(List<RaceResult> ranked, int fromPos) {
        Consecutive best = null;
        for (int i = fromPos; i < ranked.size(); i++) {
            double gap = ranked.get(i - 1).getTotalTours() - ranked.get(i).getTotalTours();
            if (gap < 0) {
                continue;
            }
            if (best == null || gap < best.gapLaps) {
                best = new Consecutive(
                        ranked.get(i - 1).getNom(),
                        ranked.get(i).getNom(),
                        i,
                        i + 1,
                        gap
                );
            }
        }
        return best;
    }

    private static Map<String, Integer> qualiPositions(List<Qualif> quals) {
        Map<String, Integer> startPos = new HashMap<>();
        if (quals == null) {
            return startPos;
        }
        List<Qualif> sorted = quals.stream()
                .filter(q -> q != null && q.getPilotName() != null && !q.getPilotName().isBlank())
                .sorted(Comparator.comparingDouble(Qualif::getBestTime))
                .toList();
        for (int i = 0; i < sorted.size(); i++) {
            startPos.put(norm(sorted.get(i).getPilotName()), i + 1);
        }
        return startPos;
    }

    private static Integer qualiPosition(List<Qualif> quals, String name) {
        return qualiPositions(quals).get(norm(name));
    }

    private static String poleSitter(List<Qualif> quals) {
        if (quals == null || quals.isEmpty()) {
            return null;
        }
        return quals.stream()
                .filter(q -> q != null && q.getPilotName() != null && !q.getPilotName().isBlank())
                .min(Comparator.comparingDouble(Qualif::getBestTime))
                .map(Qualif::getPilotName)
                .orElse(null);
    }

    private static String pistesLabel(List<Integer> tracks) {
        if (tracks == null || tracks.isEmpty()) {
            return "";
        }
        if (tracks.size() == 1) {
            return "la piste " + tracks.get(0);
        }
        return "les pistes " + etJoin(tracks.stream().map(String::valueOf).toList());
    }

    private static String pistesShort(List<Integer> tracks) {
        if (tracks == null || tracks.isEmpty()) {
            return "";
        }
        return etJoin(tracks.stream().map(t -> "piste " + t).toList());
    }

    private static String etJoin(List<String> items) {
        if (items.size() == 1) {
            return items.get(0);
        }
        if (items.size() == 2) {
            return items.get(0) + " et " + items.get(1);
        }
        return String.join(", ", items.subList(0, items.size() - 1))
                + " et " + items.get(items.size() - 1);
    }

    private static String posLabel(int p) {
        if (p <= 0) {
            return "";
        }
        return p == 1 ? "1er" : p + "e";
    }

    private static String lapsPhrase(double laps) {
        String number = formatLaps(laps);
        double rounded = Math.round(laps * 10.0) / 10.0;
        return number + (rounded < 2.0 ? " tour" : " tours");
    }

    private static String norm(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static Double bestLapOnTrack(RaceResult result, int trackNumber) {
        if (result.getBestTime() == null) {
            return null;
        }
        for (BestTime bt : result.getBestTime()) {
            if (bt.getTrackNumber() == trackNumber) {
                return bt.getBestLapTime();
            }
        }
        int idx = trackNumber - 1;
        if (idx >= 0 && idx < result.getBestTime().size()) {
            return result.getBestTime().get(idx).getBestLapTime();
        }
        return null;
    }

    private static Integer lapsOnTrack(RaceResult result, int trackNumber) {
        if (result.getLapsPerTrack() == null) {
            return null;
        }
        for (LapsPerTrack l : result.getLapsPerTrack()) {
            if (l.getTrackNumber() == trackNumber) {
                return l.getLaps();
            }
        }
        int idx = trackNumber - 1;
        if (idx >= 0 && idx < result.getLapsPerTrack().size()) {
            return result.getLapsPerTrack().get(idx).getLaps();
        }
        return null;
    }

    private static String formatTime(double seconds) {
        return RallyeTimeFormat.format(seconds);
    }

    private static String formatGap(double gapSeconds) {
        return RallyeTimeFormat.formatGap(gapSeconds);
    }

    private static String formatLaps(double laps) {
        double rounded = Math.round(laps * 10.0) / 10.0;
        if (Math.abs(rounded - Math.rint(rounded)) < 0.001) {
            return String.valueOf((long) Math.rint(rounded));
        }
        return String.format(Locale.US, "%.1f", rounded);
    }

    private record TrackMark(
            int track,
            String bestPilot,
            double bestLap,
            String worstPilot,
            double worstLap
    ) {
    }

    private record Consecutive(
            String ahead,
            String behind,
            int aheadPos,
            int behindPos,
            double gapLaps
    ) {
    }

    private record PacePeak(
            int track,
            String scratchPilot,
            String lapsPilot,
            int laps
    ) {
    }
}
