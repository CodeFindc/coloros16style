package com.mess1on.combinedstatus;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import java.util.Locale;

final class Capsule {
    private static final float ASPECT = 1.95f;
    private static final Path BOLT = new Path();
    private static final Paint.FontMetrics FM = new Paint.FontMetrics();
    private static final float H = 104.0f;
    private static final Paint NETWORK = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final RectF OVAL = new RectF();
    private static final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path PLANE = new Path();
    private static final Path RING = new Path();
    private static final Paint TEXT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path WIFI_BASE = new Path();
    private static final Path WIFI_INNER = new Path();
    private static final Path WIFI_OUTER = new Path();

    static float capBoltW = 0.0f;
    static boolean capCharging = false;
    static float capContent = 0.0f;
    static float capDigitSize = 60.32f;
    static String capDigits = "82";
    static float capDigitsW = 0.0f;
    static float capLeft = 0.0f;
    static boolean capPlane = false;
    static String capTech = "5G";
    static float capTechSize = 42.0f;
    static boolean capTime = false;
    static float capWifi = 41.6f;
    static boolean capWifiOn = false;
    static float gap = 0.0f;
    static float geoBody = 202.8f;
    static float geoContent = 120.0f;
    static float geoSqueeze = 1.0f;

    static {
        WIFI_OUTER.addArc(new RectF(-28.0f, -18.0f, 28.0f, 38.0f), -135.0f, 90.0f);
        WIFI_INNER.addArc(new RectF(-19.0f, -7.0f, 19.0f, 31.0f), -135.0f, 90.0f);
        WIFI_BASE.addArc(new RectF(-10.0f, 4.0f, 10.0f, 24.0f), -135.0f, 90.0f);

        BOLT.moveTo(11.0f, 0.0f);
        BOLT.lineTo(2.0f, 12.0f);
        BOLT.lineTo(9.0f, 12.0f);
        BOLT.lineTo(7.0f, 23.0f);
        BOLT.lineTo(16.0f, 10.0f);
        BOLT.lineTo(9.5f, 10.0f);
        BOLT.close();
    }

    private Capsule() {
    }

    private static int alphaFor(int baseAlpha, float factor) {
        return Math.max(0, Math.min(255, Math.round(baseAlpha * factor)));
    }

    static int card1Bars(IconState state) {
        if (state == null || state.airplane) return 0;
        if (!dualSim(state)) return 4;
        return (state.simPresent && state.inService) ? Math.max(0, Math.min(4, state.mobileLevel)) : 0;
    }

    static int card2Bars(IconState state) {
        if (state == null || state.airplane) return 0;
        if (dualSim(state)) {
            return Dots.otherAlive ? Math.max(0, Math.min(4, Dots.otherLevel)) : 0;
        }
        return (state.simPresent && state.inService) ? Math.max(0, Math.min(4, state.mobileLevel)) : 0;
    }

    static boolean dualSim(IconState state) {
        return Dots.twoCards();
    }

    private static float extra() {
        return (Math.max(0.0f, Math.min(100.0f, gap)) / 100.0f) * 40.0f;
    }

    private static Typeface face(int weightPercent) {
        int fw = DisplayPolicy.textWeight(DisplayPolicy.weight(weightPercent));
        return Typeface.create(Typeface.create("sans-serif-condensed", Typeface.BOLD), fw, false);
    }

    private static boolean showWifi(IconState state) {
        return state != null && !state.airplane && state.wifiEnabled && (state.wifiConnected || Style.wifiOverSim);
    }

    private static void parts(IconState state, boolean showPercent) {
        capCharging = state != null && state.charging && Bolt.show;
        capWifiOn = showWifi(state);
        capPlane = state != null && state.airplane;
        capTech = (state == null || capWifiOn || capPlane) ? "" : TechLabel.apply(state.centreTechnology());
        capTime = Style.capsuleTime;

        if (capTime) {
            capDigits = Style.timeLabel(null);
        } else if (showPercent && state != null) {
            capDigits = String.valueOf(Math.max(0, Math.min(100, state.battery)));
        } else {
            capDigits = "";
        }

        float wifiScale = ColorOsPill.cWifiScale / 100.0f;
        float centreScale = ColorOsPill.cCentreScale / 100.0f;
        float battScale = ColorOsPill.cBattScale / 100.0f;

        capWifi = capWifiOn ? (41.6f * wifiScale) : 0.0f;
        NETWORK.setTypeface(face(ColorOsPill.cCentreWeight));
        float mul5ga = "5GA".equals(capTech) ? Style.centre5gaMul() : 1.0f;
        capTechSize = (capTech.length() >= 3 ? 34.0f : 44.0f) * centreScale * mul5ga;

        if (capWifiOn) {
            capLeft = capWifi;
        } else if (capPlane) {
            capLeft = 29.4f * Bolt.planeScale();
        } else if (!capTech.isEmpty()) {
            NETWORK.setTextSize(capTechSize);
            capLeft = NETWORK.measureText(capTech);
        } else {
            capLeft = 20.0f;
        }

        capDigitSize = (capDigits.length() >= 4 ? 46.0f : 58.0f) * battScale;
        TEXT.setTypeface(face(ColorOsPill.cBattWeight));
        TEXT.setTextSize(capDigitSize);
        TEXT.setLetterSpacing(Style.pctTracking());
        capDigitsW = capDigits.isEmpty() ? 0.0f : TEXT.measureText(capDigits);
        capBoltW = capCharging ? (26.0f * (Bolt.scale / 100.0f)) : 0.0f;

        float midGap = (!capDigits.isEmpty() && capLeft > 0.0f) ? (12.0f + extra()) : 0.0f;
        float boltGap = (capCharging && !capDigits.isEmpty()) ? 6.0f : 0.0f;
        capContent = capLeft + midGap + capDigitsW + boltGap + capBoltW;
        geoContent = capContent;
        geoBody = Math.max(202.8f, Math.min(410.0f, capContent + 62.4f));
        geoSqueeze = capContent + 62.4f > 410.0f ? (347.6f / Math.max(1.0f, capContent)) : 1.0f;
    }

    private static void appendStadiumArc(Path out, float left, float top, float right, float bottom, float gapSpan, float frac) {
        out.reset();
        if (frac <= 0.001f) return;
        float r = (bottom - top) * 0.5f;
        float cx = (left + right) * 0.5f;
        float straight = Math.max(0.0f, (right - left) - (2.0f * r));
        float topHalf = Math.max(0.0f, (straight - gapSpan) * 0.5f);
        float botHalf = Math.max(0.0f, (straight - (gapSpan * 1.08f)) * 0.5f);
        float arcLen = (float) (Math.PI * r);

        // Right half (clockwise from top-right of top gap to bottom-right of bottom gap)
        float rightTotal = topHalf + arcLen + botHalf;
        float rightDraw = rightTotal * Math.min(1.0f, frac);
        if (rightDraw > 0.0f) {
            float x0 = cx + (gapSpan * 0.5f);
            out.moveTo(x0, top);
            float seg1 = Math.min(rightDraw, topHalf);
            if (seg1 > 0.0f) {
                out.lineTo(x0 + seg1, top);
            }
            float rem = rightDraw - seg1;
            if (rem > 0.0f) {
                float sweep = Math.min(180.0f, (rem / arcLen) * 180.0f);
                OVAL.set(right - (2.0f * r), top, right, bottom);
                out.arcTo(OVAL, -90.0f, sweep, false);
                rem -= arcLen;
                if (rem > 0.0f && botHalf > 0.0f) {
                    float seg3 = Math.min(rem, botHalf);
                    out.lineTo((right - r) - seg3, bottom);
                }
            }
        }

        // Left half (counter-clockwise from top-left of top gap to bottom-left of bottom gap)
        if (frac > 0.0f) {
            float leftTotal = topHalf + arcLen + botHalf;
            float leftDraw = leftTotal * Math.min(1.0f, frac);
            float x0 = cx - (gapSpan * 0.5f);
            out.moveTo(x0, top);
            float seg1 = Math.min(leftDraw, topHalf);
            if (seg1 > 0.0f) {
                out.lineTo(x0 - seg1, top);
            }
            float rem = leftDraw - seg1;
            if (rem > 0.0f) {
                float sweep = Math.min(180.0f, (rem / arcLen) * 180.0f);
                OVAL.set(left, top, left + (2.0f * r), bottom);
                out.arcTo(OVAL, -90.0f, -sweep, false);
                rem -= arcLen;
                if (rem > 0.0f && botHalf > 0.0f) {
                    float seg3 = Math.min(rem, botHalf);
                    out.lineTo((left + r) + seg3, bottom);
                }
            }
        }
    }

    static boolean draw(Canvas canvas, float left, float top, float height, IconState state, int batteryColor, int normalColor, int weightPercent, boolean showPercent, float alphaFactor) {
        if (height <= 0.0f || alphaFactor <= 0.0f || state == null) {
            return false;
        }
        parts(state, showPercent);
        float scale = height / 120.0f;
        float wScale = DisplayPolicy.weight(weightPercent) / 100.0f;
        float strokeW = 7.2f * wScale;

        int save = canvas.save();
        canvas.translate(left, top);
        canvas.scale(scale, scale);

        float bodyW = geoBody;
        float boxLeft = 6.0f;
        float boxTop = 10.0f;
        float boxRight = boxLeft + bodyW;
        float boxBottom = boxTop + 98.0f;
        float cy = (boxTop + boxBottom) * 0.5f;
        float cx = (boxLeft + boxRight) * 0.5f;

        float dotScale = ColorOsPill.cDotsScale / 100.0f;
        float dotW = (DisplayPolicy.weight(ColorOsPill.cDotsWeight) / 100.0f) * dotScale;
        float rTop = 3.8f * dotW;
        float rBot = 4.5f * dotW;
        float dotStep = 14.8f * dotScale;
        float gapSpan = (3.0f * dotStep) + (2.0f * rBot) + 18.0f + (PillRing.forceGap * 1.4f);

        // 1. Draw stadium battery border with top & bottom signal dot cutouts
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeCap(Paint.Cap.ROUND);
        P.setStrokeJoin(Paint.Join.ROUND);
        P.setStrokeWidth(strokeW);

        appendStadiumArc(RING, boxLeft, boxTop, boxRight, boxBottom, gapSpan, 1.0f);
        P.setColor(batteryColor);
        P.setAlpha(alphaFor(Color.alpha(batteryColor), 0.28f * alphaFactor));
        canvas.drawPath(RING, P);

        int lvl = Math.max(0, Math.min(100, state.battery));
        if (lvl > 0) {
            appendStadiumArc(RING, boxLeft, boxTop, boxRight, boxBottom, gapSpan, lvl / 100.0f);
            P.setColor(batteryColor);
            P.setAlpha(alphaFor(Color.alpha(batteryColor), alphaFactor));
            canvas.drawPath(RING, P);
        }

        // 2. Draw top 4 signal dots (Card 1) and bottom 4 signal dots (Card 2) inside the cutouts
        P.setStyle(Paint.Style.FILL);
        int b1 = card1Bars(state);
        int b2 = card2Bars(state);
        float dotsStartX = (cx - (1.5f * dotStep)) + Style.dotsShiftX(scale);
        float dotsDy = Style.dotsDy(scale);

        for (int i = 0; i < 4; i++) {
            float dx = dotsStartX + (i * dotStep);
            boolean lit1 = i < b1;
            int col1 = StatusColors.dotRainbow ? StatusColors.dotColour(i) : normalColor;
            P.setColor(col1);
            P.setAlpha(alphaFor(Color.alpha(col1), (lit1 ? 1.0f : 0.26f) * alphaFactor));
            canvas.drawCircle(dx, boxTop + dotsDy, rTop, P);

            boolean lit2 = i < b2;
            int col2 = StatusColors.dotRainbow ? StatusColors.dotColour(4 + i) : normalColor;
            P.setColor(col2);
            P.setAlpha(alphaFor(Color.alpha(col2), (lit2 ? 1.0f : 0.26f) * alphaFactor));
            canvas.drawCircle(dx, boxBottom + dotsDy, rBot, P);
        }

        // 3. Draw horizontal center row: [Wi-Fi / 5G / Plane] + [Battery % / Time] + [Bolt]
        int cSave = canvas.save();
        if (geoSqueeze < 0.999f) {
            canvas.scale(geoSqueeze, 1.0f, cx, cy);
        }
        float startX = cx - (capContent * 0.5f);
        float curX = startX;
        boolean swap = ColorOsPill.cSwapMid;

        float midGap = (!capDigits.isEmpty() && capLeft > 0.0f) ? (12.0f + extra()) : 0.0f;
        int digitInk = StatusColors.digitInk(state, ColorOsPill.cColour, normalColor, ColorOsPill.cThreshold, ColorOsPill.cPowerSave, ColorOsPill.cPerfMode);

        if (!swap) {
            drawLeftMark(canvas, curX + (capLeft * 0.5f), cy, state, normalColor, wScale, scale, alphaFactor);
            curX += capLeft + midGap;
            drawDigitsPart(canvas, curX, cy, digitInk, scale, alphaFactor);
            curX += capDigitsW;
        } else {
            drawDigitsPart(canvas, curX, cy, digitInk, scale, alphaFactor);
            curX += capDigitsW + midGap;
            drawLeftMark(canvas, curX + (capLeft * 0.5f), cy, state, normalColor, wScale, scale, alphaFactor);
            curX += capLeft;
        }

        if (capCharging) {
            curX += 6.0f;
            int bSave = canvas.save();
            float bs = (Bolt.scale / 100.0f) * 1.45f;
            canvas.translate(curX + (capBoltW * 0.5f) - (9.0f * bs), cy - (11.5f * bs));
            canvas.scale(bs, bs);
            P.setStyle(Paint.Style.FILL);
            P.setColor(batteryColor);
            P.setAlpha(alphaFor(Color.alpha(batteryColor), alphaFactor));
            canvas.drawPath(BOLT, P);
            canvas.restoreToCount(bSave);
        }

        canvas.restoreToCount(cSave);
        canvas.restoreToCount(save);
        return true;
    }

    private static void drawLeftMark(Canvas canvas, float markCx, float markCy, IconState state, int normalColor, float wScale, float scale, float alphaFactor) {
        int save = canvas.save();
        canvas.translate(markCx + Style.shiftX(capWifiOn, scale), markCy + Style.shiftY(capWifiOn, scale));
        if (capPlane) {
            float ps = Bolt.planeScale() * 0.65f;
            canvas.scale(ps, ps);
            P.setStyle(Paint.Style.STROKE);
            P.setStrokeCap(Paint.Cap.ROUND);
            P.setStrokeWidth(7.5f * wScale);
            P.setColor(normalColor);
            P.setAlpha(alphaFor(Color.alpha(normalColor), alphaFactor));
            canvas.drawLine(-18.0f, 0.0f, 18.0f, 0.0f, P);
            canvas.drawLine(-2.0f, -15.0f, 9.0f, 0.0f, P);
            canvas.drawLine(-2.0f, 15.0f, 9.0f, 0.0f, P);
        } else if (capWifiOn) {
            float ws = (ColorOsPill.cWifiScale / 100.0f) * 0.72f;
            float ww = (ColorOsPill.cWifiWeight / 100.0f) * wScale;
            canvas.scale(ws, ws);
            canvas.translate(0.0f, -6.0f);
            int segs = state.wifiConnected ? IconState.wifiSegments(state.wifiLevel) : 0;
            P.setStyle(Paint.Style.STROKE);
            P.setStrokeCap(Paint.Cap.ROUND);
            P.setStrokeWidth(7.2f * ww);
            P.setColor(normalColor);
            P.setAlpha(alphaFor(Color.alpha(normalColor), (segs >= 3 ? 1.0f : 0.28f) * alphaFactor));
            canvas.drawPath(WIFI_OUTER, P);
            P.setAlpha(alphaFor(Color.alpha(normalColor), (segs >= 2 ? 1.0f : 0.28f) * alphaFactor));
            canvas.drawPath(WIFI_INNER, P);
            P.setAlpha(alphaFor(Color.alpha(normalColor), (segs >= 1 ? 1.0f : 0.28f) * alphaFactor));
            canvas.drawPath(WIFI_BASE, P);
            P.setStyle(Paint.Style.FILL);
            canvas.drawCircle(0.0f, 14.5f, 4.2f * ww, P);
        } else if (!capTech.isEmpty()) {
            int ink = TechLabel.ink(normalColor);
            NETWORK.setColor(ink);
            NETWORK.setAlpha(alphaFor(Color.alpha(ink), alphaFactor));
            NETWORK.setTextAlign(Paint.Align.CENTER);
            NETWORK.setTextSize(capTechSize);
            NETWORK.getFontMetrics(FM);
            canvas.drawText(capTech, 0.0f, -((FM.ascent + FM.descent) * 0.5f), NETWORK);
        }
        canvas.restoreToCount(save);
    }

    private static void drawDigitsPart(Canvas canvas, float leftX, float cy, int digitInk, float scale, float alphaFactor) {
        if (capDigits.isEmpty()) return;
        TEXT.setColor(digitInk);
        TEXT.setAlpha(alphaFor(Color.alpha(digitInk), alphaFactor));
        TEXT.setTextAlign(Paint.Align.CENTER);
        TEXT.setTextSize(capDigitSize);
        TEXT.getFontMetrics(FM);
        float cx = leftX + (capDigitsW * 0.5f) + Style.pctShiftX(scale);
        float baselineY = (cy + Style.pctShiftY(scale)) - ((FM.ascent + FM.descent) * 0.5f);
        canvas.drawText(capDigits, cx, baselineY, TEXT);
    }

    static String geometry() {
        return "内容=" + Math.round(geoContent) + " 横向比例=" + String.format(Locale.ROOT, "%.3f", geoSqueeze) + " 框宽=" + Math.round(geoBody) + "（上限 410）";
    }

    static int reserveUnits() {
        return 4;
    }

    static int reserveWidth(int heightPx) {
        return Math.max(1, Math.round(heightPx * ASPECT));
    }
}
