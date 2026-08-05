package org.example.belgianslotclubspring.services;

import org.example.belgianslotclubspring.entities.Rallye;
import org.example.belgianslotclubspring.entities.RallyePilot;
import org.example.belgianslotclubspring.models.RallyeBoucleGrid;
import org.example.belgianslotclubspring.models.RallyeRecaps;
import org.example.belgianslotclubspring.models.RallyeStandingRow;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface RallyeService {

    List<Rallye> listByClub(String club);

    Rallye get(Long id);

    Rallye create(String name, LocalDate date, String club, Integer boucles, Integer stagesPerBoucle);

    void delete(Long id);

    RallyePilot addPilot(Long rallyeId, String name, String car, String category);

    void updatePilot(Long rallyeId, Long pilotId, String name, String car, String category);

    void removePilot(Long rallyeId, Long pilotId);

    /**
     * Sauvegarde les temps d'une boucle.
     * Map clé = pilotId, valeur = map stage→temps texte (stage 0 = PENO).
     */
    void saveBoucleTimes(Long rallyeId, int boucle, Map<Long, Map<Integer, String>> timesByPilot);

    /**
     * Classement après {@code afterStages} ES (ex. 5, 10, 15, 20).
     * Si null, utilise toutes les ES déjà courues pour le max atteint.
     */
    List<RallyeStandingRow> standings(Long rallyeId, Integer afterStages, String categoryFilter);

    /** Compte-rendus par boucle + résumé final (scratchs, coups durs, podium). */
    RallyeRecaps buildRecaps(Long rallyeId);

    /** Importe nom + voiture (+ catégorie) depuis la feuille « Pilotes » d'un Excel rallye SRCS. */
    int importPilotsFromExcel(Long rallyeId, String filePath);

    /**
     * Génère la grille de groupes / manches pour une boucle (comme feuilles B1–B4 Excel).
     * Nombre de groupes = nombre d'ES. Chaque manche fait tourner les groupes sur les ES.
     */
    RallyeBoucleGrid buildGroupGrid(Long rallyeId, int boucle);

    /**
     * Enregistre une composition manuelle des groupes pour une boucle.
     * {@code groups} = liste de groupes, chaque groupe = liste ordonnée d'ids pilotes.
     */
    void saveGroupAssignments(Long rallyeId, int boucle, List<List<Long>> groups);

    /** Efface la composition manuelle et revient à la répartition automatique. */
    void clearGroupAssignments(Long rallyeId, int boucle);

    /** Ajoute une boucle (passage) supplémentaire. */
    Rallye addBoucle(Long rallyeId);

    /** Ajoute une ES (spéciale) supplémentaire. */
    Rallye addStage(Long rallyeId);

    /** Supprime la dernière boucle et ses temps associés. */
    Rallye removeBoucle(Long rallyeId);

    /** Supprime la dernière ES et ses temps associés. */
    Rallye removeStage(Long rallyeId);
}
