package hu.ppke.itk.plang.gui.widgets;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;

import javax.swing.Icon;

import hu.ppke.itk.plang.gui.theme.Theme;

/**
 * Vektoros, a Codicon készlet szellemében rajzolt ikonok.
 *
 * Az ikonok mérete és színe futásidőben állítható, így élesek maradnak nagy
 * felbontású kijelzőn is, és követik a téma színeit.
 */
public final class VSIcons {

   public static final int FILES = 0;
   public static final int RUN = 1;
   public static final int SEARCH = 2;
   public static final int SETTINGS = 3;
   public static final int PLAY = 4;
   public static final int STOP = 5;
   public static final int PARSE = 6;
   public static final int EDIT = 7;
   public static final int COPY = 8;
   public static final int SAVE = 9;
   public static final int OPEN = 10;
   public static final int NEW = 11;
   public static final int STEP_INTO = 12;
   public static final int STEP_OUT = 13;
   public static final int CLOSE = 14;
   public static final int CHEVRON_DOWN = 15;
   public static final int CHEVRON_RIGHT = 16;
   public static final int THEME = 17;
   public static final int TERMINAL = 18;
   public static final int VARIABLES = 19;
   public static final int TREE = 20;
   public static final int ARROW_UP = 21;
   public static final int ARROW_DOWN = 22;
   public static final int ERROR = 23;
   public static final int CHECK = 24;
   public static final int INPUT = 25;
   public static final int OUTPUT = 26;
   public static final int CALLSTACK = 27;
   public static final int HELP = 28;

   /** Egy megrajzolt ikon. */
   public static final class VIcon implements Icon {
      private final int type;
      private final int size;
      private Color color;

      public VIcon(int type, int size, Color color) {
         this.type = type;
         this.size = size;
         this.color = color;
      }

      public void setColor(Color c) {
         this.color = c;
      }

      public Color getColor() {
         return color;
      }

      public int getIconWidth() {
         return size;
      }

      public int getIconHeight() {
         return size;
      }

      public void paintIcon(Component c, Graphics g, int x, int y) {
         Graphics2D g2 = (Graphics2D) g.create();
         g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
         g2.translate(x, y);
         double s = size / 16.0;
         g2.scale(s, s);
         g2.setColor(color != null ? color : Theme.p().activityBarFg);
         g2.setStroke(new BasicStroke(1.35f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
         draw(g2, type);
         g2.dispose();
      }
   }

   public static VIcon icon(int type, int size, Color color) {
      return new VIcon(type, size, color);
   }

   private static void draw(Graphics2D g, int type) {
      switch (type) {
         case FILES:
            // két lap egymáson
            g.drawRect(2, 2, 8, 10);
            g.drawLine(5, 5, 8, 5);
            g.drawLine(5, 7, 8, 7);
            g.setStroke(new BasicStroke(1.2f));
            g.drawRect(5, 4, 8, 10);
            break;

         case RUN: {
            // lejátszás + hibakereső "bogár"
            GeneralPath p = tri(3, 2, 12, 8, 3, 14);
            g.draw(p);
            break;
         }

         case SEARCH:
            g.draw(new Ellipse2D.Double(2.5, 2.5, 8, 8));
            g.drawLine(10, 10, 14, 14);
            break;

         case SETTINGS: {
            g.draw(new Ellipse2D.Double(5.5, 5.5, 5, 5));
            for (int i = 0; i < 8; i++) {
               double a = Math.PI * i / 4.0;
               double x1 = 8 + Math.cos(a) * 5.6;
               double y1 = 8 + Math.sin(a) * 5.6;
               double x2 = 8 + Math.cos(a) * 7.2;
               double y2 = 8 + Math.sin(a) * 7.2;
               g.drawLine((int) Math.round(x1), (int) Math.round(y1),
                          (int) Math.round(x2), (int) Math.round(y2));
            }
            break;
         }

         case PLAY:
            g.fill(tri(4, 2.5, 13, 8, 4, 13.5));
            break;

         case STOP:
            g.fillRect(4, 4, 8, 8);
            break;

         case PARSE:
            // "ellenőrzött lap"
            g.drawRect(3, 2, 8, 12);
            g.drawLine(5, 5, 9, 5);
            g.drawLine(5, 7, 9, 7);
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(7, 11, 9, 13);
            g.drawLine(9, 13, 14, 7);
            break;

         case EDIT:
            g.drawLine(2, 14, 4, 13);
            g.draw(quad(3.2, 12.2, 11, 2.5, 13.5, 5, 5.5, 13.8));
            g.drawLine(10, 3.5f == 0 ? 4 : 4, 12, 6);
            break;

         case COPY:
            g.drawRect(2, 2, 8, 8);
            g.drawRect(6, 6, 8, 8);
            break;

         case SAVE:
            g.drawRect(2, 2, 12, 12);
            g.drawRect(5, 2, 6, 4);
            g.drawRect(4, 9, 8, 5);
            break;

         case OPEN:
            g.drawLine(2, 5, 2, 13);
            g.drawLine(2, 13, 14, 13);
            g.drawLine(2, 5, 6, 5);
            g.drawLine(6, 5, 7, 7);
            g.drawLine(7, 7, 13, 7);
            g.drawLine(13, 7, 14, 13);
            break;

         case NEW:
            g.drawRect(3, 2, 9, 12);
            g.drawLine(7.5f == 0 ? 7 : 7, 6, 7, 10);
            g.drawLine(5, 8, 9, 8);
            break;

         case STEP_INTO:
            g.drawLine(8, 2, 8, 9);
            g.fill(tri(5, 8, 11, 8, 8, 12));
            g.draw(new Ellipse2D.Double(6, 12.5, 4, 3));
            break;

         case STEP_OUT:
            g.drawLine(8, 12, 8, 5);
            g.fill(tri(5, 6, 11, 6, 8, 2));
            g.draw(new Ellipse2D.Double(6, 12.5, 4, 3));
            break;

         case CLOSE:
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(4, 4, 12, 12);
            g.drawLine(12, 4, 4, 12);
            break;

         case CHEVRON_DOWN:
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(4, 6, 8, 10);
            g.drawLine(8, 10, 12, 6);
            break;

         case CHEVRON_RIGHT:
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(6, 4, 10, 8);
            g.drawLine(10, 8, 6, 12);
            break;

         case THEME:
            g.draw(new Ellipse2D.Double(3, 3, 10, 10));
            GeneralPath half = new GeneralPath();
            half.moveTo(8, 3);
            half.append(new java.awt.geom.Arc2D.Double(3, 3, 10, 10, 90, -180, java.awt.geom.Arc2D.PIE), true);
            g.fill(half);
            break;

         case TERMINAL:
            g.drawRect(1.5f == 0 ? 1 : 1, 2, 13, 11);
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(4, 6, 6.5f == 0 ? 6 : 6, 8);
            g.drawLine(6, 8, 4, 10);
            g.drawLine(8, 10, 12, 10);
            break;

         case VARIABLES:
            // {x}
            g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(quadOpen(6, 2, 4, 4, 4, 8, 2, 8));
            g.draw(quadOpen(6, 14, 4, 12, 4, 8, 2, 8));
            g.draw(quadOpen(10, 2, 12, 4, 12, 8, 14, 8));
            g.draw(quadOpen(10, 14, 12, 12, 12, 8, 14, 8));
            break;

         case TREE:
            g.drawLine(3, 3, 3, 13);
            g.drawLine(3, 6, 7, 6);
            g.drawLine(3, 10, 7, 10);
            g.drawLine(3, 13, 7, 13);
            g.fillRect(7, 4.5f == 0 ? 4 : 4, 6, 3);
            g.drawRect(7, 8.5f == 0 ? 8 : 8, 6, 3);
            g.drawRect(7, 12, 6, 3);
            break;

         case ARROW_UP:
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(8, 12, 8, 4);
            g.drawLine(4, 8, 8, 4);
            g.drawLine(12, 8, 8, 4);
            break;

         case ARROW_DOWN:
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(8, 4, 8, 12);
            g.drawLine(4, 8, 8, 12);
            g.drawLine(12, 8, 8, 12);
            break;

         case ERROR:
            g.draw(new Ellipse2D.Double(2, 2, 12, 12));
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(6, 6, 10, 10);
            g.drawLine(10, 6, 6, 10);
            break;

         case CHECK:
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(3, 8, 6.5f == 0 ? 6 : 6, 12);
            g.drawLine(6, 12, 13, 4);
            break;

         case INPUT:
            g.drawRect(2, 3, 12, 10);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(5, 8, 11, 8);
            g.drawLine(8, 5, 11, 8);
            g.drawLine(8, 11, 11, 8);
            break;

         case OUTPUT:
            g.drawRect(2, 3, 12, 10);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(5, 6, 11, 6);
            g.drawLine(5, 9, 9, 9);
            break;

         case CALLSTACK:
            g.drawRect(2, 3, 12, 3);
            g.drawRect(2, 7, 12, 3);
            g.drawRect(2, 11, 12, 3);
            break;

         case HELP:
            g.draw(new Ellipse2D.Double(2, 2, 12, 12));
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(quadOpen(6, 6, 6, 4, 10, 4, 10, 6.5));
            g.drawLine(10, 6, 8, 9);
            g.drawLine(8, 9, 8, 10);
            g.fillOval(7, 11, 2, 2);
            break;

         default:
            g.drawRect(3, 3, 10, 10);
      }
   }

   private static GeneralPath tri(double x1, double y1, double x2, double y2, double x3, double y3) {
      GeneralPath p = new GeneralPath(Path2D.WIND_NON_ZERO);
      p.moveTo(x1, y1);
      p.lineTo(x2, y2);
      p.lineTo(x3, y3);
      p.closePath();
      return p;
   }

   private static GeneralPath quad(double x1, double y1, double x2, double y2,
                                   double x3, double y3, double x4, double y4) {
      GeneralPath p = new GeneralPath(Path2D.WIND_NON_ZERO);
      p.moveTo(x1, y1);
      p.lineTo(x2, y2);
      p.lineTo(x3, y3);
      p.lineTo(x4, y4);
      p.closePath();
      return p;
   }

   private static GeneralPath quadOpen(double x1, double y1, double x2, double y2,
                                       double x3, double y3, double x4, double y4) {
      GeneralPath p = new GeneralPath(Path2D.WIND_NON_ZERO);
      p.moveTo(x1, y1);
      p.lineTo(x2, y2);
      p.lineTo(x3, y3);
      p.lineTo(x4, y4);
      return p;
   }

   private VSIcons() {
   }
}
