package hu.ppke.itk.plang.gui.editor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

import hu.ppke.itk.plang.gui.theme.Theme;

/**
 * Sorszámozó sáv a szerkesztő bal oldalán, a VS Code "gutter" mintájára.
 * Az aktuális sor száma kiemelten jelenik meg, a hibás sor mellett pedig
 * hibajelölő pont látszik.
 */
public class LineNumberGutter extends JPanel {

   private static final long serialVersionUID = 1L;

   private final CodeEditor editor;
   private int minDigits = 2;

   public LineNumberGutter(CodeEditor editor) {
      this.editor = editor;
      setOpaque(true);
      applyTheme();

      editor.getDocument().addDocumentListener(new DocumentListener() {
         public void changedUpdate(DocumentEvent e) { refresh(); }
         public void insertUpdate(DocumentEvent e) { refresh(); }
         public void removeUpdate(DocumentEvent e) { refresh(); }
      });
      editor.addCaretListener(new CaretListener() {
         public void caretUpdate(CaretEvent e) { repaint(); }
      });

      /* Kattintás a sávon: az adott sor töréspontját kapcsolja. */
      addMouseListener(new java.awt.event.MouseAdapter() {
         public void mousePressed(java.awt.event.MouseEvent e) {
            int line = lineAtY(e.getY());
            if (line >= 0) {
               editor.toggleBreakpoint(line);
            }
         }
      });
   }

   /** A megadott y koordinátához tartozó sor (0-alapú), vagy -1. */
   private int lineAtY(int y) {
      javax.swing.text.Element root = editor.getDocument().getDefaultRootElement();
      for (int i = 0; i < root.getElementCount(); i++) {
         try {
            java.awt.Rectangle r = editor.modelToView(root.getElement(i).getStartOffset());
            if (r == null) {
               continue;
            }
            int lh = r.height > 0 ? r.height : getFontMetrics(editor.getFont()).getHeight();
            if (y >= r.y && y < r.y + lh) {
               return i;
            }
         } catch (javax.swing.text.BadLocationException e) {
            // kihagyjuk
         }
      }
      return -1;
   }

   public void applyTheme() {
      setBackground(Theme.p().editorBg);
      setForeground(Theme.p().gutterFg);
      repaint();
   }

   private void refresh() {
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            revalidate();
            repaint();
         }
      });
   }

   public Dimension getPreferredSize() {
      FontMetrics fm = getFontMetrics(editor.getFont());
      int digits = Math.max(minDigits, String.valueOf(Math.max(1, editor.lineCount())).length());
      int w = fm.charWidth('0') * digits + 26;
      return new Dimension(w, Math.max(1, editor.getHeight()));
   }

   protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                          RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

      Theme.Palette p = Theme.p();
      g2.setColor(p.editorBg);
      g2.fillRect(0, 0, getWidth(), getHeight());

      g2.setFont(editor.getFont());
      FontMetrics fm = g2.getFontMetrics();

      Element root = editor.getDocument().getDefaultRootElement();
      int caretLine = root.getElementIndex(editor.getCaretPosition());

      Rectangle clip = g2.getClipBounds();

      for (int i = 0; i < root.getElementCount(); i++) {
         try {
            Element le = root.getElement(i);
            Rectangle r = editor.modelToView(le.getStartOffset());
            if (r == null) {
               continue;
            }
            // a szerkesztő koordinátái a sáv koordinátáival egyeznek (közös nézetablak)
            int y = r.y;
            int lh = r.height > 0 ? r.height : fm.getHeight();
            if (clip != null && (y + lh < clip.y || y > clip.y + clip.height)) {
               continue;
            }

            boolean isCurrent = (i == caretLine);
            boolean isError = editor.isErrorLine(i);
            boolean isBp = editor.isBreakpoint(i);

            String num = String.valueOf(i + 1);
            g2.setColor(isError ? p.error : (isCurrent ? p.gutterActiveFg : p.gutterFg));
            int tw = fm.stringWidth(num);
            int x = getWidth() - tw - 14;
            g2.drawString(num, x, y + fm.getAscent());

            if (isBp) {
               g2.setColor(p.error);
               g2.fillOval(getWidth() - 10, y + lh / 2 - 4, 8, 8);
            } else if (isError) {
               g2.setColor(p.error);
               g2.fillOval(4, y + lh / 2 - 3, 6, 6);
            }

         } catch (BadLocationException e) {
            // kihagyjuk az adott sort
         }
      }

      g2.dispose();
   }
}
