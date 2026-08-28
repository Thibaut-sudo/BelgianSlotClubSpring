package org.example.belgianslotclubspring.services;

import org.example.belgianslotclubspring.models.RallyeChampionshipTable;

public interface RallyeChampionshipService {

    RallyeChampionshipTable build(String club, Integer year, String category);
}
