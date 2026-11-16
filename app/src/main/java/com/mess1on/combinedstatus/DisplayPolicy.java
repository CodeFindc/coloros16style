package com.mess1on.combinedstatus;

final class DisplayPolicy {
    static final int DEFAULT_LOW_BATTERY_THRESHOLD = 15;

    private DisplayPolicy() {
    }

    static boolean attach(int from, boolean lockscreen, boolean aod) {
        if (from != 1) {
            return true;
        }
        return lockscreen || aod;
    }

    static int batteryTint(int level, boolean charging, boolean colourEnabled, int normalTint, int threshold) {
        if (!colourEnabled) {
            return normalTint;
        }
        if (charging) {
            return -13315748; // #FF34C759
        }
        if (level >= 0 && level <= lowBatteryThreshold(threshold)) {
            return -278483; // #FFFF3B30
        }
        return normalTint;
    }

    static boolean enabled(int mode, int from, boolean ambient, boolean lockscreen, boolean aod, boolean keyguardShowing) {
        if (mode(mode) == 0) {
            return false;
        }
        if (ambient) {
            return aod;
        }
        if (from == 1 || keyguardShowing) {
            return lockscreen;
        }
        return true;
    }

    static float expansion(int from, boolean ambient, float shade, float control) {
        if (ambient || from == 1 || from == 3 || from == 4) {
            return 0.0f;
        }
        return Motion.clamp(Math.max(shade, control));
    }

    static boolean keepPrivacyRenderer(boolean enabled, String privacyState, boolean batteryVisible, boolean islandTaking) {
        return !enabled || privacyTransition(privacyState) || !batteryVisible || islandTaking;
    }

    static int lowBatteryThreshold(int threshold) {
        return Math.max(5, Math.min(50, threshold));
    }

    static int mode(int mode) {
        return mode == 0 ? 0 : 1;
    }

    static boolean privacyTransition(String privacyState) {
        return "START_SHOW_PRIVACY".equals(privacyState) || "STOP_SHOW_PRIVACY".equals(privacyState);
    }

    static int textWeight(int weight) {
        return Math.max(300, Math.min(800, Math.round((weight(weight) * 5.0f) + 50.0f)));
    }

    static int weight(int weight) {
        if (weight <= 0) {
            return 100;
        }
        return Math.max(70, Math.min(135, weight));
    }
}
