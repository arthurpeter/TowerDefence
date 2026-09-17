package com.towerdefence.engine.model;

/**
 * Fixed goal board. Rewards {@link Currency#CREST} and never pay out from waves directly.
 */
public enum MissionId {
    WAVE_30(30, 1),
    WAVE_80(80, 2),
    WAVE_200(200, 3),
    WAVE_500(500, 5),
    WAVE_2000(2000, 8),
    CLEAR_TIER(1, 12),
    TIER_2(2, 2),
    TIER_4(4, 4),
    PRESTIGE_1(1, 1),
    PRESTIGE_5(5, 3),
    PRESTIGE_20(20, 6),
    KILL_100(100, 1),
    KILL_1000(1000, 3),
    ELITE_15(15, 2),
    FIRST_LAB(1, 2),
    UNLOCK_AUTO(1, 2);

    private final int target;
    private final int crests;

    MissionId(int target, int crests) {
        this.target = target;
        this.crests = crests;
    }

    public int target() {
        return target;
    }

    public int crests() {
        return crests;
    }
}
