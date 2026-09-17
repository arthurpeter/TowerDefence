package com.towerdefence.engine.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SciFormatTest {

    @Test
    void formatsSmallAndLargeValues() {
        assertEquals("12.5", SciFormat.of(12.5));
        assertTrue(SciFormat.of(15_000).endsWith("K"));
        assertTrue(SciFormat.of(2_500_000).endsWith("M"));
    }
}
