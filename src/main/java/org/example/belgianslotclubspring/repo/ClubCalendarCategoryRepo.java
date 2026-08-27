package org.example.belgianslotclubspring.repo;

import org.example.belgianslotclubspring.entities.ClubCalendarCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClubCalendarCategoryRepo extends JpaRepository<ClubCalendarCategory, Long> {

    List<ClubCalendarCategory> findByClubNameOrderByNameAsc(String clubName);

    Optional<ClubCalendarCategory> findByClubNameAndNameIgnoreCase(String clubName, String name);

    void deleteByClubNameAndNameIgnoreCase(String clubName, String name);
}
