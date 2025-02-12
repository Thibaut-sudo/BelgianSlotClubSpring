package org.example.belgianslotclubspring.repo;

import org.example.belgianslotclubspring.entities.RaceResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public interface RaceResultRepo extends JpaRepository<RaceResult, Integer> {

    @Query("SELECT DISTINCT r.date, r.categoryName FROM RaceResult r WHERE r.ClubName = :club")
    List<Object[]> getRaceResultDateByClubName(@Param("club") String club);

    @Query("SELECT DISTINCT concat(upper(substring(lower(r.categoryName), 1, 1)), substring(lower(r.categoryName), 2)) FROM RaceResult r")
    List<String> getAllCategoriesClub(String club);


}
