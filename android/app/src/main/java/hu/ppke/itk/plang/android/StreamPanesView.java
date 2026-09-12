package hu.ppke.itk.plang.android;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import hu.ppke.itk.plang.prog.StreamData;
import hu.ppke.itk.plang.prog.StreamKind;
import hu.ppke.itk.plang.prog.StreamState;

/**
 * A be- vagy kimeneti csatornák fülekkel ellátott panelje – a asztali
 * StreamTabs + StreamDocument portja. A már feldolgozott rész halványan,
 * az aktuális szakasz kiemelve látszik.
 */
public class StreamPanesView extends LinearLayout {

    private static final class Pane {
        String title;
        StreamKind kind = StreamKind.INPUT;
        TextView view;
        int start;    // az aktuális szakasz kezdete
        int length;   // az aktuális szakasz hossza
    }

    private final StreamKind kind;
    private final EditorTabBar tabs;
    private final FrameLayout pages;
    private final List<Pane> panes = new ArrayList<Pane>();
    private final TextView emptyLabel;
    private final FrameLayout emptyPage;

    public StreamPanesView(Context ctx, StreamKind kind) {
        super(ctx);
        setOrientation(VERTICAL);
        this.kind = kind;

        tabs = new EditorTabBar(ctx);
        tabs.setStyle(EditorTabBar.STYLE_PANEL);
        tabs.setListener(new EditorTabBar.Listener() {
            @Override
            public void tabSelected(String id) {
                showPage(id);
            }
            @Override
            public void tabClosed(String id) {}
        });

        emptyLabel = new TextView(ctx);
        emptyLabel.setText(kind == StreamKind.INPUT
            ? "Nincs bemeneti csatorna – értelmezd a programot"
            : "Nincs kimeneti csatorna – értelmezd a programot");
        emptyLabel.setGravity(Gravity.CENTER);
        emptyPage = new FrameLayout(ctx);
        emptyPage.addView(emptyLabel, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        pages = new FrameLayout(ctx);
        pages.addView(emptyPage, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        addView(tabs, new LayoutParams(LayoutParams.MATCH_PARENT, FlatButton.dp(ctx, 28)));
        addView(pages, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        applyTheme();
    }

    public int count() {
        return panes.size();
    }

    public String getTitleAt(int i) {
        return panes.get(i).title;
    }

    /** A megadott csatorna tartalma (bemenetnek). */
    public String getText(String title) {
        Pane p = pane(title);
        return p == null ? "" : p.view.getText().toString();
    }

    public void setEditable(boolean b) {
        for (Pane p : panes) {
            if (p.view instanceof EditText) {
                ((EditText) p.view).setEnabled(b);
                ((EditText) p.view).setFocusable(b);
                ((EditText) p.view).setFocusableInTouchMode(b);
            }
        }
    }

    /** A megjelenítési attribútumok alaphelyzetbe állítása (Stop után). */
    public void resetAttributes() {
        for (Pane p : panes) {
            applyNormal(p, 0, p.view.length());
        }
    }

    private Pane pane(String title) {
        for (Pane p : panes) {
            if (p.title.equals(title)) {
                return p;
            }
        }
        return null;
    }

    private void showPage(String id) {
        for (int i = 0; i < pages.getChildCount(); i++) {
            View c = pages.getChildAt(i);
            String tag = (String) c.getTag();
            c.setVisibility(id != null && id.equals(tag) ? VISIBLE : GONE);
        }
    }

    /**
     * A fülek szinkronizálása a program csatornáival – az asztali sync().
     */
    public void sync(Set<String> streams, boolean editable) {
        // a már nem szükséges fülek eltávolítása
        for (int i = panes.size() - 1; i >= 0; i--) {
            if (!streams.contains(panes.get(i).title)) {
                Pane p = panes.remove(i);
                tabs.removeTab(p.title);
                for (int j = 0; j < pages.getChildCount(); j++) {
                    if (p.title.equals(pages.getChildAt(j).getTag())) {
                        pages.removeViewAt(j);
                        break;
                    }
                }
            }
        }

        // az új csatornák felvétele (rendezett sorrendben)
        List<String> sorted = new ArrayList<String>(streams);
        java.util.Collections.sort(sorted);
        for (String name : sorted) {
            if (pane(name) != null) {
                continue;
            }
            Pane p = new Pane();
            p.title = name;
            p.kind = kind;
            Context ctx = getContext();
            if (kind == StreamKind.INPUT) {
                EditText ed = new EditText(ctx);
                ed.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Theme.p().editorBg));
                ed.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                                | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                ed.setGravity(Gravity.TOP | Gravity.START);
                ed.setHorizontalScrollBarEnabled(true);
                ed.setFocusable(editable);
                ed.setFocusableInTouchMode(editable);
                ed.setLongClickable(editable);
                ed.setCursorVisible(editable);
                p.view = ed;
            } else {
                TextView tv = new TextView(ctx);
                tv.setTextIsSelectable(true);
                p.view = tv;
            }
            p.view.setPadding(FlatButton.dp(ctx, 8), FlatButton.dp(ctx, 6),
                              FlatButton.dp(ctx, 8), FlatButton.dp(ctx, 6));
            p.view.setText("");
            p.view.setTag(name);
            pages.addView(p.view, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            panes.add(p);
            tabs.addTab(new EditorTabBar.Tab(name, name,
                    kind == StreamKind.INPUT ? VsIcons.INPUT : VsIcons.OUTPUT, false));
        }

        if (panes.isEmpty()) {
            tabs.clearTabs();
            tabs.setVisibility(GONE);
            showPage(null);
            emptyPage.setVisibility(VISIBLE);
        } else {
            emptyPage.setVisibility(GONE);
            tabs.select(panes.get(0).title);
            showPage(panes.get(0).title);
            /* Egyetlen csatorna esetén a fülcsík csak a fejlécet duplikálná. */
            tabs.setVisibility(panes.size() > 1 ? VISIBLE : GONE);
        }
        applyTheme();
    }

    /** A csatorna tartalmának beállítása (a StreamDocument.setStream portja). */
    public void setStream(String title, StreamData stream) {
        Pane p = pane(title);
        if (p == null) {
            return;
        }
        p.start = 0;
        p.length = 0;
        if (stream == null) {
            p.view.setText(new SpannableString(""));
            if (p.kind == StreamKind.INPUT) {
                EditText ed = (EditText) p.view;
                ed.setFocusable(true);
                ed.setFocusableInTouchMode(true);
                ed.setLongClickable(true);
                ed.setCursorVisible(true);
            }
        } else {
            String content = stream.getSection(0, stream.getLength());
            p.view.setText(new SpannableString(content));
            if (p.kind == StreamKind.INPUT) {
                EditText ed = (EditText) p.view;
                ed.setFocusable(false);
                ed.setFocusableInTouchMode(false);
                ed.setLongClickable(false);
                ed.setCursorVisible(false);
            }
        }
    }

    /**
     * Az adott állapot szerinti kiemelés (a StreamDocument.setState portja).
     */
    public void setState(String title, StreamState state) {
        Pane p = pane(title);
        if (p == null) {
            return;
        }
        Theme.Palette pal = Theme.p();
        CharSequence cs = p.view.getText();
        if (!(cs instanceof Spannable)) {
            SpannableString ss = new SpannableString(cs == null ? "" : cs.toString());
            p.view.setText(ss);
            cs = ss;
        }
        Spannable text = (Spannable) cs;
        if (state == null) {
            applyNormal(p, 0, text.length());
            return;
        }
        p.length = state.getLastSec();
        p.start = state.getPtr() - p.length;
        int len = text.length();
        int start = Math.max(0, Math.min(len, p.start));
        int curEnd = Math.max(start, Math.min(len, p.start + p.length));

        // alaphelyzet: minden normál
        applyNormal(p, 0, len);
        if (p.kind == StreamKind.INPUT) {
            // feldolgozott rész halványan
            CharacterStyle[] old;
            setRange(text, start, 0, new ForegroundColorSpan(pal.gutterFg));
        }
        // aktuális szakasz kiemelve
        setRange(text, start, curEnd - start,
                 new BackgroundColorSpan(Theme.isDark() ? 0xFF264F78 : 0xFFADD6FF));
    }

    private void applyNormal(Pane p, int from, int to) {
        if (to <= from) {
            return;
        }
        Theme.Palette pal = Theme.p();
        CharSequence cs2 = p.view.getText();
        if (!(cs2 instanceof Spannable)) {
            SpannableString ss = new SpannableString(cs2 == null ? "" : cs2.toString());
            p.view.setText(ss);
            cs2 = ss;
        }
        Spannable text = (Spannable) cs2;
        to = Math.min(text.length(), to);
        setRange(text, from, to - from, new ForegroundColorSpan(pal.editorFg));
    }

    private static void setRange(Spannable text, int start, int len, CharacterStyle style) {
        int s = Math.max(0, Math.min(text.length(), start));
        int e = Math.max(s, Math.min(text.length(), start + len));
        if (e > s) {
            text.setSpan(style, s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    public void setTextFont() {
        String family = AppPrefs.getFontFamily();
        int size = AppPrefs.getFontSize();
        Typeface tf = Typeface.create(family, Typeface.NORMAL);
        float ts = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                size, getResources().getDisplayMetrics());
        for (Pane p : panes) {
            p.view.setTypeface(tf);
            p.view.setTextSize(TypedValue.COMPLEX_UNIT_PX, ts);
        }
        emptyLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    }

    public void applyTheme() {
        Theme.Palette p = Theme.p();
        tabs.applyTheme();
        setBackgroundColor(p.editorBg);
        pages.setBackgroundColor(p.editorBg);
        emptyLabel.setTextColor(p.gutterFg);
        emptyPage.setBackgroundColor(p.editorBg);
        for (Pane pane : panes) {
            pane.view.setBackgroundColor(p.editorBg);
            pane.view.setTextColor(p.editorFg);
            if (pane.view instanceof EditText) {
                ((EditText) pane.view).setHighlightColor(p.selection);
                ((EditText) pane.view).setTextColor(p.editorFg);
            }
        }
        setTextFont();
        invalidate();
    }
}
