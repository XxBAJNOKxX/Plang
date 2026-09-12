package hu.ppke.itk.plang.android;

import android.text.Editable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Visszavonás/Újra kezelő EditText-hez – az asztali editor.PlangUndoManager
 * viselkedését utánozza: csoportosítja a gépelést idő- és szóhatár alapján,
 * így nem karakterenként lehet visszavonni.
 */
public class PlangUndoManager {

    /** Egy gépelési egység maximális ideje két karakter között (ms). */
    private static final long THRESHOLD = 900;

    private static final class Op {
        final boolean insert;
        final int offset;
        final String text;

        Op(boolean insert, int offset, String text) {
            this.insert = insert;
            this.offset = offset;
            this.text = text;
        }
    }

    public static final class Group {
        final List<Op> ops = new ArrayList<Op>();
    }

    private final ArrayDeque<Group> done = new ArrayDeque<Group>();
    private final ArrayDeque<Group> redo = new ArrayDeque<Group>();
    private Group current;

    private long lastTime = 0;
    private int lastOffset = -1;
    private int lastLength = 0;
    private boolean lastWasInsert = true;
    private String lastText = null;

    public void discardAllEdits() {
        done.clear();
        redo.clear();
        current = null;
    }

    /** Egy szövegművelet bejegyzése (a TextWatcher hívja). */
    public void onEdit(boolean insert, int offset, String text, int replacedLen) {
        long now = System.currentTimeMillis();
        redo.clear();

        String boundary = insert ? text : lastText;

        if (current == null) {
            current = new Group();
            current.ops.add(new Op(insert, offset, text));
            done.addLast(current);
            lastTime = now;
            lastOffset = offset;
            lastLength = replacedLen;
            lastWasInsert = insert;
            lastText = text;
            if (isWordBoundary(boundary)) {
                commit();
            }
            return;
        }

        boolean timeGap = (now - lastTime) > THRESHOLD;
        boolean typeChange = insert != lastWasInsert;
        boolean notContiguous = false;
        if (offset != -1 && lastOffset != -1) {
            if (insert) {
                notContiguous = offset != (lastOffset + lastLength);
            } else {
                notContiguous = !(offset == lastOffset || offset == lastOffset - 1 || offset + replacedLen == lastOffset);
            }
        }

        if (timeGap || typeChange || notContiguous) {
            current = new Group();
            current.ops.add(new Op(insert, offset, text));
            done.addLast(current);
        } else {
            current.ops.add(new Op(insert, offset, text));
        }

        lastTime = now;
        lastOffset = offset;
        lastLength = replacedLen;
        lastWasInsert = insert;
        lastText = text;

        if (isWordBoundary(boundary)) {
            commit();
        }
    }

    private boolean isWordBoundary(String text) {
        if (text == null || text.length() == 0) {
            return false;
        }
        if (text.length() == 1) {
            char c = text.charAt(0);
            if (Character.isWhitespace(c)) return true;
            return ",;:.()[]{}=+-*/<>!&|\"'".indexOf(c) >= 0;
        }
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public void commit() {
        current = null;
    }

    public boolean canUndo() {
        return !done.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }

    /** Visszavonás: visszaadja az utolsó csoportot, a hívó alkalmazza. */
    public Group popUndo() {
        if (done.isEmpty()) {
            return null;
        }
        Group g = done.removeLast();
        redo.addLast(g);
        current = null;
        return g;
    }

    /** Újra: visszaadja a következő csoportot, a hívó alkalmazza. */
    public Group popRedo() {
        if (redo.isEmpty()) {
            return null;
        }
        Group g = redo.removeLast();
        done.addLast(g);
        current = null;
        return g;
    }

    /**
     * Egy csoport visszavonásának alkalmazása az Editable-en.
     * A műveleteket visszafelé, fordított sorrendben viszi vissza.
     */
    public static void applyUndo(Editable e, Group g) {
        for (int i = g.ops.size() - 1; i >= 0; i--) {
            Op op = g.ops.get(i);
            if (op.insert) {
                int end = Math.min(e.length(), op.offset + op.text.length());
                if (op.offset >= 0 && op.offset <= end) {
                    e.delete(op.offset, end);
                }
            } else {
                int at = Math.min(e.length(), op.offset);
                e.insert(at, op.text);
            }
        }
    }

    /** Egy csoport újbóli alkalmazása az Editable-en. */
    public static void applyRedo(Editable e, Group g) {
        for (Op op : g.ops) {
            if (op.insert) {
                int at = Math.min(e.length(), op.offset);
                e.insert(at, op.text);
            } else {
                int end = Math.min(e.length(), op.offset + op.text.length());
                if (op.offset >= 0 && op.offset <= end) {
                    e.delete(op.offset, end);
                }
            }
        }
    }
}
