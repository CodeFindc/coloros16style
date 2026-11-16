package com.mess1on.combinedstatus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Executor;

final class TechLabel {
    private static BroadcastReceiver dataRx;
    private static Executor exec;
    static boolean greyWhenNoData = true;
    private static Handler handler;
    private static Paint paint;
    private static Typeface paintBase;
    private static int paintWeight = -1;
    private static final PerSub[] subs = new PerSub[]{new PerSub(), new PerSub()};
    static boolean show5ga = false;
    static volatile boolean fiveGa = false;
    static volatile String rawNote = "off";
    static volatile boolean dataOff = false;
    static volatile String dataNote = "-";
    static volatile String otherTech = "";

    private static final class PerSub extends TelephonyCallback implements
            TelephonyCallback.DisplayInfoListener,
            TelephonyCallback.ServiceStateListener {
        boolean advanced;
        boolean inService;
        int netType;
        String note = "-";
        int override;
        int subId = -1;
        TelephonyManager tm;

        PerSub() {
        }

        private void refresh(Object infoObj, String prefix) {
            boolean adv = this.override == 5 || TechLabel.is5gaObject(infoObj);
            this.advanced = adv;
            this.note = prefix + "ov=" + this.override + " net=" + this.netType + " 5ga=" + adv;
            TechLabel.recompute();
        }

        String label() {
            if (!this.inService) {
                return "";
            }
            if (this.advanced && TechLabel.show5ga) {
                return "5GA";
            }
            if (this.override == 3 || this.override == 4 || this.override == 5 || this.netType == 20) {
                return "5G";
            }
            return ColorOsPill.techLabel(this.netType);
        }

        @Override
        public void onDisplayInfoChanged(TelephonyDisplayInfo info) {
            if (info == null) {
                return;
            }
            this.override = info.getOverrideNetworkType();
            int nt = info.getNetworkType();
            if (nt != 0) {
                this.netType = nt;
            }
            refresh(info, "di:");
        }

        @Override
        public void onServiceStateChanged(ServiceState ss) {
            if (ss == null) {
                return;
            }
            this.inService = ss.getState() == ServiceState.STATE_IN_SERVICE;
            int dnt = ss.getDataNetworkType();
            if (dnt != 0) {
                this.netType = dnt;
            }
            refresh(ss, "ss:");
        }
    }

    private TechLabel() {
    }

    static String apply(String label) {
        if ("5G".equals(label) && show5ga && fiveGa) {
            return "5GA";
        }
        return label;
    }

    private static void bindSlot(Context context, TelephonyManager baseTm, int slotIdx, int targetSubId) {
        PerSub slot = subs[slotIdx];
        if (slot.subId == targetSubId && slot.tm != null) {
            return;
        }
        unbindSlot(slotIdx);
        if (targetSubId == -1) {
            return;
        }
        try {
            TelephonyManager subTm = baseTm.createForSubscriptionId(targetSubId);
            slot.subId = targetSubId;
            slot.tm = subTm;
            try {
                slot.netType = subTm.getDataNetworkType();
            } catch (Throwable ignored) {
            }
            try {
                ServiceState ss = subTm.getServiceState();
                if (ss != null) {
                    slot.inService = ss.getState() == ServiceState.STATE_IN_SERVICE;
                    if (is5gaObject(ss)) {
                        slot.advanced = true;
                        slot.note = "init-ss:true";
                    }
                }
            } catch (Throwable ignored) {
            }
            subTm.registerTelephonyCallback(exec, slot);
        } catch (Throwable t) {
            slot.note = "err:" + t.getClass().getSimpleName();
        }
    }

    static boolean draw(Canvas canvas, float left, float top, float size, IconState state, int normalColor, int weightPercent, float alphaFactor) {
        if (size <= 0.0f || alphaFactor <= 0.0f || state == null || state.airplane) {
            return false;
        }
        if (state.wifiEnabled && (state.wifiConnected || Style.wifiOverSim)) {
            return false;
        }
        String label = apply(state.centreTechnology());
        if (label == null || label.isEmpty() || label.length() < 3) {
            return false;
        }
        float scale = size / 120.0f;
        int a = Math.max(0, Math.min(255, Math.round(255.0f * alphaFactor)));
        int ink = ink((normalColor & 0x00FFFFFF) | (a << 24));
        drawNow(canvas, left, top, scale, label, ink, weightPercent);
        return true;
    }

    static void drawNow(Canvas canvas, float left, float top, float scale, String label, int color, int weightPercent) {
        if (paint == null) {
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setStyle(Paint.Style.FILL);
            paintBase = Typeface.create("sans-serif-condensed", Typeface.BOLD);
        }
        int fw = DisplayPolicy.textWeight(DisplayPolicy.weight(weightPercent));
        if (fw != paintWeight) {
            paintWeight = fw;
            paint.setTypeface(Typeface.create(paintBase, fw, false));
        }
        float mul = "5GA".equals(label) ? Style.centre5gaMul() : 1.0f;
        paint.setTextSize(31.0f * mul);
        paint.setColor(color);
        int save = canvas.save();
        canvas.translate(left, top);
        canvas.scale(scale, scale);
        float dx = Style.shiftX(false, scale);
        float dy = Style.shiftY(false, scale);
        if (dx != 0.0f || dy != 0.0f) {
            canvas.translate(dx, dy);
        }
        Paint.FontMetrics fm = paint.getFontMetrics();
        canvas.drawText(label, 60.0f, 60.0f - ((fm.ascent + fm.descent) * 0.5f), paint);
        canvas.restoreToCount(save);
    }

    static int ink(int color) {
        if (!greyWhenNoData || !dataOff) {
            return color;
        }
        int a = Math.round(((color >>> 24) & 0xFF) * 0.38f);
        return (color & 0x00FFFFFF) | (a << 24);
    }

    static boolean is5gaObject(Object obj) {
        if (obj == null) {
            return false;
        }
        String s = String.valueOf(obj);
        if (s.contains("5G_ADVANCED") || s.contains("5GA") || s.contains("NR_ADVANCED")
            || s.contains("5G-A") || s.contains("nrAdvanced=true") || s.contains("isNrAdvanced=true")
            || s.contains("5g_advanced")) {
            return true;
        }
        Class<?> cls = obj.getClass();
        String[] methods = new String[]{"isNrAdvanced", "is5gAdvanced", "getNrAdvanced", "get5gAdvancedCapable"};
        for (String mName : methods) {
            try {
                Method m = cls.getMethod(mName);
                Object r = m.invoke(obj);
                if (r instanceof Boolean && (Boolean) r) return true;
                if (r instanceof Integer && ((Integer) r) > 0) return true;
            } catch (Throwable ignored) {
            }
        }
        String[] fields = new String[]{"mIsNrAdvanced", "isNrAdvanced", "mNrAdvanced", "m5gAdvanced"};
        for (String fName : fields) {
            try {
                Field f = cls.getDeclaredField(fName);
                f.setAccessible(true);
                Object r = f.get(obj);
                if (r instanceof Boolean && (Boolean) r) return true;
                if (r instanceof Integer && ((Integer) r) > 0) return true;
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    static void listen(final Context context) {
        if (context == null) {
            return;
        }
        try {
            final TelephonyManager tm = (TelephonyManager) context.getSystemService("phone");
            if (tm == null) {
                return;
            }
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
                exec = new Executor() {
                    @Override
                    public void execute(Runnable r) {
                        handler.post(r);
                    }
                };
            }
            int dataSub = SubscriptionManager.getDefaultDataSubscriptionId();
            int otherSub = -1;
            try {
                SubscriptionManager sm = (SubscriptionManager) context.getSystemService(SubscriptionManager.class);
                List<SubscriptionInfo> list = sm == null ? null : sm.getActiveSubscriptionInfoList();
                if (list != null) {
                    for (SubscriptionInfo si : list) {
                        int id = si.getSubscriptionId();
                        if (dataSub == -1) {
                            dataSub = id;
                        } else if (id != dataSub && otherSub == -1) {
                            otherSub = id;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
            bindSlot(context, tm, 0, dataSub);
            bindSlot(context, tm, 1, otherSub);
            refreshDataEnabled(tm, dataSub);
            recompute();
            if (dataRx == null) {
                dataRx = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context c, Intent i) {
                        listen(context);
                        Style.refreshAll();
                    }
                };
                IntentFilter f = new IntentFilter();
                f.addAction("android.intent.action.ANY_DATA_STATE");
                f.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
                f.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                context.registerReceiver(dataRx, f, Context.RECEIVER_EXPORTED);
            }
        } catch (Throwable t) {
            rawNote = "err:" + t.getClass().getSimpleName();
        }
    }

    static float neededGap(float centreScale, float weightPercent) {
        float mul = (centreScale / 100.0f) * Style.centre5gaMul();
        float halfW = 31.0f * mul * 1.05f;
        float strokeW = (DisplayPolicy.weight((int) weightPercent) * 10.5f) / 100.0f;
        return Math.min(58.0f, Math.max(24.0f, ((halfW + (strokeW * 0.5f)) * 0.72f) + 6.0f));
    }

    static void recompute() {
        boolean adv0 = subs[0].advanced;
        fiveGa = adv0;
        otherTech = subs[1].label();
        String n = subs[0].note + (subs[1].subId != -1 ? (" | s2:" + subs[1].note + "→" + otherTech) : "");
        boolean changed = !n.equals(rawNote);
        rawNote = n;
        if (changed) {
            Style.refreshAll();
        }
    }

    private static void refreshDataEnabled(TelephonyManager baseTm, int subId) {
        try {
            TelephonyManager tm = subId != -1 ? baseTm.createForSubscriptionId(subId) : baseTm;
            boolean en = tm.isDataEnabled();
            dataOff = !en;
            dataNote = en ? "on" : "off";
        } catch (Throwable t) {
            dataOff = false;
            dataNote = "err:" + t.getClass().getSimpleName();
        }
    }

    static void stop() {
        unbindSlot(0);
        unbindSlot(1);
    }

    private static void unbindSlot(int idx) {
        PerSub slot = subs[idx];
        if (slot.tm != null) {
            try {
                slot.tm.unregisterTelephonyCallback(slot);
            } catch (Throwable ignored) {
            }
        }
        slot.tm = null;
        slot.subId = -1;
        slot.advanced = false;
        slot.inService = false;
        slot.netType = 0;
        slot.override = 0;
    }
}
