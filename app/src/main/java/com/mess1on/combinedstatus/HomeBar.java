package com.mess1on.combinedstatus;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import java.util.WeakHashMap;

final class HomeBar {
    private static int heals = 0;
    private static String lastFix = "-";
    private static final WeakHashMap<ViewGroup, View> watched = new WeakHashMap<>();
    static boolean force = false;
    static String probe = "-";

    private HomeBar() {
    }

    static void foreign(View pillView, String tag) {
        if (pillView == null) return;
        ViewParent p = pillView.getParent();
        if (p instanceof ViewGroup) {
            poke((ViewGroup) p, pillView, tag);
        }
    }

    static void poke(ViewGroup container, View pillView, String reason) {
        if (container == null || pillView == null) return;
        if (pillView.getVisibility() != View.VISIBLE || pillView.getAlpha() < 0.5f) {
            pillView.setVisibility(View.VISIBLE);
            pillView.setAlpha(1.0f);
            heals++;
            lastFix = reason + "@" + Trace.timeNow();
        }
    }

    static void reveal(View v) {
        if (v == null) return;
        if (v.getVisibility() != View.VISIBLE) {
            v.setVisibility(View.VISIBLE);
        }
        if (v.getAlpha() < 1.0f) {
            v.setAlpha(1.0f);
        }
    }

    static String state() {
        return "watched=" + watched.size() + " heals=" + heals + " last=" + lastFix + " force=" + force;
    }

    static void watch(final ViewGroup container, final View pillView) {
        if (container == null || pillView == null) return;
        synchronized (watched) {
            if (watched.containsKey(container)) return;
            watched.put(container, pillView);
        }
        container.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                if (HomeBar.force) {
                    HomeBar.poke(container, pillView, "layout");
                }
            }
        });
    }
}
