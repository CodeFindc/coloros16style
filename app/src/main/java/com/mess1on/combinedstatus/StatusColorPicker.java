package com.mess1on.combinedstatus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.function.IntConsumer;

public final class StatusColorPicker {
    private StatusColorPicker() {
    }

    private static final class Palette extends View {
        private final RectF bar = new RectF();
        private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF box = new RectF();
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float[] hsv = new float[]{120.0f, 0.8f, 0.9f};
        private Shader hueShader;
        private int mode;
        final Runnable onMove;
        private final float pad;
        private Shader valShader;

        Palette(Context context, int initialColor, Runnable onMove) {
            super(context);
            this.onMove = onMove;
            float d = context.getResources().getDisplayMetrics().density;
            this.pad = 12.0f * d;
            this.border.setStyle(Paint.Style.STROKE);
            this.border.setStrokeWidth(2.0f * d);
            setColor(initialColor);
        }

        private void layoutRects() {
            float d = getResources().getDisplayMetrics().density;
            float w = getWidth();
            float h = getHeight();
            float barH = 26.0f * d;
            float gap = 14.0f * d;
            float p = this.pad;
            this.box.set(p, p, w - p, ((h - p) - barH) - gap);
            this.bar.set(p, (h - p) - barH, w - p, h - p);
        }

        int color() {
            return Color.HSVToColor(this.hsv);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            layoutRects();
            float d = getResources().getDisplayMetrics().density;
            float r = 10.0f * d;
            int pureHue = Color.HSVToColor(new float[]{this.hsv[0], 1.0f, 1.0f});
            this.fill.setShader(new LinearGradient(this.box.left, this.box.top, this.box.right, this.box.top, -1, pureHue, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(this.box, r, r, this.fill);
            if (this.valShader != null) {
                this.fill.setShader(this.valShader);
                canvas.drawRoundRect(this.box, r, r, this.fill);
            }
            if (this.hueShader != null) {
                this.fill.setShader(this.hueShader);
                float br = this.bar.height() * 0.5f;
                canvas.drawRoundRect(this.bar, br, br, this.fill);
            }
            this.fill.setShader(null);
            float cx = this.box.left + (this.hsv[1] * this.box.width());
            float cy = this.box.top + ((1.0f - this.hsv[2]) * this.box.height());
            this.border.setColor(-1);
            canvas.drawCircle(cx, cy, 8.0f * d, this.border);
            this.border.setColor(0x80000000);
            canvas.drawCircle(cx, cy, 9.5f * d, this.border);
            float hx = this.bar.left + ((this.hsv[0] / 360.0f) * this.bar.width());
            float hy = this.bar.centerY();
            this.border.setColor(-1);
            canvas.drawCircle(hx, hy, 9.0f * d, this.border);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            layoutRects();
            this.valShader = new LinearGradient(0.0f, this.box.top, 0.0f, this.box.bottom, 0, -16777216, Shader.TileMode.CLAMP);
            this.hueShader = new LinearGradient(this.bar.left, 0.0f, this.bar.right, 0.0f,
                new int[]{-65536, -256, -16711936, -16711681, -16776961, -65281, -65536},
                null, Shader.TileMode.CLAMP);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            float x = event.getX();
            float y = event.getY();
            if (action == MotionEvent.ACTION_DOWN) {
                if (y >= this.bar.top - this.pad) {
                    this.mode = 2;
                } else {
                    this.mode = 1;
                }
            }
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                if (this.mode == 2) {
                    this.hsv[0] = ColorSelection.fraction(x, this.bar.left, getWidth()) * 360.0f;
                } else if (this.mode == 1) {
                    this.hsv[1] = ColorSelection.fraction(x, this.box.left, getWidth());
                    float spanY = this.box.height();
                    float fy = spanY > 0.0f ? (y - this.box.top) / spanY : 0.0f;
                    if (fy < 0.0f) fy = 0.0f;
                    if (fy > 1.0f) fy = 1.0f;
                    this.hsv[2] = 1.0f - fy;
                }
                invalidate();
                this.onMove.run();
                return true;
            }
            this.mode = 0;
            return super.onTouchEvent(event);
        }

        void setColor(int color) {
            Color.colorToHSV(StatusColors.opaque(color), this.hsv);
            invalidate();
        }
    }

    private static int dp(Context context, int v) {
        return Math.round(v * context.getResources().getDisplayMetrics().density);
    }

    public static void show(Activity activity, String title, int currentColor, final int fallbackColor, boolean allowSystemDefault, final IntConsumer onPick) {
        int initColor = StatusColors.resolve(currentColor, fallbackColor);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(activity, 20);
        root.setPadding(pad, dp(activity, 14), pad, dp(activity, 6));

        LinearLayout topRow = new LinearLayout(activity);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        View swatchView = new View(activity);
        final GradientDrawable swatchBg = new GradientDrawable();
        swatchBg.setCornerRadius(dp(activity, 10));
        swatchBg.setColor(initColor);
        swatchBg.setStroke(dp(activity, 1), 0x33888888);
        swatchView.setBackground(swatchBg);
        LinearLayout.LayoutParams swatchLp = new LinearLayout.LayoutParams(dp(activity, 40), dp(activity, 40));
        swatchLp.rightMargin = dp(activity, 12);
        topRow.addView(swatchView, swatchLp);

        final EditText hexInput = new EditText(activity);
        hexInput.setSingleLine(true);
        hexInput.setText(StatusColors.hex(initColor));
        hexInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(9)});
        topRow.addView(hexInput, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        root.addView(topRow);

        final boolean[] updating = new boolean[]{false};
        final Palette[] paletteRef = new Palette[1];
        Palette palette = new Palette(activity, initColor, new Runnable() {
            @Override
            public void run() {
                if (updating[0]) return;
                updating[0] = true;
                int c = paletteRef[0].color();
                swatchBg.setColor(c);
                hexInput.setText(StatusColors.hex(c));
                hexInput.setSelection(hexInput.getText().length());
                updating[0] = false;
            }
        });
        paletteRef[0] = palette;
        LinearLayout.LayoutParams palLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 210));
        palLp.topMargin = dp(activity, 12);
        root.addView(palette, palLp);

        hexInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (updating[0]) return;
                int parsed = StatusColors.parse(s.toString());
                if (parsed != 0) {
                    updating[0] = true;
                    swatchBg.setColor(parsed);
                    paletteRef[0].setColor(parsed);
                    updating[0] = false;
                }
            }
        });

        final AlertDialog[] dlgRef = new AlertDialog[1];
        if (allowSystemDefault) {
            TextView sysBtn = new TextView(activity);
            sysBtn.setText("跟随系统（清除自定色，立即生效）");
            sysBtn.setTextSize(14.0f);
            sysBtn.setTextColor(-16745729);
            sysBtn.setGravity(Gravity.CENTER);
            GradientDrawable sysBg = new GradientDrawable();
            sysBg.setCornerRadius(dp(activity, 10));
            sysBg.setColor(0x18007AFF);
            sysBtn.setBackground(sysBg);
            int vp = dp(activity, 10);
            sysBtn.setPadding(vp, vp, vp, vp);
            LinearLayout.LayoutParams sysLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            sysLp.topMargin = dp(activity, 10);
            sysBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onPick.accept(0);
                    if (dlgRef[0] != null) {
                        dlgRef[0].dismiss();
                    }
                }
            });
            root.addView(sysBtn, sysLp);
        }

        AlertDialog dlg = new AlertDialog.Builder(activity)
            .setTitle(title)
            .setView(root)
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int w) {
                    int parsed = StatusColors.parse(hexInput.getText().toString());
                    if (parsed == 0) {
                        parsed = paletteRef[0].color();
                    }
                    onPick.accept(parsed);
                }
            })
            .setNeutralButton(allowSystemDefault ? "跟随系统" : "默认", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int w) {
                    onPick.accept(0);
                }
            })
            .setNegativeButton("取消", null)
            .create();
        dlgRef[0] = dlg;
        dlg.show();
    }
}
