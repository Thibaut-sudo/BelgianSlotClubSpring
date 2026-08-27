package org.example.belgianslotclubspring.repo;

import org.example.belgianslotclubspring.entities.ClubCalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClubCalendarEventRepo extends JpaRepository<ClubCalendarEvent, Long> {

    List<ClubCalendarEvent> findByClubName(String clubName);

    Optional<ClubCalendarEvent> findByClubNameAndEventDate(String clubName, LocalDate eventDate);

    void deleteByClubNameAndEventDate(String clubName, LocalDate eventDate);
}
