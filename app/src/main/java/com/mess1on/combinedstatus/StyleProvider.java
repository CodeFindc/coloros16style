package com.mess1on.combinedstatus;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;

public final class StyleProvider extends ContentProvider {
    public static final String AUTHORITY = "com.mess1on.combinedstatus.style";
    public static final String PREFS = "coloros_style";
    public static final Uri URI = Uri.parse("content://com.mess1on.combinedstatus.style/style");

    public static SharedPreferences prefs(Context context) {
        Context dps = context.createDeviceProtectedStorageContext();
        dps.moveSharedPreferencesFrom(context, PREFS);
        return dps.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Context ctx = getContext();
        if (ctx != null && "report".equals(method) && extras != null) {
            SharedPreferences.Editor ed = prefs(ctx).edit()
                .putString("report", extras.getString("text", ""))
                .putLong("report_time", System.currentTimeMillis());
            String diag = extras.getString("diag", "");
            if (!diag.isEmpty()) {
                ed.putString("self_diag", diag);
            }
            ed.commit();
        }
        return Bundle.EMPTY;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.item/vnd.com.mess1on.combinedstatus.style";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return URI;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Context ctx = getContext();
        SharedPreferences sp = ctx == null ? null : prefs(ctx);
        String[] cols = new String[]{
            "merge", "size", "weight", "threshold", "show_percent", "colour",
            "batt_scale", "batt_weight", "centre_scale", "centre_weight",
            "wifi_scale", "wifi_weight", "dots_scale", "dots_weight",
            "power_color", "color_charging", "color_low", "color_performance",
            "swap_mid", "look_preset", "global_scale", "icon_gap",
            "pct_dx", "pct_dy", "pct_track", "exit_anim",
            "burn_in", "burn_sec", "burn_px", "burn_test",
            "bolt_scale", "bolt_dx", "bolt_dy", "bolt_double",
            "wifi_over_sim", "color_normal", "bolt_show", "color_alpha",
            "alpha_follow", "icon_trim", "icon_auto", "color_normal_light",
            "wifi_dx", "wifi_dy", "tech_dx", "tech_dy",
            "pill_dx", "pill_dy", "pill_dx_l", "pill_dy_l",
            "weight_link", "bolt_center", "color_fast", "color_super",
            "color_wireless", "dots_rainbow", "dot_c1", "dot_c2",
            "dot_c3", "dot_c4", "qs_takeover", "dots_auto",
            "dots_dual", "dots_line", "dots_dy", "dot_c5",
            "dot_c6", "dot_c7", "dot_c8", "show_5ga",
            "plane_scale", "centre_5ga_scale", "room", "tech_grey",
            "gap_extra", "capsule_dual", "charge_digit", "capsule_gap",
            "batt_sys_hide", "capsule_time", "hide_clock"
        };
        MatrixCursor cursor = new MatrixCursor(cols, 1);
        Object[] row = new Object[]{
            sp == null || sp.getBoolean("merge", true) ? 1 : 0,
            sp == null ? 24 : sp.getInt("size", 24),
            sp == null ? 100 : sp.getInt("weight", 100),
            sp == null ? 20 : sp.getInt("threshold", 20),
            sp == null || sp.getBoolean("show_percent", true) ? 1 : 0,
            sp == null || sp.getBoolean("colour", true) ? 1 : 0,
            sp == null ? 100 : sp.getInt("batt_scale", 100),
            sp == null ? 100 : sp.getInt("batt_weight", 100),
            sp == null ? 100 : sp.getInt("centre_scale", 100),
            sp == null ? 100 : sp.getInt("centre_weight", 100),
            sp == null ? 100 : sp.getInt("wifi_scale", 100),
            sp == null ? 100 : sp.getInt("wifi_weight", 100),
            sp == null ? 100 : sp.getInt("dots_scale", 100),
            sp == null ? 100 : sp.getInt("dots_weight", 100),
            sp == null ? 0 : sp.getInt("power_color", 0),
            sp == null ? 0 : sp.getInt("color_charging", 0),
            sp == null ? 0 : sp.getInt("color_low", 0),
            sp == null ? 0 : sp.getInt("color_performance", 0),
            sp != null && sp.getBoolean("swap_mid", false) ? 1 : 0,
            sp == null ? 0 : sp.getInt("look_preset", 0),
            sp == null ? 100 : sp.getInt("global_scale", 100),
            sp == null ? 0 : sp.getInt("icon_gap", 0),
            sp == null ? 0 : sp.getInt("pct_dx", 0),
            sp == null ? 0 : sp.getInt("pct_dy", 0),
            sp == null ? 0 : sp.getInt("pct_track", 0),
            sp == null || sp.getBoolean("exit_anim", true) ? 1 : 0,
            sp == null || sp.getBoolean("burn_in", true) ? 1 : 0,
            sp == null ? 60 : sp.getInt("burn_sec", 60),
            sp == null ? 1 : sp.getInt("burn_px", 1),
            sp == null ? 0 : sp.getInt("burn_test", 0),
            sp == null ? 100 : sp.getInt("bolt_scale", 100),
            sp == null ? 86 : sp.getInt("bolt_dx", 86),
            sp == null ? 82 : sp.getInt("bolt_dy", 82),
            sp == null ? 0 : sp.getInt("bolt_double", 0),
            sp != null && sp.getBoolean("wifi_over_sim", false) ? 1 : 0,
            sp == null ? 0 : sp.getInt("color_normal", 0),
            sp == null ? 1 : sp.getInt("bolt_show", 1),
            sp == null ? 100 : sp.getInt("color_alpha", 100),
            sp == null ? 1 : sp.getInt("alpha_follow", 1),
            sp == null ? 100 : sp.getInt("icon_trim", 100),
            sp == null ? 1 : sp.getInt("icon_auto", 1),
            sp == null ? 0 : sp.getInt("color_normal_light", 0),
            sp == null ? 0 : sp.getInt("wifi_dx", 0),
            sp == null ? 0 : sp.getInt("wifi_dy", 0),
            sp == null ? 0 : sp.getInt("tech_dx", 0),
            sp == null ? 0 : sp.getInt("tech_dy", 0),
            sp == null ? 0 : sp.getInt("pill_dx", 0),
            sp == null ? 0 : sp.getInt("pill_dy", 0),
            sp == null ? 0 : sp.getInt("pill_dx_l", 0),
            sp == null ? 0 : sp.getInt("pill_dy_l", 0),
            sp == null ? 0 : sp.getInt("weight_link", 0),
            sp == null ? 0 : sp.getInt("bolt_center", 0),
            sp == null ? 0 : sp.getInt("color_fast", 0),
            sp == null ? 0 : sp.getInt("color_super", 0),
            sp == null ? 0 : sp.getInt("color_wireless", 0),
            sp == null ? 0 : sp.getInt("dots_rainbow", 0),
            sp == null ? 0 : sp.getInt("dot_c1", 0),
            sp == null ? 0 : sp.getInt("dot_c2", 0),
            sp == null ? 0 : sp.getInt("dot_c3", 0),
            sp == null ? 0 : sp.getInt("dot_c4", 0),
            sp == null ? 1 : sp.getInt("qs_takeover", 1),
            sp == null ? 1 : sp.getInt("dots_auto", 1),
            sp == null ? 0 : sp.getInt("dots_dual", 0),
            sp == null ? 0 : sp.getInt("dots_line", 0),
            sp == null ? 0 : sp.getInt("dots_dy", 0),
            sp == null ? 0 : sp.getInt("dot_c5", 0),
            sp == null ? 0 : sp.getInt("dot_c6", 0),
            sp == null ? 0 : sp.getInt("dot_c7", 0),
            sp == null ? 0 : sp.getInt("dot_c8", 0),
            sp == null ? 0 : sp.getInt("show_5ga", 0),
            sp == null ? 100 : sp.getInt("plane_scale", 100),
            sp == null ? 0 : sp.getInt("centre_5ga_scale", 0),
            sp == null ? 0 : sp.getInt("room", 0),
            sp == null ? 1 : sp.getInt("tech_grey", 1),
            sp == null ? 0 : sp.getInt("gap_extra", 0),
            sp == null ? 0 : sp.getInt("capsule_dual", 0),
            sp == null ? 0 : sp.getInt("charge_digit", 0),
            sp == null ? 0 : sp.getInt("capsule_gap", 0),
            sp == null ? 0 : sp.getInt("batt_sys_hide", 0),
            sp == null ? 0 : sp.getInt("capsule_time", 0),
            sp == null ? 0 : sp.getInt("hide_clock", 0)
        };
        cursor.addRow(row);
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
