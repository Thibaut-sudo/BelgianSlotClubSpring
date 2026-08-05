package org.example.belgianslotclubspring.repo;

import org.example.belgianslotclubspring.entities.Rallye;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RallyeRepo extends JpaRepository<Rallye, Long> {

    List<Rallye> findByClubNameOrderByDateDesc(String clubName);

    @Query("""
            SELECT DISTINCT r FROM Rallye r
            LEFT JOIN FETCH r.pilots p
            LEFT JOIN FETCH p.stageTimes
            WHERE r.id = :id
            """)
    Optional<Rallye> findDetailedById(@Param("id") Long id);
}
