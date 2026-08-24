package org.example.belgianslotclubspring.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RallyeTimeFormatTest {

    @Test
    void parsesDecimalAndClockFormats() {
        assertEquals(12.345, RallyeTimeFormat.parse("12.345"), 0.0001);
        assertEquals(12.345, RallyeTimeFormat.parse("12,345"), 0.0001);
        assertEquals(72.784, RallyeTimeFormat.parse("72,784"), 0.0001);
        assertEquals(72.784, RallyeTimeFormat.parse("72.784"), 0.0001);
        assertEquals(83.45, RallyeTimeFormat.parse("1:23.45"), 0.0001);
        assertEquals(83.45, RallyeTimeFormat.parse("1:23,45"), 0.0001);
        assertEquals(3723.4, RallyeTimeFormat.parse("1:02:03.4"), 0.0001);
        assertNull(RallyeTimeFormat.parse(""));
        assertNull(RallyeTimeFormat.parse("  "));
        assertNull(RallyeTimeFormat.parse("—"));
    }

    @Test
    void formatsSeconds() {
        assertEquals("12.345", RallyeTimeFormat.format(12.345));
        assertEquals("1:23.450", RallyeTimeFormat.format(83.45));
        assertEquals("+1.000", RallyeTimeFormat.formatGap(1.0));
        assertEquals("—", RallyeTimeFormat.formatGap(0.0));
    }
}
