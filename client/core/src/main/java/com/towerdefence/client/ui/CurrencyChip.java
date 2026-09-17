package com.towerdefence.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.towerdefence.engine.model.Currency;

/**
 * The three currencies share one framed widget so they read as the same kind of thing,
 * each with its own shape: coins are discs, shards are diamonds, cores are rings.
 */
public final class CurrencyChip {
    private CurrencyChip() {}

    public static Color color(Currency currency) {
        return switch (currency) {
            case COIN -> Theme.GOLD;
            case SHARD -> Theme.SHARD;
            case CORE -> Theme.ACCENT;
            case SIGIL -> Theme.SIGIL;
            case CREST -> Theme.CREST;
            case STAR -> Theme.STAR;
        };
    }

    public static String shortName(Currency currency) {
        return switch (currency) {
            case COIN -> "COINS";
            case SHARD -> "SHARDS";
            case CORE -> "CORES";
            case SIGIL -> "SIGILS";
            case CREST -> "CRESTS";
            case STAR -> "PRESTIGE";
        };
    }

    public static void icon(Batch batch, Ui ui, Currency currency, float cx, float cy, float radius) {
        Color color = color(currency);
        ui.glow(batch, cx, cy, radius * 2.1f, color, 0.28f);
        switch (currency) {
            case COIN -> {
                ui.circle(batch, cx, cy, radius, color, 1f);
                ui.circle(batch, cx, cy, radius * 0.55f, Theme.BG_TOP, 0.35f);
            }
            case SHARD -> {
                ui.diamond(batch, cx, cy, radius * 1.1f, color, 1f);
                ui.diamond(batch, cx, cy, radius * 0.5f, Theme.BG_TOP, 0.4f);
            }
            case CORE -> {
                ui.ring(batch, cx, cy, radius * 1.05f, color, 1f);
                ui.circle(batch, cx, cy, radius * 0.34f, color, 1f);
            }
            case SIGIL -> {
                ui.triangle(batch, cx, cy - radius * 0.15f, radius * 1.15f, color, 1f);
                ui.triangle(batch, cx, cy - radius * 0.3f, radius * 0.5f, Theme.BG_TOP, 0.45f);
            }
            case CREST -> {
                ui.diamond(batch, cx, cy, radius * 1.15f, color, 1f);
                ui.ring(batch, cx, cy, radius * 0.7f, Theme.BG_TOP, 1f);
                ui.circle(batch, cx, cy, radius * 0.28f, color, 1f);
            }
            case STAR -> {
                ui.triangle(batch, cx, cy + radius * 0.1f, radius * 1.2f, color, 1f);
                ui.diamond(batch, cx, cy - radius * 0.15f, radius * 0.55f, Theme.BG_TOP, 0.5f);
            }
        }
    }

    /** Wide chip with icon, value and a caption, for the menu. */
    public static void tall(Batch batch, Ui ui, float x, float y, float w, float h,
                            Currency currency, String value, String caption, float alpha) {
        Color color = color(currency);
        ui.panelOutlined(batch, x, y, w, h, Theme.SURFACE_LO, Theme.STROKE, 1.5f);
        icon(batch, ui, currency, x + 24f, y + h - 24f, 9f);
        ui.textFit(batch, shortName(currency), x + 40f, y + h - 24f, w - 50f, Theme.TEXT_TINY,
                ui.fade(Theme.TEXT_DIM, alpha), Ui.LEFT);
        ui.textFit(batch, value, x + Theme.GAP, y + h / 2f - 12f, w - Theme.GAP * 2f, Theme.TEXT_H1,
                ui.fade(color, alpha), Ui.LEFT);
        ui.textFit(batch, caption, x + Theme.GAP, y + 14f, w - Theme.GAP * 2f, Theme.TEXT_TINY,
                ui.fade(Theme.TEXT_DIM, alpha * 0.85f), Ui.LEFT);
    }

    /** Short chip with icon and value only, for the in-game HUD. */
    public static void compact(Batch batch, Ui ui, float x, float y, float w, float h,
                               Currency currency, String value) {
        Color color = color(currency);
        ui.panelOutlined(batch, x, y, w, h, Theme.SURFACE, Theme.STROKE, 1.2f);
        float cy = y + h / 2f;
        icon(batch, ui, currency, x + 15f, cy, 7f);
        ui.textFit(batch, value, x + w - 9f, cy, w - 34f, Theme.TEXT_SMALL, color, Ui.RIGHT);
    }
}
