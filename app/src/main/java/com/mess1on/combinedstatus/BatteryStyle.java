package com.mess1on.combinedstatus;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import java.util.List;
import java.util.WeakHashMap;

public final class BatteryStyle {
    private static final String[] KEYS = new String[]{
        "status_bar_battery_style",
        "battery_style",
        "oplus_customize_status_bar_battery_style",
        "display_battery_style",
        "notification_And_StatusBar_Battery_Style"
    };
    private static final String SLOT = "battery";
    static boolean allowSystemHide = false;
    private static int benchCount = 0;
    private static long benchMaxUs = 0L;
    private static long benchSumUs = 0L;
    private static boolean blacklisted = false;
    private static String culprit0 = "";
    private static String culprit1 = "";
    private static String culprit2 = "";
    private static int culpritFlips = 0;
    private static int culpritTotal = 0;
    private static long lastHeal = 0L;
    private static boolean merged = true;
    private static String note = "-";
    private static boolean observing = false;
    static boolean room = false;
    private static final WeakHashMap<View, Boolean> watched = new WeakHashMap<>();

    private BatteryStyle() {
    }

    static void benchFlip(View v, int count) {
        if (v == null || count <= 0) {
            return;
        }
        long sum = 0L;
        long max = 0L;
        for (int i = 0; i < count; i++) {
            long t0 = SystemClock.elapsedRealtimeNanos();
            v.setVisibility(View.VISIBLE);
            long dt = (SystemClock.elapsedRealtimeNanos() - t0) / 1000L;
            sum += dt;
            if (dt > max) max = dt;
        }
        benchCount += count;
        benchSumUs += sum;
        if (max > benchMaxUs) benchMaxUs = max;
    }

    static String culpritStacks() {
        if (culpritFlips == 0) {
            return "(尚未抓到有人改原生电池)";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("flips=").append(culpritFlips).append(" (total=").append(culpritTotal).append(")\n");
        if (!culprit0.isEmpty()) sb.append("#1 ").append(culprit0).append('\n');
        if (!culprit1.isEmpty()) sb.append("#2 ").append(culprit1).append('\n');
        if (!culprit2.isEmpty()) sb.append("#3 ").append(culprit2).append('\n');
        return sb.toString();
    }

    static String culpritSummary() {
        return culpritFlips == 0 ? "0次" : (culpritFlips + "次(共" + culpritTotal + ")");
    }

    static void fakeCulprit() {
        recordStack(new Throwable("probe"), "probe");
    }

    static void flush(List<View> views) {
        if (!merged || views == null) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - lastHeal < 40L) {
            return;
        }
        lastHeal = now;
        for (int i = 0; i < views.size(); i++) {
            heal(views.get(i));
        }
    }

    static void heal(View v) {
        if (v == null || !merged) {
            return;
        }
        try {
            if (isBatteryRelated(v)) {
                watch(v);
                suppressOne(v);
            } else {
                v.setVisibility(View.GONE);
            }
        } catch (Throwable ignored) {
        }
    }

    static void healLater(final View v) {
        if (v == null || !merged) {
            return;
        }
        v.postDelayed(new Runnable() {
            @Override
            public void run() {
                BatteryStyle.heal(v);
            }
        }, 80L);
        v.postDelayed(new Runnable() {
            @Override
            public void run() {
                BatteryStyle.heal(v);
            }
        }, 320L);
    }

    static boolean holdsSlot(Context context) {
        if (context == null) {
            return false;
        }
        try {
            String bl = Settings.Secure.getString(context.getContentResolver(), "icon_blacklist");
            if (bl == null || bl.isEmpty()) {
                return false;
            }
            for (String s : bl.split(",")) {
                if (SLOT.equals(s.trim())) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean isBatteryRelated(View v) {
        for (int d = 0; d < 4 && v != null; d++) {
            String cls = v.getClass().getSimpleName();
            if (cls.contains("Battery") || cls.contains("battery")) {
                return true;
            }
            int id = v.getId();
            if (id != View.NO_ID) {
                try {
                    if (v.getResources().getResourceEntryName(id).contains(SLOT)) {
                        return true;
                    }
                } catch (Throwable ignored) {
                }
            }
            Object p = v.getParent();
            v = p instanceof View ? (View) p : null;
        }
        return false;
    }

    static String now(Context context) {
        if (context == null) {
            return "no-ctx";
        }
        ContentResolver cr = context.getContentResolver();
        StringBuilder sb = new StringBuilder();
        for (String k : KEYS) {
            int sys = Settings.System.getInt(cr, k, -99);
            int sec = Settings.Secure.getInt(cr, k, -99);
            if (sys != -99 || sec != -99) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(k).append('=').append(sys != -99 ? ("sys:" + sys) : ("sec:" + sec));
            }
        }
        String bl = Settings.Secure.getString(cr, "icon_blacklist");
        sb.append(" bl=").append((bl == null || bl.isEmpty()) ? "∅" : bl);
        return sb.toString();
    }

    private static void observeOnce(final Context context) {
        if (observing || context == null) {
            return;
        }
        observing = true;
        try {
            ContentResolver cr = context.getContentResolver();
            ContentObserver obs = new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    if (BatteryStyle.merged && BatteryStyle.allowSystemHide) {
                        BatteryStyle.sync(context, true);
                    }
                    Style.refreshAll();
                }
            };
            for (String k : KEYS) {
                try { cr.registerContentObserver(Settings.System.getUriFor(k), false, obs); } catch (Throwable ignored) {}
                try { cr.registerContentObserver(Settings.Secure.getUriFor(k), false, obs); } catch (Throwable ignored) {}
            }
            try { cr.registerContentObserver(Settings.Secure.getUriFor("icon_blacklist"), false, obs); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {
        }
    }

    private static void recordStack(Throwable t, String tag) {
        culpritTotal++;
        StackTraceElement[] els = t.getStackTrace();
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(tag).append("@").append(Trace.timeNow()).append("] ");
        int n = 0;
        for (StackTraceElement e : els) {
            String cn = e.getClassName();
            if (!cn.startsWith("com.mess1on.combinedstatus") && !cn.startsWith("java.lang")) {
                if (n > 0) sb.append(" <- ");
                int dot = cn.lastIndexOf('.');
                sb.append(dot >= 0 ? cn.substring(dot + 1) : cn).append('.').append(e.getMethodName()).append(':').append(e.getLineNumber());
                n++;
                if (n >= 6) break;
            }
        }
        String s = sb.toString();
        if (s.equals(culprit0) || s.equals(culprit1) || s.equals(culprit2)) {
            return;
        }
        culpritFlips++;
        if (culprit0.isEmpty()) culprit0 = s;
        else if (culprit1.isEmpty()) culprit1 = s;
        else culprit2 = s;
    }

    static String state() {
        String s = note + " room=" + (room ? "on" : "off") + " sysHide=" + (allowSystemHide ? "on" : "off") + " culprit=" + culpritSummary();
        if (benchCount > 0) {
            s += " flip=" + benchCount + "× avg=" + (benchSumUs / benchCount) + "us max=" + benchMaxUs + "us";
        }
        return s;
    }

    public static String stripSlot(Context context) {
        if (context == null) {
            return "no-ctx";
        }
        try {
            ContentResolver cr = context.getContentResolver();
            String bl = Settings.Secure.getString(cr, "icon_blacklist");
            if (bl == null || bl.isEmpty()) {
                blacklisted = false;
                return "empty";
            }
            StringBuilder sb = new StringBuilder();
            boolean found = false;
            for (String p : bl.split(",")) {
                String t = p.trim();
                if (SLOT.equals(t)) {
                    found = true;
                } else if (!t.isEmpty()) {
                    if (sb.length() > 0) sb.append(',');
                    sb.append(t);
                }
            }
            if (!found) {
                blacklisted = false;
                return "not-present(" + bl + ")";
            }
            Settings.Secure.putString(cr, "icon_blacklist", sb.toString());
            blacklisted = false;
            note = "stripped";
            return "removed->" + (sb.length() == 0 ? "∅" : sb.toString());
        } catch (Throwable t) {
            return "err:" + t.getClass().getSimpleName();
        }
    }

    private static void suppressOne(View v) {
        if (!room) {
            if (v.getVisibility() != View.GONE) {
                v.setVisibility(View.GONE);
            }
            return;
        }
        if (v.getVisibility() != View.VISIBLE) {
            v.setVisibility(View.VISIBLE);
        }
        v.setAlpha(0.0f);
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp != null && (lp.width != 0 || lp.height != 0)) {
            lp.width = 0;
            lp.height = 0;
            v.setLayoutParams(lp);
        }
        if (v.getWidth() > 0 || v.getHeight() > 0) {
            v.setLeft(0);
            v.setRight(0);
            v.setTop(0);
            v.setBottom(0);
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View c = vg.getChildAt(i);
                c.setAlpha(0.0f);
                if (c.getVisibility() != View.GONE) {
                    c.setVisibility(View.GONE);
                }
            }
        }
    }

    static void sync(Context context, boolean merge) {
        merged = merge;
        if (context == null) {
            return;
        }
        observeOnce(context);
        try {
            ContentResolver cr = context.getContentResolver();
            String bl = Settings.Secure.getString(cr, "icon_blacklist");
            if (bl == null) bl = "";
            boolean has = false;
            for (String p : bl.split(",")) {
                if (SLOT.equals(p.trim())) {
                    has = true;
                    break;
                }
            }
            if (!merge || !allowSystemHide) {
                if (blacklisted && has) {
                    StringBuilder sb = new StringBuilder();
                    for (String p : bl.split(",")) {
                        String t = p.trim();
                        if (!t.isEmpty() && !SLOT.equals(t)) {
                            if (sb.length() > 0) sb.append(',');
                            sb.append(t);
                        }
                    }
                    Settings.Secure.putString(cr, "icon_blacklist", sb.toString());
                    blacklisted = false;
                    note = "restored";
                } else {
                    note = merge ? (has ? "external-has-battery" : "view-only") : "off";
                }
            } else if (!has) {
                Settings.Secure.putString(cr, "icon_blacklist", bl.isEmpty() ? SLOT : (bl + ",battery"));
                blacklisted = true;
                note = "blacklist+battery";
            } else {
                note = "already-blacklisted";
            }
        } catch (Throwable t) {
            note = "err:" + t.getClass().getSimpleName();
        }
    }

    static void watch(View v) {
        if (v == null || !isBatteryRelated(v)) {
            return;
        }
        synchronized (watched) {
            if (watched.containsKey(v)) {
                return;
            }
            watched.put(v, Boolean.TRUE);
        }
        v.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                if (!BatteryStyle.merged) return;
                if (BatteryStyle.room) {
                    if (r - l > 0 || view.getAlpha() > 0.01f) {
                        BatteryStyle.recordStack(new Throwable("layout"), view.getClass().getSimpleName() + ":w=" + (r - l));
                        BatteryStyle.suppressOne(view);
                    }
                } else if (view.getVisibility() != View.GONE || r - l > 0) {
                    BatteryStyle.recordStack(new Throwable("layout"), view.getClass().getSimpleName() + ":vis=" + view.getVisibility());
                    BatteryStyle.suppressOne(view);
                }
            }
        });
    }
}
