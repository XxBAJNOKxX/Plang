package hu.ppke.itk.plang.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Állapotsor – a asztali widgets.StatusBar megfelelője: bal oldali futás-/hiba-
 * cellák, jobb oldali információs cellák; egyes cellák kattinthatók.
 */
public class StatusBarView extends View {

    public static final class Cell {
        public final String id;
        public String text = "";
        public int iconType = -1;
        public int iconColor = -1;
        public String tooltip = null;
        public Runnable action = null;
        final boolean rightSide;

        Cell(String id, String text, boolean rightSide) {
            this.id = id;
            this.text = text;
            this.rightSide = rightSide;
        }
    }

    private final List<Cell> cells = new ArrayList<Cell>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public StatusBarView(Context ctx) {
        super(ctx);
    }

    public Cell add(String id, String text, boolean rightSide) {
        Cell c = new Cell(id, text, rightSide);
        cells.add(c);
        invalidate();
        return c;
    }

    public Cell cell(String id) {
        for (Cell c : cells) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return null;
    }

    public void setText(String id, String text) {
        Cell c = cell(id);
        if (c != null && !c.text.equals(text)) {
            c.text = text;
            invalidate();
        }
    }

    public void setIcon(String id, int iconType, Integer color) {
        Cell c = cell(id);
        if (c != null) {
            c.iconType = iconType;
            c.iconColor = color == null ? -1 : color.intValue();
            invalidate();
        }
    }

    public void applyTheme() {
        invalidate();
    }

    private float cellWidth(Cell c, Paint fm) {
        float w = 10;
        if (c.iconType >= 0) {
            w += FlatButton.dp(getContext(), 17);
        }
        if (c.text != null && c.text.length() > 0) {
            w += fm.measureText(c.text) + 8;
        }
        return w;
    }

    @Override
    protected void onDraw(Canvas g) {
        Theme.Palette p = Theme.p();
        g.drawColor(p.statusBg);
        float textSize = FlatButton.dp(getContext(), 11);
        paint.setTextSize(textSize);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));

        // jobb oldali cellák jobbra zárva
        float xr = getWidth();
        for (int i = cells.size() - 1; i >= 0; i--) {
            Cell c = cells.get(i);
            if (!c.rightSide) continue;
            float w = cellWidth(c, paint);
            xr -= w;
            drawCell(g, p, c, xr, w);
        }

        float xl = 0;
        for (Cell c : cells) {
            if (c.rightSide || c.text == null) continue;
            float w = cellWidth(c, paint);
            if (c.text.length() == 0 && c.iconType < 0) continue;
            drawCell(g, p, c, xl, w);
            xl += w;
        }
    }

    private void drawCell(Canvas g, Theme.Palette p, Cell c, float x, float w) {
        if (c.text.length() == 0 && c.iconType < 0) {
            return;
        }
        // futás jelzése külön háttérszínnel
        if ("run".equals(c.id) && c.text != null && c.text.startsWith("Fut –")) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(p.statusRunBg);
            g.drawRect(x, 0, x + w, getHeight(), paint);
        }
        float iconSize = FlatButton.dp(getContext(), 13);
        float tx = x + FlatButton.dp(getContext(), 5);
        if (c.iconType >= 0) {
            DrawableHolder h = new DrawableHolder(VsIcons.icon(c.iconType, (int) iconSize,
                    c.iconColor == -1 ? p.statusFg : c.iconColor));
            h.d.setBounds((int) tx, (int) ((getHeight() - iconSize) / 2),
                          (int) (tx + iconSize), (int) ((getHeight() + iconSize) / 2));
            h.d.draw(g);
            tx += FlatButton.dp(getContext(), 17);
        }
        if (c.text != null && c.text.length() > 0) {
            paint.setColor(p.statusFg);
            float baseline = (getHeight() + paint.getTextSize() * 0.7f) / 2;
            g.drawText(c.text, tx, baseline, paint);
        }
    }

    private static final class DrawableHolder {
        final android.graphics.drawable.Drawable d;
        DrawableHolder(android.graphics.drawable.Drawable d) { this.d = d; }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            float textSize = FlatButton.dp(getContext(), 11);
            paint.setTextSize(textSize);
            float xr = getWidth();
            for (int i = cells.size() - 1; i >= 0; i--) {
                Cell c = cells.get(i);
                if (!c.rightSide) continue;
                float w = cellWidth(c, paint);
                xr -= w;
                if (ev.getX() >= xr && ev.getX() <= xr + w && c.action != null) {
                    c.action.run();
                    return true;
                }
            }
            float xl = 0;
            for (Cell c : cells) {
                if (c.rightSide) continue;
                float w = cellWidth(c, paint);
                if (ev.getX() >= xl && ev.getX() <= xl + w && c.action != null) {
                    c.action.run();
                    return true;
                }
                xl += w;
            }
        }
        return true;
    }

    @Override
    protected void onMeasure(int wspec, int hspec) {
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), wspec),
                             FlatButton.dp(getContext(), 24));
    }
}
