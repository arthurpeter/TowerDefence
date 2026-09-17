package com.towerdefence.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;

public final class Button {
    public enum Style { PRIMARY, GHOST }

    public final Rectangle bounds = new Rectangle();
    public String label;
    public Style style;
    public Color accent = Theme.ACCENT;
    public boolean enabled = true;

    private float press;

    public Button(String label, Style style) {
        this.label = label;
        this.style = style;
    }

    public void update(float delta) {
        press = Math.max(0f, press - delta * 4f);
    }

    public boolean touch(float x, float y) {
        if (!enabled || !bounds.contains(x, y)) {
            return false;
        }
        press = 1f;
        return true;
    }

    public void draw(Batch batch, Ui ui, float textScale) {
        float squash = press * 2.5f;
        float x = bounds.x + squash;
        float y = bounds.y + squash * 0.5f;
        float w = bounds.width - squash * 2f;
        float h = bounds.height - squash;
        float alpha = enabled ? 1f : 0.45f;

        if (style == Style.PRIMARY) {
            ui.glow(batch, x + w / 2f, y + h / 2f, w * 0.62f, accent, 0.22f * alpha);
            ui.panel(batch, x, y, w, h, accent, alpha);
            ui.panel(batch, x, y + h * 0.42f, w, h * 0.58f, Theme.TEXT, 0.10f * alpha);
            ui.textFit(batch, label, x + w / 2f, y + h / 2f, w - Theme.PAD * 2f, textScale, Theme.BG_TOP, Ui.CENTER);
        } else {
            ui.panelOutlined(batch, x, y, w, h, Theme.SURFACE, Theme.STROKE, 1.5f);
            Color text = enabled ? Theme.TEXT : Theme.TEXT_DIM;
            ui.textFit(batch, label, x + w / 2f, y + h / 2f, w - Theme.PAD * 2f, textScale, text, Ui.CENTER);
        }
    }
}
