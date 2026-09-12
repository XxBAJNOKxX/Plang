package hu.ppke.itk.plang.android;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * A vektoros ikonkészlet átvitele az Android Canvas-ra.
 * Minden ikon 16 egységes rácson rajzolódik, tetszőleges méretben éles,
 * és felveszi a téma színét – az asztali {@code widgets.VSIcons}-szal
 * megegyező rajzokkal.
 */
public final class VsIcons {

    public static final int FILES = 0;
    public static final int RUN = 1;
    public static final int SEARCH = 2;
    public static final int SETTINGS = 3;
    public static final int PLAY = 4;
    public static final int STOP = 5;
    public static final int PARSE = 6;
    public static final int EDIT = 7;
    public static final int COPY = 8;
    public static final int SAVE = 9;
    public static final int OPEN = 10;
    public static final int NEW = 11;
    public static final int STEP_INTO = 12;
    public static final int STEP_OUT = 13;
    public static final int CLOSE = 14;
    public static final int CHEVRON_DOWN = 15;
    public static final int CHEVRON_RIGHT = 16;
    public static final int THEME = 17;
    public static final int TERMINAL = 18;
    public static final int VARIABLES = 19;
    public static final int TREE = 20;
    public static final int ARROW_UP = 21;
    public static final int ARROW_DOWN = 22;
    public static final int ERROR = 23;
    public static final int CHECK = 24;
    public static final int INPUT = 25;
    public static final int OUTPUT = 26;
    public static final int CALLSTACK = 27;
    public static final int HELP = 28;

    private VsIcons() {}

    /** Egyszerű Drawable, amely egy ikont rajzol a megadott színnel. */
    public static Drawable icon(final int type, final int size, final int color) {
        return new Drawable() {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override
            public int getIntrinsicWidth() { return size; }

            @Override
            public int getIntrinsicHeight() { return size; }

            @Override
            public void draw(Canvas canvas) {
                android.graphics.Rect b = getBounds();
                float sc = Math.min(b.width(), b.height()) / 16f;
                canvas.save();
                canvas.translate(b.left, b.top);
                canvas.scale(sc, sc);
                paint.setColor(color);
                VsIcons.draw(canvas, paint, type);
                canvas.restore();
            }

            @Override
            public void setAlpha(int alpha) { paint.setAlpha(alpha); }

            @Override
            public void setColorFilter(ColorFilter cf) { paint.setColorFilter(cf); }

            @Override
            public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
        };
    }

    private static Path tri(float x1, float y1, float x2, float y2, float x3, float y3) {
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        p.lineTo(x3, y3);
        p.close();
        return p;
    }

    private static void line(Canvas g, Paint p, float x1, float y1, float x2, float y2) {
        g.drawLine(x1, y1, x2, y2, p);
    }

    private static void rect(Canvas g, Paint p, float x, float y, float w, float h, boolean fill) {
        if (fill) {
            g.drawRect(x, y, x + w, y + h, p);
        } else {
            Paint s = new Paint(p);
            s.setStyle(Paint.Style.STROKE);
            s.setStrokeWidth(1.2f);
            g.drawRect(x, y, x + w, y + h, s);
        }
    }

    private static void oval(Canvas g, Paint p, float x, float y, float w, float h, boolean fill) {
        Paint s = new Paint(p);
        if (fill) {
            s.setStyle(Paint.Style.FILL);
        } else {
            s.setStyle(Paint.Style.STROKE);
            s.setStrokeWidth(1.2f);
        }
        g.drawOval(new RectF(x, y, x + w, y + h), s);
    }

    private static void draw(Canvas g, Paint base, int type) {
        Paint p = new Paint(base);
        p.setStyle(Paint.Style.FILL);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        switch (type) {
            case FILES: {
                Paint s = stroke(p, 1.35f);
                g.drawRect(2, 2, 10, 12, s);
                line(g, s, 5, 5, 8, 5);
                line(g, s, 5, 7, 8, 7);
                s = stroke(p, 1.2f);
                g.drawRect(5, 4, 13, 14, s);
                break;
            }
            case RUN: {
                Paint s = stroke(p, 1.35f);
                Path t = tri(3, 2, 12, 8, 3, 14);
                s.setStyle(Paint.Style.STROKE);
                g.drawPath(t, s);
                break;
            }
            case SEARCH: {
                Paint s = stroke(p, 1.35f);
                oval(g, s, 2.5f, 2.5f, 8, 8, false);
                line(g, s, 10, 10, 14, 14);
                break;
            }
            case SETTINGS: {
                Paint s = stroke(p, 1.2f);
                Path gear = gearPath(8, 8, 8, 4.6f, 6.4f, 0.42f);
                g.drawPath(gear, s);
                oval(g, s, 6.0f, 6.0f, 4.0f, 4.0f, false);
                break;
            }
            case PLAY:
                g.drawPath(tri(4, 2.5f, 13, 8, 4, 13.5f), p);
                break;
            case STOP:
                g.drawRect(4, 4, 12, 12, p);
                break;
            case PARSE: {
                Paint s = stroke(p, 1.35f);
                g.drawRect(3, 2, 11, 14, s);
                line(g, s, 5, 5, 9, 5);
                line(g, s, 5, 7, 9, 7);
                s = stroke(p, 1.8f);
                line(g, s, 7, 11, 9, 13);
                line(g, s, 9, 13, 14, 7);
                break;
            }
            case EDIT: {
                Paint s = stroke(p, 1.35f);
                line(g, s, 2, 14, 4, 13);
                Path q = new Path();
                q.moveTo(3.2f, 12.2f);
                q.lineTo(11, 2.5f);
                q.lineTo(13.5f, 5);
                q.lineTo(5.5f, 13.8f);
                q.close();
                s.setStyle(Paint.Style.STROKE);
                g.drawPath(q, s);
                line(g, s, 10, 4, 12, 6);
                break;
            }
            case COPY: {
                Paint s = stroke(p, 1.35f);
                g.drawRect(2, 2, 10, 10, s);
                g.drawRect(6, 6, 14, 14, s);
                break;
            }
            case SAVE: {
                Paint s = stroke(p, 1.35f);
                g.drawRect(2, 2, 14, 14, s);
                g.drawRect(5, 2, 11, 6, s);
                g.drawRect(4, 9, 12, 14, s);
                break;
            }
            case OPEN: {
                Paint s = stroke(p, 1.35f);
                line(g, s, 2, 5, 2, 13);
                line(g, s, 2, 13, 14, 13);
                line(g, s, 2, 5, 6, 5);
                line(g, s, 6, 5, 7, 7);
                line(g, s, 7, 7, 13, 7);
                line(g, s, 13, 7, 14, 13);
                break;
            }
            case NEW: {
                Paint s = stroke(p, 1.35f);
                g.drawRect(3, 2, 12, 14, s);
                line(g, s, 7, 6, 7, 10);
                line(g, s, 5, 8, 9, 8);
                break;
            }
            case STEP_INTO: {
                Paint s = stroke(p, 1.35f);
                line(g, s, 8, 2, 8, 9);
                g.drawPath(tri(5, 8, 11, 8, 8, 12), p);
                oval(g, s, 6, 12.5f, 4, 3, false);
                break;
            }
            case STEP_OUT: {
                Paint s = stroke(p, 1.35f);
                line(g, s, 8, 12, 8, 5);
                g.drawPath(tri(5, 6, 11, 6, 8, 2), p);
                oval(g, s, 6, 12.5f, 4, 3, false);
                break;
            }
            case CLOSE: {
                Paint s = stroke(p, 1.4f);
                line(g, s, 4, 4, 12, 12);
                line(g, s, 12, 4, 4, 12);
                break;
            }
            case CHEVRON_DOWN: {
                Paint s = stroke(p, 1.5f);
                line(g, s, 4, 6, 8, 10);
                line(g, s, 8, 10, 12, 6);
                break;
            }
            case CHEVRON_RIGHT: {
                Paint s = stroke(p, 1.5f);
                line(g, s, 6, 4, 10, 8);
                line(g, s, 10, 8, 6, 12);
                break;
            }
            case THEME: {
                Paint s = stroke(p, 1.2f);
                oval(g, s, 3, 3, 10, 10, false);
                Path half = new Path();
                half.addCircle(8, 8, 5, Path.Direction.CW);
                half.addRect(8, 3, 13, 13, Path.Direction.CW);
                p.setStyle(Paint.Style.FILL);
                canvasHalfFill(g, p, 8, 8, 5);
                break;
            }
            case TERMINAL: {
                Paint s = stroke(p, 1.35f);
                g.drawRect(1, 2, 14, 13, s);
                s = stroke(p, 1.4f);
                line(g, s, 4, 6, 6, 8);
                line(g, s, 6, 8, 4, 10);
                line(g, s, 8, 10, 12, 10);
                break;
            }
            case VARIABLES: {
                Paint s = stroke(p, 1.3f);
                openQuad(g, s, 6, 2, 4, 4, 4, 8, 2, 8);
                openQuad(g, s, 6, 14, 4, 12, 4, 8, 2, 8);
                openQuad(g, s, 10, 2, 12, 4, 12, 8, 14, 8);
                openQuad(g, s, 10, 14, 12, 12, 12, 8, 14, 8);
                break;
            }
            case TREE: {
                Paint s = stroke(p, 1.2f);
                line(g, s, 3, 3, 3, 13);
                line(g, s, 3, 6, 7, 6);
                line(g, s, 3, 10, 7, 10);
                line(g, s, 3, 13, 7, 13);
                g.drawRect(7, 4, 13, 7, p);
                g.drawRect(7, 8, 13, 11, stroke(p, 1.2f));
                g.drawRect(7, 12, 13, 15, stroke(p, 1.2f));
                break;
            }
            case ARROW_UP: {
                Paint s = stroke(p, 1.5f);
                line(g, s, 8, 12, 8, 4);
                line(g, s, 4, 8, 8, 4);
                line(g, s, 12, 8, 8, 4);
                break;
            }
            case ARROW_DOWN: {
                Paint s = stroke(p, 1.5f);
                line(g, s, 8, 4, 8, 12);
                line(g, s, 4, 8, 8, 12);
                line(g, s, 12, 8, 8, 12);
                break;
            }
            case ERROR: {
                Paint s = stroke(p, 1.2f);
                oval(g, s, 2, 2, 12, 12, false);
                s = stroke(p, 1.6f);
                line(g, s, 6, 6, 10, 10);
                line(g, s, 10, 6, 6, 10);
                break;
            }
            case CHECK: {
                Paint s = stroke(p, 1.8f);
                line(g, s, 3, 8, 6, 12);
                line(g, s, 6, 12, 13, 4);
                break;
            }
            case INPUT: {
                Paint s = stroke(p, 1.2f);
                g.drawRect(2, 3, 14, 13, s);
                s = stroke(p, 1.5f);
                line(g, s, 5, 8, 11, 8);
                line(g, s, 8, 5, 11, 8);
                line(g, s, 8, 11, 11, 8);
                break;
            }
            case OUTPUT: {
                Paint s = stroke(p, 1.2f);
                g.drawRect(2, 3, 14, 13, s);
                s = stroke(p, 1.5f);
                line(g, s, 5, 6, 11, 6);
                line(g, s, 5, 9, 9, 9);
                break;
            }
            case CALLSTACK: {
                Paint s = stroke(p, 1.2f);
                g.drawRect(2, 3, 14, 6, s);
                g.drawRect(2, 7, 14, 10, s);
                g.drawRect(2, 11, 14, 14, s);
                break;
            }
            case HELP: {
                Paint s = stroke(p, 1.2f);
                oval(g, s, 2, 2, 12, 12, false);
                s = stroke(p, 1.4f);
                openQuad(g, s, 6, 6, 6, 4, 10, 4, 10, 6.5f);
                line(g, s, 10, 6, 8, 9);
                line(g, s, 8, 9, 8, 10);
                g.drawCircle(8, 12, 1, p);
                break;
            }
        }
    }

    private static void canvasHalfFill(Canvas g, Paint p, float cx, float cy, float r) {
        // a jobb fele teli kör – a témaikon "félhold" jelzése
        RectF oval = new RectF(cx - r, cy - r, cx + r, cy + r);
        g.drawArc(oval, -90, 180, true, p);
    }

    private static Paint stroke(Paint base, float w) {
        Paint s = new Paint(base);
        s.setStyle(Paint.Style.STROKE);
        s.setStrokeWidth(w);
        return s;
    }

    private static void openQuad(Canvas g, Paint p,
                                 float x1, float y1, float x2, float y2,
                                 float x3, float y3, float x4, float y4) {
        Path path = new Path();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.lineTo(x4, y4);
        g.drawPath(path, p);
    }

    private static Path gearPath(float cx, float cy, float R, float rIn, float rOut, double holeRatio) {
        Path path = new Path();
        int teeth = 8;
        for (int i = 0; i < teeth * 2; i++) {
            double ang = Math.PI * i / teeth;
            double r = (i % 2 == 0) ? rOut : rIn;
            float x = (float) (cx + Math.cos(ang) * r);
            float y = (float) (cy + Math.sin(ang) * r);
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        path.close();
        return path;
    }
}
