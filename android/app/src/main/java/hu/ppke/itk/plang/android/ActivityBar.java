package hu.ppke.itk.plang.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * A VS Code stílusú tevékenységsáv – a asztali widgets.ActivityBar
 * megfelelője: fent a nézetválasztó ikonok, alul a téma és a beállítások.
 */
public class ActivityBar extends View {

    public static final class Item {
        final int icon;
        final String desc;
        final Runnable action;
        final boolean toggle;

        Item(int icon, String desc, Runnable action, boolean toggle) {
            this.icon = icon;
            this.desc = desc;
            this.action = action;
            this.toggle = toggle;
        }
    }

    private final List<Item> top = new ArrayList<Item>();
    private final List<Item> bottom = new ArrayList<Item>();
    private int selected = 0;
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ActivityBar(Context ctx) {
        super(ctx);
    }

    public int addView(int icon, String desc, Runnable action) {
        top.add(new Item(icon, desc, action, true));
        return top.size() - 1;
    }

    public void addBottomAction(int icon, String desc, Runnable action) {
        bottom.add(new Item(icon, desc, action, false));
    }

    public void setSelected(int index) {
        this.selected = index;
        invalidate();
    }

    public int getSelected() {
        return selected;
    }

    @Override
    protected void onDraw(Canvas g) {
        Theme.Palette p = Theme.p();
        g.drawColor(p.activityBar);
        int size = FlatButton.dp(getContext(), 48);
        int iconSize = FlatButton.dp(getContext(), 22);

        for (int i = 0; i < top.size(); i++) {
            Item it = top.get(i);
            int y = i * size;
            if (i == selected) {
                borderPaint.setColor(p.activityBarActiveBorder);
                g.drawRect(0, y, FlatButton.dp(getContext(), 2), y + size, borderPaint);
            }
            int color = (i == selected) ? p.activityBarActiveFg : p.activityBarFg;
            Drawable d = VsIcons.icon(it.icon, iconSize, color);
            d.setBounds((getWidth() - iconSize) / 2, y + (size - iconSize) / 2,
                        (getWidth() + iconSize) / 2, y + (size + iconSize) / 2);
            d.draw(g);
        }

        int by = getHeight() - bottom.size() * size;
        for (int i = 0; i < bottom.size(); i++) {
            Item it = bottom.get(i);
            int y = by + i * size;
            Drawable d = VsIcons.icon(it.icon, iconSize, p.activityBarFg);
            d.setBounds((getWidth() - iconSize) / 2, y + (size - iconSize) / 2,
                        (getWidth() + iconSize) / 2, y + (size + iconSize) / 2);
            d.draw(g);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            int size = FlatButton.dp(getContext(), 48);
            int y = (int) ev.getY();
            int idx = y / size;
            if (idx >= 0 && idx < top.size()) {
                if (idx == selected) {
                    // ugyanarra koppintva a panel összecsukható
                    selected = -1;
                } else {
                    selected = idx;
                }
                top.get(idx).action.run();
                invalidate();
                return true;
            }
            int by = getHeight() - bottom.size() * size;
            if (y >= by) {
                int bi = (y - by) / size;
                if (bi >= 0 && bi < bottom.size()) {
                    bottom.get(bi).action.run();
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    protected void onMeasure(int wspec, int hspec) {
        setMeasuredDimension(FlatButton.dp(getContext(), 46), getDefaultSize(getSuggestedMinimumHeight(), hspec));
    }
}
