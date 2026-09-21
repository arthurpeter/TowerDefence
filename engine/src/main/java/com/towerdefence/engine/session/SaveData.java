package com.towerdefence.engine.session;

import com.towerdefence.engine.model.RunPhase;
import com.towerdefence.engine.sim.EnemyKind;

import java.util.List;

public record SaveData(
        RunPhase phase,
        int wave,
        int tier,
        double coins,
        double shards,
        double cores,
        double sigils,
        double crests,
        double stars,
        int lastRunWave,
        int coresAwarded,
        int sigilsAwarded,
        int prestiges,
        long enemiesKilled,
        long elitesKilled,
        int labsFinished,
        boolean[] missionsDone,
        boolean[] tiersCleared,
        double towerHp,
        double wall,
        int[] levels,
        int[] bestWavePerTier,
        boolean[] autoTargets,
        boolean autoBuy,
        AutoPrestigeRule autoPrestige,
        boolean autoCast,
        boolean autoRun,
        int autoRunTier,
        double[] abilityCooldowns,
        double goldenRemaining,
        double blackHoleRemaining,
        int[] labLevels,
        int runningLabOrdinal,
        long labEndsAtMillis,
        long labStartedAtMillis,
        int runsCompleted,
        int remainingToSpawn,
        int spawnedInWave,
        double spawnCooldown,
        double waveBreak,
        double fireCooldown,
        int nextEnemyId,
        long wallClockMillis,
        List<EnemySave> enemies
) {
    public record EnemySave(int id, EnemyKind kind, double pathT, double hp, double maxHp, double speed) {}
}
