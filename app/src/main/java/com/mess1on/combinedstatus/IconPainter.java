package com.mess1on.combinedstatus;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Typeface;

final class IconPainter {
    private static final String[] NUMBERS = new String[101];
    static final float[] XS = new float[]{34.0f, 51.0f, 69.0f, 86.0f};
    static final float[] YS = new float[]{102.0f, 109.0f, 109.0f, 102.0f};

    static {
        for (int i = 0; i <= 100; i++) {
            NUMBERS[i] = String.valueOf(i);
        }
    }

    private final Path base = new Path();
    private final Paint batteryFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint batteryStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint batteryText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path bolt = new Path();
    private final Paint dotFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path inner = new Path();
    private int lowBatteryThreshold = 15;
    private final Paint maskClear = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint maskLayer = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint networkFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint networkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint networkStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path outer = new Path();
    private final RectF ring = new RectF(10.0f, 8.0f, 110.0f, 108.0f);
    private boolean showPercent = true;
    private int weightPercent = 100;

    IconPainter() {
        this.batteryStroke.setStyle(Paint.Style.STROKE);
        this.batteryStroke.setStrokeCap(Paint.Cap.ROUND);
        this.batteryStroke.setStrokeJoin(Paint.Join.ROUND);

        this.networkStroke.setStyle(Paint.Style.STROKE);
        this.networkStroke.setStrokeCap(Paint.Cap.ROUND);
        this.networkStroke.setStrokeJoin(Paint.Join.ROUND);

        this.batteryFill.setStyle(Paint.Style.FILL);
        this.networkFill.setStyle(Paint.Style.FILL);
        this.dotFill.setStyle(Paint.Style.FILL);

        this.batteryText.setTextAlign(Paint.Align.CENTER);
        this.batteryText.setTextSize(BatteryHeader.textSize(100));
        this.batteryText.setFakeBoldText(true);
        this.batteryText.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));

        this.networkPaint.setTextAlign(Paint.Align.CENTER);
        this.networkPaint.setTextSize(42.0f);
        this.networkPaint.setFakeBoldText(true);
        this.networkPaint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));

        this.maskClear.setStyle(Paint.Style.STROKE);
        this.maskClear.setStrokeCap(Paint.Cap.ROUND);
        this.maskClear.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        this.outer.addArc(new RectF(28.0f, 41.0f, 92.0f, 105.0f), -135.0f, 90.0f);
        this.inner.addArc(new RectF(39.0f, 54.0f, 81.0f, 96.0f), -135.0f, 90.0f);
        this.base.addArc(new RectF(50.0f, 67.0f, 70.0f, 87.0f), -135.0f, 90.0f);

        this.bolt.moveTo(64.0f, 31.0f);
        this.bolt.lineTo(46.0f, 60.0f);
        this.bolt.lineTo(59.0f, 60.0f);
        this.bolt.lineTo(54.0f, 85.0f);
        this.bolt.lineTo(74.0f, 54.0f);
        this.bolt.lineTo(61.0f, 54.0f);
        this.bolt.close();
        setWeightPercent(100);
    }

    private static int alpha(int color, float factor) {
        int a = Math.max(0, Math.min(255, Math.round(255.0f * factor)));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    private void battery(Canvas canvas, IconState state, int batteryColor, float alphaFactor, boolean trackOnly) {
        float strokeScale = this.weightPercent / 100.0f;
        int dimTrack = alpha(batteryColor, 0.28f * alphaFactor);
        int activeInk = alpha(batteryColor, alphaFactor);
        int lvl = Math.max(0, Math.min(100, state.battery));
        String txt = NUMBERS[lvl];
        float strokeW = 10.5f * strokeScale;
        if (!this.showPercent) {
            this.batteryStroke.setColor(dimTrack);
            canvas.drawArc(this.ring, -90.0f, 360.0f, false, this.batteryStroke);
            if (!trackOnly && lvl > 0) {
                this.batteryStroke.setColor(activeInk);
                canvas.drawArc(this.ring, -90.0f, (lvl * 360.0f) / 100.0f, false, this.batteryStroke);
            }
            return;
        }
        float halfGap = BatteryHeader.halfGap(this.batteryText.measureText(txt), strokeW);
        float startAngle = halfGap - 90.0f;
        float fullSweep = 360.0f - (2.0f * halfGap);
        float activeSweep = (lvl * fullSweep) / 100.0f;
        this.batteryStroke.setColor(dimTrack);
        canvas.drawArc(this.ring, startAngle, fullSweep, false, this.batteryStroke);
        if (!trackOnly) {
            if (activeSweep > 0.0f) {
                this.batteryStroke.setColor(activeInk);
                canvas.drawArc(this.ring, startAngle, activeSweep, false, this.batteryStroke);
            }
            this.batteryText.setColor(activeInk);
            Paint.FontMetrics fm = this.batteryText.getFontMetrics();
            canvas.drawText(txt, 60.0f, BatteryHeader.centreY() - ((fm.ascent + fm.descent) * 0.5f), this.batteryText);
        }
    }

    private void drawMask(Canvas canvas, Path path, int strokeColor, int clearColor, float width) {
        int save = canvas.saveLayer(0.0f, 0.0f, 120.0f, 120.0f, this.maskLayer);
        this.networkStroke.setStrokeWidth(width);
        this.networkStroke.setColor(strokeColor);
        canvas.drawPath(path, this.networkStroke);
        this.maskClear.setStrokeWidth(width + 5.0f);
        canvas.drawPath(path, this.maskClear);
        this.networkStroke.setStrokeWidth(width);
        this.networkStroke.setColor(clearColor);
        canvas.drawPath(path, this.networkStroke);
        canvas.restoreToCount(save);
    }

    private void fill(Canvas canvas, String text, int color) {
        this.networkPaint.setStyle(Paint.Style.FILL);
        this.networkPaint.setStrokeWidth(0.0f);
        this.networkPaint.setColor(color);
        Paint.FontMetrics fm = this.networkPaint.getFontMetrics();
        canvas.drawText(text, 60.0f, 60.0f - ((fm.ascent + fm.descent) * 0.5f), this.networkPaint);
    }

    private void stroke(Canvas canvas, String text, int color, float strokeWidth) {
        this.networkPaint.setStyle(Paint.Style.STROKE);
        this.networkPaint.setStrokeJoin(Paint.Join.ROUND);
        this.networkPaint.setStrokeWidth(strokeWidth);
        this.networkPaint.setColor(color);
        Paint.FontMetrics fm = this.networkPaint.getFontMetrics();
        canvas.drawText(text, 60.0f, 60.0f - ((fm.ascent + fm.descent) * 0.5f), this.networkPaint);
    }

    private void symbol(Canvas canvas, IconState state, int activeInk, int dimInk) {
        float wScale = this.weightPercent / 100.0f;
        float w = 8.4f * wScale;
        if (state.airplane) {
            this.networkStroke.setColor(activeInk);
            this.networkStroke.setStrokeWidth(w);
            canvas.drawLine(38.0f, 60.0f, 82.0f, 60.0f, this.networkStroke);
            canvas.drawLine(58.0f, 40.0f, 70.0f, 60.0f, this.networkStroke);
            canvas.drawLine(58.0f, 80.0f, 70.0f, 60.0f, this.networkStroke);
            return;
        }
        if (state.wifiEnabled && (state.wifiConnected || Style.wifiOverSim)) {
            int segs = state.wifiConnected ? IconState.wifiSegments(state.wifiLevel) : 0;
            this.networkStroke.setStrokeWidth(w);
            this.networkStroke.setColor(segs >= 3 ? activeInk : dimInk);
            canvas.drawPath(this.outer, this.networkStroke);
            this.networkStroke.setColor(segs >= 2 ? activeInk : dimInk);
            canvas.drawPath(this.inner, this.networkStroke);
            this.networkStroke.setColor(segs >= 1 ? activeInk : dimInk);
            canvas.drawPath(this.base, this.networkStroke);
            this.networkFill.setColor(segs >= 1 ? activeInk : dimInk);
            canvas.drawCircle(60.0f, 78.5f, 4.8f * wScale, this.networkFill);
            return;
        }
        String label = TechLabel.apply(state.centreTechnology());
        if (label == null || label.isEmpty()) {
            this.networkStroke.setColor(dimInk);
            this.networkStroke.setStrokeWidth(w);
            canvas.drawLine(44.0f, 60.0f, 76.0f, 60.0f, this.networkStroke);
            return;
        }
        int ink = TechLabel.ink(activeInk);
        float mul = "5GA".equals(label) ? Style.centre5gaMul() : 1.0f;
        float origSize = this.networkPaint.getTextSize();
        this.networkPaint.setTextSize((label.length() >= 3 ? 31.0f : 42.0f) * mul);
        fill(canvas, label, ink);
        this.networkPaint.setTextSize(origSize);
    }

    void draw(Canvas canvas, float left, float top, float size, IconState state, int batteryColor, int normalColor, float progress) {
        float p = Motion.clamp(progress);
        drawPart(canvas, 0, left, top, size, state, batteryColor, normalColor, 1.0f - (0.35f * p), false);
        drawPart(canvas, 1, left, top, size, state, batteryColor, normalColor, 1.0f - p, false);
        drawPart(canvas, 2, left, top, size, state, batteryColor, normalColor, 1.0f - p, false);
    }

    void drawPart(Canvas canvas, int part, float left, float top, float size, IconState state, int batteryColor, int normalColor, float alphaFactor, boolean trackOnly) {
        if (size <= 0.0f || alphaFactor <= 0.0f) {
            return;
        }
        if (Style.capsuleAt(null)) {
            if (part == 0) {
                Capsule.draw(canvas, left, top, size, state, batteryColor, normalColor, this.weightPercent, this.showPercent, alphaFactor);
            }
            return;
        }
        int save = canvas.save();
        float scale = size / 120.0f;
        canvas.translate(left, top);
        canvas.scale(scale, scale);

        int activeInk = alpha(normalColor, alphaFactor);
        int dimInk = alpha(normalColor, 0.28f * alphaFactor);
        float wScale = this.weightPercent / 100.0f;

        if (part == 0) {
            battery(canvas, state, batteryColor, alphaFactor, trackOnly);
        } else if (part == 1) {
            if (!Style.hideMiddle(state)) {
                int s2 = canvas.save();
                boolean isWifi = !state.airplane && state.wifiEnabled && (state.wifiConnected || Style.wifiOverSim);
                float dx = Style.shiftX(isWifi, scale);
                float dy = Style.shiftY(isWifi, scale);
                if (dx != 0.0f || dy != 0.0f) {
                    canvas.translate(dx, dy);
                }
                symbol(canvas, state, activeInk, dimInk);
                canvas.restoreToCount(s2);
            }
        } else if (part == 2) {
            Dots.paint(canvas, this.dotFill, state, activeInk, dimInk, 4.4f * wScale, alphaFactor, scale);
        }
        canvas.restoreToCount(save);
    }

    void setLowBatteryThreshold(int threshold) {
        this.lowBatteryThreshold = DisplayPolicy.lowBatteryThreshold(threshold);
    }

    void setShowPercent(boolean show) {
        this.showPercent = show;
    }

    void setWeightPercent(int weight) {
        int w = DisplayPolicy.weight(weight);
        if (this.weightPercent == w && this.batteryStroke.getStrokeWidth() > 0.0f) {
            return;
        }
        this.weightPercent = w;
        float s = w / 100.0f;
        this.batteryStroke.setStrokeWidth(10.5f * s);
        this.networkStroke.setStrokeWidth(8.4f * s);
        Typeface tf = Typeface.create("sans-serif-condensed", Typeface.BOLD);
        int fontWeight = DisplayPolicy.textWeight(w);
        Typeface weighted = Typeface.create(tf, fontWeight, false);
        this.batteryText.setTypeface(weighted);
        this.networkPaint.setTypeface(weighted);
    }
}
