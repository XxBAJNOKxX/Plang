package hu.ppke.itk.plang.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.GuiBridge;
import hu.ppke.itk.plang.gui.editor.PlangSyntax;

/**
 * A kifejezésfa – a asztali ExprTree + ExprRenderer portja RecyclerView
 * helyett platform ListView-val: a látható csomópontok lapított listája,
 * behúzással és nyíl-ikonokkal. Koppintás: kijelölés; kettős koppintás vagy
 * hosszú nyomás: belépés alprogramba (ha van).
 */
public class ExprTreeView extends ListView {

    private static final class Node {
        final ExprNode node;
        final int depth;
        boolean expanded = true;

        Node(ExprNode node, int depth) {
            this.node = node;
            this.depth = depth;
        }
    }

    private final List<Node> visible = new ArrayList<Node>();
    private final java.util.IdentityHashMap<ExprNode, Boolean> expandedMap =
            new java.util.IdentityHashMap<ExprNode, Boolean>();
    private ExprNode root = ExprNode.EMPTY;
    private int selected = -1;
    private final Adapter adapter;
    private NodeSelectListener listener;
    private EnterListener enterListener;

    public interface NodeSelectListener {
        void onNodeSelected(ExprNode node);
    }

    public interface EnterListener {
        void onEnterRequested(ExprNode node);
    }

    public ExprTreeView(Context ctx) {
        super(ctx);
        adapter = new Adapter();
        setAdapter(adapter);
        setDivider(null);
        setChoiceMode(CHOICE_MODE_SINGLE);
        setOnItemClickListener(new AdapterView.OnItemClickListener() {
            private long lastClick = 0;
            private int lastIndex = -1;

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Node n = visible.get(position);
                long now = System.currentTimeMillis();
                if (position == lastIndex && now - lastClick < 400 && GuiBridge.subStates(n.node) != null) {
                    if (enterListener != null) {
                        enterListener.onEnterRequested(n.node);
                    }
                } else {
                    selected = position;
                    adapter.notifyDataSetChanged();
                    if (listener != null) {
                        listener.onNodeSelected(n.node);
                    }
                }
                lastClick = now;
                lastIndex = position;
            }
        });
        setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Node n = visible.get(position);
                if (GuiBridge.subStates(n.node) != null && enterListener != null) {
                    enterListener.onEnterRequested(n.node);
                    return true;
                }
                // levegő koppintásra a csomópont nyit/zár
                if (GuiBridge.childCount(n.node) > 0) {
                    n.expanded = !n.expanded;
                    rebuild();
                    return true;
                }
                return false;
            }
        });
    }

    public void setListener(NodeSelectListener l) {
        listener = l;
    }

    public void setEnterListener(EnterListener l) {
        enterListener = l;
    }

    public void setRoot(ExprNode newRoot) {
        this.root = (newRoot == null) ? ExprNode.EMPTY : newRoot;
        selected = -1;
        rebuild();
    }

    public ExprNode getSelectedNode() {
        if (selected >= 0 && selected < visible.size()) {
            return visible.get(selected).node;
        }
        return null;
    }

    public void expandAll() {
        for (Node n : visible) {
            n.expanded = true;
        }
        rebuild();
    }

    private void rebuild() {
        visible.clear();
        if (root != null) {
            addNode(root, 0);
        }
        adapter.notifyDataSetChanged();
    }

    private void addNode(ExprNode n, int depth) {
        Node node = new Node(n, depth);
        visible.add(node);
        if (node.expanded) {
            for (int i = 0; i < GuiBridge.childCount(n); i++) {
                addNode(GuiBridge.child(n, i), depth + 1);
            }
        }
    }

    public void applyTheme() {
        adapter.notifyDataSetChanged();
        invalidate();
    }

    private class Adapter extends BaseAdapter {
        @Override
        public int getCount() {
            return visible.size();
        }

        @Override
        public Object getItem(int position) {
            return visible.get(position);
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
            c.configure(visible.get(position), position == selected);
            return c;
        }
    }

    private final class Cell extends View {
        private Node node;
        private boolean sel;
        private String expr = "";
        private String result;
        private boolean isError;
        private boolean subProgram;
        private final TextPaint textPaint = new TextPaint();

        Cell(Context ctx) {
            super(ctx);
            syncFont();
        }

        void syncFont() {
            String family = AppPrefs.getFontFamily();
            int size = AppPrefs.getFontSize();
            textPaint.setTypeface(Typeface.create(family, Typeface.NORMAL));
            textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                    size, getResources().getDisplayMetrics()));
        }

        void configure(Node n, boolean selectedCell) {
            this.node = n;
            this.sel = selectedCell;
            syncFont();
            this.subProgram = GuiBridge.subStates(n.node) != null;
            String[] parts = ProgRender.splitResult(n.node.toString());
            this.expr = parts[0];
            this.result = parts[1];
            this.isError = parts[2] != null;
        }

        @Override
        protected void onDraw(Canvas g) {
            Theme.Palette p = Theme.p();
            g.drawColor(sel ? p.listSelection : p.panelBg);
            float lh = textPaint.getTextSize() * 1.4f;
            float baseline = (getHeight() + textPaint.getTextSize() * 0.7f) / 2f;
            float x = FlatButton.dp(getContext(), 4) + node.depth * FlatButton.dp(getContext(), 14);

            // nyíl (ha van gyereke)
            int chev = node.expanded ? VsIcons.CHEVRON_DOWN : VsIcons.CHEVRON_RIGHT;
            if (GuiBridge.childCount(node.node) > 0) {
                android.graphics.drawable.Drawable d = VsIcons.icon(chev,
                        FlatButton.dp(getContext(), 13),
                        sel ? p.listSelectionFg : p.gutterFg);
                int ds = FlatButton.dp(getContext(), 13);
                d.setBounds((int) x, (int) (getHeight() - ds) / 2, (int) x + ds, (int) (getHeight() + ds) / 2);
                d.draw(g);
            }
            x += FlatButton.dp(getContext(), 15);

            if (isError) {
                android.graphics.drawable.Drawable d = VsIcons.icon(VsIcons.ERROR,
                        FlatButton.dp(getContext(), 13), p.error);
                int ds = FlatButton.dp(getContext(), 13);
                d.setBounds((int) x, (int) (getHeight() - ds) / 2, (int) x + ds, (int) (getHeight() + ds) / 2);
                d.draw(g);
                x += FlatButton.dp(getContext(), 17);
                textPaint.setColor(p.error);
                g.drawText(expr, x, baseline, textPaint);
            } else {
                if (subProgram) {
                    android.graphics.drawable.Drawable d = VsIcons.icon(VsIcons.STEP_INTO,
                            FlatButton.dp(getContext(), 12),
                            sel ? p.listSelectionFg : p.synFunction);
                    int ds = FlatButton.dp(getContext(), 12);
                    d.setBounds((int) x, (int) (getHeight() - ds) / 2, (int) x + ds, (int) (getHeight() + ds) / 2);
                    d.draw(g);
                    x += FlatButton.dp(getContext(), 16);
                }
                // kifejezés tokenenként színezve
                textPaint.setColor(sel ? p.listSelectionFg : p.editorFg);
                float px = x;
                java.util.List<PlangSyntax.Token> tokens = PlangSyntax.tokenize(expr);
                for (PlangSyntax.Token t : tokens) {
                    if (t.start >= expr.length() || t.end > expr.length()) {
                        continue;
                    }
                    int color = sel ? p.listSelectionFg : CodeEditorView.colorOf(p, t.kind);
                    textPaint.setColor(color);
                    float x1 = px + textPaint.measureText(expr.substring(0, t.start));
                    g.drawText(expr.substring(t.start, t.end), x1, baseline, textPaint);
                }
                textPaint.setColor(sel ? p.listSelectionFg : p.editorFg);
                px += textPaint.measureText(expr);
                if (result != null) {
                    String res = " = " + result;
                    textPaint.setTypeface(Typeface.create(textPaint.getTypeface(), Typeface.BOLD));
                    textPaint.setColor(sel ? p.listSelectionFg : p.synNumber);
                    g.drawText(res, px, baseline, textPaint);
                    textPaint.setTypeface(Typeface.create(textPaint.getTypeface(), Typeface.NORMAL));
                }
            }
        }

        @Override
        protected void onMeasure(int wspec, int hspec) {
            float lh = textPaint.getTextSize() * 1.4f + FlatButton.dp(getContext(), 3);
            setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), wspec), (int) lh);
        }
    }
}
