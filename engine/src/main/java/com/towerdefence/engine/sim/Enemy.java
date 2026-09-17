package com.towerdefence.engine.sim;

public final class Enemy {
    public final int id;
    public final EnemyKind kind;
    public final double maxHp;
    public final double speed;
    public double pathT;
    public double hp;

    public Enemy(int id, EnemyKind kind, double hp, double speed) {
        this.id = id;
        this.kind = kind;
        this.pathT = 0;
        this.hp = hp;
        this.maxHp = hp;
        this.speed = speed;
    }

    public boolean isBoss() {
        return kind == EnemyKind.BOSS;
    }
}
