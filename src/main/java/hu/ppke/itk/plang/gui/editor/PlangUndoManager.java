package hu.ppke.itk.plang.gui.editor;

import javax.swing.event.DocumentEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

/**
 * Undo/Redo kezelő, amely:
 * - szűri a CHANGE típusú (szintaxis-színezés) editeket,
 * - csoportosítja a gépelést idő- és szóhatár alapján,
 *   így nem karakterenként lehet visszavonni.
 */
public class PlangUndoManager extends UndoManager {

    private static final long serialVersionUID = 1L;

    /** Egy gépelési egység maximális ideje két karakter között (ms). */
    private static final long THRESHOLD = 900;

    private CompoundEdit current;
    private long lastTime = 0;
    private int lastOffset = -1;
    private int lastLength = 0;
    private boolean lastWasInsert = true;

    public PlangUndoManager() {
        super();
    }

    @Override
    public synchronized void discardAllEdits() {
        if (current != null) {
            current.end();
            current = null;
        }
        super.discardAllEdits();
    }

    /**
     * Lezárja az aktuális csoportosított szerkesztést, ha van.
     * Undo/Redo előtt mindig hívni kell.
     */
    public synchronized void commit() {
        if (current != null) {
            current.end();
            // current már benne van a super listájában, csak lezárjuk
            current = null;
        }
    }

    @Override
    public synchronized boolean addEdit(UndoableEdit edit) {
        // Szűrjük a szintaxis-színezésből származó CHANGE editeket
        if (edit instanceof AbstractDocument.DefaultDocumentEvent) {
            AbstractDocument.DefaultDocumentEvent ev = (AbstractDocument.DefaultDocumentEvent) edit;
            if (ev.getType() == DocumentEvent.EventType.CHANGE) {
                return false;
            }
        }
        // Biztonsági szűrés: ha nem DefaultDocumentEvent, de attribútum-változás
        // (pl. Style), azt is ignoráljuk – az ilyen edit osztályneve tartalmazza
        // a "Attribute" vagy "Style" szót, vagy nem hordoz offsetet.
        // A DefaultStyledDocument CHANGE típusú editjeit már kiszűrtük,
        // így ami itt marad, az szöveges INSERT/REMOVE.

        long now = System.currentTimeMillis();
        int offset = -1;
        int len = 0;
        boolean isInsert = true;
        String text = null;

        if (edit instanceof AbstractDocument.DefaultDocumentEvent) {
            AbstractDocument.DefaultDocumentEvent ev = (AbstractDocument.DefaultDocumentEvent) edit;
            offset = ev.getOffset();
            len = ev.getLength();
            isInsert = ev.getType() == DocumentEvent.EventType.INSERT;
            if (isInsert) {
                try {
                    Document doc = ev.getDocument();
                    if (doc != null && len > 0) {
                        int docLen = doc.getLength();
                        if (offset >= 0 && offset + len <= docLen) {
                            text = doc.getText(offset, len);
                        }
                    }
                } catch (Exception ex) {
                    // nem kritikus
                }
            }
        }

        if (current == null) {
            // új csoport indítása
            current = new CompoundEdit();
            current.addEdit(edit);
            super.addEdit(current);
            lastTime = now;
            lastOffset = offset;
            lastLength = len;
            lastWasInsert = isInsert;

            // szóhatár esetén azonnal zárjuk, hogy a következő szó új csoport legyen
            if (isWordBoundary(text)) {
                commit();
            }
            return true;
        }

        // döntsük el, hogy új csoportot kezdünk-e
        boolean timeGap = (now - lastTime) > THRESHOLD;
        boolean typeChange = isInsert != lastWasInsert;
        boolean notContiguous = false;

        if (offset != -1 && lastOffset != -1) {
            if (isInsert) {
                // előre gépelés: offset == lastOffset + lastLength
                notContiguous = offset != (lastOffset + lastLength);
            } else {
                // törlés: backspace esetén offset == lastOffset-1,
                // delete esetén offset == lastOffset
                notContiguous = !(offset == lastOffset || offset == lastOffset - 1 || offset + len == lastOffset);
            }
        }

        if (timeGap || typeChange || notContiguous) {
            // lezárjuk az előzőt és újat kezdünk
            current.end();
            current = new CompoundEdit();
            current.addEdit(edit);
            super.addEdit(current);
        } else {
            current.addEdit(edit);
        }

        lastTime = now;
        lastOffset = offset;
        lastLength = len;
        lastWasInsert = isInsert;

        if (isWordBoundary(text)) {
            commit();
        }

        return true;
    }

    private boolean isWordBoundary(String text) {
        if (text == null) return false;
        if (text.length() == 0) return false;
        // ha a beszúrt szöveg szóközt, újsort vagy tabulátort tartalmaz,
        // vagy egyetlen nem betű/szám karakter, akkor határ
        if (text.length() == 1) {
            char c = text.charAt(0);
            if (Character.isWhitespace(c)) return true;
            // írásjelek, operátorok is határnak számítanak
            if (",;:.()[]{}=+-*/<>!&|\"'".indexOf(c) >= 0) return true;
            return false;
        } else {
            // több karakteres beszúrás (pl. beillesztés) esetén,
            // ha tartalmaz szóközt vagy újsort, határ
            for (int i = 0; i < text.length(); i++) {
                if (Character.isWhitespace(text.charAt(i))) return true;
            }
            return false;
        }
    }

    @Override
    public synchronized void undo() throws CannotUndoException {
        commit();
        super.undo();
    }

    @Override
    public synchronized void redo() throws CannotRedoException {
        commit();
        super.redo();
    }

    @Override
    public synchronized boolean canUndo() {
        if (current != null) {
            // van nyitott csoport, az is visszavonható
            return true;
        }
        return super.canUndo();
    }

    @Override
    public synchronized boolean canRedo() {
        // ha van nyitott csoport, a redo-t már törölte a super.addEdit
        // amikor az első új edit bekerült, így a super állapota helyes
        return super.canRedo();
    }
}
