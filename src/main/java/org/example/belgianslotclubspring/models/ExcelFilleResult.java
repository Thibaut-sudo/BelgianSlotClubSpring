package org.example.belgianslotclubspring.models;

import lombok.Getter;
import lombok.Setter;
import org.example.belgianslotclubspring.entities.Qualif;
import org.example.belgianslotclubspring.entities.RaceResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Setter
@Getter
@Component
public class ExcelFilleResult {
    private List<Qualif> qualif;

    private List<RaceResult> raceResult; ;
    private String categoryName;



    public void setQualifs(List<Qualif> qualifs) {
        this.qualif = qualifs;
    }
    public void setRaceResults(List<RaceResult> raceResults) {
        this.raceResult = raceResults;
    }
    public void setCategoriseName(String categoryName) {
        this.categoryName = categoryName;

    }

    @Override
    public String toString() {
        return "ExcelFilleResult{" +
                "qualif=" + qualif +
                ", raceResult=" + raceResult +
                ", categoryName='" + categoryName + '\'' +
                '}';
    }

    public List<Qualif> getQualifs() {
        return qualif;
    }

    public List<RaceResult> getRaceResults() {
        return raceResult;
    }
}
