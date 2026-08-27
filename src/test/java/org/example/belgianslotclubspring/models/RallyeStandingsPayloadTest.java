package org.example.belgianslotclubspring.models;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RallyeStandingsPayloadTest {

    @Test
    void fingerprintChangesWhenATimeChanges() {
        RallyeStandingRow first = new RallyeStandingRow(
                1, 10L, "Alice", "Fiesta", "WRC 1",
                12.3, 0.0, 0.0, "12.300", "—", "—", 2, 5
        );
        RallyeStandingRow updated = new RallyeStandingRow(
                1, 10L, "Alice", "Fiesta", "WRC 1",
                11.1, 0.0, 0.0, "11.100", "—", "—", 2, 5
        );

        long before = RallyeStandingsPayload.fingerprintOf(List.of(first));
        long after = RallyeStandingsPayload.fingerprintOf(List.of(updated));

        assertNotEquals(before, after);
        assertEquals(before, RallyeStandingsPayload.fingerprintOf(List.of(first)));
    }
}
