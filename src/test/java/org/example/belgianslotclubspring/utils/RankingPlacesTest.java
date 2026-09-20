package org.example.belgianslotclubspring.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RankingPlacesTest {

    @Test
    void sharesPlaceOnEqualScoreThenSkips() {
        record Row(double score) {
        }
        List<Row> ranked = List.of(
                new Row(10),
                new Row(8),
                new Row(8),
                new Row(5)
        );
        assertEquals(List.of(1, 2, 2, 4), RankingPlaces.olympic(ranked, Row::score));
    }

    @Test
    void tiesForFirstSkipToThird() {
        record Row(double score) {
        }
        assertEquals(
                List.of(1, 1, 3),
                RankingPlaces.olympic(List.of(new Row(50), new Row(50), new Row(40)), Row::score)
        );
    }

    @Test
    void emptyListIsEmpty() {
        assertEquals(List.of(), RankingPlaces.olympic(List.of(), value -> 0));
    }
}
