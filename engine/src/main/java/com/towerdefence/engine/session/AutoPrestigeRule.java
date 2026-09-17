package com.towerdefence.engine.session;

/**
 * When the game should cash a run out. Simple triggers are optional; zero disables one.
 * A non-blank {@code formula} takes over completely once the formula editor is unlocked.
 * Evaluated every tick, so a low-HP rule can fire mid-wave and during offline catch-up.
 */
public record AutoPrestigeRule(
        boolean enabled,
        int waveTarget,
        double stallSeconds,
        double hpPercent,
        String formula
) {
    public AutoPrestigeRule(boolean enabled, int waveTarget, double stallSeconds) {
        this(enabled, waveTarget, stallSeconds, 0, "");
    }

    public AutoPrestigeRule(boolean enabled, int waveTarget, double stallSeconds, String formula) {
        this(enabled, waveTarget, stallSeconds, 0, formula);
    }

    public static AutoPrestigeRule off() {
        return new AutoPrestigeRule(false, 0, 0, 0, "");
    }

    public boolean hasWaveTrigger() {
        return waveTarget >= 2;
    }

    public boolean hasStallTrigger() {
        return stallSeconds > 0;
    }

    public boolean hasHpTrigger() {
        return hpPercent > 0;
    }

    public boolean hasFormula() {
        return formula != null && !formula.isBlank();
    }

    public AutoPrestigeRule withFormula(String newFormula) {
        return new AutoPrestigeRule(enabled, waveTarget, stallSeconds, hpPercent, newFormula);
    }

    /** Only the simple triggers; the formula path is handled by the session, which owns the variables. */
    public boolean triggersSimply(int wave, double secondsSinceWaveChange, double hpPercentNow) {
        if (!enabled) {
            return false;
        }
        if (hasWaveTrigger() && wave >= waveTarget) {
            return true;
        }
        if (hasStallTrigger() && secondsSinceWaveChange >= stallSeconds) {
            return true;
        }
        return hasHpTrigger() && hpPercentNow <= hpPercent;
    }
}
