package com.towerdefence.engine.session;

import com.towerdefence.engine.sim.EnemyKind;

public record EnemyView(int id, EnemyKind kind, double pathT, double hpRatio, boolean shielded) {
    public boolean elite() {
        return kind == EnemyKind.BOSS;
    }
}
