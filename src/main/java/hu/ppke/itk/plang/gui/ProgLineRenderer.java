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
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import hu.ppke.itk.plang.gui.editor.PlangSyntax;
import hu.ppke.itk.plang.gui.theme.Theme;

/**
 * Az értelmezett program soraink megjelenítése.
 *
 * A korábbi HTML-alapú megjelenítést saját rajzolás váltja fel: így a sorok
 * ugyanazt a szintaxis-színezést kapják, mint a szerkesztő, sorszámozással,
 * aktuális sor kiemeléssel és hibajelöléssel.
 */
public class ProgLineRenderer implements ListCellRenderer {

   private Font textFont;
   private final Cell cell = new Cell();
   private int currentLine = -1;
   private int gutterWidth = 44;

   public ProgLineRenderer(Font font) {
      this.textFont = font;
   }

   public void setFont(Font f) {
      this.textFont = f;
   }

   /** A végrehajtás aktuális sora (0-alapú), vagy -1. */
   public void setCurrentLine(int line) {
      this.currentLine = line;
   }

   public int getCurrentLine() {
      return currentLine;
   }

   public Component getListCellRendererComponent(JList list, Object value, int index,
                                                 boolean isSelected, boolean hasFocus) {
      cell.configure(value, index, isSelected, list);
      return cell;
   }

   /**
    * A HTML jelölést egyszerű szöveggé alakítja, megjegyezve, mely
    * szakaszok voltak hibásra színezve.
    */
   static String[] stripHtml(String html) {
      StringBuffer text = new StringBuffer();
      StringBuffer flags = new StringBuffer();
      boolean bad = false;
      int i = 0;
      while (i < html.length()) {
         char c = html.charAt(i);
         if (c == '<') {
            int end = html.indexOf('>', i);
            if (end < 0) {
               break;
            }
            String tag = html.substring(i + 1, end).toLowerCase();
            if (tag.startsWith("font") && tag.indexOf("red") >= 0) {
               bad = true;
            } else if (tag.equals("/font")) {
               bad = false;
            }
            i = end + 1;
            continue;
         }
         if (c == '&') {
            int semi = html.indexOf(';', i);
            if (semi > 0) {
               String ent = html.substring(i + 1, semi);
               char rep;
               if (ent.equals("lt")) {
                  rep = '<';
               } else if (ent.equals("gt")) {
                  rep = '>';
               } else if (ent.equals("amp")) {
                  rep = '&';
               } else if (ent.equals("nbsp")) {
                  rep = ' ';
               } else if (ent.equals("quot")) {
                  rep = '"';
               } else {
                  rep = '?';
               }
               text.append(rep);
               flags.append(bad ? '1' : '0');
               i = semi + 1;
               continue;
            }
         }
         text.append(c);
         flags.append(bad ? '1' : '0');
         i++;
      }
      return new String[] { text.toString(), flags.toString() };
   }

   /** A ténylegesen rajzoló komponens. */
   private final class Cell extends JComponent {
      private static final long serialVersionUID = 1L;

      private String text = "";
      private String badFlags = "";
      private boolean selected;
      private boolean error;
      private boolean current;
      private int lineNo;
      private String tip;

      void configure(Object value, int index, boolean isSelected, JList list) {
         this.selected = isSelected;
         this.lineNo = index;
         this.current = (index == currentLine);

         if (value instanceof ProgramLine) {
            ProgramLine pl = (ProgramLine) value;
            String[] r = stripHtml(pl.render());
            this.text = r[0];
            this.badFlags = r[1];
            this.error = pl.hasError();
            this.tip = pl.hasError() ? "HIBA: " + pl.getError() : null;
         } else {
            String[] r = stripHtml(String.valueOf(value));
            this.text = r[0];
            this.badFlags = r[1];
            this.error = false;
            this.tip = null;
         }
         setToolTipText(tip);
         setFont(textFont);
      }

      public Dimension getPreferredSize() {
         FontMetrics fm = getFontMetrics(textFont);
         return new Dimension(gutterWidth + fm.stringWidth(text) + 24, fm.getHeight() + 4);
      }

      public String getToolTipText() {
         return tip;
      }

      protected void paintComponent(Graphics g) {
         Graphics2D g2 = (Graphics2D) g.create();
         g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         Theme.Palette p = Theme.p();

         int w = getWidth();
         int h = getHeight();

         // Az alapháttér mindig az editor háttere, erre kerülnek az
         // áttetsző kiemelések – így a szintaxis-színek olvashatók maradnak
         // a kijelölt soron is (ahogy a VS Code hibakeresőjében).
         g2.setColor(error ? p.errorLineBg : p.editorBg);
         g2.fillRect(0, 0, w, h);

         if (current) {
            g2.setColor(Theme.alpha(p.warning, 40));
            g2.fillRect(0, 0, w, h);
         }
         if (selected) {
            g2.setColor(Theme.alpha(p.focusBorder, 48));
            g2.fillRect(0, 0, w, h);
            g2.setColor(Theme.alpha(p.focusBorder, 110));
            g2.drawLine(0, 0, w, 0);
            g2.drawLine(0, h - 1, w, h - 1);
            g2.setColor(p.focusBorder);
            g2.fillRect(0, 0, 3, h);
         } else if (current) {
            g2.setColor(p.warning);
            g2.fillRect(0, 0, 3, h);
         }

         g2.setFont(textFont);
         FontMetrics fm = g2.getFontMetrics();
         int baseline = (h - fm.getHeight()) / 2 + fm.getAscent();

         // sorszám
         String num = String.valueOf(lineNo + 1);
         g2.setColor(error ? p.error : (selected ? p.gutterActiveFg : p.gutterFg));
         g2.drawString(num, gutterWidth - 14 - fm.stringWidth(num), baseline);

         if (error) {
            g2.setColor(p.error);
            g2.fillOval(6, h / 2 - 3, 6, 6);
         }

         // szintaxis szerint színezett szöveg
         int x = gutterWidth;
         List<PlangSyntax.Token> toks = PlangSyntax.tokenize(text);
         int cursor = 0;
         for (int t = 0; t < toks.size(); t++) {
            PlangSyntax.Token tk = toks.get(t);
            if (tk.start > cursor) {
               String gapText = text.substring(cursor, tk.start);
               g2.setColor(p.editorFg);
               g2.drawString(gapText, x, baseline);
               x += fm.stringWidth(gapText);
            }
            String piece = text.substring(tk.start, tk.end);
            boolean isBad = anyBad(tk.start, tk.end);
            g2.setColor(isBad ? p.error : colorFor(tk.kind, selected));
            g2.drawString(piece, x, baseline);
            if (isBad) {
               // hullámos aláhúzás a hibás résznél
               int uy = baseline + 2;
               int x2 = x + fm.stringWidth(piece);
               for (int wx = x; wx < x2 - 1; wx += 4) {
                  g2.drawLine(wx, uy, wx + 2, uy + 1);
                  g2.drawLine(wx + 2, uy + 1, wx + 4, uy);
               }
            }
            x += fm.stringWidth(piece);
            cursor = tk.end;
         }
         if (cursor < text.length()) {
            String rest = text.substring(cursor);
            g2.setColor(p.editorFg);
            g2.drawString(rest, x, baseline);
         }

         g2.dispose();
      }

      private boolean anyBad(int from, int to) {
         for (int i = from; i < to && i < badFlags.length(); i++) {
            if (badFlags.charAt(i) == '1') {
               return true;
            }
         }
         return false;
      }

      private Color colorFor(int kind, boolean sel) {
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
               return p.synVariable;
            case PlangSyntax.OPERATOR:
               return p.synOperator;
            default:
               return p.editorFg;
         }
      }
   }
}
