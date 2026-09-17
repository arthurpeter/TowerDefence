package com.towerdefence.engine.session;

import com.towerdefence.engine.model.LabId;

public record LabView(
        LabId id,
        int level,
        double cost,
        boolean affordable,
        boolean running,
        boolean maxed,
        double secondsRemaining,
        double secondsTotal,
        String effect
) {
    public float progress() {
        if (!running || secondsTotal <= 0) {
            return 0f;
        }
        return (float) Math.max(0d, Math.min(1d, 1d - secondsRemaining / secondsTotal));
    }
}
