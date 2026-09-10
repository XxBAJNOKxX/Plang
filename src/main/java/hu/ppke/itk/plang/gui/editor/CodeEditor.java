package hu.ppke.itk.plang.gui.editor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Utilities;

import hu.ppke.itk.plang.gui.theme.Theme;

/**
 * VS Code stílusú kódszerkesztő: szintaxiskiemelés, aktuális sor kiemelése,
 * behúzás-segédvonalak, automatikus behúzás és keresési találatok jelölése.
 */
public class CodeEditor extends JTextPane {

   private static final long serialVersionUID = 1L;

   private int tabSize = 2;
   private boolean showIndentGuides = true;
   private final List<int[]> findMatches = new ArrayList<int[]>();
   private int activeMatch = -1;
   /* A keresés elavult találatai adatvesztést okoznának cserekor, ezért az
      utolsó keresési feltételt megjegyezzük, és minden szövegváltozás után
      újraszámoljuk a találatokat. */
   private String lastNeedle = null;
   private boolean lastCaseSensitive = false;
   private boolean refreshSuspended = false;
   private final java.util.Set<Integer> errorLines = new java.util.TreeSet<Integer>();
   private final java.util.Set<Integer> breakpoints = new java.util.TreeSet<Integer>();
   private int runningLine = -1;

   private final PlangUndoManager undoManager = new PlangUndoManager();

   /* ---- IntelliSense-állapot ---- */
   private boolean autoComplete = true;
   private boolean suppressAutoComplete = false;
   private javax.swing.JPopupMenu completionPopup;
   private javax.swing.JList completionList;
   private String completionPrefix = "";
   /* Az utolsó kurzorpozíció: ha a kurzor elmozdul (nyilak, egérkattintás),
      a kiegészítő lista bezáródik. */
   private int lastCaretDot = 0;
   /* Explicit módban (Ctrl+Space) a lista veszi át a fókuszt – ilyenkor a
      fókuszvesztés miatt nem szabad bezárni a listát. */
   private boolean completionFocusable = false;

   public CodeEditor() {
      super(new SyntaxDocument());
      // A hátteret magunk festjük (a díszítésekkel együtt), ezért az
      // ősosztály ne törölje le őket.
      setOpaque(false);
      setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
      getDocument().addUndoableEditListener(undoManager);
      applyTheme();
      installKeyBindings();
      installContextMenu();
      addCaretListener(new CaretListener() {
         public void caretUpdate(CaretEvent e) {
            /* A kurzor elmozdulása (nyíl-billentyű, egérkattintás) bezárja a
               kiegészítő listát, hogy az ne maradjon a képernyőn a már
               begépelt szó után. */
            if (e.getDot() != lastCaretDot) {
               lastCaretDot = e.getDot();
               hideCompletions();
            }
            repaint();
         }
      });
      addFocusListener(new java.awt.event.FocusAdapter() {
         public void focusLost(java.awt.event.FocusEvent e) {
            /* A fókusz nélküli (automatikus) lista nem záródik magától,
               amikor a szerkesztő elveszti a fókuszt, ezért itt zárjuk be.
               Az explicit (Ctrl+Space) lista éppen a listának adja a fókuszt,
               azt maga a Swing zárja be, amikor másra kattintunk. */
            if (!completionFocusable) {
               hideCompletions();
            }
         }
      });
      getDocument().addDocumentListener(new DocumentListener() {
         public void changedUpdate(DocumentEvent e) { repaint(); }
         public void insertUpdate(DocumentEvent e) {
            textChanged();
            maybeAutoComplete(e);
         }
         public void removeUpdate(DocumentEvent e) {
            textChanged();
            hideCompletions();
         }
      });
   }

   /**
    * A szöveg megváltozott: a keresési találatok offszetjei eltolódtak, ezért
    * újraszámoljuk őket. Enélkül a kiemelések elvándorolnának, a „Csere”
    * pedig a rossz helyre írva rongálná a szöveget.
    */
   private void textChanged() {
      repaint();
      if (!refreshSuspended) {
         recomputeMatches();
      }
   }

   public PlangUndoManager getUndoManager() {
      return undoManager;
   }

   public void discardUndoHistory() {
      undoManager.discardAllEdits();
   }

   public void undo() {
      try {
         if (undoManager.canUndo()) {
            undoManager.undo();
            /* A visszavonás nyers szövegművelet, amely után a soronkénti
               újraszínezés nem mindig fedi le a változást (pl. egy komment
               törlése összeolvasztja a sorokat) – ezért a teljes dokumentumot
               újraszínezzük. A színezés CHANGE típusú edit, az undo manager
               kiszűri, így nem kerül be a visszavonási történetbe. */
            if (getDocument() instanceof SyntaxDocument) {
               ((SyntaxDocument) getDocument()).highlightAll();
            }
            repaint();
         }
      } catch (Exception e) {
         // nem kritikus
      }
   }

   public void redo() {
      try {
         if (undoManager.canRedo()) {
            undoManager.redo();
            if (getDocument() instanceof SyntaxDocument) {
               ((SyntaxDocument) getDocument()).highlightAll();
            }
            repaint();
         }
      } catch (Exception e) {
         // nem kritikus
      }
   }

   private void installContextMenu() {
      final javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();

      final javax.swing.JMenuItem undoItem = new javax.swing.JMenuItem("Visszavonás");
      undoItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent e) {
            undo();
         }
      });
      final javax.swing.JMenuItem redoItem = new javax.swing.JMenuItem("Újra");
      redoItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent e) {
            redo();
         }
      });
      final javax.swing.JMenuItem cutItem = new javax.swing.JMenuItem("Kivágás");
      cutItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent e) {
            cut();
         }
      });
      final javax.swing.JMenuItem copyItem = new javax.swing.JMenuItem("Másolás");
      copyItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent e) {
            copy();
         }
      });
      final javax.swing.JMenuItem pasteItem = new javax.swing.JMenuItem("Beillesztés");
      pasteItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent e) {
            paste();
         }
      });
      final javax.swing.JMenuItem selAllItem = new javax.swing.JMenuItem("Mind kijelölése");
      selAllItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent e) {
            selectAll();
         }
      });

      popup.add(undoItem);
      popup.add(redoItem);
      popup.addSeparator();
      popup.add(cutItem);
      popup.add(copyItem);
      popup.add(pasteItem);
      popup.addSeparator();
      popup.add(selAllItem);

      popup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
         public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
            undoItem.setEnabled(undoManager.canUndo());
            redoItem.setEnabled(undoManager.canRedo());
         }
         public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {}
         public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
      });

      addMouseListener(new java.awt.event.MouseAdapter() {
         public void mousePressed(java.awt.event.MouseEvent e) {
            if (e.isPopupTrigger()) {
               popup.show(e.getComponent(), e.getX(), e.getY());
            }
         }
         public void mouseReleased(java.awt.event.MouseEvent e) {
            if (e.isPopupTrigger()) {
               popup.show(e.getComponent(), e.getX(), e.getY());
            }
         }
      });
   }

   public void applyTheme() {
      Theme.Palette p = Theme.p();
      setBackground(p.editorBg);
      setForeground(p.editorFg);
      setCaretColor(p.caret);
      setSelectionColor(p.selection);
      setSelectedTextColor(p.editorFg);
      if (getDocument() instanceof SyntaxDocument) {
         ((SyntaxDocument) getDocument()).refreshTheme();
      }
      repaint();
   }

   public void setEditorFont(Font f) {
      setFont(f);
      // a tabulátor szélessége a betűmérethez igazodik
      FontMetrics fm = getFontMetrics(f);
      int w = fm.charWidth('m') * tabSize;
      javax.swing.text.TabStop[] stops = new javax.swing.text.TabStop[60];
      for (int i = 0; i < stops.length; i++) {
         stops[i] = new javax.swing.text.TabStop((i + 1) * w);
      }
      javax.swing.text.SimpleAttributeSet a = new javax.swing.text.SimpleAttributeSet();
      javax.swing.text.StyleConstants.setTabSet(a, new javax.swing.text.TabSet(stops));
      getStyledDocument().setParagraphAttributes(0, getDocument().getLength() + 1, a, false);
      repaint();
   }

   public void setShowIndentGuides(boolean b) {
      showIndentGuides = b;
      repaint();
   }

   public boolean isShowIndentGuides() {
      return showIndentGuides;
   }

   /** A futás közben kiemelt sor (0-alapú), vagy -1. */
   public void setRunningLine(int line) {
      runningLine = line;
      repaint();
   }

   public int getRunningLine() {
      return runningLine;
   }

   /** A hibás sor (0-alapú), vagy -1 – a többit törli. */
   public void setErrorLine(int line) {
      errorLines.clear();
      if (line >= 0) {
         errorLines.add(Integer.valueOf(line));
      }
      repaint();
   }

   /**
    * Több hibás sor (0-alapú) jelölése egyszerre; {@code null} vagy üres
    * tömb esetén a jelölés megszűnik.
    */
   public void setErrorLines(int[] lines) {
      errorLines.clear();
      if (lines != null) {
         for (int i = 0; i < lines.length; i++) {
            if (lines[i] >= 0) {
               errorLines.add(Integer.valueOf(lines[i]));
            }
         }
      }
      repaint();
   }

   /** A hibásnak jelölt sorok (0-alapú, növekvő sorrendben). */
   public int[] getErrorLines() {
      int[] r = new int[errorLines.size()];
      int i = 0;
      for (Integer v : errorLines) {
         r[i++] = v.intValue();
      }
      return r;
   }

   public boolean isErrorLine(int line) {
      return errorLines.contains(Integer.valueOf(line));
   }

   /* ------------------------- töréspontok ------------------------- */

   /** Töréspont ki/be a megadott (0-alapú) soron. */
   public void toggleBreakpoint(int line) {
      Integer l = Integer.valueOf(line);
      if (line < 0 || line >= lineCount()) {
         return;
      }
      if (!breakpoints.remove(l)) {
         breakpoints.add(l);
      }
      repaint();
   }

   public boolean isBreakpoint(int line) {
      return breakpoints.contains(Integer.valueOf(line));
   }

   /** A töréspontos sorok (0-alapú, növekvő sorrendben). */
   public int[] getBreakpoints() {
      int[] r = new int[breakpoints.size()];
      int i = 0;
      for (Integer v : breakpoints) {
         r[i++] = v.intValue();
      }
      return r;
   }

   public void clearBreakpoints() {
      breakpoints.clear();
      repaint();
   }

   /** Az első hibás sor (0-alapú), vagy -1. */
   public int getErrorLine() {
      return errorLines.isEmpty() ? -1 : errorLines.iterator().next().intValue();
   }

   /* --------------------------- keresés ---------------------------- */

   public int search(String needle, boolean caseSensitive) {
      this.lastNeedle = needle;
      this.lastCaseSensitive = caseSensitive;
      findMatches.clear();
      activeMatch = -1;
      if (needle == null || needle.length() == 0) {
         repaint();
         return 0;
      }
      fillMatches();
      if (!findMatches.isEmpty()) {
         activeMatch = 0;
         scrollToMatch();
      }
      repaint();
      return findMatches.size();
   }

   /**
    * Újraépíti a találatlistát a jelenlegi szövegre, a kurzor mozgatása
    * nélkül – szövegszerkesztés után hívódik.
    */
   private void recomputeMatches() {
      if (lastNeedle == null || lastNeedle.length() == 0) {
         return;
      }
      int keep = activeMatch;
      findMatches.clear();
      fillMatches();
      if (findMatches.isEmpty()) {
         activeMatch = -1;
      } else if (keep < 0 || keep >= findMatches.size()) {
         activeMatch = 0;
      } else {
         activeMatch = keep;
      }
   }

   /** A {@code lastNeedle} összes előfordulását gyűjti ki. */
   private void fillMatches() {
      String needle = lastNeedle;
      if (needle == null || needle.length() == 0) {
         return;
      }
      String text;
      try {
         text = getDocument().getText(0, getDocument().getLength());
      } catch (BadLocationException e) {
         return;
      }
      String hay = lastCaseSensitive ? text : text.toLowerCase();
      String pin = lastCaseSensitive ? needle : needle.toLowerCase();
      int idx = hay.indexOf(pin);
      while (idx >= 0) {
         findMatches.add(new int[] { idx, idx + pin.length() });
         idx = hay.indexOf(pin, idx + Math.max(1, pin.length()));
      }
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
      repaint();
   }

   public void prevMatch() {
      if (findMatches.isEmpty()) {
         return;
      }
      activeMatch = (activeMatch - 1 + findMatches.size()) % findMatches.size();
      scrollToMatch();
      repaint();
   }

   /** Lecseréli az aktuális találatot, és visszaadja, sikerült-e. */
   public boolean replaceCurrent(String replacement) {
      if (activeMatch < 0 || activeMatch >= findMatches.size()) {
         return false;
      }
      int[] m = findMatches.get(activeMatch);
      int len = getDocument().getLength();
      if (m[0] < 0 || m[1] > len || m[0] > m[1]) {
         return false;
      }
      try {
         getDocument().remove(m[0], m[1] - m[0]);
         getDocument().insertString(m[0], replacement, null);
         return true;
      } catch (BadLocationException e) {
         return false;
      }
   }

   public int replaceAll(String needle, String replacement, boolean caseSensitive) {
      int n = search(needle, caseSensitive);
      if (n == 0) {
         return 0;
      }
      /* A csere alatt a dokumentumfigyelő ne számolja újra a találatokat:
         a lista éppen bejárás alatt van, és a visszafelé haladás miatt
         az offszetek amúgy is érvényesek maradnak. */
      refreshSuspended = true;
      try {
         for (int i = findMatches.size() - 1; i >= 0; i--) {
            int[] m = findMatches.get(i);
            getDocument().remove(m[0], m[1] - m[0]);
            getDocument().insertString(m[0], replacement, null);
         }
      } catch (BadLocationException e) {
         // részleges csere is elfogadható
      } finally {
         refreshSuspended = false;
      }
      findMatches.clear();
      activeMatch = -1;
      repaint();
      return n;
   }

   public void clearSearch() {
      findMatches.clear();
      activeMatch = -1;
      lastNeedle = null;
      repaint();
   }

   private void scrollToMatch() {
      if (activeMatch < 0 || activeMatch >= findMatches.size()) {
         return;
      }
      final int[] m = findMatches.get(activeMatch);
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            try {
               setCaretPosition(m[0]);
               moveCaretPosition(m[1]);
               Rectangle r = modelToView2DRect(m[0]);
               if (r != null) {
                  scrollRectToVisible(r);
               }
            } catch (Exception e) {
               // a görgetés hibája nem kritikus
            }
         }
      });
   }

   @SuppressWarnings("deprecation")
   private Rectangle modelToView2DRect(int pos) throws BadLocationException {
      return modelToView(pos);
   }

   /* ------------------------ billentyűk ---------------------------- */

   private void installKeyBindings() {
      // Enter: automatikus behúzás a PLanG szerkezetei szerint
      getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "plang-newline");
      getActionMap().put("plang-newline", new AbstractAction() {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent e) {
            smartNewline();
         }
      });

      // Tab / Shift+Tab: ha nyitva a kiegészítő, a Tab elfogadja a javaslatot,
      // egyébként a szokásos blokk-behúzás történik.
      getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "plang-tab");
      getActionMap().put("plang-tab", new AbstractAction() {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent e) {
            if (isCompletionActive()) {
               acceptCompletion();
               return;
            }
            if (getSelectionStart() != getSelectionEnd()) {
               shiftLines(true);
            } else {
               replaceSelection(spaces(tabSize));
            }
         }
      });
      // Esc: nyitott kiegészítő lista bezárása
      getInputMap(JComponent.WHEN_FOCUSED).put(
         KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "plang-esc");
      getActionMap().put("plang-esc", new AbstractAction() {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent e) {
            if (isCompletionActive()) {
               hideCompletions();
            }
         }
      });
      getInputMap(JComponent.WHEN_FOCUSED).put(
         KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK), "plang-untab");
      getActionMap().put("plang-untab", new AbstractAction() {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent e) {
            shiftLines(false);
         }
      });

      // Ctrl+/ : sor ki- és bekommentelése
      getInputMap(JComponent.WHEN_FOCUSED).put(
         KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, InputEvent.CTRL_DOWN_MASK), "plang-comment");
      getActionMap().put("plang-comment", new AbstractAction() {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent e) {
            toggleComment();
         }
      });

      // Ctrl+D : sor megkettőzése
      getInputMap(JComponent.WHEN_FOCUSED).put(
         KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK), "plang-dup");
      getActionMap().put("plang-dup", new AbstractAction() {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent e) {
            duplicateLine();
         }
      });

      // Alt+Fel / Alt+Le : sor mozgatása
      getInputMap(JComponent.WHEN_FOCUSED).put(
         KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK), "plang-moveup");
      getActionMap().put("plang-moveup", new AbstractAction() {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent e) {
            moveLine(-1);
         }
      });
      getInputMap(JComponent.WHEN_FOCUSED).put(
         KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK), "plang-movedown");
      getActionMap().put("plang-movedown", new AbstractAction() {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent e) {
            moveLine(1);
         }
      });

      // Ctrl+Space : kódkiegészítés
      getInputMap(JComponent.WHEN_FOCUSED).put(
         KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK), "plang-complete");
      getActionMap().put("plang-complete", new AbstractAction() {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent e) {
            showCompletions(true);
         }
      });
   }

   /** Automatikus kiegészítés be-/kikapcsolása (alapból be). */
   public void setAutoComplete(boolean b) {
      autoComplete = b;
      if (!b) {
         hideCompletions();
      }
   }

   public boolean isAutoComplete() {
      return autoComplete;
   }

   /* ---------------------- kódkiegészítés ------------------------- */

   /**
    * A kurzor előtti azonosítótöredék a jelenlegi sorban.
    */
   public String wordPrefixAtCaret() {
      try {
         int pos = getCaretPosition();
         Element root = getDocument().getDefaultRootElement();
         Element le = root.getElement(root.getElementIndex(pos));
         int s = le.getStartOffset();
         String line = getDocument().getText(s, pos - s);
         int i = line.length();
         while (i > 0 && PlangSyntax.isWordChar(line.charAt(i - 1))) {
            i--;
         }
         return line.substring(i);
      } catch (BadLocationException e) {
         return "";
      }
   }

   /**
    * A dokumentumban deklarált felhasználói azonosítók: a {@code VÁLTOZÓK}
    * blokkban szereplő változónevek, valamint a program-, eljárás- és
    * függvénynevek. Ezeket is kínáljuk a kiegészítéshez, így a lista nemcsak
    * kulcsszavakból áll – közelebb kerülve egy valódi IntelliSense-hez.
    */
   public List<String> collectDeclaredNames() {
      java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<String>();
      String text;
      try {
         text = getDocument().getText(0, getDocument().getLength());
      } catch (BadLocationException e) {
         return out.isEmpty() ? new ArrayList<String>() : new ArrayList<String>(out);
      }
      String[] lines = text.split("\n", -1);
      for (int i = 0; i < lines.length; i++) {
         String t = lines[i].trim();
         String low = PlangSyntax.deacc(t);

         // program / eljárás / függvény neve
         String[] heads = { "program ", "eljaras ", "fuggveny " };
         for (int h = 0; h < heads.length; h++) {
            if (low.startsWith(heads[h])) {
               String name = t.substring(heads[h].length()).trim();
               if (isIdent(name)) {
                  out.add(name);
               }
            }
         }

         // deklaráció: "a, b, osszeg: EGÉSZ" – a bal oldalon vesszős nevek,
         // a jobb oldalon egy ismert típuskulcsszó áll
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
      for (int i = 0; i < types.length; i++) {
         if (deaccRight.equals(types[i]) || deaccRight.startsWith(types[i] + " ")
             || deaccRight.startsWith(types[i] + "[")) {
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

   /**
    * A kurzor előtti töredékre illeszkedő javaslatok: először a saját
    * azonosítók, majd a kulcsszavak; azonosítók és kulcsszavak nem
    * duplikálódnak.
    */
   public List<String> allCompletions(String prefix) {
      java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<String>();
      List<String> out = new ArrayList<String>();
      String want = PlangSyntax.deacc(prefix == null ? "" : prefix);

      for (String id : collectDeclaredNames()) {
         if (want.length() == 0 || PlangSyntax.deacc(id).startsWith(want)) {
            if (seen.add(PlangSyntax.deacc(id))) {
               out.add(id);
            }
         }
      }
      for (String kw : completionsFor(prefix)) {
         if (seen.add(PlangSyntax.deacc(kw))) {
            out.add(kw);
         }
      }
      return out;
   }

   /** Kontextus: típusnév várt (deklarációban a {@code :} után). */
   public static final int CTX_TYPE = 0;
   /** Kontextus: kifejezés/utasítás. */
   public static final int CTX_EXPR = 1;

   /**
    * Megállapítja a kurzor helyéhez tartozó kiegészítési kontextust: ha a sor
    * eleje azonosító(k)ból áll és egy {@code :} zárja (deklaráció), akkor
    * típusra van szükség; egyébként kifejezés/utasítás a kontextus.
    */
   public int completionContext() {
      try {
         int pos = getCaretPosition();
         Element root = getDocument().getDefaultRootElement();
         Element le = root.getElement(root.getElementIndex(pos));
         int s = le.getStartOffset();
         String before = getDocument().getText(s, Math.max(0, pos - s));
         int ci = before.lastIndexOf(':');
         if (ci >= 0 && !(ci + 1 < before.length() && before.charAt(ci + 1) == '=')) {
            String left = before.substring(0, ci).trim();
            if (leftLooksLikeDeclNames(left)) {
               return CTX_TYPE;
            }
         }
      } catch (BadLocationException e) {
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
         /* Egyetlen kulcsszó (BE:, KI:, VÁLTOZÓK:) nem deklarációs bal oldal. */
         if (PlangSyntax.KEYWORD.contains(PlangSyntax.deacc(t))) {
            return false;
         }
      }
      return true;
   }

   /**
    * Kontextusfüggő javaslatok: deklarációban típusnevek, egyébként a saját
    * azonosítók, majd a függvények/konstansok, végül a vezérlő kulcsszavak.
    */
   public List<String> contextualCompletions(String prefix) {
      List<String> out = new ArrayList<String>();
      java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<String>();
      String want = PlangSyntax.deacc(prefix == null ? "" : prefix);

      if (completionContext() == CTX_TYPE) {
         for (String c : PlangSyntax.COMPLETIONS) {
            if (catOf(c) == PlangSyntax.TYPE_T && match(c, want)
                  && seen.add(PlangSyntax.deacc(c))) {
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
         if ((cat == PlangSyntax.FUNC || cat == PlangSyntax.CONST)
               && match(c, want) && seen.add(PlangSyntax.deacc(c))) {
            out.add(c);
         }
      }
      for (String c : PlangSyntax.COMPLETIONS) {
         int cat = catOf(c);
         if ((cat == PlangSyntax.CTRL || cat == PlangSyntax.KW)
               && match(c, want) && seen.add(PlangSyntax.deacc(c))) {
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

   /**
    * A megadott töredékre illeszkedő kulcsszavak. Az összehasonlítás
    * kisbetűs és ékezettelenített, így a „valtoz” és a „VÁLTOZ” is megtalálja
    * a {@code VÁLTOZÓK:} alakot.
    */
   public static List<String> completionsFor(String prefix) {
      List<String> out = new ArrayList<String>();
      if (prefix == null) {
         prefix = "";
      }
      String want = PlangSyntax.deacc(prefix);
      for (int i = 0; i < PlangSyntax.COMPLETIONS.length; i++) {
         String c = PlangSyntax.COMPLETIONS[i];
         if (want.length() == 0 || PlangSyntax.deacc(c).startsWith(want)) {
            out.add(c);
         }
      }
      return out;
   }

   /** A kiegészítő lista megjelenítése (fejjel nem futó módban nem jelenik meg). */
   public void showCompletions() {
      showCompletions(true);
   }

   /**
    * A kiegészítő lista megjelenítése.
    *
    * @param explicit {@code true} esetén a felhasználó szándékosan hívta
    *        (Ctrl+Space): egyetlen találat azonnal kiegészítődik, a lista
    *        megkapja a fókuszt. {@code false} a gépelés közbeni automatikus
    *        felbukkanás: a lista nem veszi el a fókuszt, a Tab fogadja el.
    */
   public void showCompletions(boolean explicit) {
      final String prefix = wordPrefixAtCaret();
      final List<String> items = contextualCompletions(prefix);
      if (items.isEmpty()) {
         hideCompletions();
         return;
      }
      /* Ha az egyetlen találat pontosan a már begépelt szó, nincs mit tenni. */
      if (items.size() == 1
          && PlangSyntax.deacc(items.get(0)).equals(PlangSyntax.deacc(prefix))) {
         hideCompletions();
         return;
      }
      if (explicit && items.size() == 1) {
         applyCompletion(items.get(0), prefix);
         hideCompletions();
         return;
      }
      try {
         /* A korábbi listát bezárjuk, mielőtt az újat megjelenítjük: különben
            a gépelés során az előugró ablakok egymásra halmozódnának, és a
            régebbiek a szó begépelése után is a képernyőn maradnának. */
         hideCompletions();

         final javax.swing.JList list = new javax.swing.JList(items.toArray());
         list.setSelectedIndex(0);
         final javax.swing.JScrollPane sp = new javax.swing.JScrollPane(list);
         sp.setPreferredSize(new Dimension(220, Math.min(160, 22 * items.size() + 8)));
         final javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();
         popup.setLayout(new java.awt.BorderLayout());
         popup.add(sp, java.awt.BorderLayout.CENTER);
         Runnable commit = new Runnable() {
            public void run() {
               Object sel = list.getSelectedValue();
               popup.setVisible(false);
               if (sel != null) {
                  applyCompletion(String.valueOf(sel), prefix);
               }
            }
         };
         final Runnable doCommit = commit;
         list.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
               if (e.getClickCount() >= 2) {
                  doCommit.run();
               }
            }
         });
         list.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent e) {
               if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                  e.consume();
                  doCommit.run();
               } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                  popup.setVisible(false);
                  requestFocusInWindow();
               }
            }
         });
         java.awt.Point p = caretPopupLocation();
         if (p != null) {
            this.completionPopup = popup;
            this.completionList = list;
            this.completionPrefix = prefix;
            this.completionFocusable = explicit;
            popup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
               public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {}
               public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                  /* Csak akkor töröljük a hivatkozást, ha az éppen bezáródó
                     lista a jelenlegi – egy korábbi lista bezáródása ne
                     takarítsa el az újabbat. */
                  if (completionPopup == popup) {
                     completionPopup = null;
                     completionList = null;
                  }
               }
               public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                  if (completionPopup == popup) {
                     completionPopup = null;
                     completionList = null;
                  }
               }
            });
            if (explicit) {
               popup.show(this, p.x, p.y);
               list.requestFocusInWindow();
            } else {
               /* Automatikus módban a fókusz a szerkesztőben marad, így a
                  gépelés folytatódhat; a lista csak javaslat. */
               popup.setFocusable(false);
               list.setFocusable(false);
               popup.show(this, p.x, p.y);
            }
         }
      } catch (java.awt.HeadlessException he) {
         // fej nélküli környezetben nincs előugró lista
      }
   }

   /** Nyitva van-e éppen a kiegészítő lista. */
   public boolean isCompletionActive() {
      return completionPopup != null && completionPopup.isVisible();
   }

   /** A kijelölt (vagy első) javaslat elfogadása – a Tab hívja. */
   public void acceptCompletion() {
      if (completionList != null && isCompletionActive()) {
         Object sel = completionList.getSelectedValue();
         String word = sel != null ? String.valueOf(sel) : null;
         completionPopup.setVisible(false);
         if (word != null) {
            applyCompletion(word, completionPrefix);
         }
      }
   }

   /** Bezárja a kiegészítő listát. */
   public void hideCompletions() {
      if (completionPopup != null) {
         completionPopup.setVisible(false);
      }
      completionPopup = null;
      completionList = null;
      completionFocusable = false;
   }

   /**
    * Gépelés közbeni automatikus felbukkanás: ha a beszúrt karakter szóalkotó
    * és a kurzor előtti töredék legalább 2 karakter, megjelenítjük a listát.
    */
   private void maybeAutoComplete(DocumentEvent e) {
      if (!autoComplete || suppressAutoComplete) {
         return;
      }
      try {
         int len = e.getLength();
         if (len < 1) {
            return;
         }
         String ins = e.getDocument().getText(e.getOffset(), len);
         char last = ins.charAt(ins.length() - 1);
         if (!PlangSyntax.isWordChar(last)) {
            hideCompletions();
            return;
         }
         final String prefix = wordPrefixAtCaret();
         if (prefix.length() >= 2) {
            SwingUtilities.invokeLater(new Runnable() {
               public void run() {
                  if (!suppressAutoComplete) {
                     showCompletions(false);
                  }
               }
            });
         } else {
            hideCompletions();
         }
      } catch (BadLocationException ex) {
         // nem kritikus
      }
   }

   /** Kicseréli a kurzor előtti töredéket a választott szóra. */
   private void applyCompletion(String word, String prefix) {
      /* A programozott beszúrás ne indítson újra automatikus kiegészítést. */
      suppressAutoComplete = true;
      try {
         int pos = getCaretPosition();
         getDocument().remove(pos - prefix.length(), prefix.length());
         getDocument().insertString(pos - prefix.length(), word, null);
         setCaretPosition(pos - prefix.length() + word.length());
      } catch (BadLocationException e) {
         // nem kritikus
      } finally {
         suppressAutoComplete = false;
      }
   }

   /**
    * A kurzor alatti pozíció a szerkesztő saját koordinátáiban – a
    * kiegészítő lista ehhez igazodik (a {@code popup.show} is ezt várja).
    */
   private java.awt.Point caretPopupLocation() {
      try {
         Rectangle r = modelToView2DRect(getCaretPosition());
         if (r == null) {
            return null;
         }
         return new java.awt.Point(r.x, r.y + r.height);
      } catch (Exception e) {
         return null;
      }
   }

   private static String spaces(int n) {
      StringBuffer b = new StringBuffer();
      for (int i = 0; i < n; i++) {
         b.append(' ');
      }
      return b.toString();
   }

   private void smartNewline() {
      try {
         int pos = getCaretPosition();
         Element root = getDocument().getDefaultRootElement();
         int li = root.getElementIndex(pos);
         Element le = root.getElement(li);
         String line = getDocument().getText(le.getStartOffset(),
                                             Math.max(0, le.getEndOffset() - le.getStartOffset() - 1));

         int indent = 0;
         while (indent < line.length() && line.charAt(indent) == ' ') {
            indent++;
         }

         int[] eff = PlangSyntax.indentEffect(line);
         int nextIndent = Math.max(0, indent + eff[1] * tabSize);

         replaceSelection("\n" + spaces(nextIndent));
      } catch (BadLocationException e) {
         replaceSelection("\n");
      }
   }

   private void shiftLines(boolean right) {
      try {
         Element root = getDocument().getDefaultRootElement();
         int a = root.getElementIndex(getSelectionStart());
         int b = root.getElementIndex(Math.max(getSelectionStart(), getSelectionEnd() - 1));
         for (int i = a; i <= b; i++) {
            Element le = root.getElement(i);
            int s = le.getStartOffset();
            if (right) {
               getDocument().insertString(s, spaces(tabSize), null);
            } else {
               int len = Math.max(0, le.getEndOffset() - s - 1);
               String line = getDocument().getText(s, len);
               int rm = 0;
               while (rm < tabSize && rm < line.length() && line.charAt(rm) == ' ') {
                  rm++;
               }
               if (rm > 0) {
                  getDocument().remove(s, rm);
               }
            }
         }
      } catch (BadLocationException e) {
         // figyelmen kívül hagyható
      }
   }

   private void toggleComment() {
      try {
         Element root = getDocument().getDefaultRootElement();
         int a = root.getElementIndex(getSelectionStart());
         int b = root.getElementIndex(Math.max(getSelectionStart(), getSelectionEnd() - 1));

         boolean allCommented = true;
         for (int i = a; i <= b; i++) {
            Element le = root.getElement(i);
            String line = getDocument().getText(le.getStartOffset(),
                                                Math.max(0, le.getEndOffset() - le.getStartOffset() - 1));
            if (line.trim().length() > 0 && !line.trim().startsWith("**")) {
               allCommented = false;
               break;
            }
         }

         for (int i = a; i <= b; i++) {
            Element le = root.getElement(i);
            int s = le.getStartOffset();
            int len = Math.max(0, le.getEndOffset() - s - 1);
            String line = getDocument().getText(s, len);
            if (line.trim().length() == 0) {
               continue;
            }
            if (allCommented) {
               int idx = line.indexOf("**");
               if (idx >= 0) {
                  int rm = 2;
                  if (idx + 2 < line.length() && line.charAt(idx + 2) == ' ') {
                     rm = 3;
                  }
                  getDocument().remove(s + idx, rm);
               }
            } else {
               int indent = 0;
               while (indent < line.length() && line.charAt(indent) == ' ') {
                  indent++;
               }
               getDocument().insertString(s + indent, "** ", null);
            }
         }
      } catch (BadLocationException e) {
         // figyelmen kívül hagyható
      }
   }

   private void duplicateLine() {
      try {
         Element root = getDocument().getDefaultRootElement();
         int li = root.getElementIndex(getCaretPosition());
         Element le = root.getElement(li);
         int s = le.getStartOffset();
         int len = Math.max(0, le.getEndOffset() - s - 1);
         String line = getDocument().getText(s, len);
         getDocument().insertString(le.getEndOffset() - 1, "\n" + line, null);
      } catch (BadLocationException e) {
         // figyelmen kívül hagyható
      }
   }

   private void moveLine(int dir) {
      try {
         Element root = getDocument().getDefaultRootElement();
         int li = root.getElementIndex(getCaretPosition());
         int target = li + dir;
         if (target < 0 || target >= root.getElementCount()) {
            return;
         }
         Element cur = root.getElement(li);
         int cs = cur.getStartOffset();
         int clen = Math.max(0, cur.getEndOffset() - cs - 1);
         String line = getDocument().getText(cs, clen);
         int caretCol = getCaretPosition() - cs;

         // az aktuális sor törlése (a sortöréssel együtt)
         int removeLen = Math.min(getDocument().getLength() - cs, clen + 1);
         getDocument().remove(cs, removeLen);

         Element tgt = getDocument().getDefaultRootElement().getElement(target);
         int insertAt = (tgt != null) ? tgt.getStartOffset() : getDocument().getLength();
         getDocument().insertString(insertAt, line + "\n", null);
         setCaretPosition(Math.min(getDocument().getLength(), insertAt + caretCol));
      } catch (BadLocationException e) {
         // figyelmen kívül hagyható
      }
   }

   /* ------------------------- kirajzolás ---------------------------- */

   protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      Theme.Palette p = Theme.p();

      g2.setColor(getBackground());
      g2.fillRect(0, 0, getWidth(), getHeight());

      FontMetrics fm = g.getFontMetrics(getFont());
      int lh = fm.getHeight();
      Insets ins = getInsets();

      // futó / hibás / aktuális sor háttere
      try {
         Element root = getDocument().getDefaultRootElement();

         for (Integer el : errorLines) {
            int line = el.intValue();
            if (line >= 0 && line < root.getElementCount()) {
               Rectangle r = lineRect(line, lh);
               if (r != null) {
                  g2.setColor(p.errorLineBg);
                  g2.fillRect(0, r.y, getWidth(), lh);
               }
            }
         }
         if (runningLine >= 0 && runningLine < root.getElementCount()) {
            Rectangle r = lineRect(runningLine, lh);
            if (r != null) {
               g2.setColor(Theme.alpha(p.warning, 45));
               g2.fillRect(0, r.y, getWidth(), lh);
            }
         }

         int caretLine = root.getElementIndex(getCaretPosition());
         if (getSelectionStart() == getSelectionEnd()
             && !errorLines.contains(Integer.valueOf(caretLine)) && caretLine != runningLine) {
            Rectangle r = lineRect(caretLine, lh);
            if (r != null) {
               g2.setColor(p.currentLine);
               g2.fillRect(0, r.y, getWidth(), lh);
            }
         }

         // keresési találatok
         if (!findMatches.isEmpty()) {
            for (int i = 0; i < findMatches.size(); i++) {
               int[] m = findMatches.get(i);
               Rectangle r1 = modelToView2DRect(m[0]);
               Rectangle r2 = modelToView2DRect(m[1]);
               if (r1 == null || r2 == null) {
                  continue;
               }
               g2.setColor(i == activeMatch ? p.findMatchActive : p.findMatch);
               if (r1.y == r2.y) {
                  g2.fillRect(r1.x, r1.y, Math.max(2, r2.x - r1.x), r1.height);
               }
            }
         }

         // behúzás-segédvonalak
         if (showIndentGuides) {
            g2.setColor(p.indentGuide);
            g2.setStroke(new BasicStroke(1f));
            int charW = fm.charWidth(' ');
            for (int i = 0; i < root.getElementCount(); i++) {
               Element le = root.getElement(i);
               int s = le.getStartOffset();
               int len = Math.max(0, le.getEndOffset() - s - 1);
               if (len <= 0) {
                  continue;
               }
               String line = getDocument().getText(s, len);
               int indent = 0;
               while (indent < line.length() && line.charAt(indent) == ' ') {
                  indent++;
               }
               if (indent == 0) {
                  continue;
               }
               Rectangle r = lineRect(i, lh);
               if (r == null) {
                  continue;
               }
               for (int c = tabSize; c < indent; c += tabSize) {
                  int x = ins.left + c * charW;
                  g2.drawLine(x, r.y, x, r.y + lh);
               }
            }
         }
      } catch (Exception e) {
         // a díszítés hibája ne akadályozza a szöveg kirajzolását
      }

      g2.dispose();
      super.paintComponent(g);
   }

   private Rectangle lineRect(int line, int lh) {
      try {
         Element root = getDocument().getDefaultRootElement();
         if (line < 0 || line >= root.getElementCount()) {
            return null;
         }
         Element le = root.getElement(line);
         Rectangle r = modelToView2DRect(le.getStartOffset());
         return r;
      } catch (BadLocationException e) {
         return null;
      }
   }

   /** A soreleji szóköz nélküli hossz – a sortördelés kikapcsolásához. */
   public boolean getScrollableTracksViewportWidth() {
      java.awt.Container parent = getParent();
      if (parent == null) {
         return true;
      }
      return getUI().getPreferredSize(this).width <= parent.getSize().width;
   }

   public Dimension getPreferredSize() {
      Dimension d = super.getPreferredSize();
      java.awt.Container parent = getParent();
      if (parent != null) {
         d.width = Math.max(d.width, parent.getWidth());
      }
      return d;
   }

   /** Az aktuális sor száma (1-alapú). */
   public int caretLine() {
      return getDocument().getDefaultRootElement().getElementIndex(getCaretPosition()) + 1;
   }

   /** Az aktuális oszlop (1-alapú). */
   public int caretColumn() {
      int pos = getCaretPosition();
      Element root = getDocument().getDefaultRootElement();
      int li = root.getElementIndex(pos);
      return pos - root.getElement(li).getStartOffset() + 1;
   }

   public int lineCount() {
      return getDocument().getDefaultRootElement().getElementCount();
   }

   /** A megadott sor elejére ugrik (0-alapú). */
   public void gotoLine(int line) {
      Element root = getDocument().getDefaultRootElement();
      if (line < 0 || line >= root.getElementCount()) {
         return;
      }
      final int off = root.getElement(line).getStartOffset();
      setCaretPosition(off);
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            try {
               Rectangle r = modelToView2DRect(off);
               if (r != null) {
                  scrollRectToVisible(r);
               }
            } catch (Exception e) {
               // nem kritikus
            }
         }
      });
   }
}
