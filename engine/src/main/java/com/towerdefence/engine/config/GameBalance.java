package com.towerdefence.engine.config;

import com.towerdefence.engine.command.UpgradeId;
import com.towerdefence.engine.model.AbilityId;
import com.towerdefence.engine.model.LabId;
import com.towerdefence.engine.sim.EnemyKind;

/**
 * All tuning numbers and the formulas derived from them. Mutable on purpose: tests and future
 * balance passes tweak a copy, while the simulation only ever reads it.
 */
public final class GameBalance {
    // Run economy.
    // The wall exists because enemy health grows faster per wave than buyable damage does:
    // damage scales like coinPerWave^(log(powerPerLevel)/log(powerCostGrowth)). Keep
    // enemyHpGrowth above that or runs never end.
    public double startingCoins = 15;
    public double coinBase = 3.0;
    public double coinPerWave = 0.11;

    // Tower survivability
    public double baseTowerHp = 150;
    public double foundationHpPerLevel = 0.16;
    public double coreHpPerLevel = 0.12;
    public double regenFractionPerSecond = 0.004;
    public double coreRegenPerLevel = 0.35;

    // Damage
    public double baseDamage = 12;
    public double powerPerLevel = 0.22;
    public double foundationDamagePerLevel = 0.06;
    /**
     * Held shards used to add 5% damage each and deleted every wall. A log keeps prestige
     * useful without making the next five tiers free. Spend shards on FOUNDATION to push.
     */
    public double shardDamageLog = 0.22;
    public double coreDamagePerLevel = 0.15;

    // Rate of fire
    public double baseFireInterval = 0.85;
    public double tempoPerLevel = 0.10;
    public double minFireInterval = 0.12;

    // Reach, as a fraction of the lane the tower can cover
    public double baseRange = 0.55;
    public double rangePerLevel = 0.06;
    public double maxRange = 0.95;

    // Projectiles travel the lane instead of hitting instantly
    public double projectileSpeed = 2.1;

    // Coin gain
    public double fortunePerLevel = 0.14;
    public double coreCoinPerLevel = 0.20;

    // Enemies
    public double enemyHpBase = 10;
    public double enemyHpGrowth = 0.24;
    public double enemySpeedBase = 0.085;
    public double enemySpeedPerWave = 0.0022;
    public double enemySpeedMax = 0.26;
    public double leakDamageBase = 9;
    /**
     * A leak hurts in proportion to how strong that enemy was. Tying it to enemy health keeps
     * runs bounded: health grows per wave faster than you can buy tower health, so leaks
     * eventually end the run no matter how much wall and regen you stack.
     */
    public double leakDamageFraction = 0.8;

    // Upgrade costs
    public double powerCostBase = 12;
    public double powerCostGrowth = 0.16;
    public double tempoCostBase = 25;
    public double tempoCostGrowth = 0.19;
    public double reachCostBase = 40;
    public double reachCostGrowth = 0.22;
    public double foundationCostBase = 3;
    public double foundationCostGrowth = 0.34;
    public double fortuneCostBase = 4;
    public double fortuneCostGrowth = 0.36;
    public double autoUnlockShards = 12;
    public double autoPrestigeCost = 4;
    public double autoRunCost = 40;
    /** Shards drop from kills, scaled by that enemy's HP so later waves farm more. */
    public double shardPerHp = 0.35;
    /** Sublinear so a wave-100 farm does not buy the whole workshop. */
    public double shardHpExponent = 0.42;
    public double overclockPerLevel = 0.08;
    public double opticsPerLevel = 0.035;
    public double recoveryPerLevel = 0.35;
    public double rampartPerLevel = 8;
    public double harvestPerLevel = 0.12;
    public double overclockCostBase = 5;
    public double overclockCostGrowth = 0.36;
    public double opticsCostBase = 5;
    public double opticsCostGrowth = 0.36;
    public double recoveryCostBase = 5;
    public double recoveryCostGrowth = 0.36;
    public double rampartCostBase = 6;
    public double rampartCostGrowth = 0.38;
    public double harvestCostBase = 8;
    public double harvestCostGrowth = 0.40;
    public double coreCostBase = 1;
    public double coreCostStep = 1;

    // Tiers: each one multiplies enemy health harder than it multiplies rewards, so you need
    // meta upgrades to climb. Cores per milestone scale with the tier, which is the exponential part.
    public int maxTier = 10;
    /** Hard ceiling per tier: finish this wave and the floor is cleared. */
    public int wavesPerTier = 5000;
    /**
     * Wave you must reach <em>on this tier</em> to open the next one.
     * T1 is meant to take 2–3 prestiges (first run dies ~20). Later floors cost more.
     */
    public int unlockBase = 68;
    public double unlockGrowth = 1.5;
    /** Unlock waves grow early, then sit here so the ladder does not run away. */
    public int unlockCap = 500;
    /**
     * Extra waves of HP baked into a higher tier, so a new floor is a real step down
     * (~10–20 waves) instead of "same ceiling, slightly fatter enemies".
     */
    public int tierWaveBias = 12;
    public double tierHpGrowth = 2.6;
    public double tierSpeedGrowth = 1.06;
    public double tierCoinGrowth = 2.2;
    public double tierShardGrowth = 1.9;

    // Elites: one heavy enemy closes every tenth wave.
    public int eliteEveryWaves = 10;
    public double eliteHpMultiplier = 7;
    public double eliteCoinMultiplier = 6;
    public double eliteSpeedMultiplier = 0.75;

    // Enemy variety. Kinds appear on a fixed cadence so the simulation stays deterministic.
    public int fastFromWave = 5;
    public int tankFromWave = 8;
    public int rangedFromWave = 12;
    public int protectorFromWave = 16;
    public double fastHpMultiplier = 0.45;
    public double fastSpeedMultiplier = 2.0;
    public double fastCoinMultiplier = 0.8;
    public double tankHpMultiplier = 3.5;
    public double tankSpeedMultiplier = 0.6;
    public double tankCoinMultiplier = 2.2;
    public double rangedHpMultiplier = 0.8;
    public double rangedCoinMultiplier = 1.4;
    /** Where a ranged enemy parks and starts shooting. */
    public double rangedStopT = 0.62;
    public double rangedDpsBase = 3.5;
    public double rangedDpsPerWave = 0.45;
    public double protectorHpMultiplier = 1.5;
    public double protectorSpeedMultiplier = 0.8;
    public double protectorCoinMultiplier = 1.8;
    /** Each live protector divides incoming damage on everything else. */
    public double protectorShieldPerUnit = 0.6;

    // Attack depth. Every multiplier here is capped, so it moves the wall without changing
    // the per-wave exponent that makes runs end.
    public int multishotMax = 4;
    public double multishotCostBase = 250;
    public double multishotCostGrowth = 1.2;
    public double critFactor = 2.5;
    public double critChancePerLevel = 0.05;
    public double critChanceCap = 0.55;
    public double critCostBase = 180;
    public double critCostGrowth = 0.45;
    public int bounceMax = 3;
    public double bounceCostBase = 320;
    public double bounceCostGrowth = 1.4;
    /** REACH also adds damage against enemies far from the tower. */
    public double distanceDamagePerLevel = 0.08;
    public double distanceDamageMax = 1.5;

    // Defence depth.
    public double healthPerLevel = 0.18;
    public double healthCostBase = 45;
    public double healthCostGrowth = 0.18;
    public double regenPerLevel = 0.5;
    public double regenCostBase = 70;
    public double regenCostGrowth = 0.2;
    public double wallPerLevel = 60;
    public double wallCostBase = 150;
    public double wallCostGrowth = 0.3;
    public double wallRegenBase = 2;
    public double wallRegenPerLevel = 3;
    public double wallRegenCostBase = 130;
    public double wallRegenCostGrowth = 0.25;
    public double thornsDpsPerLevel = 6;
    /** Enemies this close to the tower stand on the spikes. */
    public double thornsReach = 0.88;
    public double thornsCostBase = 160;
    public double thornsCostGrowth = 0.28;
    public int knockbackStartInterval = 9;
    public int knockbackBestInterval = 2;
    public double knockbackDistance = 0.08;
    public double knockbackCostBase = 220;
    public double knockbackCostGrowth = 0.35;

    // Meta progression.
    // Offline time starts deliberately short so the early game is played, not waited out.
    // Each NIGHT SHIFT level doubles it, and the cost doubles too.
    public int wavesPerCore = 10;
    public int wavesPerSigil = 25;
    public double offlineHoursBase = 0.5;
    public double offlineHoursMax = 12;
    public double offlineCostBase = 2;
    public double starPerLevel = 0.02;
    public double starCostBase = 1;
    public double autoCastCost = 3;
    public double formulaCost = 6;

    // Ultimate abilities. Cores buy them, levels shorten the cooldown and sharpen the effect.
    public double abilityCostBase = 3;
    public double abilityCostGrowth = 0.8;
    public double goldenCoinMultiplierBase = 2;
    public double goldenCoinMultiplierPerLevel = 1;
    public double goldenDuration = 10;
    public double goldenCooldownBase = 90;
    public double deathWaveDamageBase = 8;
    public double deathWaveDamagePerLevel = 4;
    public double deathWaveCooldownBase = 120;
    public int chainTargetsBase = 2;
    public double chainDamageBase = 3;
    public double chainCooldownBase = 45;
    public double blackHolePullBase = 0.15;
    public double blackHolePullPerLevel = 0.03;
    public double blackHoleSlow = 0.5;
    public double blackHoleDuration = 6;
    public double blackHoleCooldownBase = 100;
    /** Each level shaves this fraction off a cooldown, down to 40% of the base. */
    public double abilityCooldownPerLevel = 0.06;
    public double abilityCooldownFloor = 0.4;

    // Game speed multiplies simulated time, offline catch-up included.
    public double gameSpeedPerLevel = 0.5;
    public double gameSpeedMax = 3;
    public double gameSpeedCostBase = 4;
    public double gameSpeedCostGrowth = 1.0;

    // Labs run on the real clock, so they keep working with the app closed.
    public double labCostBase = 40;
    public double labCostGrowth = 0.9;
    public double labMinutesBase = 20;
    public double labMinutesGrowth = 1.35;
    public double labDamagePerLevel = 0.10;
    public double labCoinsPerLevel = 0.10;
    public double labHealthPerLevel = 0.10;
    public int labMultishotMaxLevels = 3;

    // Crest shop. These change how a run starts, so they cannot be farmed from waves.
    public double headStartWavesPerLevel = 5;
    public int headStartMaxLevel = 20;
    public double headStartCostBase = 2;
    public double headStartCostGrowth = 1.0;
    public double warChestPerLevel = 0.75;
    public double warChestCostBase = 2;
    public double warChestCostGrowth = 1.1;
    public int crestsPerTierClearBase = 2;

    public long tickStepNanos = 50_000_000L;

    public static GameBalance standard() {
        return new GameBalance();
    }

    /**
     * Copies every tuning field. Reflection keeps this honest: new fields are picked up
     * automatically instead of silently defaulting when someone forgets a line here.
     */
    public GameBalance copy() {
        GameBalance c = new GameBalance();
        for (java.lang.reflect.Field field : GameBalance.class.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                field.set(c, field.get(this));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("cannot copy " + field.getName(), e);
            }
        }
        return c;
    }

    // --- wave shape ---

    public int waveSize(int wave) {
        return 4 + wave / 2;
    }

    public double spawnInterval(int wave) {
        return Math.max(0.28, 0.95 - wave * 0.012);
    }

    public double waveBreak() {
        return 1.35;
    }

    // --- tower ---

    public double towerMaxHp(int foundationLevel, int coreHpLevel, int healthLevel) {
        return baseTowerHp
                * Math.pow(1.0 + foundationHpPerLevel, foundationLevel)
                * Math.pow(1.0 + coreHpPerLevel, coreHpLevel)
                * Math.pow(1.0 + healthPerLevel, healthLevel);
    }

    public double regenPerSecond(int foundationLevel, int coreHpLevel, int coreRegenLevel,
                                int regenLevel, int recoveryLevel) {
        return towerMaxHp(foundationLevel, coreHpLevel, 0) * regenFractionPerSecond
                + coreRegenLevel * coreRegenPerLevel
                + regenLevel * regenPerLevel
                + recoveryLevel * recoveryPerLevel;
    }

    public double wallMax(int wallLevel, int rampartLevel) {
        return wallPerLevel * wallLevel + rampartPerLevel * rampartLevel;
    }

    public double wallRegen(int wallRegenLevel) {
        return wallRegenLevel == 0 ? 0 : wallRegenBase + wallRegenPerLevel * wallRegenLevel;
    }

    public double thornsDps(int thornsLevel) {
        return thornsDpsPerLevel * thornsLevel;
    }

    public int knockbackInterval(int level) {
        return level <= 0 ? 0 : Math.max(knockbackBestInterval, knockbackStartInterval - level);
    }

    // --- attack depth ---

    public int multishotCount(int level) {
        return Math.min(multishotMax, 1 + level);
    }

    public double critChance(int level) {
        return level <= 0 ? 0d : Math.min(critChanceCap, critChancePerLevel * level);
    }

    public int bounceCount(int level) {
        return Math.min(bounceMax, level);
    }

    /** REACH doubles as a distance bonus: far targets take more. */
    public double distanceDamageBonus(int reachLevel, double pathT) {
        double farness = Math.max(0d, 1d - pathT);
        return Math.min(distanceDamageMax, 1d + distanceDamagePerLevel * reachLevel * farness);
    }

    // --- enemy kinds ---

    public EnemyKind kindFor(int wave, int indexInWave, int waveSize) {
        if (isEliteWave(wave) && indexInWave == waveSize - 1) {
            return EnemyKind.BOSS;
        }
        if (wave >= protectorFromWave && indexInWave % 7 == 6) {
            return EnemyKind.PROTECTOR;
        }
        if (wave >= rangedFromWave && indexInWave % 6 == 5) {
            return EnemyKind.RANGED;
        }
        if (wave >= tankFromWave && indexInWave % 5 == 4) {
            return EnemyKind.TANK;
        }
        if (wave >= fastFromWave && indexInWave % 3 == 2) {
            return EnemyKind.FAST;
        }
        return EnemyKind.BASIC;
    }

    public double kindHpMultiplier(EnemyKind kind) {
        return switch (kind) {
            case BASIC -> 1d;
            case FAST -> fastHpMultiplier;
            case TANK -> tankHpMultiplier;
            case RANGED -> rangedHpMultiplier;
            case PROTECTOR -> protectorHpMultiplier;
            case BOSS -> eliteHpMultiplier;
        };
    }

    public double kindSpeedMultiplier(EnemyKind kind) {
        return switch (kind) {
            case BASIC, RANGED -> 1d;
            case FAST -> fastSpeedMultiplier;
            case TANK -> tankSpeedMultiplier;
            case PROTECTOR -> protectorSpeedMultiplier;
            case BOSS -> eliteSpeedMultiplier;
        };
    }

    public double kindCoinMultiplier(EnemyKind kind) {
        return switch (kind) {
            case BASIC -> 1d;
            case FAST -> fastCoinMultiplier;
            case TANK -> tankCoinMultiplier;
            case RANGED -> rangedCoinMultiplier;
            case PROTECTOR -> protectorCoinMultiplier;
            case BOSS -> eliteCoinMultiplier;
        };
    }

    public double rangedDps(int wave) {
        return rangedDpsBase + rangedDpsPerWave * (wave - 1);
    }

    /** Divisor applied to damage while protectors are alive. */
    public double protectorShield(int liveProtectors) {
        return 1d + protectorShieldPerUnit * liveProtectors;
    }

    // --- abilities ---

    public double abilityCooldown(AbilityId id, int level) {
        double base = switch (id) {
            case GOLDEN_TOWER -> goldenCooldownBase;
            case DEATH_WAVE -> deathWaveCooldownBase;
            case CHAIN_LIGHTNING -> chainCooldownBase;
            case BLACK_HOLE -> blackHoleCooldownBase;
        };
        double scale = Math.max(abilityCooldownFloor, 1d - abilityCooldownPerLevel * Math.max(0, level - 1));
        return base * scale;
    }

    public double abilityDuration(AbilityId id) {
        return switch (id) {
            case GOLDEN_TOWER -> goldenDuration;
            case BLACK_HOLE -> blackHoleDuration;
            case DEATH_WAVE, CHAIN_LIGHTNING -> 0d;
        };
    }

    public double goldenCoinMultiplier(int level) {
        return goldenCoinMultiplierBase + goldenCoinMultiplierPerLevel * Math.max(0, level - 1);
    }

    public double deathWaveDamage(int level) {
        return deathWaveDamageBase + deathWaveDamagePerLevel * Math.max(0, level - 1);
    }

    public int chainTargets(int level) {
        return chainTargetsBase + Math.max(0, level - 1);
    }

    public double chainDamage(int level) {
        return chainDamageBase + Math.max(0, level - 1);
    }

    public double blackHolePull(int level) {
        return blackHolePullBase + blackHolePullPerLevel * Math.max(0, level - 1);
    }

    // --- game speed ---

    public double gameSpeed(int level) {
        return Math.min(gameSpeedMax, 1d + gameSpeedPerLevel * level);
    }

    // --- labs ---

    public double labCost(LabId id, int level) {
        double scale = id == LabId.LAB_MULTISHOT ? 4d : 1d;
        return labCostBase * scale * Math.pow(1.0 + labCostGrowth, level);
    }

    public double labMinutes(LabId id, int level) {
        double scale = id == LabId.LAB_MULTISHOT ? 6d : 1d;
        return labMinutesBase * scale * Math.pow(labMinutesGrowth, level);
    }

    public int labMaxLevel(LabId id) {
        return id == LabId.LAB_MULTISHOT ? labMultishotMaxLevels : 50;
    }

    public double labDamageMultiplier(int level) {
        return 1d + labDamagePerLevel * level;
    }

    public double labCoinMultiplier(int level) {
        return 1d + labCoinsPerLevel * level;
    }

    public double labHealthMultiplier(int level) {
        return 1d + labHealthPerLevel * level;
    }

    public double shardDamageMultiplier(double shards) {
        return 1.0 + shardDamageLog * Math.log1p(Math.max(0d, shards));
    }

    public double damage(int powerLevel, int foundationLevel, double shards, int coreDamageLevel) {
        double run = baseDamage * Math.pow(1.0 + powerPerLevel, powerLevel);
        double meta = 1.0 + foundationLevel * foundationDamagePerLevel;
        double permanent = Math.pow(1.0 + coreDamagePerLevel, coreDamageLevel);
        return run * meta * shardDamageMultiplier(shards) * permanent;
    }

    public double fireInterval(int tempoLevel, int overclockLevel) {
        return Math.max(minFireInterval,
                baseFireInterval / (1.0 + tempoPerLevel * tempoLevel + overclockPerLevel * overclockLevel));
    }

    public double range(int reachLevel, int opticsLevel) {
        return Math.min(maxRange, baseRange + rangePerLevel * reachLevel + opticsPerLevel * opticsLevel);
    }

    public double starMultiplier(int level) {
        return Math.pow(1.0 + starPerLevel, Math.max(0, level));
    }

    public int prestigeStars(int wave) {
        return Math.max(1, wave / 20);
    }

    public double shardsForKill(double enemyHp, int harvestLevel) {
        return Math.pow(Math.max(0d, enemyHp), shardHpExponent)
                * shardPerHp
                * (1d + harvestPerLevel * Math.max(0, harvestLevel));
    }

    // --- tiers ---

    /** Wave required on {@code tier} to unlock {@code tier + 1}. */
    public int unlockWaveOn(int tier) {
        int raw = (int) Math.round(unlockBase * Math.pow(unlockGrowth, Math.max(0, tier - 1)));
        return Math.min(unlockCap, raw);
    }

    /** Wave required on the previous floor to open this one. Tier 1 is free. */
    public int waveToUnlockTier(int tier) {
        return tier <= 1 ? 0 : unlockWaveOn(tier - 1);
    }

    public double tierHpMultiplier(int tier) {
        return Math.pow(tierHpGrowth, tier - 1);
    }

    public double tierCoinMultiplier(int tier) {
        return Math.pow(tierCoinGrowth, tier - 1);
    }

    public double tierShardMultiplier(int tier) {
        return Math.pow(tierShardGrowth, tier - 1);
    }

    /** Cores handed out per completed 10-wave milestone in this tier. */
    public int coresPerMilestone(int tier) {
        return tier;
    }

    public int coresFor(int tier, int bestWaveInTier) {
        return (bestWaveInTier / wavesPerCore) * coresPerMilestone(tier);
    }

    /** Sigils are far rarer: one batch per 25-wave record, scaled by tier. */
    public int sigilsFor(int tier, int bestWaveInTier) {
        return (bestWaveInTier / wavesPerSigil) * tier;
    }

    // --- enemies ---

    public boolean isEliteWave(int wave) {
        return wave % eliteEveryWaves == 0;
    }

    public double enemyHp(int wave, int tier) {
        int extra = tierWaveBias * Math.max(0, tier - 1);
        // Ramp the extra in so a new floor is a step down, not a wave-1 wipe.
        double ramp = Math.min(1d, Math.max(0, wave - 1) / 20d);
        double effectiveWave = wave + extra * ramp;
        return enemyHpBase * Math.pow(1.0 + enemyHpGrowth, Math.max(0d, effectiveWave - 1d))
                * tierHpMultiplier(tier);
    }

    public double enemySpeed(int wave, int tier) {
        double base = enemySpeedBase + enemySpeedPerWave * (wave - 1);
        return Math.min(enemySpeedMax, base * Math.pow(tierSpeedGrowth, tier - 1));
    }

    public double leakDamage(int wave, int tier) {
        return Math.max(leakDamageBase, enemyHp(wave, tier) * leakDamageFraction);
    }

    // --- rewards ---

    public double coinMultiplier(int fortuneLevel, int coreCoinLevel) {
        return Math.pow(1.0 + fortunePerLevel, fortuneLevel)
                * Math.pow(1.0 + coreCoinPerLevel, coreCoinLevel);
    }

    public double coinsForKill(int wave, int tier, int fortuneLevel, int coreCoinLevel) {
        return coinBase
                * Math.pow(1.0 + coinPerWave, wave - 1)
                * tierCoinMultiplier(tier)
                * coinMultiplier(fortuneLevel, coreCoinLevel);
    }

    /** Prestige no longer dumps shards. Kept so old snapshot code stays compiling. */
    public double pendingShards(int wave, int tier) {
        return 0d;
    }

    public double offlineHours(int nightShiftLevel) {
        return Math.min(offlineHoursMax, offlineHoursBase * Math.pow(2, nightShiftLevel));
    }

    public long maxCatchUpNanos(int nightShiftLevel) {
        return (long) (offlineHours(nightShiftLevel) * 60 * 60 * 1_000_000_000L);
    }

    public double cost(UpgradeId id, int level) {
        return switch (id) {
            case POWER -> powerCostBase * Math.pow(1.0 + powerCostGrowth, level);
            case TEMPO -> tempoCostBase * Math.pow(1.0 + tempoCostGrowth, level);
            case REACH -> reachCostBase * Math.pow(1.0 + reachCostGrowth, level);
            case MULTISHOT -> multishotCostBase * Math.pow(1.0 + multishotCostGrowth, level);
            case CRIT -> critCostBase * Math.pow(1.0 + critCostGrowth, level);
            case BOUNCE -> bounceCostBase * Math.pow(1.0 + bounceCostGrowth, level);
            case HEALTH -> healthCostBase * Math.pow(1.0 + healthCostGrowth, level);
            case REGEN -> regenCostBase * Math.pow(1.0 + regenCostGrowth, level);
            case WALL -> wallCostBase * Math.pow(1.0 + wallCostGrowth, level);
            case WALL_REGEN -> wallRegenCostBase * Math.pow(1.0 + wallRegenCostGrowth, level);
            case THORNS -> thornsCostBase * Math.pow(1.0 + thornsCostGrowth, level);
            case KNOCKBACK -> knockbackCostBase * Math.pow(1.0 + knockbackCostGrowth, level);
            case FOUNDATION -> foundationCostBase * Math.pow(1.0 + foundationCostGrowth, level);
            case FORTUNE -> fortuneCostBase * Math.pow(1.0 + fortuneCostGrowth, level);
            case AUTO -> autoUnlockShards;
            case AUTO_PRESTIGE -> autoPrestigeCost;
            case AUTO_RUN -> autoRunCost;
            case NIGHT_SHIFT -> offlineCostBase * Math.pow(2, level);
            case AUTO_CAST -> autoCastCost;
            case FORMULA -> formulaCost;
            case GAME_SPEED -> gameSpeedCostBase * Math.pow(1.0 + gameSpeedCostGrowth, level);
            case ABILITY_GOLDEN, ABILITY_DEATH_WAVE, ABILITY_CHAIN, ABILITY_BLACK_HOLE ->
                    abilityCostBase * Math.pow(1.0 + abilityCostGrowth, level);
            case CORE_DAMAGE, CORE_HP, CORE_REGEN, CORE_COIN ->
                    coreCostBase + coreCostStep * level;
            case HEADSTART -> headStartCostBase * Math.pow(1.0 + headStartCostGrowth, level);
            case WAR_CHEST -> warChestCostBase * Math.pow(1.0 + warChestCostGrowth, level);
            case OVERCLOCK -> overclockCostBase * Math.pow(1.0 + overclockCostGrowth, level);
            case OPTICS -> opticsCostBase * Math.pow(1.0 + opticsCostGrowth, level);
            case RECOVERY -> recoveryCostBase * Math.pow(1.0 + recoveryCostGrowth, level);
            case RAMPART -> rampartCostBase * Math.pow(1.0 + rampartCostGrowth, level);
            case HARVEST -> harvestCostBase * Math.pow(1.0 + harvestCostGrowth, level);
            case STAR_DAMAGE, STAR_HP, STAR_COIN, STAR_REGEN -> starCostBase + level;
        };
    }

    public int startingWave(int headStartLevel) {
        int wave = 1 + (int) (headStartWavesPerLevel * Math.min(headStartMaxLevel, Math.max(0, headStartLevel)));
        return Math.min(wavesPerTier, wave);
    }

    public double runStartingCoins(int warChestLevel) {
        return startingCoins * (1d + warChestPerLevel * Math.max(0, warChestLevel));
    }

    public int crestsForTierClear(int tier) {
        return crestsPerTierClearBase + Math.max(1, tier);
    }
}
