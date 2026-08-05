package org.example.belgianslotclubspring.repo;

import org.example.belgianslotclubspring.entities.Qualif;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface QualifRepo extends JpaRepository<Qualif, Integer> {

    Qualif findQualifById(Long id);

    @Query("""
            SELECT q FROM Qualif q
            WHERE q.date = :date AND lower(q.clubName) = lower(:club)
            ORDER BY q.bestTime ASC
            """)
    List<Qualif> getQualifByDateAndClub(@Param("date") LocalDate date, @Param("club") String club);

    /** @deprecated utiliser getQualifByDateAndClub — ne filtre pas par club */
    @Deprecated
    List<Qualif> getQualifByDate(LocalDate selectedDate);
}
