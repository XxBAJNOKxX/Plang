package hu.ppke.itk.plang.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import hu.ppke.itk.plang.prog.StreamData;

import java.util.ArrayList;
import java.util.List;

/**
 * A hívási verem – a asztali CallStack + CallStackRenderer portja.
 */
public class CallStackView extends ListView {

    private static final class Entry {
        final String expr;
        final List<hu.ppke.itk.plang.prog.State> states;

        Entry(String expr, List<hu.ppke.itk.plang.prog.State> states) {
            this.expr = expr;
            this.states = states;
        }
    }

    private final List<Entry> stack = new ArrayList<Entry>();
    private final StateTableView stateTable;
    private int maxSteps;
    private final Adapter adapter;

    public CallStackView(Context ctx, StateTableView stateTable, int maxSteps) {
        super(ctx);
        this.stateTable = stateTable;
        this.maxSteps = maxSteps;
        adapter = new Adapter();
        setAdapter(adapter);
        setDivider(null);
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public void setMaxSteps(int steps) {
        maxSteps = steps;
    }

    /** Egy futtatás eredménye (a számítás UI-száltól függetlenül is futtatható). */
    public static final class RunResult {
        public final List<hu.ppke.itk.plang.prog.State> states;
        public final hu.ppke.itk.plang.prog.State last;

        RunResult(List<hu.ppke.itk.plang.prog.State> states, hu.ppke.itk.plang.prog.State last) {
            this.states = states;
            this.last = last;
        }
    }

    /**
     * A program leszipuzása háttérszálon is hívható: nem nyúl a nézetekhez.
     */
    public static RunResult computeProgram(hu.ppke.itk.plang.prog.MainProgram prog,
                                           java.util.Map<String, String> input,
                                           int maxSteps) {
        List<hu.ppke.itk.plang.prog.State> states = prog.runProgram(input, maxSteps);
        return new RunResult(states, states.get(states.size() - 1));
    }

    /**
     * A futtatás eredményének megjelenítése (UI szálon): verem, állapottábla,
     * kimeneti csatornák.
     */
    public void applyRunResult(RunResult r, java.util.Map<String, StreamData> output) {
        stack.clear();
        stack.add(new Entry("FŐPROGRAM", r.states));
        adapter.notifyDataSetChanged();
        stateTable.setStates(r.states);
        for (String stream : r.last.getStreamNames()) {
            output.put(stream, r.last.getStream(stream));
        }
    }

    /** A verem törlése és az állapottábla ürítése (Stop). */
    public void clearProgram() {
        stack.clear();
        adapter.notifyDataSetChanged();
        stateTable.setStates(null);
    }

    /** Belépés alprogramba (az ExprNode altállapotjaival). */
    public void enter(String expr, List<hu.ppke.itk.plang.prog.State> subProg) {
        stack.add(new Entry(expr, subProg));
        adapter.notifyDataSetChanged();
        stateTable.setStates(subProg);
    }

    /** Kilépés az alprogramból. */
    public void leave() {
        if (stack.size() > 1) {
            stack.remove(stack.size() - 1);
            adapter.notifyDataSetChanged();
            stateTable.setStates(stack.get(stack.size() - 1).states);
        }
    }

    public int stackDepth() {
        return stack.size();
    }

    public void applyTheme() {
        adapter.notifyDataSetChanged();
        invalidate();
    }

    private class Adapter extends BaseAdapter {
        @Override
        public int getCount() {
            return stack.size();
        }

        @Override
        public Object getItem(int position) {
            return stack.get(position);
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
            c.configure(ProgRender.stripHtml(stack.get(position).expr)[0],
                        position == stack.size() - 1);
            return c;
        }
    }

    private static final class Cell extends View {
        private String text = "";
        private boolean top;
        private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);

        Cell(Context ctx) {
            super(ctx);
            String family = AppPrefs.getFontFamily();
            tp.setTypeface(Typeface.create(family, Typeface.NORMAL));
            tp.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                    AppPrefs.getFontSize(), ctx.getResources().getDisplayMetrics()));
        }

        void configure(String t, boolean topFrame) {
            text = t;
            top = topFrame;
        }

        @Override
        protected void onDraw(Canvas g) {
            Theme.Palette p = Theme.p();
            g.drawColor(p.panelBg);
            float baseline = (getHeight() + tp.getTextSize() * 0.7f) / 2f;
            float x = FlatButton.dp(getContext(), 4);
            if (top) {
                android.graphics.drawable.Drawable d = VsIcons.icon(VsIcons.CHEVRON_RIGHT,
                        FlatButton.dp(getContext(), 13), p.warning);
                int ds = FlatButton.dp(getContext(), 13);
                d.setBounds((int) x, (int) (getHeight() - ds) / 2, (int) x + ds, (int) (getHeight() + ds) / 2);
                d.draw(g);
                x += FlatButton.dp(getContext(), 18);
            } else {
                x += FlatButton.dp(getContext(), 4);
            }
            tp.setColor(top ? p.editorFg : p.gutterFg);
            g.drawText(text, x, baseline, tp);
        }

        @Override
        protected void onMeasure(int wspec, int hspec) {
            setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), wspec),
                                 FlatButton.dp(getContext(), 24));
        }
    }
}
