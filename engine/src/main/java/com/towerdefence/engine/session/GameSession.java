package com.towerdefence.engine.session;

import com.towerdefence.engine.command.GameCommand;

public interface GameSession {
    void apply(GameCommand command);

    void tick(long nowNanos);

    /**
     * Moves the tick clock forward without simulating anything. A paused game must call this,
     * otherwise the whole paused stretch is handed to the next {@link #tick} and played out
     * in one jump, which turns pausing into a way to bank time.
     */
    void holdClock(long nowNanos);

    void catchUpElapsed(long elapsedNanos);

    /**
     * Feeds the real calendar clock in, used by research that runs on wall time rather than
     * simulated time. The engine never reads the clock itself so it stays deterministic.
     */
    void wallClock(long epochMillis);

    GameSnapshot snapshot();
}
