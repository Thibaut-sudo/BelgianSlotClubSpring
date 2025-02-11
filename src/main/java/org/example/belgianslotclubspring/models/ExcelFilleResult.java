package org.example.belgianslotclubspring.models;

import lombok.Getter;
import lombok.Setter;
import org.example.belgianslotclubspring.entities.Qualif;
import org.example.belgianslotclubspring.entities.RaceResult;

import java.util.List;

@Setter
@Getter
public class ExcelFilleResult {
    private List<Qualif> qualif;
    @Setter
    private List<RaceResult> raceResult; ;
    private String categoryName;



    public void setQualifs(List<Qualif> qualifs) {
        this.qualif = qualifs;
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
