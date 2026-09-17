package com.towerdefence.client.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;

/** Slow drifting motes over a gradient, shared by every screen so transitions feel continuous. */
public final class Backdrop {
    private static final int COUNT = 46;

    private final float[] x = new float[COUNT];
    private final float[] y = new float[COUNT];
    private final float[] radius = new float[COUNT];
    private final float[] speed = new float[COUNT];
    private final float[] alpha = new float[COUNT];
    private final float width;
    private final float height;

    public Backdrop(float width, float height) {
        this.width = width;
        this.height = height;
        for (int i = 0; i < COUNT; i++) {
            x[i] = MathUtils.random(width);
            y[i] = MathUtils.random(height);
            radius[i] = MathUtils.random(1.2f, 3.4f);
            speed[i] = MathUtils.random(4f, 16f);
            alpha[i] = MathUtils.random(0.06f, 0.22f);
        }
    }

    public void update(float delta) {
        for (int i = 0; i < COUNT; i++) {
            y[i] -= speed[i] * delta;
            if (y[i] < -4f) {
                y[i] = height + 4f;
                x[i] = MathUtils.random(width);
            }
        }
    }

    public void draw(Batch batch, Ui ui) {
        ui.verticalGradient(batch, 0, 0, width, height, Theme.BG_BOTTOM, Theme.BG_TOP, 24);
        for (int i = 0; i < COUNT; i++) {
            ui.circle(batch, x[i], y[i], radius[i], Theme.ACCENT, alpha[i]);
        }
    }
}
