package com.towerdefence.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.utils.Disposable;

/**
 * Tiny drawing toolkit built on generated textures, so the game needs no art assets.
 * Everything goes through one SpriteBatch, which keeps shapes and text correctly layered.
 */
public final class Ui implements Disposable {
    public static final int LEFT = -1;
    public static final int CENTER = 0;
    public static final int RIGHT = 1;

    private static final String ELLIPSIS = "..";
    private static final float MIN_FIT = 0.6f;
    private static final int SMALL_RADIUS = 5;

    private final FontKit fonts;
    private final GlyphLayout layout = new GlyphLayout();
    private final Texture pixel;
    private final Texture disc;
    private final Texture glow;
    private final Texture diamond;
    private final Texture ring;
    private final Texture triangle;
    private final Texture roundTex;
    private final Texture roundSmallTex;
    private final NinePatch round;
    private final NinePatch roundSmall;
    private final Color tint = new Color();
    private final Color fadeColor = new Color();

    public Ui() {
        fonts = new FontKit();
        pixel = solid();
        disc = disc(128);
        glow = glow(128);
        diamond = diamond(128);
        ring = ring(128, 0.62f);
        triangle = triangle(128);
        roundTex = rounded((int) Theme.RADIUS);
        round = new NinePatch(roundTex, (int) Theme.RADIUS, (int) Theme.RADIUS, (int) Theme.RADIUS, (int) Theme.RADIUS);
        roundSmallTex = rounded(SMALL_RADIUS);
        roundSmall = new NinePatch(roundSmallTex, SMALL_RADIUS, SMALL_RADIUS, SMALL_RADIUS, SMALL_RADIUS);
    }

    /** Scratch color for fading text; valid until the next call. */
    public Color fade(Color color, float alpha) {
        return fadeColor.set(color.r, color.g, color.b, color.a * alpha);
    }

    /** Call on resize so glyphs are rasterised at the window's real resolution. */
    public void densityChanged(float pixelsPerWorldUnit) {
        fonts.densityChanged(pixelsPerWorldUnit);
    }

    private BitmapFont font() {
        return fonts.font();
    }

    private void applyScale(float scale) {
        fonts.font().getData().setScale(scale * fonts.compensation());
    }

    // --- shapes ---

    public void rect(Batch batch, float x, float y, float w, float h, Color color) {
        rect(batch, x, y, w, h, color, 1f);
    }

    public void rect(Batch batch, float x, float y, float w, float h, Color color, float alpha) {
        batch.setColor(tint.set(color.r, color.g, color.b, color.a * alpha));
        batch.draw(pixel, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    public void panel(Batch batch, float x, float y, float w, float h, Color color) {
        panel(batch, x, y, w, h, color, 1f);
    }

    /**
     * A NinePatch refuses to render thinner than its corner caps, so pick a patch that fits the
     * box and fall back to a plain quad for hairlines. Without this, thin bars render oversized.
     */
    public void panel(Batch batch, float x, float y, float w, float h, Color color, float alpha) {
        float min = Math.min(w, h);
        NinePatch patch = min >= Theme.RADIUS * 2f ? round : min >= SMALL_RADIUS * 2f ? roundSmall : null;
        if (patch == null) {
            rect(batch, x, y, w, h, color, alpha);
            return;
        }
        patch.setColor(tint.set(color.r, color.g, color.b, color.a * alpha));
        patch.draw(batch, x, y, w, h);
        patch.setColor(Color.WHITE);
    }

    /** Panel with a 1px-style outline drawn as a slightly larger panel behind it. */
    public void panelOutlined(Batch batch, float x, float y, float w, float h, Color fill, Color stroke, float weight) {
        panel(batch, x - weight, y - weight, w + weight * 2f, h + weight * 2f, stroke);
        panel(batch, x, y, w, h, fill);
    }

    public void circle(Batch batch, float cx, float cy, float radius, Color color, float alpha) {
        batch.setColor(tint.set(color.r, color.g, color.b, color.a * alpha));
        batch.draw(disc, cx - radius, cy - radius, radius * 2f, radius * 2f);
        batch.setColor(Color.WHITE);
    }

    public void glow(Batch batch, float cx, float cy, float radius, Color color, float alpha) {
        batch.setColor(tint.set(color.r, color.g, color.b, color.a * alpha));
        batch.draw(glow, cx - radius, cy - radius, radius * 2f, radius * 2f);
        batch.setColor(Color.WHITE);
    }

    public void diamond(Batch batch, float cx, float cy, float radius, Color color, float alpha) {
        batch.setColor(tint.set(color.r, color.g, color.b, color.a * alpha));
        batch.draw(diamond, cx - radius, cy - radius, radius * 2f, radius * 2f);
        batch.setColor(Color.WHITE);
    }

    public void ring(Batch batch, float cx, float cy, float radius, Color color, float alpha) {
        batch.setColor(tint.set(color.r, color.g, color.b, color.a * alpha));
        batch.draw(ring, cx - radius, cy - radius, radius * 2f, radius * 2f);
        batch.setColor(Color.WHITE);
    }

    public void triangle(Batch batch, float cx, float cy, float radius, Color color, float alpha) {
        batch.setColor(tint.set(color.r, color.g, color.b, color.a * alpha));
        batch.draw(triangle, cx - radius, cy - radius, radius * 2f, radius * 2f);
        batch.setColor(Color.WHITE);
    }

    public void bar(Batch batch, float x, float y, float w, float h, float ratio, Color track, Color fill) {
        float clamped = Math.max(0f, Math.min(1f, ratio));
        panel(batch, x, y, w, h, track);
        if (clamped > 0f) {
            float fw = Math.max(h, w * clamped);
            panel(batch, x, y, fw, h, fill);
        }
    }

    /** Vertical gradient approximated with stacked bands; cheap and good enough for a backdrop. */
    public void verticalGradient(Batch batch, float x, float y, float w, float h, Color bottom, Color top, int bands) {
        float bh = h / bands;
        for (int i = 0; i < bands; i++) {
            float t = i / (float) (bands - 1);
            tint.set(
                    bottom.r + (top.r - bottom.r) * t,
                    bottom.g + (top.g - bottom.g) * t,
                    bottom.b + (top.b - bottom.b) * t,
                    1f
            );
            batch.setColor(tint);
            batch.draw(pixel, x, y + i * bh, w, bh + 1f);
        }
        batch.setColor(Color.WHITE);
    }

    // --- text ---

    public float textWidth(String text, float scale) {
        applyScale(scale);
        layout.setText(font(), text);
        return layout.width;
    }

    public float textHeight(String text, float scale) {
        applyScale(scale);
        layout.setText(font(), text);
        return layout.height;
    }

    /**
     * Draws text anchored horizontally by {@code align}, vertically centered on {@code centerY}.
     * The font color must be set before measuring: GlyphLayout bakes the color into its runs.
     */
    public void text(Batch batch, String text, float x, float centerY, float scale, Color color, int align) {
        font().setColor(color);
        applyScale(scale);
        layout.setText(font(), text);
        font().draw(batch, layout, anchor(x, align), centerY + layout.height / 2f);
    }

    /**
     * Same as {@link #text} but guarantees the result stays inside {@code maxWidth}: first by shrinking
     * the scale, then by truncating. This is what keeps labels from spilling out of their boxes.
     */
    public void textFit(Batch batch, String text, float x, float centerY, float maxWidth, float scale, Color color, int align) {
        font().setColor(color);
        applyScale(scale);
        layout.setText(font(), text);
        if (layout.width <= maxWidth) {
            font().draw(batch, layout, anchor(x, align), centerY + layout.height / 2f);
            return;
        }

        applyScale(Math.max(scale * MIN_FIT, scale * maxWidth / layout.width));
        layout.setText(font(), text);

        String shown = text;
        while (layout.width > maxWidth && shown.length() > 1) {
            shown = shown.substring(0, shown.length() - 1);
            layout.setText(font(), shown + ELLIPSIS);
        }
        if (shown.length() != text.length()) {
            shown = shown + ELLIPSIS;
        }
        layout.setText(font(), shown);
        font().draw(batch, layout, anchor(x, align), centerY + layout.height / 2f);
    }

    /** Letter-spaced text, used for headings so the default font reads as intentional. */
    public void textTracked(Batch batch, String text, float centerX, float centerY, float scale, float tracking, Color color) {
        font().setColor(color);
        applyScale(scale);
        float total = 0f;
        for (int i = 0; i < text.length(); i++) {
            total += charAdvance(text.charAt(i)) + (i < text.length() - 1 ? tracking : 0f);
        }
        float x = centerX - total / 2f;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != ' ') {
                layout.setText(font(), String.valueOf(c));
                font().draw(batch, layout, x, centerY + layout.height / 2f);
            }
            x += charAdvance(c) + tracking;
        }
    }

    private float charAdvance(char c) {
        if (c == ' ') {
            return font().getData().spaceXadvance;
        }
        layout.setText(font(), String.valueOf(c));
        return layout.width;
    }

    private float anchor(float x, int align) {
        return switch (align) {
            case CENTER -> x - layout.width / 2f;
            case RIGHT -> x - layout.width;
            default -> x;
        };
    }

    // --- generated textures ---

    private static Texture solid() {
        Pixmap map = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        map.setColor(Color.WHITE);
        map.fill();
        Texture texture = new Texture(map);
        map.dispose();
        return texture;
    }

    private static Texture disc(int size) {
        Pixmap map = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        map.setBlending(Pixmap.Blending.None);
        map.setColor(1f, 1f, 1f, 0f);
        map.fill();
        map.setColor(Color.WHITE);
        map.fillCircle(size / 2, size / 2, size / 2 - 1);
        Texture texture = new Texture(map, true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        map.dispose();
        return texture;
    }

    private static Texture glow(int size) {
        Pixmap map = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        map.setBlending(Pixmap.Blending.None);
        float r = size / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x + 0.5f - r) / r;
                float dy = (y + 0.5f - r) / r;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float a = Math.max(0f, 1f - d);
                map.setColor(1f, 1f, 1f, a * a * a);
                map.drawPixel(x, y);
            }
        }
        Texture texture = new Texture(map, true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        map.dispose();
        return texture;
    }

    private static Texture diamond(int size) {
        Pixmap map = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        map.setBlending(Pixmap.Blending.None);
        float r = size / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = Math.abs(x + 0.5f - r) / r;
                float dy = Math.abs(y + 0.5f - r) / r;
                float edge = dx + dy;
                float a = edge <= 0.92f ? 1f : edge <= 1f ? (1f - edge) / 0.08f : 0f;
                map.setColor(1f, 1f, 1f, a);
                map.drawPixel(x, y);
            }
        }
        Texture texture = new Texture(map, true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        map.dispose();
        return texture;
    }

    private static Texture ring(int size, float innerRatio) {
        Pixmap map = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        map.setBlending(Pixmap.Blending.None);
        float r = size / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x + 0.5f - r) / r;
                float dy = (y + 0.5f - r) / r;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float a = d <= 0.96f && d >= innerRatio ? 1f : 0f;
                map.setColor(1f, 1f, 1f, a);
                map.drawPixel(x, y);
            }
        }
        Texture texture = new Texture(map, true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        map.dispose();
        return texture;
    }

    private static Texture triangle(int size) {
        Pixmap map = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        map.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < size; y++) {
            // Pixmap rows run top-down, so flip to get an upward-pointing triangle.
            float up = 1f - (y + 0.5f) / size;
            float halfWidth = (1f - up) * 0.5f;
            for (int x = 0; x < size; x++) {
                float offset = Math.abs((x + 0.5f) / size - 0.5f);
                float a = offset <= halfWidth - 0.02f ? 1f
                        : offset <= halfWidth ? (halfWidth - offset) / 0.02f : 0f;
                map.setColor(1f, 1f, 1f, a);
                map.drawPixel(x, y);
            }
        }
        Texture texture = new Texture(map, true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        map.dispose();
        return texture;
    }

    private static Texture rounded(int radius) {
        int size = radius * 2 + 2;
        int far = size - 1 - radius;
        Pixmap map = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        map.setBlending(Pixmap.Blending.None);
        map.setColor(1f, 1f, 1f, 0f);
        map.fill();
        map.setColor(Color.WHITE);
        map.fillCircle(radius, radius, radius);
        map.fillCircle(far, radius, radius);
        map.fillCircle(radius, far, radius);
        map.fillCircle(far, far, radius);
        map.fillRectangle(radius, 0, size - radius * 2, size);
        map.fillRectangle(0, radius, size, size - radius * 2);
        Texture texture = new Texture(map);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        map.dispose();
        return texture;
    }

    @Override
    public void dispose() {
        fonts.dispose();
        pixel.dispose();
        disc.dispose();
        glow.dispose();
        diamond.dispose();
        ring.dispose();
        triangle.dispose();
        roundTex.dispose();
        roundSmallTex.dispose();
    }
}
