package com.towerdefence.engine.session;

public record TierView(
        int tier,
        boolean unlocked,
        boolean active,
        boolean cleared,
        int bestWave,
        int unlockWave,
        int unlockFromTier,
        int nextUnlockWave,
        int waveCap,
        double hpMultiplier,
        double coinMultiplier,
        double shardMultiplier,
        int coresPerMilestone
) {}
