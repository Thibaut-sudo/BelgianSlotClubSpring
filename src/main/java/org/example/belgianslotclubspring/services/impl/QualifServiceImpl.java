package org.example.belgianslotclubspring.services.impl;

import org.example.belgianslotclubspring.entities.Qualif;
import org.example.belgianslotclubspring.repo.QualifRepo;
import org.example.belgianslotclubspring.services.QualifService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class QualifServiceImpl implements QualifService {


    private final QualifRepo qualifRepo;


    public QualifServiceImpl(QualifRepo qualifRepo) {
        this.qualifRepo = qualifRepo;
    }


    @Override
    public void saveQualif(Qualif qualif) {
        qualifRepo.save(qualif);


    }

    @Override
    public void updateQualif(Qualif qualif) {

    }

    @Override
    public void deleteQualif(Qualif qualif) {

    }


    @Override
    public Qualif findQualifById(Long id) {

        return qualifRepo.findQualifById(id);

    }



    @Override
    public Qualif findQualifByName(String name) {
        return null;
    }

    @Override
    public void saveQualifList(List<Qualif> qualifs) {
        for (Qualif qualif : qualifs) {

            saveQualif(qualif);
        }

    }

    @Override
    public List<Qualif> getQualifByDate(LocalDate selectedDate) {
        return qualifRepo.getQualifByDate(selectedDate);
    }

}