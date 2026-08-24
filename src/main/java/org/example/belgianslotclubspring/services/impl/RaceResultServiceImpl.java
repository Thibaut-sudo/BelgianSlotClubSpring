package org.example.belgianslotclubspring.services.impl;

import org.example.belgianslotclubspring.entities.RaceResult;
import org.example.belgianslotclubspring.entities.Qualif;
import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.models.RaceSummary;
import org.example.belgianslotclubspring.repo.QualifRepo;
import org.example.belgianslotclubspring.repo.RaceResultRepo;
import org.example.belgianslotclubspring.services.RaceResultService;
import org.example.belgianslotclubspring.utils.CategoryNames;
import org.example.belgianslotclubspring.utils.ChampionshipPoints;
import org.example.belgianslotclubspring.utils.PilotNames;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RaceResultServiceImpl implements RaceResultService {

    private final RaceResultRepo raceResultRepo;
    private final QualifRepo qualifRepo;

    /** Une seule passe de maintenance (normalize / bis) par club et JVM. */
    private final Set<String> categoriesNormalized = ConcurrentHashMap.newKeySet();
    private final Set<String> bisSynced = ConcurrentHashMap.newKeySet();
    /** Pages championnat / listes : invalidées après import ou suppression. */
    private final ConcurrentHashMap<String, Object> pageCache = new ConcurrentHashMap<>();

    public RaceResultServiceImpl(RaceResultRepo raceResultRepo, QualifRepo qualifRepo) {
        this.raceResultRepo = raceResultRepo;
        this.qualifRepo = qualifRepo;
    }

    @Override
    public void saveRaceResult(RaceResult raceResult) {
    }

    @Override
    public void updateRaceResult(RaceResult raceResult) {
    }

    @Override
    public void deleteRaceResult(RaceResult raceResult) {
    }

    @Override
    public RaceResult findRaceResultById(int id) {
        return null;
    }

    @Override
    public Map<LocalDate, String> getRaceResultDate() {
        return Map.of();
    }

    @Override
    @Deprecated
    public List<RaceResult> getRaceResultByDate(LocalDate date) {
        throw new UnsupportedOperationException("Utiliser getRaceResultByDateAndClub(date, club)");
    }

    @Override
    public List<RaceResult> getRaceResultByDateAndClub(LocalDate date, String club) {
        String clubCode = Club.requireCode(club);
        return raceResultRepo.getRaceResultByDateAndClub(date, clubCode);
    }

    @Override
    public List<RaceResult> getRaceResultByDateClubAndCategory(LocalDate date, String club, String category) {
        String clubCode = Club.requireCode(club);
        String wantedKey = CategoryNames.key(category);
        if (wantedKey.isEmpty()) {
            return getRaceResultByDateAndClub(date, clubCode);
        }
        return raceResultRepo.findByDateAndClub(date, clubCode).stream()
                .filter(r -> wantedKey.equals(CategoryNames.key(r.getCategoryName())))
                .sorted((a, b) -> Double.compare(b.getTotalTours(), a.getTotalTours()))
                .toList();
    }

    @Override
    @Transactional
    public List<RaceSummary> getRaceSummariesByClub(String club) {
        String clubCode = Club.requireCode(club);
        String cacheKey = "summaries|" + clubCode;
        List<RaceSummary> cached = cachedPage(cacheKey);
        if (cached != null) {
            return cached;
        }
        normalizeStoredCategories(clubCode);

        List<Object[]> results = raceResultRepo.getRaceResultDateByClubName(clubCode);
        Map<String, RaceSummary> byDateAndKey = new LinkedHashMap<>();

        for (Object[] row : results) {
            LocalDate date = (LocalDate) row[0];
            String category = CategoryNames.canonical((String) row[1]);
            String mapKey = date + "|" + CategoryNames.key(category);
            byDateAndKey.putIfAbsent(mapKey, new RaceSummary(date, category, clubCode));
        }

        List<RaceSummary> copy = List.copyOf(byDateAndKey.values());
        pageCache.put(cacheKey, copy);
        return copy;
    }

    @Override
    @Deprecated
    public Map<LocalDate, String> getRaceResultDateByClub(String club) {
        return getRaceSummariesByClub(club).stream()
                .collect(Collectors.toMap(
                        RaceSummary::date,
                        RaceSummary::category,
                        (existing, replacement) -> replacement,
                        LinkedHashMap::new
                ));
    }

    @Override
    public List<String> getAllCategories() {
        return List.of();
    }

    @Override
    public List<String> getRaceDates(String cathegorie, Integer year) {
        return List.of();
    }

    @Override
    @Transactional
    public List<String> getAllCategoriesClub(String club) {
        String clubCode = Club.requireCode(club);
        String cacheKey = "cats|" + clubCode;
        List<String> cached = cachedPage(cacheKey);
        if (cached != null) {
            return cached;
        }
        normalizeStoredCategories(clubCode);

        List<String> categories = raceResultRepo.getAllCategoriesClub(clubCode).stream()
                .map(CategoryNames::canonical)
                .filter(c -> !c.isBlank())
                .distinct()
                .sorted(Comparator.comparing(c -> c.toLowerCase(Locale.ROOT)))
                .toList();
        pageCache.put(cacheKey, categories);
        return categories;
    }

    @Override
    public List<String> getAllCategoriesClub(String club, Integer year) {
        if (year == null) {
            return getAllCategoriesClub(club);
        }
        String clubCode = Club.requireCode(club);
        String cacheKey = "cats|" + clubCode + "|" + year;
        List<String> cached = cachedPage(cacheKey);
        if (cached != null) {
            return cached;
        }
        normalizeStoredCategories(clubCode);

        List<String> categories = raceResultRepo.getAllCategoriesClubForYear(clubCode, year).stream()
                .map(CategoryNames::canonical)
                .filter(c -> !c.isBlank())
                .distinct()
                .sorted(Comparator.comparing(c -> c.toLowerCase(Locale.ROOT)))
                .toList();
        pageCache.put(cacheKey, categories);
        return categories;
    }

    @Override
    @Transactional
    public Map<LocalDate, Map<String, Double>> getChampionshipResults(String category, String club, Integer year) {
        String clubCode = Club.requireCode(club);
        String wantedKey = CategoryNames.key(category);
        String cacheKey = "champ|" + clubCode + "|" + year + "|" + wantedKey;
        Map<LocalDate, Map<String, Double>> cached = cachedPage(cacheKey);
        if (cached != null) {
            return cached;
        }
        normalizeStoredCategories(clubCode);
        syncBisMarkersFromQualifs(clubCode);

        List<Object[]> results = raceResultRepo.getChampionshipResultsRaw(clubCode, year);

        Map<LocalDate, Map<String, Double>> championshipResults = new LinkedHashMap<>();

        for (Object[] row : results) {
            String rowCategory = (String) row[3];
            if (!wantedKey.equals(CategoryNames.key(rowCategory))) {
                continue;
            }
            LocalDate raceDate = (LocalDate) row[0];
            String pilot = (String) row[1];
            Double totalTours = (Double) row[2];

            Map<String, Double> day = championshipResults.computeIfAbsent(raceDate, k -> new LinkedHashMap<>());
            day.merge(pilot, totalTours, Math::max);
        }

        for (Map<String, Double> raceResults : championshipResults.values()) {
            updateRaceResultsWithStar(raceResults);
        }

        Map<LocalDate, Map<String, Double>> frozen = freezeChampionship(championshipResults);
        pageCache.put(cacheKey, frozen);
        return frozen;
    }

    @Override
    public List<String> getAllYearsClub(String club) {
        return getAvailableYears(club).stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    @Override
    public List<Integer> getAvailableYears(String club) {
        String clubCode = Club.requireCode(club);
        String cacheKey = "years|" + clubCode;
        List<Integer> cached = cachedPage(cacheKey);
        if (cached != null) {
            return cached;
        }
        List<Integer> years = List.copyOf(raceResultRepo.getAvailableYears(clubCode));
        pageCache.put(cacheKey, years);
        return years;
    }

    @Override
    @Transactional
    public void deleteRace(LocalDate date, String club, String category) {
        if (date == null) {
            throw new IllegalArgumentException("Date de course obligatoire");
        }
        String clubCode = Club.requireCode(club);
        String cat = category == null ? "" : category.trim();
        if (cat.isEmpty()) {
            throw new IllegalArgumentException("Catégorie obligatoire");
        }

        String wantedKey = CategoryNames.key(cat);
        List<RaceResult> results = raceResultRepo.findByDateAndClub(date, clubCode).stream()
                .filter(r -> wantedKey.equals(CategoryNames.key(r.getCategoryName())))
                .toList();

        if (results.isEmpty()) {
            throw new IllegalArgumentException("Course introuvable pour " + CategoryNames.canonical(cat) + " le " + date);
        }

        raceResultRepo.deleteAll(results);
        invalidateClubMaintenanceCaches(clubCode);

        if (raceResultRepo.countByDateAndClub(date, clubCode) == 0) {
            qualifRepo.deleteAll(qualifRepo.findAllByDateAndClub(date, clubCode));
        }
    }

    @Override
    public void invalidateClubMaintenanceCaches(String club) {
        String clubCode = Club.requireCode(club);
        categoriesNormalized.remove(clubCode);
        bisSynced.remove(clubCode);
        pageCache.keySet().removeIf(key -> key.contains("|" + clubCode + "|") || key.endsWith("|" + clubCode));
    }

    @SuppressWarnings("unchecked")
    private <T> T cachedPage(String key) {
        return (T) pageCache.get(key);
    }

    private static Map<LocalDate, Map<String, Double>> freezeChampionship(
            Map<LocalDate, Map<String, Double>> source) {
        Map<LocalDate, Map<String, Double>> frozen = new LinkedHashMap<>();
        for (Map.Entry<LocalDate, Map<String, Double>> entry : source.entrySet()) {
            frozen.put(entry.getKey(), Collections.unmodifiableMap(new LinkedHashMap<>(entry.getValue())));
        }
        return Collections.unmodifiableMap(frozen);
    }

    /** Réécrit en base les variantes (« Slot it », « Slot-it ») vers le libellé canonique. */
    private void normalizeStoredCategories(String clubCode) {
        if (!categoriesNormalized.add(clubCode)) {
            return;
        }
        List<RaceResult> all = raceResultRepo.findAllByClub(clubCode);
        boolean dirty = false;
        for (RaceResult r : all) {
            String current = r.getCategoryName();
            String canon = CategoryNames.canonical(current);
            if (canon != null && !canon.equals(current)) {
                r.setCategoryName(canon);
                dirty = true;
            }
        }
        if (dirty) {
            raceResultRepo.flush();
        }
    }

    /**
     * Les Excel Slot 4000 marquent souvent le bis sur la colonne quali (*),
     * pas sur le nom de course. On reporte la marque sur le bon RaceResult
     * (rapprochement par date + nom de base + id le plus proche).
     */
    private void syncBisMarkersFromQualifs(String clubCode) {
        if (!bisSynced.add(clubCode)) {
            return;
        }
        List<RaceResult> races = raceResultRepo.findAllByClub(clubCode);
        Map<LocalDate, List<RaceResult>> racesByDate = races.stream()
                .filter(r -> r.getDate() != null)
                .collect(Collectors.groupingBy(RaceResult::getDate, LinkedHashMap::new, Collectors.toList()));

        boolean dirty = false;
        for (Map.Entry<LocalDate, List<RaceResult>> entry : racesByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<Qualif> bisQualifs = qualifRepo.findAllByDateAndClub(date, clubCode).stream()
                    .filter(q -> PilotNames.isBis(q.getPilotName()))
                    .sorted(Comparator.comparing(Qualif::getId, Comparator.nullsLast(Long::compareTo)))
                    .toList();
            if (bisQualifs.isEmpty()) {
                continue;
            }

            List<RaceResult> available = entry.getValue().stream()
                    .filter(r -> !PilotNames.isBis(r.getNom()))
                    .collect(Collectors.toCollection(ArrayList::new));

            for (Qualif bisQuali : bisQualifs) {
                String base = PilotNames.baseName(bisQuali.getPilotName()).toLowerCase(Locale.ROOT);
                if (base.isEmpty()) {
                    continue;
                }
                RaceResult best = null;
                long bestDist = Long.MAX_VALUE;
                for (RaceResult race : available) {
                    if (!base.equals(PilotNames.baseName(race.getNom()).toLowerCase(Locale.ROOT))) {
                        continue;
                    }
                    long dist = Math.abs(
                            (race.getId() == null ? 0L : race.getId())
                                    - (bisQuali.getId() == null ? 0L : bisQuali.getId())
                    );
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = race;
                    }
                }
                if (best != null) {
                    best.setNom(PilotNames.withBisMarker(best.getNom(), true));
                    available.remove(best);
                    dirty = true;
                }
            }
        }
        if (dirty) {
            raceResultRepo.flush();
        }
    }

    private static void updateRaceResultsWithStar(Map<String, Double> raceResults) {
        List<Map.Entry<String, Double>> normalList = new ArrayList<>();
        List<Map.Entry<String, Double>> starList = new ArrayList<>();

        for (Map.Entry<String, Double> entry : raceResults.entrySet()) {
            if (PilotNames.isBis(entry.getKey())) {
                starList.add(entry);
            } else {
                normalList.add(entry);
            }
        }

        normalList.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        starList.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        Map<String, Double> updatedResults = new LinkedHashMap<>();
        assignPoints(normalList, updatedResults);
        assignPoints(starList, updatedResults);

        raceResults.clear();
        raceResults.putAll(updatedResults);
    }

    private static void assignPoints(
            List<Map.Entry<String, Double>> rankedByLaps,
            Map<String, Double> out
    ) {
        int i = 0;
        while (i < rankedByLaps.size()) {
            double laps = rankedByLaps.get(i).getValue() == null ? 0 : rankedByLaps.get(i).getValue();
            int j = i + 1;
            while (j < rankedByLaps.size()) {
                double other = rankedByLaps.get(j).getValue() == null ? 0 : rankedByLaps.get(j).getValue();
                if (Double.compare(other, laps) != 0) {
                    break;
                }
                j++;
            }
            int points = ChampionshipPoints.forRankIndex(i);
            for (int k = i; k < j; k++) {
                out.put(rankedByLaps.get(k).getKey(), (double) points);
            }
            i = j;
        }
    }
}
