package com.towerdefence.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Disposable;

/**
 * Generates the UI font at the window's real pixel density instead of upscaling a fixed bitmap.
 * Layout keeps working in world units: callers ask for the same scales as before and
 * {@link #compensation()} cancels out the extra glyph resolution.
 */
public final class FontKit implements Disposable {
    /** Glyph height in world units; every text scale in {@link Theme} is relative to this. */
    private static final int BASE_UNITS = 16;
    private static final float MIN_STEP = 0.2f;

    private final FreeTypeFontGenerator generator;
    private BitmapFont font;
    private float pixelsPerUnit;

    public FontKit() {
        generator = new FreeTypeFontGenerator(Gdx.files.classpath("ui-font.ttf"));
        rebuild(1f);
    }

    /** Rebuilds the font only when the density moved enough to matter. */
    public void densityChanged(float newPixelsPerUnit) {
        float clamped = Math.max(0.5f, Math.min(6f, newPixelsPerUnit));
        if (Math.abs(clamped - pixelsPerUnit) < MIN_STEP) {
            return;
        }
        rebuild(clamped);
    }

    private void rebuild(float newPixelsPerUnit) {
        if (font != null) {
            font.dispose();
        }
        pixelsPerUnit = newPixelsPerUnit;
        FreeTypeFontGenerator.FreeTypeFontParameter parameter =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = Math.max(8, Math.round(BASE_UNITS * pixelsPerUnit));
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        parameter.hinting = FreeTypeFontGenerator.Hinting.Slight;
        font = generator.generateFont(parameter);
        font.setUseIntegerPositions(false);
    }

    public BitmapFont font() {
        return font;
    }

    /** Multiply requested scales by this so a denser font still draws at the same world size. */
    public float compensation() {
        return 1f / pixelsPerUnit;
    }

    @Override
    public void dispose() {
        if (font != null) {
            font.dispose();
        }
        generator.dispose();
    }
}
