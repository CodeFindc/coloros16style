package com.mess1on.combinedstatus;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

final class PerformanceModeSource {
    private static final String[][] KEYS = new String[][]{
        new String[]{"system", "power_performance_mode_open"},
        new String[]{"system", "performance_mode_enable"},
        new String[]{"system", "high_performance_mode_on"},
        new String[]{"secure", "high_performance_mode_on"},
        new String[]{"secure", "performance_mode_enable"},
        new String[]{"secure", "oplus_customize_power_mode"},
        new String[]{"global", "high_performance_mode_on"},
        new String[]{"global", "performance_mode_enable"}
    };
    private final Context context;
    private volatile boolean enabled;
    private final ContentObserver observer;
    private final Runnable onChange;
    private boolean registered;
    private volatile String source = "none";

    PerformanceModeSource(Context context, Handler handler, final Runnable onChange) {
        this.context = context;
        this.onChange = onChange;
        this.observer = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                if (PerformanceModeSource.this.refresh() && onChange != null) {
                    onChange.run();
                }
            }
        };
    }

    private static boolean isPerformanceValue(String key, String val) {
        if (val == null) {
            return false;
        }
        String t = val.trim();
        if (t.isEmpty()) {
            return false;
        }
        if ("oplus_customize_power_mode".equals(key)) {
            return "2".equals(t) || "high".equalsIgnoreCase(t) || "performance".equalsIgnoreCase(t);
        }
        return "1".equals(t) || "true".equalsIgnoreCase(t);
    }

    private static String readSetting(ContentResolver cr, String table, String key) {
        try {
            switch (table) {
                case "system": return Settings.System.getString(cr, key);
                case "secure": return Settings.Secure.getString(cr, key);
                case "global": return Settings.Global.getString(cr, key);
                default: return null;
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Uri uriFor(String table, String key) {
        switch (table) {
            case "system": return Settings.System.getUriFor(key);
            case "secure": return Settings.Secure.getUriFor(key);
            case "global": return Settings.Global.getUriFor(key);
            default: return null;
        }
    }

    boolean enabled() {
        return this.enabled;
    }

    boolean refresh() {
        boolean old = this.enabled;
        ContentResolver cr = this.context.getContentResolver();
        boolean found = false;
        String hit = "none";
        for (String[] pair : KEYS) {
            String val = readSetting(cr, pair[0], pair[1]);
            if (isPerformanceValue(pair[1], val)) {
                found = true;
                hit = pair[0] + ":" + pair[1] + "=" + val.trim();
                break;
            }
        }
        this.enabled = found;
        this.source = hit;
        return old != found;
    }

    String source() {
        return this.source;
    }

    void start() {
        if (this.registered) {
            return;
        }
        ContentResolver cr = this.context.getContentResolver();
        for (String[] pair : KEYS) {
            try {
                Uri uri = uriFor(pair[0], pair[1]);
                if (uri != null) {
                    cr.registerContentObserver(uri, false, this.observer);
                }
            } catch (Throwable ignored) {
            }
        }
        this.registered = true;
        refresh();
    }

    void stop() {
        if (!this.registered) {
            return;
        }
        try {
            this.context.getContentResolver().unregisterContentObserver(this.observer);
        } catch (Throwable ignored) {
        }
        this.registered = false;
    }
}
