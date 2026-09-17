package com.towerdefence.engine.model;

public enum Currency {
    /** Earned during a run, wiped when the run ends. */
    COIN,
    /** Farmed from kills (fatter enemies pay more). Workshop and labs. */
    SHARD,
    /** New records only. The important currency: permanent combat upgrades. */
    CORE,
    /** Rare: one batch per 25-wave record. Buys automation and offline time. */
    SIGIL,
    /** Earned from missions and tier clears, never from just surviving waves. */
    CREST,
    /** Earned on prestige. Buys tiny permanent multipliers. */
    STAR
}
