package hu.ppke.itk.plang.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import hu.ppke.itk.plang.gui.GuiBridge;
import hu.ppke.itk.plang.gui.ProgramLine;
import hu.ppke.itk.plang.gui.editor.PlangSyntax;

/**
 * Az értelmezett program sorainak listája – a asztali ProgramList +
 * ProgLineRenderer portja: sorszámozás, szintaxis-színezés, hibajelzés,
 * aktuális sor kiemelése. Koppintás: kifejezésfa frissítés; dupla koppintás:
 * ugrás a forrássorra.
 */
public class ProgramListView extends ListView {

    private List<ProgramLine> lines = new ArrayList<ProgramLine>();
    private int selected = -1;
    private int currentLine = -1;
    private final Adapter adapter;
    private ItemSelectListener listener;
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public interface ItemSelectListener {
        void onSelected(int index);
        void onDoubleTap(int index);
    }

    public ProgramListView(Context ctx) {
        super(ctx);
        adapter = new Adapter();
        setAdapter(adapter);
        setChoiceMode(CHOICE_MODE_SINGLE);
        setDivider(null);
        setVerticalScrollBarEnabled(true);
        setOnItemClickListener(new AdapterView.OnItemClickListener() {
            private long lastClick = 0;
            private int lastIndex = -1;

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                long now = System.currentTimeMillis();
                if (position == lastIndex && now - lastClick < 400) {
                    if (listener != null) {
                        listener.onDoubleTap(position);
                    }
                } else if (listener != null) {
                    listener.onSelected(position);
                }
                lastClick = now;
                lastIndex = position;
            }
        });
    }

    public void setListener(ItemSelectListener l) {
        listener = l;
    }

    public void setLines(List<ProgramLine> newLines) {
        lines = new ArrayList<ProgramLine>(newLines);
        // a sorok indexének beállítása – mint az asztali ProgramList.setProgram:
        // a State.getLine() erre az indexre hivatkozik
        int l = 0;
        for (ProgramLine line : lines) {
            line.setLine(l++);
        }
        selected = -1;
        adapter.notifyDataSetChanged();
    }

    public List<ProgramLine> getLines() {
        return lines;
    }

    public ProgramLine getAt(int i) {
        return (i >= 0 && i < lines.size()) ? lines.get(i) : null;
    }

    public void setSelectedIndex(int i) {
        selected = i;
        if (i >= 0) {
            setSelection(i);
        }
        adapter.notifyDataSetChanged();
    }

    public int getSelectedIndex() {
        return selected;
    }

    public void setCurrentLine(int line) {
        if (currentLine != line) {
            currentLine = line;
            adapter.notifyDataSetChanged();
        }
    }

    public int getCurrentLine() {
        return currentLine;
    }

    public void applyTheme() {
        setBackgroundColor(Theme.p().editorBg);
        adapter.notifyDataSetChanged();
        invalidate();
    }

    private class Adapter extends BaseAdapter {
        @Override
        public int getCount() {
            return lines.size();
        }

        @Override
        public Object getItem(int position) {
            return lines.get(position);
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
            c.configure(position);
            return c;
        }
    }

    /** Egy sor kirajzoló nézete (a Cell portja). */
    private final class Cell extends View {
        private int index;
        private String text = "";
        private String badFlags = "";
        private boolean error;
        private String tip;
        private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

        Cell(Context ctx) {
            super(ctx);
            textPaint.setTypeface(Typeface.MONOSPACE);
            setTextSizeFromPrefs();
        }

        void setTextSizeFromPrefs() {
            String family = AppPrefs.getFontFamily();
            int size = AppPrefs.getFontSize();
            textPaint.setTypeface(Typeface.create(family, Typeface.NORMAL));
            textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                    size, getResources().getDisplayMetrics()));
        }

        void configure(int idx) {
            this.index = idx;
            setTextSizeFromPrefs();
            ProgramLine pl = lines.get(idx);
            String[] r = ProgRender.stripHtml(pl.toString());
            this.text = r[0];
            this.badFlags = r[1];
            this.error = GuiBridge.hasError(pl);
            this.tip = GuiBridge.hasError(pl) ? "HIBA: " + GuiBridge.error(pl) : null;
        }

        @Override
        protected void onDraw(Canvas g) {
            Theme.Palette p = Theme.p();
            g.drawColor(selected == index ? p.listSelection : p.editorBg);

            float padL = FlatButton.dp(getContext(), 6);
            float lh = textPaint.getTextSize() * 1.35f;
            float baseline = (getHeight() + textPaint.getTextSize() * 0.7f) / 2f;

            // aktuális végrehajtási sor sávja
            if (index == currentLine) {
                bgPaint.setColor(Theme.alpha(p.warning, 60));
                g.drawRect(0, 0, getWidth(), getHeight(), bgPaint);
            }

            // hibapötty
            if (error) {
                dotPaint.setColor(p.error);
                g.drawCircle(FlatButton.dp(getContext(), 7), getHeight() / 2f,
                             FlatButton.dpf(getContext(), 2.6f), dotPaint);
                padL = FlatButton.dp(getContext(), 14);
            }

            // sorszám
            textPaint.setColor(selected == index ? p.listSelectionFg : p.gutterFg);
            String num = String.valueOf(index + 1);
            g.drawText(num, padL, baseline, textPaint);
            float x = padL + Math.max(FlatButton.dp(getContext(), 20),
                                      textPaint.measureText(num)) + FlatButton.dp(getContext(), 6);

            // szöveg szintaxisszínekkel
            textPaint.setColor(selected == index ? p.listSelectionFg : p.editorFg);
            int off = 0;
            while (off < text.length()) {
                boolean bad = off < badFlags.length() && badFlags.charAt(off) == '1';
                int end = off;
                while (end < text.length()
                       && ((end < badFlags.length() && badFlags.charAt(end) == '1') == bad)) {
                    end++;
                }
                String seg = text.substring(off, end);
                if (bad) {
                    textPaint.setColor(selected == index ? p.listSelectionFg : p.error);
                    g.drawText(seg, x, baseline, textPaint);
                } else {
                    // tokenenkénti színezés
                    java.util.List<PlangSyntax.Token> tokens = PlangSyntax.tokenize(seg);
                    for (PlangSyntax.Token t : tokens) {
                        if (t.start >= seg.length() || t.end > seg.length() || t.end <= t.start) {
                            continue;
                        }
                        int color = selected == index
                            ? p.listSelectionFg
                            : CodeEditorView.colorOf(p, t.kind);
                        textPaint.setColor(color);
                        float x1 = x + textPaint.measureText(seg.substring(0, t.start));
                        g.drawText(seg.substring(t.start, t.end), x1, baseline, textPaint);
                    }
                    textPaint.setColor(selected == index ? p.listSelectionFg : p.editorFg);
                }
                x += textPaint.measureText(seg);
                off = end;
            }
        }

        @Override
        protected void onMeasure(int wspec, int hspec) {
            float lh = textPaint.getTextSize() * 1.35f + FlatButton.dp(getContext(), 4);
            setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), wspec),
                                 (int) lh);
        }
    }
}
