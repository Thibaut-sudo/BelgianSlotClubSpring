package org.example.belgianslotclubspring.services;

import org.example.belgianslotclubspring.entities.BestTime;
import org.example.belgianslotclubspring.entities.Qualif;
import org.example.belgianslotclubspring.entities.RaceResult;
import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.models.ClubRaceStats;
import org.example.belgianslotclubspring.models.ClubRaceStats.NamedCount;
import org.example.belgianslotclubspring.models.ClubRaceStats.PilotCount;
import org.example.belgianslotclubspring.models.ClubRaceStats.RaceCrowd;
import org.example.belgianslotclubspring.models.ClubRaceStats.RecordHolder;
import org.example.belgianslotclubspring.repo.QualifRepo;
import org.example.belgianslotclubspring.repo.RaceResultRepo;
import org.example.belgianslotclubspring.utils.CategoryNames;
import org.example.belgianslotclubspring.utils.PilotNames;
import org.example.belgianslotclubspring.utils.RallyeTimeFormat;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ClubRaceStatsService {

    private static final int TOP_PILOTS = 15;
    private static final int TOP_FIELDS = 10;
    /** Temps de tour slot plausibles (secondes) — ignore 0 / valeurs aberrantes. */
    private static final double MIN_LAP = 0.8;
    private static final double MAX_LAP = 40.0;

    private final RaceResultRepo raceResultRepo;
    private final QualifRepo qualifRepo;

    public ClubRaceStatsService(RaceResultRepo raceResultRepo, QualifRepo qualifRepo) {
        this.raceResultRepo = raceResultRepo;
        this.qualifRepo = qualifRepo;
    }

    public ClubRaceStats build(String club, Integer year) {
        String clubCode = Club.requireCode(club);
        List<RaceResult> results = raceResultRepo.findAllByClub(clubCode).stream()
                .filter(r -> r.getDate() != null)
                .filter(r -> year == null || r.getDate().getYear() == year)
                .toList();

        if (results.isEmpty()) {
            return ClubRaceStats.empty();
        }

        record RaceKey(LocalDate date, String catKey) {
        }

        Map<RaceKey, List<RaceResult>> byRace = new LinkedHashMap<>();
        for (RaceResult r : results) {
            RaceKey key = new RaceKey(r.getDate(), CategoryNames.key(r.getCategoryName()));
            byRace.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        int raceCount = byRace.size();
        int starterCount = results.size();
        Map<String, Integer> present = new HashMap<>();
        Map<String, Integer> wins = new HashMap<>();
        Map<String, Integer> byCategory = new HashMap<>();
        Map<Integer, Integer> byYear = new TreeMap<>();
        List<RaceCrowd> fields = new ArrayList<>();
        RecordHolder fastestLap = null;
        double fastestLapValue = Double.MAX_VALUE;

        for (Map.Entry<RaceKey, List<RaceResult>> entry : byRace.entrySet()) {
            List<RaceResult> field = entry.getValue();
            LocalDate date = entry.getKey().date();
            String category = CategoryNames.canonical(field.getFirst().getCategoryName());
            if (category.isBlank()) {
                category = "Sans catégorie";
            }

            byCategory.merge(category, 1, Integer::sum);
            byYear.merge(date.getYear(), 1, Integer::sum);
            fields.add(new RaceCrowd(date, category, field.size()));

            double bestLaps = -1;
            List<RaceResult> winners = new ArrayList<>();
            for (RaceResult r : field) {
                String name = PilotNames.baseName(r.getNom());
                if (!name.isBlank()) {
                    present.merge(name, 1, Integer::sum);
                }
                if (r.getTotalTours() > bestLaps) {
                    bestLaps = r.getTotalTours();
                    winners.clear();
                    winners.add(r);
                } else if (r.getTotalTours() == bestLaps) {
                    winners.add(r);
                }

                if (r.getBestTime() == null) {
                    continue;
                }
                for (BestTime t : r.getBestTime()) {
                    if (t == null) {
                        continue;
                    }
                    double v = t.getBestLapTime();
                    if (v < MIN_LAP || v > MAX_LAP) {
                        continue;
                    }
                    if (v < fastestLapValue) {
                        fastestLapValue = v;
                        fastestLap = new RecordHolder(
                                name.isBlank() ? r.getNom() : name,
                                category,
                                date,
                                RallyeTimeFormat.format(v)
                        );
                    }
                }
            }
            for (RaceResult winner : winners) {
                String name = PilotNames.baseName(winner.getNom());
                if (!name.isBlank()) {
                    wins.merge(name, 1, Integer::sum);
                }
            }
        }

        RecordHolder fastestQuali = findFastestQuali(clubCode, year);

        String avgField = String.format(Locale.FRANCE, "%.1f", (double) starterCount / raceCount);

        return new ClubRaceStats(
                raceCount,
                present.size(),
                byCategory.size(),
                starterCount,
                avgField,
                fastestLap,
                fastestQuali,
                toNamedCounts(byCategory, Comparator.comparingInt(Map.Entry<String, Integer>::getValue).reversed()
                        .thenComparing(Map.Entry::getKey)),
                toYearCounts(byYear),
                topPilots(present),
                topPilots(wins),
                fields.stream()
                        .sorted(Comparator.comparingInt(RaceCrowd::pilots).reversed()
                                .thenComparing(RaceCrowd::date, Comparator.reverseOrder()))
                        .limit(TOP_FIELDS)
                        .toList()
        );
    }

    private RecordHolder findFastestQuali(String clubCode, Integer year) {
        RecordHolder best = null;
        double bestValue = Double.MAX_VALUE;
        for (Qualif q : qualifRepo.findAllByClub(clubCode)) {
            if (q.getDate() == null) {
                continue;
            }
            if (year != null && q.getDate().getYear() != year) {
                continue;
            }
            double v = q.getBestTime();
            if (v < MIN_LAP || v > MAX_LAP) {
                continue;
            }
            if (v < bestValue) {
                bestValue = v;
                String name = PilotNames.baseName(q.getPilotName());
                best = new RecordHolder(
                        name.isBlank() ? q.getPilotName() : name,
                        "Qualifications",
                        q.getDate(),
                        RallyeTimeFormat.format(v)
                );
            }
        }
        return best;
    }

    private static List<NamedCount> toNamedCounts(
            Map<String, Integer> counts,
            Comparator<Map.Entry<String, Integer>> order
    ) {
        int max = counts.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        return counts.entrySet().stream()
                .sorted(order)
                .map(e -> new NamedCount(e.getKey(), e.getValue(), pct(e.getValue(), max)))
                .toList();
    }

    private static List<NamedCount> toYearCounts(Map<Integer, Integer> byYear) {
        int max = byYear.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        return byYear.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByKey().reversed())
                .map(e -> new NamedCount(String.valueOf(e.getKey()), e.getValue(), pct(e.getValue(), max)))
                .toList();
    }

    private static List<PilotCount> topPilots(Map<String, Integer> counts) {
        return counts.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry<String, Integer>::getValue).reversed()
                        .thenComparing(e -> e.getKey().toLowerCase(Locale.ROOT)))
                .limit(TOP_PILOTS)
                .map(e -> new PilotCount(e.getKey(), e.getValue()))
                .toList();
    }

    private static int pct(int value, int max) {
        if (max <= 0) {
            return 0;
        }
        return (int) Math.round(100.0 * value / max);
    }
}
