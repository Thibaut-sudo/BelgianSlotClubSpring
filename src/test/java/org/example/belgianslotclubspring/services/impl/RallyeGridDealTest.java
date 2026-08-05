package org.example.belgianslotclubspring.services.impl;

import org.example.belgianslotclubspring.models.RallyeGridPilot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RallyeGridDealTest {

    @Test
    void fifthPilotGoesToFirstGroupWhenFourSpecials() {
        List<RallyeGridPilot> pilots = pilots(5);

        List<List<RallyeGridPilot>> groups = RallyeServiceImpl.dealRoundRobin(pilots, 4);

        assertEquals(List.of(1, 5), numbers(groups.get(0)));
        assertEquals(List.of(2), numbers(groups.get(1)));
        assertEquals(List.of(3), numbers(groups.get(2)));
        assertEquals(List.of(4), numbers(groups.get(3)));
    }

    @Test
    void roundRobinFillsEvenly() {
        List<RallyeGridPilot> pilots = pilots(21);

        List<List<RallyeGridPilot>> groups = RallyeServiceImpl.dealRoundRobin(pilots, 5);

        assertEquals(List.of(1, 6, 11, 16, 21), numbers(groups.get(0)));
        assertEquals(List.of(2, 7, 12, 17), numbers(groups.get(1)));
        assertEquals(List.of(3, 8, 13, 18), numbers(groups.get(2)));
        assertEquals(List.of(4, 9, 14, 19), numbers(groups.get(3)));
        assertEquals(List.of(5, 10, 15, 20), numbers(groups.get(4)));
    }

    @Test
    void startingOrderRotatesWithinGroupPerEs() {
        List<RallyeGridPilot> group = pilots(2); // #1 puis #2 (ordre classement figé)

        assertEquals(List.of(1, 2), numbers(RallyeServiceImpl.rotateList(group, 0)));
        assertEquals(List.of(2, 1), numbers(RallyeServiceImpl.rotateList(group, 1)));
        assertEquals(List.of(1, 2), numbers(RallyeServiceImpl.rotateList(group, 2)));
        assertEquals(List.of(2, 1), numbers(RallyeServiceImpl.rotateList(group, 3)));
    }

    @Test
    void startingOrderRotatesWithThreePilots() {
        List<RallyeGridPilot> group = pilots(3);

        assertEquals(List.of(1, 2, 3), numbers(RallyeServiceImpl.rotateList(group, 0)));
        assertEquals(List.of(2, 3, 1), numbers(RallyeServiceImpl.rotateList(group, 1)));
        assertEquals(List.of(3, 1, 2), numbers(RallyeServiceImpl.rotateList(group, 2)));
    }

    private static List<RallyeGridPilot> pilots(int count) {
        List<RallyeGridPilot> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(new RallyeGridPilot((long) i, i, "P" + i, null));
        }
        return list;
    }

    private static List<Integer> numbers(List<RallyeGridPilot> group) {
        return group.stream().map(RallyeGridPilot::startNumber).collect(Collectors.toList());
    }
}
