package com.towerdefence.engine.session;

import com.towerdefence.engine.command.UpgradeId;
import com.towerdefence.engine.model.Currency;

public record UpgradeView(
        UpgradeId id,
        int level,
        double cost,
        Currency currency,
        boolean affordable,
        boolean purchased,
        String effect,
        /** True when the auto buyer may spend on this stat; only meaningful for run upgrades. */
        boolean autoTarget,
        boolean autoEligible
) {}
