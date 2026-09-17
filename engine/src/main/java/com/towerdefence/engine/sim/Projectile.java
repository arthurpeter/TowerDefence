package com.towerdefence.engine.sim;

/** A bullet travelling down the lane toward a specific enemy. */
public final class Projectile {
    public final int id;
    public final double damage;
    public final double speed;
    public final boolean crit;
    public int targetId;
    public double pathT;
    public int bouncesLeft;

    public Projectile(int id, int targetId, double pathT, double damage, double speed,
                      boolean crit, int bouncesLeft) {
        this.id = id;
        this.targetId = targetId;
        this.pathT = pathT;
        this.damage = damage;
        this.speed = speed;
        this.crit = crit;
        this.bouncesLeft = bouncesLeft;
    }
}
