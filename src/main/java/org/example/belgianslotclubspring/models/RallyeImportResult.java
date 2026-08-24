package org.example.belgianslotclubspring.models;

/** Résultat d'un import Excel rallye (pilotes + temps). */
public record RallyeImportResult(
        int pilotsAdded,
        int timesImported,
        int bouclesDetected,
        int stagesDetected
) {
    public String successMessage() {
        StringBuilder sb = new StringBuilder();
        if (pilotsAdded > 0) {
            sb.append(pilotsAdded).append(" pilote(s) ajouté(s)");
        } else {
            sb.append("Aucun nouveau pilote");
        }
        if (timesImported > 0) {
            sb.append(" · ").append(timesImported).append(" temps importé(s)");
        }
        if (bouclesDetected > 0 && stagesDetected > 0) {
            sb.append(" (").append(bouclesDetected).append(" boucles × ")
                    .append(stagesDetected).append(" ES)");
        }
        sb.append('.');
        return sb.toString();
    }
}
