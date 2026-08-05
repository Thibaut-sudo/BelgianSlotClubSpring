package org.example.belgianslotclubspring.repo;

import org.example.belgianslotclubspring.entities.RallyeGroupAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RallyeGroupAssignmentRepo extends JpaRepository<RallyeGroupAssignment, Long> {

    List<RallyeGroupAssignment> findByRallyeIdAndBoucleOrderByGroupNumberAscPositionInGroupAsc(
            Long rallyeId, int boucle
    );

    boolean existsByRallyeIdAndBoucle(Long rallyeId, int boucle);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RallyeGroupAssignment a WHERE a.rallye.id = :rallyeId AND a.boucle = :boucle")
    void deleteByRallyeIdAndBoucle(@Param("rallyeId") Long rallyeId, @Param("boucle") int boucle);
}
