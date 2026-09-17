package com.towerdefence.client.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.Graphics;
import com.towerdefence.client.TowerIdleGame;

public final class DesktopLauncher {
    /** Leaves room for the taskbar and window chrome. */
    private static final float SCREEN_FRACTION = 0.88f;

    public static void main(String[] args) {
        Graphics.DisplayMode display = Lwjgl3ApplicationConfiguration.getDisplayMode();
        int height = windowHeight(display);
        int width = Math.round(height * TowerIdleGame.WORLD_WIDTH / TowerIdleGame.WORLD_HEIGHT);

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Tower Idle");
        config.setWindowedMode(width, height);
        config.setWindowPosition(
                Math.max(0, (display.width - width) / 2),
                Math.max(0, (display.height - height) / 2)
        );
        config.setWindowSizeLimits(360, 640, -1, -1);
        config.setResizable(true);
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new TowerIdleGame(), config);
    }

    private static int windowHeight(Graphics.DisplayMode display) {
        int fromScreen = Math.round(display.height * SCREEN_FRACTION);
        return Math.max(640, Math.min(fromScreen, 1400));
    }
}
