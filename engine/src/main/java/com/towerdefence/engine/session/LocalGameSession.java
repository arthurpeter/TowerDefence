package com.towerdefence.engine.session;

import com.towerdefence.engine.command.GameCommand;
import com.towerdefence.engine.command.UpgradeId;
import com.towerdefence.engine.config.GameBalance;
import com.towerdefence.engine.model.AbilityId;
import com.towerdefence.engine.model.LabId;
import com.towerdefence.engine.model.MissionId;
import com.towerdefence.engine.model.RunPhase;
import com.towerdefence.engine.sim.Enemy;
import com.towerdefence.engine.sim.EnemyKind;
import com.towerdefence.engine.sim.Projectile;
import com.towerdefence.engine.util.SciFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

public final class LocalGameSession implements GameSession {
    private static final UpgradeId[] ATTACK = {
            UpgradeId.POWER, UpgradeId.TEMPO, UpgradeId.REACH,
            UpgradeId.MULTISHOT, UpgradeId.CRIT, UpgradeId.BOUNCE
    };
    private static final UpgradeId[] DEFENSE = {
            UpgradeId.HEALTH, UpgradeId.REGEN, UpgradeId.WALL,
            UpgradeId.WALL_REGEN, UpgradeId.THORNS, UpgradeId.KNOCKBACK
    };
    private static final UpgradeId[] META = {
            UpgradeId.FOUNDATION, UpgradeId.FORTUNE, UpgradeId.OVERCLOCK,
            UpgradeId.OPTICS, UpgradeId.RECOVERY, UpgradeId.RAMPART,
            UpgradeId.HARVEST
    };
    private static final UpgradeId[] PERMANENTS = {
            UpgradeId.CORE_DAMAGE, UpgradeId.CORE_HP, UpgradeId.CORE_REGEN,
            UpgradeId.CORE_COIN, UpgradeId.GAME_SPEED
    };
    private static final UpgradeId[] AUTOMATION = {
            UpgradeId.AUTO, UpgradeId.AUTO_PRESTIGE, UpgradeId.AUTO_RUN,
            UpgradeId.AUTO_CAST, UpgradeId.NIGHT_SHIFT, UpgradeId.FORMULA
    };
    private static final UpgradeId[] PRESTIGE = {
            UpgradeId.STAR_DAMAGE, UpgradeId.STAR_HP, UpgradeId.STAR_COIN, UpgradeId.STAR_REGEN
    };
    private static final UpgradeId[] CONTRACTS = {
            UpgradeId.HEADSTART, UpgradeId.WAR_CHEST
    };

    private final GameBalance balance;
    private final int[] levels = new int[UpgradeId.values().length];
    private final boolean[] autoTargets = new boolean[UpgradeId.values().length];
    private final double[] abilityCooldowns = new double[AbilityId.values().length];
    private final int[] labLevels = new int[LabId.values().length];
    private final boolean[] missionsDone = new boolean[MissionId.values().length];
    private final boolean[] tiersCleared;
    private final int[] bestWavePerTier;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();

    private RunPhase phase = RunPhase.IDLE;
    private int wave = 1;
    private int tier = 1;
    private double coins;
    private double shards;
    private double stars;
    /** Held shards at run start. Farmed shards do not feed damage mid-run. */
    private double combatShards;
    private double cores;
    private double sigils;
    private double crests;
    private int coresAwarded;
    private int sigilsAwarded;
    private double towerHp;
    private double wall;
    private boolean autoBuy;
    private boolean autoCast;
    private boolean autoRun;
    private int autoRunTier = 1;
    private int nextRunStars;
    /**
     * Wave the previous run ended on. You only get to cash out by beating it, otherwise
     * prestige is an infinite tap that pays for standing still. It follows the last run
     * rather than an all-time record so a bad run lowers the bar instead of locking you out.
     */
    private int lastRunWave;
    private AutoPrestigeRule autoPrestige = AutoPrestigeRule.off();
    private PrestigeFormula compiledFormula;
    private String formulaError;
    private int runsCompleted;
    private int prestiges;
    private long enemiesKilled;
    private long elitesKilled;
    private int labsFinished;

    private double goldenRemaining;
    private double blackHoleRemaining;

    private LabId runningLab;
    private long labEndsAtMillis;
    private long labStartedAtMillis;
    private long wallClockMillis;

    private int nextEnemyId = 1;
    private int nextProjectileId = 1;
    private double fireCooldown;
    private int remainingToSpawn;
    private int spawnedInWave;
    private double spawnCooldown;
    private double waveBreak;
    private double secondsSinceWaveChange;
    /**
     * Fixed seed: the simulation has to be reproducible so balance probes can A/B a change,
     * and so a future authoritative server can replay a client's run and get the same result.
     */
    private final Random rng = new Random(0x7D1DEL);
    private long hitCounter;
    private long lastNanos;
    private boolean clockStarted;

    public LocalGameSession() {
        this(GameBalance.standard());
    }

    public LocalGameSession(GameBalance balance) {
        this.balance = Objects.requireNonNull(balance);
        this.bestWavePerTier = new int[balance.maxTier + 1];
        this.tiersCleared = new boolean[balance.maxTier + 1];
        this.coins = balance.startingCoins;
        this.towerHp = maxHp();
        for (UpgradeId id : UpgradeId.values()) {
            autoTargets[id.ordinal()] = id.isRunUpgrade();
        }
    }

    @Override
    public void apply(GameCommand command) {
        switch (command) {
            case GameCommand.StartRun() -> startRun();
            case GameCommand.Prestige() -> prestige();
            case GameCommand.SelectTier(int target) -> selectTier(target);
            case GameCommand.SetAutoBuy(boolean enabled) -> {
                if (levels[UpgradeId.AUTO.ordinal()] > 0) {
                    autoBuy = enabled;
                }
            }
            case GameCommand.SetAutoTarget(UpgradeId id, boolean enabled) -> {
                if (id.isRunUpgrade()) {
                    autoTargets[id.ordinal()] = enabled;
                }
            }
            case GameCommand.SetAutoPrestige(AutoPrestigeRule rule) -> setAutoPrestige(rule);
            case GameCommand.SetAutoCast(boolean enabled) -> {
                if (levels[UpgradeId.AUTO_CAST.ordinal()] > 0) {
                    autoCast = enabled;
                }
            }
            case GameCommand.SetAutoRun(boolean enabled) -> {
                if (levels[UpgradeId.AUTO_RUN.ordinal()] > 0) {
                    autoRun = enabled;
                }
            }
            case GameCommand.SetAutoRunTier(int target) -> {
                if (target >= 1 && target <= highestTierUnlocked()) {
                    autoRunTier = target;
                }
            }
            case GameCommand.CastAbility(AbilityId id) -> cast(id);
            case GameCommand.StartLab(LabId id) -> startLab(id);
            case GameCommand.BuyUpgrade(UpgradeId id) -> buy(id);
        }
    }

    @Override
    public void tick(long nowNanos) {
        if (!clockStarted) {
            clockStarted = true;
            lastNanos = nowNanos;
            return;
        }
        long elapsed = nowNanos - lastNanos;
        lastNanos = nowNanos;
        catchUpElapsed(elapsed);
    }

    @Override
    public void holdClock(long nowNanos) {
        clockStarted = true;
        lastNanos = nowNanos;
    }

    @Override
    public void catchUpElapsed(long elapsedNanos) {
        if (phase != RunPhase.RUNNING) {
            maybeResumeAutoRun();
            if (phase != RunPhase.RUNNING) {
                return;
            }
        }
        long cap = balance.maxCatchUpNanos(levels[UpgradeId.NIGHT_SHIFT.ordinal()]);
        long real = Math.min(Math.max(0L, elapsedNanos), cap);
        // Game speed stretches simulated time, offline catch-up included.
        long remaining = (long) (real * balance.gameSpeed(levels[UpgradeId.GAME_SPEED.ordinal()]));
        while (remaining > 0L && phase == RunPhase.RUNNING) {
            long step = Math.min(balance.tickStepNanos, remaining);
            simulate(step / 1_000_000_000d);
            if (autoBuyActive()) {
                autoBuyLoop();
                autoBuyStars();
            }
            remaining -= step;
        }
    }

    @Override
    public void wallClock(long epochMillis) {
        wallClockMillis = epochMillis;
        if (runningLab != null && epochMillis >= labEndsAtMillis) {
            labLevels[runningLab.ordinal()] += 1;
            labsFinished += 1;
            runningLab = null;
            labEndsAtMillis = 0;
            labStartedAtMillis = 0;
            evaluateMissions();
        }
    }

    @Override
    public GameSnapshot snapshot() {
        int liveProtectors = liveProtectors();
        List<EnemyView> enemyViews = new ArrayList<>(enemies.size());
        for (Enemy enemy : enemies) {
            enemyViews.add(new EnemyView(
                    enemy.id,
                    enemy.kind,
                    enemy.pathT,
                    enemy.maxHp <= 0 ? 0 : enemy.hp / enemy.maxHp,
                    liveProtectors > 0 && enemy.kind != EnemyKind.PROTECTOR
            ));
        }
        List<ProjectileView> projectileViews = new ArrayList<>(projectiles.size());
        for (Projectile projectile : projectiles) {
            projectileViews.add(new ProjectileView(
                    projectile.id, projectile.pathT, projectile.targetId, projectile.crit));
        }

        int highestUnlocked = highestTierUnlocked();
        List<TierView> tiers = new ArrayList<>(balance.maxTier);
        for (int t = 1; t <= balance.maxTier; t++) {
            tiers.add(new TierView(
                    t,
                    t <= highestUnlocked,
                    t == tier,
                    tiersCleared[t],
                    bestWavePerTier[t],
                    balance.waveToUnlockTier(t),
                    t <= 1 ? 0 : t - 1,
                    balance.unlockWaveOn(t),
                    balance.wavesPerTier,
                    balance.tierHpMultiplier(t),
                    balance.tierCoinMultiplier(t),
                    balance.tierShardMultiplier(t),
                    balance.coresPerMilestone(t)
            ));
        }

        int reached = Math.max(bestWavePerTier[tier], wave);
        return new GameSnapshot(
                phase,
                wave,
                bestWaveOverall(),
                tier,
                highestUnlocked,
                bestWavePerTier[tier],
                balance.coresPerMilestone(tier),
                balance.wavesPerCore,
                balance.wavesPerCore - (reached % balance.wavesPerCore),
                balance.wavesPerSigil,
                balance.wavesPerSigil - (reached % balance.wavesPerSigil),
                balance.isEliteWave(wave),
                coins,
                shards,
                cores,
                sigils,
                crests,
                stars,
                canPrestige() ? balance.prestigeStars(wave, tier) : 0,
                prestigeFloor(),
                canPrestige(),
                0d,
                towerHp,
                maxHp(),
                wall,
                currentWallMax(),
                currentRegen(),
                currentDamage(),
                currentFireInterval(),
                currentRange(),
                multishotCount(),
                balance.critChance(levels[UpgradeId.CRIT.ordinal()]),
                balance.bounceCount(levels[UpgradeId.BOUNCE.ordinal()]),
                balance.thornsDps(levels[UpgradeId.THORNS.ordinal()]),
                balance.gameSpeed(levels[UpgradeId.GAME_SPEED.ordinal()]),
                goldenRemaining,
                blackHoleRemaining,
                autoCast,
                levels[UpgradeId.AUTO_CAST.ordinal()] > 0,
                autoBuy,
                levels[UpgradeId.AUTO.ordinal()] > 0,
                autoRun,
                levels[UpgradeId.AUTO_RUN.ordinal()] > 0,
                autoRunTier,
                balance.offlineHours(levels[UpgradeId.NIGHT_SHIFT.ordinal()]),
                autoPrestige,
                levels[UpgradeId.AUTO_PRESTIGE.ordinal()] > 0,
                levels[UpgradeId.FORMULA.ordinal()] > 0,
                formulaError,
                runsCompleted,
                List.copyOf(enemyViews),
                List.copyOf(projectileViews),
                views(ATTACK),
                views(DEFENSE),
                views(META),
                views(PERMANENTS),
                views(AUTOMATION),
                abilityViews(),
                labViews(),
                List.copyOf(tiers),
                balance.wavesPerTier,
                missionViews(),
                views(CONTRACTS),
                views(PRESTIGE)
        );
    }

    private List<UpgradeView> views(UpgradeId[] ids) {
        List<UpgradeView> out = new ArrayList<>(ids.length);
        for (UpgradeId id : ids) {
            out.add(view(id));
        }
        return List.copyOf(out);
    }

    private List<AbilityView> abilityViews() {
        List<AbilityView> out = new ArrayList<>(AbilityId.values().length);
        for (AbilityId id : AbilityId.values()) {
            UpgradeId upgrade = upgradeFor(id);
            int level = levels[upgrade.ordinal()];
            double cost = balance.cost(upgrade, level);
            double total = balance.abilityCooldown(id, Math.max(1, level));
            double active = switch (id) {
                case GOLDEN_TOWER -> goldenRemaining;
                case BLACK_HOLE -> blackHoleRemaining;
                case DEATH_WAVE, CHAIN_LIGHTNING -> 0d;
            };
            out.add(new AbilityView(
                    id,
                    level,
                    level > 0,
                    cost,
                    cores >= cost,
                    abilityCooldowns[id.ordinal()],
                    total,
                    active,
                    level > 0 && abilityCooldowns[id.ordinal()] <= 0 && phase == RunPhase.RUNNING,
                    abilityEffect(id, level)
            ));
        }
        return List.copyOf(out);
    }

    private List<LabView> labViews() {
        List<LabView> out = new ArrayList<>(LabId.values().length);
        for (LabId id : LabId.values()) {
            int level = labLevels[id.ordinal()];
            boolean maxed = level >= balance.labMaxLevel(id);
            double cost = balance.labCost(id, level);
            boolean running = runningLab == id;
            double totalSeconds = balance.labMinutes(id, level) * 60d;
            double remaining = running
                    ? Math.max(0d, (labEndsAtMillis - wallClockMillis) / 1000d)
                    : 0d;
            out.add(new LabView(
                    id,
                    level,
                    cost,
                    !maxed && runningLab == null && shards >= cost,
                    running,
                    maxed,
                    remaining,
                    running ? (labEndsAtMillis - labStartedAtMillis) / 1000d : totalSeconds,
                    labEffect(id, level)
            ));
        }
        return List.copyOf(out);
    }

    // --- run lifecycle ---

    private void startRun() {
        if (phase == RunPhase.RUNNING) {
            return;
        }
        if (phase == RunPhase.DEAD) {
            runsCompleted += 1;
        }
        resetRun();
    }

    private void prestige() {
        if (!canPrestige()) {
            return;
        }
        bankRun();
        resetRun();
    }

    /** A cash-out has to be earned: get further than the run before this one. */
    private boolean canPrestige() {
        return phase == RunPhase.RUNNING && wave > prestigeFloor();
    }

    private int prestigeFloor() {
        return Math.max(balance.minPrestigeWave, lastRunWave);
    }

    private void selectTier(int target) {
        if (target < 1 || target > balance.maxTier || target > highestTierUnlocked()) {
            return;
        }
        if (phase == RunPhase.RUNNING) {
            bankRun();
        } else if (phase == RunPhase.DEAD) {
            runsCompleted += 1;
        }
        // A new floor is a different difficulty, so the bar to beat starts over with it.
        if (tier != target) {
            lastRunWave = 0;
        }
        tier = target;
        resetRun();
    }

    private void bankRun() {
        nextRunStars = wave > prestigeFloor() ? balance.prestigeStars(wave, tier) : 0;
        lastRunWave = wave;
        prestiges += 1;
        runsCompleted += 1;
        evaluateMissions();
    }

    private void clearTier() {
        if (phase != RunPhase.RUNNING) {
            return;
        }
        bankRun();
        crests += balance.crestsForTierClear(tier);
        tiersCleared[tier] = true;
        phase = RunPhase.CLEARED;
        enemies.clear();
        projectiles.clear();
        evaluateMissions();
    }

    private int highestTierUnlocked() {
        int highest = 1;
        for (int t = 1; t < balance.maxTier; t++) {
            if (bestWavePerTier[t] >= balance.unlockWaveOn(t) || tiersCleared[t]) {
                highest = t + 1;
            } else {
                break;
            }
        }
        return highest;
    }

    private int bestWaveOverall() {
        int best = 1;
        for (int reached : bestWavePerTier) {
            best = Math.max(best, reached);
        }
        return Math.max(best, wave);
    }

    private void resetRun() {
        phase = RunPhase.RUNNING;
        combatShards = shards;
        stars = nextRunStars;
        nextRunStars = 0;
        wave = balance.startingWave(levels[UpgradeId.HEADSTART.ordinal()]);
        coins = balance.runStartingCoins(levels[UpgradeId.WAR_CHEST.ordinal()]);
        for (UpgradeId id : UpgradeId.values()) {
            if (id.isRunUpgrade()) {
                levels[id.ordinal()] = 0;
            }
        }
        enemies.clear();
        projectiles.clear();
        fireCooldown = 0;
        secondsSinceWaveChange = 0;
        hitCounter = 0;
        goldenRemaining = 0;
        blackHoleRemaining = 0;
        java.util.Arrays.fill(abilityCooldowns, 0d);
        towerHp = maxHp();
        wall = currentWallMax();
        // Headstart skips early waves without writing them as records, so it cannot mint cores.
        if (wave <= 1) {
            recordWave();
        }
        beginWave();
    }

    private void beginWave() {
        remainingToSpawn = balance.waveSize(wave);
        spawnedInWave = 0;
        spawnCooldown = 0;
        waveBreak = 0;
    }

    // --- simulation ---

    private void simulate(double dt) {
        secondsSinceWaveChange += dt;
        tickAbilities(dt);
        spawn(dt);
        moveEnemies(dt);
        applyThorns(dt);
        fire(dt);
        moveProjectiles(dt);
        regenerate(dt);
        if (towerHp <= 0) {
            if (canPrestige() && autoPrestigeTriggers()) {
                prestige();
                return;
            }
            towerHp = 0;
            phase = RunPhase.DEAD;
            lastRunWave = wave;
            stars = 0;
            nextRunStars = 0;
            for (UpgradeId id : UpgradeId.values()) {
                if (id.isRunUpgrade()) {
                    levels[id.ordinal()] = 0;
                }
            }
            enemies.clear();
            projectiles.clear();
            return;
        }
        if (canPrestige() && autoPrestigeTriggers()) {
            prestige();
        }
    }

    private void tickAbilities(double dt) {
        for (int i = 0; i < abilityCooldowns.length; i++) {
            abilityCooldowns[i] = Math.max(0d, abilityCooldowns[i] - dt);
        }
        goldenRemaining = Math.max(0d, goldenRemaining - dt);
        blackHoleRemaining = Math.max(0d, blackHoleRemaining - dt);

        if (!autoCast || levels[UpgradeId.AUTO_CAST.ordinal()] == 0) {
            return;
        }
        for (AbilityId id : AbilityId.values()) {
            if (levels[upgradeFor(id).ordinal()] == 0 || abilityCooldowns[id.ordinal()] > 0) {
                continue;
            }
            // Only worth casting when there is something on the lane to affect.
            if (id != AbilityId.GOLDEN_TOWER && enemies.isEmpty()) {
                continue;
            }
            cast(id);
        }
    }

    private void regenerate(double dt) {
        towerHp = Math.min(maxHp(), towerHp + currentRegen() * dt);
        double wallCap = currentWallMax();
        double wallRegen = balance.wallRegen(levels[UpgradeId.WALL_REGEN.ordinal()]);
        wall = Math.min(wallCap, wall + wallRegen * dt);
    }

    /** The wall soaks damage before the tower does. */
    private void damageTower(double amount) {
        double left = amount;
        if (wall > 0) {
            double absorbed = Math.min(wall, left);
            wall -= absorbed;
            left -= absorbed;
        }
        if (left > 0) {
            towerHp -= left;
        }
    }

    private void spawn(double dt) {
        if (remainingToSpawn <= 0) {
            if (enemies.isEmpty()) {
                waveBreak += dt;
                if (waveBreak >= balance.waveBreak()) {
                    if (wave >= balance.wavesPerTier) {
                        clearTier();
                        return;
                    }
                    wave += 1;
                    secondsSinceWaveChange = 0;
                    recordWave();
                    beginWave();
                }
            }
            return;
        }
        spawnCooldown -= dt;
        int waveSize = balance.waveSize(wave);
        while (remainingToSpawn > 0 && spawnCooldown <= 0) {
            EnemyKind kind = balance.kindFor(wave, spawnedInWave, waveSize);
            double hp = balance.enemyHp(wave, tier) * balance.kindHpMultiplier(kind);
            double speed = balance.enemySpeed(wave, tier) * balance.kindSpeedMultiplier(kind);
            enemies.add(new Enemy(nextEnemyId++, kind, hp, speed));
            remainingToSpawn -= 1;
            spawnedInWave += 1;
            spawnCooldown += balance.spawnInterval(wave);
        }
    }

    private void recordWave() {
        if (wave > bestWavePerTier[tier]) {
            bestWavePerTier[tier] = wave;
            awardMilestones();
        }
        evaluateMissions();
    }

    private void moveEnemies(double dt) {
        double slow = blackHoleRemaining > 0 ? (1d - balance.blackHoleSlow) : 1d;
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            if (enemy.kind == EnemyKind.RANGED && enemy.pathT >= balance.rangedStopT) {
                // Snipers park and shell the tower instead of ever reaching it.
                damageTower(balance.rangedDps(wave) * dt);
                continue;
            }
            enemy.pathT += enemy.speed * slow * dt;
            if (enemy.pathT >= 1.0) {
                damageTower(balance.leakDamage(wave, tier) * (enemy.isBoss() ? 2.5d : 1d));
                enemies.remove(i);
            }
        }
    }

    private void applyThorns(double dt) {
        double dps = balance.thornsDps(levels[UpgradeId.THORNS.ordinal()]);
        if (dps <= 0) {
            return;
        }
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            if (enemy.pathT < balance.thornsReach) {
                continue;
            }
            enemy.hp -= dps * dt;
            if (enemy.hp <= 0) {
                payout(enemy);
                enemies.remove(i);
            }
        }
    }

    private void fire(double dt) {
        fireCooldown -= dt;
        double minPath = 1.0 - currentRange();
        int shots = multishotCount();
        while (fireCooldown <= 0) {
            List<Enemy> targets = leadTargets(minPath, shots);
            if (targets.isEmpty()) {
                fireCooldown = 0;
                return;
            }
            int bounces = balance.bounceCount(levels[UpgradeId.BOUNCE.ordinal()]);
            double chance = balance.critChance(levels[UpgradeId.CRIT.ordinal()]);
            for (Enemy target : targets) {
                boolean crit = chance > 0 && rng.nextDouble() < chance;
                double damage = currentDamage() * (crit ? balance.critFactor : 1d);
                projectiles.add(new Projectile(
                        nextProjectileId++, target.id, 1.0, damage, balance.projectileSpeed, crit, bounces));
            }
            fireCooldown += currentFireInterval();
        }
    }

    /** The enemies closest to the tower that are already inside range, nearest first. */
    private List<Enemy> leadTargets(double minPath, int count) {
        List<Enemy> inRange = new ArrayList<>();
        for (Enemy enemy : enemies) {
            if (enemy.pathT >= minPath) {
                inRange.add(enemy);
            }
        }
        inRange.sort((a, b) -> Double.compare(b.pathT, a.pathT));
        return inRange.size() <= count ? inRange : inRange.subList(0, count);
    }

    private void moveProjectiles(double dt) {
        double minPath = 1.0 - currentRange();
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile projectile = projectiles.get(i);
            projectile.pathT -= projectile.speed * dt;

            Enemy target = enemyById(projectile.targetId);
            if (target == null) {
                target = nearestBelow(projectile.pathT, minPath);
                if (target != null) {
                    projectile.targetId = target.id;
                }
            }

            if (target != null && projectile.pathT <= target.pathT) {
                boolean killed = hit(projectile, target);
                if (killed && projectile.bouncesLeft > 0) {
                    Enemy next = nearestBelow(projectile.pathT, minPath);
                    if (next != null) {
                        projectile.bouncesLeft -= 1;
                        projectile.targetId = next.id;
                        continue;
                    }
                }
                projectiles.remove(i);
                continue;
            }
            // Bullets die at the edge of the tower's reach, so range stays meaningful.
            if (projectile.pathT <= minPath) {
                projectiles.remove(i);
            }
        }
    }

    /** Returns true when the hit killed the enemy. */
    private boolean hit(Projectile projectile, Enemy enemy) {
        double damage = projectile.damage
                * balance.distanceDamageBonus(levels[UpgradeId.REACH.ordinal()], enemy.pathT);

        hitCounter += 1;
        int knockbackEvery = balance.knockbackInterval(levels[UpgradeId.KNOCKBACK.ordinal()]);
        if (knockbackEvery > 0 && hitCounter % knockbackEvery == 0) {
            enemy.pathT = Math.max(0d, enemy.pathT - balance.knockbackDistance);
        }
        return applyDamage(enemy, damage);
    }

    /** Shared damage path so abilities, thorns and bullets all respect shields and payouts. */
    private boolean applyDamage(Enemy enemy, double rawDamage) {
        double damage = rawDamage;
        if (enemy.kind != EnemyKind.PROTECTOR) {
            damage /= balance.protectorShield(liveProtectors());
        }
        enemy.hp -= damage;
        if (enemy.hp <= 0) {
            payout(enemy);
            enemies.remove(enemy);
            return true;
        }
        return false;
    }

    private void payout(Enemy enemy) {
        double base = balance.coinsForKill(
                wave,
                tier,
                levels[UpgradeId.FORTUNE.ordinal()],
                levels[UpgradeId.CORE_COIN.ordinal()]
        );
        double golden = goldenRemaining > 0
                ? balance.goldenCoinMultiplier(levels[UpgradeId.ABILITY_GOLDEN.ordinal()])
                : 1d;
        coins += base
                * balance.kindCoinMultiplier(enemy.kind)
                * balance.labCoinMultiplier(labLevels[LabId.LAB_COINS.ordinal()])
                * balance.starMultiplier(levels[UpgradeId.STAR_COIN.ordinal()])
                * golden;
        shards += balance.shardsForKill(enemy.maxHp, levels[UpgradeId.HARVEST.ordinal()]);
        enemiesKilled += 1;
        if (enemy.isBoss()) {
            elitesKilled += 1;
        }
        evaluateMissions();
    }

    private int liveProtectors() {
        int count = 0;
        for (Enemy enemy : enemies) {
            if (enemy.kind == EnemyKind.PROTECTOR) {
                count += 1;
            }
        }
        return count;
    }

    private Enemy enemyById(int id) {
        for (Enemy enemy : enemies) {
            if (enemy.id == id) {
                return enemy;
            }
        }
        return null;
    }

    /**
     * Nearest enemy at or below the projectile but still inside range, so a bullet whose target
     * died connects with the next one instead of chasing fresh spawns down to the entrance.
     */
    private Enemy nearestBelow(double pathT, double minPath) {
        Enemy best = null;
        for (Enemy enemy : enemies) {
            if (enemy.pathT > pathT || enemy.pathT < minPath) {
                continue;
            }
            if (best == null || enemy.pathT > best.pathT) {
                best = enemy;
            }
        }
        return best;
    }

    // --- abilities ---

    private static UpgradeId upgradeFor(AbilityId id) {
        return switch (id) {
            case GOLDEN_TOWER -> UpgradeId.ABILITY_GOLDEN;
            case DEATH_WAVE -> UpgradeId.ABILITY_DEATH_WAVE;
            case CHAIN_LIGHTNING -> UpgradeId.ABILITY_CHAIN;
            case BLACK_HOLE -> UpgradeId.ABILITY_BLACK_HOLE;
        };
    }

    private void cast(AbilityId id) {
        int level = levels[upgradeFor(id).ordinal()];
        if (level == 0 || phase != RunPhase.RUNNING || abilityCooldowns[id.ordinal()] > 0) {
            return;
        }
        abilityCooldowns[id.ordinal()] = balance.abilityCooldown(id, level);

        switch (id) {
            case GOLDEN_TOWER -> goldenRemaining = balance.goldenDuration;
            case BLACK_HOLE -> {
                blackHoleRemaining = balance.blackHoleDuration;
                double pull = balance.blackHolePull(level);
                for (Enemy enemy : enemies) {
                    enemy.pathT = Math.max(0d, enemy.pathT - pull);
                }
            }
            case DEATH_WAVE -> {
                double damage = currentDamage() * balance.deathWaveDamage(level);
                for (int i = enemies.size() - 1; i >= 0; i--) {
                    if (i < enemies.size()) {
                        applyDamage(enemies.get(i), damage);
                    }
                }
            }
            case CHAIN_LIGHTNING -> {
                double damage = currentDamage() * balance.chainDamage(level);
                List<Enemy> targets = leadTargets(0d, balance.chainTargets(level));
                for (Enemy target : new ArrayList<>(targets)) {
                    applyDamage(target, damage);
                }
            }
        }
    }

    private String abilityEffect(AbilityId id, int level) {
        int shown = Math.max(1, level);
        return switch (id) {
            case GOLDEN_TOWER -> String.format(Locale.ROOT, "x%.0f coins for %.0fs",
                    balance.goldenCoinMultiplier(shown), balance.goldenDuration);
            case DEATH_WAVE -> String.format(Locale.ROOT, "x%.0f damage to all",
                    balance.deathWaveDamage(shown));
            case CHAIN_LIGHTNING -> String.format(Locale.ROOT, "%d targets, x%.0f",
                    balance.chainTargets(shown), balance.chainDamage(shown));
            case BLACK_HOLE -> String.format(Locale.ROOT, "pull %.0f%%, slow %.0fs",
                    balance.blackHolePull(shown) * 100, balance.blackHoleDuration);
        };
    }

    // --- labs ---

    private void startLab(LabId id) {
        if (runningLab != null || labLevels[id.ordinal()] >= balance.labMaxLevel(id)) {
            return;
        }
        double cost = balance.labCost(id, labLevels[id.ordinal()]);
        if (shards < cost) {
            return;
        }
        shards -= cost;
        runningLab = id;
        labStartedAtMillis = wallClockMillis;
        labEndsAtMillis = wallClockMillis + (long) (balance.labMinutes(id, labLevels[id.ordinal()]) * 60_000d);
    }

    private String labEffect(LabId id, int level) {
        return switch (id) {
            case LAB_DAMAGE -> String.format(Locale.ROOT, "x%.2f damage", balance.labDamageMultiplier(level));
            case LAB_COINS -> String.format(Locale.ROOT, "x%.2f coins", balance.labCoinMultiplier(level));
            case LAB_HEALTH -> String.format(Locale.ROOT, "x%.2f tower hp", balance.labHealthMultiplier(level));
            case LAB_MULTISHOT -> "+" + level + " bullet cap";
        };
    }

    // --- auto prestige ---

    private void setAutoPrestige(AutoPrestigeRule rule) {
        if (levels[UpgradeId.AUTO_PRESTIGE.ordinal()] == 0) {
            return;
        }
        autoPrestige = rule;
        compiledFormula = null;
        formulaError = null;
        if (rule.hasFormula() && levels[UpgradeId.FORMULA.ordinal()] > 0) {
            try {
                compiledFormula = PrestigeFormula.compile(rule.formula());
            } catch (RuntimeException e) {
                formulaError = e.getMessage();
            }
        }
    }

    private boolean autoPrestigeTriggers() {
        if (autoRunActive()) {
            AutoPrestigeRule farm = autoPrestige.enabled() ? autoPrestige
                    : new AutoPrestigeRule(true, 0, 0, 25, "");
            return farm.triggersSimply(wave, secondsSinceWaveChange, hpPercent());
        }
        if (levels[UpgradeId.AUTO_PRESTIGE.ordinal()] == 0 || !autoPrestige.enabled()) {
            return false;
        }
        if (levels[UpgradeId.FORMULA.ordinal()] > 0 && autoPrestige.hasFormula()) {
            if (compiledFormula == null) {
                return false;
            }
            try {
                return compiledFormula.evaluate(this::formulaVariable);
            } catch (RuntimeException e) {
                formulaError = e.getMessage();
                return false;
            }
        }
        return autoPrestige.triggersSimply(wave, secondsSinceWaveChange, hpPercent());
    }

    private double formulaVariable(String name) {
        return switch (name) {
            case "wave" -> wave;
            case "hp" -> towerHp;
            case "hpPct" -> hpPercent();
            case "coins" -> coins;
            case "shards" -> shards;
            case "stars" -> stars;
            case "cores" -> cores;
            case "sigils" -> sigils;
            case "stall" -> secondsSinceWaveChange;
            case "tier" -> tier;
            case "runs" -> runsCompleted;
            default -> throw new IllegalArgumentException("unknown name '" + name + "'");
        };
    }

    // --- upgrades ---

    private void awardMilestones() {
        int coreTotal = 0;
        int sigilTotal = 0;
        for (int t = 1; t <= balance.maxTier; t++) {
            coreTotal += balance.coresFor(t, bestWavePerTier[t]);
            sigilTotal += balance.sigilsFor(t, bestWavePerTier[t]);
        }
        if (coreTotal > coresAwarded) {
            cores += coreTotal - coresAwarded;
            coresAwarded = coreTotal;
        }
        if (sigilTotal > sigilsAwarded) {
            sigils += sigilTotal - sigilsAwarded;
            sigilsAwarded = sigilTotal;
        }
    }

    private void autoBuyLoop() {
        for (int i = 0; i < 24; i++) {
            UpgradeId pick = cheapestAffordableRunUpgrade();
            if (pick == null) {
                return;
            }
            buy(pick);
        }
    }

    private void autoBuyStars() {
        for (int i = 0; i < 8; i++) {
            UpgradeId pick = cheapestStarUpgrade();
            if (pick == null) {
                return;
            }
            buy(pick);
        }
    }

    private boolean autoRunActive() {
        return autoRun && levels[UpgradeId.AUTO_RUN.ordinal()] > 0;
    }

    private boolean autoBuyActive() {
        return autoRunActive() || (autoBuy && levels[UpgradeId.AUTO.ordinal()] > 0);
    }

    private void maybeResumeAutoRun() {
        if (!autoRunActive() || phase == RunPhase.RUNNING) {
            return;
        }
        int target = Math.max(1, Math.min(autoRunTier, highestTierUnlocked()));
        if (tier != target) {
            tier = target;
        }
        startRun();
    }

    /** Cheapest stat the player left enabled, so AUTO never touches a stat you turned off. */
    private UpgradeId cheapestAffordableRunUpgrade() {
        UpgradeId best = null;
        double bestCost = Double.POSITIVE_INFINITY;
        for (UpgradeId id : UpgradeId.values()) {
            if (!id.isRunUpgrade() || id.currency() != com.towerdefence.engine.model.Currency.COIN
                    || !autoTargets[id.ordinal()]) {
                continue;
            }
            double cost = balance.cost(id, levels[id.ordinal()]);
            if (cost <= coins && cost < bestCost) {
                best = id;
                bestCost = cost;
            }
        }
        return best;
    }

    private UpgradeId cheapestStarUpgrade() {
        UpgradeId best = null;
        double bestCost = Double.POSITIVE_INFINITY;
        for (UpgradeId id : PRESTIGE) {
            double cost = balance.cost(id, levels[id.ordinal()]);
            if (cost <= stars && cost < bestCost) {
                best = id;
                bestCost = cost;
            }
        }
        return best;
    }

    private static boolean isOneShot(UpgradeId id) {
        return id == UpgradeId.AUTO
                || id == UpgradeId.AUTO_PRESTIGE
                || id == UpgradeId.AUTO_RUN
                || id == UpgradeId.AUTO_CAST
                || id == UpgradeId.FORMULA;
    }

    private void buy(UpgradeId id) {
        int level = levels[id.ordinal()];
        if (isOneShot(id) && level > 0) {
            return;
        }
        if (id == UpgradeId.HEADSTART && level >= balance.headStartMaxLevel) {
            return;
        }
        if (id == UpgradeId.AUTO_RUN
                && (levels[UpgradeId.AUTO.ordinal()] == 0 || levels[UpgradeId.AUTO_PRESTIGE.ordinal()] == 0)) {
            return;
        }
        double cost = balance.cost(id, level);

        switch (id.currency()) {
            case COIN -> {
                if (phase != RunPhase.RUNNING || coins < cost) {
                    return;
                }
                coins -= cost;
            }
            case SHARD -> {
                if (shards < cost) {
                    return;
                }
                shards -= cost;
            }
            case CORE -> {
                if (cores < cost) {
                    return;
                }
                cores -= cost;
            }
            case SIGIL -> {
                if (sigils < cost) {
                    return;
                }
                sigils -= cost;
            }
            case CREST -> {
                if (crests < cost) {
                    return;
                }
                crests -= cost;
            }
            case STAR -> {
                if (phase != RunPhase.RUNNING || stars < cost) {
                    return;
                }
                stars -= cost;
            }
        }

        double hpBefore = maxHp();
        double wallBefore = currentWallMax();
        levels[id.ordinal()] = level + 1;
        if (id == UpgradeId.AUTO) {
            autoBuy = true;
            evaluateMissions();
        }
        if (id == UpgradeId.AUTO_RUN) {
            autoRun = true;
            autoRunTier = Math.max(1, tier);
        }
        if (id == UpgradeId.AUTO_CAST) {
            autoCast = true;
        }
        if (id == UpgradeId.AUTO_PRESTIGE) {
            autoPrestige = new AutoPrestigeRule(true, 0, 0, 25, "");
        }
        if (id == UpgradeId.WALL || id == UpgradeId.RAMPART) {
            wall += currentWallMax() - wallBefore;
        }
        if (phase == RunPhase.RUNNING) {
            towerHp = Math.min(maxHp(), towerHp + Math.max(0d, maxHp() - hpBefore));
        }
    }

    private int multishotCount() {
        return balance.multishotCount(levels[UpgradeId.MULTISHOT.ordinal()])
                + labLevels[LabId.LAB_MULTISHOT.ordinal()];
    }

    private double maxHp() {
        return balance.towerMaxHp(
                levels[UpgradeId.FOUNDATION.ordinal()],
                levels[UpgradeId.CORE_HP.ordinal()],
                levels[UpgradeId.HEALTH.ordinal()]
        ) * balance.labHealthMultiplier(labLevels[LabId.LAB_HEALTH.ordinal()])
                * balance.starMultiplier(levels[UpgradeId.STAR_HP.ordinal()]);
    }

    private double currentRegen() {
        return balance.regenPerSecond(
                levels[UpgradeId.FOUNDATION.ordinal()],
                levels[UpgradeId.CORE_HP.ordinal()],
                levels[UpgradeId.CORE_REGEN.ordinal()],
                levels[UpgradeId.REGEN.ordinal()],
                levels[UpgradeId.RECOVERY.ordinal()]
        ) * balance.starMultiplier(levels[UpgradeId.STAR_REGEN.ordinal()]);
    }

    private double currentWallMax() {
        return balance.wallMax(
                levels[UpgradeId.WALL.ordinal()],
                levels[UpgradeId.RAMPART.ordinal()]
        );
    }

    private double currentFireInterval() {
        return balance.fireInterval(
                levels[UpgradeId.TEMPO.ordinal()],
                levels[UpgradeId.OVERCLOCK.ordinal()]
        );
    }

    private double currentRange() {
        return balance.range(
                levels[UpgradeId.REACH.ordinal()],
                levels[UpgradeId.OPTICS.ordinal()]
        );
    }

    private double currentDamage() {
        return balance.damage(
                levels[UpgradeId.POWER.ordinal()],
                levels[UpgradeId.FOUNDATION.ordinal()],
                combatShards,
                levels[UpgradeId.CORE_DAMAGE.ordinal()]
        ) * balance.labDamageMultiplier(labLevels[LabId.LAB_DAMAGE.ordinal()])
                * balance.starMultiplier(levels[UpgradeId.STAR_DAMAGE.ordinal()]);
    }

    private UpgradeView view(UpgradeId id) {
        int level = levels[id.ordinal()];
        boolean purchased = isOneShot(id) && level > 0;
        double cost = purchased ? 0 : balance.cost(id, level);
        boolean affordable = !purchased && switch (id.currency()) {
            case COIN -> phase == RunPhase.RUNNING && coins >= cost;
            case SHARD -> shards >= cost;
            case CORE -> cores >= cost;
            case SIGIL -> sigils >= cost;
            case CREST -> crests >= cost;
            case STAR -> phase == RunPhase.RUNNING && stars >= cost;
        };
        return new UpgradeView(
                id,
                level,
                cost,
                id.currency(),
                affordable,
                purchased,
                effect(id, level),
                id.isRunUpgrade() && id.currency() == com.towerdefence.engine.model.Currency.COIN
                        && autoTargets[id.ordinal()],
                id.isRunUpgrade() && id.currency() == com.towerdefence.engine.model.Currency.COIN
        );
    }

    private String effect(UpgradeId id, int level) {
        return switch (id) {
            case POWER -> SciFormat.of(currentDamage()) + " dmg";
            case TEMPO -> String.format(Locale.ROOT, "%.2fs shot", currentFireInterval());
            case REACH -> String.format(Locale.ROOT, "%.0f%% lane", currentRange() * 100);
            case MULTISHOT -> multishotCount() + " bullets";
            case CRIT -> level == 0
                    ? "no crits"
                    : String.format(Locale.ROOT, "%.0f%% x%.1f",
                            balance.critChance(level) * 100, balance.critFactor);
            case BOUNCE -> level == 0 ? "no bounce" : balance.bounceCount(level) + " bounces";
            case HEALTH -> SciFormat.of(maxHp()) + " hp";
            case REGEN -> String.format(Locale.ROOT, "%.1f hp/s", currentRegen());
            case WALL -> currentWallMax() <= 0 ? "no wall" : SciFormat.of(currentWallMax()) + " wall";
            case WALL_REGEN -> String.format(Locale.ROOT, "%.0f wall/s", balance.wallRegen(level));
            case THORNS -> level == 0
                    ? "no spikes"
                    : String.format(Locale.ROOT, "%.0f dps close", balance.thornsDps(level));
            case KNOCKBACK -> level == 0 ? "no push" : "every " + balance.knockbackInterval(level) + " hits";
            case FOUNDATION -> SciFormat.of(balance.towerMaxHp(level,
                    levels[UpgradeId.CORE_HP.ordinal()], levels[UpgradeId.HEALTH.ordinal()])) + " hp";
            case FORTUNE -> String.format(Locale.ROOT, "x%.2f coins",
                    balance.coinMultiplier(level, levels[UpgradeId.CORE_COIN.ordinal()]));
            case AUTO -> level > 0
                    ? (autoBuy ? "on" : "off")
                    : "buys A stats for you";
            case AUTO_RUN -> level > 0
                    ? (autoRun ? "farming T" + autoRunTier : "paused")
                    : "needs AUTO BUY + AUTO PRESTIGE";
            case CORE_DAMAGE -> String.format(Locale.ROOT, "+%.0f%% damage", coreDamagePercent(level));
            case CORE_HP -> String.format(Locale.ROOT, "+%.0f%% tower hp", level * balance.coreHpPerLevel * 100);
            case CORE_REGEN -> String.format(Locale.ROOT, "+%.1f hp/s", level * balance.coreRegenPerLevel);
            case CORE_COIN -> String.format(Locale.ROOT, "+%.0f%% coins", level * balance.coreCoinPerLevel * 100);
            case GAME_SPEED -> String.format(Locale.ROOT, "x%.1f game speed", balance.gameSpeed(level));
            case ABILITY_GOLDEN -> abilityEffect(AbilityId.GOLDEN_TOWER, level);
            case ABILITY_DEATH_WAVE -> abilityEffect(AbilityId.DEATH_WAVE, level);
            case ABILITY_CHAIN -> abilityEffect(AbilityId.CHAIN_LIGHTNING, level);
            case ABILITY_BLACK_HOLE -> abilityEffect(AbilityId.BLACK_HOLE, level);
            case NIGHT_SHIFT -> formatHours(balance.offlineHours(level)) + " offline";
            case AUTO_PRESTIGE -> level > 0 ? "rules unlocked" : "prestige on its own";
            case AUTO_CAST -> level > 0 ? (autoCast ? "casting" : "paused") : "casts abilities for you";
            case FORMULA -> level > 0 ? "formula unlocked" : "write your own rule";
            case HEADSTART -> level == 0
                    ? "start later in the run"
                    : "start at wave " + balance.startingWave(level);
            case WAR_CHEST -> String.format(Locale.ROOT, "x%.2f start coins",
                    1d + balance.warChestPerLevel * level);
            case OVERCLOCK -> String.format(Locale.ROOT, "%.2fs shot", currentFireInterval());
            case OPTICS -> String.format(Locale.ROOT, "%.0f%% lane", currentRange() * 100);
            case RECOVERY -> String.format(Locale.ROOT, "%.1f hp/s", currentRegen());
            case RAMPART -> currentWallMax() <= 0 ? "no wall" : SciFormat.of(currentWallMax()) + " wall";
            case HARVEST -> String.format(Locale.ROOT, "+%.0f%% shards/kill",
                    level * balance.harvestPerLevel * 100);
            case STAR_DAMAGE, STAR_HP, STAR_COIN, STAR_REGEN -> String.format(Locale.ROOT, "x%.2f > x%.2f",
                    balance.starMultiplier(level), balance.starMultiplier(level + 1));
        };
    }

    private double hpPercent() {
        return towerHp / Math.max(1d, maxHp()) * 100d;
    }

    private int clearedTierCount() {
        int count = 0;
        for (boolean cleared : tiersCleared) {
            if (cleared) {
                count += 1;
            }
        }
        return count;
    }

    private List<MissionView> missionViews() {
        List<MissionView> out = new ArrayList<>(MissionId.values().length);
        for (MissionId id : MissionId.values()) {
            out.add(new MissionView(
                    id,
                    missionTitle(id),
                    missionDetail(id),
                    id.crests(),
                    missionsDone[id.ordinal()],
                    missionProgress(id),
                    id.target()
            ));
        }
        return List.copyOf(out);
    }

    private void evaluateMissions() {
        for (MissionId id : MissionId.values()) {
            if (missionsDone[id.ordinal()]) {
                continue;
            }
            if (missionProgress(id) >= id.target()) {
                missionsDone[id.ordinal()] = true;
                crests += id.crests();
            }
        }
    }

    private double missionProgress(MissionId id) {
        return switch (id) {
            case WAVE_30, WAVE_80, WAVE_200, WAVE_500, WAVE_2000 -> bestWaveOverall();
            case CLEAR_TIER -> clearedTierCount();
            case TIER_2, TIER_4 -> highestTierUnlocked();
            case PRESTIGE_1, PRESTIGE_5, PRESTIGE_20 -> prestiges;
            case KILL_100, KILL_1000 -> enemiesKilled;
            case ELITE_15 -> elitesKilled;
            case FIRST_LAB -> labsFinished;
            case UNLOCK_AUTO -> levels[UpgradeId.AUTO.ordinal()] > 0 ? 1 : 0;
        };
    }

    private static String missionTitle(MissionId id) {
        return switch (id) {
            case WAVE_30 -> "Reach wave 30";
            case WAVE_80 -> "Reach wave 80";
            case WAVE_200 -> "Reach wave 200";
            case WAVE_500 -> "Reach wave 500";
            case WAVE_2000 -> "Reach wave 2000";
            case CLEAR_TIER -> "Clear a tier";
            case TIER_2 -> "Unlock tier 2";
            case TIER_4 -> "Unlock tier 4";
            case PRESTIGE_1 -> "Prestige once";
            case PRESTIGE_5 -> "Prestige 5 times";
            case PRESTIGE_20 -> "Prestige 20 times";
            case KILL_100 -> "Kill 100 enemies";
            case KILL_1000 -> "Kill 1,000 enemies";
            case ELITE_15 -> "Kill 15 bosses";
            case FIRST_LAB -> "Finish a lab";
            case UNLOCK_AUTO -> "Unlock AUTO";
        };
    }

    private String missionDetail(MissionId id) {
        return switch (id) {
            case WAVE_30, WAVE_80, WAVE_200, WAVE_500, WAVE_2000 -> "best wave in any tier";
            case CLEAR_TIER -> "finish all " + balance.wavesPerTier + " waves on a floor";
            case TIER_2, TIER_4 -> "reach the unlock wave on the previous floor";
            case PRESTIGE_1, PRESTIGE_5, PRESTIGE_20 -> "cash out a live run";
            case KILL_100, KILL_1000 -> "any enemy kind";
            case ELITE_15 -> "the boss that closes a tenth wave";
            case FIRST_LAB -> "research that finishes on the real clock";
            case UNLOCK_AUTO -> "buy AUTO BUY with sigils";
        };
    }

    private static String formatHours(double hours) {
        if (hours < 1) {
            return String.format(Locale.ROOT, "%.0fmin", hours * 60);
        }
        return String.format(Locale.ROOT, "%.0fh", hours);
    }

    private double coreDamagePercent(int level) {
        return (Math.pow(1.0 + balance.coreDamagePerLevel, level) - 1.0) * 100.0;
    }

    // --- persistence ---

    public SaveData toSave(long wallClockMillisNow) {
        List<SaveData.EnemySave> savedEnemies = new ArrayList<>(enemies.size());
        for (Enemy enemy : enemies) {
            savedEnemies.add(new SaveData.EnemySave(
                    enemy.id, enemy.kind, enemy.pathT, enemy.hp, enemy.maxHp, enemy.speed
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
                missionsDone.clone(),
                tiersCleared.clone(),
                towerHp,
                wall,
                levels.clone(),
                bestWavePerTier.clone(),
                autoTargets.clone(),
                autoBuy,
                autoPrestige,
                autoCast,
                autoRun,
                autoRunTier,
                abilityCooldowns.clone(),
                goldenRemaining,
                blackHoleRemaining,
                labLevels.clone(),
                runningLab == null ? -1 : runningLab.ordinal(),
                labEndsAtMillis,
                labStartedAtMillis,
                runsCompleted,
                remainingToSpawn,
                spawnedInWave,
                spawnCooldown,
                waveBreak,
                fireCooldown,
                nextEnemyId,
                wallClockMillisNow,
                List.copyOf(savedEnemies)
        );
    }

    public static LocalGameSession fromSave(GameBalance balance, SaveData save) {
        LocalGameSession session = new LocalGameSession(balance);
        session.phase = save.phase();
        session.wave = save.wave();
        session.tier = Math.max(1, Math.min(balance.maxTier, save.tier()));
        session.coins = save.coins();
        session.shards = save.shards();
        session.combatShards = save.shards();
        session.cores = save.cores();
        session.sigils = save.sigils();
        session.crests = save.crests();
        session.stars = save.stars();
        session.lastRunWave = save.lastRunWave();
        session.coresAwarded = save.coresAwarded();
        session.sigilsAwarded = save.sigilsAwarded();
        session.prestiges = save.prestiges();
        session.enemiesKilled = save.enemiesKilled();
        session.elitesKilled = save.elitesKilled();
        session.labsFinished = save.labsFinished();
        copyInto(save.missionsDone(), session.missionsDone);
        copyInto(save.tiersCleared(), session.tiersCleared);
        session.towerHp = save.towerHp();
        session.wall = save.wall();
        copyInto(save.levels(), session.levels);
        copyInto(save.bestWavePerTier(), session.bestWavePerTier);
        copyInto(save.labLevels(), session.labLevels);
        boolean[] savedTargets = save.autoTargets();
        System.arraycopy(savedTargets, 0, session.autoTargets, 0,
                Math.min(savedTargets.length, session.autoTargets.length));
        double[] savedCooldowns = save.abilityCooldowns();
        System.arraycopy(savedCooldowns, 0, session.abilityCooldowns, 0,
                Math.min(savedCooldowns.length, session.abilityCooldowns.length));
        session.autoBuy = save.autoBuy();
        session.autoCast = save.autoCast();
        session.autoRun = save.autoRun();
        session.autoRunTier = Math.max(1, save.autoRunTier());
        session.goldenRemaining = save.goldenRemaining();
        session.blackHoleRemaining = save.blackHoleRemaining();
        session.runningLab = save.runningLabOrdinal() < 0
                ? null
                : LabId.values()[save.runningLabOrdinal()];
        session.labEndsAtMillis = save.labEndsAtMillis();
        session.labStartedAtMillis = save.labStartedAtMillis();
        session.wallClockMillis = save.wallClockMillis();
        session.runsCompleted = save.runsCompleted();
        session.remainingToSpawn = save.remainingToSpawn();
        session.spawnedInWave = save.spawnedInWave();
        session.spawnCooldown = save.spawnCooldown();
        session.waveBreak = save.waveBreak();
        session.fireCooldown = save.fireCooldown();
        session.nextEnemyId = save.nextEnemyId();
        session.enemies.clear();
        for (SaveData.EnemySave enemy : save.enemies()) {
            Enemy restored = new Enemy(enemy.id(), enemy.kind(), enemy.maxHp(), enemy.speed());
            restored.pathT = enemy.pathT();
            restored.hp = enemy.hp();
            session.enemies.add(restored);
        }
        // Recompile so a saved formula keeps working, and so a broken one reports again.
        session.autoPrestige = AutoPrestigeRule.off();
        AutoPrestigeRule saved = save.autoPrestige();
        if (session.levels[UpgradeId.AUTO_PRESTIGE.ordinal()] > 0) {
            session.setAutoPrestige(saved);
        }
        return session;
    }

    private static void copyInto(int[] source, int[] target) {
        System.arraycopy(source, 0, target, 0, Math.min(source.length, target.length));
    }

    private static void copyInto(boolean[] source, boolean[] target) {
        if (source == null) {
            return;
        }
        System.arraycopy(source, 0, target, 0, Math.min(source.length, target.length));
    }
}
