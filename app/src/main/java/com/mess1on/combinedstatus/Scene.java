package com.mess1on.combinedstatus;

import android.view.View;
import android.view.ViewGroup;
import java.util.Map;
import java.util.WeakHashMap;

final class Scene {
    private static final WeakHashMap<ViewGroup, Boolean> hooked = new WeakHashMap<>();
    static volatile String last = "(还没采样)";
    static volatile int lastCount = -1;
    static volatile long lastWhen = 0L;

    private Scene() {
    }

    private static boolean isKeyguard(ViewGroup vg) {
        View v = vg;
        for (int i = 0; i < 6 && v != null; i++) {
            if (v.getClass().getSimpleName().contains("Keyguard")) {
                return true;
            }
            Object p = v.getParent();
            v = p instanceof View ? (View) p : null;
        }
        return false;
    }

    private static int lit(ViewGroup root) {
        int count = 0;
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            String cls = child.getClass().getSimpleName();
            if (!"Pill".equals(cls) && child.getVisibility() == View.VISIBLE && child.getAlpha() > 0.05f && child.getWidth() > 0) {
                if (child instanceof ViewGroup) {
                    count += lit((ViewGroup) child);
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    static String now(Map<ViewGroup, ?> map) {
        if (map == null || map.isEmpty()) {
            return "(无接管树)";
        }
        StringBuilder sb = new StringBuilder();
        for (ViewGroup vg : map.keySet()) {
            if (vg != null) {
                sb.append("=== ").append(vg.getClass().getSimpleName())
                    .append('@').append(Integer.toHexString(System.identityHashCode(vg)))
                    .append(" vis=").append(vg.getVisibility())
                    .append(" a=").append(vg.getAlpha())
                    .append(" lit=").append(lit(vg)).append(" ===\n");
                tree(vg, 1, sb);
            }
        }
        return sb.toString();
    }

    private static void sample(ViewGroup vg) {
        int c = lit(vg);
        long now = System.currentTimeMillis();
        if (c != lastCount || now - lastWhen >= 1500L) {
            lastCount = c;
            lastWhen = now;
            StringBuilder sb = new StringBuilder();
            sb.append("lit=").append(c).append(" @").append(Trace.timeNow()).append('\n');
            tree(vg, 0, sb);
            last = sb.toString();
            Trace.log("lockScene", "锁屏可见子view=" + c);
        }
    }

    static String state() {
        return last;
    }

    private static void tree(View v, int depth, StringBuilder sb) {
        if (v == null || depth > 5 || sb.length() > 3500) {
            return;
        }
        for (int i = 0; i < depth; i++) {
            sb.append("  ");
        }
        sb.append(v.getClass().getSimpleName());
        int id = v.getId();
        if (id != View.NO_ID) {
            try {
                sb.append('#').append(v.getResources().getResourceEntryName(id));
            } catch (Throwable ignored) {
            }
        }
        sb.append(" vis=").append(v.getVisibility())
            .append(" a=").append(String.format("%.2f", v.getAlpha()))
            .append(" ").append(v.getWidth()).append('x').append(v.getHeight()).append('\n');
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                tree(vg.getChildAt(i), depth + 1, sb);
            }
        }
    }

    static void watch(final ViewGroup vg) {
        if (vg == null || !isKeyguard(vg)) {
            return;
        }
        synchronized (hooked) {
            if (hooked.containsKey(vg)) {
                return;
            }
            hooked.put(vg, Boolean.TRUE);
        }
        try {
            vg.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    try {
                        Scene.sample(vg);
                    } catch (Throwable ignored) {
                    }
                    return true;
                }
            });
            sample(vg);
        } catch (Throwable ignored) {
        }
    }
}
