package org.example.belgianslotclubspring.repo;

import org.example.belgianslotclubspring.entities.RaceResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RaceResultRepo extends JpaRepository<RaceResult, Integer> {

    @Query("""
            SELECT DISTINCT r.date, trim(r.categoryName)
            FROM RaceResult r
            WHERE lower(r.ClubName) = lower(:club)
            ORDER BY r.date DESC
            """)
    List<Object[]> getRaceResultDateByClubName(@Param("club") String club);

    @Query("""
            SELECT DISTINCT trim(r.categoryName)
            FROM RaceResult r
            WHERE lower(r.ClubName) = lower(:club)
              AND r.categoryName IS NOT NULL
              AND trim(r.categoryName) <> ''
            """)
    List<String> getAllCategoriesClub(@Param("club") String club);

    @Query("""
            SELECT DISTINCT trim(r.categoryName)
            FROM RaceResult r
            WHERE lower(r.ClubName) = lower(:club)
              AND r.categoryName IS NOT NULL
              AND trim(r.categoryName) <> ''
              AND year(r.date) = :year
            """)
    List<String> getAllCategoriesClubForYear(@Param("club") String club, @Param("year") Integer year);

    @Query("""
            SELECT r FROM RaceResult r
            WHERE r.date = :date AND lower(r.ClubName) = lower(:club)
            ORDER BY r.totalTours DESC
            """)
    List<RaceResult> getRaceResultByDateAndClub(@Param("date") LocalDate date, @Param("club") String club);

    @Query("""
            SELECT r.date, r.nom, r.totalTours, r.categoryName
            FROM RaceResult r
            WHERE lower(r.ClubName) = lower(:club)
              AND (:year IS NULL OR year(r.date) = :year)
            ORDER BY r.date ASC
            """)
    List<Object[]> getChampionshipResultsRaw(
            @Param("club") String club,
            @Param("year") Integer year
    );

    @Query("""
            SELECT r FROM RaceResult r
            WHERE r.date = :date AND lower(r.ClubName) = lower(:club)
            """)
    List<RaceResult> findByDateAndClub(
            @Param("date") LocalDate date,
            @Param("club") String club
    );

    @Query("""
            SELECT r FROM RaceResult r
            WHERE lower(r.ClubName) = lower(:club)
            """)
    List<RaceResult> findAllByClub(@Param("club") String club);

    @Query("""
            SELECT COUNT(r) FROM RaceResult r
            WHERE r.date = :date AND lower(r.ClubName) = lower(:club)
            """)
    long countByDateAndClub(@Param("date") LocalDate date, @Param("club") String club);

    @Query("""
            SELECT DISTINCT year(r.date)
            FROM RaceResult r
            WHERE lower(r.ClubName) = lower(:club)
            ORDER BY year(r.date) ASC
            """)
    List<Integer> getAvailableYears(@Param("club") String club);
}
