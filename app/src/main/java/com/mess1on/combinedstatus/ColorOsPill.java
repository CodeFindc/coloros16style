package com.mess1on.combinedstatus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.CellSignalStrength;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class ColorOsPill extends XposedModule {
    public static final String ACTION_STYLE = "com.mess1on.combinedstatus.STYLE_CHANGED";
    private static final int DEFAULT_TINT = -1;
    private static final int PILL_DP = 24;
    private static final Uri STYLE_URI = Uri.parse("content://com.mess1on.combinedstatus.style/style");
    private static final String TAG = "CombinedStatus";

    public static volatile ColorOsPill INSTANCE;
    static int cBattScale = 100;
    static int cBattWeight = 100;
    static boolean cBurnIn = true;
    static int cBurnPx = 1;
    static int cBurnSec = 60;
    static int cBurnTest = 0;
    static int cCentreScale = 100;
    static int cCentreWeight = 100;
    static int cColorCharging = 0;
    static int cColorLow = 0;
    static int cColorPerf = 0;
    static boolean cColour = true;
    static int cDotsScale = 100;
    static int cDotsWeight = 100;
    static boolean cExitAnim = true;
    static int cIconGap = 0;
    static int cLook = 0;
    static int cPctDx = 0;
    static int cPctDy = 0;
    static boolean cPerfMode = false;
    static int cPowerColor = 0;
    static boolean cPowerSave = false;
    static int cScale = 100;
    static boolean cShowPct = true;
    static int cSizePx = 0;
    static boolean cSwapMid = false;
    static int cThreshold = 15;
    static int cWeight = 100;
    static int cWifiScale = 100;
    static int cWifiWeight = 100;

    private static final PillDrift drift = new PillDrift();
    private static int drawCalls = 0;
    private static long drawMaxNs = 0L;
    private static long drawSumNs = 0L;
    private static int drop12 = 0;
    private static int drop20 = 0;
    private static int frameCount = 0;
    private static long frameLastNs = 0L;
    private static long frameMinNs = Long.MAX_VALUE;
    private static long frameSumNs = 0L;
    private static String fracHired = "";
    private static float fracLeft = 0.0f;
    private static float fracRight = 0.0f;
    private static String fracSource = "";
    private static final Map<ViewGroup, View> pills = Collections.synchronizedMap(new WeakHashMap<ViewGroup, View>());
    private static float shadeT = 0.0f;
    private static final Map<ViewGroup, Integer> treeTints = Collections.synchronizedMap(new WeakHashMap<ViewGroup, Integer>());
    private static int tintColor = -1;

    private Context appCtx;
    private volatile boolean blown = false;
    private final Runnable burnTicker;
    private String containerSummary = "";
    private boolean hookOkAttach = false;
    private boolean hookOkDark = false;
    private boolean hookOkFrag = false;
    private boolean hookOkKeyguard = false;
    private boolean hookOkQs = false;
    private boolean hookOkShade = false;
    private boolean hookOkSlot = false;
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean merged = true;
    private int overrideNet = 0;
    private PerformanceModeSource perfSource;
    private PhoneCb phoneCb;
    private TelephonyManager phoneTm;
    private int registeredSub = -2;
    private final IconState state = new IconState();
    private int styleApplies = 0;
    private String styleLastNote = "尚未读取";
    private long styleLastTime = 0L;
    private final List<View> suppressed = new ArrayList<>();
    private final Set<View> tracked = Collections.newSetFromMap(new WeakHashMap<View, Boolean>());
    private String wifiDebug = "-";
    private volatile boolean wired = false;

    private final class PhoneCb extends TelephonyCallback implements
            TelephonyCallback.SignalStrengthsListener,
            TelephonyCallback.ServiceStateListener,
            TelephonyCallback.DisplayInfoListener {
        PhoneCb() {
        }

        @Override
        public void onDisplayInfoChanged(TelephonyDisplayInfo info) {
            if (info == null) return;
            ColorOsPill.this.overrideNet = info.getOverrideNetworkType();
            int o = ColorOsPill.this.overrideNet;
            if (o == 3 || o == 4 || o == 5) {
                ColorOsPill.this.state.mobileTechnology = "5G";
                ColorOsPill.this.invalidateAll();
            }
        }

        @Override
        public void onServiceStateChanged(ServiceState ss) {
            if (ss == null) return;
            ColorOsPill.this.state.inService = ss.getState() == ServiceState.STATE_IN_SERVICE;
            int dnt = TechLabel.dataNetworkType(ss);
            int o = ColorOsPill.this.overrideNet;
            ColorOsPill.this.state.mobileTechnology = (o == 3 || o == 4 || o == 5) ? "5G" : ColorOsPill.techLabel(dnt);
            ColorOsPill.this.invalidateAll();
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength ss) {
            int lvl = ss == null ? 0 : ss.getLevel();
            if (lvl <= 0 && ss != null) {
                for (CellSignalStrength c : ss.getCellSignalStrengths()) {
                    lvl = Math.max(lvl, c.getLevel());
                }
            }
            ColorOsPill.this.state.mobileLevel = Math.max(0, Math.min(4, lvl));
            ColorOsPill.this.state.mobileKnown = true;
            ColorOsPill.this.invalidateAll();
        }
    }

    static final class Pill extends FrameLayout {
        static final float[] DOT_X = new float[]{34.0f, 51.0f, 69.0f, 86.0f};
        static final float[] DOT_Y = new float[]{102.0f, 109.0f, 109.0f, 102.0f};

        private final Rect boxBounds = new Rect();
        private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int Draws = 0;
        private int lastBase = 0;
        private int lastCap = 0;
        private int lastCell = 0;
        private float lastDriftX = 0.0f;
        private float lastDriftY = 0.0f;
        private int lastLitDots = -1;
        private int lastOwnH = 0;
        private int lastPctFaceW = 0;
        private int lastPctReqW = 0;
        private float lastPctSize = 0.0f;
        private float lastPctStroke = 0.0f;
        private int lastRefRowH = 0;
        private float lastScale = 1.0f;
        private final IconPainter painter = new IconPainter();
        private final TextView pct;
        private final Typeface pctBase;
        private int pctBuiltWeight = -1;
        private Typeface pctFace;
        private String pctLastText = "";
        private int refHeight = 0;
        private Bitmap ringCache;
        private Canvas ringCanvas;
        private int ringKey = 0;
        final IconState state;
        final ViewGroup tree;

        Pill(Context context, IconState state, ViewGroup tree) {
            super(context);
            this.state = state;
            this.tree = tree;
            setWillNotDraw(false);
            setClipChildren(false);
            setClipToPadding(false);
            this.dotPaint.setStyle(Paint.Style.FILL);
            this.pctBase = Typeface.create("sans-serif-condensed", Typeface.BOLD);
            this.pctFace = this.pctBase;
            TextView tv = new TextView(context) {
                @Override
                protected void onDraw(Canvas c) {
                    float sw = Pill.this.pctStrokePx();
                    if (sw > 0.02f) {
                        android.text.TextPaint tp = getPaint();
                        tp.setStyle(Paint.Style.FILL_AND_STROKE);
                        tp.setStrokeJoin(Paint.Join.ROUND);
                        tp.setStrokeCap(Paint.Cap.ROUND);
                        tp.setStrokeWidth(sw);
                    } else {
                        getPaint().setStyle(Paint.Style.FILL);
                        getPaint().setStrokeWidth(0.0f);
                    }
                    super.onDraw(c);
                }
            };
            this.pct = tv;
            tv.setIncludeFontPadding(false);
            tv.setSingleLine(true);
            tv.setGravity(Gravity.CENTER);
            tv.setTypeface(this.pctFace);
            addView(tv, new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        }

        private void layoutPctNow(int l, int t, int r, int b) {
            int w = r - l;
            int h = b - t;
            if (w <= 0 || h <= 0) return;
            float cell = Math.min(w, h) * (ColorOsPill.cScale / 100.0f);
            float scale = cell / 120.0f;
            float ox = (w - cell) * 0.5f;
            float oy = (h - cell) * 0.5f;
            boolean swap = ColorOsPill.cSwapMid && (!ColorOsPill.cShowPct || !PillRing.middleBlank(this.state));
            float designY = swap ? 58.0f : BatteryHeader.centreY();
            float cx = ox + (60.0f * scale) + ColorOsPill.cPctDx + this.lastDriftX;
            float cy = oy + (designY * scale) + ColorOsPill.cPctDy + this.lastDriftY;
            int pw = Math.max(this.pct.getMeasuredWidth(), Math.round(cell * 0.9f));
            int ph = Math.max(this.pct.getMeasuredHeight(), Math.round(cell * 0.5f));
            this.pct.measure(
                MeasureSpec.makeMeasureSpec(pw, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(ph, MeasureSpec.EXACTLY)
            );
            int pl = Math.round(cx - (pw * 0.5f));
            int pt = Math.round(cy - (ph * 0.5f));
            this.pct.layout(pl, pt, pl + pw, pt + ph);
        }

        private String pctBoxText() {
            if (Style.capsuleTime) {
                String tl = Style.timeLabel(getContext());
                if (!tl.isEmpty()) return tl;
            }
            return String.valueOf(Math.max(0, Math.min(100, this.state.battery)));
        }

        private int pctFaceWeight() {
            return Math.max(100, Math.min(900, Math.round(ColorOsPill.cBattWeight * 6.5f)));
        }

        float pctStrokePx() {
            if (ColorOsPill.cBattWeight <= 105) return 0.0f;
            return ((ColorOsPill.cBattWeight - 100) / 100.0f) * 1.4f * (this.lastScale > 0.0f ? this.lastScale : 1.0f);
        }

        private void refreshPct(float scale, int tint) {
            boolean show = Style.showPctNow(ColorOsPill.cShowPct);
            if (!show) {
                if (this.pct.getVisibility() != View.GONE) {
                    this.pct.setVisibility(View.GONE);
                }
                return;
            }
            if (this.pct.getVisibility() != View.VISIBLE) {
                this.pct.setVisibility(View.VISIBLE);
            }
            int fw = pctFaceWeight();
            this.lastPctReqW = ColorOsPill.cBattWeight;
            this.lastPctFaceW = fw;
            this.lastPctStroke = pctStrokePx();
            if (fw != this.pctBuiltWeight) {
                this.pctBuiltWeight = fw;
                this.pctFace = Typeface.create(this.pctBase, fw, false);
                this.pct.setTypeface(this.pctFace);
            }
            String txt = pctBoxText();
            if (!txt.equals(this.pctLastText)) {
                this.pctLastText = txt;
                this.pct.setText(txt);
            }
            float ts = BatteryHeader.textSize(ColorOsPill.cBattScale) * scale * (txt.length() >= 4 ? 0.78f : 1.0f);
            this.lastPctSize = ts;
            this.pct.setTextSize(TypedValue.COMPLEX_UNIT_PX, ts);
            this.pct.setLetterSpacing(Style.pctTracking());
            int ink = StatusColors.digitInk(this.state, ColorOsPill.cColour, tint, ColorOsPill.cThreshold, ColorOsPill.cPowerSave, ColorOsPill.cPerfMode);
            this.pct.setTextColor(ink);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            long t0 = System.nanoTime();
            if (ColorOsPill.frameLastNs != 0L) {
                long dt = t0 - ColorOsPill.frameLastNs;
                if (dt > 0L && dt < 500000000L) {
                    ColorOsPill.frameCount++;
                    ColorOsPill.frameSumNs += dt;
                    if (dt < ColorOsPill.frameMinNs) ColorOsPill.frameMinNs = dt;
                    if (dt > 12000000L) ColorOsPill.drop12++;
                    if (dt > 20000000L) ColorOsPill.drop20++;
                }
            }
            ColorOsPill.frameLastNs = t0;
            this.Draws++;
            Style.note(this);

            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            Integer treeTint = ColorOsPill.treeTints.get(this.tree);
            int sysTint = (treeTint != null && treeTint != 0) ? treeTint : Tints.forPill(this);
            if (sysTint == 0) sysTint = ColorOsPill.tintColor;
            int normalInk = StatusColors.normalFor(sysTint);
            int battInk = StatusColors.tint(this.state.battery, this.state.charging, ColorOsPill.cColour, sysTint, ColorOsPill.cThreshold, ColorOsPill.cPowerSave, ColorOsPill.cPerfMode);

            float fade = ColorOsPill.cBurnIn ? PillDrift.fade(ColorOsPill.shadeT) : 0.0f;
            float dx = PillDrift.offset(ColorOsPill.drift.x(), fade, ColorOsPill.cBurnPx);
            float dy = PillDrift.offset(ColorOsPill.drift.y(), fade, ColorOsPill.cBurnPx);
            this.lastDriftX = dx;
            this.lastDriftY = dy;

            float animT = ColorOsPill.cExitAnim ? Motion.clamp(ColorOsPill.shadeT) : 0.0f;
            float alphaFactor = 1.0f - (0.85f * animT);

            if (Style.capsuleAt(this)) {
                if (this.pct.getVisibility() != View.GONE) {
                    this.pct.setVisibility(View.GONE);
                }
                float ch = h * (ColorOsPill.cScale / 100.0f);
                float cy = ((h - ch) * 0.5f) + dy;
                Capsule.draw(canvas, dx, cy, ch, this.state, battInk, normalInk, ColorOsPill.cWeight, ColorOsPill.cShowPct, alphaFactor);
                long dt = System.nanoTime() - t0;
                ColorOsPill.drawCalls++;
                ColorOsPill.drawSumNs += dt;
                if (dt > ColorOsPill.drawMaxNs) ColorOsPill.drawMaxNs = dt;
                return;
            }

            float cell = Math.min(w, h) * (ColorOsPill.cScale / 100.0f);
            this.lastCell = Math.round(cell);
            float scale = cell / 120.0f;
            this.lastScale = scale;
            float left = ((w - cell) * 0.5f) + dx;
            float top = ((h - cell) * 0.5f) + dy;

            refreshPct(scale, sysTint);
            layoutPctNow(getLeft(), getTop(), getRight(), getBottom());

            this.painter.setWeightPercent(ColorOsPill.cWeight);
            this.painter.setLowBatteryThreshold(ColorOsPill.cThreshold);
            this.painter.setShowPercent(false);

            boolean showTopGap = Style.showPctNow(ColorOsPill.cShowPct) && !ColorOsPill.cSwapMid;
            boolean showMidAtTop = ColorOsPill.cSwapMid && !PillRing.middleBlank(this.state);
            if (showTopGap || showMidAtTop || PillRing.forceGap > 0.0f) {
                float gapDeg = 28.0f + PillRing.forceGap;
                if (showTopGap) {
                    String txt = pctBoxText();
                    this.pct.getPaint().getTextBounds(txt, 0, txt.length(), this.boxBounds);
                    float designW = scale > 0.0f ? (this.boxBounds.width() / scale) : 36.0f;
                    gapDeg = BatteryHeader.halfGap(designW, (ColorOsPill.cWeight * 10.5f) / 100.0f) + PillRing.forceGap;
                } else if (showMidAtTop) {
                    gapDeg = TechLabel.neededGap(ColorOsPill.cCentreScale, ColorOsPill.cCentreWeight) + PillRing.forceGap;
                }
                PillRing.draw(canvas, left, top, cell, this.state, battInk, ColorOsPill.cWeight, ColorOsPill.cBattWeight, gapDeg, alphaFactor);
            } else {
                this.painter.drawPart(canvas, 0, left, top, cell, this.state, battInk, normalInk, alphaFactor, false);
            }

            if (!Style.hideMiddle(this.state)) {
                int sMid = canvas.save();
                float midY = ColorOsPill.cSwapMid ? (top - (48.0f * scale)) : top;
                boolean isWifi = !this.state.airplane && this.state.wifiEnabled && (this.state.wifiConnected || Style.wifiOverSim);
                float midScale = (isWifi ? ColorOsPill.cWifiScale : ColorOsPill.cCentreScale) / 100.0f;
                int midWeight = isWifi ? ColorOsPill.cWifiWeight : ColorOsPill.cCentreWeight;
                if (this.state.airplane) {
                    midScale = Bolt.planeScale();
                }
                float cx = left + (cell * 0.5f);
                float cy = midY + (cell * 0.48333332f);
                canvas.scale(midScale, midScale, cx, cy);
                this.painter.setWeightPercent(midWeight);
                if (!TechLabel.draw(canvas, left, midY, cell, this.state, normalInk, midWeight, alphaFactor)) {
                    this.painter.drawPart(canvas, 1, left, midY, cell, this.state, battInk, normalInk, alphaFactor, false);
                }
                canvas.restoreToCount(sMid);
            } else {
                Style.applyCentreBolt(canvas, left, top, cell, this.state.charging, battInk);
            }

            this.painter.setWeightPercent(ColorOsPill.cDotsWeight);
            this.painter.drawPart(canvas, 2, left, top, cell, this.state, battInk, normalInk, alphaFactor, false);
            this.lastLitDots = (!this.state.airplane && this.state.simPresent && this.state.inService) ? this.state.mobileLevel : 0;

            if (Bolt.visible(this.state.charging)) {
                Bolt.paint(canvas, left, top, cell, battInk);
            }

            long dt = System.nanoTime() - t0;
            ColorOsPill.drawCalls++;
            ColorOsPill.drawSumNs += dt;
            if (dt > ColorOsPill.drawMaxNs) ColorOsPill.drawMaxNs = dt;
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            layoutPctNow(left, top, right, bottom);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int parentH = MeasureSpec.getSize(heightMeasureSpec);
            int fallbackPx = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, PILL_DP, getResources().getDisplayMetrics()));
            int refH = this.refHeight > 0 ? this.refHeight : parentH;
            this.lastRefRowH = refH;
            int customPx = ColorOsPill.cSizePx > 0 ? Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ColorOsPill.cSizePx, getResources().getDisplayMetrics())) : fallbackPx;
            this.lastBase = customPx;
            int h = CompactGeometry.height(customPx, refH, parentH, fallbackPx);
            this.lastCap = h;
            this.lastOwnH = h;
            int w = Style.capsuleAt(this) ? Style.capsuleWidth(this, h) : CompactGeometry.width(customPx, refH, fallbackPx);
            int bleed = Math.max(ColorOsPill.cBurnIn ? ColorOsPill.cBurnPx : 0, 2) * 2;
            setMeasuredDimension(w + bleed, h + bleed);
        }

        void setRefHeight(int h) {
            if (h > 0 && h != this.refHeight) {
                this.refHeight = h;
                requestLayout();
            }
        }
    }

    public ColorOsPill() {
        INSTANCE = this;
        this.burnTicker = new Runnable() {
            @Override
            public void run() {
                ColorOsPill.this.stepBurnIn();
            }
        };
    }

    private boolean alive() {
        return !this.blown && this.merged;
    }

    private void applyIconGap(Context context) {
        if (context == null) return;
        final int gapPx = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, cIconGap, context.getResources().getDisplayMetrics()));
        this.main.post(new Runnable() {
            @Override
            public void run() {
                synchronized (ColorOsPill.pills) {
                    for (View v : ColorOsPill.pills.values()) {
                        if (v != null && (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) {
                            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                            if (mlp.leftMargin != gapPx) {
                                mlp.leftMargin = gapPx;
                                v.setLayoutParams(mlp);
                            }
                        }
                    }
                }
            }
        });
    }

    private void applyState() {
        Log.i(TAG, "[pill] " + (this.merged ? "接管（藏原生图标）" : "交还原生 UI"));
        setMerge(this.merged);
        invalidateAll();
    }

    private void attach(ViewGroup root) {
        if (root == null || this.blown) return;
        try {
            Context ctx = root.getContext();
            if (this.appCtx == null && ctx != null) {
                this.appCtx = ctx.getApplicationContext() != null ? ctx.getApplicationContext() : ctx;
                wireSignals(this.appCtx);
                readStyle(this.appCtx);
            }
            View anchor = Slots.batteryView(root);
            if (anchor == null) {
                int endId = resId(ctx, "id", "status_bar_end_side_content");
                if (endId != 0) {
                    View end = root.findViewById(endId);
                    if (end instanceof ViewGroup) {
                        anchor = Slots.batteryView((ViewGroup) end);
                    }
                }
            }
            if (anchor == null) {
                Log.w(TAG, "[pill] 这棵树里找不到锚点，跳过: " + root.getClass().getSimpleName());
                return;
            }
            ViewGroup parent = (ViewGroup) anchor.getParent();
            if (parent == null) return;
            boolean isKg = root.getClass().getSimpleName().contains("Keyguard");
            if (insertPill(root, anchor, isKg)) {
                collect(root);
                collectTints(root);
                setMerge(this.merged);
                Log.i(TAG, "[pill] 已接管: " + root.getClass().getSimpleName() + " 容器=" + parent.getClass().getSimpleName());
                report(false);
            }
        } catch (Throwable t) {
            blow("attach", t);
        }
    }

    private String bgOf(View v) {
        if (v == null) return "无";
        Drawable d = v.getBackground();
        if (d == null) return "无";
        if (d instanceof ColorDrawable) {
            int c = ((ColorDrawable) d).getColor();
            return d.getClass().getSimpleName() + "=#" + Integer.toHexString(c) + "(a=" + Color.alpha(c) + ")";
        }
        return d.getClass().getSimpleName() + "(a=" + d.getAlpha() + ")";
    }

    private String bgProbe() {
        StringBuilder sb = new StringBuilder();
        synchronized (pills) {
            int idx = 0;
            for (View pill : pills.values()) {
                if (pill != null) {
                    idx++;
                    sb.append("      #").append(idx).append(' ');
                    View cur = pill;
                    for (int d = 0; d < 8 && cur != null; d++) {
                        if (d > 0) sb.append(" -> ");
                        sb.append(simple(cur)).append('[').append(bgOf(cur)).append(']');
                        ViewParent p = cur.getParent();
                        cur = p instanceof View ? (View) p : null;
                    }
                    sb.append('\n');
                }
            }
        }
        return sb.toString();
    }

    private void blow(String where, Throwable t) {
        this.blown = true;
        Log.e(TAG, "[pill] 保险丝熔断（" + where + "）: " + t, t);
        this.main.post(new Runnable() {
            @Override
            public void run() {
                ColorOsPill.this.setMerge(false);
            }
        });
    }

    String buildReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("三合一 Status bar ").append(BuildInfo.NAME).append("（CODE ").append(BuildInfo.CODE).append("）\n");
        sb.append("保险丝: ").append(this.blown ? "已熔断" : "正常").append('\n');
        sb.append("接管的树: ").append(pills.size()).append(" 棵 [");
        synchronized (pills) {
            boolean first = true;
            for (ViewGroup vg : pills.keySet()) {
                if (vg != null) {
                    if (!first) sb.append(", ");
                    first = false;
                    sb.append(vg.getClass().getSimpleName());
                }
            }
        }
        sb.append("]\n");
        sb.append("每树染色: [");
        synchronized (treeTints) {
            boolean first = true;
            for (Map.Entry<ViewGroup, Integer> e : treeTints.entrySet()) {
                if (e.getKey() != null) {
                    if (!first) sb.append(", ");
                    first = false;
                    int val = e.getValue() != null ? e.getValue() : 0;
                    sb.append(e.getKey().getClass().getSimpleName()).append('=')
                        .append(val == 0 ? "未知" : ("#" + Integer.toHexString(val)));
                }
            }
        }
        sb.append("] ").append(Tints.state()).append('\n');
        sb.append("信号槽位: ").append(Slots.state()).append('\n');
        sb.append("图标容器: ").append(this.containerSummary).append('\n');
        sb.append("WiFi 原始值: ").append(this.wifiDebug).append('\n');
        sb.append("已压制原生视图: ").append(this.suppressed.size()).append('\n');
        sb.append("合并: ").append(this.merged ? "已开启" : "未开启")
            .append("  退场动画: ").append(cExitAnim ? "开" : "关").append('\n');
        sb.append("电量: ").append(this.state.battery).append("% ")
            .append(this.state.charging ? "充电中" : "")
            .append(cPowerSave ? " 省电" : "")
            .append(cPerfMode ? (" 性能模式=" + (this.perfSource != null ? this.perfSource.source() : "on")) : "")
            .append('\n');
        sb.append("网络: wifi=").append(!this.state.wifiEnabled ? "off" : (this.state.wifiConnected ? "connected" : "on"))
            .append(" lvl=").append(this.state.wifiLevel)
            .append("  信号=").append(this.state.mobileLevel)
            .append(" 制式=").append(TechLabel.apply(this.state.centreTechnology()))
            .append("  飞行=").append(this.state.airplane).append('\n');
        sb.append("样式: ").append(Style.state()).append('\n');
        sb.append("探针: 样式生效 ").append(this.styleApplies).append(" 次")
            .append(this.styleLastTime > 0L ? ("，最近 " + hms(this.styleLastTime) + " " + this.styleLastNote) : "（还没生效过）")
            .append('\n');
        if (drawCalls > 0) {
            sb.append("      自身绘制 平均").append(String.format(Locale.US, "%.2f", (drawSumNs / (double) drawCalls) / 1000000.0))
                .append("ms 最大").append(String.format(Locale.US, "%.2f", drawMaxNs / 1000000.0)).append("ms\n");
        }
        sb.append("      状态色 充电#").append(Integer.toHexString(cColorCharging))
            .append(" 低电#").append(Integer.toHexString(cColorLow))
            .append(" 省电#").append(Integer.toHexString(cPowerColor))
            .append(" 性能#").append(Integer.toHexString(cColorPerf))
            .append(" 电量粗细(字重)=").append(cBattWeight)
            .append(" 图标空隙=").append(cIconGap).append("dp")
            .append(" 数字微调=(").append(cPctDx).append(',').append(cPctDy).append(")\n");
        sb.append("      防烧屏 ").append(cBurnIn ? "开" : "关")
            .append(" 间隔").append(cBurnSec).append("s 幅度").append(cBurnPx).append("px")
            .append(" 相位").append(drift.phase()).append("(").append(drift.x()).append(',').append(drift.y()).append(")")
            .append(" 自动挪 ").append(drift.steps()).append(" 手动 ").append(drift.forced()).append('\n');
        sb.append("      外观 预设=").append(cLook == 1 ? "岛" : "经典")
            .append(" 全局缩放=").append(cScale).append("%\n");
        sb.append("      进度源 左=").append(String.format(Locale.US, "%.3f", fracLeft))
            .append(" 右=").append(String.format(Locale.US, "%.3f", fracRight))
            .append(" 生效=").append(fracSource.isEmpty() ? "（无）" : fracSource)
            .append(" 已挂=").append(fracHired.isEmpty() ? "（无）" : fracHired).append('\n');
        sb.append("      换位 中间↔电量=").append(cSwapMid ? "已互换" : "常规")
            .append("  shadeT=").append(String.format(Locale.US, "%.3f", shadeT)).append('\n');
        sb.append("      原生图标 Drawable 探针（学 KeiMi）:\n      ").append(drawableProbe()).append('\n');
        sb.append("      背景探针（胶囊往上 8 层）:\n").append(bgProbe());
        sb.append("      参数表 百分比").append(cBattScale).append('/').append(cBattWeight)
            .append(" 文字").append(cCentreScale).append('/').append(cCentreWeight)
            .append(" WiFi").append(cWifiScale).append('/').append(cWifiWeight)
            .append(" 点").append(cDotsScale).append('/').append(cDotsWeight).append('\n');
        return sb.toString();
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private void collect(View v) {
        if (v == null || (v instanceof Pill)) return;
        if (isManaged(v)) {
            if (this.tracked.add(v)) {
                this.suppressed.add(v);
                BatteryStyle.watch(v);
            }
            return;
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                collect(vg.getChildAt(i));
            }
        }
    }

    private void collectTints(ViewGroup root) {
        if (root == null) return;
        Tints.seed(root);
        int t = Tints.sticky(root);
        if (t != 0) {
            treeTints.put(root, t);
            tintColor = t;
        }
    }

    private Context ctxOf() {
        if (this.appCtx != null) return this.appCtx;
        try {
            Class<?> at = Class.forName("android.app.ActivityThread");
            Method m = at.getMethod("currentApplication");
            Object app = m.invoke(null);
            if (app instanceof Context) {
                this.appCtx = (Context) app;
                return this.appCtx;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private String drawableProbe() {
        Context ctx = ctxOf();
        if (ctx == null) return "无 ctx";
        String[] names = new String[]{
            "stat_sys_battery", "stat_sys_battery_charge",
            "stat_sys_wifi_signal_1", "stat_sys_wifi_signal_3",
            "stat_sys_signal_3", "stat_sys_signal_4"
        };
        StringBuilder sb = new StringBuilder();
        for (String n : names) {
            try {
                int id = ctx.getResources().getIdentifier(n, "drawable", "com.android.systemui");
                if (id == 0) {
                    sb.append(n).append("=缺 ");
                } else {
                    Drawable d = ctx.getDrawable(id);
                    sb.append(n).append('=').append(d == null ? "null" : (d.getClass().getSimpleName() + (d.isStateful() ? "(有状态)" : ""))).append(' ');
                }
            } catch (Throwable ignored) {
                sb.append(n).append("=err ");
            }
        }
        return sb.toString();
    }

    private void forceRemeasure(View v) {
        View cur = v;
        for (int i = 0; i < 6 && cur != null; i++) {
            cur.requestLayout();
            cur.invalidate();
            if (cur.getClass().getSimpleName().contains("StatusIconContainer")) {
                break;
            }
            ViewParent p = cur.getParent();
            cur = p instanceof View ? (View) p : null;
        }
    }

    private static String hms(long ms) {
        return new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date(ms));
    }

    private void hookAttach(ClassLoader cl) {
        try {
            Method m = View.class.getDeclaredMethod("setVisibility", Integer.TYPE);
            hook(m).intercept(new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    Object thiz = chain.getThisObject();
                    if ((thiz instanceof View) && ColorOsPill.this.alive()) {
                        View v = (View) thiz;
                        if (ColorOsPill.this.tracked.contains(v)) {
                            List<Object> args = chain.getArgs();
                            int req = (args != null && !args.isEmpty() && (args.get(0) instanceof Integer)) ? (Integer) args.get(0) : View.VISIBLE;
                            if (!BatteryStyle.room && req != View.GONE) {
                                return chain.proceed(new Object[]{View.GONE});
                            }
                        }
                        Slots.reassert(v);
                    }
                    return chain.proceed();
                }
            });
            this.hookOkAttach = true;
        } catch (Throwable t) {
            Log.w(TAG, "[pill] setVisibility 挂不上: " + t);
        }
    }

    private void hookDark(ClassLoader cl) {
        try {
            Class<?> cls = Class.forName("com.oplus.systemui.statusbar.pipeline.battery.ui.view.StatBatteryMeterView", false, cl);
            for (Method m : cls.getDeclaredMethods()) {
                if ("onDarkChanged".equals(m.getName())) {
                    hook(m).intercept(new XposedInterface.Hooker() {
                        @Override
                        public Object intercept(XposedInterface.Chain chain) throws Throwable {
                            Object res = chain.proceed();
                            Object thiz = chain.getThisObject();
                            List<Object> args = chain.getArgs();
                            if (args != null) {
                                for (int i = args.size() - 1; i >= 0; i--) {
                                    Object arg = args.get(i);
                                    if (arg instanceof Integer) {
                                        int c = (Integer) arg;
                                        if (c != 0) {
                                            ColorOsPill.tintColor = c | 0xFF000000;
                                            if (thiz instanceof View) {
                                                ViewGroup tree = ColorOsPill.this.treeOf((View) thiz);
                                                if (tree != null) {
                                                    ColorOsPill.treeTints.put(tree, ColorOsPill.tintColor);
                                                }
                                            }
                                            ColorOsPill.this.invalidateAll();
                                            break;
                                        }
                                    }
                                }
                            }
                            return res;
                        }
                    });
                    this.hookOkDark = true;
                }
            }
            // Pure ColorOS mode: MIUI dark hook removed
        } catch (Throwable t) {
            Log.w(TAG, "[pill] onDarkChanged 挂不上: " + t);
        }
    }

    private void hookFractionMethod(ClassLoader cl, final String methodName, final boolean isRight) {
        try {
            Class<?> cls = Class.forName("com.oplus.systemui.separate.OplusPanelViewPagerController", false, cl);
            for (Method m : cls.getDeclaredMethods()) {
                if (methodName.equals(m.getName())) {
                    hook(m).intercept(new XposedInterface.Hooker() {
                        @Override
                        public Object intercept(XposedInterface.Chain chain) throws Throwable {
                            List<Object> args = chain.getArgs();
                            if (args != null) {
                                for (Object a : args) {
                                    if (a instanceof Float) {
                                        float f = Motion.clamp((Float) a);
                                        if (isRight) ColorOsPill.fracRight = f;
                                        else ColorOsPill.fracLeft = f;
                                        float next = Math.max(ColorOsPill.fracLeft, ColorOsPill.fracRight);
                                        if (Math.abs(next - ColorOsPill.shadeT) > 0.004f) {
                                            ColorOsPill.shadeT = next;
                                            ColorOsPill.fracSource = (isRight ? "QS." : "Notify.") + methodName;
                                            ColorOsPill.this.invalidateAll();
                                        }
                                        break;
                                    }
                                }
                            }
                            return chain.proceed();
                        }
                    });
                    fracHired += methodName + " ";
                    this.hookOkQs = true;
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "[pill] 进度源挂不上: " + t);
        }
    }

    private void hookFragment(ClassLoader cl) {
        try {
            Class<?> cls = Class.forName("com.android.systemui.statusbar.phone.fragment.CollapsedStatusBarFragment", false, cl);
            for (Method m : cls.getDeclaredMethods()) {
                if ("onViewCreated".equals(m.getName())) {
                    hook(m).intercept(new XposedInterface.Hooker() {
                        @Override
                        public Object intercept(XposedInterface.Chain chain) throws Throwable {
                            Object res = chain.proceed();
                            List<Object> args = chain.getArgs();
                            if (args != null && !args.isEmpty() && (args.get(0) instanceof ViewGroup)) {
                                final ViewGroup vg = (ViewGroup) args.get(0);
                                vg.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        ColorOsPill.this.attach(vg);
                                    }
                                });
                            }
                            return res;
                        }
                    });
                    this.hookOkFrag = true;
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "[pill] 状态栏入口挂不上: " + t);
        }
    }

    private void hookKeyguard(ClassLoader cl) {
        try {
            Class<?> cls = Class.forName("com.android.systemui.statusbar.phone.KeyguardStatusBarView", false, cl);
            Method m = cls.getDeclaredMethod("onFinishInflate");
            hook(m).intercept(new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    Object res = chain.proceed();
                    Object thiz = chain.getThisObject();
                    if (thiz instanceof ViewGroup) {
                        final ViewGroup vg = (ViewGroup) thiz;
                        vg.post(new Runnable() {
                            @Override
                            public void run() {
                                ColorOsPill.this.attach(vg);
                            }
                        });
                    }
                    return res;
                }
            });
            this.hookOkKeyguard = true;
        } catch (Throwable t) {
            Log.w(TAG, "[pill] 锁屏入口挂不上: " + t);
        }
    }

    private void hookQsFraction(ClassLoader cl) {
        hookFractionMethod(cl, "updateQSExpandFraction", true);
        hookFractionMethod(cl, "updateNotifyExpandFraction", false);
    }

    private void hookShadeFraction(ClassLoader cl) {
        try {
            Class<?> cls = Class.forName("com.android.systemui.shade.ShadeHeaderController", false, cl);
            for (Method m : cls.getDeclaredMethods()) {
                String n = m.getName().toLowerCase(Locale.US);
                if (n.contains("fraction") || n.contains("shade") || n.contains("expansion")) {
                    Class<?>[] pts = m.getParameterTypes();
                    if (pts.length == 1 && pts[0] == Float.TYPE) {
                        hook(m).intercept(new XposedInterface.Hooker() {
                            @Override
                            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                                List<Object> args = chain.getArgs();
                                if (args != null && !args.isEmpty() && (args.get(0) instanceof Float)) {
                                    float f = Motion.clamp((Float) args.get(0));
                                    if (Math.abs(f - ColorOsPill.shadeT) > 0.004f) {
                                        ColorOsPill.shadeT = f;
                                        ColorOsPill.fracSource = "shade";
                                        ColorOsPill.this.invalidateAll();
                                    }
                                }
                                return chain.proceed();
                            }
                        });
                        this.hookOkShade = true;
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "[pill] 展开进度挂不上: " + t);
        }
    }

    private void hookSlotVisibility(ClassLoader cl) {
        try {
            Class<?> cls = Class.forName("com.oplus.systemui.statusbar.phone.signal.OplusStatusBarSignalPolicy", false, cl);
            for (Method m : cls.getDeclaredMethods()) {
                if ("updateSlotIconVisibility".equals(m.getName())) {
                    hook(m).intercept(new XposedInterface.Hooker() {
                        @Override
                        public Object intercept(XposedInterface.Chain chain) throws Throwable {
                            if (ColorOsPill.this.alive()) {
                                List<Object> args = chain.getArgs();
                                if (args != null && args.size() >= 2 && (args.get(0) instanceof String) && (args.get(1) instanceof Boolean)) {
                                    String slot = ((String) args.get(0)).toLowerCase(Locale.US);
                                    if (slot.contains("airplane") || slot.contains("flight") || slot.contains("飞行") || slot.contains("nosim")) {
                                        Object[] next = args.toArray();
                                        next[1] = Boolean.FALSE;
                                        return chain.proceed(next);
                                    }
                                }
                            }
                            return chain.proceed();
                        }
                    });
                    this.hookOkSlot = true;
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "[pill] 槽位入口挂不上: " + t);
        }
    }

    private static String idName(View v) {
        if (v == null || v.getId() == View.NO_ID) return "-";
        try {
            return v.getResources().getResourceEntryName(v.getId());
        } catch (Throwable ignored) {
            return "?";
        }
    }

    private boolean insertPill(ViewGroup root, View anchor, boolean isKeyguard) {
        return insertPill(root, anchor, isKeyguard, false);
    }

    private boolean insertPill(ViewGroup root, View anchor, boolean isKeyguard, boolean force) {
        ViewGroup parent = (ViewGroup) anchor.getParent();
        if (parent == null) return false;
        View existing = pills.get(root);
        if (existing instanceof Pill && existing.getParent() == parent && !force) {
            return true;
        }
        if (existing != null && existing.getParent() instanceof ViewGroup) {
            ((ViewGroup) existing.getParent()).removeView(existing);
        }
        Pill pill = new Pill(root.getContext(), this.state, root);
        int refH = anchor.getHeight() > 0 ? anchor.getHeight() : parent.getHeight();
        pill.setRefHeight(refH);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_VERTICAL;
        lp.leftMargin = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, cIconGap, root.getResources().getDisplayMetrics()));
        int idx = parent.indexOfChild(anchor);
        parent.addView(pill, idx >= 0 ? idx + 1 : parent.getChildCount(), lp);
        Bolt.unclip(pill);
        Style.watchIconTrim(parent, pill);
        pills.put(root, pill);
        Trace.onPillAdded(root, pill);
        return true;
    }

    private void invalidateAll() {
        this.main.post(new Runnable() {
            @Override
            public void run() {
                synchronized (ColorOsPill.pills) {
                    for (View v : ColorOsPill.pills.values()) {
                        if (v != null) {
                            v.invalidate();
                        }
                    }
                }
            }
        });
    }

    private boolean isAirplane(View v) {
        if (v == null) return false;
        CharSequence cd = v.getContentDescription();
        if (cd != null) {
            String s = cd.toString().toLowerCase(Locale.US);
            if (s.contains("飞行") || s.contains("airplane") || s.contains("flight")) {
                return true;
            }
        }
        String idn = idName(v).toLowerCase(Locale.US);
        return idn.contains("airplane") || idn.contains("flight");
    }

    private boolean isManaged(View v) {
        if (v == null || (v instanceof Pill)) return false;
        String cls = v.getClass().getSimpleName();
        if (cls.contains("ModernStatusBarMobileView") || cls.contains("ModernStatusBarWifiView")
            || cls.contains("StatBatteryMeterView") || cls.contains("BatteryMeterView")) {
            return true;
        }
        String idn = idName(v).toLowerCase(Locale.US);
        if ("battery".equals(idn) || "wifi_combo".equals(idn) || "mobile_combo".equals(idn)) {
            return true;
        }
        return isAirplane(v);
    }

    static Map<ViewGroup, View> pillsMap() {
        return pills;
    }

    private void pollBattery(Context context) {
        if (context == null) return;
        try {
            Intent sticky = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (sticky != null) {
                int lvl = sticky.getIntExtra("level", -1);
                int sc = sticky.getIntExtra("scale", 100);
                if (lvl >= 0 && sc > 0) {
                    this.state.battery = (lvl * 100) / sc;
                }
                int status = sticky.getIntExtra("status", 1);
                int plugged = sticky.getIntExtra("plugged", 0);
                this.state.charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL || plugged != 0;
                Bolt.apply(sticky);
            }
            PowerManager pm = (PowerManager) context.getSystemService(PowerManager.class);
            if (pm != null) {
                cPowerSave = pm.isPowerSaveMode();
            }
        } catch (Throwable ignored) {
        }
    }

    private void pollPhone(Context context) {
        if (context == null) return;
        try {
            this.state.airplane = Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
            TelephonyManager baseTm = (TelephonyManager) context.getSystemService("phone");
            if (baseTm == null) {
                this.state.simPresent = false;
                this.state.inService = false;
                this.state.mobileKnown = true;
                return;
            }
            int dataSub = SubscriptionManager.getDefaultDataSubscriptionId();
            if (dataSub == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                dataSub = SubscriptionManager.getDefaultSubscriptionId();
            }
            TelephonyManager tm = dataSub != SubscriptionManager.INVALID_SUBSCRIPTION_ID ? baseTm.createForSubscriptionId(dataSub) : baseTm;
            int simState = tm.getSimState();
            this.state.simPresent = simState == TelephonyManager.SIM_STATE_READY || simState == TelephonyManager.SIM_STATE_UNKNOWN;
            ServiceState ss = tm.getServiceState();
            this.state.inService = this.state.simPresent && !this.state.airplane && (ss == null || ss.getState() == ServiceState.STATE_IN_SERVICE);
            SignalStrength sig = tm.getSignalStrength();
            if (sig != null) {
                this.state.mobileLevel = Math.max(0, Math.min(4, sig.getLevel()));
            }
            int dnt = tm.getDataNetworkType();
            this.state.mobileTechnology = (this.overrideNet == 3 || this.overrideNet == 4 || this.overrideNet == 5) ? "5G" : techLabel(dnt);
            this.state.mobileKnown = true;

            if (dataSub != this.registeredSub) {
                if (this.phoneTm != null && this.phoneCb != null) {
                    try { this.phoneTm.unregisterTelephonyCallback(this.phoneCb); } catch (Throwable ignored) {}
                }
                this.registeredSub = dataSub;
                this.phoneTm = tm;
                this.phoneCb = new PhoneCb();
                tm.registerTelephonyCallback(new Executor() {
                    @Override
                    public void execute(Runnable r) {
                        ColorOsPill.this.main.post(r);
                    }
                }, this.phoneCb);
            }
        } catch (Throwable ignored) {
        }
    }

    private void pollWifi(Context context) {
        if (context == null) return;
        try {
            WifiManager wm = (WifiManager) context.getSystemService("wifi");
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
            if (wm == null) {
                this.state.wifiEnabled = false;
                this.state.wifiConnected = false;
                this.state.wifiLevel = 0;
                this.state.wifiKnown = true;
                return;
            }
            boolean enabled = wm.isWifiEnabled();
            boolean actWifi = false;
            int rssi = -127;
            if (cm != null) {
                Network net = cm.getActiveNetwork();
                NetworkCapabilities nc = net == null ? null : cm.getNetworkCapabilities(net);
                if (nc != null && nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    actWifi = true;
                    if (nc.getTransportInfo() instanceof WifiInfo) {
                        rssi = ((WifiInfo) nc.getTransportInfo()).getRssi();
                    }
                    if (rssi <= -126) {
                        rssi = nc.getSignalStrength();
                    }
                }
            }
            WifiInfo wi = wm.getConnectionInfo();
            int netId = wi == null ? -1 : wi.getNetworkId();
            int freq = wi == null ? 0 : wi.getFrequency();
            if (rssi <= -126 && wi != null) {
                rssi = wi.getRssi();
            }
            boolean connected = enabled && (actWifi || netId != -1 || (rssi > -126 && rssi < 0));
            int lvl = (connected && rssi > -126 && rssi < 0) ? wm.calculateSignalLevel(rssi) : (connected ? 2 : 0);
            this.state.wifiEnabled = enabled;
            this.state.wifiConnected = connected;
            this.state.wifiLevel = Math.max(0, Math.min(3, lvl));
            this.state.wifiKnown = true;
            this.wifiDebug = "netId=" + netId + " rssi=" + rssi + " freq=" + freq + " actIsWifi=" + actWifi;
        } catch (Throwable ignored) {
        }
    }

    private void readStyle(Context context) {
        if (context == null) return;
        Cursor c = null;
        try {
            c = context.getContentResolver().query(STYLE_URI, null, null, null, null);
            if (c == null || !c.moveToFirst()) {
                this.styleLastNote = "provider 不可用";
                return;
            }
            boolean nextMerge = getCol(c, "merge", 1) != 0;
            cSizePx = clamp(getCol(c, "size", 24), 16, 40);
            cWeight = clamp(getCol(c, "weight", 100), 60, 160);
            cThreshold = clamp(getCol(c, "threshold", 20), 5, 50);
            cShowPct = getCol(c, "show_percent", 1) != 0;
            cColour = getCol(c, "colour", 1) != 0;
            cBattScale = clamp(getCol(c, "batt_scale", 100), 50, 200);
            cBattWeight = clamp(getCol(c, "batt_weight", 100), 50, 200);
            cCentreScale = clamp(getCol(c, "centre_scale", 100), 50, 200);
            cCentreWeight = clamp(getCol(c, "centre_weight", 100), 50, 200);
            cWifiScale = clamp(getCol(c, "wifi_scale", 100), 50, 200);
            cWifiWeight = clamp(getCol(c, "wifi_weight", 100), 50, 200);
            cDotsScale = clamp(getCol(c, "dots_scale", 100), 50, 200);
            cDotsWeight = clamp(getCol(c, "dots_weight", 100), 50, 200);
            cPowerColor = StatusColors.resolve(getCol(c, "power_color", 0), -14080);
            cColorCharging = StatusColors.resolve(getCol(c, "color_charging", 0), -13315748);
            cColorLow = StatusColors.resolve(getCol(c, "color_low", 0), -278483);
            cColorPerf = StatusColors.resolve(getCol(c, "color_performance", 0), -15960321);
            StatusColors.SAVER = cPowerColor;
            StatusColors.CHARGING = cColorCharging;
            StatusColors.LOW = cColorLow;
            StatusColors.PERFORMANCE = cColorPerf;
            cSwapMid = getCol(c, "swap_mid", 0) != 0;
            cLook = clamp(getCol(c, "look_preset", 0), 0, 1);
            cScale = clamp(getCol(c, "global_scale", 100), 60, 150);
            cIconGap = clamp(getCol(c, "icon_gap", 0), -12, 24);
            cPctDx = clamp(getCol(c, "pct_dx", 0), -24, 24);
            cPctDy = clamp(getCol(c, "pct_dy", 0), -24, 24);
            cExitAnim = getCol(c, "exit_anim", 1) != 0;
            cBurnIn = getCol(c, "burn_in", 1) != 0;
            cBurnSec = clamp(getCol(c, "burn_sec", 60), 5, 600);
            cBurnPx = clamp(getCol(c, "burn_px", 1), 1, 6);
            int bt = getCol(c, "burn_test", 0);
            if (bt != cBurnTest) {
                cBurnTest = bt;
                drift.forceStep();
            }
            drift.interval(cBurnSec);
            Style.pull(context);
            BatteryStyle.sync(context, nextMerge);
            if (nextMerge != this.merged) {
                this.merged = nextMerge;
                this.main.post(new Runnable() {
                    @Override
                    public void run() {
                        ColorOsPill.this.setMerge(ColorOsPill.this.merged);
                    }
                });
            }
            applyIconGap(context);
            this.styleApplies++;
            this.styleLastTime = System.currentTimeMillis();
            this.styleLastNote = "ok 尺寸" + cSizePx + "px 粗细" + cWeight + " 百分比" + cBattScale + "/" + cBattWeight;
            invalidateAll();
        } catch (Throwable t) {
            this.styleLastNote = "读取失败: " + t.getClass().getSimpleName();
        } finally {
            if (c != null) {
                try { c.close(); } catch (Throwable ignored) {}
            }
        }
    }

    private static int getCol(Cursor c, String name, int def) {
        int i = c.getColumnIndex(name);
        return i >= 0 ? c.getInt(i) : def;
    }

    private void report(boolean withDiag) {
        final Context ctx = ctxOf();
        if (ctx == null) return;
        final String text = buildReport();
        final String diag = withDiag ? (Trace.containers(pills) + "\n" + Scene.now(pills) + "\n" + Trace.dump()) : "";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bundle b = new Bundle();
                    b.putString("text", text);
                    if (!diag.isEmpty()) {
                        b.putString("diag", diag);
                    }
                    ctx.getContentResolver().call(STYLE_URI, "report", null, b);
                } catch (Throwable ignored) {
                }
            }
        }, "CombinedStatus-Report").start();
    }

    private int resId(Context context, String type, String name) {
        if (context == null) return 0;
        try {
            return context.getResources().getIdentifier(name, type, context.getPackageName());
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private void setMerge(boolean merge) {
        this.merged = merge;
        BatteryStyle.sync(this.appCtx, merge);
        for (View v : this.suppressed) {
            if (v != null) {
                if (merge) {
                    BatteryStyle.heal(v);
                } else {
                    v.setVisibility(View.VISIBLE);
                    v.setAlpha(1.0f);
                }
            }
        }
        synchronized (pills) {
            for (View p : pills.values()) {
                if (p != null) {
                    p.setVisibility(merge ? View.VISIBLE : View.GONE);
                    forceRemeasure(p);
                }
            }
        }
    }

    private static String simple(View v) {
        return v == null ? "null" : v.getClass().getSimpleName();
    }

    private void stepBurnIn() {
        long now = SystemClock.uptimeMillis();
        if (drift.update(now, cBurnIn && this.merged)) {
            invalidateAll();
        }
        if (this.wired) {
            this.main.postDelayed(this.burnTicker, drift.delay(now));
        }
    }

    static String techLabel(int netType) {
        switch (netType) {
            case 1:
            case 2:
            case 4:
            case 7:
            case 11:
            case 16:
                return "2G";
            case 3:
            case 5:
            case 6:
            case 8:
            case 9:
            case 10:
            case 12:
            case 14:
            case 15:
            case 17:
                return "3G";
            case 13:
            case 18:
            case 19:
                return "4G";
            case 20:
                return "5G";
            default:
                return "4G";
        }
    }

    private ViewGroup treeOf(View v) {
        View cur = v;
        while (cur != null) {
            if ((cur instanceof ViewGroup) && pills.containsKey(cur)) {
                return (ViewGroup) cur;
            }
            ViewParent p = cur.getParent();
            cur = p instanceof View ? (View) p : null;
        }
        return null;
    }

    private void wireSignals(final Context context) {
        if (this.wired || context == null) return;
        this.wired = true;
        Style.ensureStarted(context);
        pollBattery(context);
        pollWifi(context);
        pollPhone(context);

        try {
            context.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent i) {
                    ColorOsPill.this.pollBattery(c);
                    Style.afterPlug(c, i);
                    ColorOsPill.this.invalidateAll();
                }
            }, new IntentFilter(Intent.ACTION_BATTERY_CHANGED), Context.RECEIVER_EXPORTED);
        } catch (Throwable t) {
            Log.w(TAG, "[pill] 电量广播挂不上: " + t);
        }

        try {
            context.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent i) {
                    PowerManager pm = (PowerManager) c.getSystemService(PowerManager.class);
                    ColorOsPill.cPowerSave = pm != null && pm.isPowerSaveMode();
                    ColorOsPill.this.invalidateAll();
                }
            }, new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED), Context.RECEIVER_EXPORTED);
        } catch (Throwable t) {
            Log.w(TAG, "[pill] 省电广播挂不上: " + t);
        }

        try {
            IntentFilter netF = new IntentFilter();
            netF.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            netF.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            netF.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            netF.addAction(WifiManager.RSSI_CHANGED_ACTION);
            netF.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
            netF.addAction("android.intent.action.SIM_STATE_CHANGED");
            context.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent i) {
                    ColorOsPill.this.pollWifi(c);
                    ColorOsPill.this.pollPhone(c);
                    ColorOsPill.this.invalidateAll();
                }
            }, netF, Context.RECEIVER_EXPORTED);
        } catch (Throwable t) {
            Log.w(TAG, "[pill] 飞行广播挂不上: " + t);
        }

        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
            if (cm != null) {
                cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network net) {
                        ColorOsPill.this.pollWifi(context);
                        ColorOsPill.this.invalidateAll();
                    }

                    @Override
                    public void onCapabilitiesChanged(Network net, NetworkCapabilities nc) {
                        ColorOsPill.this.pollWifi(context);
                        ColorOsPill.this.invalidateAll();
                    }

                    @Override
                    public void onLost(Network net) {
                        ColorOsPill.this.pollWifi(context);
                        ColorOsPill.this.invalidateAll();
                    }
                }, this.main);
            }
        } catch (Throwable t) {
            Log.w(TAG, "[pill] 网络回调挂不上: " + t);
        }

        try {
            this.perfSource = new PerformanceModeSource(context, this.main, new Runnable() {
                @Override
                public void run() {
                    ColorOsPill.cPerfMode = ColorOsPill.this.perfSource.enabled();
                    ColorOsPill.this.invalidateAll();
                }
            });
            this.perfSource.start();
            cPerfMode = this.perfSource.enabled();
        } catch (Throwable t) {
            Log.w(TAG, "[pill] 性能模式监听挂不上: " + t);
        }

        try {
            context.getContentResolver().registerContentObserver(STYLE_URI, true, new ContentObserver(this.main) {
                @Override
                public void onChange(boolean selfChange) {
                    ColorOsPill.this.readStyle(context);
                    ColorOsPill.this.report(false);
                }
            });
        } catch (Throwable t) {
            Log.w(TAG, "[pill] 样式监听挂不上: " + t);
        }

        try {
            context.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent i) {
                    if (i != null && i.getExtras() != null && !i.getExtras().isEmpty()) {
                        if ( i.hasExtra("merge")) {
                            boolean nm = i.getIntExtra("merge", 1) != 0;
                            if (nm != ColorOsPill.this.merged) {
                                ColorOsPill.this.merged = nm;
                                ColorOsPill.this.setMerge(nm);
                            }
                        }
                        if (i.hasExtra("size")) cSizePx = clamp(i.getIntExtra("size", cSizePx), 16, 40);
                        if (i.hasExtra("weight")) cWeight = clamp(i.getIntExtra("weight", cWeight), 60, 160);
                        if (i.hasExtra("threshold")) cThreshold = clamp(i.getIntExtra("threshold", cThreshold), 5, 50);
                        if (i.hasExtra("show_percent")) cShowPct = i.getIntExtra("show_percent", 1) != 0;
                        if (i.hasExtra("colour")) cColour = i.getIntExtra("colour", 1) != 0;
                        if (i.hasExtra("batt_scale")) cBattScale = clamp(i.getIntExtra("batt_scale", cBattScale), 50, 200);
                        if (i.hasExtra("batt_weight")) cBattWeight = clamp(i.getIntExtra("batt_weight", cBattWeight), 50, 200);
                        if (i.hasExtra("centre_scale")) cCentreScale = clamp(i.getIntExtra("centre_scale", cCentreScale), 50, 200);
                        if (i.hasExtra("centre_weight")) cCentreWeight = clamp(i.getIntExtra("centre_weight", cCentreWeight), 50, 200);
                        if (i.hasExtra("wifi_scale")) cWifiScale = clamp(i.getIntExtra("wifi_scale", cWifiScale), 50, 200);
                        if (i.hasExtra("wifi_weight")) cWifiWeight = clamp(i.getIntExtra("wifi_weight", cWifiWeight), 50, 200);
                        if (i.hasExtra("dots_scale")) cDotsScale = clamp(i.getIntExtra("dots_scale", cDotsScale), 50, 200);
                        if (i.hasExtra("dots_weight")) cDotsWeight = clamp(i.getIntExtra("dots_weight", cDotsWeight), 50, 200);
                        if (i.hasExtra("power_color")) {
                            cPowerColor = StatusColors.resolve(i.getIntExtra("power_color", 0), -14080);
                            StatusColors.SAVER = cPowerColor;
                        }
                        if (i.hasExtra("color_charging")) {
                            cColorCharging = StatusColors.resolve(i.getIntExtra("color_charging", 0), -13315748);
                            StatusColors.CHARGING = cColorCharging;
                        }
                        if (i.hasExtra("color_low")) {
                            cColorLow = StatusColors.resolve(i.getIntExtra("color_low", 0), -278483);
                            StatusColors.LOW = cColorLow;
                        }
                        if (i.hasExtra("color_performance")) {
                            cColorPerf = StatusColors.resolve(i.getIntExtra("color_performance", 0), -15960321);
                            StatusColors.PERFORMANCE = cColorPerf;
                        }
                        if (i.hasExtra("swap_mid")) cSwapMid = i.getIntExtra("swap_mid", 0) != 0;
                        if (i.hasExtra("look_preset")) cLook = clamp(i.getIntExtra("look_preset", 0), 0, 1);
                        if (i.hasExtra("global_scale")) cScale = clamp(i.getIntExtra("global_scale", 100), 60, 150);
                        if (i.hasExtra("icon_gap")) {
                            cIconGap = clamp(i.getIntExtra("icon_gap", 0), -12, 24);
                            ColorOsPill.this.applyIconGap(c);
                        }
                        if (i.hasExtra("pct_dx")) cPctDx = clamp(i.getIntExtra("pct_dx", 0), -24, 24);
                        if (i.hasExtra("pct_dy")) cPctDy = clamp(i.getIntExtra("pct_dy", 0), -24, 24);
                        if (i.hasExtra("exit_anim")) cExitAnim = i.getIntExtra("exit_anim", 1) != 0;
                        if (i.hasExtra("burn_in")) cBurnIn = i.getIntExtra("burn_in", 1) != 0;
                        if (i.hasExtra("burn_sec")) {
                            cBurnSec = clamp(i.getIntExtra("burn_sec", 60), 5, 600);
                            drift.interval(cBurnSec);
                        }
                        if (i.hasExtra("burn_px")) cBurnPx = clamp(i.getIntExtra("burn_px", 1), 1, 6);
                        if (i.hasExtra("burn_test")) {
                            int bt = i.getIntExtra("burn_test", 0);
                            if (bt != cBurnTest) {
                                cBurnTest = bt;
                                drift.forceStep();
                            }
                        }
                        Style.apply(c, i);
                        ColorOsPill.this.styleApplies++;
                        ColorOsPill.this.styleLastTime = System.currentTimeMillis();
                        ColorOsPill.this.styleLastNote = "broadcast@" + hms(ColorOsPill.this.styleLastTime);
                        ColorOsPill.this.invalidateAll();
                        ColorOsPill.this.report(i.getBooleanExtra("diag", false));
                    } else {
                        ColorOsPill.this.readStyle(c);
                        ColorOsPill.this.report(false);
                    }
                }
            }, new IntentFilter(ACTION_STYLE), Context.RECEIVER_EXPORTED);
        } catch (Throwable t) {
            Log.w(TAG, "[pill] 样式广播挂不上: " + t);
        }

        this.main.postDelayed(this.burnTicker, drift.delay(SystemClock.uptimeMillis()));
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (!"com.android.systemui".equals(param.getPackageName())) {
            return;
        }
        Log.i(TAG, "[pill] ColorOS 移植版 1.24 启动");
        ClassLoader cl = param.getClassLoader();
        hookFragment(cl);
        hookKeyguard(cl);
        hookAttach(cl);
        hookDark(cl);
        hookShadeFraction(cl);
        hookQsFraction(cl);
        hookSlotVisibility(cl);
    }
}
