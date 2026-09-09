package hu.ppke.itk.plang.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;

import hu.ppke.itk.plang.gui.editor.PlangSyntax;
import hu.ppke.itk.plang.gui.theme.Theme;
import hu.ppke.itk.plang.gui.widgets.VSIcons;

/**
 * A kifejezésfa csomópontjainak megjelenítése a VS Code "Változók" panel
 * stílusában: a kifejezés szintaxis szerint színezve, az eredmény pedig
 * kiemelve, egyenlőségjel után.
 */
public class ExprRenderer extends JComponent implements TreeCellRenderer {

   private static final long serialVersionUID = 1L;

   private Font textFont;
   private String expr = "";
   private String result;
   private boolean selected;
   private boolean isError;
   private boolean subProgram;

   public ExprRenderer(Font font) {
      this.textFont = font;
      setOpaque(true);
   }

   public void setFont(Font f) {
      this.textFont = f;
   }

   public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                 boolean expanded, boolean leaf, int row,
                                                 boolean hasFocus) {
      this.selected = sel;
      this.expr = "";
      this.result = null;
      this.isError = false;
      this.subProgram = false;

      if (value instanceof ExprNode) {
         ExprNode n = (ExprNode) value;
         this.subProgram = n.getSubStates() != null;
         String raw = n.toString();
         // az ExprNode HTML-t ad vissza: "<html>kifejezés<b> = eredmény</b>"
         String[] parts = splitResult(raw);
         this.expr = parts[0];
         this.result = parts[1];
         this.isError = parts[2] != null;
      } else if (value != null) {
         this.expr = ProgLineRenderer.stripHtml(String.valueOf(value))[0];
      }
      setFont(textFont);
      return this;
   }

   /**
    * Szétválasztja a kifejezést és az eredményt, és jelzi, ha a szöveg
    * hibaüzenetet tartalmazott (piros betűszín).
    */
   private static String[] splitResult(String html) {
      boolean hadRed = html.toLowerCase().indexOf("color=\"red\"") >= 0
                       || html.toLowerCase().indexOf("color=red") >= 0;
      String plain = ProgLineRenderer.stripHtml(html)[0];
      int idx = plain.lastIndexOf(" = ");
      if (idx > 0) {
         return new String[] { plain.substring(0, idx), plain.substring(idx + 3),
                               hadRed ? "1" : null };
      }
      return new String[] { plain, null, hadRed ? "1" : null };
   }

   public Dimension getPreferredSize() {
      FontMetrics fm = getFontMetrics(textFont);
      int w = 8 + fm.stringWidth(expr) + 16;
      if (result != null) {
         w += fm.stringWidth(" = " + result) + 8;
      }
      if (subProgram) {
         w += 80;
      }
      return new Dimension(w, fm.getHeight() + 6);
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

      g2.setFont(textFont);
      FontMetrics fm = g2.getFontMetrics();
      int baseline = (h - fm.getHeight()) / 2 + fm.getAscent();
      int x = 4;

      if (isError) {
         VSIcons.icon(VSIcons.ERROR, 13, p.error).paintIcon(this, g2, x, (h - 13) / 2);
         x += 17;
         g2.setColor(p.error);
         g2.drawString(expr, x, baseline);
         g2.dispose();
         return;
      }

      // a kifejezés szintaxis szerint színezve
      List<PlangSyntax.Token> toks = PlangSyntax.tokenize(expr);
      int cursor = 0;
      for (int t = 0; t < toks.size(); t++) {
         PlangSyntax.Token tk = toks.get(t);
         if (tk.start > cursor) {
            String gap = expr.substring(cursor, tk.start);
            g2.setColor(selected ? p.listSelectionFg : p.editorFg);
            g2.drawString(gap, x, baseline);
            x += fm.stringWidth(gap);
         }
         String piece = expr.substring(tk.start, tk.end);
         g2.setColor(color(tk.kind, selected));
         g2.drawString(piece, x, baseline);
         x += fm.stringWidth(piece);
         cursor = tk.end;
      }
      if (cursor < expr.length()) {
         String rest = expr.substring(cursor);
         g2.setColor(selected ? p.listSelectionFg : p.editorFg);
         g2.drawString(rest, x, baseline);
         x += fm.stringWidth(rest);
      }

      if (result != null) {
         g2.setColor(p.gutterFg);
         g2.drawString(" = ", x, baseline);
         x += fm.stringWidth(" = ");
         g2.setColor(p.success);
         g2.setFont(textFont.deriveFont(Font.BOLD));
         g2.drawString(result, x, baseline);
         x += g2.getFontMetrics().stringWidth(result);
         g2.setFont(textFont);
      }

      if (subProgram) {
         x += 8;
         String badge = "ALPROGRAM";
         Font small = Theme.ui(Font.BOLD, 10);
         g2.setFont(small);
         FontMetrics sfm = g2.getFontMetrics();
         int bw = sfm.stringWidth(badge) + 10;
         int bh = sfm.getHeight() + 1;
         int by = (h - bh) / 2;
         g2.setColor(p.badgeBg);
         g2.fillRoundRect(x, by, bw, bh, 8, 8);
         g2.setColor(p.badgeFg);
         g2.drawString(badge, x + 5, by + sfm.getAscent());
      }

      g2.dispose();
   }

   private Color color(int kind, boolean sel) {
      Theme.Palette p = Theme.p();
      switch (kind) {
         case PlangSyntax.KW:
            return p.synKeyword;
         case PlangSyntax.CTRL:
            return p.synControl;
         case PlangSyntax.TYPE_T:
            return p.synType;
         case PlangSyntax.STRING:
            return p.synString;
         case PlangSyntax.NUMBER:
            return p.synNumber;
         case PlangSyntax.COMMENT:
            return p.synComment;
         case PlangSyntax.FUNC:
            return p.synFunction;
         case PlangSyntax.CONST:
            return p.synConstant;
         case PlangSyntax.IDENT:
            return sel ? p.listSelectionFg : p.synVariable;
         default:
            return sel ? p.listSelectionFg : p.synOperator;
      }
   }
}
