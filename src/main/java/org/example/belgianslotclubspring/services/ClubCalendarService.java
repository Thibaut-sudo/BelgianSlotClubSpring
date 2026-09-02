package org.example.belgianslotclubspring.services;

import org.example.belgianslotclubspring.entities.ClubCalendarCategory;
import org.example.belgianslotclubspring.entities.ClubCalendarEvent;
import org.example.belgianslotclubspring.entities.Rallye;
import org.example.belgianslotclubspring.models.CalendarCategory;
import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.models.ClubCalendar;
import org.example.belgianslotclubspring.models.GlobalCalendarEvent;
import org.example.belgianslotclubspring.repo.ClubCalendarCategoryRepo;
import org.example.belgianslotclubspring.repo.ClubCalendarEventRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ClubCalendarService {

    static final int NAME_MAX = 80;
    static final int MIN_YEAR = 2024;
    static final int MAX_YEAR = 2032;

    private static final Set<String> SRCS_OFFICIAL = Set.of(
            "scaleauto", "gt24 / proto 24", "bpc", "revoslot", "brm",
            "bel-lms", "revo (interclubs)", "deph'one f1", "chall. e.pirotte"
    );
    private static final Set<String> SLOT4000_OFFICIAL = Set.of(
            "proto32", "gt32", "tcr-scale", "slot.it", "gr5", "proto24", "gt24",
            "1000kms", "tcr all", "soirée fun", "soirée vab"
    );
    private static final Set<String> SCO_OFFICIAL = Set.of(
            "rallye de la basse meuse"
    );
    private static final String[] DEFAULT_COLORS = {
            "#0f766e", "#be185d", "#7c3aed", "#c2410c", "#1d4ed8",
            "#365314", "#9a3412", "#0e7490", "#6d28d9", "#b45309"
    };

    private final ClubCalendarEventRepo eventRepo;
    private final ClubCalendarCategoryRepo categoryRepo;

    public ClubCalendarService(ClubCalendarEventRepo eventRepo, ClubCalendarCategoryRepo categoryRepo) {
        this.eventRepo = eventRepo;
        this.categoryRepo = categoryRepo;
    }

    public Map<String, String> eventsFor(Club club) {
        return merge(ClubCalendar.eventsFor(club), eventRepo.findByClubName(club.getCode()));
    }

    /**
     * Tous les événements de tous les clubs, groupés par date ISO.
     * Un même jour peut contenir plusieurs clubs.
     */
    public Map<String, List<GlobalCalendarEvent>> allEventsByDate() {
        Map<Club, Map<String, String>> perClub = new EnumMap<>(Club.class);
        for (Club club : Club.values()) {
            perClub.put(club, eventsFor(club));
        }
        return mergeAllClubs(perClub);
    }

    static Map<String, List<GlobalCalendarEvent>> mergeAllClubs(Map<Club, Map<String, String>> perClub) {
        Map<String, List<GlobalCalendarEvent>> byDate = new TreeMap<>();
        if (perClub == null) {
            return byDate;
        }
        for (Club club : Club.values()) {
            Map<String, String> events = perClub.get(club);
            if (events == null || events.isEmpty()) {
                continue;
            }
            for (Map.Entry<String, String> entry : events.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                byDate.computeIfAbsent(entry.getKey(), key -> new ArrayList<>())
                        .add(new GlobalCalendarEvent(club.getCode(), club.getCalendarLabel(), entry.getValue()));
            }
        }
        return byDate;
    }

    public Set<String> customDates(Club club) {
        return eventRepo.findByClubName(club.getCode()).stream()
                .map(event -> event.getEventDate().toString())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Dates ajoutées par un organisateur, par code club — pour le calendrier commun. */
    public Map<String, List<String>> customDatesByClub() {
        Map<String, List<String>> byClub = new LinkedHashMap<>();
        for (Club club : Club.values()) {
            byClub.put(club.getCode(), new ArrayList<>(customDates(club)));
        }
        return byClub;
    }

    @Transactional
    public ClubCalendarEvent upsert(Club club, LocalDate date, String name, String color) {
        requireDate(date);
        String cleanName = cleanName(name);
        ClubCalendarEvent event = eventRepo.findByClubNameAndEventDate(club.getCode(), date)
                .orElseGet(() -> new ClubCalendarEvent(club, date, cleanName));
        event.setName(cleanName);
        ClubCalendarEvent saved = eventRepo.save(event);
        String categoryName = firstCategoryName(cleanName);
        if (!isOfficialName(club, categoryName)) {
            upsertCategory(club, categoryName, color == null || color.isBlank()
                    ? defaultColorFor(categoryName)
                    : color);
        }
        return saved;
    }

    @Transactional
    public ClubCalendarEvent upsert(Club club, LocalDate date, String name) {
        return upsert(club, date, name, null);
    }

    /** Enregistre le même événement sur tous les clubs (calendrier général). */
    @Transactional
    public void upsertAllClubs(LocalDate date, String name, String color) {
        for (Club club : Club.values()) {
            upsert(club, date, name, color);
        }
    }

    /** Retire l’événement ajouté de tous les clubs pour cette date. */
    @Transactional
    public boolean deleteCustomAllClubs(LocalDate date) {
        boolean removed = false;
        for (Club club : Club.values()) {
            if (deleteCustom(club, date)) {
                removed = true;
            }
        }
        return removed;
    }

    /**
     * Place un rallye sur le calendrier du club (même date = mise à jour du nom).
     * Ignore les rallyes « test » et les dates hors plage.
     */
    @Transactional
    public boolean upsertFromRallye(Rallye rallye) {
        if (rallye == null || rallye.getDate() == null) {
            return false;
        }
        if (isTestRallyName(rallye.getName())) {
            return false;
        }
        Club club = Club.fromCode(rallye.getClubName()).orElse(null);
        if (club == null) {
            return false;
        }
        try {
            upsert(club, rallye.getDate(), calendarTitle(rallye.getName()));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Retire l’entrée calendrier si elle correspond encore à ce rallye. */
    @Transactional
    public boolean deleteIfMatchesRallye(Rallye rallye) {
        if (rallye == null || rallye.getDate() == null || rallye.getClubName() == null) {
            return false;
        }
        String expected = calendarTitle(rallye.getName());
        return eventRepo.findByClubNameAndEventDate(rallye.getClubName(), rallye.getDate())
                .filter(event -> expected.equalsIgnoreCase(event.getName()))
                .map(event -> {
                    eventRepo.deleteByClubNameAndEventDate(rallye.getClubName(), rallye.getDate());
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public boolean deleteCustom(Club club, LocalDate date) {
        requireDate(date);
        if (eventRepo.findByClubNameAndEventDate(club.getCode(), date).isEmpty()) {
            return false;
        }
        eventRepo.deleteByClubNameAndEventDate(club.getCode(), date);
        return true;
    }

    public List<CalendarCategory> customCategories(Club club) {
        List<CalendarCategory> categories = new ArrayList<>();
        for (ClubCalendarCategory row : categoryRepo.findByClubNameOrderByNameAsc(club.getCode())) {
            if (row.getName() == null || isOfficialName(club, row.getName())) {
                continue;
            }
            categories.add(new CalendarCategory(row.getName(), row.getColor(), true));
        }
        return categories;
    }

    @Transactional
    public CalendarCategory upsertCategory(Club club, String name, String color) {
        String cleanName = cleanName(name);
        if (isOfficialName(club, cleanName)) {
            throw new IllegalArgumentException("Cette catégorie existe déjà dans la légende.");
        }
        String cleanColor = cleanColor(color, cleanName);
        ClubCalendarCategory row = categoryRepo.findByClubNameAndNameIgnoreCase(club.getCode(), cleanName)
                .orElseGet(() -> new ClubCalendarCategory(club, cleanName, cleanColor));
        row.setName(cleanName);
        row.setColor(cleanColor);
        ClubCalendarCategory saved = categoryRepo.save(row);
        return new CalendarCategory(saved.getName(), saved.getColor(), true);
    }

    @Transactional
    public boolean deleteCustomCategory(Club club, String name) {
        String cleanName = cleanName(name);
        if (categoryRepo.findByClubNameAndNameIgnoreCase(club.getCode(), cleanName).isEmpty()) {
            return false;
        }
        categoryRepo.deleteByClubNameAndNameIgnoreCase(club.getCode(), cleanName);
        return true;
    }

    static String firstCategoryName(String eventName) {
        if (eventName == null || eventName.isBlank()) {
            return "";
        }
        int comma = eventName.indexOf(',');
        return comma < 0 ? eventName.trim() : eventName.substring(0, comma).trim();
    }

    static boolean isOfficialName(Club club, String name) {
        if (name == null || name.isBlank() || club == null) {
            return false;
        }
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (club.isSrcs()) {
            return SRCS_OFFICIAL.contains(key) || isRallyEventName(key);
        }
        if (club.isSlot4000()) {
            return SLOT4000_OFFICIAL.contains(key) || isRallyEventName(key);
        }
        if (club.isRallyOnly()) {
            return SCO_OFFICIAL.contains(key) || isRallyEventName(key);
        }
        return isRallyEventName(key);
    }

    public static boolean isRallyEventName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String key = name.trim().toLowerCase(Locale.ROOT);
        return key.contains("rallye") || key.contains("rallycross") || key.startsWith("rally");
    }

    static boolean isTestRallyName(String name) {
        if (name == null) {
            return false;
        }
        String key = name.trim().toLowerCase(Locale.ROOT);
        return key.equals("test") || key.startsWith("test ");
    }

    static String calendarTitle(String name) {
        String value = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        if (value.isEmpty()) {
            return "Rallye";
        }
        if (value.length() <= NAME_MAX) {
            return value;
        }
        return value.substring(0, NAME_MAX - 1).trim() + "…";
    }

    static String cleanColor(String color, String name) {
        String value = color == null ? "" : color.trim();
        if (value.isEmpty()) {
            return defaultColorFor(name);
        }
        if (value.matches("#[0-9A-Fa-f]{6}")) {
            return value.toLowerCase(Locale.ROOT);
        }
        throw new IllegalArgumentException("Couleur invalide (utilise le format #RRGGBB).");
    }

    static String defaultColorFor(String name) {
        String seed = name == null ? "" : name.toLowerCase(Locale.ROOT);
        int index = Math.floorMod(seed.hashCode(), DEFAULT_COLORS.length);
        return DEFAULT_COLORS[index];
    }

    static Map<String, String> merge(Map<String, String> official, List<ClubCalendarEvent> extras) {
        Map<String, String> events = new TreeMap<>(official);
        if (extras == null) {
            return events;
        }
        for (ClubCalendarEvent extra : extras) {
            if (extra == null || extra.getEventDate() == null || extra.getName() == null) {
                continue;
            }
            events.put(extra.getEventDate().toString(), extra.getName());
        }
        return events;
    }

    static String cleanName(String name) {
        String value = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Indique le nom de l’événement.");
        }
        if (value.length() > NAME_MAX) {
            throw new IllegalArgumentException("Le nom est trop long (max " + NAME_MAX + " caractères).");
        }
        return value;
    }

    static void requireDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date obligatoire.");
        }
        int year = date.getYear();
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new IllegalArgumentException("Date hors calendrier (" + MIN_YEAR + "–" + MAX_YEAR + ").");
        }
    }
}
