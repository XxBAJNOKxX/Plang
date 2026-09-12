package hu.ppke.itk.plang.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import hu.ppke.itk.plang.prog.BadValue;
import hu.ppke.itk.plang.prog.State;
import hu.ppke.itk.plang.prog.Type;

/**
 * Az állapottábla – a asztali StateList + JTable portja: LÉPÉS oszlop plusz
 * egy-egy oszlop minden változónak, soronként a végrehajtás egy állapota.
 * A sor kijelölése szinkronizálja a szerkesztőt, a kifejezést és a csatornákat.
 */
public class StateTableView extends LinearLayout {

    private List<State> states;
    private List<String> names;
    private List<Type> types;
    private int[] colWidths;
    private int selected = -1;

    private final HeaderView header;
    private final HorizontalScrollView headerScroll;
    private final HorizontalScrollView bodyScroll;
    private final ListView body;
    private final Adapter adapter;
    private RowSelectListener listener;

    public interface RowSelectListener {
        void onRowSelected(int row);
    }

    public StateTableView(Context ctx) {
        super(ctx);
        setOrientation(VERTICAL);
        header = new HeaderView(ctx);
        headerScroll = new HorizontalScrollView(ctx);
        headerScroll.setHorizontalScrollBarEnabled(false);
        headerScroll.addView(header);
        addView(headerScroll, new LayoutParams(LayoutParams.MATCH_PARENT, FlatButton.dp(ctx, 26)));

        bodyScroll = new HorizontalScrollView(ctx);
        bodyScroll.setHorizontalScrollBarEnabled(true);
        body = new ListView(ctx);
        body.setDivider(null);
        body.setVerticalScrollBarEnabled(true);
        adapter = new Adapter();
        body.setAdapter(adapter);
        body.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setSelectedRow(position);
                if (listener != null) {
                    listener.onRowSelected(position);
                }
            }
        });
        bodyScroll.addView(body);
        addView(bodyScroll, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        // a fejléc vízszintes görgetése követi a testet és fordítva
        bodyScroll.setOnScrollChangeListener(new HorizontalScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                headerScroll.scrollTo(scrollX, 0);
            }
        });
        headerScroll.setOnScrollChangeListener(new HorizontalScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                bodyScroll.scrollTo(scrollX, 0);
            }
        });
    }

    public void setListener(RowSelectListener l) {
        listener = l;
    }

    /**
     * Új állapotlista betöltése – az asztali StateList.setStates logikája.
     */
    public void setStates(List<State> stl) {
        states = null;
        names = null;
        types = null;
        colWidths = null;
        selected = -1;
        if (stl != null && !stl.isEmpty()) {
            states = new ArrayList<State>(stl);
            SortedSet<String> ns = new TreeSet<String>(states.get(0).getVarNames());
            names = new ArrayList<String>(ns);
            types = new ArrayList<Type>();
            for (String var : names) {
                types.add(states.get(0).getVarType(var));
            }
        }
        computeColumnWidths();
        adapter.notifyDataSetChanged();
        header.requestLayout();
        header.invalidate();
        invalidate();
    }

    public int rowCount() {
        return states == null ? 0 : states.size();
    }

    public int getSelectedRow() {
        return selected;
    }

    public void setSelectedRow(int row) {
        selected = row;
        if (row >= 0 && row < rowCount()) {
            body.setSelection(row);
        }
        adapter.notifyDataSetChanged();
        header.invalidate();
    }

    public State getState(int row) {
        return (states != null && row >= 0 && row < states.size()) ? states.get(row) : null;
    }

    /** Egy cella szövege – az asztali getValueAt logikája. */
    private String cellText(int row, int col) {
        if (col == 0) {
            return String.valueOf(row + 1);
        }
        State s = states.get(row);
        Object val = s.getVar(names.get(col - 1));
        if (val == null) {
            return "???";
        }
        if (val instanceof BadValue) {
            return "###";
        }
        return types.get(col - 1).render(val);
    }

    /** Egy cella színe (??? kék, ### piros). */
    private int cellColor(Theme.Palette p, int row, int col) {
        if (col == 0) {
            return p.gutterFg;
        }
        State s = states.get(row);
        Object val = s.getVar(names.get(col - 1));
        if (val == null) {
            return p.info;
        }
        if (val instanceof BadValue) {
            return p.error;
        }
        return p.editorFg;
    }

    /** Az oszlopszélességek kiszámítása (az autoSizeStateColumns mintájára). */
    private void computeColumnWidths() {
        Context ctx = getContext();
        int cols = columnCount();
        colWidths = new int[cols];
        Paint fm = new Paint(Paint.ANTI_ALIAS_FLAG);
        String family = AppPrefs.getFontFamily();
        fm.setTypeface(Typeface.create(family, Typeface.NORMAL));
        fm.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                AppPrefs.getFontSize(), ctx.getResources().getDisplayMetrics()));
        Paint fmHead = new Paint(fm);
        fmHead.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        fmHead.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                11, ctx.getResources().getDisplayMetrics()));
        for (int c = 0; c < cols; c++) {
            float w = FlatButton.dp(ctx, c == 0 ? 44 : 66);
            String head = columnName(c);
            w = Math.max(w, fmHead.measureText(head) + FlatButton.dp(ctx, 18));
            if (states != null) {
                int lim = Math.min(states.size(), 200);
                for (int r = 0; r < lim; r++) {
                    String v = cellText(r, c);
                    w = Math.max(w, fm.measureText(v) + FlatButton.dp(ctx, 16));
                }
            }
            colWidths[c] = (int) Math.min(w, FlatButton.dp(ctx, 200));
        }
    }

    public int columnCount() {
        return names == null ? 0 : names.size() + 1;
    }

    public String columnName(int col) {
        return col == 0 ? "LÉPÉS" : names.get(col - 1);
    }

    public void applyTheme() {
        adapter.notifyDataSetChanged();
        header.invalidate();
        invalidate();
    }

    private class Adapter extends BaseAdapter {
        @Override
        public int getCount() {
            return rowCount();
        }

        @Override
        public Object getItem(int position) {
            return states.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            RowView v;
            if (convertView instanceof RowView) {
                v = (RowView) convertView;
            } else {
                v = new RowView(getContext());
            }
            v.configure(position);
            return v;
        }
    }

    /** A táblafejléc. */
    private final class HeaderView extends View {
        HeaderView(Context ctx) {
            super(ctx);
        }

        private final Paint hp = new Paint(Paint.ANTI_ALIAS_FLAG);

        @Override
        protected void onDraw(Canvas g) {
            Theme.Palette p = Theme.p();
            g.drawColor(p.tableHeaderBg);
            if (colWidths == null) {
                return;
            }
            hp.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            hp.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                    11, getResources().getDisplayMetrics()));
            hp.setColor(p.sideBarTitleFg);
            float baseline = (getHeight() + hp.getTextSize() * 0.7f) / 2f;
            float x = 0;
            for (int c = 0; c < colWidths.length; c++) {
                String t = columnName(c);
                String shown = t;
                while (shown.length() > 1 && hp.measureText(shown + "…") > colWidths[c] - 10) {
                    shown = shown.substring(0, shown.length() - 1);
                }
                if (shown.length() != t.length()) {
                    shown = shown + "…";
                }
                g.drawText(shown, x + FlatButton.dp(getContext(), 4), baseline, hp);
            // elválasztó
            hp.setColor(p.border);
            g.drawRect(x + colWidths[c] - 1, FlatButton.dp(getContext(), 3),
                       x + colWidths[c], getHeight() - FlatButton.dp(getContext(), 3), hp);
            x += colWidths[c];
            }
            hp.setColor(p.border);
            g.drawRect(0, getHeight() - 1, Math.max(getWidth(), x), getHeight(), hp);
        }

        @Override
        protected void onMeasure(int wspec, int hspec) {
            int total = 0;
            if (colWidths != null) {
                for (int w : colWidths) {
                    total += w;
                }
            }
            setMeasuredDimension(Math.max(getDefaultSize(getSuggestedMinimumWidth(), wspec), total),
                                 FlatButton.dp(getContext(), 26));
        }
    }

    /** Egy táblasor. */
    private final class RowView extends View {
        private int row = -1;
        private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);

        RowView(Context ctx) {
            super(ctx);
            syncFont();
        }

        void syncFont() {
            String family = AppPrefs.getFontFamily();
            tp.setTypeface(Typeface.create(family, Typeface.NORMAL));
            tp.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                    AppPrefs.getFontSize(), getResources().getDisplayMetrics()));
        }

        void configure(int r) {
            row = r;
            syncFont();
        }

        @Override
        protected void onDraw(Canvas g) {
            Theme.Palette p = Theme.p();
            g.drawColor(row == selected ? p.listSelection : p.panelBg);
            if (colWidths == null || states == null) {
                return;
            }
            float baseline = (getHeight() + tp.getTextSize() * 0.7f) / 2f;
            float x = 0;
            for (int c = 0; c < colWidths.length; c++) {
                tp.setColor(row == selected ? p.listSelectionFg : cellColor(p, row, c));
                g.drawText(cellText(row, c), x + FlatButton.dp(getContext(), 5), baseline, tp);
                x += colWidths[c];
            }
        }

        @Override
        protected void onMeasure(int wspec, int hspec) {
            float lh = tp.getTextSize() * 1.5f + FlatButton.dp(getContext(), 4);
            setMeasuredDimension(header.getMeasuredWidth(), (int) lh);
        }
    }
}
