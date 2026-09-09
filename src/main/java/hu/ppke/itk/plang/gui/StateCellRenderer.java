package hu.ppke.itk.plang.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import hu.ppke.itk.plang.gui.theme.Theme;

/**
 * A változótábla celláinak megjelenítése.
 *
 * A LÉPÉS oszlop halványan, a változók értéke típus szerint színezve jelenik
 * meg (szám, szöveg, logikai), a definiálatlan érték "???" jelöléssel, a
 * hibás pedig "###" jelöléssel, a VS Code hibaszínével.
 */
public class StateCellRenderer extends JComponent implements TableCellRenderer {

   private static final long serialVersionUID = 1L;

   private Font textFont;
   private String text = "";
   private boolean selected;
   private boolean stepColumn;
   private boolean undefined;
   private boolean bad;
   private boolean changed;

   public StateCellRenderer(Font f) {
      this.textFont = f;
      setOpaque(true);
   }

   public void setFont(Font f) {
      this.textFont = f;
   }

   public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                  boolean hasFocus, int row, int column) {
      this.selected = isSelected;
      this.stepColumn = (column == 0);
      String raw = value == null ? "" : String.valueOf(value);
      String plain = ProgLineRenderer.stripHtml(raw)[0];
      this.undefined = plain.equals("???");
      this.bad = plain.equals("###");
      this.text = plain;

      // változott-e az előző lépéshez képest?
      this.changed = false;
      if (!stepColumn && row > 0 && table.getModel() instanceof StateList) {
         Object prev = table.getModel().getValueAt(row - 1, column);
         String prevPlain = prev == null ? "" : ProgLineRenderer.stripHtml(String.valueOf(prev))[0];
         this.changed = !prevPlain.equals(plain);
      }
      return this;
   }

   protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                          RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      Theme.Palette p = Theme.p();

      int w = getWidth();
      int h = getHeight();

      g2.setColor(selected ? p.listSelection : p.panelBg);
      g2.fillRect(0, 0, w, h);

      if (!selected && changed) {
         g2.setColor(Theme.alpha(p.success, 28));
         g2.fillRect(0, 0, w, h);
      }

      g2.setFont(stepColumn ? textFont : textFont);
      FontMetrics fm = g2.getFontMetrics();
      int baseline = (h - fm.getHeight()) / 2 + fm.getAscent();

      Color fg;
      if (bad) {
         fg = p.error;
      } else if (undefined) {
         fg = p.info;
      } else if (stepColumn) {
         fg = selected ? p.listSelectionFg : p.gutterFg;
      } else if (isNumber(text)) {
         fg = selected ? p.listSelectionFg : p.synNumber;
      } else if (text.equals("IGAZ") || text.equals("HAMIS")) {
         fg = selected ? p.listSelectionFg : p.synConstant;
      } else {
         fg = selected ? p.listSelectionFg : p.synString;
      }
      g2.setColor(fg);

      String draw = text;
      int tw = fm.stringWidth(draw);
      if (tw > w - 10) {
         while (draw.length() > 1 && fm.stringWidth(draw + "…") > w - 10) {
            draw = draw.substring(0, draw.length() - 1);
         }
         draw = draw + "…";
      }
      int x = stepColumn ? (w - fm.stringWidth(draw) - 8) : 6;
      g2.drawString(draw, x, baseline);

      g2.setColor(p.tableGrid);
      g2.drawLine(w - 1, 0, w - 1, h);

      g2.dispose();
   }

   private static boolean isNumber(String s) {
      if (s.length() == 0) {
         return false;
      }
      boolean digit = false;
      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if (Character.isDigit(c)) {
            digit = true;
         } else if (c != '-' && c != '+' && c != '.' && c != 'E' && c != 'e') {
            return false;
         }
      }
      return digit;
   }
}
