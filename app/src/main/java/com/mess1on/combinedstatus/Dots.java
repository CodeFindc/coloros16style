package com.mess1on.combinedstatus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.telephony.CellSignalStrength;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import java.util.List;
import java.util.concurrent.Executor;

final class Dots {
    private static final float[] LINE_X = new float[]{34.0f, 51.333332f, 68.666664f, 86.0f};
    private static final float LINE_Y = 109.0f;
    private static final float ROW2_DY = 11.0f;
    static boolean auto = true;
    static boolean dual = false;
    static int dy = 0;
    private static Executor exec;
    private static Handler handler;
    static boolean line = false;
    static boolean manualDual = false;
    static volatile boolean otherAlive = true;
    static volatile int otherLevel = 3;
    private static int otherSub = -1;
    private static TelephonyManager otherTm;
    static String probe = "uninit";
    private static BroadcastReceiver rx;
    static volatile int sims = 1;
    private static SubCb subCb;

    private static final class SubCb extends TelephonyCallback implements
            TelephonyCallback.SignalStrengthsListener,
            TelephonyCallback.ServiceStateListener {
        SubCb() {
        }

        @Override
        public void onServiceStateChanged(ServiceState ss) {
            boolean alive = ss != null && ss.getState() == ServiceState.STATE_IN_SERVICE;
            boolean changed = alive != Dots.otherAlive;
            Dots.otherAlive = alive;
            if (changed) {
                Style.dotsChanged();
            }
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength ss) {
            int lvl = ss == null ? 0 : ss.getLevel();
            if (lvl <= 0 && ss != null) {
                List<CellSignalStrength> list = ss.getCellSignalStrengths();
                for (CellSignalStrength c : list) {
                    lvl = Math.max(lvl, c.getLevel());
                }
            }
            int clamped = Math.max(0, Math.min(4, lvl));
            boolean changed = clamped != Dots.otherLevel;
            Dots.otherLevel = clamped;
            if (changed) {
                Style.dotsChanged();
            }
        }
    }

    private Dots() {
    }

    private static int alpha(int color, float factor) {
        int a = Math.max(0, Math.min(255, Math.round(255.0f * factor)));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    static void applyLayout() {
        for (int i = 0; i < 4; i++) {
            float y = (line ? LINE_Y : row1BaseY(i)) + (dy * 1.5f);
            ColorOsPill.Pill.DOT_Y[i] = y;
            IconPainter.YS[i] = y;
            float x = line ? LINE_X[i] : (i == 0 ? 34.0f : (i == 1 ? 51.0f : (i == 2 ? 69.0f : 86.0f)));
            ColorOsPill.Pill.DOT_X[i] = x;
            IconPainter.XS[i] = x;
        }
    }

    static boolean applyMode() {
        boolean want = auto ? (sims >= 2) : manualDual;
        boolean changed = want != dual;
        dual = want;
        return changed;
    }

    static boolean armed() {
        return dual || line || dy != 0 || StatusColors.dotRainbow;
    }

    static String colourStat() {
        return "rainbow=" + StatusColors.dotRainbow + " c1=#" + Integer.toHexString(StatusColors.dotColour0)
            + " c4=#" + Integer.toHexString(StatusColors.dotColour3);
    }

    static String now() {
        return "auto=" + auto + " dual=" + dual + "(manual=" + manualDual + " sims=" + sims + ")"
            + " line=" + line + " dy=" + dy + " other=" + (otherAlive ? ("lvl" + otherLevel) : "off")
            + " probe=" + probe;
    }

    static void paint(Canvas canvas, Paint fillPaint, IconState state, int activeInk, int dimInk, float radius, float alphaFactor, float scale) {
        int lvl1 = (!state.airplane && state.simPresent && state.inService) ? Math.max(0, Math.min(4, state.mobileLevel)) : 0;
        float r = radius * (Style.dotsScale() / 100.0f);
        float shiftY = shift(scale);
        float sx = shiftX(scale);
        for (int i = 0; i < 4; i++) {
            boolean lit = i < lvl1;
            int col = lit ? activeInk : dimInk;
            if (StatusColors.dotRainbow) {
                int rc = StatusColors.dotColour(i);
                col = lit ? alpha(rc, alphaFactor) : alpha(rc, 0.28f * alphaFactor);
            }
            fillPaint.setColor(col);
            float cx = (line ? LINE_X[i] : IconPainter.XS[i]) + sx;
            float cy = row1Design(i) + shiftY;
            canvas.drawCircle(cx, cy, r, fillPaint);
        }
        if (dual) {
            int lvl2 = (!state.airplane && otherAlive) ? Math.max(0, Math.min(4, otherLevel)) : 0;
            for (int i = 0; i < 4; i++) {
                boolean lit = i < lvl2;
                int col = lit ? activeInk : dimInk;
                if (StatusColors.dotRainbow) {
                    int rc = StatusColors.dotColour(4 + i);
                    col = lit ? alpha(rc, alphaFactor) : alpha(rc, 0.28f * alphaFactor);
                }
                fillPaint.setColor(col);
                float cx = (line ? LINE_X[i] : IconPainter.XS[i]) + sx;
                float cy = row1Design(i) + shiftY + ROW2_DY;
                canvas.drawCircle(cx, cy, r * 0.88f, fillPaint);
            }
        }
    }

    static float pxPerUnit(float scale) {
        return scale <= 0.0f ? 1.5f : (1.5f / scale);
    }

    static void refresh(Context context) {
        if (context == null) return;
        try {
            SubscriptionManager sm = (SubscriptionManager) context.getSystemService(SubscriptionManager.class);
            TelephonyManager tm = (TelephonyManager) context.getSystemService("phone");
            List<SubscriptionInfo> list = sm == null ? null : sm.getActiveSubscriptionInfoList();
            int count = list == null ? 0 : list.size();
            int dataSub = SubscriptionManager.getDefaultDataSubscriptionId();
            int secSub = -1;
            if (list != null) {
                for (SubscriptionInfo si : list) {
                    int id = si.getSubscriptionId();
                    if (id != dataSub) {
                        secSub = id;
                        break;
                    }
                }
            }
            sims = Math.max(1, count);
            applyMode();
            if (secSub != otherSub) {
                unbind();
                otherSub = secSub;
                if (secSub != -1 && tm != null) {
                    otherTm = tm.createForSubscriptionId(secSub);
                    subCb = new SubCb();
                    otherTm.registerTelephonyCallback(exec, subCb);
                }
            }
            probe = "sims=" + sims + " data=" + dataSub + " other=" + otherSub;
        } catch (Throwable t) {
            probe = "err:" + t.getClass().getSimpleName();
        }
    }

    private static float row1BaseY(int index) {
        return (index == 0 || index == 3) ? 102.0f : 109.0f;
    }

    static float row1Design(int index) {
        return line ? LINE_Y : row1BaseY(index);
    }

    static float shift(float scale) {
        return dy * pxPerUnit(scale);
    }

    static float shiftX(float scale) {
        return 0.0f;
    }

    static void start(final Context context) {
        if (context == null) return;
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
            exec = new Executor() {
                @Override
                public void execute(Runnable r) {
                    handler.post(r);
                }
            };
        }
        refresh(context);
        if (rx == null) {
            rx = new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent i) {
                    Dots.refresh(context);
                    Style.dotsChanged();
                }
            };
            IntentFilter f = new IntentFilter();
            f.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
            f.addAction("android.intent.action.SIM_STATE_CHANGED");
            context.registerReceiver(rx, f, Context.RECEIVER_EXPORTED);
        }
    }

    static boolean twoCards() {
        return auto ? (sims >= 2) : manualDual;
    }

    private static void unbind() {
        if (otherTm != null && subCb != null) {
            try {
                otherTm.unregisterTelephonyCallback(subCb);
            } catch (Throwable ignored) {
            }
        }
        otherTm = null;
        subCb = null;
        otherSub = -1;
    }
}
