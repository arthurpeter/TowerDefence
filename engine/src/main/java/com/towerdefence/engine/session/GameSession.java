package com.towerdefence.engine.session;

import com.towerdefence.engine.command.GameCommand;

public interface GameSession {
    void apply(GameCommand command);

    void tick(long nowNanos);

    void catchUpElapsed(long elapsedNanos);

    /**
     * Feeds the real calendar clock in, used by research that runs on wall time rather than
     * simulated time. The engine never reads the clock itself so it stays deterministic.
     */
    void wallClock(long epochMillis);

    GameSnapshot snapshot();
}
