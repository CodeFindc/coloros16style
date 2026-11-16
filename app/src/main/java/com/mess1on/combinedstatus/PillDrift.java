package com.mess1on.combinedstatus;

final class PillDrift {
    static final long INTERVAL_MS = 60000L;
    private static final int[] X = new int[]{-1, 0, 1, 1, 1, 0, -1, -1, 0};
    private static final int[] Y = new int[]{-1, -1, -1, 0, 1, 1, 1, 0, 0};
    private int forcedCount;
    private long intervalMs = INTERVAL_MS;
    private long lastStepAt = -1L;
    private int phase;
    private int stepCount;

    PillDrift() {
    }

    static float fade(float shadeFraction) {
        if (shadeFraction <= 0.0f) {
            return 1.0f;
        }
        if (shadeFraction >= 0.2f) {
            return 0.0f;
        }
        return 1.0f - (shadeFraction / 0.2f);
    }

    static float offset(int unitDir, float fadeFactor, int maxPx) {
        if (maxPx <= 0 || unitDir == 0 || fadeFactor <= 0.0f) {
            return 0.0f;
        }
        int clamped = maxPx > 6 ? 6 : maxPx;
        return unitDir * clamped * fadeFactor;
    }

    static int roomX(float viewWidth, float contentWidth, int driftPx) {
        return Math.max(0, Math.round((viewWidth - contentWidth) * 0.5f));
    }

    static int roomY(float viewHeight, float contentHeight, int driftPx) {
        int d = driftPx <= 0 ? 0 : (driftPx > 6 ? 6 : driftPx);
        return Math.min(d, Math.max(0, Math.round((viewHeight - contentHeight) * 0.5f)));
    }

    long delay(long nowMs) {
        if (this.lastStepAt < 0L) {
            return this.intervalMs;
        }
        long elapsed = nowMs - this.lastStepAt;
        if (elapsed >= this.intervalMs) {
            return 1000L;
        }
        return Math.max(1000L, this.intervalMs - elapsed);
    }

    void forceStep() {
        this.phase = (this.phase + 1) % X.length;
        this.forcedCount++;
    }

    int forced() {
        return this.forcedCount;
    }

    void interval(long sec) {
        long clamped = sec < 5L ? 5L : (sec > 600L ? 600L : sec);
        this.intervalMs = clamped * 1000L;
    }

    long lastAt() {
        return this.lastStepAt;
    }

    int phase() {
        return this.phase;
    }

    int steps() {
        return this.stepCount;
    }

    boolean update(long nowMs, boolean enabled) {
        if (!enabled) {
            boolean changed = this.phase != 8;
            this.phase = 8;
            this.lastStepAt = -1L;
            return changed;
        }
        if (this.lastStepAt < 0L) {
            this.lastStepAt = nowMs;
            return false;
        }
        if (nowMs - this.lastStepAt < this.intervalMs) {
            return false;
        }
        this.lastStepAt = nowMs;
        this.phase = (this.phase + 1) % X.length;
        this.stepCount++;
        return true;
    }

    int x() {
        return X[this.phase];
    }

    int y() {
        return Y[this.phase];
    }
}
