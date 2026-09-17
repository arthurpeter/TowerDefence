package com.towerdefence.engine.session;

import com.towerdefence.engine.command.GameCommand;
import com.towerdefence.engine.command.UpgradeId;
import com.towerdefence.engine.config.GameBalance;
import com.towerdefence.engine.model.AbilityId;
import com.towerdefence.engine.model.LabId;
import com.towerdefence.engine.model.MissionId;
import com.towerdefence.engine.model.RunPhase;
import com.towerdefence.engine.sim.EnemyKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalGameSessionTest {

    @Test
    void startRunSpawnsEnemiesAfterAShortTick() {
        LocalGameSession session = new LocalGameSession();
        session.apply(new GameCommand.StartRun());
        session.tick(0);
        session.tick(TimeUnit.MILLISECONDS.toNanos(1500));

        GameSnapshot snap = session.snapshot();
        assertEquals(RunPhase.RUNNING, snap.phase());
        assertFalse(snap.enemies().isEmpty());
        assertEquals(1, snap.wave());
    }

    @Test
    void buyingPowerSpendsCoinsAndRaisesDamage() {
        GameBalance balance = GameBalance.standard().copy();
        balance.startingCoins = 1_000;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        GameSnapshot before = session.snapshot();

        session.apply(new GameCommand.BuyUpgrade(UpgradeId.POWER));
        GameSnapshot after = session.snapshot();

        assertTrue(after.damage() > before.damage());
        assertEquals(1, levelOf(after, UpgradeId.POWER));
        assertTrue(after.coins() < before.coins());
    }

    @Test
    void towerFiresProjectilesThatTravelBeforeHitting() {
        GameBalance balance = GameBalance.standard().copy();
        balance.projectileSpeed = 0.35;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.tick(0);
        session.tick(TimeUnit.MILLISECONDS.toNanos(6000));

        GameSnapshot snap = session.snapshot();
        assertFalse(snap.projectiles().isEmpty(), "slow bullets should still be in flight");
        for (ProjectileView projectile : snap.projectiles()) {
            assertTrue(projectile.pathT() <= 1.0 && projectile.pathT() >= 0.0);
        }
    }

    @Test
    void projectilesKillEnemiesAndPayCoins() {
        LocalGameSession session = new LocalGameSession();
        session.apply(new GameCommand.StartRun());
        double start = session.snapshot().coins();
        session.tick(0);
        session.tick(TimeUnit.SECONDS.toNanos(12));

        assertTrue(session.snapshot().coins() > start, "kills should pay out");
    }

    @Test
    void towerRegeneratesAfterTakingDamage() {
        GameBalance balance = GameBalance.standard().copy();
        balance.baseDamage = 0;
        balance.regenFractionPerSecond = 0.02;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.tick(0);
        session.tick(TimeUnit.SECONDS.toNanos(14));
        double damaged = session.snapshot().towerHp();
        assertTrue(damaged < session.snapshot().towerMaxHp(), "leaks should hurt");

        balance.leakDamageBase = 0;
        balance.leakDamageFraction = 0;
        session.catchUpElapsed(TimeUnit.SECONDS.toNanos(5));
        assertTrue(session.snapshot().towerHp() > damaged, "hp should tick back up");
    }

    @Test
    void enemiesThatReachTheTowerCanEndTheRun() {
        GameBalance balance = GameBalance.standard().copy();
        balance.baseTowerHp = 20;
        balance.baseDamage = 0;
        balance.regenFractionPerSecond = 0;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.tick(0);
        session.tick(TimeUnit.SECONDS.toNanos(40));

        assertEquals(RunPhase.DEAD, session.snapshot().phase());
        assertEquals(0, session.snapshot().shards(), "dying must not bank shards");
        session.apply(new GameCommand.StartRun());
        assertEquals(0, session.snapshot().shards(), "retrying a lost run still pays nothing");
    }

    @Test
    void autoBuySpendsCoinsOnTheCheapestRunUpgrade() {
        GameBalance balance = GameBalance.standard().copy();
        balance.startingCoins = 500;
        balance.autoUnlockShards = 0;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO));
        session.tick(0);
        session.tick(TimeUnit.MILLISECONDS.toNanos(50));

        GameSnapshot snap = session.snapshot();
        assertTrue(snap.autoBuy());
        int runLevels = runUpgradeLevels(snap);
        assertTrue(runLevels >= 1);
        assertTrue(snap.coins() < 500);
    }

    @Test
    void autoBuyerOnlySpendsOnStatsYouLeftEnabled() {
        GameBalance balance = GameBalance.standard().copy();
        balance.startingCoins = 5_000;
        balance.autoUnlockShards = 0;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO));
        session.apply(new GameCommand.SetAutoTarget(UpgradeId.TEMPO, false));
        session.apply(new GameCommand.SetAutoTarget(UpgradeId.REACH, false));
        session.tick(0);
        session.catchUpElapsed(TimeUnit.SECONDS.toNanos(2));

        GameSnapshot snap = session.snapshot();
        assertEquals(0, levelOf(snap, UpgradeId.TEMPO), "TEMPO was switched off");
        assertEquals(0, levelOf(snap, UpgradeId.REACH), "REACH was switched off");
        assertTrue(levelOf(snap, UpgradeId.POWER) > 0, "POWER was the only target left");
    }

    @Test
    void autoTargetsSurviveASaveRoundTrip() {
        GameBalance balance = GameBalance.standard();
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.SetAutoTarget(UpgradeId.POWER, false));

        String encoded = SaveCodec.encode(session.toSave(1_000L));
        LocalGameSession restored = LocalGameSession.fromSave(balance, SaveCodec.decode(encoded));

        UpgradeView power = restored.snapshot().attack().getFirst();
        assertEquals(UpgradeId.POWER, power.id());
        assertFalse(power.autoTarget());
    }

    @Test
    void offlineWindowStartsShortAndDoubles() {
        GameBalance balance = GameBalance.standard();
        assertEquals(0.5, balance.offlineHours(0), 0.001, "early game should be played, not waited out");
        assertEquals(1.0, balance.offlineHours(1), 0.001);
        assertEquals(4.0, balance.offlineHours(3), 0.001);
        assertEquals(balance.offlineHoursMax, balance.offlineHours(20), 0.001, "and capped");
    }

    @Test
    void sigilsAreRareAndScaleWithTier() {
        GameBalance balance = GameBalance.standard();
        assertEquals(0, balance.sigilsFor(1, 24));
        assertEquals(1, balance.sigilsFor(1, 25));
        assertEquals(2, balance.sigilsFor(1, 50));
        assertEquals(6, balance.sigilsFor(3, 50), "tier 3 pays three per milestone");
        assertTrue(balance.sigilsFor(1, 100) < balance.coresFor(1, 100), "sigils must stay scarcer than cores");
    }

    @Test
    void autoPrestigeIsIgnoredUntilUnlocked() {
        GameBalance balance = GameBalance.standard().copy();
        balance.autoPrestigeCost = 0;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());

        session.apply(new GameCommand.SetAutoPrestige(new AutoPrestigeRule(true, 5, 0)));
        assertFalse(session.snapshot().autoPrestige().enabled(), "rule needs the sigil unlock first");

        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO_PRESTIGE));
        session.apply(new GameCommand.SetAutoPrestige(new AutoPrestigeRule(true, 5, 0)));
        assertTrue(session.snapshot().autoPrestige().enabled());
    }

    @Test
    void autoPrestigeRestartsTheRunAtTheWaveTarget() {
        GameBalance balance = GameBalance.standard().copy();
        balance.autoPrestigeCost = 0;
        balance.autoUnlockShards = 0;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO_PRESTIGE));
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO));
        session.apply(new GameCommand.SetAutoPrestige(new AutoPrestigeRule(true, 4, 0)));
        session.tick(0);

        session.catchUpElapsed(TimeUnit.MINUTES.toNanos(4));

        GameSnapshot snap = session.snapshot();
        assertTrue(snap.runsCompleted() >= 2, "only banked " + snap.runsCompleted() + " runs");
        assertTrue(snap.wave() <= 4, "should keep resetting near the target, sat at wave " + snap.wave());
        assertTrue(snap.shards() > 0, "each banked run should pay shards");
    }

    @Test
    void autoPrestigeAlsoFiresWhenARunStalls() {
        GameBalance balance = GameBalance.standard().copy();
        balance.autoPrestigeCost = 0;
        // Enemies that never move and never die: the run cannot progress on its own.
        balance.enemySpeedBase = 0;
        balance.enemySpeedPerWave = 0;
        balance.baseDamage = 0;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO_PRESTIGE));
        session.apply(new GameCommand.SetAutoPrestige(new AutoPrestigeRule(true, 0, 10)));
        session.tick(0);

        session.catchUpElapsed(TimeUnit.SECONDS.toNanos(45));

        assertTrue(session.snapshot().runsCompleted() >= 3,
                "a stalled run should bank and restart, got " + session.snapshot().runsCompleted());
    }

    @Test
    void autoPrestigeRuleSurvivesASaveRoundTrip() {
        GameBalance balance = GameBalance.standard().copy();
        balance.autoPrestigeCost = 0;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO_PRESTIGE));
        session.apply(new GameCommand.SetAutoPrestige(new AutoPrestigeRule(true, 30, 120)));

        String encoded = SaveCodec.encode(session.toSave(1_000L));
        AutoPrestigeRule restored = LocalGameSession.fromSave(balance, SaveCodec.decode(encoded))
                .snapshot().autoPrestige();

        assertTrue(restored.enabled());
        assertEquals(30, restored.waveTarget());
        assertEquals(120, restored.stallSeconds(), 0.001);
        assertEquals(0, restored.hpPercent(), 0.001);
    }

    @Test
    void killsPayShardsAndPrestigeDoesNot() {
        GameBalance balance = GameBalance.standard().copy();
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.tick(0);
        session.catchUpElapsed(TimeUnit.SECONDS.toNanos(25));
        double farmed = session.snapshot().shards();
        assertTrue(farmed > 0, "kills should already have paid shards");
        session.apply(new GameCommand.Prestige());
        assertEquals(farmed, session.snapshot().shards(), 0.001, "prestige must not dump more shards");
        assertTrue(session.snapshot().stars() >= 1, "prestige should pay star points");
        assertEquals(RunPhase.RUNNING, session.snapshot().phase());
    }

    @Test
    void starBonusesResetWithTheRunAndDeathPaysNothing() {
        GameBalance balance = GameBalance.standard().copy();
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.tick(0);
        session.catchUpElapsed(TimeUnit.SECONDS.toNanos(8));
        int payout = session.snapshot().pendingStars();
        session.apply(new GameCommand.Prestige());
        assertEquals(payout, session.snapshot().stars(), 0.001);
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.STAR_DAMAGE));
        assertEquals(1, session.snapshot().prestige().getFirst().level());
        assertEquals(0, session.snapshot().stars(), 0.001);

        session.apply(new GameCommand.Prestige());
        assertEquals(0, session.snapshot().prestige().getFirst().level(),
                "x1.02 bonuses must die with the run");
        assertTrue(session.snapshot().stars() >= 1);

        GameBalance lethal = GameBalance.standard().copy();
        lethal.baseDamage = 0;
        lethal.regenFractionPerSecond = 0;
        lethal.leakDamageBase = 1_000_000;
        LocalGameSession dying = new LocalGameSession(lethal);
        dying.apply(new GameCommand.StartRun());
        dying.apply(new GameCommand.Prestige());
        dying.apply(new GameCommand.BuyUpgrade(UpgradeId.STAR_DAMAGE));
        assertEquals(1, dying.snapshot().prestige().getFirst().level());
        dying.tick(0);
        dying.catchUpElapsed(TimeUnit.SECONDS.toNanos(45));
        assertEquals(RunPhase.DEAD, dying.snapshot().phase());
        assertEquals(0, dying.snapshot().stars(), 0.001);
        assertEquals(0, dying.snapshot().prestige().getFirst().level());
        dying.apply(new GameCommand.StartRun());
        assertEquals(0, dying.snapshot().stars(), 0.001);
        assertEquals(0, dying.snapshot().prestige().getFirst().level());
    }

    @Test
    void autoRunNeedsBuyAndPrestigeThenResumesAfterDeath() {
        GameBalance balance = GameBalance.standard().copy();
        balance.autoUnlockShards = 0;
        balance.autoPrestigeCost = 0;
        balance.autoRunCost = 0;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO_RUN));
        assertFalse(session.snapshot().autoRunUnlocked(), "AUTO RUN is gated behind the cheaper automations");

        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO));
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO_PRESTIGE));
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO_RUN));
        assertTrue(session.snapshot().autoRunUnlocked());
        assertTrue(session.snapshot().autoRun());

        GameBalance idle = GameBalance.standard().copy();
        idle.autoUnlockShards = 0;
        idle.autoPrestigeCost = 0;
        idle.autoRunCost = 0;
        LocalGameSession parked = new LocalGameSession(idle);
        parked.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO));
        parked.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO_PRESTIGE));
        parked.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO_RUN));
        assertEquals(RunPhase.IDLE, parked.snapshot().phase());
        parked.tick(0);
        parked.catchUpElapsed(TimeUnit.SECONDS.toNanos(1));
        assertEquals(RunPhase.RUNNING, parked.snapshot().phase(), "auto-run should start from the menu");
    }

    @Test
    void autoPrestigeCanCashOutOnLowHp() {
        GameBalance balance = GameBalance.standard().copy();
        balance.autoPrestigeCost = 0;
        balance.baseDamage = 0;
        balance.regenFractionPerSecond = 0;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO_PRESTIGE));
        session.apply(new GameCommand.SetAutoPrestige(new AutoPrestigeRule(true, 0, 0, 80, "")));
        session.tick(0);
        session.catchUpElapsed(TimeUnit.SECONDS.toNanos(20));

        GameSnapshot snap = session.snapshot();
        assertEquals(RunPhase.RUNNING, snap.phase(), "auto-prestige restarts instead of dying");
        assertTrue(snap.runsCompleted() >= 1, "the threatened run should have been banked");
        assertEquals(80, snap.autoPrestige().hpPercent(), 0.001);
    }

    @Test
    void finishingTheWaveCapClearsTheTier() {
        GameBalance balance = GameBalance.standard().copy();
        balance.wavesPerTier = 4;
        balance.enemyHpBase = 1;
        balance.baseDamage = 400;
        balance.projectileSpeed = 8;
        balance.leakDamageFraction = 0;
        balance.leakDamageBase = 0;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.tick(0);
        session.catchUpElapsed(TimeUnit.MINUTES.toNanos(2));

        GameSnapshot snap = session.snapshot();
        assertEquals(RunPhase.CLEARED, snap.phase(), "sat at " + snap.phase() + " wave " + snap.wave());
        assertTrue(snap.shards() > 0, "kills on the clear should have farmed shards");
        assertTrue(snap.crests() > 0, "a clear and its mission pay crests");
        assertTrue(snap.tiers().getFirst().cleared());
        double shards = snap.shards();
        session.apply(new GameCommand.StartRun());
        assertEquals(RunPhase.RUNNING, session.snapshot().phase());
        assertEquals(shards, session.snapshot().shards(), 0.001, "starting after a clear must not pay again");
    }

    @Test
    void missionsPayCrestsAndNeverComeFromWavesAlone() {
        GameBalance balance = GameBalance.standard().copy();
        balance.autoUnlockShards = 0;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        assertEquals(0, session.snapshot().crests(), "fresh save has no mission rewards");
        session.apply(new GameCommand.Prestige());

        GameSnapshot snap = session.snapshot();
        assertTrue(missionOf(snap, MissionId.PRESTIGE_1).completed());
        assertTrue(snap.crests() >= MissionId.PRESTIGE_1.crests());
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO));
        assertTrue(missionOf(session.snapshot(), MissionId.UNLOCK_AUTO).completed());
    }

    @Test
    void everyRunPaysSomeShards() {
        GameBalance balance = GameBalance.standard();
        assertTrue(balance.shardsForKill(10, 0) > 0);
        assertTrue(balance.shardsForKill(100, 0) > balance.shardsForKill(10, 0), "fatter enemies farm more");
        assertTrue(balance.shardsForKill(100, 3) > balance.shardsForKill(100, 0), "harvest raises the farm");
        assertEquals(0, balance.coresFor(1, 9), "cores stay locked to records");
        assertEquals(1, balance.coresFor(1, 10));
        assertEquals(0, balance.pendingShards(40, 1), "prestige is not a shard faucet");
    }

    @Test
    void coresArriveEveryTenWavesAndScaleWithTier() {
        GameBalance balance = GameBalance.standard();
        assertEquals(0, balance.coresFor(1, 9));
        assertEquals(1, balance.coresFor(1, 10));
        assertEquals(3, balance.coresFor(1, 31));
        // Tier 3 hands out three cores per ten-wave milestone.
        assertEquals(6, balance.coresFor(3, 21));
    }

    @Test
    void higherTiersHitHarderThanTheyPay() {
        GameBalance balance = GameBalance.standard();
        assertTrue(balance.enemyHp(10, 2) > balance.enemyHp(10, 1));
        assertTrue(balance.coinsForKill(10, 2, 0, 0) > balance.coinsForKill(10, 1, 0, 0));
        double hpJump = balance.tierHpMultiplier(2);
        double coinJump = balance.tierCoinMultiplier(2);
        assertTrue(hpJump > coinJump, "a tier must be a real step up, not free money");
    }

    @Test
    void tiersUnlockOnlyAfterReachingTheThresholdInThePreviousTier() {
        LocalGameSession session = new LocalGameSession();
        session.apply(new GameCommand.StartRun());
        assertEquals(1, session.snapshot().highestTierUnlocked());

        session.apply(new GameCommand.SelectTier(2));
        assertEquals(1, session.snapshot().tier(), "tier 2 should still be locked");
    }

    @Test
    void eliteWavesCloseEveryTenthWave() {
        GameBalance balance = GameBalance.standard();
        assertTrue(balance.isEliteWave(10));
        assertFalse(balance.isEliteWave(11));
    }

    @Test
    void enemyKindsArriveOnSchedule() {
        GameBalance balance = GameBalance.standard();
        int size = balance.waveSize(1);
        for (int i = 0; i < size; i++) {
            assertEquals(EnemyKind.BASIC, balance.kindFor(1, i, size), "wave 1 should be plain walkers");
        }
        assertEquals(EnemyKind.FAST, balance.kindFor(balance.fastFromWave, 2, 12));
        assertEquals(EnemyKind.TANK, balance.kindFor(balance.tankFromWave, 4, 12));
        assertEquals(EnemyKind.RANGED, balance.kindFor(balance.rangedFromWave, 5, 12));
        assertEquals(EnemyKind.PROTECTOR, balance.kindFor(balance.protectorFromWave, 6, 12));
        // The boss always closes a milestone wave.
        assertEquals(EnemyKind.BOSS, balance.kindFor(10, 8, 9));
    }

    @Test
    void rangedEnemiesShellTheTowerInsteadOfLeaking() {
        GameBalance balance = GameBalance.standard().copy();
        balance.baseDamage = 0;
        balance.regenFractionPerSecond = 0;
        // Survive long enough to watch, and let snipers show up from wave one.
        balance.baseTowerHp = 5_000_000;
        balance.leakDamageFraction = 0;
        balance.leakDamageBase = 0;
        balance.fastFromWave = 999;
        balance.tankFromWave = 999;
        balance.protectorFromWave = 999;
        balance.rangedFromWave = 1;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.tick(0);
        // A sniper is the sixth spawn of a wave, so waves have to grow first.
        session.catchUpElapsed(TimeUnit.SECONDS.toNanos(90));

        GameSnapshot snap = session.snapshot();
        boolean parked = snap.enemies().stream()
                .anyMatch(e -> e.kind() == EnemyKind.RANGED && e.pathT() >= balance.rangedStopT);
        assertTrue(parked, "a sniper should stop short of the tower at wave " + snap.wave());
        assertTrue(snap.towerHp() < snap.towerMaxHp(), "snipers should be chipping the tower");
    }

    @Test
    void protectorsShieldEveryOtherEnemy() {
        GameBalance balance = GameBalance.standard();
        assertEquals(1.0, balance.protectorShield(0), 0.001);
        assertTrue(balance.protectorShield(2) > balance.protectorShield(1));
    }

    @Test
    void theWallSoaksDamageBeforeTheTower() {
        GameBalance balance = GameBalance.standard().copy();
        balance.startingCoins = 10_000;
        balance.baseDamage = 0;
        balance.regenFractionPerSecond = 0;
        balance.wallRegenBase = 0;
        balance.wallRegenPerLevel = 0;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.WALL));
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.WALL));

        GameSnapshot before = session.snapshot();
        assertTrue(before.wall() > 0, "buying WALL should raise the wall immediately");
        assertEquals(before.wallMax(), before.wall(), 0.001);

        session.tick(0);
        session.catchUpElapsed(TimeUnit.SECONDS.toNanos(14));

        GameSnapshot after = session.snapshot();
        assertTrue(after.wall() < before.wall(), "leaks should eat the wall first");
        assertEquals(before.towerHp(), after.towerHp(), 0.001, "tower hp stays untouched while wall holds");
    }

    @Test
    void multishotAndCritAreCappedMultipliers() {
        GameBalance balance = GameBalance.standard();
        assertEquals(1, balance.multishotCount(0));
        assertEquals(balance.multishotMax, balance.multishotCount(99), "multishot must stay capped");
        assertEquals(0, balance.critChance(0), 0.001, "no crits at level zero");
        assertEquals(balance.critChanceCap, balance.critChance(99), 0.001, "crit chance must stay capped");
        assertEquals(balance.bounceMax, balance.bounceCount(99), "bounces must stay capped");
    }

    @Test
    void multishotPutsSeveralBulletsInTheAir() {
        GameBalance balance = GameBalance.standard().copy();
        balance.startingCoins = 100_000;
        balance.projectileSpeed = 0.2;
        balance.baseDamage = 0;
        // Everything in range and barely moving, so there are always several targets to spread over.
        balance.baseRange = 0.99;
        balance.enemySpeedBase = 0.02;
        balance.enemySpeedPerWave = 0;
        balance.baseTowerHp = 5_000_000;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.tick(0);
        session.catchUpElapsed(TimeUnit.SECONDS.toNanos(12));
        int single = session.snapshot().projectiles().size();

        session.apply(new GameCommand.BuyUpgrade(UpgradeId.MULTISHOT));
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.MULTISHOT));
        session.catchUpElapsed(TimeUnit.SECONDS.toNanos(12));

        GameSnapshot snap = session.snapshot();
        assertEquals(3, snap.multishot());
        assertTrue(snap.projectiles().size() > single,
                "multishot should thicken the stream: " + snap.projectiles().size() + " vs " + single);
    }

    @Test
    void thornsChewEnemiesStandingOnTheTower() {
        GameBalance balance = GameBalance.standard().copy();
        balance.startingCoins = 100_000;
        balance.baseDamage = 0;
        balance.thornsDpsPerLevel = 400;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.THORNS));
        double start = session.snapshot().coins();
        session.tick(0);
        session.catchUpElapsed(TimeUnit.SECONDS.toNanos(20));

        assertTrue(session.snapshot().coins() > start, "spikes should be killing and paying out");
    }

    @Test
    void permanentUpgradesCostCoresAndSurvivePrestige() {
        GameBalance balance = GameBalance.standard().copy();
        balance.autoUnlockShards = 0;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO));
        session.tick(0);
        // Run long enough to clear the first core milestone.
        session.catchUpElapsed(TimeUnit.MINUTES.toNanos(12));

        GameSnapshot earned = session.snapshot();
        assertTrue(earned.cores() >= 1, "expected a core by wave " + earned.bestWave());

        session.apply(new GameCommand.BuyUpgrade(UpgradeId.CORE_DAMAGE));
        int level = levelOf(session.snapshot(), UpgradeId.CORE_DAMAGE);
        assertEquals(1, level);

        session.apply(new GameCommand.Prestige());
        assertEquals(1, levelOf(session.snapshot(), UpgradeId.CORE_DAMAGE), "permanents must not reset");
    }

    @Test
    void autoBuyCarriesAnUnattendedRunWellPastWaveSix() {
        GameBalance balance = GameBalance.standard().copy();
        balance.autoUnlockShards = 0;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO));
        session.tick(0);
        session.catchUpElapsed(TimeUnit.MINUTES.toNanos(10));

        GameSnapshot snap = session.snapshot();
        assertTrue(snap.bestWave() >= 12, "10 idle minutes reached only wave " + snap.bestWave());
    }

    /**
     * The whole idle loop depends on runs ending. Buyable damage scales like
     * coinPerWave^(log(powerPerLevel)/log(powerCostGrowth)); if enemy health does not grow faster
     * than that, the tower outruns the waves forever and there is no wall to prestige against.
     */
    @Test
    void enemyHealthOutgrowsBuyableDamage() {
        GameBalance balance = GameBalance.standard();
        double damageGrowth = Math.pow(
                1 + balance.coinPerWave,
                Math.log(1 + balance.powerPerLevel) / Math.log(1 + balance.powerCostGrowth)
        );
        double hpGrowth = 1 + balance.enemyHpGrowth;
        assertTrue(hpGrowth > damageGrowth,
                "runs would never end: hp x" + hpGrowth + " vs damage x" + damageGrowth + " per wave");
    }

    @Test
    void anUnattendedRunEventuallyEnds() {
        GameBalance balance = GameBalance.standard().copy();
        balance.autoUnlockShards = 0;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO));
        session.tick(0);

        for (int i = 0; i < 80 && session.snapshot().phase() == RunPhase.RUNNING; i++) {
            session.catchUpElapsed(TimeUnit.SECONDS.toNanos(30));
        }

        assertEquals(RunPhase.DEAD, session.snapshot().phase(),
                "still alive at wave " + session.snapshot().wave() + " after 40 idle minutes");
    }

    @Test
    void laterTiersAskForADeeperWaveOnTheFloorYouArePlaying() {
        GameBalance balance = GameBalance.standard();
        assertEquals(68, balance.unlockWaveOn(1));
        assertTrue(balance.unlockWaveOn(2) > balance.unlockWaveOn(1));
        assertTrue(balance.unlockWaveOn(4) > balance.unlockWaveOn(2));
        assertEquals(balance.unlockCap, balance.unlockWaveOn(12));
        assertEquals(balance.unlockWaveOn(1), balance.waveToUnlockTier(2));
        assertEquals(0, balance.waveToUnlockTier(1));
    }

    @Test
    void playingOnlyTierOneNeverOpensTierThree() {
        GameBalance balance = GameBalance.standard().copy();
        balance.autoUnlockShards = 0;
        balance.autoPrestigeCost = 0;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO));
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO_PRESTIGE));
        session.apply(new GameCommand.SetAutoPrestige(new AutoPrestigeRule(true, 0, 0, 25, "")));
        session.tick(0);

        for (int i = 0; i < 80; i++) {
            session.catchUpElapsed(TimeUnit.SECONDS.toNanos(15));
            GameSnapshot mid = session.snapshot();
            for (UpgradeView view : mid.meta()) {
                if (view.id() != UpgradeId.AUTO && view.affordable()) {
                    session.apply(new GameCommand.BuyUpgrade(view.id()));
                }
            }
            if (session.snapshot().runsCompleted() >= 3 && session.snapshot().highestTierUnlocked() >= 2) {
                break;
            }
        }

        GameSnapshot snap = session.snapshot();
        assertEquals(0, snap.tiers().get(1).bestWave(), "T2 was never played");
        assertTrue(snap.highestTierUnlocked() <= 2, "a T1-only save opened T" + snap.highestTierUnlocked());
    }

    @Test
    void aFirstUnattendedRunDoesNotUnlockTheNextTier() {
        GameBalance balance = GameBalance.standard().copy();
        balance.autoUnlockShards = 0;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO));
        session.tick(0);

        for (int i = 0; i < 80 && session.snapshot().phase() == RunPhase.RUNNING; i++) {
            session.catchUpElapsed(TimeUnit.SECONDS.toNanos(30));
        }

        GameSnapshot snap = session.snapshot();
        assertEquals(RunPhase.DEAD, snap.phase(), "first run should still hit a wall");
        assertTrue(snap.bestWave() < balance.unlockWaveOn(1),
                "first run already reached wave " + snap.bestWave() + ", unlock is too early");
        assertEquals(1, snap.highestTierUnlocked());
        assertTrue(snap.cores() >= 1, "a long first run should still mint a core");
    }

    @Test
    void abilitiesStayLockedUntilBoughtThenGoOnCooldown() {
        GameBalance balance = GameBalance.standard().copy();
        balance.abilityCostBase = 0;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());

        assertFalse(abilityOf(session.snapshot(), AbilityId.GOLDEN_TOWER).unlocked());
        session.apply(new GameCommand.CastAbility(AbilityId.GOLDEN_TOWER));
        assertEquals(0d, session.snapshot().goldenRemaining(), 0.001, "a locked ability must do nothing");

        session.apply(new GameCommand.BuyUpgrade(UpgradeId.ABILITY_GOLDEN));
        AbilityView ready = abilityOf(session.snapshot(), AbilityId.GOLDEN_TOWER);
        assertTrue(ready.unlocked());
        assertTrue(ready.ready());

        session.apply(new GameCommand.CastAbility(AbilityId.GOLDEN_TOWER));
        GameSnapshot cast = session.snapshot();
        assertTrue(cast.goldenRemaining() > 0, "golden tower should be running");
        AbilityView cooling = abilityOf(cast, AbilityId.GOLDEN_TOWER);
        assertTrue(cooling.cooldownRemaining() > 0);
        assertFalse(cooling.ready());
    }

    @Test
    void goldenTowerMultipliesCoinIncomeWhileActive() {
        GameBalance balance = GameBalance.standard().copy();
        balance.abilityCostBase = 0;
        // Two identical runs; only one of them casts. Anything else would compare unequal windows.
        double plainGain = coinsOverWindow(balance, false);
        double goldenGain = coinsOverWindow(balance, true);

        assertTrue(goldenGain > plainGain * 1.5,
                "golden tower should visibly boost income: " + goldenGain + " vs " + plainGain);
    }

    private static double coinsOverWindow(GameBalance balance, boolean castGolden) {
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.ABILITY_GOLDEN));
        session.tick(0);
        session.catchUpElapsed(TimeUnit.SECONDS.toNanos(20));

        double before = session.snapshot().coins();
        if (castGolden) {
            session.apply(new GameCommand.CastAbility(AbilityId.GOLDEN_TOWER));
        }
        session.catchUpElapsed(TimeUnit.SECONDS.toNanos(8));
        return session.snapshot().coins() - before;
    }

    @Test
    void deathWaveHitsEverythingOnTheLane() {
        GameBalance balance = GameBalance.standard().copy();
        balance.abilityCostBase = 0;
        balance.baseDamage = 0;
        balance.deathWaveDamageBase = 0;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.ABILITY_DEATH_WAVE));
        session.tick(0);
        session.catchUpElapsed(TimeUnit.SECONDS.toNanos(4));
        int before = session.snapshot().enemies().size();
        assertTrue(before > 0, "need something on the lane");

        // Give the wave real damage, then fire it.
        balance.baseDamage = 1_000_000;
        balance.deathWaveDamageBase = 10;
        session.apply(new GameCommand.CastAbility(AbilityId.DEATH_WAVE));

        assertEquals(0, session.snapshot().enemies().size(), "death wave should clear the lane");
    }

    @Test
    void blackHoleDragsEnemiesBackDownTheLane() {
        GameBalance balance = GameBalance.standard().copy();
        balance.abilityCostBase = 0;
        balance.baseDamage = 0;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.ABILITY_BLACK_HOLE));
        session.tick(0);
        session.catchUpElapsed(TimeUnit.SECONDS.toNanos(6));

        double furthestBefore = session.snapshot().enemies().stream()
                .mapToDouble(EnemyView::pathT).max().orElse(0d);
        session.apply(new GameCommand.CastAbility(AbilityId.BLACK_HOLE));
        double furthestAfter = session.snapshot().enemies().stream()
                .mapToDouble(EnemyView::pathT).max().orElse(0d);

        assertTrue(furthestAfter < furthestBefore, "black hole should pull enemies back");
        assertTrue(session.snapshot().blackHoleRemaining() > 0);
    }

    @Test
    void autoCastFiresAbilitiesOnceUnlocked() {
        GameBalance balance = GameBalance.standard().copy();
        balance.abilityCostBase = 0;
        balance.autoCastCost = 0;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.ABILITY_GOLDEN));
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO_CAST));
        assertTrue(session.snapshot().autoCast());

        session.tick(0);
        session.catchUpElapsed(TimeUnit.SECONDS.toNanos(2));

        assertTrue(session.snapshot().goldenRemaining() > 0, "auto cast should have fired golden tower");
    }

    @Test
    void gameSpeedStretchesSimulatedTime() {
        GameBalance balance = GameBalance.standard().copy();
        balance.gameSpeedCostBase = 0;
        LocalGameSession slow = new LocalGameSession(balance);
        slow.apply(new GameCommand.StartRun());
        slow.tick(0);
        slow.catchUpElapsed(TimeUnit.SECONDS.toNanos(60));

        LocalGameSession fast = new LocalGameSession(balance);
        fast.apply(new GameCommand.StartRun());
        fast.apply(new GameCommand.BuyUpgrade(UpgradeId.GAME_SPEED));
        fast.apply(new GameCommand.BuyUpgrade(UpgradeId.GAME_SPEED));
        fast.tick(0);
        fast.catchUpElapsed(TimeUnit.SECONDS.toNanos(60));

        assertEquals(2.0, fast.snapshot().gameSpeed(), 0.001);
        assertTrue(fast.snapshot().wave() > slow.snapshot().wave(),
                "double speed should be further along: " + fast.snapshot().wave()
                        + " vs " + slow.snapshot().wave());
    }

    @Test
    void labsRunOnTheRealClockAndRaiseTheirStatWhenDone() {
        GameBalance balance = GameBalance.standard().copy();
        balance.labCostBase = 0;
        LocalGameSession session = new LocalGameSession(balance);
        session.wallClock(1_000_000L);
        session.apply(new GameCommand.StartRun());

        double damageBefore = session.snapshot().damage();
        session.apply(new GameCommand.StartLab(LabId.LAB_DAMAGE));
        LabView running = labOf(session.snapshot(), LabId.LAB_DAMAGE);
        assertTrue(running.running());
        assertTrue(running.secondsRemaining() > 0);

        // Another lab cannot start while one is going.
        session.apply(new GameCommand.StartLab(LabId.LAB_COINS));
        assertFalse(labOf(session.snapshot(), LabId.LAB_COINS).running());

        // Jump the wall clock past the finish line.
        long minutes = (long) balance.labMinutes(LabId.LAB_DAMAGE, 0);
        session.wallClock(1_000_000L + minutes * 60_000L + 1);

        assertEquals(1, labOf(session.snapshot(), LabId.LAB_DAMAGE).level());
        assertFalse(labOf(session.snapshot(), LabId.LAB_DAMAGE).running());
        assertTrue(session.snapshot().damage() > damageBefore, "the finished lab should raise damage");
    }

    @Test
    void autoPrestigeCanRunOffAWrittenFormula() {
        GameBalance balance = GameBalance.standard().copy();
        balance.autoPrestigeCost = 0;
        balance.formulaCost = 0;
        balance.autoUnlockShards = 0;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO_PRESTIGE));
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.FORMULA));
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO));
        session.apply(new GameCommand.SetAutoPrestige(
                new AutoPrestigeRule(true, 0, 0, "wave >= 4")));
        assertNull(session.snapshot().formulaError());

        session.tick(0);
        session.catchUpElapsed(TimeUnit.MINUTES.toNanos(4));

        GameSnapshot snap = session.snapshot();
        assertTrue(snap.runsCompleted() >= 2, "only banked " + snap.runsCompleted() + " runs");
        assertTrue(snap.wave() <= 4, "sat at wave " + snap.wave());
    }

    @Test
    void abrokenFormulaIsReportedAndNeverFires() {
        GameBalance balance = GameBalance.standard().copy();
        balance.autoPrestigeCost = 0;
        balance.formulaCost = 0;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.AUTO_PRESTIGE));
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.FORMULA));
        session.apply(new GameCommand.SetAutoPrestige(
                new AutoPrestigeRule(true, 0, 0, "wave >>> 4")));

        assertNotNull(session.snapshot().formulaError());
        int runsBefore = session.snapshot().runsCompleted();
        session.tick(0);
        session.catchUpElapsed(TimeUnit.MINUTES.toNanos(2));
        assertEquals(runsBefore, session.snapshot().runsCompleted(), "a broken formula must not prestige");
    }

    @Test
    void saveRoundTripKeepsProgressAndLevels() {
        GameBalance balance = GameBalance.standard().copy();
        balance.startingCoins = 400;
        LocalGameSession session = new LocalGameSession(balance);
        session.apply(new GameCommand.StartRun());
        session.apply(new GameCommand.BuyUpgrade(UpgradeId.POWER));
        session.tick(0);
        session.catchUpElapsed(TimeUnit.SECONDS.toNanos(3));

        String encoded = SaveCodec.encode(session.toSave(1_000L));
        LocalGameSession restored = LocalGameSession.fromSave(balance, SaveCodec.decode(encoded));

        GameSnapshot before = session.snapshot();
        GameSnapshot after = restored.snapshot();
        assertEquals(before.wave(), after.wave());
        assertEquals(before.tier(), after.tier());
        assertEquals(before.bestWave(), after.bestWave());
        assertEquals(before.enemies().size(), after.enemies().size());
        assertEquals(levelOf(before, UpgradeId.POWER), levelOf(after, UpgradeId.POWER));
        assertEquals(before.towerHp(), after.towerHp(), 0.001);
    }

    private static AbilityView abilityOf(GameSnapshot snap, AbilityId id) {
        return snap.abilities().stream()
                .filter(view -> view.id() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("missing " + id));
    }

    private static MissionView missionOf(GameSnapshot snap, MissionId id) {
        return snap.missions().stream()
                .filter(view -> view.id() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("missing " + id));
    }

    private static LabView labOf(GameSnapshot snap, LabId id) {
        return snap.labs().stream()
                .filter(view -> view.id() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("missing " + id));
    }

    private static int runUpgradeLevels(GameSnapshot snap) {
        int total = 0;
        for (UpgradeView view : allViews(snap)) {
            if (view.id().isRunUpgrade()) {
                total += view.level();
            }
        }
        return total;
    }

    private static List<UpgradeView> allViews(GameSnapshot snap) {
        List<UpgradeView> all = new ArrayList<>();
        all.addAll(snap.attack());
        all.addAll(snap.defense());
        all.addAll(snap.meta());
        all.addAll(snap.permanents());
        all.addAll(snap.automation());
        all.addAll(snap.contracts());
        return all;
    }

    private static int levelOf(GameSnapshot snap, UpgradeId id) {
        for (UpgradeView view : allViews(snap)) {
            if (view.id() == id) {
                return view.level();
            }
        }
        throw new IllegalArgumentException("missing " + id);
    }
}
