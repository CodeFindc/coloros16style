package com.mess1on.combinedstatus;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

final class QsHeader {
    private static final WeakHashMap<ViewGroup, Adopted> adopted = new WeakHashMap<>();
    private static boolean enabled = true;
    private static int guardFixes = 0;
    private static int guardRuns = 0;

    private static final class Adopted {
        final ViewGroup container;
        Guard guard;
        final ArrayList<View> hidden = new ArrayList<>();
        final View pill;

        Adopted(ViewGroup container, View pill) {
            this.container = container;
            this.pill = pill;
        }
    }

    private static final class Guard implements ViewTreeObserver.OnPreDrawListener {
        final Adopted a;

        Guard(Adopted a) {
            this.a = a;
        }

        @Override
        public boolean onPreDraw() {
            QsHeader.guardRuns++;
            if (!QsHeader.enabled) {
                return true;
            }
            if (this.a.pill != null && this.a.pill.getVisibility() != View.VISIBLE) {
                this.a.pill.setVisibility(View.VISIBLE);
                QsHeader.guardFixes++;
            }
            return true;
        }
    }

    private QsHeader() {
    }

    static String census(Map<ViewGroup, View> pillsMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("enabled=").append(enabled).append(" adopted=").append(adopted.size())
            .append(" guard=").append(guardFixes).append('/').append(guardRuns);
        return sb.toString();
    }

    static String enabledText() {
        return enabled ? "开启" : "关闭";
    }

    private static boolean isQsTree(View v) {
        View cur = v;
        for (int i = 0; i < 8 && cur != null; i++) {
            String n = cur.getClass().getSimpleName();
            if (n.contains("QuickStatusBarHeader") || n.contains("ShadeHeader") || n.contains("QsHeader") || n.contains("OplusQs")) {
                return true;
            }
            ViewParent p = cur.getParent();
            cur = p instanceof View ? (View) p : null;
        }
        return false;
    }

    static void setEnabled(boolean en, Map<ViewGroup, View> pillsMap) {
        enabled = en;
        synchronized (adopted) {
            for (Adopted a : adopted.values()) {
                if (a != null && a.pill != null) {
                    a.pill.setVisibility(en ? View.VISIBLE : View.GONE);
                    for (View h : a.hidden) {
                        if (h != null) {
                            h.setVisibility(en ? View.GONE : View.VISIBLE);
                        }
                    }
                }
            }
        }
    }

    static void watch(ViewGroup container, View pillView) {
        if (container == null || pillView == null || !isQsTree(container)) {
            return;
        }
        synchronized (adopted) {
            if (adopted.containsKey(container)) {
                return;
            }
            Adopted a = new Adopted(container, pillView);
            Guard g = new Guard(a);
            a.guard = g;
            adopted.put(container, a);
            try {
                container.getViewTreeObserver().addOnPreDrawListener(g);
            } catch (Throwable ignored) {
            }
            pillView.setVisibility(enabled ? View.VISIBLE : View.GONE);
        }
    }
}
