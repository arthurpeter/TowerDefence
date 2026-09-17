package com.towerdefence.engine.util;

public final class SciFormat {
    private SciFormat() {}

    public static String of(double value) {
        double abs = Math.abs(value);
        if (abs < 1_000d) {
            return strip(String.format(java.util.Locale.ROOT, "%.1f", value));
        }
        if (abs < 1_000_000d) {
            return strip(String.format(java.util.Locale.ROOT, "%.2fK", value / 1_000d));
        }
        if (abs < 1_000_000_000d) {
            return strip(String.format(java.util.Locale.ROOT, "%.2fM", value / 1_000_000d));
        }
        if (abs < 1_000_000_000_000d) {
            return strip(String.format(java.util.Locale.ROOT, "%.2fB", value / 1_000_000_000d));
        }
        return String.format(java.util.Locale.ROOT, "%.2e", value);
    }

    private static String strip(String formatted) {
        if (formatted.contains("e") || formatted.contains("E")) {
            return formatted;
        }
        return formatted.replaceAll("\\.?0+([KMB]?)$", "$1");
    }
}
