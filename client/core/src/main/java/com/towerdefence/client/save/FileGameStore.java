package com.towerdefence.client.save;

import com.towerdefence.engine.session.SaveCodec;
import com.towerdefence.engine.session.SaveData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class FileGameStore {
    private final Path path;

    public FileGameStore() {
        this(Path.of(System.getProperty("user.home"), ".towerdefence", "save.txt"));
    }

    public FileGameStore(Path path) {
        this.path = path;
    }

    public Optional<SaveData> load() {
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(SaveCodec.decode(Files.readString(path, StandardCharsets.UTF_8)));
        } catch (IOException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public void save(SaveData data) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, SaveCodec.encode(data), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Local save is best-effort while we iterate on the game.
        }
    }
}
