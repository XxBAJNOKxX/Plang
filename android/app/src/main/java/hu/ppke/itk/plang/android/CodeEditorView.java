package hu.ppke.itk.plang.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import hu.ppke.itk.plang.gui.editor.PlangSyntax;

/**
 * VS Code stílusú kódszerkesztő Androidon – a asztali editor.CodeEditor
 * portja: szintaxiskiemelés, aktuális sor kiemelése, behúzás-segédvonalak,
 * hiba- és futásjelölés, keresési találatok, töréspontok, undo/redo,
 * automatikus behúzás és kontextusfüggő kódkiegészítés.
 */
public class CodeEditorView extends EditText {

    private int tabSize = 2;
    private boolean showIndentGuides = true;

    private final TreeSet<Integer> errorLines = new TreeSet<Integer>();
    private final TreeSet<Integer> breakpoints = new TreeSet<Integer>();
    /* a sorszámsáv, amelyet a töréspont/hiba/futásjelölés változásakor
       frissíteni kell (a sáv külön nézet, önmagát nem rajtolja újra) */
    private android.view.View gutterView;
    private int runningLine = -1;

    private final PlangUndoManager undoManager = new PlangUndoManager();
    private boolean suspendUndo = false;
    private boolean suspendHighlight = false;

    private final List<int[]> findMatches = new ArrayList<int[]>();
    private int activeMatch = -1;
    private String lastNeedle = null;
    private boolean lastCaseSensitive = false;

    /* ---- IntelliSense ---- */
    private boolean autoComplete = true;
    private boolean suppressAutoComplete = false;
    private PopupWindow completionPopup;
    private ListView completionList;
    private List<String> completionItems = new ArrayList<String>();
    private String completionPrefix = "";
    private int lastCaretDot = 0;

    private final Paint decorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guidePaint = new Paint();
    private final android.os.Handler handler = new android.os.Handler();
    private TextChangeListener changeListener;

    /** Egyszerű figyelő a Workbenchnek (piszkos-jelzés, állapotsor). */
    public interface TextChangeListener {
        void onTextChanged();
    }

    public CodeEditorView(Context ctx) {
        super(ctx);
        setBackgroundResource(android.R.color.transparent);
        setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                     | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        setHorizontallyScrolling(true);
        int pad = FlatButton.dp(ctx, 8);
        setPadding(pad, FlatButton.dp(ctx, 6), pad, FlatButton.dp(ctx, 6));
        setGravity(Gravity.TOP | Gravity.START);
        applyTheme();
        updateEditorFont();

        addTextChangedListener(new TextWatcher() {
            private int removedStart;
            private String removedText;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                removedStart = start;
                removedText = count > 0 ? s.subSequence(start, start + count).toString() : null;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!suspendUndo) {
                    // csere esetén először a törlést, majd a beszúrást rögzítjük
                    if (before > 0) {
                        undoManager.onEdit(false, start, removedText, (int) before);
                    }
                    if (count > 0) {
                        undoManager.onEdit(true, start,
                                s.subSequence(start, start + count).toString(), (int) before);
                    }
                }
                // a kurzor mozgása bezárja a javaslatlistát (a fel/le kivétel)
                if (isCompletionActive() && Math.abs(count - before) > 1) {
                    hideCompletions();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (suspendHighlight) {
                    return;
                }
                textChanged();
                if (changeListener != null) {
                    changeListener.onTextChanged();
                }
                maybeAutoComplete();
            }
        });
    }

    public void setChangeListener(TextChangeListener l) {
        this.changeListener = l;
    }

    public PlangUndoManager getUndoManager() {
        return undoManager;
    }

    public void discardUndoHistory() {
        undoManager.discardAllEdits();
    }

    public void undo() {
        if (undoManager.canUndo()) {
            PlangUndoManager.Group g = undoManager.popUndo();
            if (g != null) {
                suspendUndo = true;
                try {
                    PlangUndoManager.applyUndo(getText(), g);
                    highlightAll();
                } finally {
                    suspendUndo = false;
                }
            }
        }
    }

    public void redo() {
        if (undoManager.canRedo()) {
            PlangUndoManager.Group g = undoManager.popRedo();
            if (g != null) {
                suspendUndo = true;
                try {
                    PlangUndoManager.applyRedo(getText(), g);
                    highlightAll();
                } finally {
                    suspendUndo = false;
                }
            }
        }
    }

    /* =================== szöveg beállítása =================== */

    /** A teljes szöveg cseréje programbetöltéskor (undo-történet nélkül). */
    public void setProgramText(String text) {
        suspendUndo = true;
        suspendHighlight = true;
        try {
            setText(text);
            setSelection(0);
            undoManager.discardAllEdits();
        } finally {
            suspendHighlight = false;
            suspendUndo = false;
        }
        highlightAll();
    }

    public String programText() {
        return getText().toString();
    }

    /** Van kijelölt szöveg? */
    public boolean hasSelection() {
        return getSelectionStart() != getSelectionEnd()
               && getSelectionStart() >= 0 && getSelectionEnd() <= length();
    }

    /** A kijelölt szöveg, vagy null. */
    public String selectedText() {
        if (!hasSelection()) {
            return null;
        }
        return getText().toString().substring(getSelectionStart(), getSelectionEnd());
    }

    /* =================== vonal/kurzor segédek =================== */

    public int lineCount() {
        Layout l = getLayout();
        return l == null ? 1 : l.getLineCount();
    }

    /** Az aktuális sor száma (1-alapú). */
    public int caretLine() {
        int pos = Math.max(0, Math.min(length(), getSelectionStart()));
        Layout l = getLayout();
        return (l == null ? 0 : l.getLineForOffset(pos)) + 1;
    }

    /** Az aktuális oszlop (1-alapú). */
    public int caretColumn() {
        int pos = Math.max(0, Math.min(length(), getSelectionStart()));
        Layout l = getLayout();
        if (l == null) {
            return 1;
        }
        int line = l.getLineForOffset(pos);
        return pos - l.getLineStart(line) + 1;
    }

    public String lineText(int line) {
        Layout l = getLayout();
        if (l == null || line < 0 || line >= l.getLineCount()) {
            return "";
        }
        int s = l.getLineStart(line);
        int e = l.getLineEnd(line);
        // a sorzáró \n nem része a sornak
        if (e > s && getText().charAt(e - 1) == '\n') {
            e--;
        }
        return getText().toString().substring(s, e);
    }

    public int lineStartOffset(int line) {
        Layout l = getLayout();
        if (l == null) {
            return 0;
        }
        line = Math.max(0, Math.min(line, l.getLineCount() - 1));
        return l.getLineStart(line);
    }

    /** A megadott sor elejére ugrik (0-alapú). */
    public void gotoLine(int line) {
        Layout l = getLayout();
        if (l == null || line < 0 || line >= l.getLineCount()) {
            return;
        }
        int off = l.getLineStart(line);
        setSelection(off);
        post(new Runnable() {
            @Override
            public void run() {
                bringCaretIntoView();
            }
        });
    }

    private void bringCaretIntoView() {
        Layout l = getLayout();
        if (l == null) {
            return;
        }
        int pos = Math.max(0, Math.min(length(), getSelectionStart()));
        int line = l.getLineForOffset(pos);
        int yTop = l.getLineTop(line) + getPaddingTop();
        int yBottom = l.getLineBottom(line) + getPaddingTop();
        if (yTop < getScrollY()) {
            scrollTo(0, yTop);
        } else if (yBottom > getScrollY() + getHeight() - getPaddingBottom()) {
            scrollTo(0, Math.max(0, yBottom - getHeight() + getPaddingBottom()));
        }
    }

    /* =================== hibák / futás / töréspontok =================== */

    public void setErrorLines(int[] lines) {
        errorLines.clear();
        if (lines != null) {
            for (int l : lines) {
                errorLines.add(l);
            }
        }
        invalidate();
        invalidateGutter();
    }

    public int[] getErrorLines() {
        int[] out = new int[errorLines.size()];
        int i = 0;
        for (Integer l : errorLines) {
            out[i++] = l;
        }
        return out;
    }

    public boolean isErrorLine(int line) {
        return errorLines.contains(line);
    }

    public void setRunningLine(int line) {
        if (runningLine != line) {
            runningLine = line;
            invalidate();
            invalidateGutter();
        }
    }

    /** A sorszámsáv nézetének megadása (a jelölések rajta jelennek meg). */
    public void setGutterView(android.view.View v) {
        gutterView = v;
    }

    private void invalidateGutter() {
        if (gutterView != null) {
            gutterView.invalidate();
        }
    }

    public int getRunningLine() {
        return runningLine;
    }

    public void toggleBreakpoint(int line) {
        if (breakpoints.contains(line)) {
            breakpoints.remove(line);
        } else {
            breakpoints.add(line);
        }
        invalidate();
        invalidateGutter();
    }

    public boolean isBreakpoint(int line) {
        return breakpoints.contains(line);
    }

    public int[] getBreakpoints() {
        int[] out = new int[breakpoints.size()];
        int i = 0;
        for (Integer l : breakpoints) {
            out[i++] = l;
        }
        return out;
    }

    public void clearBreakpoints() {
        breakpoints.clear();
        invalidate();
        invalidateGutter();
    }

    /* =================== keresés/csere =================== */

    public int search(String needle, boolean caseSensitive) {
        lastNeedle = needle;
        lastCaseSensitive = caseSensitive;
        recomputeMatches();
        return findMatches.size();
    }

    private void recomputeMatches() {
        findMatches.clear();
        activeMatch = -1;
        String needle = lastNeedle;
        if (needle == null || needle.length() == 0) {
            invalidate();
            return;
        }
        String hay = getText().toString();
        String n = lastCaseSensitive ? needle : needle.toLowerCase();
        String h = lastCaseSensitive ? hay : hay.toLowerCase();
        int at = 0;
        while ((at = h.indexOf(n, at)) >= 0) {
            findMatches.add(new int[]{at, at + n.length()});
            at += n.length();
        }
        invalidate();
    }

    public int matchCount() {
        return findMatches.size();
    }

    public int activeMatchIndex() {
        return activeMatch;
    }

    public void nextMatch() {
        if (findMatches.isEmpty()) {
            return;
        }
        activeMatch = (activeMatch + 1) % findMatches.size();
        scrollToMatch();
    }

    public void prevMatch() {
        if (findMatches.isEmpty()) {
            return;
        }
        activeMatch = (activeMatch - 1 + findMatches.size()) % findMatches.size();
        scrollToMatch();
    }

    public boolean replaceCurrent(String replacement) {
        if (activeMatch < 0 || activeMatch >= findMatches.size()) {
            return false;
        }
        int[] m = findMatches.get(activeMatch);
        suspendUndo = true;
        try {
            getText().replace(m[0], m[1], replacement);
        } finally {
            suspendUndo = false;
        }
        recomputeMatches();
        return true;
    }

    public int replaceAll(String needle, String replacement, boolean caseSensitive) {
        String hay = getText().toString();
        String n = caseSensitive ? needle : needle.toLowerCase();
        String h = caseSensitive ? hay : hay.toLowerCase();
        int count = 0;
        SpannableStringBuilder b = new SpannableStringBuilder(hay);
        int at = 0;
        int delta = 0;
        while ((at = h.indexOf(n, at)) >= 0) {
            b.replace(at + delta, at + delta + n.length(), replacement);
            delta += replacement.length() - n.length();
            at += n.length();
            count++;
        }
        if (count > 0) {
            suspendUndo = true;
            try {
                setText(b);
            } finally {
                suspendUndo = false;
            }
            highlightAll();
        }
        return count;
    }

    public void clearSearch() {
        lastNeedle = null;
        findMatches.clear();
        activeMatch = -1;
        invalidate();
    }

    private void scrollToMatch() {
        if (activeMatch < 0 || activeMatch >= findMatches.size()) {
            return;
        }
        int[] m = findMatches.get(activeMatch);
        setSelection(m[0]);
        post(new Runnable() {
            @Override
            public void run() {
                bringCaretIntoView();
            }
        });
        invalidate();
    }

    private void textChanged() {
        invalidate();
        recomputeMatches();
        scheduleHighlight();
    }

    /* =================== szintaxiskiemelés =================== */

    private final Runnable highlightRunnable = new Runnable() {
        @Override
        public void run() {
            highlightAll();
        }
    };

    private void scheduleHighlight() {
        handler.removeCallbacks(highlightRunnable);
        handler.postDelayed(highlightRunnable, 120);
    }

    /** A teljes dokumentum újraszínezése (soranként tokenizálva). */
    public void highlightAll() {
        if (suspendHighlight) {
            return;
        }
        suspendHighlight = true;
        try {
            Editable e = getText();
            int len = e.length();
            ForegroundColorSpan[] fg = e.getSpans(0, len, ForegroundColorSpan.class);
            for (ForegroundColorSpan s : fg) {
                if (s instanceof TokenSpan) {
                    e.removeSpan(s);
                }
            }
            Layout l = getLayout();
            int lines = (l == null) ? lineCount() : l.getLineCount();
            for (int i = 0; i < lines; i++) {
                String line = lineText(i);
                List<PlangSyntax.Token> tokens = PlangSyntax.tokenize(line);
                Theme.Palette p = Theme.p();
                int start = lineStartOffset(i);
                for (PlangSyntax.Token t : tokens) {
                    int color = colorOf(p, t.kind);
                    if (color != p.editorFg) {
                        e.setSpan(new TokenSpan(color), start + t.start, start + t.end,
                                  Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        } finally {
            suspendHighlight = false;
        }
        invalidate();
    }

    /** Jelszintű szín (a asztali ProgLineRenderer színképezésének felelője). */
    static int colorOf(Theme.Palette p, int kind) {
        switch (kind) {
            case PlangSyntax.KW: return p.synKeyword;
            case PlangSyntax.CTRL: return p.synControl;
            case PlangSyntax.TYPE_T: return p.synType;
            case PlangSyntax.STRING: return p.synString;
            case PlangSyntax.NUMBER: return p.synNumber;
            case PlangSyntax.COMMENT: return p.synComment;
            case PlangSyntax.FUNC: return p.synFunction;
            case PlangSyntax.CONST: return p.synConstant;
            case PlangSyntax.OPERATOR: return p.synOperator;
            default: return p.editorFg;
        }
    }

    /** Az azonosításra használt saját span-osztály (a törléshez). */
    private static final class TokenSpan extends ForegroundColorSpan {
        TokenSpan(int color) {
            super(color);
        }
    }

    /* =================== kirajzolás =================== */

    @Override
    protected void onDraw(Canvas g) {
        Theme.Palette p = Theme.p();
        Layout l = getLayout();
        if (l != null) {
            /* A TextView a szöveget (compoundPaddingLeft, extendedPaddingTop)
               eltolással rajzolja, tehát a Layout y=0 vonala a képernyőn a
               paddingnél kezdődik: minden függőleges koordinátához a
               paddingTop-et hozzá kell adni, a vízszintesekhez a paddingLeft-et.
               A vászon ekkor már -scrollX/-scrollYra van tolatva, ezért a
               teljes szélesség lefedése [scrollX, scrollX+szélesség]. */
            final int padL = getPaddingLeft();
            final int padT = getPaddingTop();
            final int sx = getScrollX();

            // hibás sorok háttere
            for (Integer el : errorLines) {
                int line = el.intValue();
                if (line < l.getLineCount()) {
                    int y = l.getLineTop(line) + padT;
                    int y2 = l.getLineBottom(line) + padT;
                    decorPaint.setColor(p.errorLineBg);
                    g.drawRect(sx, y, sx + getWidth(), y2, decorPaint);
                }
            }
            // a futás aktuális sora
            if (runningLine >= 0 && runningLine < l.getLineCount()) {
                int y = l.getLineTop(runningLine) + padT;
                int y2 = l.getLineBottom(runningLine) + padT;
                decorPaint.setColor(Theme.alpha(p.warning, 45));
                g.drawRect(sx, y, sx + getWidth(), y2, decorPaint);
            }
            // aktuális (kurzoros) sor
            int selS = Math.max(0, Math.min(length(), getSelectionStart()));
            int selE = Math.max(0, Math.min(length(), getSelectionEnd()));
            int caretLine = l.getLineForOffset(selS);
            if (selS == selE && !errorLines.contains(caretLine) && caretLine != runningLine) {
                int y = l.getLineTop(caretLine) + padT;
                int y2 = l.getLineBottom(caretLine) + padT;
                decorPaint.setColor(p.currentLine);
                g.drawRect(sx, y, sx + getWidth(), y2, decorPaint);
            }

            // keresési találatok
            for (int i = 0; i < findMatches.size(); i++) {
                int[] m = findMatches.get(i);
                if (m[0] > length()) {
                    continue;
                }
                int line1 = l.getLineForOffset(m[0]);
                int line2 = l.getLineForOffset(Math.min(length(), m[1]));
                decorPaint.setColor(i == activeMatch ? p.findMatchActive : p.findMatch);
                for (int ln = line1; ln <= line2; ln++) {
                    int s = Math.max(l.getLineStart(ln), m[0]);
                    int e = Math.min(l.getLineEnd(ln), m[1]);
                    if (e <= s) {
                        continue;
                    }
                    float x1 = l.getPrimaryHorizontal(s) + padL;
                    float x2 = l.getPrimaryHorizontal(e) + padL;
                    int y = l.getLineTop(ln) + padT;
                    int y2 = l.getLineBottom(ln) + padT;
                    g.drawRect(x1, y, x2, y2, decorPaint);
                }
            }

            // behúzás-segédvonalak
            if (showIndentGuides) {
                guidePaint.setColor(p.indentGuide);
                guidePaint.setStrokeWidth(FlatButton.dp(getContext(), 1));
                float charW = getPaint().measureText(" ");
                for (int i = 0; i < l.getLineCount(); i++) {
                    String line = lineText(i);
                    if (line.length() == 0) {
                        continue;
                    }
                    int indent = 0;
                    while (indent < line.length() && line.charAt(indent) == ' ') {
                        indent++;
                    }
                    if (indent == 0) {
                        continue;
                    }
                    int y = l.getLineTop(i) + padT;
                    int y2 = l.getLineBottom(i) + padT;
                    for (int cc = tabSize; cc < indent; cc += tabSize) {
                        float x = padL + cc * charW;
                        g.drawLine(x, y, x, y2, guidePaint);
                    }
                }
            }
        }
        super.onDraw(g);
    }

    /* =================== megjelenés =================== */

    public void applyTheme() {
        Theme.Palette p = Theme.p();
        setTextColor(p.editorFg);
        setHighlightColor(p.selection);
        setHintTextColor(p.gutterFg);
        // a tokenek színe az aktuális palettáról rajzolódik: téma váltásnál
        // azonnal újraszínezzük, ne csak a következő gépeléskor
        highlightAll();
        invalidate();
    }

    public void updateEditorFont() {
        String family = AppPrefs.getFontFamily();
        int size = AppPrefs.getFontSize();
        Typeface tf = Typeface.create(family, Typeface.NORMAL);
        setTypeface(tf);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        if (getLayout() != null) {
            highlightAll();
        }
        invalidate();
    }

    public void setShowIndentGuides(boolean b) {
        showIndentGuides = b;
        invalidate();
    }

    public boolean isShowIndentGuides() {
        return showIndentGuides;
    }

    public int getTabSize() {
        return tabSize;
    }

    /* =================== szerkesztési műveletek =================== */

    private static String spaces(int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) {
            b.append(' ');
        }
        return b.toString();
    }

    private void insertText(int at, String text) {
        suspendUndo = false;
        getText().insert(Math.min(length(), Math.max(0, at)), text);
    }

    private void removeText(int start, int len) {
        int e = Math.min(length(), start + len);
        if (start >= 0 && start < e) {
            getText().delete(start, e);
        }
    }

    /** Enter: nyitott javaslat elfogadása, egyébként intelligens újsor. */
    public void smartNewline() {
        try {
            int pos = Math.max(0, Math.min(length(), getSelectionEnd()));
            Layout l = getLayout();
            int li = l.getLineForOffset(pos);
            String line = lineText(li);

            int indent = 0;
            while (indent < line.length() && line.charAt(indent) == ' ') {
                indent++;
            }
            int[] eff = PlangSyntax.indentEffect(line);
            int nextIndent = Math.max(0, indent + eff[1] * tabSize);

            int at = Math.max(0, Math.min(length(), getSelectionEnd()));
            getText().replace(Math.min(at, getSelectionStart()), at, "\n" + spaces(nextIndent));
        } catch (Exception e) {
            int at = Math.max(0, Math.min(length(), getSelectionEnd()));
            getText().replace(Math.min(at, getSelectionStart()), at, "\n");
        }
    }

    /** Tab / Shift+Tab: kijelölt sorok blokk-behúzása. */
    public void shiftLines(boolean right) {
        try {
            Layout l = getLayout();
            int a = l.getLineForOffset(Math.max(0, Math.min(length(), getSelectionStart())));
            int b = l.getLineForOffset(Math.max(0, Math.min(length(), Math.max(getSelectionStart(), getSelectionEnd()) - 1)));
            for (int i = a; i <= b; i++) {
                int s = l.getLineStart(i);
                if (right) {
                    insertText(s, spaces(tabSize));
                } else {
                    String line = lineText(i);
                    int rm = 0;
                    while (rm < tabSize && rm < line.length() && line.charAt(rm) == ' ') {
                        rm++;
                    }
                    if (rm > 0) {
                        removeText(s, rm);
                    }
                }
            }
        } catch (Exception e) {
            // figyelmen kívül hagyható
        }
    }

    /** Ctrl+/ : sor ki- és bekommentelése. */
    public void toggleComment() {
        try {
            Layout l = getLayout();
            int a = l.getLineForOffset(Math.max(0, Math.min(length(), getSelectionStart())));
            int b = l.getLineForOffset(Math.max(0, Math.min(length(), Math.max(getSelectionStart(), getSelectionEnd()) - 1)));

            boolean allCommented = true;
            for (int i = a; i <= b; i++) {
                String line = lineText(i);
                String t = line.trim();
                if (t.length() > 0 && !t.startsWith("**")) {
                    allCommented = false;
                    break;
                }
            }

            for (int i = a; i <= b; i++) {
                String line = lineText(i);
                if (line.trim().length() == 0) {
                    continue;
                }
                int s = l.getLineStart(i);
                if (allCommented) {
                    int idx = line.indexOf("**");
                    if (idx >= 0) {
                        int rm = 2;
                        if (idx + 2 < line.length() && line.charAt(idx + 2) == ' ') {
                            rm = 3;
                        }
                        removeText(s + idx, rm);
                    }
                } else {
                    int indent = 0;
                    while (indent < line.length() && line.charAt(indent) == ' ') {
                        indent++;
                    }
                    insertText(s + indent, "** ");
                }
            }
        } catch (Exception e) {
            // figyelmen kívül hagyható
        }
    }

    /** Ctrl+D : sor megkettőzése. */
    public void duplicateLine() {
        try {
            Layout l = getLayout();
            int li = l.getLineForOffset(Math.max(0, Math.min(length(), getSelectionEnd())));
            String line = lineText(li);
            insertText(l.getLineEnd(li), "\n" + line);
        } catch (Exception e) {
            // figyelmen kívül hagyható
        }
    }

    /** Alt+Fel / Alt+Le : sor mozgatása. */
    public void moveLine(int dir) {
        try {
            Layout l = getLayout();
            int li = l.getLineForOffset(Math.max(0, Math.min(length(), getSelectionEnd())));
            int target = li + dir;
            if (target < 0 || target >= l.getLineCount()) {
                return;
            }
            String line = lineText(li);
            int lineStart = l.getLineStart(li);
            int caretCol = Math.max(0, Math.min(length(), getSelectionEnd())) - lineStart;

            boolean last = (li == l.getLineCount() - 1);
            removeText(lineStart, line.length() + (last ? 0 : 1));
            int insertAt;
            if (dir < 0) {
                insertAt = l.getLineStart(target);
            } else {
                insertAt = (target >= l.getLineCount()) ? length() : l.getLineStart(target);
            }
            // egymás utáni sorok cseréje: a törölt sor elé/mögé szúrjuk
            if (dir > 0 && target < l.getLineCount()) {
                insertText(insertAt, lineText(target) + "\n" + line);
            } else if (dir < 0) {
                insertText(insertAt, line + "\n");
            } else {
                insertText(insertAt, "\n" + line);
            }
            int newCaret = Math.min(length(), (dir < 0 ? lineStartOffset(target) : lineStartOffset(target)) + caretCol);
            setSelection(newCaret);
        } catch (Exception e) {
            // figyelmen kívül hagyható
        }
    }

    /* =================== kódkiegészítés =================== */

    public void setAutoComplete(boolean b) {
        autoComplete = b;
        if (!b) {
            hideCompletions();
        }
    }

    public boolean isAutoComplete() {
        return autoComplete;
    }

    /** A kurzor előtti azonosítótöredék a jelenlegi sorban. */
    public String wordPrefixAtCaret() {
        try {
            int pos = Math.max(0, Math.min(length(), getSelectionStart()));
            Layout l = getLayout();
            int line = l.getLineForOffset(pos);
            int s = l.getLineStart(line);
            String text = getText().toString().substring(s, pos);
            int i = text.length();
            while (i > 0 && PlangSyntax.isWordChar(text.charAt(i - 1))) {
                i--;
            }
            return text.substring(i);
        } catch (Exception e) {
            return "";
        }
    }

    /** A dokumentumban deklarált azonosítók (változók, program-, eljárás-, függvénynevek). */
    public List<String> collectDeclaredNames() {
        LinkedHashSet<String> out = new LinkedHashSet<String>();
        String text = getText().toString();
        String[] lines = text.split("\n", -1);
        for (String value : lines) {
            String t = value.trim();
            String low = PlangSyntax.deacc(t);

            String[] heads = { "program ", "eljaras ", "fuggveny " };
            for (String head : heads) {
                if (low.startsWith(head)) {
                    String name = t.substring(head.length()).trim();
                    if (isIdent(name)) {
                        out.add(name);
                    }
                }
            }

            int colon = t.indexOf(':');
            if (colon > 0) {
                String left = t.substring(0, colon).trim();
                String right = PlangSyntax.deacc(t.substring(colon + 1).trim());
                if (isTypeName(right) && left.indexOf(' ') < 0 && left.length() > 0) {
                    java.util.StringTokenizer st = new java.util.StringTokenizer(left, ",");
                    while (st.hasMoreTokens()) {
                        String nm = st.nextToken().trim();
                        if (isIdent(nm)) {
                            out.add(nm);
                        }
                    }
                }
            }
        }
        return new ArrayList<String>(out);
    }

    private static boolean isTypeName(String deaccRight) {
        String[] types = { "egesz", "valos", "szoveg", "karakter", "logikai", "befajl", "kifajl" };
        for (String type : types) {
            if (deaccRight.equals(type) || deaccRight.startsWith(type + " ")
                || deaccRight.startsWith(type + "[")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isIdent(String s) {
        if (s == null || s.length() == 0) {
            return false;
        }
        if (!(Character.isLetter(s.charAt(0)) || s.charAt(0) == '_')) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!PlangSyntax.isWordChar(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** Kontextus: típusnév várt (deklarációban a {@code :} után). */
    public static final int CTX_TYPE = 0;
    /** Kontextus: kifejezés/utasítás. */
    public static final int CTX_EXPR = 1;

    public int completionContext() {
        try {
            int pos = Math.max(0, Math.min(length(), getSelectionStart()));
            Layout l = getLayout();
            int line = l.getLineForOffset(pos);
            int s = l.getLineStart(line);
            String before = getText().toString().substring(s, pos);
            int ci = before.lastIndexOf(':');
            if (ci >= 0 && !(ci + 1 < before.length() && before.charAt(ci + 1) == '=')) {
                String left = before.substring(0, ci).trim();
                if (leftLooksLikeDeclNames(left)) {
                    return CTX_TYPE;
                }
            }
        } catch (Exception e) {
            // kontextus nélkül kifejezést feltételezünk
        }
        return CTX_EXPR;
    }

    private static boolean leftLooksLikeDeclNames(String left) {
        if (left.length() == 0) {
            return false;
        }
        java.util.StringTokenizer st = new java.util.StringTokenizer(left, ",");
        if (!st.hasMoreTokens()) {
            return false;
        }
        while (st.hasMoreTokens()) {
            String t = st.nextToken().trim();
            if (!isIdent(t)) {
                return false;
            }
            if (PlangSyntax.KEYWORD.contains(PlangSyntax.deacc(t))) {
                return false;
            }
        }
        return true;
    }

    /** Kontextusfüggő javaslatok. */
    public List<String> contextualCompletions(String prefix) {
        List<String> out = new ArrayList<String>();
        LinkedHashSet<String> seen = new LinkedHashSet<String>();
        String want = PlangSyntax.deacc(prefix == null ? "" : prefix);

        if (completionContext() == CTX_TYPE) {
            for (String c : PlangSyntax.COMPLETIONS) {
                if (catOf(c) == PlangSyntax.TYPE_T && match(c, want) && seen.add(PlangSyntax.deacc(c))) {
                    out.add(c);
                }
            }
            return out;
        }

        for (String id : collectDeclaredNames()) {
            if (match(id, want) && seen.add(PlangSyntax.deacc(id))) {
                out.add(id);
            }
        }
        for (String c : PlangSyntax.COMPLETIONS) {
            int cat = catOf(c);
            if ((cat == PlangSyntax.FUNC || cat == PlangSyntax.CONST) && match(c, want) && seen.add(PlangSyntax.deacc(c))) {
                out.add(c);
            }
        }
        for (String c : PlangSyntax.COMPLETIONS) {
            int cat = catOf(c);
            if ((cat == PlangSyntax.CTRL || cat == PlangSyntax.KW) && match(c, want) && seen.add(PlangSyntax.deacc(c))) {
                out.add(c);
            }
        }
        return out;
    }

    private static boolean match(String cand, String want) {
        return want.length() == 0 || PlangSyntax.deacc(cand).startsWith(want);
    }

    /** Egy megjelenített kulcsszó szemantikai kategóriája. */
    private static int catOf(String display) {
        String w = PlangSyntax.deacc(display);
        if (w.endsWith(":")) {
            w = w.substring(0, w.length() - 1);
        }
        if (PlangSyntax.TYPE.contains(w)) return PlangSyntax.TYPE_T;
        if (PlangSyntax.FUNCTION.contains(w)) return PlangSyntax.FUNC;
        if (PlangSyntax.CONSTANT.contains(w)) return PlangSyntax.CONST;
        if (PlangSyntax.CONTROL.contains(w) || PlangSyntax.WORD_OPERATOR.contains(w)) {
            return PlangSyntax.CTRL;
        }
        if (PlangSyntax.KEYWORD.contains(w)) return PlangSyntax.KW;
        return PlangSyntax.PLAIN;
    }

    /** A megadott töredékre illeszkedő összes kulcsszó. */
    public static List<String> completionsFor(String prefix) {
        List<String> out = new ArrayList<String>();
        if (prefix == null) {
            prefix = "";
        }
        String want = PlangSyntax.deacc(prefix);
        for (String c : PlangSyntax.COMPLETIONS) {
            if (want.length() == 0 || PlangSyntax.deacc(c).startsWith(want)) {
                out.add(c);
            }
        }
        return out;
    }

    /** A kiegészítő lista megjelenítése (explicit = Ctrl+Space). */
    public void showCompletions(boolean explicit) {
        String prefix = wordPrefixAtCaret();
        List<String> items = contextualCompletions(prefix);
        if (items.isEmpty()) {
            hideCompletions();
            return;
        }
        if (items.size() == 1 && !explicit) {
            String only = items.get(0);
            if (only.equalsIgnoreCase(prefix) || PlangSyntax.deacc(only).equals(PlangSyntax.deacc(prefix))) {
                hideCompletions();
                return;
            }
        }
        completionPrefix = prefix;
        completionItems = items;

        hidePopupOnly();
        Context ctx = getContext();
        completionList = new ListView(ctx);
        completionList.setAdapter(new ArrayAdapter<String>(ctx, android.R.layout.simple_list_item_1, items));
        completionList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        completionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                acceptCompletionAt(position);
            }
        });

        int rowH = FlatButton.dp(ctx, 30);
        int maxRows = Math.min(8, items.size());
        int w = FlatButton.dp(ctx, 210);
        completionPopup = new PopupWindow(completionList, w, rowH * maxRows + FlatButton.dp(ctx, 8), explicit);
        completionPopup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Theme.p().widgetBg));
        completionPopup.setOutsideTouchable(true);
        completionPopup.setTouchable(true);
        completionPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                if (completionPopup != null && completionPopup.isShowing()) {
                    // normal kilépés
                }
                completionPopup = null;
                completionList = null;
            }
        });

        completionList.setSelection(0);

        // pozicionálás a kurzor alá
        try {
            Layout l = getLayout();
            int pos = Math.max(0, Math.min(length(), getSelectionStart()));
            int line = l.getLineForOffset(pos);
            float x = l.getPrimaryHorizontal(pos) + getPaddingLeft();
            int y = l.getLineBottom(line) + getPaddingTop();
            completionPopup.showAsDropDown(this,
                (int) (x - getScrollX()),
                (int) (y - getScrollY() + FlatButton.dp(ctx, 4)));
        } catch (Exception e) {
            completionPopup.showAsDropDown(this, 0, 0);
        }
    }

    private void hidePopupOnly() {
        if (completionPopup != null) {
            completionList = null;
            PopupWindow pw = completionPopup;
            completionPopup = null;
            try {
                pw.dismiss();
            } catch (Exception e) {
                // nem kritikus
            }
        }
    }

    public boolean isCompletionActive() {
        return completionPopup != null && completionPopup.isShowing();
    }

    public boolean moveCompletion(int delta) {
        if (completionList == null) {
            return false;
        }
        int n = completionItems.size();
        if (n <= 0) {
            return false;
        }
        int i = completionList.getCheckedItemPosition();
        if (i < 0 || i == AdapterView.INVALID_POSITION) {
            i = completionList.getSelectedItemPosition();
        }
        if (i < 0) {
            i = 0;
        }
        int next = Math.max(0, Math.min(n - 1, i + delta));
        completionList.setSelection(next);
        completionList.setItemChecked(next, true);
        return true;
    }

    public void acceptCompletion() {
        if (completionList != null && isCompletionActive()) {
            int i = completionList.getCheckedItemPosition();
            if (i < 0) {
                i = 0;
            }
            acceptCompletionAt(i);
        }
    }

    private void acceptCompletionAt(int index) {
        if (index < 0 || index >= completionItems.size()) {
            hideCompletions();
            return;
        }
        String word = completionItems.get(index);
        hideCompletions();
        applyCompletion(word, completionPrefix);
    }

    public void hideCompletions() {
        hidePopupOnly();
    }

    private void applyCompletion(String word, String prefix) {
        suppressAutoComplete = true;
        try {
            int pos = Math.max(0, Math.min(length(), getSelectionStart()));
            int s = pos - prefix.length();
            if (s >= 0 && s <= pos) {
                getText().replace(s, pos, word);
                setSelection(s + word.length());
            }
        } finally {
            suppressAutoComplete = false;
        }
    }

    /** Gépelés közbeni automatikus felbukkanás. */
    private void maybeAutoComplete() {
        if (!autoComplete || suppressAutoComplete) {
            return;
        }
        String prefix = wordPrefixAtCaret();
        if (prefix.length() >= 2) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (!suppressAutoComplete) {
                        showCompletions(false);
                    }
                }
            });
        } else if (isCompletionActive()) {
            hideCompletions();
        }
    }

    /* =================== hardveres billentyűk =================== */

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isCompletionActive()) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                case KeyEvent.KEYCODE_TAB:
                    acceptCompletion();
                    return true;
                case KeyEvent.KEYCODE_ESCAPE:
                    hideCompletions();
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                    moveCompletion(-1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    moveCompletion(1);
                    return true;
                case KeyEvent.KEYCODE_PAGE_UP:
                    moveCompletion(-8);
                    return true;
                case KeyEvent.KEYCODE_PAGE_DOWN:
                    moveCompletion(8);
                    return true;
            }
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                smartNewline();
                return true;
            case KeyEvent.KEYCODE_TAB:
                if (getSelectionStart() != getSelectionEnd()) {
                    shiftLines(true);
                } else {
                    int at = Math.max(0, Math.min(length(), getSelectionEnd()));
                    getText().replace(Math.min(at, getSelectionStart()), at, spaces(tabSize));
                }
                return true;
            case KeyEvent.KEYCODE_SLASH:
                if (event.isCtrlPressed()) {
                    toggleComment();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_D:
                if (event.isCtrlPressed()) {
                    duplicateLine();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (event.isAltPressed()) {
                    moveLine(-1);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (event.isAltPressed()) {
                    moveLine(1);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_SPACE:
                if (event.isCtrlPressed()) {
                    showCompletions(true);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_Z:
                if (event.isCtrlPressed()) {
                    undo();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_Y:
                if (event.isCtrlPressed()) {
                    redo();
                    return true;
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }
}
