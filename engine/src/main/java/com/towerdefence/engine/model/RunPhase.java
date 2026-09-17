package com.towerdefence.engine.model;

public enum RunPhase {
    IDLE,
    RUNNING,
    DEAD,
    /** Hit the wave cap of the current tier. The run is banked, unlike a death. */
    CLEARED
}
