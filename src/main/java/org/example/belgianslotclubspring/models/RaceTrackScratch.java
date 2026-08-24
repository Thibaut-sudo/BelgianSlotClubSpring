package org.example.belgianslotclubspring.models;

import java.util.List;

/** Scratch (meilleur chrono) sur une piste. */
public record RaceTrackScratch(
        int trackNumber,
        String pilotName,
        String bestLapFormatted,
        String worstPilotName,
        String worstGapFormatted
) {
}
