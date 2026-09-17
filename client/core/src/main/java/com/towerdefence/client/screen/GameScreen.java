package com.towerdefence.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntMap;
import com.towerdefence.client.TowerIdleGame;
import com.towerdefence.client.ui.Button;
import com.towerdefence.client.ui.CurrencyChip;
import com.towerdefence.client.ui.Theme;
import com.towerdefence.client.ui.Ui;
import com.towerdefence.engine.command.GameCommand;
import com.towerdefence.engine.command.UpgradeId;
import com.towerdefence.engine.model.AbilityId;
import com.towerdefence.engine.model.Currency;
import com.towerdefence.engine.model.RunPhase;
import com.towerdefence.engine.session.AutoPrestigeRule;
import com.towerdefence.engine.session.AbilityView;
import com.towerdefence.engine.session.EnemyView;
import com.towerdefence.engine.session.GameSnapshot;
import com.towerdefence.engine.session.ProjectileView;
import com.towerdefence.engine.session.UpgradeView;
import com.towerdefence.engine.sim.EnemyKind;
import com.towerdefence.engine.util.SciFormat;

import java.util.ArrayList;
import java.util.List;

public final class GameScreen implements Screen {
    private static final float W = TowerIdleGame.WORLD_WIDTH;
    private static final float H = TowerIdleGame.WORLD_HEIGHT;

    private static final float DECK_TOP = 292f;
    private static final float ABILITY_Y = 298f;
    private static final float ABILITY_H = 46f;
    private static final float ARENA_BOTTOM = 350f;
    private static final float ARENA_TOP = 742f;
    private static final float LANE_W = 196f;
    private static final float LANE_X = (W - LANE_W) / 2f;
    private static final float LANE_BOTTOM = 370f;
    private static final float LANE_TOP = 700f;
    private static final float CENTER_X = W / 2f;
    private static final float WOBBLE = 22f;

    private static final int CARDS = 6;
    private static final float CARD_W = (W - Theme.PAD * 2f - Theme.GAP * 2f) / 3f;
    private static final float CARD_H = 80f;

    private enum Deck { ATTACK, DEFENSE, STAR }

    private final TowerIdleGame game;
    private final Rectangle[] cards = new Rectangle[CARDS];
    private final Rectangle[] autoBadges = new Rectangle[CARDS];
    private final Rectangle[] deckTabs = new Rectangle[3];
    private final Rectangle autoBuyToggle = new Rectangle();
    private final Rectangle autoPrestToggle = new Rectangle();
    private final Rectangle[] abilitySlots = new Rectangle[AbilityId.values().length];
    private final float[] abilityPress = new float[AbilityId.values().length];
    private final float[] cardPress = new float[CARDS];
    private final Button action = new Button("PRESTIGE", Button.Style.GHOST);
    private final Button menuChip = new Button("MENU", Button.Style.GHOST);
    private final Button pauseChip = new Button("PAUSE", Button.Style.GHOST);
    private final Button collect = new Button("COLLECT & RESTART", Button.Style.PRIMARY);
    private final Button backToMenu = new Button("MENU", Button.Style.GHOST);
    private final Vector3 touch = new Vector3();

    private final IntMap<Track> tracks = new IntMap<>();
    private final IntArray gone = new IntArray();
    private final List<Pop> pops = new ArrayList<>();
    private final List<Impact> impacts = new ArrayList<>();

    private Deck deck = Deck.ATTACK;
    private float time;
    private float recoil;
    private float shake;
    private float banner;
    private float shownCoins;
    private int lastWave;
    private double lastCoins;
    private double lastTowerHp = Double.NaN;
    private int lastProjectileCount;

    public GameScreen(TowerIdleGame game) {
        this.game = game;
        for (int i = 0; i < CARDS; i++) {
            float col = i % 3;
            float row = i / 3;
            float x = Theme.PAD + col * (CARD_W + Theme.GAP);
            float y = row == 0 ? 190f : 106f;
            cards[i] = new Rectangle(x, y, CARD_W, CARD_H);
            autoBadges[i] = new Rectangle(x + CARD_W - 30f, y + CARD_H - 26f, 24f, 20f);
        }
        float tabW = (W - Theme.PAD * 2f - Theme.GAP * 4f) / 5f;
        for (int i = 0; i < deckTabs.length; i++) {
            deckTabs[i] = new Rectangle(Theme.PAD + i * (tabW + Theme.GAP), 74f, tabW, 26f);
        }
        autoBuyToggle.set(Theme.PAD + 3 * (tabW + Theme.GAP), 74f, tabW, 26f);
        autoPrestToggle.set(Theme.PAD + 4 * (tabW + Theme.GAP), 74f, tabW, 26f);
        int slots = abilitySlots.length;
        float slotW = (W - Theme.PAD * 2f - Theme.GAP * (slots - 1)) / slots;
        for (int i = 0; i < slots; i++) {
            abilitySlots[i] = new Rectangle(Theme.PAD + i * (slotW + Theme.GAP), ABILITY_Y, slotW, ABILITY_H);
        }
        action.bounds.set(Theme.PAD, Theme.PAD, W - Theme.PAD * 2f, 50f);
        menuChip.bounds.set(Theme.PAD, 812f, 78f, 30f);
        pauseChip.bounds.set(Theme.PAD + 86f, 812f, 72f, 30f);

        float panelX = 28f;
        float panelW = W - 56f;
        collect.bounds.set(panelX + 20f, 380f, panelW - 40f, 56f);
        backToMenu.bounds.set(panelX + 20f, 320f, panelW - 40f, 44f);
    }

    @Override
    public void show() {
        GameSnapshot snap = game.snapshot();
        lastWave = snap.wave();
        lastCoins = snap.coins();
        shownCoins = (float) snap.coins();
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                game.viewport.unproject(touch.set(screenX, screenY, 0));
                onTouch(touch.x, touch.y);
                return true;
            }
        });
    }

    private List<UpgradeView> deckViews(GameSnapshot snap) {
        return switch (deck) {
            case ATTACK -> snap.attack();
            case DEFENSE -> snap.defense();
            case STAR -> snap.prestige();
        };
    }

    private void onTouch(float x, float y) {
        GameSnapshot snap = game.snapshot();

        if (snap.phase() == RunPhase.DEAD || snap.phase() == RunPhase.CLEARED) {
            if (collect.touch(x, y)) {
                game.command(new GameCommand.StartRun());
                tracks.clear();
                pops.clear();
                impacts.clear();
            } else if (backToMenu.touch(x, y)) {
                game.showMenu();
            }
            return;
        }

        if (menuChip.touch(x, y)) {
            game.showMenu();
            return;
        }
        if (pauseChip.touch(x, y) || game.paused()) {
            game.setPaused(!game.paused());
            return;
        }

        if (autoBuyToggle.contains(x, y) && snap.autoUnlocked()) {
            game.command(new GameCommand.SetAutoBuy(!snap.autoBuy()));
            return;
        }
        if (autoPrestToggle.contains(x, y) && snap.autoPrestigeUnlocked()) {
            var rule = snap.autoPrestige();
            boolean on = !rule.enabled();
            double hp = on && rule.hpPercent() <= 0 ? 25 : rule.hpPercent();
            game.command(new GameCommand.SetAutoPrestige(
                    new AutoPrestigeRule(on, rule.waveTarget(), rule.stallSeconds(), hp, rule.formula())));
            return;
        }

        for (int i = 0; i < deckTabs.length; i++) {
            if (deckTabs[i].contains(x, y)) {
                deck = Deck.values()[i];
                return;
            }
        }

        List<AbilityView> abilities = snap.abilities();
        for (int i = 0; i < abilitySlots.length && i < abilities.size(); i++) {
            if (!abilitySlots[i].contains(x, y)) {
                continue;
            }
            AbilityView ability = abilities.get(i);
            if (ability.ready()) {
                abilityPress[i] = 1f;
                game.command(new GameCommand.CastAbility(ability.id()));
            }
            return;
        }

        List<UpgradeView> views = deckViews(snap);

        // The A badge sits inside the card, so it has to win the hit test.
        for (int i = 0; i < autoBadges.length && i < views.size(); i++) {
            UpgradeView view = views.get(i);
            if (view.autoEligible() && snap.autoUnlocked() && autoBadges[i].contains(x, y)) {
                game.command(new GameCommand.SetAutoTarget(view.id(), !view.autoTarget()));
                return;
            }
        }

        for (int i = 0; i < cards.length && i < views.size(); i++) {
            if (!cards[i].contains(x, y)) {
                continue;
            }
            cardPress[i] = 1f;
            UpgradeView view = views.get(i);
            if (view.id() == UpgradeId.AUTO && snap.autoUnlocked()) {
                game.command(new GameCommand.SetAutoBuy(!snap.autoBuy()));
            } else {
                game.command(new GameCommand.BuyUpgrade(view.id()));
            }
            return;
        }

        if (action.touch(x, y)) {
            if (snap.phase() == RunPhase.RUNNING) {
                game.command(new GameCommand.Prestige());
            } else {
                game.command(new GameCommand.StartRun());
            }
        }
    }

    @Override
    public void render(float delta) {
        time += delta;
        GameSnapshot snap = game.snapshot();
        advanceEffects(delta, snap);

        SpriteBatch batch = game.batch;
        Ui ui = game.ui;
        game.camera.position.set(
                W / 2f + shake * MathUtils.sin(time * 47f) * 5f,
                H / 2f + shake * MathUtils.cos(time * 41f) * 5f,
                0f
        );
        game.camera.update();
        batch.setProjectionMatrix(game.camera.combined);

        batch.begin();
        game.backdrop.draw(batch, ui);
        drawArena(batch, ui, snap);
        drawAbilityBar(batch, ui, snap);
        drawDeck(batch, ui, snap);
        drawHud(batch, ui, snap);
        drawBanner(batch, ui, snap);
        if (game.paused() && snap.phase() == RunPhase.RUNNING) {
            drawPauseOverlay(batch, ui);
        }
        if (snap.phase() == RunPhase.DEAD || snap.phase() == RunPhase.CLEARED) {
            drawEndOverlay(batch, ui, snap);
        }
        batch.end();
    }

    // --- effects ---

    private void advanceEffects(float delta, GameSnapshot snap) {
        recoil = Math.max(0f, recoil - delta * 6f);
        shake = Math.max(0f, shake - delta * 3.2f);
        banner = Math.max(0f, banner - delta);
        action.update(delta);
        menuChip.update(delta);
        pauseChip.update(delta);
        collect.update(delta);
        backToMenu.update(delta);
        for (int i = 0; i < cardPress.length; i++) {
            cardPress[i] = Math.max(0f, cardPress[i] - delta * 4f);
        }
        for (int i = 0; i < abilityPress.length; i++) {
            abilityPress[i] = Math.max(0f, abilityPress[i] - delta * 4f);
        }

        shownCoins += ((float) snap.coins() - shownCoins) * Math.min(1f, delta * 9f);

        if (snap.wave() != lastWave) {
            lastWave = snap.wave();
            banner = 1.5f;
        }
        if (!Double.isNaN(lastTowerHp) && snap.towerHp() < lastTowerHp - 0.001) {
            shake = Math.min(1f, shake + 0.55f);
        }
        lastTowerHp = snap.towerHp();

        int projectileCount = snap.projectiles().size();
        if (projectileCount > lastProjectileCount) {
            recoil = 1f;
        }
        lastProjectileCount = projectileCount;

        trackEnemies(delta, snap);

        for (int i = pops.size() - 1; i >= 0; i--) {
            Pop pop = pops.get(i);
            pop.life += delta;
            pop.y += pop.vy * delta;
            if (pop.life >= pop.ttl) {
                pops.remove(i);
            }
        }
        for (int i = impacts.size() - 1; i >= 0; i--) {
            Impact impact = impacts.get(i);
            impact.life += delta;
            if (impact.life >= impact.ttl) {
                impacts.remove(i);
            }
        }
    }

    private void trackEnemies(float delta, GameSnapshot snap) {
        for (Track track : tracks.values()) {
            track.seen = false;
            track.flash = Math.max(0f, track.flash - delta * 7f);
        }

        for (EnemyView enemy : snap.enemies()) {
            Track track = tracks.get(enemy.id());
            if (track == null) {
                track = new Track();
                track.hp = (float) enemy.hpRatio();
                track.wobble = MathUtils.random(MathUtils.PI2);
                tracks.put(enemy.id(), track);
            }
            if (enemy.hpRatio() < track.hp - 0.0001) {
                track.flash = 1f;
                impacts.add(new Impact(trackX(track), laneY(enemy.pathT()), 20f, Theme.GOLD, 0.2f));
            }
            track.hp = (float) enemy.hpRatio();
            track.pathT = (float) enemy.pathT();
            track.seen = true;
        }

        gone.clear();
        for (IntMap.Entry<Track> entry : tracks.entries()) {
            if (!entry.value.seen) {
                gone.add(entry.key);
            }
        }

        int kills = 0;
        for (int i = 0; i < gone.size; i++) {
            Track track = tracks.remove(gone.get(i));
            if (track == null) {
                continue;
            }
            if (track.pathT >= 0.96f && track.hp > 0.03f) {
                pops.add(new Pop("BREACH", CENTER_X, LANE_TOP - 54f, 24f, 0.9f, Theme.DANGER, Theme.TEXT_SMALL));
                impacts.add(new Impact(CENTER_X, LANE_TOP - 20f, 52f, Theme.DANGER, 0.4f));
            } else {
                kills++;
                impacts.add(new Impact(trackX(track), laneY(track.pathT), 32f, Theme.ACCENT, 0.3f));
            }
        }

        double coinDelta = snap.coins() - lastCoins;
        lastCoins = snap.coins();
        if (kills > 0 && coinDelta > 0 && pops.size() < 20) {
            pops.add(new Pop("+" + SciFormat.of(coinDelta / kills),
                    CENTER_X + MathUtils.random(-40f, 40f), laneY(0.6f), 32f, 0.75f,
                    Theme.GOLD, Theme.TEXT_SMALL));
        }
    }

    private float trackX(Track track) {
        return CENTER_X + MathUtils.sin(track.wobble + track.pathT * 4.5f) * WOBBLE;
    }

    private static float laneY(double pathT) {
        return LANE_BOTTOM + (float) pathT * (LANE_TOP - LANE_BOTTOM);
    }

    private static Color enemyColor(EnemyKind kind) {
        return switch (kind) {
            case BASIC -> Theme.DANGER;
            case FAST -> Theme.ENEMY_FAST;
            case TANK -> Theme.ENEMY_TANK;
            case RANGED -> Theme.ENEMY_RANGED;
            case PROTECTOR -> Theme.ENEMY_PROTECTOR;
            case BOSS -> Theme.ELITE;
        };
    }

    private static float enemyRadius(EnemyKind kind) {
        return switch (kind) {
            case BASIC, FAST -> 11f;
            case RANGED -> 10f;
            case TANK, PROTECTOR -> 14f;
            case BOSS -> 17f;
        };
    }

    // --- arena ---

    private void drawArena(SpriteBatch batch, Ui ui, GameSnapshot snap) {
        ui.panel(batch, LANE_X, ARENA_BOTTOM, LANE_W, ARENA_TOP - ARENA_BOTTOM, Theme.LANE, 0.94f);
        ui.panel(batch, LANE_X + 6f, ARENA_BOTTOM + 6f, 3f, ARENA_TOP - ARENA_BOTTOM - 12f, Theme.STROKE, 0.55f);
        ui.panel(batch, LANE_X + LANE_W - 9f, ARENA_BOTTOM + 6f, 3f, ARENA_TOP - ARENA_BOTTOM - 12f, Theme.STROKE, 0.55f);

        float spacing = 54f;
        float offset = (time * 26f) % spacing;
        for (float y = LANE_BOTTOM + offset; y < LANE_TOP - 16f; y += spacing) {
            ui.rect(batch, CENTER_X - 1.5f, y, 3f, 18f, Theme.STROKE, 0.4f);
        }

        ui.rect(batch, LANE_X + 16f, LANE_BOTTOM - 8f, LANE_W - 32f, 2f, Theme.DANGER, 0.35f);
        ui.text(batch, "SPAWN", CENTER_X, LANE_BOTTOM - 20f, Theme.TEXT_TINY, ui.fade(Theme.TEXT_DIM, 0.7f), Ui.CENTER);

        float rangeY = laneY(1.0 - snap.range());
        if (rangeY > LANE_BOTTOM + 10f) {
            for (float x = LANE_X + 14f; x < LANE_X + LANE_W - 14f; x += 15f) {
                ui.rect(batch, x, rangeY, 8f, 1.5f, Theme.ACCENT, 0.28f);
            }
            ui.text(batch, "RANGE", LANE_X + 14f, rangeY + 13f, Theme.TEXT_TINY, ui.fade(Theme.ACCENT, 0.5f), Ui.LEFT);
        }

        if (snap.thornsDps() > 0) {
            float thornsY = laneY(0.88);
            for (float x = LANE_X + 12f; x < LANE_X + LANE_W - 12f; x += 12f) {
                ui.triangle(batch, x, thornsY, 4f, Theme.OK, 0.45f);
            }
        }

        drawEnemies(batch, ui, snap);
        drawProjectiles(batch, ui, snap);

        for (Impact impact : impacts) {
            float t = impact.life / impact.ttl;
            ui.glow(batch, impact.x, impact.y, impact.radius * (0.4f + t), impact.color, (1f - t) * 0.75f);
        }

        drawTower(batch, ui);

        for (Pop pop : pops) {
            float t = pop.life / pop.ttl;
            ui.text(batch, pop.text, pop.x, pop.y, pop.scale, ui.fade(pop.color, 1f - t * t), Ui.CENTER);
        }
    }

    private void drawEnemies(SpriteBatch batch, Ui ui, GameSnapshot snap) {
        for (EnemyView enemy : snap.enemies()) {
            Track track = tracks.get(enemy.id());
            float x = track == null ? CENTER_X : trackX(track);
            float y = laneY(enemy.pathT());
            float flash = track == null ? 0f : track.flash;
            float hp = (float) enemy.hpRatio();
            EnemyKind kind = enemy.kind();
            Color body = enemyColor(kind);
            float radius = enemyRadius(kind);

            ui.glow(batch, x, y, radius * 2f, body, 0.22f + flash * 0.4f);
            switch (kind) {
                case BASIC -> {
                    ui.circle(batch, x, y, radius, body, 1f);
                    ui.circle(batch, x, y, radius * 0.6f, Theme.BG_TOP, 0.7f);
                }
                case FAST -> {
                    ui.triangle(batch, x, y, radius, body, 1f);
                    ui.triangle(batch, x, y - radius * 0.2f, radius * 0.45f, Theme.BG_TOP, 0.6f);
                }
                case TANK -> {
                    ui.panel(batch, x - radius, y - radius, radius * 2f, radius * 2f, body, 1f);
                    ui.panel(batch, x - radius * 0.5f, y - radius * 0.5f, radius, radius, Theme.BG_TOP, 0.6f);
                }
                case RANGED -> {
                    ui.ring(batch, x, y, radius, body, 1f);
                    ui.circle(batch, x, y, radius * 0.3f, body, 1f);
                    // Muzzle flare pointing at the tower.
                    ui.rect(batch, x - 1.5f, y + radius, 3f, 7f, body, 0.8f);
                }
                case PROTECTOR -> {
                    ui.diamond(batch, x, y, radius, body, 1f);
                    ui.diamond(batch, x, y, radius * 0.45f, Theme.BG_TOP, 0.6f);
                }
                case BOSS -> {
                    ui.ring(batch, x, y, radius + 5f + MathUtils.sin(time * 3.4f) * 1.6f, body, 0.55f);
                    ui.circle(batch, x, y, radius, body, 1f);
                    ui.circle(batch, x, y, radius * 0.62f, Theme.BG_TOP, 0.7f);
                    ui.circle(batch, x, y, radius * 0.36f, body, 0.5f + 0.5f * hp);
                }
            }

            if (enemy.shielded()) {
                ui.ring(batch, x, y, radius + 4f, Theme.ENEMY_PROTECTOR, 0.35f);
            }
            if (flash > 0f) {
                ui.circle(batch, x, y, radius + 1f, Theme.TEXT, flash * 0.5f);
            }
            if (hp < 0.999f) {
                float barW = radius * 2f + 8f;
                ui.rect(batch, x - barW / 2f, y + radius + 5f, barW, 2.5f, Theme.SURFACE_LO, 0.9f);
                ui.rect(batch, x - barW / 2f, y + radius + 5f, barW * hp, 2.5f, body, 0.95f);
            }
        }
    }

    private void drawProjectiles(SpriteBatch batch, Ui ui, GameSnapshot snap) {
        for (ProjectileView projectile : snap.projectiles()) {
            Track target = tracks.get(projectile.targetId());
            float y = laneY(projectile.pathT());
            float x = CENTER_X;
            if (target != null) {
                float span = Math.max(0.001f, 1f - target.pathT);
                float progress = MathUtils.clamp((1f - (float) projectile.pathT()) / span, 0f, 1f);
                x = MathUtils.lerp(CENTER_X, trackX(target), progress);
            }
            Color tint = projectile.crit() ? Theme.TEXT : Theme.GOLD;
            float size = projectile.crit() ? 4.4f : 3.2f;
            ui.rect(batch, x - 1.5f, y, 3f, 16f, tint, 0.35f);
            ui.glow(batch, x, y, projectile.crit() ? 15f : 11f, tint, 0.6f);
            ui.circle(batch, x, y, size, Theme.TEXT, 0.95f);
        }
    }

    private void drawTower(SpriteBatch batch, Ui ui) {
        float lift = recoil * 6f;
        float baseY = LANE_TOP + 2f;

        ui.glow(batch, CENTER_X, LANE_TOP, 74f, Theme.ACCENT, 0.1f + recoil * 0.3f);
        ui.rect(batch, CENTER_X - 4.5f, LANE_TOP - 24f + lift, 9f, 34f, Theme.ACCENT, 0.85f);
        ui.rect(batch, CENTER_X - 7f, LANE_TOP - 26f + lift, 14f, 5f, Theme.ACCENT, 1f);
        if (recoil > 0f) {
            ui.glow(batch, CENTER_X, LANE_TOP - 26f + lift, 24f, Theme.GOLD, recoil * 0.8f);
        }
        ui.panelOutlined(batch, CENTER_X - 52f, baseY + lift * 0.35f, 104f, 38f, Theme.SURFACE_HI, Theme.ACCENT, 1.5f);
        ui.circle(batch, CENTER_X, baseY + 19f + lift * 0.35f, 9f, Theme.ACCENT, 0.9f);
        ui.circle(batch, CENTER_X, baseY + 19f + lift * 0.35f, 4f, Theme.BG_TOP, 0.9f);
    }

    // --- abilities ---

    private void drawAbilityBar(SpriteBatch batch, Ui ui, GameSnapshot snap) {
        List<AbilityView> abilities = snap.abilities();
        for (int i = 0; i < abilitySlots.length && i < abilities.size(); i++) {
            AbilityView ability = abilities.get(i);
            Rectangle box = abilitySlots[i];
            float squash = abilityPress[i] * 2f;
            float x = box.x + squash;
            float y = box.y + squash * 0.5f;
            float w = box.width - squash * 2f;
            float h = box.height - squash;

            Color accent = abilityColor(ability.id());
            boolean active = ability.activeRemaining() > 0;
            Color stroke = active ? accent : ability.ready() ? Theme.OK : Theme.STROKE;
            ui.panelOutlined(batch, x, y, w, h,
                    ability.unlocked() ? Theme.SURFACE : Theme.SURFACE_LO, stroke, 1.2f);

            // Cooldown eats the tile from the left as it ticks down.
            float ratio = ability.cooldownRatio();
            if (ratio > 0f) {
                ui.panel(batch, x, y, Math.max(2f, w * ratio), h, Theme.BG_TOP, 0.62f);
            }
            if (active) {
                ui.glow(batch, x + w / 2f, y + h / 2f, w * 0.6f, accent, 0.3f);
            }

            Color label = ability.unlocked()
                    ? (ability.ready() || active ? Theme.TEXT : Theme.TEXT_DIM)
                    : Theme.STROKE;
            ui.textFit(batch, abilityLabel(ability.id()), x + w / 2f, y + h - 15f, w - 8f,
                    Theme.TEXT_TINY, label, Ui.CENTER);

            String status;
            if (!ability.unlocked()) {
                status = "LOCKED";
            } else if (active) {
                status = String.format("%.0fs", ability.activeRemaining());
            } else if (ability.cooldownRemaining() > 0) {
                status = String.format("%.0fs", ability.cooldownRemaining());
            } else {
                status = "READY";
            }
            ui.textFit(batch, status, x + w / 2f, y + 14f, w - 8f, Theme.TEXT_TINY,
                    ability.ready() ? Theme.OK : ui.fade(Theme.TEXT_DIM, 0.95f), Ui.CENTER);
        }
    }

    private static Color abilityColor(AbilityId id) {
        return switch (id) {
            case GOLDEN_TOWER -> Theme.GOLD;
            case DEATH_WAVE -> Theme.DANGER;
            case CHAIN_LIGHTNING -> Theme.ACCENT;
            case BLACK_HOLE -> Theme.ENEMY_TANK;
        };
    }

    private static String abilityLabel(AbilityId id) {
        return switch (id) {
            case GOLDEN_TOWER -> "GOLDEN";
            case DEATH_WAVE -> "D.WAVE";
            case CHAIN_LIGHTNING -> "CHAIN";
            case BLACK_HOLE -> "B.HOLE";
        };
    }

    // --- deck ---

    private void drawDeck(SpriteBatch batch, Ui ui, GameSnapshot snap) {
        ui.rect(batch, 0, 0, W, DECK_TOP, Theme.BG_BOTTOM, 0.96f);
        ui.rect(batch, 0, DECK_TOP - 2f, W, 2f, Theme.STROKE, 0.8f);

        drawDeckTab(batch, ui, deckTabs[0], "ATK", deck == Deck.ATTACK);
        drawDeckTab(batch, ui, deckTabs[1], "DEF", deck == Deck.DEFENSE);
        drawDeckTab(batch, ui, deckTabs[2], "★", deck == Deck.STAR);
        drawToggle(batch, ui, autoBuyToggle, "BUY", snap.autoUnlocked(), snap.autoBuy());
        drawToggle(batch, ui, autoPrestToggle, "PREST", snap.autoPrestigeUnlocked(),
                snap.autoPrestige().enabled());

        List<UpgradeView> views = deckViews(snap);
        for (int i = 0; i < cards.length && i < views.size(); i++) {
            drawCard(batch, ui, cards[i], autoBadges[i], views.get(i), cardPress[i], snap);
        }

        boolean running = snap.phase() == RunPhase.RUNNING;
        action.enabled = true;
        action.label = running
                ? "PRESTIGE  +" + snap.pendingStars() + " ★"
                : "START RUN";
        action.draw(batch, ui, Theme.TEXT_BODY);
    }

    private void drawDeckTab(SpriteBatch batch, Ui ui, Rectangle box, String label, boolean active) {
        ui.panel(batch, box.x, box.y, box.width, box.height,
                active ? Theme.SURFACE_HI : Theme.SURFACE_LO, active ? 1f : 0.85f);
        ui.textFit(batch, label, box.x + box.width / 2f, box.y + box.height / 2f, box.width - 8f,
                Theme.TEXT_TINY, active ? Theme.ACCENT : Theme.TEXT_DIM, Ui.CENTER);
    }

    private void drawToggle(SpriteBatch batch, Ui ui, Rectangle box, String label,
                            boolean unlocked, boolean on) {
        Color stroke = !unlocked ? Theme.STROKE : on ? Theme.OK : Theme.STROKE;
        ui.panelOutlined(batch, box.x, box.y, box.width, box.height,
                on && unlocked ? Theme.SURFACE_HI : Theme.SURFACE_LO, stroke, 1.2f);
        String text = unlocked ? (on ? label + " ON" : label) : label;
        ui.textFit(batch, text, box.x + box.width / 2f, box.y + box.height / 2f, box.width - 6f,
                Theme.TEXT_TINY, unlocked ? (on ? Theme.OK : Theme.TEXT) : Theme.TEXT_DIM, Ui.CENTER);
    }

    private void drawCard(SpriteBatch batch, Ui ui, Rectangle box, Rectangle badge,
                          UpgradeView view, float press, GameSnapshot snap) {
        float squash = press * 2.5f;
        float x = box.x + squash;
        float y = box.y + squash * 0.5f;
        float w = box.width - squash * 2f;
        float h = box.height - squash;
        float inner = w - Theme.GAP * 2f;

        boolean autoOn = view.id() == UpgradeId.AUTO && snap.autoBuy();
        boolean showBadge = view.autoEligible() && snap.autoUnlocked();
        Color stroke = autoOn ? Theme.OK : view.affordable() ? Theme.ACCENT : Theme.STROKE;
        Color fill = view.affordable() || autoOn ? Theme.SURFACE_HI : Theme.SURFACE;
        ui.panelOutlined(batch, x, y, w, h, fill, stroke, 1.5f);

        Color labelColor = view.affordable() || autoOn ? Theme.TEXT : Theme.TEXT_DIM;
        float headerRight = showBadge ? 58f : 26f;
        ui.textFit(batch, label(view.id()), x + Theme.GAP, y + h - 17f, inner - headerRight,
                Theme.TEXT_SMALL, labelColor, Ui.LEFT);
        ui.textFit(batch, "L" + view.level(), x + w - Theme.GAP - (showBadge ? 32f : 0f), y + h - 17f, 26f,
                Theme.TEXT_TINY, Theme.TEXT_DIM, Ui.RIGHT);
        if (showBadge) {
            boolean on = view.autoTarget();
            ui.panelOutlined(batch, badge.x, badge.y, badge.width, badge.height,
                    on ? Theme.OK : Theme.SURFACE_LO, on ? Theme.OK : Theme.STROKE, 1.2f);
            ui.textFit(batch, "A", badge.x + badge.width / 2f, badge.y + badge.height / 2f, badge.width - 6f,
                    Theme.TEXT_TINY, on ? Theme.BG_TOP : Theme.TEXT_DIM, Ui.CENTER);
        }
        ui.textFit(batch, view.effect(), x + Theme.GAP, y + h / 2f - 4f, inner, Theme.TEXT_TINY,
                ui.fade(Theme.ACCENT, 0.9f), Ui.LEFT);

        float pillY = y + 9f;
        ui.panel(batch, x + Theme.GAP, pillY, inner, 22f, Theme.SURFACE_LO, 0.9f);
        if (view.id() == UpgradeId.AUTO && snap.autoUnlocked()) {
            ui.textFit(batch, snap.autoBuy() ? "PAUSE" : "RESUME", x + w / 2f, pillY + 11f, inner - 10f,
                    Theme.TEXT_TINY, Theme.OK, Ui.CENTER);
        } else {
            Color costColor = view.affordable() ? CurrencyChip.color(view.currency()) : Theme.TEXT_DIM;
            CurrencyChip.icon(batch, ui, view.currency(), x + Theme.GAP + 12f, pillY + 11f, 5f);
            ui.textFit(batch, SciFormat.of(view.cost()), x + w - Theme.GAP - 7f, pillY + 11f, inner - 30f,
                    Theme.TEXT_TINY, costColor, Ui.RIGHT);
        }
    }

    private static String label(UpgradeId id) {
        return switch (id) {
            case POWER -> "POWER";
            case TEMPO -> "TEMPO";
            case REACH -> "REACH";
            case MULTISHOT -> "MULTI";
            case CRIT -> "CRIT";
            case BOUNCE -> "BOUNCE";
            case HEALTH -> "HEALTH";
            case REGEN -> "REGEN";
            case WALL -> "WALL";
            case WALL_REGEN -> "W.REGEN";
            case THORNS -> "THORNS";
            case KNOCKBACK -> "PUSH";
            case FOUNDATION -> "BASE";
            case FORTUNE -> "FORTUNE";
            case AUTO -> "AUTO";
            case STAR_DAMAGE -> "★ DMG";
            case STAR_HP -> "★ HP";
            case STAR_COIN -> "★ COIN";
            case STAR_REGEN -> "★ REG";
            default -> id.name();
        };
    }

    // --- hud ---

    private void drawHud(SpriteBatch batch, Ui ui, GameSnapshot snap) {
        ui.rect(batch, 0, ARENA_TOP, W, H - ARENA_TOP, Theme.BG_BOTTOM, 0.95f);
        ui.rect(batch, 0, ARENA_TOP, W, 2f, Theme.STROKE, 0.8f);

        menuChip.draw(batch, ui, Theme.TEXT_TINY);
        pauseChip.label = game.paused() ? "PLAY" : "PAUSE";
        pauseChip.draw(batch, ui, Theme.TEXT_TINY);
        ui.textFit(batch, "WAVE " + snap.wave() + " / " + snap.wavesPerTier(),
                CENTER_X, 826f, 210f, Theme.TEXT_H1, Theme.TEXT, Ui.CENTER);

        float pillW = 84f;
        float pillX = W - Theme.PAD - pillW;
        ui.panelOutlined(batch, pillX, 812f, pillW, 28f, Theme.SURFACE_HI, Theme.ACCENT, 1.2f);
        ui.textFit(batch, "TIER " + snap.tier(), pillX + pillW / 2f, 826f, pillW - 10f, Theme.TEXT_SMALL,
                Theme.ACCENT, Ui.CENTER);

        if (snap.gameSpeed() > 1.001) {
            ui.textFit(batch, String.format("x%.1f", snap.gameSpeed()), pillX - 8f, 826f, 44f,
                    Theme.TEXT_TINY, Theme.ELITE, Ui.RIGHT);
        }

        float chipW = (W - Theme.PAD * 2f - Theme.GAP * 3f) / 4f;
        CurrencyChip.compact(batch, ui, Theme.PAD, 780f, chipW, 28f, Currency.COIN, SciFormat.of(shownCoins));
        CurrencyChip.compact(batch, ui, Theme.PAD + chipW + Theme.GAP, 780f, chipW, 28f,
                Currency.SHARD, SciFormat.of(snap.shards()));
        CurrencyChip.compact(batch, ui, Theme.PAD + (chipW + Theme.GAP) * 2f, 780f, chipW, 28f,
                Currency.CORE, SciFormat.of(snap.cores()));
        CurrencyChip.compact(batch, ui, Theme.PAD + (chipW + Theme.GAP) * 3f, 780f, chipW, 28f,
                Currency.STAR, SciFormat.of(snap.stars()));

        float hpRatio = (float) (snap.towerHp() / Math.max(1d, snap.towerMaxHp()));
        Color hpColor = hpRatio < 0.3f ? Theme.DANGER : Theme.OK;
        ui.textFit(batch, "HP " + SciFormat.of(snap.towerHp()) + " / " + SciFormat.of(snap.towerMaxHp()),
                Theme.PAD, 770f, 200f, Theme.TEXT_TINY, Theme.TEXT, Ui.LEFT);
        ui.textFit(batch, "+" + SciFormat.of(snap.regenPerSecond()) + " hp/s",
                W - Theme.PAD, 770f, 130f, Theme.TEXT_TINY, ui.fade(Theme.OK, 0.85f), Ui.RIGHT);
        ui.bar(batch, Theme.PAD, 752f, W - Theme.PAD * 2f, 8f, hpRatio, Theme.SURFACE_LO, hpColor);

        if (snap.wallMax() > 0) {
            float wallRatio = (float) (snap.wall() / snap.wallMax());
            ui.bar(batch, Theme.PAD, 744f, W - Theme.PAD * 2f, 5f, wallRatio, Theme.SURFACE_LO, Theme.WALL);
        }
    }

    private void drawBanner(SpriteBatch batch, Ui ui, GameSnapshot snap) {
        if (banner <= 0f) {
            return;
        }
        float slide = (1f - Theme.easeOut((1.5f - banner) * 4f)) * 70f;
        float alpha = banner > 1.1f ? Theme.easeOut((1.5f - banner) * 4f) : Math.min(1f, banner / 1.1f);
        float h = 44f;
        float y = (ARENA_BOTTOM + ARENA_TOP) / 2f - h / 2f;
        Color accent = snap.eliteWave() ? Theme.ELITE : Theme.ACCENT;
        String label = snap.eliteWave() ? "WAVE " + snap.wave() + "  -  BOSS" : "WAVE " + snap.wave();

        ui.panel(batch, Theme.PAD - slide, y, W - Theme.PAD * 2f, h, Theme.SURFACE_HI, alpha * 0.94f);
        ui.rect(batch, Theme.PAD - slide + 6f, y + 6f, 4f, h - 12f, accent, alpha);
        ui.textFit(batch, label, W / 2f - slide, y + h / 2f, W - Theme.PAD * 4f, Theme.TEXT_H1,
                ui.fade(snap.eliteWave() ? Theme.ELITE : Theme.TEXT, alpha), Ui.CENTER);
    }

    private void drawPauseOverlay(SpriteBatch batch, Ui ui) {
        ui.rect(batch, 0, 0, W, H, Theme.BG_TOP, 0.55f);
        ui.panelOutlined(batch, 70f, 390f, W - 140f, 90f, Theme.SURFACE, Theme.ACCENT, 1.5f);
        ui.textTracked(batch, "PAUSED", W / 2f, 448f, Theme.TEXT_H1, 3f, Theme.ACCENT);
        ui.textFit(batch, "tap to resume  -  menu still works",
                W / 2f, 418f, W - 180f, Theme.TEXT_TINY, Theme.TEXT_DIM, Ui.CENTER);
    }

    private void drawEndOverlay(SpriteBatch batch, Ui ui, GameSnapshot snap) {
        boolean cleared = snap.phase() == RunPhase.CLEARED;
        ui.rect(batch, 0, 0, W, H, Theme.BG_TOP, 0.8f);

        float x = 28f;
        float w = W - 56f;
        float y = 300f;
        float h = 280f;
        ui.panelOutlined(batch, x, y, w, h, Theme.SURFACE, cleared ? Theme.OK : Theme.DANGER, 1.5f);

        ui.textTracked(batch, cleared ? "TIER CLEARED" : "RUN LOST", x + w / 2f, y + h - 38f,
                Theme.TEXT_H1, 3f, cleared ? Theme.OK : Theme.DANGER);
        ui.textFit(batch, "tier " + snap.tier() + "  -  wave " + snap.wave() + " / " + snap.wavesPerTier(),
                x + w / 2f, y + h - 72f, w - Theme.PAD * 2f, Theme.TEXT_BODY, Theme.TEXT, Ui.CENTER);

        if (cleared) {
            ui.textFit(batch, "shards from kills stay  -  next floor is waiting",
                    x + w / 2f, y + h - 112f, w - Theme.PAD * 2f, Theme.TEXT_SMALL, Theme.SHARD, Ui.CENTER);
            collect.label = "NEXT RUN";
        } else {
            ui.textFit(batch, "shards from kills stay  -  the run is over",
                    x + w / 2f, y + h - 112f, w - Theme.PAD * 2f, Theme.TEXT_SMALL, Theme.SHARD, Ui.CENTER);
            collect.label = "TRY AGAIN";
        }
        ui.textFit(batch, "auto-prestige can cash out on low hp",
                x + w / 2f, y + h - 140f, w - Theme.PAD * 2f, Theme.TEXT_TINY,
                ui.fade(Theme.TEXT_DIM, 0.9f), Ui.CENTER);

        collect.draw(batch, ui, Theme.TEXT_BODY);
        backToMenu.draw(batch, ui, Theme.TEXT_SMALL);
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

    private static final class Track {
        float pathT;
        float hp = 1f;
        float flash;
        float wobble;
        boolean seen;
    }

    private static final class Pop {
        final String text;
        final float x;
        final float vy;
        final float ttl;
        final Color color;
        final float scale;
        float y;
        float life;

        Pop(String text, float x, float y, float vy, float ttl, Color color, float scale) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.vy = vy;
            this.ttl = ttl;
            this.color = color;
            this.scale = scale;
        }
    }

    private static final class Impact {
        final float x;
        final float y;
        final float radius;
        final Color color;
        final float ttl;
        float life;

        Impact(float x, float y, float radius, Color color, float ttl) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.color = color;
            this.ttl = ttl;
        }
    }
}
