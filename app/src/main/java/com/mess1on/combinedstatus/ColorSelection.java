package com.mess1on.combinedstatus;

final class ColorSelection {
    private ColorSelection() {
    }

    static float fraction(float x, float pad, float width) {
        float span = width - (2.0f * pad);
        if (span <= 0.0f) {
            return 0.0f;
        }
        float f = (x - pad) / span;
        if (f < 0.0f) {
            return 0.0f;
        }
        if (f > 1.0f) {
            return 1.0f;
        }
        return f;
    }
}
