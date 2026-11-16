package com.mess1on.combinedstatus;

final class BatteryHeader {
    private static final float BASE_SIZE = 33.8f;

    private BatteryHeader() {
    }

    static float centreY() {
        return 10.0f;
    }

    static int fromLegacy(int v) {
        if (v >= 70) {
            return scale(v);
        }
        return scale((v * 2) + 80);
    }

    static float halfGap(float textWidth, float strokeWidth) {
        if (textWidth <= 0.0f) {
            return 20.0f;
        }
        return Math.min(46.0f, Math.max(20.0f, ((textWidth + strokeWidth) * 0.62f) + 5.0f));
    }

    static int scale(int v) {
        if (v <= 0) {
            return 100;
        }
        return Math.max(70, Math.min(160, v));
    }

    static float textSize(int v) {
        return (scale(v) * BASE_SIZE) / 100.0f;
    }

    static int toLegacy(int v) {
        int s = scale(v);
        if (s <= 80) {
            return 0;
        }
        return Math.min(30, Math.round((s - 80) / 2.0f));
    }
}
