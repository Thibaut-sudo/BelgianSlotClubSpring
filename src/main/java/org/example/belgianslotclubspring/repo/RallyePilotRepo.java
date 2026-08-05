package org.example.belgianslotclubspring.repo;

import org.example.belgianslotclubspring.entities.RallyePilot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RallyePilotRepo extends JpaRepository<RallyePilot, Long> {

    List<RallyePilot> findByRallyeIdOrderByStartNumberAscIdAsc(Long rallyeId);

    void deleteByRallyeIdAndId(Long rallyeId, Long id);
}
