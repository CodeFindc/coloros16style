package com.mess1on.combinedstatus;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

public final class PillPreviewView extends View {
    public static final String[] SCENES = new String[]{
        "正常", "充电中", "低电量", "省电", "飞行", "WiFi关", "WiFi未连", "性能模式"
    };

    private final IconPainter painter = new IconPainter();
    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pctPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF cardRect = new RectF();
    private int scenario = 0;
    private boolean thumbMode = false;
    private boolean thumbCapsule = false;

    public PillPreviewView(Context context) {
        super(context);
        this.labelPaint.setTextAlign(Paint.Align.CENTER);
        this.labelPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        this.pctPaint.setTextAlign(Paint.Align.CENTER);
        this.pctPaint.setStyle(Paint.Style.FILL);
    }

    public static PillPreviewView createThumb(Context context, boolean capsule) {
        PillPreviewView v = new PillPreviewView(context);
        v.thumbMode = true;
        v.thumbCapsule = capsule;
        return v;
    }

    public void setScenario(int index) {
        this.scenario = Math.max(0, Math.min(SCENES.length - 1, index));
        invalidate();
    }

    public int getScenario() {
        return this.scenario;
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private int styleInt(SharedPreferences sp, String key, int def) {
        try {
            return sp.getInt(key, def);
        } catch (ClassCastException ignored) {
            try {
                return sp.getBoolean(key, def != 0) ? 1 : 0;
            } catch (Throwable ignored2) {
                return def;
            }
        }
    }

    private boolean styleBool(SharedPreferences sp, String key, boolean def) {
        try {
            return sp.getBoolean(key, def);
        } catch (ClassCastException ignored) {
            try {
                return sp.getInt(key, def ? 1 : 0) != 0;
            } catch (Throwable ignored2) {
                return def;
            }
        }
    }

    public IconState sampleState() {
        IconState st = new IconState();
        st.battery = 82;
        st.charging = false;
        st.wifiEnabled = true;
        st.wifiConnected = true;
        st.wifiLevel = 3;
        st.simPresent = true;
        st.inService = true;
        st.mobileLevel = 4;
        st.mobileTechnology = "5G";
        if (this.thumbMode) {
            return st;
        }
        switch (this.scenario) {
            case 1:
                st.charging = true;
                break;
            case 2:
                st.battery = 12;
                break;
            case 3:
                st.battery = 45;
                break;
            case 4:
                st.airplane = true;
                st.wifiEnabled = false;
                st.wifiConnected = false;
                st.inService = false;
                break;
            case 5:
                st.wifiEnabled = false;
                st.wifiConnected = false;
                break;
            case 6:
                st.wifiEnabled = true;
                st.wifiConnected = false;
                break;
            case 7:
                st.battery = 88;
                break;
        }
        return st;
    }

    public void syncRuntimeGlobals(SharedPreferences sp) {
        if (sp == null) return;
        ColorOsPill.cSizePx = styleInt(sp, "size", 24);
        ColorOsPill.cScale = styleInt(sp, "global_scale", 100);
        ColorOsPill.cWeight = styleInt(sp, "weight", 100);
        ColorOsPill.cThreshold = styleInt(sp, "threshold", 20);
        ColorOsPill.cShowPct = styleBool(sp, "show_percent", true);
        ColorOsPill.cColour = styleBool(sp, "colour", true);
        ColorOsPill.cBattScale = styleInt(sp, "batt_scale", 100);
        ColorOsPill.cBattWeight = styleInt(sp, "batt_weight", 100);
        ColorOsPill.cCentreScale = styleInt(sp, "centre_scale", 100);
        ColorOsPill.cCentreWeight = styleInt(sp, "centre_weight", 100);
        ColorOsPill.cWifiScale = styleInt(sp, "wifi_scale", 100);
        ColorOsPill.cWifiWeight = styleInt(sp, "wifi_weight", 100);
        ColorOsPill.cDotsScale = styleInt(sp, "dots_scale", 100);
        ColorOsPill.cDotsWeight = styleInt(sp, "dots_weight", 100);
        ColorOsPill.cSwapMid = styleBool(sp, "swap_mid", false);
        ColorOsPill.cPctDx = styleInt(sp, "pct_dx", 0);
        ColorOsPill.cPctDy = styleInt(sp, "pct_dy", 0);

        Style.capsuleWanted = styleInt(sp, "capsule_dual", 0) != 0;
        Style.capsuleTime = styleInt(sp, "capsule_time", 0) != 0;
        Slots.clockHide = styleInt(sp, "hide_clock", 0) != 0;
        Capsule.gap = styleInt(sp, "capsule_gap", 0);
        PillRing.forceGap = styleInt(sp, "gap_extra", 0);
        Style.wifiOverSim = styleBool(sp, "wifi_over_sim", false);
        TechLabel.show5ga = styleInt(sp, "show_5ga", 0) != 0;
        TechLabel.fiveGa = TechLabel.show5ga;
        Style.centre5gaScale = styleInt(sp, "centre_5ga_scale", 0);
        TechLabel.greyWhenNoData = styleInt(sp, "tech_grey", 1) != 0;

        Dots.auto = styleInt(sp, "dots_auto", 1) != 0;
        Dots.manualDual = styleInt(sp, "dots_dual", 0) != 0;
        Dots.line = styleInt(sp, "dots_line", 0) != 0;
        Dots.dy = styleInt(sp, "dots_dy", 0);
        Dots.applyMode();
        Dots.applyLayout();

        Bolt.show = styleInt(sp, "bolt_show", 1) != 0;
        Bolt.center = styleInt(sp, "bolt_center", 0) != 0;
        Bolt.scale = styleInt(sp, "bolt_scale", 155);
        Bolt.dx = styleInt(sp, "bolt_dx", 95);
        Bolt.dy = styleInt(sp, "bolt_dy", 88);
        Bolt.doubleMode = styleInt(sp, "bolt_double", 0);
        Bolt.planeScale = styleInt(sp, "plane_scale", 100);

        StatusColors.CHARGING = StatusColors.resolve(styleInt(sp, "color_charging", 0), 0xFF1CA053);
        StatusColors.FAST = StatusColors.opaque(styleInt(sp, "color_fast", 0));
        StatusColors.LOW = StatusColors.resolve(styleInt(sp, "color_low", 0), 0xFFE73C3A);
        StatusColors.SAVER = StatusColors.resolve(styleInt(sp, "power_color", 0), 0xFFFFB300);
        StatusColors.PERFORMANCE = StatusColors.resolve(styleInt(sp, "color_performance", 0), 0xFF2F6BED);
        StatusColors.normal = StatusColors.opaque(styleInt(sp, "color_normal", 0));
        StatusColors.normalLight = StatusColors.opaque(styleInt(sp, "color_normal_light", 0));
        StatusColors.chargeDigits = styleInt(sp, "charge_digit", 0) != 0;
        StatusColors.dotRainbow = styleInt(sp, "dots_rainbow", 0) != 0;
        StatusColors.setDotColour(0, styleInt(sp, "dot_c1", 0), 0xFF0A84FF);
        StatusColors.setDotColour(1, styleInt(sp, "dot_c2", 0), 0xFF5E5CE6);
        StatusColors.setDotColour(2, styleInt(sp, "dot_c3", 0), 0xFFBF5AF2);
        StatusColors.setDotColour(3, styleInt(sp, "dot_c4", 0), 0xFFFF375F);
        StatusColors.setDotColour(4, styleInt(sp, "dot_c5", 0), 0xFF64D2FF);
        StatusColors.setDotColour(5, styleInt(sp, "dot_c6", 0), 0xFF30D158);
        StatusColors.setDotColour(6, styleInt(sp, "dot_c7", 0), 0xFFFFD60A);
        StatusColors.setDotColour(7, styleInt(sp, "dot_c8", 0), 0xFFFF9F0A);
        StatusColors.followIconAlpha = styleInt(sp, "alpha_follow", 1) != 0;
        StatusColors.alpha = Math.max(10, Math.min(100, styleInt(sp, "color_alpha", 10000) > 100 ? (styleInt(sp, "color_alpha", 10000) / 100) : styleInt(sp, "color_alpha", 100)));
        StatusColors.iconAuto = styleInt(sp, "icon_auto", 1) != 0;
        StatusColors.iconTrim = Math.max(40, Math.min(100, styleInt(sp, "icon_trim", 10000) > 100 ? (styleInt(sp, "icon_trim", 10000) / 100) : styleInt(sp, "icon_trim", 100)));
        Style.pctTrack = styleInt(sp, "pct_track", 0);
        Style.wifiDx = styleInt(sp, "wifi_dx", 0);
        Style.wifiDy = styleInt(sp, "wifi_dy", 0);
        Style.techDx = styleInt(sp, "tech_dx", 0);
        Style.techDy = styleInt(sp, "tech_dy", 0);
    }

    private void drawOne(Canvas canvas, float cx, float cy, float size, IconState st, int sysTint, boolean useCapsule, int weight, int thresh, boolean showPct, boolean colEn) {
        boolean pSave = !this.thumbMode && this.scenario == 3;
        boolean pPerf = !this.thumbMode && this.scenario == 7;
        int battInk = StatusColors.tint(st.battery, st.charging, colEn, sysTint, thresh, pSave, pPerf);
        int normInk = StatusColors.normalFor(sysTint);

        boolean prevCap = Style.capsuleWanted;
        Style.capsuleWanted = useCapsule;
        try {
            if (useCapsule) {
                float w = Capsule.reserveWidth(Math.round(size));
                Capsule.draw(canvas, cx - (w * 0.5f), cy - (size * 0.5f), size, st, battInk, normInk, weight, showPct, 1.0f);
                return;
            }
            float left = cx - (size * 0.5f);
            float top = cy - (size * 0.5f);
            float scale = size / 120.0f;

            boolean showTopGap = Style.showPctNow(showPct) && !ColorOsPill.cSwapMid;
            boolean showMidAtTop = ColorOsPill.cSwapMid && !PillRing.middleBlank(st);
            if (showTopGap || showMidAtTop || PillRing.forceGap > 0.0f) {
                float gapDeg = 28.0f + PillRing.forceGap;
                if (showTopGap) {
                    gapDeg = BatteryHeader.halfGap(36.0f * (ColorOsPill.cBattScale / 100.0f), (weight * 10.5f) / 100.0f) + PillRing.forceGap;
                } else if (showMidAtTop) {
                    gapDeg = TechLabel.neededGap(ColorOsPill.cCentreScale, ColorOsPill.cCentreWeight) + PillRing.forceGap;
                }
                PillRing.draw(canvas, left, top, size, st, battInk, weight, ColorOsPill.cBattWeight, gapDeg, 1.0f);
            } else {
                this.painter.setWeightPercent(weight);
                this.painter.setLowBatteryThreshold(thresh);
                this.painter.setShowPercent(false);
                this.painter.drawPart(canvas, 0, left, top, size, st, battInk, normInk, 1.0f, false);
            }

            if (Style.showPctNow(showPct)) {
                int fw = Math.max(100, Math.min(900, Math.round(ColorOsPill.cBattWeight * 6.5f)));
                this.pctPaint.setTypeface(Typeface.create(Typeface.create("sans-serif-condensed", Typeface.BOLD), fw, false));
                String txt = Style.capsuleTime ? Style.timeLabel(getContext()) : String.valueOf(st.battery);
                float ts = BatteryHeader.textSize(ColorOsPill.cBattScale) * scale * (txt.length() >= 4 ? 0.78f : 1.0f);
                this.pctPaint.setTextSize(ts);
                this.pctPaint.setLetterSpacing(Style.pctTracking());
                int digitInk = StatusColors.digitInk(st, colEn, sysTint, thresh, pSave, pPerf);
                this.pctPaint.setColor(digitInk);
                float designY = ColorOsPill.cSwapMid ? 58.0f : BatteryHeader.centreY();
                Paint.FontMetrics fm = this.pctPaint.getFontMetrics();
                canvas.drawText(
                    txt,
                    left + (60.0f * scale) + ColorOsPill.cPctDx,
                    top + (designY * scale) + ColorOsPill.cPctDy - ((fm.ascent + fm.descent) * 0.5f),
                    this.pctPaint
                );
            }

            if (!Style.hideMiddle(st)) {
                int sMid = canvas.save();
                float midY = ColorOsPill.cSwapMid ? (top - (48.0f * scale)) : top;
                boolean isWifi = !st.airplane && st.wifiEnabled && (st.wifiConnected || Style.wifiOverSim);
                float midScale = (isWifi ? ColorOsPill.cWifiScale : ColorOsPill.cCentreScale) / 100.0f;
                int midWeight = isWifi ? ColorOsPill.cWifiWeight : ColorOsPill.cCentreWeight;
                if (st.airplane) {
                    midScale = Bolt.planeScale();
                }
                canvas.scale(midScale, midScale, left + (size * 0.5f), midY + (size * 0.48333332f));
                this.painter.setWeightPercent(midWeight);
                if (!TechLabel.draw(canvas, left, midY, size, st, normInk, midWeight, 1.0f)) {
                    this.painter.drawPart(canvas, 1, left, midY, size, st, battInk, normInk, 1.0f, false);
                }
                canvas.restoreToCount(sMid);
            } else {
                Style.applyCentreBolt(canvas, left, top, size, st.charging, battInk);
            }

            this.painter.setWeightPercent(ColorOsPill.cDotsWeight);
            this.painter.drawPart(canvas, 2, left, top, size, st, battInk, normInk, 1.0f, false);

            if (Bolt.visible(st.charging)) {
                Bolt.paint(canvas, left, top, size, battInk);
            }
        } finally {
            Style.capsuleWanted = prevCap;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        SharedPreferences sp = StyleProvider.prefs(getContext());
        syncRuntimeGlobals(sp);
        IconState st = sampleState();

        if (this.thumbMode) {
            float sz = Math.min(w, h) * 0.68f;
            drawOne(canvas, w * 0.5f, h * 0.5f, sz, st, 0xFFF5F7FA, this.thumbCapsule, 100, 20, true, true);
            return;
        }

        boolean darkBg = styleBool(sp, "preview_dark", true);
        this.cardPaint.setColor(darkBg ? 0xFF131824 : 0xFFEAEFF8);
        this.cardRect.set(0, 0, w, h);
        float r = dp(18.0f);
        canvas.drawRoundRect(this.cardRect, r, r, this.cardPaint);

        int sysTint = darkBg ? 0xFFF8FAFC : 0xE6141923;
        boolean useCap = Style.capsuleWanted;
        int weight = ColorOsPill.cWeight;
        int thresh = ColorOsPill.cThreshold;
        boolean showPct = ColorOsPill.cShowPct;
        boolean colEn = ColorOsPill.cColour;

        float bigSize = Math.min(w * 0.26f, h * 0.62f);
        drawOne(canvas, w * 0.32f, h * 0.44f, bigSize, st, sysTint, useCap, weight, thresh, showPct, colEn);

        float realPx = dp(ColorOsPill.cSizePx) * (ColorOsPill.cScale / 100.0f);
        drawOne(canvas, w * 0.74f, h * 0.44f, realPx, st, sysTint, useCap, weight, thresh, showPct, colEn);

        this.labelPaint.setColor(darkBg ? 0x9994A3B8 : 0x99475569);
        this.labelPaint.setTextSize(dp(11.0f));
        canvas.drawText("高清放大预览（点击切换深浅底色）", w * 0.32f, h - dp(10.0f), this.labelPaint);
        canvas.drawText("状态栏 1:1 实际尺寸 (" + ColorOsPill.cSizePx + "dp)", w * 0.74f, h - dp(10.0f), this.labelPaint);
    }
}
