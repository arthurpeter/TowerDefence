package com.towerdefence.engine.session;

import com.towerdefence.engine.model.MissionId;

public record MissionView(
        MissionId id,
        String title,
        String detail,
        int reward,
        boolean completed,
        double current,
        double target
) {
    public float progress() {
        if (target <= 0) {
            return completed ? 1f : 0f;
        }
        return (float) Math.max(0d, Math.min(1d, current / target));
    }
}
