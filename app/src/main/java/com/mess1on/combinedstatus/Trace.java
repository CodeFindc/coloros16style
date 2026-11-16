package com.mess1on.combinedstatus;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

final class Trace {
    private static final int CAP = 120;
    private static final String[] RING = new String[CAP];
    private static int count = 0;
    private static int head = 0;

    private Trace() {
    }

    static String containers(Map<ViewGroup, View> pillsMap) {
        if (pillsMap == null || pillsMap.isEmpty()) {
            return "(没有接管的树)";
        }
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        for (Map.Entry<ViewGroup, View> e : pillsMap.entrySet()) {
            ViewGroup tree = e.getKey();
            View pill = e.getValue();
            if (tree != null && pill != null) {
                idx++;
                sb.append('#').append(idx).append(' ')
                    .append(tree.getClass().getSimpleName())
                    .append(" pillVis=").append(pill.getVisibility())
                    .append(" size=").append(pill.getWidth()).append('x').append(pill.getHeight())
                    .append(" lit=").append(litCount(tree)).append('\n');
            }
        }
        return sb.toString();
    }

    static synchronized String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("events=").append(count).append('\n');
        int n = Math.min(count, CAP);
        int start = (head - n + CAP) % CAP;
        for (int i = 0; i < n; i++) {
            String line = RING[(start + i) % CAP];
            if (line != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    static Object fieldOf(Class<?> cls, String name) {
        try {
            Field f = cls.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static int litCount(ViewGroup root) {
        if (root == null) return 0;
        int c = 0;
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE && child.getAlpha() > 0.05f && child.getWidth() > 0) {
                c++;
            }
        }
        return c;
    }

    static String litSignature(ViewGroup root) {
        if (root == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < root.getChildCount(); i++) {
            View c = root.getChildAt(i);
            if (c.getVisibility() == View.VISIBLE) {
                sb.append(c.getClass().getSimpleName()).append(':').append(c.getWidth()).append(' ');
            }
        }
        return sb.toString();
    }

    static synchronized void log(String tag, String msg) {
        String entry = timeNow() + " [" + tag + "] " + msg;
        RING[head] = entry;
        head = (head + 1) % CAP;
        count++;
    }

    static void onPillAdded(ViewGroup tree, View pillView) {
        log("pillAdd", (tree == null ? "null" : tree.getClass().getSimpleName()) + " -> " + (pillView == null ? "null" : pillView.getClass().getSimpleName()));
        Scene.watch(tree);
        QsHeader.watch(tree, pillView);
        HomeBar.watch(tree, pillView);
    }

    static String timeNow() {
        return new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date());
    }
}
