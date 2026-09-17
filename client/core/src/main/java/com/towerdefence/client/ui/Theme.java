package com.towerdefence.client.ui;

import com.badlogic.gdx.graphics.Color;

/** Single source of truth for colors and spacing so screens stay consistent. */
public final class Theme {
    public static final Color BG_TOP = rgb(0x0C1017);
    public static final Color BG_BOTTOM = rgb(0x151C28);
    public static final Color SURFACE = rgb(0x18202E);
    public static final Color SURFACE_HI = rgb(0x212C3E);
    public static final Color SURFACE_LO = rgb(0x121925);
    public static final Color STROKE = rgb(0x2C3B52);
    public static final Color TEXT = rgb(0xE8EEF7);
    public static final Color TEXT_DIM = rgb(0x8496AE);
    public static final Color ACCENT = rgb(0x4CC9F0);
    public static final Color GOLD = rgb(0xF2C14E);
    public static final Color SHARD = rgb(0xB48CFF);
    public static final Color SIGIL = rgb(0xFF7AC6);
    public static final Color CREST = rgb(0xFF8A4C);
    public static final Color STAR = rgb(0xF4E08A);
    public static final Color DANGER = rgb(0xE5484D);
    public static final Color OK = rgb(0x3DD68C);
    public static final Color LANE = rgb(0x161E2B);
    public static final Color ELITE = rgb(0xFF9F45);
    public static final Color ENEMY_FAST = rgb(0xFFD166);
    public static final Color ENEMY_TANK = rgb(0x9B5DE5);
    public static final Color ENEMY_RANGED = rgb(0xFF6B9D);
    public static final Color ENEMY_PROTECTOR = rgb(0x4ADEBB);
    public static final Color WALL = rgb(0x7FB2FF);

    public static final float PAD = 14f;
    public static final float GAP = 10f;
    public static final float RADIUS = 16f;

    public static final float TEXT_TITLE = 1.9f;
    public static final float TEXT_H1 = 1.25f;
    public static final float TEXT_BODY = 0.95f;
    public static final float TEXT_SMALL = 0.8f;
    public static final float TEXT_TINY = 0.7f;

    private Theme() {}

    private static Color rgb(int hex) {
        return new Color(
                ((hex >> 16) & 0xFF) / 255f,
                ((hex >> 8) & 0xFF) / 255f,
                (hex & 0xFF) / 255f,
                1f
        );
    }

    /** Smooth 0..1 ease used by the intro and press animations. */
    public static float easeOut(float t) {
        float c = Math.max(0f, Math.min(1f, t));
        return 1f - (1f - c) * (1f - c) * (1f - c);
    }
}
