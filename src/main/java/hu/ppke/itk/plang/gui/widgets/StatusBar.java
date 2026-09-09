package hu.ppke.itk.plang.gui.widgets;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import hu.ppke.itk.plang.gui.theme.Theme;

/**
 * A VS Code alsó állapotsora.
 *
 * Bal oldalon a fordítási állapot és a hibák száma, jobb oldalon a kurzor
 * pozíciója, a nyelv és a téma látszik. Futás közben a sáv narancssárgára vált,
 * ahogy a VS Code is jelzi a hibakeresési munkamenetet.
 */
public class StatusBar extends JPanel {

   private static final long serialVersionUID = 1L;

   /** Egy állapotsor-elem. */
   public static final class Cell {
      public final String id;
      public String text;
      public int iconType = -1;
      public Color iconColor;
      public boolean rightSide;
      public Runnable action;
      public String tooltip;
      int x;
      int w;

      Cell(String id, String text, boolean rightSide) {
         this.id = id;
         this.text = text;
         this.rightSide = rightSide;
      }
   }

   private final List<Cell> cells = new ArrayList<Cell>();
   private int hovered = -1;
   private boolean running;
   private final int height = 22;

   public StatusBar() {
      setOpaque(true);
      setPreferredSize(new Dimension(100, height));
      setFont(Theme.ui(java.awt.Font.PLAIN, 11));
      applyTheme();

      MouseAdapter ma = new MouseAdapter() {
         public void mouseMoved(MouseEvent e) {
            int idx = indexAt(e.getPoint());
            if (idx != hovered) {
               hovered = idx;
               boolean clickable = idx >= 0 && cells.get(idx).action != null;
               setCursor(Cursor.getPredefinedCursor(
                  clickable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
               setToolTipText(idx >= 0 ? cells.get(idx).tooltip : null);
               repaint();
            }
         }

         public void mouseExited(MouseEvent e) {
            hovered = -1;
            repaint();
         }

         public void mousePressed(MouseEvent e) {
            int idx = indexAt(e.getPoint());
            if (idx >= 0 && cells.get(idx).action != null) {
               cells.get(idx).action.run();
            }
         }
      };
      addMouseListener(ma);
      addMouseMotionListener(ma);
   }

   public void applyTheme() {
      setBackground(running ? Theme.p().statusRunBg : Theme.p().statusBg);
      repaint();
   }

   public void setRunning(boolean b) {
      running = b;
      applyTheme();
   }

   public boolean isRunning() {
      return running;
   }

   public Cell add(String id, String text, boolean rightSide) {
      Cell c = new Cell(id, text, rightSide);
      cells.add(c);
      repaint();
      return c;
   }

   public Cell cell(String id) {
      for (int i = 0; i < cells.size(); i++) {
         if (cells.get(i).id.equals(id)) {
            return cells.get(i);
         }
      }
      return null;
   }

   public void setText(String id, String text) {
      Cell c = cell(id);
      if (c != null && (c.text == null || !c.text.equals(text))) {
         c.text = text;
         repaint();
      }
   }

   public void setIcon(String id, int iconType, Color color) {
      Cell c = cell(id);
      if (c != null) {
         c.iconType = iconType;
         c.iconColor = color;
         repaint();
      }
   }

   private void layoutCells() {
      FontMetrics fm = getFontMetrics(getFont());
      int left = 8;
      int right = getWidth() - 8;

      for (int i = 0; i < cells.size(); i++) {
         Cell c = cells.get(i);
         if (c.rightSide) {
            continue;
         }
         int w = cellWidth(c, fm);
         c.x = left;
         c.w = w;
         left += w;
      }
      for (int i = cells.size() - 1; i >= 0; i--) {
         Cell c = cells.get(i);
         if (!c.rightSide) {
            continue;
         }
         int w = cellWidth(c, fm);
         right -= w;
         c.x = right;
         c.w = w;
      }
   }

   private int cellWidth(Cell c, FontMetrics fm) {
      int w = 16;
      if (c.iconType >= 0) {
         w += 14 + 4;
      }
      if (c.text != null) {
         w += fm.stringWidth(c.text);
      }
      return w;
   }

   private int indexAt(Point pt) {
      layoutCells();
      for (int i = 0; i < cells.size(); i++) {
         Cell c = cells.get(i);
         if (pt.x >= c.x && pt.x < c.x + c.w) {
            return i;
         }
      }
      return -1;
   }

   public Dimension getPreferredSize() {
      return new Dimension(super.getPreferredSize().width, height);
   }

   protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                          RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      Theme.Palette p = Theme.p();

      Color bg = running ? p.statusRunBg : p.statusBg;
      g2.setColor(bg);
      g2.fillRect(0, 0, getWidth(), getHeight());

      layoutCells();
      FontMetrics fm = g2.getFontMetrics(getFont());
      g2.setFont(getFont());

      for (int i = 0; i < cells.size(); i++) {
         Cell c = cells.get(i);
         if (i == hovered && c.action != null) {
            g2.setColor(p.statusHover);
            g2.fillRect(c.x, 0, c.w, height);
         }
         int x = c.x + 8;
         if (c.iconType >= 0) {
            VSIcons.icon(c.iconType, 14, c.iconColor != null ? c.iconColor : p.statusFg)
                   .paintIcon(this, g2, x, (height - 14) / 2);
            x += 18;
         }
         if (c.text != null) {
            g2.setColor(p.statusFg);
            g2.drawString(c.text, x, (height - fm.getHeight()) / 2 + fm.getAscent());
         }
      }

      g2.dispose();
   }
}
