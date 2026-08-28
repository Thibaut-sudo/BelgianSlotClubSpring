package org.example.belgianslotclubspring.configs;

import org.example.belgianslotclubspring.entities.Rallye;
import org.example.belgianslotclubspring.repo.RallyeRepo;
import org.example.belgianslotclubspring.services.ClubCalendarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Aligne le calendrier club sur les rallyes déjà en base (création, déploiement).
 */
@Component
@Order(50)
public class RallyeCalendarSync implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RallyeCalendarSync.class);

    private final RallyeRepo rallyeRepo;
    private final ClubCalendarService clubCalendarService;

    public RallyeCalendarSync(RallyeRepo rallyeRepo, ClubCalendarService clubCalendarService) {
        this.rallyeRepo = rallyeRepo;
        this.clubCalendarService = clubCalendarService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int synced = 0;
        for (Rallye rallye : rallyeRepo.findAll()) {
            if (clubCalendarService.upsertFromRallye(rallye)) {
                synced++;
            }
        }
        log.info("Calendrier : {} rallye(s) synchronisé(s).", synced);
    }
}
