package org.example.belgianslotclubspring.services;

import org.example.belgianslotclubspring.entities.Qualif;

import java.time.LocalDate;
import java.util.List;

public interface QualifService {
    void saveQualif(Qualif qualif);
    void updateQualif(Qualif qualif);
    void deleteQualif(Qualif qualif);
    Qualif findQualifById(Long id);


    Qualif findQualifByName(String name);

    void saveQualifList(List<Qualif> qualifs);

    List<Qualif> getQualifByDate(LocalDate selectedDate);
}
