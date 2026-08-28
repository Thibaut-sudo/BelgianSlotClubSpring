package org.example.belgianslotclubspring.utils;

import org.example.belgianslotclubspring.models.RallyeChampionshipEvent;
import org.example.belgianslotclubspring.models.RallyeChampionshipRow;
import org.example.belgianslotclubspring.models.RallyeChampionshipTable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * Agrège les classements d’épreuves en tableau de championnat.
 */
public final class RallyeChampionshipCalculator {

    private RallyeChampionshipCalculator() {
    }

    public record EventInput(
            long id,
            String name,
            LocalDate date,
            List<PilotInput> pilots
    ) {
    }

    public record PilotInput(String name, String category, int overallPlace, boolean classified) {
    }

    public static RallyeChampionshipTable compile(int year, List<EventInput> events, String categoryFilter) {
        List<EventInput> ordered = new ArrayList<>(events == null ? List.of() : events);
        ordered.sort(Comparator.comparing(EventInput::date, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(EventInput::id));

        List<RallyeChampionshipEvent> eventModels = new ArrayList<>();
        for (EventInput event : ordered) {
            eventModels.add(new RallyeChampionshipEvent(
                    event.id(),
                    event.name(),
                    shortLabel(event.name()),
                    event.date()
            ));
        }

        Map<String, Acc> byKey = new LinkedHashMap<>();
        TreeSet<String> categories = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (int eventIndex = 0; eventIndex < ordered.size(); eventIndex++) {
            EventInput event = ordered.get(eventIndex);
            Map<String, Integer> categoryPlace = categoryPlaces(event.pilots());
            for (PilotInput pilot : event.pilots()) {
                String key = RallyePilotNames.normalize(pilot.name());
                if (key.isEmpty()) {
                    continue;
                }
                Acc acc = byKey.computeIfAbsent(key, k -> new Acc(ordered.size()));
                acc.addName(pilot.name());
                String category = RallyeCategory.canonical(pilot.category());
                if (!category.isEmpty()) {
                    acc.category = category;
                    categories.add(category);
                }
                acc.starts++;
                int place = categoryPlace.getOrDefault(identity(pilot), 0);
                int pts = 0;
                if (pilot.classified() && place > 0) {
                    pts = RallyeChampionshipPoints.withScratchBonus(
                            RallyeChampionshipPoints.forCategoryPlace(place),
                            pilot.overallPlace() == 1
                    );
                }
                acc.racePoints[eventIndex] = pts;
            }
        }

        mergeUniqueFirstNames(byKey);

        String filter = categoryFilter == null || categoryFilter.isBlank() ? null : RallyeCategory.canonical(categoryFilter);
        boolean drop = RallyeChampionshipPoints.dropsWorst(ordered.size());
        List<RallyeChampionshipRow> rows = new ArrayList<>();
        for (Acc acc : byKey.values()) {
            if (acc.merged) {
                continue;
            }
            if (filter != null && !RallyeCategory.same(acc.category, filter)) {
                continue;
            }
            int[] counted = new int[acc.starts];
            int ci = 0;
            Integer dropped = null;
            int worst = Integer.MAX_VALUE;
            List<Integer> display = new ArrayList<>(ordered.size());
            for (int i = 0; i < ordered.size(); i++) {
                Integer pts = acc.racePoints[i];
                display.add(pts);
                if (pts != null) {
                    if (ci < counted.length) {
                        counted[ci++] = pts;
                    }
                    if (pts < worst) {
                        worst = pts;
                    }
                }
            }
            int raceTotal = RallyeChampionshipPoints.countedRaceTotal(
                    java.util.Arrays.copyOf(counted, ci),
                    ordered.size()
            );
            if (drop && ci > RallyeChampionshipPoints.COUNTED_RESULTS && worst != Integer.MAX_VALUE) {
                dropped = worst;
            }
            int bonus = RallyeChampionshipPoints.presenceBonus(acc.starts);
            rows.add(new RallyeChampionshipRow(
                    0,
                    acc.displayName,
                    acc.category,
                    !RallyeCategory.isWrc3(acc.category),
                    display,
                    dropped,
                    raceTotal,
                    bonus,
                    acc.starts,
                    raceTotal + bonus
            ));
        }

        rows.sort(Comparator
                .comparingInt(RallyeChampionshipRow::total).reversed()
                .thenComparing(Comparator.comparingInt(RallyeChampionshipRow::countedRaceTotal).reversed())
                .thenComparing(RallyeChampionshipRow::name, String.CASE_INSENSITIVE_ORDER));

        List<RallyeChampionshipRow> ranked = new ArrayList<>(rows.size());
        int pos = 1;
        for (RallyeChampionshipRow row : rows) {
            ranked.add(new RallyeChampionshipRow(
                    pos++,
                    row.name(),
                    row.category(),
                    row.titleEligible(),
                    row.racePoints(),
                    row.droppedPoints(),
                    row.countedRaceTotal(),
                    row.presenceBonus(),
                    row.starts(),
                    row.total()
            ));
        }

        return new RallyeChampionshipTable(
                year,
                List.copyOf(eventModels),
                List.copyOf(categories),
                filter,
                drop,
                List.copyOf(ranked)
        );
    }

    private static Map<String, Integer> categoryPlaces(List<PilotInput> pilots) {
        Map<String, List<PilotInput>> byCat = new HashMap<>();
        for (PilotInput pilot : pilots) {
            if (!pilot.classified() || pilot.overallPlace() <= 0) {
                continue;
            }
            String cat = RallyeCategory.canonical(pilot.category());
            if (cat.isEmpty()) {
                cat = "?";
            }
            byCat.computeIfAbsent(cat, k -> new ArrayList<>()).add(pilot);
        }
        Map<String, Integer> places = new HashMap<>();
        for (List<PilotInput> group : byCat.values()) {
            group.sort(Comparator.comparingInt(PilotInput::overallPlace));
            int place = 1;
            for (PilotInput pilot : group) {
                places.put(identity(pilot), place++);
            }
        }
        return places;
    }

    private static String identity(PilotInput pilot) {
        return RallyePilotNames.normalize(pilot.name()) + "|" + RallyeCategory.canonical(pilot.category());
    }

    /**
     * Relie « Jona » à « Jona X » si le prénom n’apparaît qu’une fois parmi les clés multi-mots.
     */
    private static void mergeUniqueFirstNames(Map<String, Acc> byKey) {
        Map<String, List<String>> firstToKeys = new HashMap<>();
        for (String key : byKey.keySet()) {
            String[] tokens = RallyePilotNames.tokens(key);
            if (tokens.length == 0) {
                continue;
            }
            firstToKeys.computeIfAbsent(tokens[0], k -> new ArrayList<>()).add(key);
            if (tokens.length > 1) {
                firstToKeys.computeIfAbsent(tokens[tokens.length - 1], k -> new ArrayList<>()).add(key);
            }
        }
        for (Acc acc : byKey.values()) {
            if (acc.merged) {
                continue;
            }
            String[] tokens = RallyePilotNames.tokens(RallyePilotNames.normalize(acc.displayName));
            if (tokens.length != 1) {
                continue;
            }
            List<String> candidates = firstToKeys.getOrDefault(tokens[0], List.of());
            List<String> others = candidates.stream()
                    .filter(k -> !k.equals(tokens[0]) && RallyePilotNames.tokens(k).length > 1)
                    .distinct()
                    .toList();
            if (others.size() != 1) {
                continue;
            }
            Acc target = byKey.get(others.getFirst());
            if (target == null || target == acc || target.merged) {
                continue;
            }
            target.merge(acc);
            acc.merged = true;
        }
    }

    static String shortLabel(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("geer")) {
            return "Vallée du Geer";
        }
        if (lower.contains("diepen") || lower.contains("bijenberg")) {
            return "Diepenbeek";
        }
        if (lower.contains("slins") || lower.contains("rallycross") || lower.contains("rallye-cross")) {
            return "Slins";
        }
        if (lower.contains("basse meuse")) {
            return "Basse Meuse";
        }
        if (lower.contains("cougn")) {
            return "Cougnous";
        }
        return name;
    }

    private static final class Acc {
        final Integer[] racePoints;
        String displayName = "";
        String category = "";
        int starts;
        boolean merged;

        Acc(int eventCount) {
            this.racePoints = new Integer[eventCount];
        }

        void addName(String name) {
            displayName = RallyePilotNames.displayName(displayName, name);
        }

        void merge(Acc other) {
            addName(other.displayName);
            if (category.isEmpty()) {
                category = other.category;
            }
            starts += other.starts;
            for (int i = 0; i < racePoints.length; i++) {
                if (racePoints[i] == null) {
                    racePoints[i] = other.racePoints[i];
                } else if (other.racePoints[i] != null) {
                    racePoints[i] = racePoints[i] + other.racePoints[i];
                }
            }
        }
    }
}
