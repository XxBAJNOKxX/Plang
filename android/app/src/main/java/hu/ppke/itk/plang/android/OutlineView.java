package hu.ppke.itk.plang.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * A program vázlata (program, eljárások, függvények, VÁLTOZÓK) – a asztali
 * OutlineList portja. Koppintás: ugrás a sorra a szerkesztőben.
 */
public class OutlineView extends ListView {

    private static final class Row {
        final String label;
        final int icon;
        final int line;

        Row(String label, int icon, int line) {
            this.label = label;
            this.icon = icon;
            this.line = line;
        }
    }

    private final List<Row> rows = new ArrayList<Row>();
    private final Adapter adapter;
    private GotoListener listener;

    public interface GotoListener {
        void onGoto(int line);
    }

    public OutlineView(Context ctx) {
        super(ctx);
        adapter = new Adapter();
        setAdapter(adapter);
        setDivider(null);
        setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (listener != null) {
                    listener.onGoto(rows.get(position).line);
                }
            }
        });
    }

    public void setListener(GotoListener l) {
        listener = l;
    }

    /** A vázlat újraépítése a forrásszövegből (a rebuild portja). */
    public void rebuild(String src) {
        rows.clear();
        if (src == null) {
            adapter.notifyDataSetChanged();
            return;
        }
        String[] lines = src.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String t = lines[i].trim();
            String low = hu.ppke.itk.plang.gui.editor.PlangSyntax.deacc(t);
            if (low.startsWith("program ")) {
                rows.add(new Row(t.substring(8).trim(), VsIcons.NEW, i));
            } else if (low.startsWith("eljaras ")) {
                rows.add(new Row(t.substring(8).trim(), VsIcons.RUN, i));
            } else if (low.startsWith("fuggveny ")) {
                rows.add(new Row(t.substring(9).trim(), VsIcons.PARSE, i));
            } else if (low.startsWith("valtozok")) {
                rows.add(new Row("Változók", VsIcons.VARIABLES, i));
            }
        }
        adapter.notifyDataSetChanged();
    }

    public void applyTheme() {
        adapter.notifyDataSetChanged();
        invalidate();
    }

    private class Adapter extends BaseAdapter {
        @Override
        public int getCount() {
            return rows.size();
        }

        @Override
        public Object getItem(int position) {
            return rows.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Cell c;
            if (convertView instanceof Cell) {
                c = (Cell) convertView;
            } else {
                c = new Cell(getContext());
            }
            Row r = rows.get(position);
            c.configure(r.label, r.icon);
            return c;
        }
    }

    private static final class Cell extends View {
        private String label = "";
        private int icon;
        private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);

        Cell(Context ctx) {
            super(ctx);
            tp.setTypeface(Typeface.SANS_SERIF);
            tp.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                    13, ctx.getResources().getDisplayMetrics()));
        }

        void configure(String label, int icon) {
            this.label = label;
            this.icon = icon;
        }

        @Override
        protected void onDraw(Canvas g) {
            Theme.Palette p = Theme.p();
            g.drawColor(p.sideBar);
            float baseline = (getHeight() + tp.getTextSize() * 0.7f) / 2f;
            android.graphics.drawable.Drawable d = VsIcons.icon(icon,
                    FlatButton.dp(getContext(), 13), p.synFunction);
            int ds = FlatButton.dp(getContext(), 13);
            d.setBounds(FlatButton.dp(getContext(), 8), (int) (getHeight() - ds) / 2,
                        FlatButton.dp(getContext(), 8) + ds, (int) (getHeight() + ds) / 2);
            d.draw(g);
            tp.setColor(p.sideBarFg);
            g.drawText(label, FlatButton.dp(getContext(), 26), baseline, tp);
        }

        @Override
        protected void onMeasure(int wspec, int hspec) {
            setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), wspec),
                                 FlatButton.dp(getContext(), 24));
        }
    }
}
