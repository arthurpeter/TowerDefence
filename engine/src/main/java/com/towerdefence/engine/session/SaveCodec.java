package com.towerdefence.engine.session;

import com.towerdefence.engine.model.RunPhase;
import com.towerdefence.engine.sim.EnemyKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SaveCodec {
    private static final String HEADER = "TD12";

    private SaveCodec() {}

    public static String encode(SaveData save) {
        StringBuilder out = new StringBuilder();
        out.append(HEADER).append('\n');
        out.append(save.phase()).append('\n');
        out.append(save.wave()).append('\n');
        out.append(save.tier()).append('\n');
        appendDouble(out, save.coins());
        appendDouble(out, save.shards());
        appendDouble(out, save.cores());
        appendDouble(out, save.sigils());
        appendDouble(out, save.crests());
        appendDouble(out, save.stars());
        out.append(save.lastRunWave()).append('\n');
        out.append(save.coresAwarded()).append('\n');
        out.append(save.sigilsAwarded()).append('\n');
        out.append(save.prestiges()).append('\n');
        out.append(save.enemiesKilled()).append('\n');
        out.append(save.elitesKilled()).append('\n');
        out.append(save.labsFinished()).append('\n');
        appendFlags(out, save.missionsDone());
        appendFlags(out, save.tiersCleared());
        appendDouble(out, save.towerHp());
        appendDouble(out, save.wall());
        appendInts(out, save.levels());
        appendInts(out, save.bestWavePerTier());
        appendFlags(out, save.autoTargets());
        out.append(save.autoBuy()).append('\n');
        AutoPrestigeRule rule = save.autoPrestige();
        out.append(rule.enabled()).append(' ')
                .append(rule.waveTarget()).append(' ')
                .append(format(rule.stallSeconds())).append(' ')
                .append(format(rule.hpPercent())).append('\n');
        // Formula lives on its own line: it may contain spaces.
        out.append(rule.formula() == null ? "" : rule.formula().replace('\n', ' ')).append('\n');
        out.append(save.autoCast()).append('\n');
        out.append(save.autoRun()).append('\n');
        out.append(save.autoRunTier()).append('\n');
        appendDoubles(out, save.abilityCooldowns());
        appendDouble(out, save.goldenRemaining());
        appendDouble(out, save.blackHoleRemaining());
        appendInts(out, save.labLevels());
        out.append(save.runningLabOrdinal()).append('\n');
        out.append(save.labEndsAtMillis()).append('\n');
        out.append(save.labStartedAtMillis()).append('\n');
        out.append(save.runsCompleted()).append('\n');
        out.append(save.remainingToSpawn()).append('\n');
        out.append(save.spawnedInWave()).append('\n');
        appendDouble(out, save.spawnCooldown());
        appendDouble(out, save.waveBreak());
        appendDouble(out, save.fireCooldown());
        out.append(save.nextEnemyId()).append('\n');
        out.append(save.wallClockMillis()).append('\n');
        out.append(save.enemies().size()).append('\n');
        for (SaveData.EnemySave enemy : save.enemies()) {
            out.append(enemy.id()).append(' ');
            out.append(enemy.kind()).append(' ');
            out.append(format(enemy.pathT())).append(' ');
            out.append(format(enemy.hp())).append(' ');
            out.append(format(enemy.maxHp())).append(' ');
            out.append(format(enemy.speed())).append('\n');
        }
        return out.toString();
    }

    public static SaveData decode(String text) {
        String[] lines = text.replace("\r\n", "\n").split("\n", -1);
        if (lines.length < 45 || !HEADER.equals(lines[0])) {
            throw new IllegalArgumentException("Unknown save format");
        }
        int i = 1;
        RunPhase phase = RunPhase.valueOf(lines[i++]);
        int wave = Integer.parseInt(lines[i++]);
        int tier = Integer.parseInt(lines[i++]);
        double coins = Double.parseDouble(lines[i++]);
        double shards = Double.parseDouble(lines[i++]);
        double cores = Double.parseDouble(lines[i++]);
        double sigils = Double.parseDouble(lines[i++]);
        double crests = Double.parseDouble(lines[i++]);
        double stars = Double.parseDouble(lines[i++]);
        int lastRunWave = Integer.parseInt(lines[i++]);
        int coresAwarded = Integer.parseInt(lines[i++]);
        int sigilsAwarded = Integer.parseInt(lines[i++]);
        int prestiges = Integer.parseInt(lines[i++]);
        long enemiesKilled = Long.parseLong(lines[i++]);
        long elitesKilled = Long.parseLong(lines[i++]);
        int labsFinished = Integer.parseInt(lines[i++]);
        boolean[] missionsDone = parseFlags(lines[i++]);
        boolean[] tiersCleared = parseFlags(lines[i++]);
        double towerHp = Double.parseDouble(lines[i++]);
        double wall = Double.parseDouble(lines[i++]);
        int[] levels = parseInts(lines[i++]);
        int[] bestWavePerTier = parseInts(lines[i++]);
        boolean[] autoTargets = parseFlags(lines[i++]);
        boolean autoBuy = Boolean.parseBoolean(lines[i++]);
        String[] ruleParts = lines[i++].trim().split(" ");
        String formula = lines[i++];
        AutoPrestigeRule autoPrestige = new AutoPrestigeRule(
                Boolean.parseBoolean(ruleParts[0]),
                Integer.parseInt(ruleParts[1]),
                Double.parseDouble(ruleParts[2]),
                ruleParts.length > 3 ? Double.parseDouble(ruleParts[3]) : 0d,
                formula
        );
        boolean autoCast = Boolean.parseBoolean(lines[i++]);
        boolean autoRun = Boolean.parseBoolean(lines[i++]);
        int autoRunTier = Integer.parseInt(lines[i++]);
        double[] abilityCooldowns = parseDoubles(lines[i++]);
        double goldenRemaining = Double.parseDouble(lines[i++]);
        double blackHoleRemaining = Double.parseDouble(lines[i++]);
        int[] labLevels = parseInts(lines[i++]);
        int runningLabOrdinal = Integer.parseInt(lines[i++]);
        long labEndsAtMillis = Long.parseLong(lines[i++]);
        long labStartedAtMillis = Long.parseLong(lines[i++]);
        int runsCompleted = Integer.parseInt(lines[i++]);
        int remainingToSpawn = Integer.parseInt(lines[i++]);
        int spawnedInWave = Integer.parseInt(lines[i++]);
        double spawnCooldown = Double.parseDouble(lines[i++]);
        double waveBreak = Double.parseDouble(lines[i++]);
        double fireCooldown = Double.parseDouble(lines[i++]);
        int nextEnemyId = Integer.parseInt(lines[i++]);
        long wallClockMillis = Long.parseLong(lines[i++]);
        int enemyCount = Integer.parseInt(lines[i++]);

        List<SaveData.EnemySave> enemies = new ArrayList<>(enemyCount);
        for (int n = 0; n < enemyCount; n++) {
            String[] parts = lines[i++].trim().split(" ");
            enemies.add(new SaveData.EnemySave(
                    Integer.parseInt(parts[0]),
                    EnemyKind.valueOf(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]),
                    Double.parseDouble(parts[4]),
                    Double.parseDouble(parts[5])
            ));
        }

        return new SaveData(
                phase,
                wave,
                tier,
                coins,
                shards,
                cores,
                sigils,
                crests,
                stars,
                lastRunWave,
                coresAwarded,
                sigilsAwarded,
                prestiges,
                enemiesKilled,
                elitesKilled,
                labsFinished,
                missionsDone,
                tiersCleared,
                towerHp,
                wall,
                levels,
                bestWavePerTier,
                autoTargets,
                autoBuy,
                autoPrestige,
                autoCast,
                autoRun,
                autoRunTier,
                abilityCooldowns,
                goldenRemaining,
                blackHoleRemaining,
                labLevels,
                runningLabOrdinal,
                labEndsAtMillis,
                labStartedAtMillis,
                runsCompleted,
                remainingToSpawn,
                spawnedInWave,
                spawnCooldown,
                waveBreak,
                fireCooldown,
                nextEnemyId,
                wallClockMillis,
                List.copyOf(enemies)
        );
    }

    private static void appendFlags(StringBuilder out, boolean[] values) {
        out.append(values.length);
        for (boolean value : values) {
            out.append(' ').append(value ? 1 : 0);
        }
        out.append('\n');
    }

    private static boolean[] parseFlags(String line) {
        String[] parts = line.trim().split(" ");
        int count = Integer.parseInt(parts[0]);
        boolean[] values = new boolean[count];
        for (int n = 0; n < count; n++) {
            values[n] = "1".equals(parts[n + 1]);
        }
        return values;
    }

    private static void appendDoubles(StringBuilder out, double[] values) {
        out.append(values.length);
        for (double value : values) {
            out.append(' ').append(format(value));
        }
        out.append('\n');
    }

    private static double[] parseDoubles(String line) {
        String[] parts = line.trim().split(" ");
        int count = Integer.parseInt(parts[0]);
        double[] values = new double[count];
        for (int n = 0; n < count; n++) {
            values[n] = Double.parseDouble(parts[n + 1]);
        }
        return values;
    }

    private static void appendInts(StringBuilder out, int[] values) {
        out.append(values.length);
        for (int value : values) {
            out.append(' ').append(value);
        }
        out.append('\n');
    }

    private static int[] parseInts(String line) {
        String[] parts = line.trim().split(" ");
        int count = Integer.parseInt(parts[0]);
        int[] values = new int[count];
        for (int n = 0; n < count; n++) {
            values[n] = Integer.parseInt(parts[n + 1]);
        }
        return values;
    }

    private static void appendDouble(StringBuilder out, double value) {
        out.append(format(value)).append('\n');
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.10f", value);
    }
}
