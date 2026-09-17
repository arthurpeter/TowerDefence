package com.towerdefence.engine.session;

import com.towerdefence.engine.command.GameCommand;
import com.towerdefence.engine.command.UpgradeId;
import com.towerdefence.engine.config.GameBalance;
import com.towerdefence.engine.model.RunPhase;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A headless play-through of the live rules: auto-buy, cash out at low HP, climb when unlocked.
 * Prints a table so we can see whether runs still end, how fast the ceiling moves, and when
 * the next tier actually appears. Keep this as the place to re-check after a balance pass.
 */
class BalanceProbeTest {
    private static final int RUNS = 12;
    private static final int STEP_SECONDS = 10;
    private static final int MAX_STEPS_PER_RUN = 180;

    @Test
    void probePrintsTheEarlyGameCurve() {
        GameBalance balance = GameBalance.standard().copy();
        balance.autoUnlockShards = 0;
        balance.autoPrestigeCost = 0;

        double damageGrowth = Math.pow(
                1 + balance.coinPerWave,
                Math.log(1 + balance.powerPerLevel) / Math.log(1 + balance.powerCostGrowth)
        );
        double hpGrowth = 1 + balance.enemyHpGrowth;
        System.out.printf(Locale.ROOT,
                "per-wave growth  hp x%.4f  buyable-damage x%.4f  (hp must stay ahead)%n",
                hpGrowth, damageGrowth);
        System.out.printf(Locale.ROOT,
                "unlock T1=%d T2=%d T3=%d T4=%d T6=%d  cap %d  shard@1000=x%.2f%n",
                balance.unlockWaveOn(1), balance.unlockWaveOn(2),
                balance.unlockWaveOn(3), balance.unlockWaveOn(4),
                balance.unlockWaveOn(6), balance.unlockCap,
                balance.shardDamageMultiplier(1000));

        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO));
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO_PRESTIGE));
        session.apply(new GameCommand.SetAutoPrestige(new AutoPrestigeRule(true, 0, 0, 25, "")));
        session.tick(0);

        List<String> rows = new ArrayList<>();
        int lastPrestiges = session.snapshot().runsCompleted();
        int lastBest = session.snapshot().bestWave();

        for (int run = 1; run <= RUNS; run++) {
            int startWave = session.snapshot().wave();
            RunPhase end = RunPhase.RUNNING;
            int peak = startWave;
            int startTier = session.snapshot().tier();
            for (int step = 0; step < MAX_STEPS_PER_RUN; step++) {
                GameSnapshot before = session.snapshot();
                if (before.phase() != RunPhase.RUNNING) {
                    end = before.phase();
                    peak = Math.max(peak, before.wave());
                    break;
                }
                session.catchUpElapsed(TimeUnit.SECONDS.toNanos(STEP_SECONDS));
                GameSnapshot after = session.snapshot();
                peak = Math.max(peak, after.wave());
                if (after.phase() != RunPhase.RUNNING
                        || after.runsCompleted() > lastPrestiges
                        || after.wave() < before.wave()) {
                    end = after.phase() == RunPhase.RUNNING ? RunPhase.IDLE : after.phase();
                    lastPrestiges = after.runsCompleted();
                    break;
                }
            }
            if (session.snapshot().phase() == RunPhase.DEAD) {
                session.apply(new GameCommand.StartRun());
            }
            spendMeta(session);
            GameSnapshot snap = session.snapshot();
            lastPrestiges = snap.runsCompleted();
            if (snap.highestTierUnlocked() > snap.tier()) {
                session.apply(new GameCommand.SelectTier(snap.highestTierUnlocked()));
                lastPrestiges = session.snapshot().runsCompleted();
                spendMeta(session);
            }
            lastBest = Math.max(lastBest, peak);
            rows.add(String.format(Locale.ROOT,
                    "run %2d  T%d peak %4d  end %-7s  open %d  shards %s  cores %s  best %d",
                    run, startTier, peak, end, snap.highestTierUnlocked(),
                    compact(snap.shards()), compact(snap.cores()), snap.bestWave()));
        }

        System.out.println("early-game probe (auto-buy, prestige at 25% hp, climb when unlocked)");
        for (String row : rows) {
            System.out.println(row);
        }

        GameSnapshot finalSnap = session.snapshot();
        assertTrue(hpGrowth > damageGrowth, "a single run must still hit a wall");
        assertTrue(lastBest < balance.wavesPerTier,
                "early prestige farming already reached the 5000 cap");
        assertTrue(finalSnap.bestWave() < 400,
                "twelve smart runs already sit at wave " + finalSnap.bestWave() + " — too fast");
        assertTrue(balance.unlockWaveOn(20) == balance.unlockCap, "unlock must stop growing");
    }

    /** A real player dumps leftover shards into the meta tree instead of hoarding them. */
    private static void spendMeta(LocalGameSession session) {
        for (int i = 0; i < 24; i++) {
            GameSnapshot snap = session.snapshot();
            UpgradeView foundation = snap.meta().getFirst();
            UpgradeView fortune = snap.meta().get(1);
            UpgradeView pick = foundation.cost() <= fortune.cost() ? foundation : fortune;
            if (!pick.affordable()) {
                break;
            }
            session.apply(new GameCommand.BuyUpgrade(pick.id()));
        }
        for (int i = 0; i < 12; i++) {
            List<UpgradeView> permanents = session.snapshot().permanents();
            UpgradeView pick = null;
            for (UpgradeView view : permanents) {
                if (view.affordable() && (pick == null || view.cost() < pick.cost())) {
                    pick = view;
                }
            }
            if (pick == null) {
                return;
            }
            session.apply(new GameCommand.BuyUpgrade(pick.id()));
        }
    }

    private static String compact(double value) {
        if (value < 10) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
        if (value < 1000) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        return String.format(Locale.ROOT, "%.1e", value);
    }
}
