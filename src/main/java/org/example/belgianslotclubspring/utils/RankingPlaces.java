package org.example.belgianslotclubspring.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Places olympiques (1, 2, 2, 4) sur une liste déjà triée.
 */
public final class RankingPlaces {

    private RankingPlaces() {
    }

    public static <T> List<Integer> olympic(List<T> ranked, ToDoubleFunction<T> score) {
        if (ranked == null || ranked.isEmpty()) {
            return List.of();
        }
        List<Integer> places = new ArrayList<>(ranked.size());
        int lastPos = 0;
        Double lastScore = null;
        for (int i = 0; i < ranked.size(); i++) {
            double value = score.applyAsDouble(ranked.get(i));
            if (lastScore == null || Double.compare(value, lastScore) != 0) {
                lastPos = i + 1;
                lastScore = value;
            }
            places.add(lastPos);
        }
        return List.copyOf(places);
    }
}
