package com.mess1on.combinedstatus;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

final class Motion {
    static final Set<Object> SEEN = Collections.newSetFromMap(new WeakHashMap<>());
    static Object host;

    private Motion() {
    }

    static int batteryWidthOffset(int mergedWidth, int stockWidth, boolean merge) {
        if (!merge || mergedWidth <= 0 || stockWidth <= 0) {
            return 0;
        }
        return Math.max(0, mergedWidth - stockWidth);
    }

    static float clamp(float v) {
        if (!Float.isFinite(v)) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    static int clampLevel(int level) {
        return Math.max(0, Math.min(4, level));
    }

    static float ease(float v) {
        float c = clamp(v);
        float inv = 1.0f - c;
        return 1.0f - (inv * inv * inv);
    }

    static float mix(float a, float b, float t) {
        return a + ((b - a) * clamp(t));
    }

    static float nativeAlpha(float progress) {
        float c = clamp(progress);
        if (c <= 0.18f) {
            return 0.0f;
        }
        return clamp((c - 0.18f) / 0.82f);
    }

    static int nativeLevel(String resourceName) {
        if (resourceName == null || resourceName.isEmpty()) {
            return -1;
        }
        for (int i = resourceName.length() - 1; i >= 0; i--) {
            char ch = resourceName.charAt(i);
            if (ch >= '0' && ch <= '4') {
                if ((i == 0 || !Character.isDigit(resourceName.charAt(i - 1)))
                    && (i + 1 == resourceName.length() || !Character.isDigit(resourceName.charAt(i + 1)))) {
                    return ch - '0';
                }
            }
        }
        return -1;
    }

    static int signalLevel(int fallbackLevel, Integer nativeLevel) {
        if (nativeLevel != null && nativeLevel >= 0) {
            return clampLevel(nativeLevel);
        }
        return clampLevel(fallbackLevel);
    }

    static int width(int mergedWidth, int stockWidth, float progress) {
        return Math.round(mix(mergedWidth, stockWidth, ease(progress)));
    }
}
