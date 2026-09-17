package com.towerdefence.engine.command;

import com.towerdefence.engine.model.Currency;

public enum UpgradeId {
    // Attack, bought with coins inside a run.
    POWER(Currency.COIN),
    TEMPO(Currency.COIN),
    REACH(Currency.COIN),
    MULTISHOT(Currency.COIN),
    CRIT(Currency.COIN),
    BOUNCE(Currency.COIN),

    // Defence, also in-run.
    HEALTH(Currency.COIN),
    REGEN(Currency.COIN),
    WALL(Currency.COIN),
    WALL_REGEN(Currency.COIN),
    THORNS(Currency.COIN),
    KNOCKBACK(Currency.COIN),

    // Meta, kept between runs.
    FOUNDATION(Currency.SHARD),
    FORTUNE(Currency.SHARD),
    AUTO(Currency.SIGIL),

    CORE_DAMAGE(Currency.CORE),
    CORE_HP(Currency.CORE),
    CORE_REGEN(Currency.CORE),
    CORE_COIN(Currency.CORE),
    GAME_SPEED(Currency.CORE),

    // Ultimate abilities: level 0 means locked, every level after that sharpens them.
    ABILITY_GOLDEN(Currency.CORE),
    ABILITY_DEATH_WAVE(Currency.CORE),
    ABILITY_CHAIN(Currency.CORE),
    ABILITY_BLACK_HOLE(Currency.CORE),

    /** Offline window. Deliberately gated behind the rare currency. */
    NIGHT_SHIFT(Currency.SIGIL),
    AUTO_PRESTIGE(Currency.SIGIL),
    AUTO_CAST(Currency.SIGIL),
    FORMULA(Currency.SIGIL),

    /** Crest shop: skip the first waves of every run. */
    HEADSTART(Currency.CREST),
    /** Crest shop: more coins in hand when a run begins. */
    WAR_CHEST(Currency.CREST),

    /** Workshop: persistent fire rate. */
    OVERCLOCK(Currency.SHARD),
    /** Workshop: persistent range. */
    OPTICS(Currency.SHARD),
    /** Workshop: persistent regen. */
    RECOVERY(Currency.SHARD),
    /** Workshop: persistent wall. */
    RAMPART(Currency.SHARD),
    /** Workshop: more shards from each kill. */
    HARVEST(Currency.SHARD),

    /** Prestige tree: x1.02 per level on one stat. */
    STAR_DAMAGE(Currency.STAR),
    STAR_HP(Currency.STAR),
    STAR_COIN(Currency.STAR),
    STAR_REGEN(Currency.STAR),

    /** Farms a chosen tier using auto-buy and auto-prestige. */
    AUTO_RUN(Currency.SIGIL);

    private final Currency currency;

    UpgradeId(Currency currency) {
        this.currency = currency;
    }

    public Currency currency() {
        return currency;
    }

    public boolean isPermanent() {
        return currency == Currency.CORE || currency == Currency.SIGIL;
    }

    /** Coin and prestige-run upgrades are wiped when a run ends. */
    public boolean isRunUpgrade() {
        return currency == Currency.COIN || currency == Currency.STAR;
    }
}
