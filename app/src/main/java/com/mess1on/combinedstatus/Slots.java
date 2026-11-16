package com.mess1on.combinedstatus;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

final class Slots {
    static boolean clockHide = false;
    private static int clockHits = 0;
    private static String clockNote = "-";
    private static final WeakHashMap<View, Integer> clocks = new WeakHashMap<>();
    static int forceRom = 0;
    static int romMajor = 0;
    private static String romRaw = "";

    static {
        detectRom();
    }

    private Slots() {
    }

    static View batteryView(ViewGroup root) {
        if (root == null) return null;
        String[] ids = new String[]{"battery", "stat_battery_view", "battery_container"};
        for (String name : ids) {
            try {
                int id = root.getResources().getIdentifier(name, "id", root.getContext().getPackageName());
                if (id != 0) {
                    View v = root.findViewById(id);
                    if (v != null) return v;
                }
            } catch (Throwable ignored) {
            }
        }
        return findByClass(root, "Battery", 0);
    }

    static String clockState() {
        return "hide=" + (clockWanted() ? "on" : "off") + " hits=" + clockHits + " tracked=" + clocks.size() + " note=" + clockNote;
    }

    static void clockSweep(Map<ViewGroup, View> pills) {
        boolean wantHide = clockWanted();
        if (pills != null) {
            for (ViewGroup vg : pills.keySet()) {
                View clk = clockView(vg);
                if (clk != null) {
                    synchronized (clocks) {
                        if (!clocks.containsKey(clk)) {
                            clocks.put(clk, clk.getVisibility());
                        }
                    }
                }
            }
        }
        synchronized (clocks) {
            for (Map.Entry<View, Integer> e : clocks.entrySet()) {
                View clk = e.getKey();
                if (clk != null) {
                    if (wantHide) {
                        if (clk.getVisibility() != View.GONE) {
                            clk.setVisibility(View.GONE);
                            clockHits++;
                        }
                    } else {
                        int orig = e.getValue() != null ? e.getValue() : View.VISIBLE;
                        if (clk.getVisibility() != orig) {
                            clk.setVisibility(orig);
                        }
                    }
                }
            }
        }
        clockNote = wantHide ? "hidden" : "visible";
    }

    static View clockView(ViewGroup root) {
        if (root == null) return null;
        View top = root;
        ViewParent p = root.getParent();
        for (int i = 0; i < 5 && (p instanceof ViewGroup); i++) {
            top = (View) p;
            p = p.getParent();
        }
        String[] ids = new String[]{"clock", "status_bar_clock", "keyguard_clock"};
        for (String name : ids) {
            try {
                int id = top.getResources().getIdentifier(name, "id", top.getContext().getPackageName());
                if (id != 0) {
                    View v = top.findViewById(id);
                    if (v != null) return v;
                }
            } catch (Throwable ignored) {
            }
        }
        if (top instanceof ViewGroup) {
            return findByClass((ViewGroup) top, "Clock", 0);
        }
        return null;
    }

    static boolean clockWanted() {
        return clockHide && Style.capsuleWanted && Style.capsuleTime;
    }

    private static void detectRom() {
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            Method get = sp.getMethod("get", String.class, String.class);
            String v = (String) get.invoke(null, "ro.build.version.oplusrom", "");
            if (v == null || v.isEmpty()) {
                v = (String) get.invoke(null, "ro.build.version.oplusrom.display", "");
            }
            romRaw = v == null ? "" : v;
            if (romRaw.contains("16")) romMajor = 16;
            else if (romRaw.contains("15")) romMajor = 15;
            else if (romRaw.contains("14")) romMajor = 14;
        } catch (Throwable ignored) {
        }
    }

    static String dumpTree(ViewGroup vg) {
        if (vg == null) return "(null)";
        StringBuilder sb = new StringBuilder();
        walk(vg, 0, sb);
        return sb.toString();
    }

    private static View findByClass(ViewGroup vg, String keyword, int depth) {
        if (vg == null || depth > 6) return null;
        for (int i = 0; i < vg.getChildCount(); i++) {
            View c = vg.getChildAt(i);
            if (c.getClass().getSimpleName().contains(keyword)) {
                return c;
            }
            if (c instanceof ViewGroup) {
                View found = findByClass((ViewGroup) c, keyword, depth + 1);
                if (found != null) return found;
            }
        }
        return null;
    }

    static boolean isBatteryKid(View v) {
        View cur = v;
        for (int i = 0; i < 4 && cur != null; i++) {
            if (cur.getClass().getSimpleName().contains("Battery")) {
                return true;
            }
            ViewParent p = cur.getParent();
            cur = p instanceof View ? (View) p : null;
        }
        return false;
    }

    static void reassert(View v) {
        if (clockWanted() && v != null) {
            synchronized (clocks) {
                if (clocks.containsKey(v) && v.getVisibility() != View.GONE) {
                    v.setVisibility(View.GONE);
                    clockHits++;
                }
            }
        }
    }

    static String state() {
        return "rom=" + (forceRom != 0 ? forceRom : romMajor) + "(" + romRaw + ") " + clockState();
    }

    private static void walk(View v, int depth, StringBuilder sb) {
        if (v == null || depth > 5 || sb.length() > 2500) return;
        for (int i = 0; i < depth; i++) sb.append("  ");
        sb.append(v.getClass().getSimpleName());
        if (v instanceof TextView) {
            sb.append(" \"").append(((TextView) v).getText()).append('"');
        }
        sb.append('\n');
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                walk(vg.getChildAt(i), depth + 1, sb);
            }
        }
    }
}
