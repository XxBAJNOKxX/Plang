package hu.ppke.itk.plang.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

/**
 * Sorszámozó sáv a szerkesztő bal oldalán – a asztali editor.LineNumberGutter
 * portja. Az aktuális sor kiemelten jelenik meg, a hibás soroknál és a
 * töréspontoknál jelölő látszik; koppintással a töréspont kapcsolható.
 */
public class GutterView extends View {

    private static final int MIN_DIGITS = 2;

    private final CodeEditorView editor;
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arrow = new Path();

    public GutterView(Context ctx, CodeEditorView editor) {
        super(ctx);
        this.editor = editor;
        textPaint.setTypeface(Typeface.MONOSPACE);
        textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                AppPrefs.getFontSize(), ctx.getResources().getDisplayMetrics()));
    }

    public void syncFont() {
        String family = AppPrefs.getFontFamily();
        int size = AppPrefs.getFontSize();
        textPaint.setTypeface(Typeface.create(family, Typeface.NORMAL));
        textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                size, getResources().getDisplayMetrics()));
        requestLayout();
        invalidate();
    }

    public void applyTheme() {
        invalidate();
    }

    private int preferredWidth() {
        int digits = Math.max(MIN_DIGITS, String.valueOf(Math.max(1, editor.lineCount())).length());
        float w = textPaint.measureText("0") * digits;
        return (int) w + FlatButton.dp(getContext(), 22);
    }

    /** Az adott y koordinátához tartozó sor (0-alapú), vagy -1. */
    private int lineAtY(float y) {
        android.text.Layout l = editor.getLayout();
        if (l == null) {
            return -1;
        }
        float ey = y + editor.getScrollY() - editor.getPaddingTop();
        int line = l.getLineForVertical((int) ey);
        if (line < 0 || line >= l.getLineCount()) {
            return -1;
        }
        return line;
    }

    @Override
    protected void onDraw(Canvas g) {
        Theme.Palette p = Theme.p();
        g.drawColor(p.editorBg);

        android.text.Layout l = editor.getLayout();
        if (l == null) {
            return;
        }
        /* A szerkesztő a szöveget paddingTop eltolással rajzolja, és saját
           scrollY-val görget: a sorszámok ugyanarra a bazisvonalra kerülnek,
           mint a kód (getLineBaseline), így pontosan egymás mellett vannak. */
        final int padT = editor.getPaddingTop();
        final int scroll = editor.getScrollY();
        final int w = getWidth();
        final float textSize = textPaint.getTextSize();

        for (int i = 0; i < l.getLineCount(); i++) {
            int base = l.getLineBaseline(i) + padT - scroll;
            if (base - textSize * 1.4f > getHeight() || base < -textSize * 0.4f) {
                continue;
            }
            int caretLine = editor.caretLine() - 1;
            boolean isCaret = (i == caretLine);
            textPaint.setColor(isCaret ? p.gutterActiveFg : p.gutterFg);
            String num = String.valueOf(i + 1);
            float tw = textPaint.measureText(num);
            g.drawText(num, w - FlatButton.dp(getContext(), 16) - tw, base, textPaint);

            float cy = base - textSize * 0.35f;

            // töréspont pötty
            if (editor.isBreakpoint(i)) {
                dotPaint.setColor(p.error);
                g.drawCircle(FlatButton.dpf(getContext(), 8), cy,
                             FlatButton.dpf(getContext(), 3.2f), dotPaint);
            }
            // hibajelzés
            if (editor.isErrorLine(i)) {
                dotPaint.setColor(p.error);
                g.drawCircle(FlatButton.dpf(getContext(), 8), cy,
                             FlatButton.dpf(getContext(), 3.2f), dotPaint);
            }
            // a futás aktuális sora
            if (i == editor.getRunningLine()) {
                arrowPaint.setColor(p.warning);
                arrow.reset();
                float ax = FlatButton.dpf(getContext(), 3);
                float a = FlatButton.dpf(getContext(), 4.5f);
                arrow.moveTo(ax, cy - a);
                arrow.lineTo(ax + a * 1.6f, cy);
                arrow.lineTo(ax, cy + a);
                arrow.close();
                g.drawPath(arrow, arrowPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            int line = lineAtY(ev.getY());
            if (line >= 0) {
                editor.toggleBreakpoint(line);
                return true;
            }
        }
        return true;
    }

    @Override
    protected void onMeasure(int wspec, int hspec) {
        setMeasuredDimension(preferredWidth(), getDefaultSize(getSuggestedMinimumHeight(), hspec));
    }
}
