package com.towerdefence.client;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.towerdefence.client.save.FileGameStore;
import com.towerdefence.client.screen.GameScreen;
import com.towerdefence.client.screen.MenuScreen;
import com.towerdefence.client.ui.Backdrop;
import com.towerdefence.client.ui.Theme;
import com.towerdefence.client.ui.Ui;
import com.towerdefence.engine.command.GameCommand;
import com.towerdefence.engine.config.GameBalance;
import com.towerdefence.engine.model.RunPhase;
import com.towerdefence.engine.session.GameSnapshot;
import com.towerdefence.engine.session.LocalGameSession;
import com.towerdefence.engine.session.SaveData;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class TowerIdleGame extends Game {
    public static final float WORLD_WIDTH = 480f;
    public static final float WORLD_HEIGHT = 854f;

    private static final long OFFLINE_REPORT_THRESHOLD_MS = 60_000L;
    private static final float AUTOSAVE_SECONDS = 5f;

    private final GameBalance balance = GameBalance.standard();
    private final FileGameStore store = new FileGameStore();

    public SpriteBatch batch;
    public Ui ui;
    public FitViewport viewport;
    public OrthographicCamera camera;
    public Backdrop backdrop;

    private LocalGameSession session;
    private OfflineReport offline;
    private float saveTimer;
    /** The player's own pause. It survives menu trips and only the PAUSE button clears it. */
    private boolean userPaused;
    private boolean menuOpen;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        batch = new SpriteBatch();
        ui = new Ui();
        backdrop = new Backdrop(WORLD_WIDTH, WORLD_HEIGHT);
        loadSession();
        setScreen(new MenuScreen(this));
    }

    private void loadSession() {
        Optional<SaveData> loaded = store.load();
        if (loaded.isEmpty()) {
            session = new LocalGameSession(balance);
            session.wallClock(System.currentTimeMillis());
            return;
        }
        SaveData save = loaded.get();
        session = LocalGameSession.fromSave(balance, save);
        // Research runs on the calendar, so hand the engine the real clock before catching up.
        session.wallClock(System.currentTimeMillis());
        GameSnapshot before = session.snapshot();
        long awayMillis = Math.max(0L, System.currentTimeMillis() - save.wallClockMillis());
        session.catchUpElapsed(TimeUnit.MILLISECONDS.toNanos(awayMillis));
        GameSnapshot after = session.snapshot();
        if (awayMillis >= OFFLINE_REPORT_THRESHOLD_MS && before.phase() == RunPhase.RUNNING) {
            offline = new OfflineReport(
                    awayMillis,
                    after.wave() - before.wave(),
                    Math.max(0d, after.coins() - before.coins()),
                    after.phase() == RunPhase.DEAD
            );
        }
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        session.wallClock(System.currentTimeMillis());
        long now = System.nanoTime();
        if (simulating()) {
            session.tick(now);
        } else {
            session.holdClock(now);
        }
        backdrop.update(delta);

        saveTimer += delta;
        if (saveTimer >= AUTOSAVE_SECONDS) {
            persist();
            saveTimer = 0f;
        }

        Gdx.gl.glClearColor(Theme.BG_TOP.r, Theme.BG_TOP.g, Theme.BG_TOP.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply(true);
        super.render();
    }

    public GameSnapshot snapshot() {
        return session.snapshot();
    }

    public void command(GameCommand command) {
        session.apply(command);
        persist();
    }

    public OfflineReport offline() {
        return offline;
    }

    public void clearOffline() {
        offline = null;
    }

    public void resetSave() {
        session = new LocalGameSession(balance);
        offline = null;
        userPaused = false;
        persist();
    }

    /** Auto-run keeps farming while you browse the menu, but an explicit pause outranks it. */
    private boolean simulating() {
        return !userPaused && (!menuOpen || snapshot().autoRun());
    }

    public boolean paused() {
        return userPaused;
    }

    public void setPaused(boolean value) {
        userPaused = value;
    }

    public void showMenu() {
        menuOpen = true;
        setScreen(new MenuScreen(this));
    }

    public void showGame() {
        menuOpen = false;
        setScreen(new GameScreen(this));
    }

    public void persist() {
        store.save(session.toSave(System.currentTimeMillis()));
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        ui.densityChanged(height / WORLD_HEIGHT);
    }

    @Override
    public void pause() {
        persist();
    }

    @Override
    public void dispose() {
        persist();
        super.dispose();
        batch.dispose();
        ui.dispose();
    }
}
