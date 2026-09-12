package hu.ppke.itk.plang.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Fülsor – a asztali widgets.EditorTabBar megfelelője. Két stílusban használatos:
 * a szerkesztő fülei (normál) és a csatornák fülei (panel).
 */
public class EditorTabBar extends HorizontalScrollView {

    public static final int STYLE_EDITOR = 0;
    public static final int STYLE_PANEL = 1;

    public interface Listener {
        void tabSelected(String id);
        void tabClosed(String id);
    }

    public static final class Tab {
        public final String id;
        public String title;
        public final int icon;
        public boolean dirty;

        public Tab(String id, String title, int icon, boolean dirty) {
            this.id = id;
            this.title = title;
            this.icon = icon;
            this.dirty = dirty;
        }
    }

    private final List<Tab> tabs = new ArrayList<Tab>();
    private final LinearLayout row;
    private final Listener listenerProxy;
    private Listener listener;
    private int style = STYLE_EDITOR;
    private String selectedId = null;

    public EditorTabBar(Context ctx) {
        super(ctx);
        setHorizontalScrollBarEnabled(false);
        row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        addView(row, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        listenerProxy = new Listener() {
            @Override
            public void tabSelected(String id) {
                if (listener != null) listener.tabSelected(id);
            }
            @Override
            public void tabClosed(String id) {
                if (listener != null) listener.tabClosed(id);
            }
        };
    }

    public void setListener(Listener l) {
        this.listener = l;
    }

    public void setStyle(int style) {
        this.style = style;
        applyTheme();
    }

    public void addTab(Tab t) {
        tabs.add(t);
        if (selectedId == null) {
            selectedId = t.id;
        }
        rebuild();
    }

    public void removeTab(String id) {
        for (int i = tabs.size() - 1; i >= 0; i--) {
            if (tabs.get(i).id.equals(id)) {
                tabs.remove(i);
            }
        }
        rebuild();
    }

    public void clearTabs() {
        tabs.clear();
        selectedId = null;
        rebuild();
    }

    public void select(String id) {
        selectedId = id;
        rebuild();
    }

    public String selected() {
        return selectedId;
    }

    public void setTitle(String id, String title) {
        for (Tab t : tabs) {
            if (t.id.equals(id)) {
                t.title = title;
            }
        }
        rebuild();
    }

    public void setDirty(String id, boolean dirty) {
        for (Tab t : tabs) {
            if (t.id.equals(id)) {
                t.dirty = dirty;
            }
        }
        rebuild();
    }

    public int count() {
        return tabs.size();
    }

    public String titleAt(int i) {
        return tabs.get(i).title;
    }

    private void rebuild() {
        Context ctx = getContext();
        row.removeAllViews();
        Theme.Palette p = Theme.p();
        for (final Tab t : tabs) {
            boolean sel = t.id.equals(selectedId);
            LinearLayout cell = new LinearLayout(ctx);
            cell.setOrientation(LinearLayout.HORIZONTAL);
            cell.setGravity(Gravity.CENTER_VERTICAL);
            int padH = FlatButton.dp(ctx, 10);
            int padV = FlatButton.dp(ctx, 7);
            cell.setPadding(padH, 0, padH, 0);

            TextView label = new TextView(ctx);
            label.setText(t.title);
            label.setSingleLine(true);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            label.setTypeface(Typeface.create(Typeface.SANS_SERIF, sel ? Typeface.BOLD : Typeface.NORMAL));
            label.setTextColor(sel ? p.tabActiveFg : p.tabInactiveFg);
            cell.addView(label);

            if (t.dirty) {
                View dot = new View(ctx);
                dot.setBackground(new android.graphics.drawable.ColorDrawable(p.editorFg)) ;
                LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                        FlatButton.dp(ctx, 6), FlatButton.dp(ctx, 6));
                dlp.leftMargin = FlatButton.dp(ctx, 6);
                dot.setLayoutParams(dlp);
                cell.addView(dot);
            }

            cell.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    select(t.id);
                    listenerProxy.tabSelected(t.id);
                }
            });

            int h = FlatButton.dp(ctx, style == STYLE_EDITOR ? 34 : 28);
            LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, h);
            cell.setLayoutParams(lp);
            // egyedi háttér-rajzolás a felső active-sávval
            cell.setBackground(new TabBackground(sel));
            row.addView(cell);
        }
        applyTheme();
    }

    private static final class dirtyTouchListener implements View.OnTouchListener {
        private final Paint paint;
        dirtyTouchListener(Paint paint) { this.paint = paint; }
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            v.invalidate();
            return false;
        }
    }

    /** A fül háttere: aktívnál a szerkesztő háttérszíne + felső kiemelő sáv. */
    private final class TabBackground extends android.graphics.drawable.ColorDrawable {
        private final boolean sel;
        TabBackground(boolean sel) { this.sel = sel; }

        @Override
        public void draw(Canvas canvas) {
            Theme.Palette p = Theme.p();
            if (style == STYLE_EDITOR) {
                paint.setColor(sel ? p.tabActiveBg : p.tabInactiveBg);
                canvas.drawRect(getBounds(), paint);
                if (sel) {
                    paint.setColor(p.tabActiveTopBorder);
                    canvas.drawRect(getBounds().left, getBounds().top,
                                    getBounds().right, getBounds().top + FlatButton.dp(getContext(), 2), paint);
                }
                paint.setColor(p.tabBorder);
                canvas.drawRect(getBounds().left, getBounds().bottom - 1,
                                getBounds().right, getBounds().bottom, paint);
            } else {
                paint.setColor(sel ? p.tabActiveBg : 0x00000000);
                canvas.drawRect(getBounds(), paint);
            }
        }
    }

    private final Paint paint = new Paint();

    public void applyTheme() {
        Theme.Palette p = Theme.p();
        setBackgroundColor(style == STYLE_EDITOR ? p.tabBarBg : p.editorBg);
        for (int i = 0; i < row.getChildCount(); i++) {
            View c = row.getChildAt(i);
            if (c instanceof LinearLayout) {
                LinearLayout cell = (LinearLayout) c;
                for (int j = 0; j < cell.getChildCount(); j++) {
                    View cc = cell.getChildAt(j);
                    if (cc instanceof TextView) {
                        boolean sel = tabs.get(i).id.equals(selectedId);
                        ((TextView) cc).setTextColor(sel ? p.tabActiveFg : p.tabInactiveFg);
                        ((TextView) cc).setTypeface(Typeface.create(Typeface.SANS_SERIF,
                                sel ? Typeface.BOLD : Typeface.NORMAL));
                    }
                }
                c.setBackground(new TabBackground(tabs.get(i).id.equals(selectedId)));
            }
        }
        invalidate();
    }
}
