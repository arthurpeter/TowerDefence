package com.towerdefence.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.towerdefence.client.OfflineReport;
import com.towerdefence.client.TowerIdleGame;
import com.towerdefence.client.ui.Button;
import com.towerdefence.client.ui.CurrencyChip;
import com.towerdefence.client.ui.Theme;
import com.towerdefence.client.ui.Ui;
import com.towerdefence.engine.command.GameCommand;
import com.towerdefence.engine.command.UpgradeId;
import com.towerdefence.engine.model.Currency;
import com.towerdefence.engine.model.LabId;
import com.towerdefence.engine.model.RunPhase;
import com.towerdefence.engine.session.AutoPrestigeRule;
import com.towerdefence.engine.session.GameSnapshot;
import com.towerdefence.engine.session.LabView;
import com.towerdefence.engine.session.MissionView;
import com.towerdefence.engine.session.TierView;
import com.towerdefence.engine.session.UpgradeView;
import com.towerdefence.engine.util.SciFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MenuScreen implements Screen {
    private static final float W = TowerIdleGame.WORLD_WIDTH;
    private static final float H = TowerIdleGame.WORLD_HEIGHT;
    private static final int CORE_ROWS = 4;
    private static final int TIER_ROWS = 6;
    private static final int SIGIL_ROWS = 3;
    private static final int WORK_ROWS = 5;
    private static final int LAB_ROWS = 4;
    private static final int GOAL_ROWS = 3;
    private static final int CONTRACT_ROWS = 2;
    private static final float ROW_H = 58f;
    private static final float TIER_ROW_H = 50f;
    private static final float LAB_ROW_H = 66f;
    private static final float GOAL_ROW_H = 52f;

    private static final int WAVE_STEP = 5;
    private static final int STALL_STEP = 15;
    private static final int HP_STEP = 5;

    private enum Tab { HOME, TIERS, WORK, LABS, CORES, GOALS, AUTO }

    private final TowerIdleGame game;
    private final Button play = new Button("START RUN", Button.Style.PRIMARY);
    private final Button reset = new Button("RESET SAVE", Button.Style.GHOST);
    private final Rectangle[] tabs = new Rectangle[7];
    private final Rectangle[] workRows = new Rectangle[WORK_ROWS];
    private final Rectangle[] coreRows = new Rectangle[CORE_ROWS];
    private final Rectangle[] sigilRows = new Rectangle[SIGIL_ROWS];
    private final Rectangle autoPrev = new Rectangle();
    private final Rectangle autoNext = new Rectangle();
    private int autoPage;
    private final Rectangle[] tierRows = new Rectangle[TIER_ROWS];
    private final Rectangle[] labRows = new Rectangle[LAB_ROWS];
    private final Rectangle[] goalRows = new Rectangle[GOAL_ROWS];
    private final Rectangle[] contractRows = new Rectangle[CONTRACT_ROWS];
    private final float[] workPress = new float[WORK_ROWS];
    private final float[] corePress = new float[CORE_ROWS];
    private final Rectangle workPrev = new Rectangle();
    private final Rectangle workNext = new Rectangle();
    private int workPage;
    private final float[] sigilPress = new float[SIGIL_ROWS];
    private final float[] tierPress = new float[TIER_ROWS];
    private final float[] labPress = new float[LAB_ROWS];
    private final float[] contractPress = new float[CONTRACT_ROWS];

    private final Rectangle ruleToggle = new Rectangle();
    private final Rectangle waveMinus = new Rectangle();
    private final Rectangle wavePlus = new Rectangle();
    private final Rectangle stallMinus = new Rectangle();
    private final Rectangle stallPlus = new Rectangle();
    private final Rectangle hpMinus = new Rectangle();
    private final Rectangle hpPlus = new Rectangle();
    private final Rectangle farmToggle = new Rectangle();
    private final Rectangle farmMinus = new Rectangle();
    private final Rectangle farmPlus = new Rectangle();
    private final Vector3 touch = new Vector3();

    private Tab tab = Tab.HOME;
    private float time;
    private boolean resetArmed;
    private float resetArmedTimer;
    private int armedTier;
    private float armedTierTimer;
    private int tierWindowStart = 1;

    public MenuScreen(TowerIdleGame game) {
        this.game = game;
        play.bounds.set(Theme.PAD * 2f, 208f, W - Theme.PAD * 4f, 62f);
        reset.bounds.set(W / 2f - 90f, 142f, 180f, 42f);

        float tabW = (W - Theme.PAD * 2f - Theme.GAP * 6f) / 7f;
        for (int i = 0; i < tabs.length; i++) {
            tabs[i] = new Rectangle(Theme.PAD + i * (tabW + Theme.GAP), 694f, tabW, 44f);
        }
        for (int i = 0; i < WORK_ROWS; i++) {
            workRows[i] = new Rectangle(Theme.PAD, 606f - i * (ROW_H + 8f), W - Theme.PAD * 2f, ROW_H);
        }
        workPrev.set(Theme.PAD, 292f, 88f, 28f);
        workNext.set(W - Theme.PAD - 88f, 292f, 88f, 28f);
        for (int i = 0; i < CORE_ROWS; i++) {
            coreRows[i] = new Rectangle(Theme.PAD, 606f - i * (ROW_H + 8f), W - Theme.PAD * 2f, ROW_H);
        }
        for (int i = 0; i < SIGIL_ROWS; i++) {
            sigilRows[i] = new Rectangle(Theme.PAD, 598f - i * (ROW_H + 8f), W - Theme.PAD * 2f, ROW_H);
        }
        autoPrev.set(W - Theme.PAD - 176f, 666f, 80f, 24f);
        autoNext.set(W - Theme.PAD - 88f, 666f, 80f, 24f);
        for (int i = 0; i < TIER_ROWS; i++) {
            tierRows[i] = new Rectangle(Theme.PAD, 616f - i * (TIER_ROW_H + 8f), W - Theme.PAD * 2f, TIER_ROW_H);
        }
        for (int i = 0; i < LAB_ROWS; i++) {
            labRows[i] = new Rectangle(Theme.PAD, 606f - i * (LAB_ROW_H + 8f), W - Theme.PAD * 2f, LAB_ROW_H);
        }
        for (int i = 0; i < GOAL_ROWS; i++) {
            goalRows[i] = new Rectangle(Theme.PAD, 616f - i * (GOAL_ROW_H + 7f), W - Theme.PAD * 2f, GOAL_ROW_H);
        }
        for (int i = 0; i < CONTRACT_ROWS; i++) {
            contractRows[i] = new Rectangle(Theme.PAD, 416f - i * (ROW_H + 8f), W - Theme.PAD * 2f, ROW_H);
        }

        float panelX = Theme.PAD;
        float panelW = W - Theme.PAD * 2f;
        ruleToggle.set(panelX + panelW - 104f, 428f, 92f, 26f);
        waveMinus.set(panelX + panelW - 104f, 392f, 30f, 26f);
        wavePlus.set(panelX + panelW - 42f, 392f, 30f, 26f);
        stallMinus.set(panelX + panelW - 104f, 360f, 30f, 26f);
        stallPlus.set(panelX + panelW - 42f, 360f, 30f, 26f);
        hpMinus.set(panelX + panelW - 104f, 328f, 30f, 26f);
        hpPlus.set(panelX + panelW - 42f, 328f, 30f, 26f);
        farmToggle.set(panelX + panelW - 104f, 352f, 92f, 26f);
        farmMinus.set(panelX + panelW - 104f, 316f, 30f, 26f);
        farmPlus.set(panelX + panelW - 42f, 316f, 30f, 26f);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                game.viewport.unproject(touch.set(screenX, screenY, 0));
                onTouch(touch.x, touch.y);
                return true;
            }
        });
    }

    private void onTouch(float x, float y) {
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i].contains(x, y)) {
                tab = Tab.values()[i];
                armedTier = 0;
                return;
            }
        }

        if (play.touch(x, y)) {
            if (game.snapshot().phase() != RunPhase.RUNNING) {
                game.command(new GameCommand.StartRun());
            }
            game.clearOffline();
            game.showGame();
            return;
        }

        switch (tab) {
            case TIERS -> touchTiers(x, y);
            case WORK -> touchWork(x, y);
            case CORES -> touchRows(x, y, game.snapshot().permanents(), coreRows, corePress);
            case LABS -> touchLabs(x, y);
            case GOALS -> touchContracts(x, y);
            case AUTO -> touchAuto(x, y);
            case HOME -> {
                if (reset.touch(x, y)) {
                    if (resetArmed) {
                        game.resetSave();
                        resetArmed = false;
                    } else {
                        resetArmed = true;
                        resetArmedTimer = 3f;
                    }
                }
            }
        }
    }

    private void touchWork(float x, float y) {
        List<UpgradeView> shop = game.snapshot().meta();
        int pages = Math.max(1, (shop.size() + WORK_ROWS - 1) / WORK_ROWS);
        workPage = Math.max(0, Math.min(workPage, pages - 1));
        if (workPrev.contains(x, y)) {
            workPage = (workPage + pages - 1) % pages;
            return;
        }
        if (workNext.contains(x, y)) {
            workPage = (workPage + 1) % pages;
            return;
        }
        int start = workPage * WORK_ROWS;
        for (int i = 0; i < workRows.length && start + i < shop.size(); i++) {
            if (!workRows[i].contains(x, y)) {
                continue;
            }
            workPress[i] = 1f;
            UpgradeView view = shop.get(start + i);
            if (view.id() == UpgradeId.AUTO && view.purchased()) {
                game.command(new GameCommand.SetAutoBuy(!game.snapshot().autoBuy()));
            } else {
                game.command(new GameCommand.BuyUpgrade(view.id()));
            }
            return;
        }
    }

    private void touchLabs(float x, float y) {
        List<LabView> labs = game.snapshot().labs();
        for (int i = 0; i < labRows.length && i < labs.size(); i++) {
            if (labRows[i].contains(x, y)) {
                labPress[i] = 1f;
                game.command(new GameCommand.StartLab(labs.get(i).id()));
                return;
            }
        }
    }

    private void touchRows(float x, float y, List<UpgradeView> views, Rectangle[] rows, float[] press) {
        for (int i = 0; i < rows.length && i < views.size(); i++) {
            if (rows[i].contains(x, y)) {
                press[i] = 1f;
                game.command(new GameCommand.BuyUpgrade(views.get(i).id()));
                return;
            }
        }
    }

    private void touchAuto(float x, float y) {
        GameSnapshot snap = game.snapshot();
        if (autoPrev.contains(x, y)) {
            autoPage = 0;
            return;
        }
        if (autoNext.contains(x, y)) {
            autoPage = 1;
            return;
        }
        List<UpgradeView> automation = snap.automation();
        if (autoPage == 0) {
            int shown = Math.min(SIGIL_ROWS, automation.size());
            for (int i = 0; i < shown; i++) {
                if (!sigilRows[i].contains(x, y)) {
                    continue;
                }
                sigilPress[i] = 1f;
                UpgradeView view = automation.get(i);
                if (view.id() == UpgradeId.AUTO_RUN && view.purchased()) {
                    game.command(new GameCommand.SetAutoRun(!snap.autoRun()));
                } else if (!view.purchased()) {
                    game.command(new GameCommand.BuyUpgrade(view.id()));
                }
                return;
            }
            if (snap.autoRunUnlocked()) {
                if (farmToggle.contains(x, y)) {
                    game.command(new GameCommand.SetAutoRun(!snap.autoRun()));
                    return;
                }
                if (farmMinus.contains(x, y)) {
                    game.command(new GameCommand.SetAutoRunTier(snap.autoRunTier() - 1));
                    return;
                }
                if (farmPlus.contains(x, y)) {
                    game.command(new GameCommand.SetAutoRunTier(snap.autoRunTier() + 1));
                    return;
                }
            }
        } else {
            List<UpgradeView> rest = automation.size() <= SIGIL_ROWS
                    ? List.of()
                    : automation.subList(SIGIL_ROWS, automation.size());
            touchRows(x, y, rest, sigilRows, sigilPress);
        }
        if (!snap.autoPrestigeUnlocked() || autoPage == 0) {
            return;
        }

        AutoPrestigeRule rule = snap.autoPrestige();
        if (ruleToggle.contains(x, y)) {
            apply(copyRule(rule, !rule.enabled(), rule.waveTarget(), rule.stallSeconds(), rule.hpPercent()));
        } else if (waveMinus.contains(x, y)) {
            apply(copyRule(rule, rule.enabled(), rule.waveTarget() - WAVE_STEP, rule.stallSeconds(), rule.hpPercent()));
        } else if (wavePlus.contains(x, y)) {
            apply(copyRule(rule, rule.enabled(), Math.max(WAVE_STEP, rule.waveTarget() + WAVE_STEP),
                    rule.stallSeconds(), rule.hpPercent()));
        } else if (stallMinus.contains(x, y)) {
            apply(copyRule(rule, rule.enabled(), rule.waveTarget(), rule.stallSeconds() - STALL_STEP, rule.hpPercent()));
        } else if (stallPlus.contains(x, y)) {
            apply(copyRule(rule, rule.enabled(), rule.waveTarget(), rule.stallSeconds() + STALL_STEP, rule.hpPercent()));
        } else if (hpMinus.contains(x, y)) {
            apply(copyRule(rule, rule.enabled(), rule.waveTarget(), rule.stallSeconds(), rule.hpPercent() - HP_STEP));
        } else if (hpPlus.contains(x, y)) {
            apply(copyRule(rule, rule.enabled(), rule.waveTarget(), rule.stallSeconds(), rule.hpPercent() + HP_STEP));
        }
    }

    private void touchContracts(float x, float y) {
        touchRows(x, y, game.snapshot().contracts(), contractRows, contractPress);
    }

    private void apply(AutoPrestigeRule rule) {
        game.command(new GameCommand.SetAutoPrestige(rule));
    }

    private static AutoPrestigeRule copyRule(AutoPrestigeRule rule, boolean enabled, int wave,
                                            double stall, double hp) {
        return new AutoPrestigeRule(
                enabled,
                MathUtils.clamp(wave, 0, 500),
                MathUtils.clamp((float) stall, 0f, 900f),
                MathUtils.clamp((float) hp, 0f, 50f),
                rule.formula()
        );
    }

    private void touchTiers(float x, float y) {
        GameSnapshot snap = game.snapshot();
        List<TierView> tiers = snap.tiers();
        for (int i = 0; i < TIER_ROWS; i++) {
            if (!tierRows[i].contains(x, y)) {
                continue;
            }
            int index = tierWindowStart - 1 + i;
            if (index < 0 || index >= tiers.size()) {
                return;
            }
            TierView view = tiers.get(index);
            if (!view.unlocked() || view.active()) {
                return;
            }
            tierPress[i] = 1f;
            boolean needsConfirm = snap.phase() == RunPhase.RUNNING;
            if (!needsConfirm || armedTier == view.tier()) {
                game.command(new GameCommand.SelectTier(view.tier()));
                armedTier = 0;
            } else {
                armedTier = view.tier();
                armedTierTimer = 3f;
            }
            return;
        }
    }

    @Override
    public void render(float delta) {
        time += delta;
        play.update(delta);
        reset.update(delta);
        decay(workPress, delta);
        decay(corePress, delta);
        decay(sigilPress, delta);
        decay(tierPress, delta);
        decay(labPress, delta);
        decay(contractPress, delta);
        if (resetArmed) {
            resetArmedTimer -= delta;
            if (resetArmedTimer <= 0f) {
                resetArmed = false;
            }
        }
        if (armedTier > 0) {
            armedTierTimer -= delta;
            if (armedTierTimer <= 0f) {
                armedTier = 0;
            }
        }

        GameSnapshot snap = game.snapshot();
        tierWindowStart = Math.max(1, Math.min(
                snap.tiers().size() - TIER_ROWS + 1,
                snap.highestTierUnlocked() - TIER_ROWS + 2
        ));
        play.label = switch (snap.phase()) {
            case RUNNING -> "CONTINUE  -  TIER " + snap.tier();
            case DEAD -> "TRY AGAIN";
            case CLEARED -> "NEXT RUN";
            case IDLE -> "START RUN";
        };
        reset.label = resetArmed ? "TAP AGAIN TO WIPE" : "RESET SAVE";

        game.camera.position.set(W / 2f, H / 2f, 0f);
        game.camera.update();
        SpriteBatch batch = game.batch;
        Ui ui = game.ui;
        batch.setProjectionMatrix(game.camera.combined);
        batch.begin();
        game.backdrop.draw(batch, ui);

        float bob = MathUtils.sin(time * 1.1f) * 3f;
        ui.glow(batch, W / 2f, 790f + bob, 190f, Theme.ACCENT, 0.15f);
        ui.textTracked(batch, "TOWER IDLE", W / 2f, 792f + bob, Theme.TEXT_TITLE, 5f, ui.fade(Theme.TEXT, intro(0)));
        ui.text(batch, "hold the line while you are away", W / 2f, 756f, Theme.TEXT_SMALL,
                ui.fade(Theme.TEXT_DIM, intro(1)), Ui.CENTER);

        drawTab(batch, ui, tabs[0], "HOME", tab == Tab.HOME);
        drawTab(batch, ui, tabs[1], "TIER", tab == Tab.TIERS);
        drawTab(batch, ui, tabs[2], "WORK", tab == Tab.WORK);
        drawTab(batch, ui, tabs[3], "LAB", tab == Tab.LABS);
        drawTab(batch, ui, tabs[4], "CORE", tab == Tab.CORES);
        drawTab(batch, ui, tabs[5], "GOAL", tab == Tab.GOALS);
        drawTab(batch, ui, tabs[6], "AUTO", tab == Tab.AUTO);

        switch (tab) {
            case HOME -> drawHome(batch, ui, snap);
            case TIERS -> drawTiers(batch, ui, snap);
            case WORK -> drawWork(batch, ui, snap);
            case LABS -> drawLabs(batch, ui, snap);
            case CORES -> drawCores(batch, ui, snap);
            case GOALS -> drawGoals(batch, ui, snap);
            case AUTO -> drawAuto(batch, ui, snap);
        }

        play.draw(batch, ui, Theme.TEXT_H1);
        if (tab == Tab.HOME) {
            reset.accent = Theme.DANGER;
            reset.draw(batch, ui, Theme.TEXT_SMALL);
        }
        batch.end();
    }

    private static void decay(float[] values, float delta) {
        for (int i = 0; i < values.length; i++) {
            values[i] = Math.max(0f, values[i] - delta * 4f);
        }
    }

    private void drawTab(SpriteBatch batch, Ui ui, Rectangle box, String label, boolean active) {
        if (active) {
            ui.panelOutlined(batch, box.x, box.y, box.width, box.height, Theme.SURFACE_HI, Theme.ACCENT, 1.5f);
        } else {
            ui.panel(batch, box.x, box.y, box.width, box.height, Theme.SURFACE_LO, 0.9f);
        }
        ui.textFit(batch, label, box.x + box.width / 2f, box.y + box.height / 2f, box.width - 8f,
                Theme.TEXT_SMALL, active ? Theme.ACCENT : Theme.TEXT_DIM, Ui.CENTER);
    }

    // --- home tab ---

    private void drawHome(SpriteBatch batch, Ui ui, GameSnapshot snap) {
        drawOfflineCard(batch, ui, game.offline());
        drawCurrencyRow(batch, ui, 504f, Currency.SHARD, SciFormat.of(snap.shards()),
                "from kills  -  fatter enemies pay more", intro(3));
        drawCurrencyRow(batch, ui, 444f, Currency.CORE, SciFormat.of(snap.cores()),
                "new records only, every " + snap.wavesPerCore() + " waves", intro(3));
        drawRunCard(batch, ui, snap);
        MissionView next = nextMission(snap);
        if (next != null) {
            ui.textFit(batch, "GOAL  " + next.title() + "  +" + next.reward() + " crest",
                    W / 2f, 296f, W - Theme.PAD * 2f, Theme.TEXT_TINY, ui.fade(Theme.CREST, 0.95f), Ui.CENTER);
        } else {
            ui.textFit(batch, "every goal on the board is done",
                    W / 2f, 296f, W - Theme.PAD * 2f, Theme.TEXT_TINY, ui.fade(Theme.OK, 0.85f), Ui.CENTER);
        }
    }

    private void drawCurrencyRow(SpriteBatch batch, Ui ui, float y, Currency currency,
                                 String value, String caption, float alpha) {
        float x = Theme.PAD;
        float w = W - Theme.PAD * 2f;
        float h = 54f;
        float textW = w - 200f;

        ui.panelOutlined(batch, x, y, w, h, Theme.SURFACE_LO, Theme.STROKE, 1.5f);
        CurrencyChip.icon(batch, ui, currency, x + 28f, y + h / 2f, 10f);
        ui.textFit(batch, CurrencyChip.shortName(currency), x + 50f, y + h - 18f, textW, Theme.TEXT_SMALL,
                ui.fade(CurrencyChip.color(currency), alpha), Ui.LEFT);
        ui.textFit(batch, caption, x + 50f, y + 17f, textW, Theme.TEXT_TINY,
                ui.fade(Theme.TEXT_DIM, alpha * 0.9f), Ui.LEFT);
        ui.textFit(batch, value, x + w - Theme.PAD, y + h / 2f, 120f, Theme.TEXT_H1,
                ui.fade(CurrencyChip.color(currency), alpha), Ui.RIGHT);
    }

    private void drawOfflineCard(SpriteBatch batch, Ui ui, OfflineReport report) {
        float x = Theme.PAD;
        float w = W - Theme.PAD * 2f;
        float y = 566f;
        float h = 100f;
        float appear = intro(2);

        if (report == null) {
            ui.panelOutlined(batch, x, y, w, h, Theme.SURFACE_LO, Theme.STROKE, 1.5f);
            ui.text(batch, "NO OFFLINE PROGRESS YET", x + w / 2f, y + h / 2f + 12f, Theme.TEXT_SMALL,
                    ui.fade(Theme.TEXT_DIM, appear), Ui.CENTER);
            ui.textFit(batch, "closing mid-run keeps the tower fighting, up to "
                            + formatHours(game.snapshot().offlineHours()),
                    x + w / 2f, y + h / 2f - 14f, w - Theme.PAD * 2f, Theme.TEXT_TINY,
                    ui.fade(Theme.TEXT_DIM, appear * 0.75f), Ui.CENTER);
            return;
        }

        ui.panelOutlined(batch, x, y, w, h, Theme.SURFACE, Theme.ACCENT, 1.5f);
        ui.text(batch, "WHILE YOU WERE AWAY", x + Theme.PAD, y + h - 20f, Theme.TEXT_SMALL,
                ui.fade(Theme.ACCENT, appear), Ui.LEFT);
        ui.text(batch, report.awayText(), x + w - Theme.PAD, y + h - 20f, Theme.TEXT_SMALL,
                ui.fade(Theme.TEXT_DIM, appear), Ui.RIGHT);

        float col = w / 2f;
        drawStat(batch, ui, "WAVES", "+" + report.wavesGained(), x + Theme.PAD, y + 24f,
                col - Theme.PAD * 1.5f, Theme.OK, appear);
        drawStat(batch, ui, "COINS", "+" + SciFormat.of(report.coinsGained()), x + col + Theme.PAD * 0.5f, y + 24f,
                col - Theme.PAD * 1.5f, Theme.GOLD, appear);
    }

    private void drawRunCard(SpriteBatch batch, Ui ui, GameSnapshot snap) {
        float x = Theme.PAD;
        float w = W - Theme.PAD * 2f;
        float y = 316f;
        float h = 106f;
        float inner = w - Theme.PAD * 2f;
        float appear = intro(4);

        ui.panelOutlined(batch, x, y, w, h, Theme.SURFACE_LO, Theme.STROKE, 1.5f);
        ui.text(batch, "BEST WAVE " + snap.bestWave(), x + Theme.PAD, y + h - 22f, Theme.TEXT_SMALL,
                ui.fade(Theme.TEXT, appear), Ui.LEFT);
        ui.text(batch, "TIER " + snap.tier() + "  -  " + snap.runsCompleted() + " RUNS",
                x + w - Theme.PAD, y + h - 22f, Theme.TEXT_SMALL, ui.fade(Theme.ACCENT, appear), Ui.RIGHT);

        switch (snap.phase()) {
            case RUNNING -> {
                float ratio = (float) (snap.towerHp() / Math.max(1d, snap.towerMaxHp()));
                ui.bar(batch, x + Theme.PAD, y + 44f, inner, 9f, ratio, Theme.SURFACE,
                        ratio < 0.3f ? Theme.DANGER : Theme.OK);
                ui.textFit(batch, "wave " + snap.wave() + " - hp " + SciFormat.of(snap.towerHp()),
                        x + Theme.PAD, y + 24f, inner / 2f, Theme.TEXT_TINY,
                        ui.fade(Theme.TEXT_DIM, appear), Ui.LEFT);
                ui.textFit(batch, SciFormat.of(snap.coins()) + " coins", x + w - Theme.PAD, y + 24f, inner / 2f,
                        Theme.TEXT_TINY, ui.fade(Theme.GOLD, appear), Ui.RIGHT);
            }
            case DEAD -> ui.textFit(batch, "run lost  -  shards from kills stay",
                    x + w / 2f, y + 36f, inner, Theme.TEXT_SMALL, ui.fade(Theme.DANGER, appear), Ui.CENTER);
            case CLEARED -> ui.textFit(batch, "tier cleared  -  shards from kills stay",
                    x + w / 2f, y + 36f, inner, Theme.TEXT_SMALL, ui.fade(Theme.OK, appear), Ui.CENTER);
            case IDLE -> {
                ui.textFit(batch, "the tower fires on its own - you only buy upgrades",
                        x + w / 2f, y + 46f, inner, Theme.TEXT_SMALL, ui.fade(Theme.TEXT, appear), Ui.CENTER);
                ui.textFit(batch, "kill to farm shards for workshop and labs",
                        x + w / 2f, y + 24f, inner, Theme.TEXT_TINY, ui.fade(Theme.TEXT_DIM, appear), Ui.CENTER);
            }
        }
    }

    private void drawStat(SpriteBatch batch, Ui ui, String label, String value,
                          float x, float baseY, float maxWidth, Color color, float appear) {
        ui.textFit(batch, label, x, baseY + 26f, maxWidth, Theme.TEXT_TINY, ui.fade(Theme.TEXT_DIM, appear), Ui.LEFT);
        ui.textFit(batch, value, x, baseY, maxWidth, Theme.TEXT_H1, ui.fade(color, appear), Ui.LEFT);
    }

    // --- tiers tab ---

    private void drawTiers(SpriteBatch batch, Ui ui, GameSnapshot snap) {
        ui.textFit(batch, "harder enemies, bigger payouts, more cores", W / 2f, 678f, W - Theme.PAD * 2f,
                Theme.TEXT_SMALL, Theme.ACCENT, Ui.CENTER);

        List<TierView> tiers = snap.tiers();
        for (int i = 0; i < TIER_ROWS; i++) {
            int index = tierWindowStart - 1 + i;
            if (index < 0 || index >= tiers.size()) {
                continue;
            }
            drawTierRow(batch, ui, tierRows[i], tiers.get(index), tierPress[i], snap);
        }

        TierView playing = snap.tiers().get(Math.max(0, snap.tier() - 1));
        ui.textFit(batch, "T" + snap.tier() + " wave " + playing.nextUnlockWave()
                        + " opens the next floor  -  " + snap.wavesPerTier() + " clears this one",
                W / 2f, 296f, W - Theme.PAD * 2f, Theme.TEXT_TINY, ui.fade(Theme.TEXT_DIM, 0.85f), Ui.CENTER);
    }

    private void drawTierRow(SpriteBatch batch, Ui ui, Rectangle box, TierView view, float press, GameSnapshot snap) {
        float squash = press * 2f;
        float x = box.x + squash;
        float y = box.y + squash * 0.5f;
        float w = box.width - squash * 2f;
        float h = box.height - squash;

        Color stroke = view.cleared() ? Theme.CREST : view.active() ? Theme.OK : view.unlocked() ? Theme.STROKE : Theme.SURFACE;
        Color fill = view.active() || view.cleared() ? Theme.SURFACE_HI : Theme.SURFACE_LO;
        ui.panelOutlined(batch, x, y, w, h, fill, stroke, 1.5f);

        float pillW = 86f;
        Color nameColor = view.unlocked() ? Theme.TEXT : Theme.TEXT_DIM;
        ui.textFit(batch, "TIER " + view.tier(), x + Theme.PAD, y + h - 17f, 78f, Theme.TEXT_BODY, nameColor, Ui.LEFT);
        ui.textFit(batch, view.bestWave() > 0 ? "best " + view.bestWave() : "never played",
                x + Theme.PAD, y + 16f, 78f, Theme.TEXT_TINY, ui.fade(Theme.TEXT_DIM, 0.9f), Ui.LEFT);

        float detailX = x + 100f;
        float detailW = w - pillW - 116f;
        ui.textFit(batch, String.format(Locale.ROOT, "hp x%s   coins x%s",
                        SciFormat.of(view.hpMultiplier()), SciFormat.of(view.coinMultiplier())),
                detailX, y + h - 17f, detailW, Theme.TEXT_TINY,
                ui.fade(view.unlocked() ? Theme.TEXT_DIM : Theme.STROKE, 1f), Ui.LEFT);
        ui.textFit(batch, view.coresPerMilestone() + " cores / " + snap.wavesPerCore() + " waves",
                detailX, y + 16f, detailW, Theme.TEXT_TINY,
                ui.fade(view.unlocked() ? Theme.ACCENT : Theme.STROKE, 0.9f), Ui.LEFT);

        String pill;
        Color pillColor;
        if (view.cleared()) {
            pill = "CLEAR";
            pillColor = Theme.CREST;
        } else if (view.active()) {
            pill = "ACTIVE";
            pillColor = Theme.OK;
        } else if (!view.unlocked()) {
            pill = view.unlockFromTier() > 0
                    ? "T" + view.unlockFromTier() + " " + view.unlockWave()
                    : "WAVE " + view.unlockWave();
            pillColor = Theme.TEXT_DIM;
        } else if (armedTier == view.tier()) {
            pill = "CONFIRM";
            pillColor = Theme.DANGER;
        } else {
            pill = "PLAY";
            pillColor = Theme.ACCENT;
        }
        ui.panel(batch, x + w - pillW - Theme.GAP, y + h / 2f - 13f, pillW, 26f, Theme.SURFACE_LO, 0.95f);
        ui.textFit(batch, pill, x + w - pillW / 2f - Theme.GAP, y + h / 2f, pillW - 8f, Theme.TEXT_TINY,
                pillColor, Ui.CENTER);
    }

    // --- workshop tab ---

    private void drawWork(SpriteBatch batch, Ui ui, GameSnapshot snap) {
        drawBalanceHeader(batch, ui, Currency.SHARD, snap.shards());
        List<UpgradeView> shop = snap.meta();
        int pages = Math.max(1, (shop.size() + WORK_ROWS - 1) / WORK_ROWS);
        workPage = Math.max(0, Math.min(workPage, pages - 1));
        int start = workPage * WORK_ROWS;
        for (int i = 0; i < WORK_ROWS && start + i < shop.size(); i++) {
            drawUpgradeRow(batch, ui, workRows[i], shop.get(start + i), workPress[i], Currency.SHARD);
        }
        ui.panel(batch, workPrev.x, workPrev.y, workPrev.width, workPrev.height, Theme.SURFACE_LO, 0.95f);
        ui.panel(batch, workNext.x, workNext.y, workNext.width, workNext.height, Theme.SURFACE_LO, 0.95f);
        ui.textFit(batch, "PREV", workPrev.x + workPrev.width / 2f, workPrev.y + workPrev.height / 2f,
                workPrev.width - 8f, Theme.TEXT_TINY, Theme.TEXT_DIM, Ui.CENTER);
        ui.textFit(batch, "NEXT", workNext.x + workNext.width / 2f, workNext.y + workNext.height / 2f,
                workNext.width - 8f, Theme.TEXT_TINY, Theme.TEXT_DIM, Ui.CENTER);
        ui.textFit(batch, "workshop  " + (workPage + 1) + "/" + pages + "  -  instant, keeps between runs",
                W / 2f, 306f, 220f, Theme.TEXT_TINY, ui.fade(Theme.TEXT_DIM, 0.85f), Ui.CENTER);
    }

    // --- cores tab ---

    private void drawCores(SpriteBatch batch, Ui ui, GameSnapshot snap) {
        drawBalanceHeader(batch, ui, Currency.CORE, snap.cores());

        List<UpgradeView> permanents = snap.permanents();
        for (int i = 0; i < CORE_ROWS && i < permanents.size(); i++) {
            drawUpgradeRow(batch, ui, coreRows[i], permanents.get(i), corePress[i], Currency.CORE);
        }

        ui.textFit(batch, "every " + snap.wavesPerCore() + " waves of a new record pays "
                        + snap.coresPerMilestone() + " core" + (snap.coresPerMilestone() == 1 ? "" : "s")
                        + " at tier " + snap.tier(),
                W / 2f, 296f, W - Theme.PAD * 2f, Theme.TEXT_TINY, ui.fade(Theme.TEXT_DIM, 0.85f), Ui.CENTER);
    }

    // --- labs tab ---

    private void drawLabs(SpriteBatch batch, Ui ui, GameSnapshot snap) {
        drawBalanceHeader(batch, ui, Currency.SHARD, snap.shards());

        List<LabView> labs = snap.labs();
        for (int i = 0; i < LAB_ROWS && i < labs.size(); i++) {
            drawLabRow(batch, ui, labRows[i], labs.get(i), labPress[i], snap);
        }

        ui.textFit(batch, "same shards as workshop  -  one research at a time",
                W / 2f, 296f, W - Theme.PAD * 2f, Theme.TEXT_TINY, ui.fade(Theme.TEXT_DIM, 0.85f), Ui.CENTER);
    }

    private void drawLabRow(SpriteBatch batch, Ui ui, Rectangle box, LabView view, float press, GameSnapshot snap) {
        float squash = press * 2.5f;
        float x = box.x + squash;
        float y = box.y + squash * 0.5f;
        float w = box.width - squash * 2f;
        float h = box.height - squash;

        boolean busy = snap.labs().stream().anyMatch(LabView::running);
        Color stroke = view.running() ? Theme.ACCENT : view.maxed() ? Theme.OK
                : view.affordable() ? Theme.SHARD : Theme.STROKE;
        Color fill = view.running() || view.affordable() ? Theme.SURFACE_HI : Theme.SURFACE;
        ui.panelOutlined(batch, x, y, w, h, fill, stroke, 1.5f);

        float pillW = 86f;
        float textW = w - pillW - Theme.PAD * 3f;
        String name = labLabel(view.id()) + (view.level() > 0 ? "  L" + view.level() : "");
        ui.textFit(batch, name, x + Theme.PAD, y + h - 18f, textW, Theme.TEXT_BODY,
                view.affordable() || view.running() || view.maxed() ? Theme.TEXT : Theme.TEXT_DIM, Ui.LEFT);
        ui.textFit(batch, view.effect(), x + Theme.PAD, y + (view.running() ? 28f : 18f), textW, Theme.TEXT_SMALL,
                ui.fade(Theme.SHARD, 0.85f), Ui.LEFT);

        if (view.running()) {
            ui.bar(batch, x + Theme.PAD, y + 10f, textW, 7f, view.progress(), Theme.SURFACE, Theme.ACCENT);
        }

        float pillX = x + w - pillW - Theme.PAD;
        ui.panel(batch, pillX, y + h / 2f - 14f, pillW, 28f, Theme.SURFACE_LO, 0.95f);
        if (view.maxed()) {
            ui.textFit(batch, "MAX", pillX + pillW / 2f, y + h / 2f, pillW - 8f, Theme.TEXT_TINY,
                    Theme.OK, Ui.CENTER);
        } else if (view.running()) {
            ui.textFit(batch, formatLabTime(view.secondsRemaining()),
                    pillX + pillW / 2f, y + h / 2f, pillW - 8f, Theme.TEXT_TINY, Theme.ACCENT, Ui.CENTER);
        } else if (busy) {
            ui.textFit(batch, "WAIT", pillX + pillW / 2f, y + h / 2f, pillW - 8f, Theme.TEXT_TINY,
                    Theme.TEXT_DIM, Ui.CENTER);
        } else {
            CurrencyChip.icon(batch, ui, Currency.SHARD, pillX + 16f, y + h / 2f, 6f);
            ui.textFit(batch, SciFormat.of(view.cost()), pillX + pillW - 10f, y + h / 2f, pillW - 34f,
                    Theme.TEXT_SMALL, view.affordable() ? Theme.SHARD : Theme.TEXT_DIM, Ui.RIGHT);
        }
    }

    private static String labLabel(LabId id) {
        return switch (id) {
            case LAB_DAMAGE -> "DAMAGE";
            case LAB_COINS -> "COINS";
            case LAB_HEALTH -> "HEALTH";
            case LAB_MULTISHOT -> "MULTISHOT";
        };
    }

    private static String formatLabTime(double seconds) {
        int total = (int) Math.max(0d, Math.ceil(seconds));
        int hours = total / 3600;
        int minutes = (total % 3600) / 60;
        int secs = total % 60;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%dh %02dm", hours, minutes);
        }
        if (minutes > 0) {
            return String.format(Locale.ROOT, "%dm %02ds", minutes, secs);
        }
        return secs + "s";
    }

    // --- goals tab ---

    private void drawGoals(SpriteBatch batch, Ui ui, GameSnapshot snap) {
        drawBalanceHeader(batch, ui, Currency.CREST, snap.crests());
        ui.textFit(batch, "missions, not waves", W / 2f, 656f, W - Theme.PAD * 2f,
                Theme.TEXT_TINY, ui.fade(Theme.TEXT_DIM, 0.9f), Ui.CENTER);

        List<MissionView> visible = visibleMissions(snap);
        for (int i = 0; i < GOAL_ROWS && i < visible.size(); i++) {
            drawMissionRow(batch, ui, goalRows[i], visible.get(i));
        }

        List<UpgradeView> contracts = snap.contracts();
        for (int i = 0; i < CONTRACT_ROWS && i < contracts.size(); i++) {
            drawUpgradeRow(batch, ui, contractRows[i], contracts.get(i), contractPress[i], Currency.CREST);
        }

        ui.textFit(batch, "dying drops the run  -  prestige (or auto on low hp) banks it",
                W / 2f, 296f, W - Theme.PAD * 2f, Theme.TEXT_TINY, ui.fade(Theme.TEXT_DIM, 0.85f), Ui.CENTER);
    }

    private void drawMissionRow(SpriteBatch batch, Ui ui, Rectangle box, MissionView view) {
        Color stroke = view.completed() ? Theme.OK : Theme.STROKE;
        Color fill = view.completed() ? Theme.SURFACE_HI : Theme.SURFACE;
        ui.panelOutlined(batch, box.x, box.y, box.width, box.height, fill, stroke, 1.5f);

        float textW = box.width - 88f;
        ui.textFit(batch, view.title(), box.x + Theme.PAD, box.y + box.height - 16f, textW,
                Theme.TEXT_SMALL, view.completed() ? Theme.TEXT : Theme.TEXT, Ui.LEFT);
        ui.bar(batch, box.x + Theme.PAD, box.y + 10f, textW, 6f, view.progress(), Theme.SURFACE_LO,
                view.completed() ? Theme.OK : Theme.CREST);
        ui.textFit(batch, "+" + view.reward(), box.x + box.width - Theme.PAD, box.y + box.height / 2f,
                64f, Theme.TEXT_SMALL, Theme.CREST, Ui.RIGHT);
    }

    private static List<MissionView> visibleMissions(GameSnapshot snap) {
        List<MissionView> open = new ArrayList<>();
        List<MissionView> done = new ArrayList<>();
        for (MissionView mission : snap.missions()) {
            (mission.completed() ? done : open).add(mission);
        }
        List<MissionView> out = new ArrayList<>(open);
        out.addAll(done);
        if (out.size() > GOAL_ROWS) {
            return out.subList(0, GOAL_ROWS);
        }
        return out;
    }

    // --- auto tab ---

    private void drawAuto(SpriteBatch batch, Ui ui, GameSnapshot snap) {
        CurrencyChip.icon(batch, ui, Currency.SIGIL, Theme.PAD + 12f, 678f, 8f);
        ui.textFit(batch, SciFormat.of(snap.sigils()) + " sigils",
                Theme.PAD + 30f, 678f, 150f, Theme.TEXT_SMALL, Theme.SIGIL, Ui.LEFT);
        ui.panel(batch, autoPrev.x, autoPrev.y, autoPrev.width, autoPrev.height, Theme.SURFACE_LO, 0.95f);
        ui.panel(batch, autoNext.x, autoNext.y, autoNext.width, autoNext.height, Theme.SURFACE_LO, 0.95f);
        ui.textFit(batch, "AUTO", autoPrev.x + autoPrev.width / 2f, autoPrev.y + 12f,
                autoPrev.width - 6f, Theme.TEXT_TINY, autoPage == 0 ? Theme.ACCENT : Theme.TEXT_DIM, Ui.CENTER);
        ui.textFit(batch, "MORE", autoNext.x + autoNext.width / 2f, autoNext.y + 12f,
                autoNext.width - 6f, Theme.TEXT_TINY, autoPage == 1 ? Theme.ACCENT : Theme.TEXT_DIM, Ui.CENTER);

        List<UpgradeView> automation = snap.automation();
        if (autoPage == 0) {
            int shown = Math.min(SIGIL_ROWS, automation.size());
            for (int i = 0; i < shown; i++) {
                drawUpgradeRow(batch, ui, sigilRows[i], automation.get(i), sigilPress[i],
                        automation.get(i).currency());
            }
            drawFarmPanel(batch, ui, snap);
        } else {
            List<UpgradeView> rest = automation.size() <= SIGIL_ROWS
                    ? List.of()
                    : automation.subList(SIGIL_ROWS, automation.size());
            for (int i = 0; i < SIGIL_ROWS && i < rest.size(); i++) {
                drawUpgradeRow(batch, ui, sigilRows[i], rest.get(i), sigilPress[i], rest.get(i).currency());
            }
            drawRulePanel(batch, ui, snap);
        }
    }

    private void drawFarmPanel(SpriteBatch batch, Ui ui, GameSnapshot snap) {
        float x = Theme.PAD;
        float w = W - Theme.PAD * 2f;
        float y = 286f;
        float h = 110f;
        boolean unlocked = snap.autoRunUnlocked();
        ui.panelOutlined(batch, x, y, w, h,
                unlocked ? Theme.SURFACE : Theme.SURFACE_LO,
                unlocked && snap.autoRun() ? Theme.OK : Theme.STROKE, 1.5f);
        ui.textFit(batch, "AUTO RUN", x + Theme.PAD, y + h - 20f, w - 130f, Theme.TEXT_BODY,
                unlocked ? Theme.TEXT : Theme.TEXT_DIM, Ui.LEFT);
        if (!unlocked) {
            ui.textFit(batch, "buy AUTO, AUTO PRESTIGE, then AUTO RUN  -  BUY/PREST toggles live in the run",
                    x + w / 2f, y + h / 2f - 8f, w - Theme.PAD * 2f, Theme.TEXT_SMALL,
                    ui.fade(Theme.TEXT_DIM, 0.9f), Ui.CENTER);
            return;
        }
        ui.panelOutlined(batch, farmToggle.x, farmToggle.y, farmToggle.width, farmToggle.height,
                snap.autoRun() ? Theme.OK : Theme.SURFACE_LO,
                snap.autoRun() ? Theme.OK : Theme.STROKE, 1.2f);
        ui.textFit(batch, snap.autoRun() ? "ON" : "OFF", farmToggle.x + farmToggle.width / 2f,
                farmToggle.y + farmToggle.height / 2f, farmToggle.width - 8f, Theme.TEXT_SMALL,
                snap.autoRun() ? Theme.BG_TOP : Theme.TEXT_DIM, Ui.CENTER);
        drawStepper(batch, ui, "farm tier", "T" + snap.autoRunTier(),
                329f, farmMinus, farmPlus, true);
        ui.textFit(batch, "keeps running in the menu with auto-buy and auto-prestige",
                x + w / 2f, y + 18f, w - Theme.PAD * 2f, Theme.TEXT_TINY,
                ui.fade(snap.autoRun() ? Theme.OK : Theme.TEXT_DIM, 0.9f), Ui.CENTER);
    }

    private void drawRulePanel(SpriteBatch batch, Ui ui, GameSnapshot snap) {
        float x = Theme.PAD;
        float w = W - Theme.PAD * 2f;
        float y = 286f;
        float h = 176f;
        boolean unlocked = snap.autoPrestigeUnlocked();
        AutoPrestigeRule rule = snap.autoPrestige();

        ui.panelOutlined(batch, x, y, w, h,
                unlocked ? Theme.SURFACE : Theme.SURFACE_LO,
                unlocked && rule.enabled() ? Theme.OK : Theme.STROKE, 1.5f);

        ui.textFit(batch, "AUTO PRESTIGE", x + Theme.PAD, y + h - 20f, w - 130f, Theme.TEXT_BODY,
                unlocked ? Theme.TEXT : Theme.TEXT_DIM, Ui.LEFT);

        if (!unlocked) {
            ui.textFit(batch, "unlock above - dying loses the run, this cashes out first",
                    x + w / 2f, y + h / 2f - 8f, w - Theme.PAD * 2f, Theme.TEXT_SMALL,
                    ui.fade(Theme.TEXT_DIM, 0.9f), Ui.CENTER);
            return;
        }

        ui.panelOutlined(batch, ruleToggle.x, ruleToggle.y, ruleToggle.width, ruleToggle.height,
                rule.enabled() ? Theme.OK : Theme.SURFACE_LO, rule.enabled() ? Theme.OK : Theme.STROKE, 1.2f);
        ui.textFit(batch, rule.enabled() ? "ON" : "OFF", ruleToggle.x + ruleToggle.width / 2f,
                ruleToggle.y + ruleToggle.height / 2f, ruleToggle.width - 8f, Theme.TEXT_SMALL,
                rule.enabled() ? Theme.BG_TOP : Theme.TEXT_DIM, Ui.CENTER);

        drawStepper(batch, ui, "prestige at wave", rule.hasWaveTrigger() ? String.valueOf(rule.waveTarget()) : "off",
                405f, waveMinus, wavePlus, rule.hasWaveTrigger());
        drawStepper(batch, ui, "or if stalled for", rule.hasStallTrigger()
                        ? String.format(Locale.ROOT, "%.0fs", rule.stallSeconds()) : "off",
                373f, stallMinus, stallPlus, rule.hasStallTrigger());
        drawStepper(batch, ui, "or if hp below", rule.hasHpTrigger()
                        ? String.format(Locale.ROOT, "%.0f%%", rule.hpPercent()) : "off",
                341f, hpMinus, hpPlus, rule.hasHpTrigger());

        String summary;
        if (!rule.enabled()) {
            summary = "rule is off";
        } else if (!rule.hasWaveTrigger() && !rule.hasStallTrigger() && !rule.hasHpTrigger()) {
            summary = "set a wave, a stall time, or a low-hp bail";
        } else {
            summary = summarize(rule);
        }
        ui.textFit(batch, summary, x + w / 2f, y + 22f, w - Theme.PAD * 2f, Theme.TEXT_TINY,
                ui.fade(rule.enabled() ? Theme.OK : Theme.TEXT_DIM, 0.9f), Ui.CENTER);
    }

    private void drawStepper(SpriteBatch batch, Ui ui, String label, String value, float centerY,
                             Rectangle minus, Rectangle plus, boolean active) {
        ui.textFit(batch, label, Theme.PAD * 2f, centerY, 170f, Theme.TEXT_SMALL,
                ui.fade(Theme.TEXT_DIM, 0.95f), Ui.LEFT);
        ui.textFit(batch, value, minus.x - 10f, centerY, 62f, Theme.TEXT_SMALL,
                active ? Theme.ACCENT : Theme.TEXT_DIM, Ui.RIGHT);
        drawStepperButton(batch, ui, minus, "-");
        drawStepperButton(batch, ui, plus, "+");
    }

    private void drawStepperButton(SpriteBatch batch, Ui ui, Rectangle box, String glyph) {
        ui.panelOutlined(batch, box.x, box.y, box.width, box.height, Theme.SURFACE_LO, Theme.STROKE, 1.2f);
        ui.textFit(batch, glyph, box.x + box.width / 2f, box.y + box.height / 2f, box.width - 6f,
                Theme.TEXT_BODY, Theme.TEXT, Ui.CENTER);
    }

    // --- shared rows ---

    private void drawBalanceHeader(SpriteBatch batch, Ui ui, Currency currency, double amount) {
        CurrencyChip.icon(batch, ui, currency, W / 2f - 52f, 678f, 8f);
        ui.textFit(batch, SciFormat.of(amount) + " " + CurrencyChip.shortName(currency),
                W / 2f + 14f, 678f, 190f, Theme.TEXT_SMALL, CurrencyChip.color(currency), Ui.CENTER);
    }

    private void drawUpgradeRow(SpriteBatch batch, Ui ui, Rectangle box, UpgradeView view,
                                float press, Currency currency) {
        float squash = press * 2.5f;
        float x = box.x + squash;
        float y = box.y + squash * 0.5f;
        float w = box.width - squash * 2f;
        float h = box.height - squash;

        Color accent = CurrencyChip.color(currency);
        boolean owned = view.purchased();
        Color stroke = owned ? Theme.OK : view.affordable() ? accent : Theme.STROKE;
        Color fill = view.affordable() || owned ? Theme.SURFACE_HI : Theme.SURFACE;
        ui.panelOutlined(batch, x, y, w, h, fill, stroke, 1.5f);

        float pillW = 82f;
        float textW = w - pillW - Theme.PAD * 3f;
        String name = label(view.id()) + (owned || view.level() == 0 ? "" : "  L" + view.level());
        ui.textFit(batch, name, x + Theme.PAD, y + h - 20f, textW, Theme.TEXT_BODY,
                view.affordable() || owned ? Theme.TEXT : Theme.TEXT_DIM, Ui.LEFT);
        ui.textFit(batch, view.effect(), x + Theme.PAD, y + 20f, textW, Theme.TEXT_SMALL,
                ui.fade(accent, 0.85f), Ui.LEFT);

        float pillX = x + w - pillW - Theme.PAD;
        ui.panel(batch, pillX, y + h / 2f - 14f, pillW, 28f, Theme.SURFACE_LO, 0.95f);
        if (view.id() == UpgradeId.AUTO_RUN && owned) {
            ui.textFit(batch, game.snapshot().autoRun() ? "ON" : "OFF",
                    pillX + pillW / 2f, y + h / 2f, pillW - 8f, Theme.TEXT_TINY, Theme.OK, Ui.CENTER);
        } else if (owned) {
            ui.textFit(batch, "OWNED", pillX + pillW / 2f, y + h / 2f, pillW - 8f, Theme.TEXT_TINY,
                    Theme.OK, Ui.CENTER);
        } else {
            CurrencyChip.icon(batch, ui, currency, pillX + 16f, y + h / 2f, 6f);
            ui.textFit(batch, SciFormat.of(view.cost()), pillX + pillW - 10f, y + h / 2f, pillW - 34f,
                    Theme.TEXT_SMALL, view.affordable() ? accent : Theme.TEXT_DIM, Ui.RIGHT);
        }
    }

    private static String label(UpgradeId id) {
        return switch (id) {
            case CORE_DAMAGE -> "OVERCHARGE";
            case CORE_HP -> "REINFORCE";
            case CORE_REGEN -> "REPAIR DRONES";
            case CORE_COIN -> "SALVAGE";
            case NIGHT_SHIFT -> "NIGHT SHIFT";
            case AUTO_PRESTIGE -> "AUTO PRESTIGE";
            case FORMULA -> "FORMULA EDITOR";
            case HEADSTART -> "HEADSTART";
            case WAR_CHEST -> "WAR CHEST";
            case FOUNDATION -> "BASE";
            case FORTUNE -> "FORTUNE";
            case OVERCLOCK -> "OVERCLOCK";
            case OPTICS -> "OPTICS";
            case RECOVERY -> "RECOVERY";
            case RAMPART -> "RAMPART";
            case HARVEST -> "HARVEST";
            case AUTO -> "AUTO BUY";
            case AUTO_RUN -> "AUTO RUN";
            case AUTO_CAST -> "AUTO CAST";
            case GAME_SPEED -> "GAME SPEED";
            default -> id.name();
        };
    }

    private static String summarize(AutoPrestigeRule rule) {
        StringBuilder out = new StringBuilder("cashes out");
        boolean any = false;
        if (rule.hasWaveTrigger()) {
            out.append(" at wave ").append(rule.waveTarget());
            any = true;
        }
        if (rule.hasStallTrigger()) {
            out.append(any ? " or" : "").append(" after ")
                    .append(String.format(Locale.ROOT, "%.0fs", rule.stallSeconds())).append(" stuck");
            any = true;
        }
        if (rule.hasHpTrigger()) {
            out.append(any ? " or" : "").append(" under ")
                    .append(String.format(Locale.ROOT, "%.0f%%", rule.hpPercent())).append(" hp");
        }
        return out.toString();
    }

    private static MissionView nextMission(GameSnapshot snap) {
        for (MissionView mission : snap.missions()) {
            if (!mission.completed()) {
                return mission;
            }
        }
        return null;
    }

    private static String formatHours(double hours) {
        if (hours < 1) {
            return String.format(Locale.ROOT, "%.0f min", hours * 60);
        }
        return String.format(Locale.ROOT, "%.0f h", hours);
    }

    private float intro(int index) {
        return Theme.easeOut((time - index * 0.08f) / 0.42f);
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
        game.persist();
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
    }
}
