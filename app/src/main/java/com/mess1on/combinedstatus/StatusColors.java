package com.mess1on.combinedstatus;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

public final class StatusColors {
    static int CHARGING = -13315748; // #FF34C759
    static int FAST = 0;
    static int LOW = -278483; // #FFFF3B30
    static int PERFORMANCE = -15960321; // #FF0C7AFF
    static int SAVER = -14080; // #FFFFC900
    static int SUPER = 0;
    static int WIRELESS = 0;
    static int alpha = 100;
    static boolean chargeDigits = false;
    static int dotColour0 = -12746241;
    static int dotColour1 = -10256129;
    static int dotColour2 = -6001409;
    static int dotColour3 = -268051;
    static int dotColour4 = -12746241;
    static int dotColour5 = -10256129;
    static int dotColour6 = -6001409;
    static int dotColour7 = -268051;
    static boolean dotRainbow = false;
    static boolean followIconAlpha = true;
    static boolean iconAuto = true;
    static int iconTrim = 100;
    static int normal = 0;
    static int normalLight = 0;

    private StatusColors() {
    }

    static int chargingPick() {
        if (Bolt.chargingNow()) {
            int cls = Bolt.chargeClass();
            if ((cls == Bolt.CLASS_FAST || cls == Bolt.CLASS_SUPER) && FAST != 0) {
                return FAST;
            }
        }
        return CHARGING != 0 ? CHARGING : -13315748;
    }

    static int digitInk(IconState state, boolean colourEnabled, int normalTint, int threshold, boolean powerSave, boolean perfMode) {
        if (chargeDigits && colourEnabled && state != null && state.charging && !Bolt.chargedFull(state.battery) && !powerSave && !perfMode) {
            return fadeColour(chargingPick(), opacity());
        }
        return normalFor(normalTint);
    }

    static int dotColour(int index) {
        switch (index) {
            case 0: return dotColour0;
            case 1: return dotColour1;
            case 2: return dotColour2;
            case 3: return dotColour3;
            case 4: return dotColour4;
            case 5: return dotColour5;
            case 6: return dotColour6;
            case 7: return dotColour7;
            default: return dotColour0;
        }
    }

    static float fade(int percent) {
        if (percent <= 10) {
            return 0.1f;
        }
        if (percent >= 100) {
            return 1.0f;
        }
        return percent / 100.0f;
    }

    static int fadeColour(int color, int percent) {
        if (percent >= 100) {
            return color;
        }
        int a = Math.round(Color.alpha(color) * fade(percent));
        if (a < 0) a = 0;
        if (a > 255) a = 255;
        return (color & 0x00FFFFFF) | (a << 24);
    }

    static boolean follows() {
        return followIconAlpha || iconAuto;
    }

    public static String hex(int color) {
        return String.format("#%06X", color & 0x00FFFFFF);
    }

    static int lowBatteryThreshold(int threshold) {
        if (threshold < 5) return 5;
        if (threshold > 50) return 50;
        return threshold;
    }

    static int normalFor(int systemTint) {
        return fadeColour(rawNormal(systemTint), opacity());
    }

    static int opacity() {
        if (follows()) {
            return iconTrim;
        }
        return (alpha * iconTrim) / 100;
    }

    static int opaque(int color) {
        if (color == 0) {
            return 0;
        }
        return color | 0xFF000000;
    }

    static void paintDigits(View pillView, IconState state, boolean colourEnabled, int normalTint, int threshold, boolean powerSave, boolean perfMode) {
        if (!(pillView instanceof android.view.ViewGroup)) {
            return;
        }
        android.view.ViewGroup vg = (android.view.ViewGroup) pillView;
        int ink = digitInk(state, colourEnabled, normalTint, threshold, powerSave, perfMode);
        for (int i = 0; i < vg.getChildCount(); i++) {
            View child = vg.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(ink);
            }
        }
    }

    public static int parse(String str) {
        if (str == null) {
            return 0;
        }
        String s = str.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.length() == 6) {
            try {
                return Integer.parseInt(s, 16) | 0xFF000000;
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        if (s.length() == 8) {
            try {
                return (int) Long.parseLong(s, 16);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static int rawNormal(int systemTint) {
        if (normal == 0 && normalLight == 0) {
            return systemTint;
        }
        int r = (systemTint >> 16) & 0xFF;
        int g = (systemTint >> 8) & 0xFF;
        int b = systemTint & 0xFF;
        boolean sysIsDark = (((r * 299) + (g * 587)) + (b * 114)) < 128000;
        if (sysIsDark) {
            return normalLight != 0 ? normalLight : normal;
        }
        return normal != 0 ? normal : normalLight;
    }

    public static int resolve(int color, int fallback) {
        return color != 0 ? opaque(color) : opaque(fallback);
    }

    static void setDotColour(int index, int color, int fallback) {
        int c = color != 0 ? opaque(color) : fallback;
        switch (index) {
            case 0: dotColour0 = c; break;
            case 1: dotColour1 = c; break;
            case 2: dotColour2 = c; break;
            case 3: dotColour3 = c; break;
            case 4: dotColour4 = c; break;
            case 5: dotColour5 = c; break;
            case 6: dotColour6 = c; break;
            case 7: dotColour7 = c; break;
        }
    }

    static int tint(int level, boolean charging, boolean colourEnabled, int normalTint, int threshold, boolean powerSave, boolean perfMode) {
        return fadeColour(tintRaw(level, charging, colourEnabled, normalTint, threshold, powerSave, perfMode), opacity());
    }

    private static int tintRaw(int level, boolean charging, boolean colourEnabled, int normalTint, int threshold, boolean powerSave, boolean perfMode) {
        if (!colourEnabled) {
            return rawNormal(normalTint);
        }
        if (powerSave) {
            return SAVER;
        }
        if (perfMode) {
            return PERFORMANCE;
        }
        if (charging && !Bolt.chargedFull(level)) {
            return chargingPick();
        }
        if (level >= 0 && level <= lowBatteryThreshold(threshold)) {
            return LOW;
        }
        return rawNormal(normalTint);
    }
}
