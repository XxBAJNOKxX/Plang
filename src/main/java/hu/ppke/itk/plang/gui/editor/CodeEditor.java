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
   private int errorLine = -1;
   private int runningLine = -1;

   public CodeEditor() {
      super(new SyntaxDocument());
      // A hátteret magunk festjük (a díszítésekkel együtt), ezért az
      // ősosztály ne törölje le őket.
      setOpaque(false);
      setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
      applyTheme();
      installKeyBindings();
      addCaretListener(new CaretListener() {
         public void caretUpdate(CaretEvent e) {
            repaint();
         }
      });
      getDocument().addDocumentListener(new DocumentListener() {
         public void changedUpdate(DocumentEvent e) { repaint(); }
         public void insertUpdate(DocumentEvent e) { repaint(); }
         public void removeUpdate(DocumentEvent e) { repaint(); }
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

   /** A hibás sor (0-alapú), vagy -1. */
   public void setErrorLine(int line) {
      errorLine = line;
      repaint();
   }

   public int getErrorLine() {
      return errorLine;
   }

   /* --------------------------- keresés ---------------------------- */

   public int search(String needle, boolean caseSensitive) {
      findMatches.clear();
      activeMatch = -1;
      if (needle == null || needle.length() == 0) {
         repaint();
         return 0;
      }
      String text;
      try {
         text = getDocument().getText(0, getDocument().getLength());
      } catch (BadLocationException e) {
         return 0;
      }
      String hay = caseSensitive ? text : text.toLowerCase();
      String pin = caseSensitive ? needle : needle.toLowerCase();
      int idx = hay.indexOf(pin);
      while (idx >= 0) {
         findMatches.add(new int[] { idx, idx + pin.length() });
         idx = hay.indexOf(pin, idx + Math.max(1, pin.length()));
      }
      if (!findMatches.isEmpty()) {
         activeMatch = 0;
         scrollToMatch();
      }
      repaint();
      return findMatches.size();
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
      try {
         for (int i = findMatches.size() - 1; i >= 0; i--) {
            int[] m = findMatches.get(i);
            getDocument().remove(m[0], m[1] - m[0]);
            getDocument().insertString(m[0], replacement, null);
         }
      } catch (BadLocationException e) {
         // részleges csere is elfogadható
      }
      findMatches.clear();
      activeMatch = -1;
      repaint();
      return n;
   }

   public void clearSearch() {
      findMatches.clear();
      activeMatch = -1;
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

      // Tab / Shift+Tab: blokk behúzása
      getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "plang-tab");
      getActionMap().put("plang-tab", new AbstractAction() {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent e) {
            if (getSelectionStart() != getSelectionEnd()) {
               shiftLines(true);
            } else {
               replaceSelection(spaces(tabSize));
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

         if (errorLine >= 0 && errorLine < root.getElementCount()) {
            Rectangle r = lineRect(errorLine, lh);
            if (r != null) {
               g2.setColor(p.errorLineBg);
               g2.fillRect(0, r.y, getWidth(), lh);
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
         if (getSelectionStart() == getSelectionEnd() && caretLine != errorLine && caretLine != runningLine) {
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
