package com.towerdefence.client;

/** What the simulation produced while the app was closed, shown once on the menu. */
public record OfflineReport(long awayMillis, int wavesGained, double coinsGained, boolean runEnded) {
    public String awayText() {
        long minutes = awayMillis / 60_000L;
        long hours = minutes / 60L;
        if (hours > 0) {
            return hours + "h " + (minutes % 60L) + "m";
        }
        return minutes + "m";
    }
}
