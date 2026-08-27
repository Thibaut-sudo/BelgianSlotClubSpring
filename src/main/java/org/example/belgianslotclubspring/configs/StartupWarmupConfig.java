package org.example.belgianslotclubspring.configs;

import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.services.RaceResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Précharge la maintenance (normalize / bis) et les listes de courses
 * pour que le premier visiteur ne paie pas 3–5 s de TTFB.
 */
@Configuration
@EnableAsync
@Profile("prod")
public class StartupWarmupConfig {

    private static final Logger log = LoggerFactory.getLogger(StartupWarmupConfig.class);

    private final RaceResultService raceResultService;

    public StartupWarmupConfig(RaceResultService raceResultService) {
        this.raceResultService = raceResultService;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void warmCaches() {
        for (Club club : Club.values()) {
            if (club.isRallyOnly()) {
                continue;
            }
            try {
                raceResultService.getRaceSummariesByClub(club.getCode());
                raceResultService.getAllCategoriesClub(club.getCode());
                var years = raceResultService.getAvailableYears(club.getCode());
                if (!years.isEmpty()) {
                    Integer year = years.get(0);
                    for (String category : raceResultService.getAllCategoriesClub(club.getCode())) {
                        raceResultService.getChampionshipResults(category, club.getCode(), year);
                    }
                }
                log.info("Warmup terminé pour {}", club.getCode());
            } catch (Exception e) {
                log.warn("Warmup échoué pour {}: {}", club.getCode(), e.getMessage());
            }
        }
    }
}
