package org.example.belgianslotclubspring.services.impl;

import org.example.belgianslotclubspring.entities.Rallye;
import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.models.RallyeChampionshipTable;
import org.example.belgianslotclubspring.models.RallyeStandingRow;
import org.example.belgianslotclubspring.services.RallyeChampionshipService;
import org.example.belgianslotclubspring.services.RallyeService;
import org.example.belgianslotclubspring.utils.RallyeChampionshipCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class RallyeChampionshipServiceImpl implements RallyeChampionshipService {

    private final RallyeService rallyeService;

    public RallyeChampionshipServiceImpl(RallyeService rallyeService) {
        this.rallyeService = rallyeService;
    }

    @Override
    @Transactional(readOnly = true)
    public RallyeChampionshipTable build(String club, Integer year, String category) {
        String clubCode = Club.requireCode(club);
        int season = year != null ? year : LocalDate.now().getYear();
        List<RallyeChampionshipCalculator.EventInput> events = new ArrayList<>();
        for (Rallye rallye : rallyeService.listByClub(clubCode)) {
            if (!countsForChampionship(rallye, season)) {
                continue;
            }
            Rallye detailed = rallyeService.get(rallye.getId());
            List<RallyeStandingRow> standings = rallyeService.standings(detailed.getId(), detailed.totalStages(), null);
            List<RallyeChampionshipCalculator.PilotInput> pilots = new ArrayList<>();
            for (RallyeStandingRow row : standings) {
                boolean classified = row.totalSeconds() != null && row.position() > 0;
                pilots.add(new RallyeChampionshipCalculator.PilotInput(
                        row.name(),
                        row.category(),
                        classified ? row.position() : 0,
                        classified
                ));
            }
            events.add(new RallyeChampionshipCalculator.EventInput(
                    detailed.getId(),
                    detailed.getName(),
                    detailed.getDate(),
                    pilots
            ));
        }
        return RallyeChampionshipCalculator.compile(season, events, category);
    }

    static boolean countsForChampionship(Rallye rallye, int year) {
        if (rallye == null || !rallye.isFinished() || rallye.getPilots() == null || rallye.getPilots().isEmpty()) {
            return false;
        }
        if (rallye.getDate() == null || rallye.getDate().getYear() != year) {
            return false;
        }
        String name = rallye.getName() == null ? "" : rallye.getName().trim().toLowerCase(Locale.ROOT);
        return !name.equals("test") && !name.startsWith("test ");
    }
}
