package com.towerdefence.engine.sim;

public enum EnemyKind {
    /** Plain walker. */
    BASIC,
    /** Low health, sprints up the lane. */
    FAST,
    /** Slow and thick. */
    TANK,
    /** Stops short of the tower and shoots it, so it never leaks. */
    RANGED,
    /** Shields every other live enemy until it dies. */
    PROTECTOR,
    /** Closes out milestone waves. */
    BOSS
}
