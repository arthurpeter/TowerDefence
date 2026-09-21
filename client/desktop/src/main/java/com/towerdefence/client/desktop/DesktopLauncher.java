package com.towerdefence.client.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.Graphics;
import com.towerdefence.client.TowerIdleGame;
import org.lwjgl.glfw.GLFW;

public final class DesktopLauncher {
    /** Leaves room for the taskbar and window chrome. */
    private static final float SCREEN_FRACTION = 0.84f;
    private static final int MIN_HEIGHT = 420;
    private static final int MIN_WIDTH = 240;

    public static void main(String[] args) {
        Graphics.DisplayMode display = Lwjgl3ApplicationConfiguration.getDisplayMode();
        float scale = monitorScale();
        int screenW = Math.round(display.width / scale);
        int screenH = Math.round(display.height / scale);
        int height = Math.max(MIN_HEIGHT, Math.min(Math.round(screenH * SCREEN_FRACTION), 1400));
        int width = Math.round(height * TowerIdleGame.WORLD_WIDTH / TowerIdleGame.WORLD_HEIGHT);

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Tower Idle");
        config.setWindowedMode(width, height);
        config.setWindowPosition(
                Math.max(0, (screenW - width) / 2),
                Math.max(0, (screenH - height) / 2)
        );
        config.setWindowSizeLimits(MIN_WIDTH, MIN_HEIGHT, -1, -1);
        config.setResizable(true);
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new TowerIdleGame(), config);
    }

    /**
     * The display mode reports physical pixels while GLFW places and sizes windows in screen
     * coordinates. On a scaled desktop (Windows at 150%) the raw numbers build a window taller
     * than the screen and park it too low, so the bottom bar — deck, auto toggles, the prestige
     * button — falls off the edge and the game looks like it has no controls at all.
     */
    private static float monitorScale() {
        float[] xScale = new float[1];
        float[] yScale = new float[1];
        GLFW.glfwGetMonitorContentScale(GLFW.glfwGetPrimaryMonitor(), xScale, yScale);
        return Math.max(1f, yScale[0]);
    }
}
