package com.mess1on.combinedstatus;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

final class PillRing {
    private static final RectF ARC = new RectF(10.0f, 8.0f, 110.0f, 108.0f);
    private static final Paint STROKE = new Paint(Paint.ANTI_ALIAS_FLAG);
    static float forceGap = 0.0f;
    static String gapNote = "-";

    static {
        STROKE.setStyle(Paint.Style.STROKE);
        STROKE.setStrokeCap(Paint.Cap.ROUND);
        STROKE.setStrokeJoin(Paint.Join.ROUND);
    }

    private PillRing() {
    }

    private static int alpha(int color, float factor) {
        int a = Math.max(0, Math.min(255, Math.round(255.0f * factor)));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    static boolean draw(Canvas canvas, float left, float top, float size, IconState state, int batteryColor, int weightPercent, int battWeightPercent, float gapDeg, float alphaFactor) {
        if (size <= 0.0f || alphaFactor <= 0.0f || gapDeg <= 0.0f) {
            return false;
        }
        float scale = size / 120.0f;
        float wScale = DisplayPolicy.weight(weightPercent) / 100.0f;
        int dimInk = alpha(batteryColor, 0.28f * alphaFactor);
        int activeInk = alpha(batteryColor, alphaFactor);
        int lvl = Math.max(0, Math.min(100, state.battery));
        float startAngle = gapDeg - 90.0f;
        float fullSweep = 360.0f - (2.0f * gapDeg);
        float activeSweep = (lvl * fullSweep) / 100.0f;

        int save = canvas.save();
        canvas.translate(left, top);
        canvas.scale(scale, scale);
        STROKE.setStrokeWidth(10.5f * wScale);
        STROKE.setColor(dimInk);
        canvas.drawArc(ARC, startAngle, fullSweep, false, STROKE);
        if (activeSweep > 0.0f) {
            STROKE.setColor(activeInk);
            canvas.drawArc(ARC, startAngle, activeSweep, false, STROKE);
        }
        canvas.restoreToCount(save);
        gapNote = Math.round(gapDeg) + "°";
        return true;
    }

    static boolean middleBlank(IconState state) {
        if (state == null || state.airplane) {
            return false;
        }
        if (state.wifiEnabled && (state.wifiConnected || Style.wifiOverSim)) {
            return false;
        }
        String label = TechLabel.apply(state.centreTechnology());
        return label == null || label.isEmpty();
    }
}
