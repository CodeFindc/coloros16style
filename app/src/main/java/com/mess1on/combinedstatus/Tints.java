package com.mess1on.combinedstatus;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;

final class Tints {
    private static int benchFlips = 0;
    private static long benchMaxNs = 0L;
    private static long benchSumNs = 0L;
    private static final WeakHashMap<ViewGroup, Integer> perTree = new WeakHashMap<>();

    private Tints() {
    }

    static void bench(int iterations) {
        ViewGroup tree = null;
        synchronized (perTree) {
            for (ViewGroup vg : perTree.keySet()) {
                if (vg != null && vg.isAttachedToWindow()) {
                    tree = vg;
                    break;
                }
            }
        }
        if (tree == null) {
            return;
        }
        long sum = 0L;
        long max = 0L;
        for (int i = 0; i < iterations; i++) {
            long t0 = System.nanoTime();
            followNow(tree, null);
            long dt = System.nanoTime() - t0;
            sum += dt;
            if (dt > max) max = dt;
        }
        benchFlips += iterations;
        benchSumNs += sum;
        if (max > benchMaxNs) benchMaxNs = max;
    }

    static void followNow(ViewGroup root, View pillView) {
        if (root == null) {
            return;
        }
        int sampled = sampleTree(root, pillView);
        if (sampled == 0) {
            sampled = sticky(root);
        }
        if (sampled == 0) {
            return;
        }
        synchronized (perTree) {
            Integer prev = perTree.put(root, sampled);
            if (prev != null && prev == sampled) {
                return;
            }
        }
        if (pillView != null) {
            pillView.invalidate();
        }
    }

    static int forPill(View pillView) {
        if (pillView == null) {
            return 0;
        }
        ViewGroup root = rootOf(pillView);
        if (root == null) {
            return 0;
        }
        int sampled = sampleTree(root, pillView);
        if (sampled != 0) {
            synchronized (perTree) {
                perTree.put(root, sampled);
            }
            return sampled;
        }
        return sticky(root);
    }

    private static boolean isOpaqueRGB(int c) {
        return c != 0 && ((c >>> 24) & 0xFF) >= 40;
    }

    private static int reflectTint(View v) {
        String[] fields = new String[]{"mDarkTint", "mTint", "mIconTint", "mTextColor", "mColor"};
        Class<?> c = v.getClass();
        for (int depth = 0; depth < 3 && c != null; depth++) {
            for (String fn : fields) {
                try {
                    Field f = c.getDeclaredField(fn);
                    f.setAccessible(true);
                    Object val = f.get(v);
                    if (val instanceof Integer && isOpaqueRGB((Integer) val)) {
                        return ((Integer) val) | 0xFF000000;
                    }
                } catch (Throwable ignored) {
                }
            }
            c = c.getSuperclass();
        }
        return 0;
    }

    private static ViewGroup rootOf(View v) {
        ViewGroup last = v instanceof ViewGroup ? (ViewGroup) v : null;
        ViewParent p = v.getParent();
        for (int i = 0; i < 8 && (p instanceof ViewGroup); i++) {
            ViewGroup vg = (ViewGroup) p;
            last = vg;
            String name = vg.getClass().getSimpleName();
            if (name.contains("PhoneStatusBarView") || name.contains("KeyguardStatusBarView")
                || name.contains("QuickStatusBarHeader") || name.contains("ShadeHeader")) {
                return vg;
            }
            p = vg.getParent();
        }
        return last;
    }

    private static int sampleClock(ViewGroup root, View skip) {
        try {
            int id = root.getResources().getIdentifier("clock", "id", root.getContext().getPackageName());
            if (id != 0) {
                View clk = root.findViewById(id);
                if (clk instanceof TextView && clk != skip) {
                    int c = ((TextView) clk).getCurrentTextColor();
                    if (isOpaqueRGB(c)) {
                        return c | 0xFF000000;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static int sampleImages(ViewGroup vg, View skip, int depth) {
        if (depth > 5) {
            return 0;
        }
        for (int i = 0; i < vg.getChildCount(); i++) {
            View child = vg.getChildAt(i);
            if (child != skip && !"Pill".equals(child.getClass().getSimpleName())) {
                if (child instanceof ImageView && child.getVisibility() == View.VISIBLE) {
                    ImageView iv = (ImageView) child;
                    ColorStateList csl = iv.getImageTintList();
                    if (csl != null && isOpaqueRGB(csl.getDefaultColor())) {
                        return csl.getDefaultColor() | 0xFF000000;
                    }
                    Drawable d = iv.getDrawable();
                    if (d != null) {
                        int rt = reflectTint(iv);
                        if (isOpaqueRGB(rt)) {
                            return rt | 0xFF000000;
                        }
                    }
                }
                if (child instanceof ViewGroup) {
                    int sub = sampleImages((ViewGroup) child, skip, depth + 1);
                    if (sub != 0) return sub;
                }
            }
        }
        return 0;
    }

    private static int sampleTexts(ViewGroup vg, View skip, int depth) {
        if (depth > 5) {
            return 0;
        }
        for (int i = 0; i < vg.getChildCount(); i++) {
            View child = vg.getChildAt(i);
            if (child != skip && !"Pill".equals(child.getClass().getSimpleName())) {
                if (child instanceof TextView && child.getVisibility() == View.VISIBLE && child.getWidth() > 0) {
                    int c = ((TextView) child).getCurrentTextColor();
                    if (isOpaqueRGB(c)) {
                        return c | 0xFF000000;
                    }
                }
                if (child instanceof ViewGroup) {
                    int sub = sampleTexts((ViewGroup) child, skip, depth + 1);
                    if (sub != 0) return sub;
                }
            }
        }
        return 0;
    }

    private static int sampleTree(ViewGroup root, View skip) {
        int c = sampleClock(root, skip);
        if (c != 0) return c;
        c = sampleTexts(root, skip, 0);
        if (c != 0) return c;
        return sampleImages(root, skip, 0);
    }

    static void seed(ViewGroup root) {
        followNow(root, null);
    }

    static String state() {
        StringBuilder sb = new StringBuilder();
        synchronized (perTree) {
            sb.append("trees=").append(perTree.size());
            for (Map.Entry<ViewGroup, Integer> e : perTree.entrySet()) {
                ViewGroup k = e.getKey();
                Integer v = e.getValue();
                if (k != null && v != null) {
                    sb.append(' ').append(k.getClass().getSimpleName())
                        .append("=#").append(Integer.toHexString(v));
                }
            }
        }
        if (benchFlips > 0) {
            sb.append(" bench=").append(benchFlips).append("× avg=")
                .append((benchSumNs / benchFlips) / 1000L).append("us max=")
                .append(benchMaxNs / 1000L).append("us");
        }
        return sb.toString();
    }

    static int sticky(ViewGroup root) {
        synchronized (perTree) {
            Integer v = perTree.get(root);
            return v != null ? v : 0;
        }
    }
}
