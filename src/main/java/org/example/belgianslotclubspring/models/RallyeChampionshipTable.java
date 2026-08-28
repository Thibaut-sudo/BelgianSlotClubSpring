package org.example.belgianslotclubspring.models;

import java.util.List;

public record RallyeChampionshipTable(
        int year,
        List<RallyeChampionshipEvent> events,
        List<String> categories,
        String selectedCategory,
        boolean dropsWorst,
        List<RallyeChampionshipRow> rows
) {
    public int rallyCount() {
        return events == null ? 0 : events.size();
    }

    public int pilotCount() {
        return rows == null ? 0 : rows.size();
    }

    public RallyeChampionshipRow titleLeader() {
        if (rows == null) {
            return null;
        }
        for (RallyeChampionshipRow row : rows) {
            if (row.titleEligible()) {
                return row;
            }
        }
        return null;
    }

    public int leaderPoints() {
        RallyeChampionshipRow leader = titleLeader();
        if (leader != null) {
            return leader.total();
        }
        return rows == null || rows.isEmpty() ? 0 : rows.getFirst().total();
    }
}
