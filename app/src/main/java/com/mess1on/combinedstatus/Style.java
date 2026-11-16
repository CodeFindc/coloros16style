package com.mess1on.combinedstatus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;

final class Style {
    static boolean capsuleColours = true;
    static boolean capsuleTime = false;
    static boolean capsuleWanted = false;
    static int centre5gaScale = 0;
    private static int lastAppliedCharge = 0;
    private static int lastOrient = 0;
    private static boolean listening = false;
    private static Handler mainHandler;
    static int pillDx = 0;
    static int pillDxL = 0;
    static int pillDy = 0;
    static int pillDyL = 0;
    private static int pullCount = 0;
    private static String pullNote = "-";
    static int techDx = 0;
    static int techDy = 0;
    static boolean weightLink = false;
    static int wifiDx = 0;
    static int wifiDy = 0;
    static boolean wifiOverSim = false;
    static int pctTrack = 0;

    private Style() {
    }

    static void afterPlug(Context context, Intent intent) {
        Bolt.apply(intent);
        Bolt.pollCurrent(context);
        applyChargingColour();
        refreshAll();
    }

    static int appliedChargingColour() {
        return lastAppliedCharge;
    }

    static boolean apply(Context context, Intent intent) {
        if (intent == null) {
            return pull(context);
        }
        try {
            if (intent.hasExtra("wifi_over_sim")) wifiOverSim = intent.getIntExtra("wifi_over_sim", 0) != 0;
            if (intent.hasExtra("capsule_dual")) capsuleWanted = intent.getIntExtra("capsule_dual", 0) != 0;
            if (intent.hasExtra("capsule_time")) capsuleTime = intent.getIntExtra("capsule_time", 0) != 0;
            if (intent.hasExtra("hide_clock")) Slots.clockHide = intent.getIntExtra("hide_clock", 0) != 0;
            if (intent.hasExtra("capsule_gap")) Capsule.gap = intent.getIntExtra("capsule_gap", 0);
            if (intent.hasExtra("gap_extra")) PillRing.forceGap = intent.getIntExtra("gap_extra", 0);
            if (intent.hasExtra("bolt_show")) Bolt.show = intent.getIntExtra("bolt_show", 1) != 0;
            if (intent.hasExtra("bolt_center")) Bolt.center = intent.getIntExtra("bolt_center", 0) != 0;
            if (intent.hasExtra("bolt_scale")) Bolt.scale = intent.getIntExtra("bolt_scale", 100);
            if (intent.hasExtra("bolt_dx")) Bolt.dx = intent.getIntExtra("bolt_dx", 86);
            if (intent.hasExtra("bolt_dy")) Bolt.dy = intent.getIntExtra("bolt_dy", 82);
            if (intent.hasExtra("bolt_double")) Bolt.doubleMode = intent.getIntExtra("bolt_double", 0);
            if (intent.hasExtra("plane_scale")) Bolt.planeScale = intent.getIntExtra("plane_scale", 100);
            if (intent.hasExtra("centre_5ga_scale")) centre5gaScale = intent.getIntExtra("centre_5ga_scale", 0);
            if (intent.hasExtra("show_5ga")) TechLabel.show5ga = intent.getIntExtra("show_5ga", 0) != 0;
            if (intent.hasExtra("tech_grey")) TechLabel.greyWhenNoData = intent.getIntExtra("tech_grey", 1) != 0;
            if (intent.hasExtra("room")) BatteryStyle.room = intent.getIntExtra("room", 0) != 0;
            if (intent.hasExtra("batt_sys_hide")) BatteryStyle.allowSystemHide = intent.getIntExtra("batt_sys_hide", 0) != 0;
            if (intent.hasExtra("qs_takeover")) QsHeader.setEnabled(intent.getIntExtra("qs_takeover", 1) != 0, pills());
            if (intent.hasExtra("dots_auto")) Dots.auto = intent.getIntExtra("dots_auto", 1) != 0;
            if (intent.hasExtra("dots_dual")) Dots.manualDual = intent.getIntExtra("dots_dual", 0) != 0;
            if (intent.hasExtra("dots_line")) Dots.line = intent.getIntExtra("dots_line", 0) != 0;
            if (intent.hasExtra("dots_dy")) Dots.dy = intent.getIntExtra("dots_dy", 0);
            if (intent.hasExtra("dots_rainbow")) StatusColors.dotRainbow = intent.getIntExtra("dots_rainbow", 0) != 0;
            if (intent.hasExtra("dot_c1")) StatusColors.setDotColour(0, intent.getIntExtra("dot_c1", 0), -12746241);
            if (intent.hasExtra("dot_c2")) StatusColors.setDotColour(1, intent.getIntExtra("dot_c2", 0), -10256129);
            if (intent.hasExtra("dot_c3")) StatusColors.setDotColour(2, intent.getIntExtra("dot_c3", 0), -6001409);
            if (intent.hasExtra("dot_c4")) StatusColors.setDotColour(3, intent.getIntExtra("dot_c4", 0), -268051);
            if (intent.hasExtra("dot_c5")) StatusColors.setDotColour(4, intent.getIntExtra("dot_c5", 0), -12746241);
            if (intent.hasExtra("dot_c6")) StatusColors.setDotColour(5, intent.getIntExtra("dot_c6", 0), -10256129);
            if (intent.hasExtra("dot_c7")) StatusColors.setDotColour(6, intent.getIntExtra("dot_c7", 0), -6001409);
            if (intent.hasExtra("dot_c8")) StatusColors.setDotColour(7, intent.getIntExtra("dot_c8", 0), -268051);
            if (intent.hasExtra("color_normal")) StatusColors.normal = StatusColors.opaque(intent.getIntExtra("color_normal", 0));
            if (intent.hasExtra("color_normal_light")) StatusColors.normalLight = StatusColors.opaque(intent.getIntExtra("color_normal_light", 0));
            if (intent.hasExtra("color_fast")) StatusColors.FAST = StatusColors.opaque(intent.getIntExtra("color_fast", 0));
            if (intent.hasExtra("color_super")) StatusColors.SUPER = StatusColors.opaque(intent.getIntExtra("color_super", 0));
            if (intent.hasExtra("color_wireless")) StatusColors.WIRELESS = StatusColors.opaque(intent.getIntExtra("color_wireless", 0));
            if (intent.hasExtra("charge_digit")) StatusColors.chargeDigits = intent.getIntExtra("charge_digit", 0) != 0;
            if (intent.hasExtra("color_alpha")) StatusColors.alpha = intent.getIntExtra("color_alpha", 100);
            if (intent.hasExtra("alpha_follow")) StatusColors.followIconAlpha = intent.getIntExtra("alpha_follow", 1) != 0;
            if (intent.hasExtra("icon_trim")) StatusColors.iconTrim = intent.getIntExtra("icon_trim", 100);
            if (intent.hasExtra("icon_auto")) StatusColors.iconAuto = intent.getIntExtra("icon_auto", 1) != 0;
            if (intent.hasExtra("wifi_dx")) wifiDx = intent.getIntExtra("wifi_dx", 0);
            if (intent.hasExtra("wifi_dy")) wifiDy = intent.getIntExtra("wifi_dy", 0);
            if (intent.hasExtra("tech_dx")) techDx = intent.getIntExtra("tech_dx", 0);
            if (intent.hasExtra("tech_dy")) techDy = intent.getIntExtra("tech_dy", 0);
            if (intent.hasExtra("pill_dx")) pillDx = intent.getIntExtra("pill_dx", 0);
            if (intent.hasExtra("pill_dy")) pillDy = intent.getIntExtra("pill_dy", 0);
            if (intent.hasExtra("pill_dx_l")) pillDxL = intent.getIntExtra("pill_dx_l", 0);
            if (intent.hasExtra("pill_dy_l")) pillDyL = intent.getIntExtra("pill_dy_l", 0);
            if (intent.hasExtra("pct_track")) pctTrack = intent.getIntExtra("pct_track", 0);
            if (intent.hasExtra("weight_link")) weightLink = intent.getIntExtra("weight_link", 0) != 0;

            Dots.applyMode();
            Dots.applyLayout();
            applyChargingColour();
            Slots.clockSweep(pills());
            pullCount++;
            pullNote = "intent@" + Trace.timeNow();
            refreshAll();
            return true;
        } catch (Throwable t) {
            pullNote = "intent-err:" + t.getClass().getSimpleName();
            return false;
        }
    }

    static void applyCentreBolt(Canvas canvas, float left, float top, float size, boolean charging, int color) {
        if (Bolt.centreWanted(charging)) {
            Bolt.paintCentre(canvas, left, top, size, color);
        }
    }

    static void applyChargingColour() {
        int picked = StatusColors.chargingPick();
        lastAppliedCharge = picked;
        ColorOsPill.cColorCharging = picked;
    }

    static void applyOpacity(View pillView) {
        if (pillView == null) return;
        float op = StatusColors.opacity() / 100.0f;
        if (Math.abs(pillView.getAlpha() - op) > 0.01f) {
            pillView.setAlpha(op);
        }
    }

    static int battScale() {
        return ColorOsPill.cBattScale;
    }

    static int battWeight() {
        return ColorOsPill.cBattWeight;
    }

    static boolean capsuleAt(View pillView) {
        return capsuleWanted;
    }

    static boolean capsuleNow() {
        return capsuleWanted;
    }

    static int capsuleWidth(View pillView, int heightPx) {
        return Capsule.reserveWidth(heightPx);
    }

    static float centre5gaMul() {
        if (centre5gaScale <= 0) {
            return 1.0f;
        }
        return Math.max(50, Math.min(200, centre5gaScale)) / 100.0f;
    }

    static int centreScale() {
        return ColorOsPill.cCentreScale;
    }

    static int centreWeight() {
        return ColorOsPill.cCentreWeight;
    }

    static int digitInkFor(IconState state, int fallbackTint) {
        return StatusColors.digitInk(state, ColorOsPill.cColour, fallbackTint, ColorOsPill.cThreshold, ColorOsPill.cPowerSave, ColorOsPill.cPerfMode);
    }

    static boolean digitsStay() {
        return StatusColors.chargeDigits;
    }

    static void dotsChanged() {
        Dots.applyMode();
        Dots.applyLayout();
        refreshAll();
    }

    static float dotsDy(float scale) {
        return Dots.shift(scale);
    }

    static int dotsScale() {
        return ColorOsPill.cDotsScale;
    }

    static float dotsShiftX(float scale) {
        return Dots.shiftX(scale);
    }

    static void ensureStarted(final Context context) {
        if (listening || context == null) {
            return;
        }
        listening = true;
        mainHandler = new Handler(Looper.getMainLooper());
        Bolt.init(context);
        Dots.start(context);
        TechLabel.listen(context);
        try {
            IntentFilter tf = new IntentFilter();
            tf.addAction(Intent.ACTION_TIME_TICK);
            tf.addAction(Intent.ACTION_TIME_CHANGED);
            tf.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            tf.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
            context.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent i) {
                    if (Intent.ACTION_CONFIGURATION_CHANGED.equals(i.getAction())) {
                        Style.rotated(c);
                    } else {
                        Style.timeTick();
                    }
                }
            }, tf, Context.RECEIVER_EXPORTED);
        } catch (Throwable ignored) {
        }
    }

    static boolean hideMiddle(IconState state) {
        return state != null && Bolt.centreWanted(state.charging);
    }

    static boolean hookFlag(String fieldName) {
        try {
            Field f = ColorOsPill.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            Object owner = owner();
            if (owner != null) {
                Object v = f.get(owner);
                return v instanceof Boolean && (Boolean) v;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    static void note(View pillView) {
        if (pillView == null) {
            return;
        }
        Context ctx = pillView.getContext();
        ensureStarted(ctx);
        boolean land = false;
        try {
            Configuration cfg = pillView.getResources().getConfiguration();
            land = cfg != null && cfg.orientation == Configuration.ORIENTATION_LANDSCAPE;
        } catch (Throwable ignored) {
        }
        int dx = land ? pillDxL : pillDx;
        int dy = land ? pillDyL : pillDy;
        pillView.setTranslationX(dx);
        pillView.setTranslationY(dy);
        applyOpacity(pillView);
    }

    static Object owner() {
        return ColorOsPill.INSTANCE;
    }

    static float pctShiftX(float scale) {
        return scale > 0.0f ? (ColorOsPill.cPctDx / scale) : ColorOsPill.cPctDx;
    }

    static float pctShiftY(float scale) {
        return scale > 0.0f ? (ColorOsPill.cPctDy / scale) : ColorOsPill.cPctDy;
    }

    static float pctTracking() {
        return pctTrack / 1000.0f;
    }

    static int pickTrimNow(ViewGroup root, View skip) {
        if (root == null) return 100;
        for (int i = 0; i < root.getChildCount(); i++) {
            View c = root.getChildAt(i);
            if (c != skip && c.getVisibility() == View.VISIBLE && c.getWidth() > 0) {
                int a = Math.round(c.getAlpha() * 100.0f);
                if (a > 10 && a <= 100) {
                    return a;
                }
            }
        }
        return 100;
    }

    static Map<ViewGroup, View> pills() {
        return ColorOsPill.pillsMap();
    }

    static boolean pollNeedsRedraw(Context context) {
        return pull(context);
    }

    static String pollNote() {
        return "pulls=" + pullCount + " (" + pullNote + ") capsule=" + capsuleWanted + " 5ga=" + TechLabel.show5ga + "/" + TechLabel.fiveGa;
    }

    static boolean pull(Context context) {
        if (context == null) return false;
        Cursor c = null;
        try {
            c = context.getContentResolver().query(StyleProvider.URI, null, null, null, null);
            if (c == null || !c.moveToFirst()) {
                return false;
            }
            wifiOverSim = getInt(c, "wifi_over_sim", 0) != 0;
            capsuleWanted = getInt(c, "capsule_dual", 0) != 0;
            capsuleTime = getInt(c, "capsule_time", 0) != 0;
            Slots.clockHide = getInt(c, "hide_clock", 0) != 0;
            Capsule.gap = getInt(c, "capsule_gap", 0);
            PillRing.forceGap = getInt(c, "gap_extra", 0);
            Bolt.show = getInt(c, "bolt_show", 1) != 0;
            Bolt.center = getInt(c, "bolt_center", 0) != 0;
            Bolt.scale = getInt(c, "bolt_scale", 100);
            Bolt.dx = getInt(c, "bolt_dx", 86);
            Bolt.dy = getInt(c, "bolt_dy", 82);
            Bolt.doubleMode = getInt(c, "bolt_double", 0);
            Bolt.planeScale = getInt(c, "plane_scale", 100);
            centre5gaScale = getInt(c, "centre_5ga_scale", 0);
            TechLabel.show5ga = getInt(c, "show_5ga", 0) != 0;
            TechLabel.greyWhenNoData = getInt(c, "tech_grey", 1) != 0;
            BatteryStyle.room = getInt(c, "room", 0) != 0;
            BatteryStyle.allowSystemHide = getInt(c, "batt_sys_hide", 0) != 0;
            Dots.auto = getInt(c, "dots_auto", 1) != 0;
            Dots.manualDual = getInt(c, "dots_dual", 0) != 0;
            Dots.line = getInt(c, "dots_line", 0) != 0;
            Dots.dy = getInt(c, "dots_dy", 0);
            StatusColors.dotRainbow = getInt(c, "dots_rainbow", 0) != 0;
            StatusColors.setDotColour(0, getInt(c, "dot_c1", 0), -12746241);
            StatusColors.setDotColour(1, getInt(c, "dot_c2", 0), -10256129);
            StatusColors.setDotColour(2, getInt(c, "dot_c3", 0), -6001409);
            StatusColors.setDotColour(3, getInt(c, "dot_c4", 0), -268051);
            StatusColors.setDotColour(4, getInt(c, "dot_c5", 0), -12746241);
            StatusColors.setDotColour(5, getInt(c, "dot_c6", 0), -10256129);
            StatusColors.setDotColour(6, getInt(c, "dot_c7", 0), -6001409);
            StatusColors.setDotColour(7, getInt(c, "dot_c8", 0), -268051);
            StatusColors.normal = StatusColors.opaque(getInt(c, "color_normal", 0));
            StatusColors.normalLight = StatusColors.opaque(getInt(c, "color_normal_light", 0));
            StatusColors.FAST = StatusColors.opaque(getInt(c, "color_fast", 0));
            StatusColors.SUPER = StatusColors.opaque(getInt(c, "color_super", 0));
            StatusColors.WIRELESS = StatusColors.opaque(getInt(c, "color_wireless", 0));
            StatusColors.chargeDigits = getInt(c, "charge_digit", 0) != 0;
            StatusColors.alpha = getInt(c, "color_alpha", 100);
            StatusColors.followIconAlpha = getInt(c, "alpha_follow", 1) != 0;
            StatusColors.iconTrim = getInt(c, "icon_trim", 100);
            StatusColors.iconAuto = getInt(c, "icon_auto", 1) != 0;
            wifiDx = getInt(c, "wifi_dx", 0);
            wifiDy = getInt(c, "wifi_dy", 0);
            techDx = getInt(c, "tech_dx", 0);
            techDy = getInt(c, "tech_dy", 0);
            pillDx = getInt(c, "pill_dx", 0);
            pillDy = getInt(c, "pill_dy", 0);
            pillDxL = getInt(c, "pill_dx_l", 0);
            pillDyL = getInt(c, "pill_dy_l", 0);
            pctTrack = getInt(c, "pct_track", 0);
            weightLink = getInt(c, "weight_link", 0) != 0;

            Dots.applyMode();
            Dots.applyLayout();
            applyChargingColour();
            Slots.clockSweep(pills());
            pullCount++;
            pullNote = "cursor@" + Trace.timeNow();
            return true;
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (c != null) {
                try { c.close(); } catch (Throwable ignored) {}
            }
        }
    }

    private static int getInt(Cursor c, String col, int def) {
        int idx = c.getColumnIndex(col);
        return idx >= 0 ? c.getInt(idx) : def;
    }

    static void refreshAll() {
        Map<ViewGroup, View> map = pills();
        if (map == null) return;
        for (View v : map.values()) {
            if (v != null) {
                v.post(new Runnable() {
                    @Override
                    public void run() {
                        Style.note(v);
                        v.requestLayout();
                        v.invalidate();
                    }
                });
            }
        }
    }

    static void rotated(Context context) {
        if (context == null) return;
        int o = context.getResources().getConfiguration().orientation;
        if (o != lastOrient) {
            lastOrient = o;
            refreshAll();
        }
    }

    static void schedulePull(final Context context) {
        if (mainHandler == null) {
            mainHandler = new Handler(Looper.getMainLooper());
        }
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Style.pull(context)) {
                    Style.refreshAll();
                }
            }
        }, 120L);
    }

    static float shiftX(boolean isWifi, float scale) {
        int px = isWifi ? wifiDx : techDx;
        if (px == 0) return 0.0f;
        return scale > 0.0f ? (px / scale) : px;
    }

    static float shiftY(boolean isWifi, float scale) {
        int px = isWifi ? wifiDy : techDy;
        if (px == 0) return 0.0f;
        return scale > 0.0f ? (px / scale) : px;
    }

    static boolean showPctNow(boolean showPct) {
        return !capsuleWanted && (showPct || capsuleTime);
    }

    static String state() {
        return pollNote() + " bolt=[" + Bolt.state() + "] dots=[" + Dots.now() + "] slots=[" + Slots.state() + "]";
    }

    static boolean swapMid() {
        return ColorOsPill.cSwapMid;
    }

    static String timeLabel(Context context) {
        boolean is24 = true;
        if (context != null) {
            try {
                is24 = DateFormat.is24HourFormat(context);
            } catch (Throwable ignored) {
            }
        }
        return DateFormat.format(is24 ? "HH:mm" : "h:mm", new Date()).toString();
    }

    static void timeTick() {
        if (capsuleTime) {
            refreshAll();
        }
    }

    static void watchIconTrim(final ViewGroup root, final View pillView) {
        if (root == null || pillView == null) return;
        root.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                if (StatusColors.iconAuto) {
                    int trim = Style.pickTrimNow(root, pillView);
                    if (trim != StatusColors.iconTrim) {
                        StatusColors.iconTrim = trim;
                        Style.applyOpacity(pillView);
                    }
                }
            }
        });
    }

    static int wifiScale() {
        return ColorOsPill.cWifiScale;
    }

    static int wifiWeight() {
        return ColorOsPill.cWifiWeight;
    }
}
