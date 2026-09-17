package com.towerdefence.engine.session;

import com.towerdefence.engine.model.AbilityId;

public record AbilityView(
        AbilityId id,
        int level,
        boolean unlocked,
        double cost,
        boolean affordable,
        double cooldownRemaining,
        double cooldownTotal,
        double activeRemaining,
        boolean ready,
        String effect
) {
    public float cooldownRatio() {
        return cooldownTotal <= 0 ? 0f : (float) (cooldownRemaining / cooldownTotal);
    }
}
