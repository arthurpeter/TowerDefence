package com.towerdefence.engine.command;

public sealed interface GameCommand {
    record BuyUpgrade(UpgradeId id) implements GameCommand {}

    /** Master switch for the auto buyer. */
    record SetAutoBuy(boolean enabled) implements GameCommand {}

    /** Picks which run stats the auto buyer is allowed to spend on. */
    record SetAutoTarget(UpgradeId id, boolean enabled) implements GameCommand {}

    record StartRun() implements GameCommand {}

    record Prestige() implements GameCommand {}

    /** Banks the current run and restarts in the given tier. */
    record SelectTier(int tier) implements GameCommand {}

    /** Replaces the auto-prestige rule; ignored until the rule set is unlocked. */
    record SetAutoPrestige(com.towerdefence.engine.session.AutoPrestigeRule rule) implements GameCommand {}

    record CastAbility(com.towerdefence.engine.model.AbilityId id) implements GameCommand {}

    record SetAutoCast(boolean enabled) implements GameCommand {}

    record SetAutoRun(boolean enabled) implements GameCommand {}

    record SetAutoRunTier(int tier) implements GameCommand {}

    /** Begins a real-clock research; one runs at a time. */
    record StartLab(com.towerdefence.engine.model.LabId id) implements GameCommand {}
}
