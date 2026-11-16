package com.mess1on.combinedstatus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

final class Bolt {
    private static final String ACTION_EXTRA_BATTERY = "android.intent.action.ADDITIONAL_BATTERY_CHANGED";
    static final int CLASS_FAST = 2;
    static final int CLASS_NONE = 0;
    static final int CLASS_NORMAL = 1;
    static final int CLASS_SUPER = 3;
    static final int CLASS_WIRELESS = 4;
    private static final String[] PROBE_KEYS = new String[]{
        "charge_fast_charger", "charger_technology", "fast_chg_type",
        "charge_wattage", "wireless_reverse_chg_type", "max_charging_current", "max_charging_voltage"
    };

    static boolean center = false;
    private static final Paint clear = new Paint(Paint.ANTI_ALIAS_FLAG);
    static int doubleMode = 0;
    static int dx = 86;
    static int dy = 82;
    private static int extraFastType = 0;
    private static int extraWatt = 0;
    private static final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static boolean hooked = false;
    private static boolean isCharging = false;
    private static int levelPct = -1;
    private static int maxCurrentUa = 0;
    private static int maxVoltageUv = 0;
    private static int oplusFast = 0;
    private static int oplusTech = 0;
    private static int oplusWatt = 0;
    static int planeScale = 100;
    private static int plugged = 0;
    private static String probeDump = "";
    static int scale = 100;
    private static final Path shape = new Path();
    static boolean show = true;
    private static int status = 1;
    private static int sysCurrentUa = 0;
    private static int voltageMv = 0;

    static {
        shape.moveTo(64.0f, 31.0f);
        shape.lineTo(46.0f, 60.0f);
        shape.lineTo(59.0f, 60.0f);
        shape.lineTo(54.0f, 85.0f);
        shape.lineTo(74.0f, 54.0f);
        shape.lineTo(61.0f, 54.0f);
        shape.close();
        fill.setStyle(Paint.Style.FILL);
        clear.setStyle(Paint.Style.STROKE);
        clear.setStrokeJoin(Paint.Join.ROUND);
        clear.setStrokeCap(Paint.Cap.ROUND);
    }

    private Bolt() {
    }

    static void apply(Intent intent) {
        if (intent == null) return;
        status = intent.getIntExtra("status", 1);
        plugged = intent.getIntExtra("plugged", 0);
        int lvl = intent.getIntExtra("level", -1);
        int sc = intent.getIntExtra("scale", 100);
        if (lvl >= 0 && sc > 0) {
            levelPct = (lvl * 100) / sc;
        }
        isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL || plugged != 0) && !chargedFull(levelPct);
        voltageMv = intent.getIntExtra("voltage", 0);
        maxCurrentUa = intent.getIntExtra("max_charging_current", 0);
        maxVoltageUv = intent.getIntExtra("max_charging_voltage", 0);
        oplusTech = intExtra(intent, "charger_technology", "fast_chg_type", "oplus_fast_chg_type");
        oplusWatt = intExtra(intent, "charge_wattage", "wattage", "charger_wattage");
        Boolean fastB = boolOrInt(intent, "charge_fast_charger", "fast_charge", "is_fast_charge");
        if (fastB != null) {
            oplusFast = fastB ? 1 : 0;
        }
        StringBuilder sb = new StringBuilder();
        Bundle ex = intent.getExtras();
        if (ex != null) {
            for (String k : PROBE_KEYS) {
                if (ex.containsKey(k)) {
                    if (sb.length() > 0) sb.append(' ');
                    sb.append(k).append('=').append(ex.get(k));
                }
            }
        }
        probeDump = sb.toString();
    }

    static void applyExtra(Intent intent) {
        if (intent == null) return;
        int ft = intExtra(intent, "fast_chg_type", "charger_technology", "charge_fast_charger");
        if (ft != 0) extraFastType = ft;
        int w = intExtra(intent, "charge_wattage", "wattage");
        if (w != 0) extraWatt = w;
        Style.refreshAll();
    }

    private static Boolean boolOrInt(Intent intent, String... keys) {
        Bundle b = intent.getExtras();
        if (b == null) return null;
        for (String k : keys) {
            if (b.containsKey(k)) {
                Object v = b.get(k);
                if (v instanceof Boolean) return (Boolean) v;
                if (v instanceof Number) return ((Number) v).intValue() != 0;
            }
        }
        return null;
    }

    static boolean centreWanted(boolean charging) {
        return show && center && charging && !chargedFull(levelPct);
    }

    static int chargeClass() {
        if (!isCharging || chargedFull(levelPct)) {
            return CLASS_NONE;
        }
        int w = effectiveWatt();
        if (plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
            return w >= 20 ? CLASS_FAST : CLASS_WIRELESS;
        }
        if (w >= 45 || oplusTech >= 20 || extraFastType >= 20) {
            return CLASS_SUPER;
        }
        if (w >= 18 || oplusFast > 0 || oplusTech > 0 || extraFastType > 0) {
            return CLASS_FAST;
        }
        return CLASS_NORMAL;
    }

    static String chargeNote() {
        int cls = chargeClass();
        String cname;
        switch (cls) {
            case CLASS_NONE: cname = "none"; break;
            case CLASS_NORMAL: cname = "normal"; break;
            case CLASS_FAST: cname = "fast"; break;
            case CLASS_SUPER: cname = "super"; break;
            case CLASS_WIRELESS: cname = "wireless"; break;
            default: cname = "?"; break;
        }
        return cname + "(plug=" + plugged + " w=" + effectiveWatt() + "W tech=" + oplusTech + "/" + extraFastType + " cur=" + (sysCurrentUa / 1000) + "mA)";
    }

    static boolean chargedFull(int level) {
        return level >= 100 && status == BatteryManager.BATTERY_STATUS_FULL;
    }

    static boolean chargingNow() {
        return isCharging && !chargedFull(levelPct);
    }

    private static boolean doubleNow() {
        if (doubleMode == 1) return true;
        if (doubleMode == 2) return false;
        int cls = chargeClass();
        return cls == CLASS_FAST || cls == CLASS_SUPER;
    }

    private static void drawAt(Canvas canvas, float cx, float cy, float s, int color) {
        int save = canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(s, s);
        canvas.translate(-60.0f, -58.0f);
        clear.setColor(0x66000000);
        clear.setStrokeWidth(6.0f);
        canvas.drawPath(shape, clear);
        fill.setColor(color);
        canvas.drawPath(shape, fill);
        canvas.restoreToCount(save);
    }

    private static int effectiveWatt() {
        if (extraWatt > 0) return extraWatt;
        if (oplusWatt > 0) return oplusWatt;
        if (maxCurrentUa > 0 && maxVoltageUv > 0) {
            int mw = (int) (((long) maxCurrentUa * (long) maxVoltageUv) / 1000000000000L);
            if (mw > 0 && mw < 300) return mw;
        }
        int curMa = Math.abs(sysCurrentUa) / 1000;
        int vMv = voltageMv > 0 ? voltageMv : 4000;
        int w = (curMa * vMv) / 1000000;
        return (w > 0 && w < 300) ? w : 0;
    }

    static void init(final Context context) {
        if (hooked || context == null) {
            return;
        }
        hooked = true;
        try {
            Intent sticky = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (sticky != null) {
                apply(sticky);
            }
            BroadcastReceiver rx = new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent i) {
                    if (ACTION_EXTRA_BATTERY.equals(i.getAction())) {
                        Bolt.applyExtra(i);
                    } else {
                        Bolt.apply(i);
                    }
                    Bolt.pollCurrent(c);
                }
            };
            IntentFilter f = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            f.addAction(ACTION_EXTRA_BATTERY);
            context.registerReceiver(rx, f, Context.RECEIVER_EXPORTED);
            pollCurrent(context);
            readSettings(context);
            ContentObserver obs = new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    Bolt.readSettings(context);
                    Style.refreshAll();
                }
            };
            String[] keys = new String[]{"fast_charge_state", "oplus_customize_fast_charge", "super_vooc_charge_state"};
            for (String k : keys) {
                try { context.getContentResolver().registerContentObserver(Settings.System.getUriFor(k), false, obs); } catch (Throwable ignored) {}
                try { context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(k), false, obs); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {
        }
    }

    private static int intExtra(Intent intent, String... keys) {
        Bundle b = intent.getExtras();
        if (b == null) return 0;
        for (String k : keys) {
            if (b.containsKey(k)) {
                Object v = b.get(k);
                if (v instanceof Number) {
                    int n = ((Number) v).intValue();
                    if (n != 0) return n;
                }
            }
        }
        return 0;
    }

    static void paint(Canvas canvas, float ringLeft, float ringTop, float ringSize, int color) {
        if (!show || ringSize <= 0.0f) {
            return;
        }
        float cx = px(ringLeft, ringSize);
        float cy = py(ringTop, ringSize);
        float s = ps(ringSize);
        if (!doubleNow()) {
            drawAt(canvas, cx, cy, s, color);
            return;
        }
        float s2 = ps2(ringSize);
        float gap = ringSize * 0.11f * (scale / 100.0f);
        drawAt(canvas, cx, cy - gap, s2, color);
        drawAt(canvas, cx, cy + gap, s2, color);
    }

    static void paintCentre(Canvas canvas, float ringLeft, float ringTop, float ringSize, int color) {
        if (ringSize <= 0.0f) {
            return;
        }
        float cx = ringLeft + (ringSize * 0.5f);
        float cy = ringTop + (ringSize * 0.48333332f);
        float s = (ringSize / 120.0f) * 0.62f * (scale / 100.0f);
        if (!doubleNow()) {
            drawAt(canvas, cx, cy, s, color);
            return;
        }
        float s2 = s * 0.82f;
        float gap = ringSize * 0.1f * (scale / 100.0f);
        drawAt(canvas, cx, cy - gap, s2, color);
        drawAt(canvas, cx, cy + gap, s2, color);
    }

    static float planeScale() {
        return Math.max(50, Math.min(200, planeScale)) / 100.0f;
    }

    static void pollCurrent(Context context) {
        if (context == null) return;
        try {
            BatteryManager bm = (BatteryManager) context.getSystemService(BatteryManager.class);
            if (bm != null) {
                sysCurrentUa = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
            }
        } catch (Throwable ignored) {
        }
    }

    static float ps(float ringSize) {
        return (ringSize / 120.0f) * 0.46f * (scale / 100.0f);
    }

    static float ps2(float ringSize) {
        return ps(ringSize) * 0.84f;
    }

    static float px(float ringLeft, float ringSize) {
        return ringLeft + ((ringSize * dx) / 100.0f);
    }

    static float py(float ringTop, float ringSize) {
        return ringTop + ((ringSize * dy) / 100.0f);
    }

    static float py2(float ringTop, float ringSize) {
        return py(ringTop, ringSize) + (ringSize * 0.14f);
    }

    static void readSettings(Context context) {
        if (context == null) return;
        String[] keys = new String[]{"fast_charge_state", "oplus_customize_fast_charge", "super_vooc_charge_state"};
        for (String k : keys) {
            try {
                int v = Settings.System.getInt(context.getContentResolver(), k, 0);
                if (v == 0) v = Settings.Secure.getInt(context.getContentResolver(), k, 0);
                if (v > 0) {
                    oplusFast = Math.max(oplusFast, v);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    static String state() {
        return "show=" + show + " center=" + center + " scale=" + scale + "% pos=(" + dx + "," + dy + ")% dbl=" + doubleMode
            + " " + chargeNote() + (probeDump.isEmpty() ? "" : (" [" + probeDump + "]"));
    }

    static void unclip(View v) {
        ViewParent p = v == null ? null : v.getParent();
        for (int i = 0; i < 3 && (p instanceof ViewGroup); i++) {
            ViewGroup vg = (ViewGroup) p;
            vg.setClipChildren(false);
            vg.setClipToPadding(false);
            p = vg.getParent();
        }
    }

    static boolean visible(boolean charging) {
        return show && !center && charging && !chargedFull(levelPct);
    }
}
