package org.example.belgianslotclubspring.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Entity
@Getter
@Setter
public class Qualif {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private float bestTime;
    private String pilotName;
    private LocalDate date;

    public Qualif(String qualifName, String qualiTime) {
        this.bestTime = Float.parseFloat(qualiTime);
        this.pilotName = qualifName;
    }

    public Qualif(String qualifName, String qualiTime, String date) {
        this.bestTime = Float.parseFloat(qualiTime);
        this.pilotName = qualifName;
        this.date = convertStringToLocalDate(date);



    }



    private static LocalDate convertStringToLocalDate(String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.FRENCH);
        return LocalDate.parse(dateString, formatter);
    }


}
