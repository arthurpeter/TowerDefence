package com.towerdefence.engine.session;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrestigeFormulaTest {
    private static final Map<String, Double> READINGS = Map.of(
            "wave", 42d,
            "hp", 500d,
            "hpPct", 25d,
            "coins", 1_000d,
            "shards", 80d,
            "cores", 3d,
            "sigils", 1d,
            "stall", 75d,
            "tier", 2d,
            "runs", 9d
    );

    private static boolean eval(String source) {
        return PrestigeFormula.compile(source).evaluate(name -> READINGS.getOrDefault(name, 0d));
    }

    @Test
    void comparesVariablesAgainstNumbers() {
        assertTrue(eval("wave > 40"));
        assertFalse(eval("wave > 50"));
        assertTrue(eval("wave >= 42"));
        assertTrue(eval("wave == 42"));
        assertTrue(eval("wave != 41"));
        assertTrue(eval("hpPct < 30"));
    }

    @Test
    void combinesConditionsWithAndOr() {
        assertTrue(eval("wave > 40 && hpPct < 30"));
        assertFalse(eval("wave > 40 && hpPct > 30"));
        assertTrue(eval("wave > 100 || stall > 60"));
        assertFalse(eval("wave > 100 || stall > 600"));
    }

    @Test
    void handlesArithmeticParenthesesAndNegation() {
        assertTrue(eval("wave * 2 > 80"));
        assertTrue(eval("(wave + 8) / 2 >= 25"));
        assertTrue(eval("!(wave < 10)"));
        assertTrue(eval("coins / shards > 10"));
    }

    @Test
    void twoCharacterOperatorsAreNotSplit() {
        // A naive parser reads ">=" as ">" then fails on "="; these must stay whole.
        assertTrue(eval("wave >= 42"));
        assertTrue(eval("wave <= 42"));
        assertFalse(eval("wave != 42"));
    }

    @Test
    void divisionByZeroIsTreatedAsZeroInsteadOfBlowingUp() {
        assertFalse(eval("coins / 0 > 1"));
    }

    @Test
    void rejectsBrokenInputWithAReadableMessage() {
        assertThrows(IllegalArgumentException.class, () -> PrestigeFormula.compile(""));
        assertThrows(IllegalArgumentException.class, () -> PrestigeFormula.compile("wave >"));
        assertThrows(IllegalArgumentException.class, () -> PrestigeFormula.compile("(wave > 2"));
        assertThrows(IllegalArgumentException.class, () -> PrestigeFormula.compile("wave # 2"));

        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class,
                () -> PrestigeFormula.compile("banana > 2"));
        assertTrue(unknown.getMessage().contains("banana"), unknown.getMessage());
    }

    @Test
    void everyAdvertisedVariableIsAccepted() {
        for (String name : PrestigeFormula.VARIABLES) {
            assertTrue(PrestigeFormula.isKnownVariable(name), name);
            String source = name + " >= 0";
            assertEquals(source, PrestigeFormula.compile(source).source());
            assertTrue(eval(source), source);
        }
    }
}
