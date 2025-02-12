package org.example.belgianslotclubspring.repo;

import org.example.belgianslotclubspring.entities.Qualif;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface QualifRepo extends JpaRepository<Qualif, Integer> {

}
