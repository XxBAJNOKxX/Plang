package hu.ppke.itk.plang.gui.widgets;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
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
 * A VS Code bal szélén látható függőleges ikonsáv.
 *
 * Felül a nézetváltó gombok (Kód, Futtatás, Keresés), alul a beállítások.
 * Az aktív elem mellett bal oldalon fehér jelölősáv fut.
 */
public class ActivityBar extends JPanel {

   private static final long serialVersionUID = 1L;

   /** Egy sávelem. */
   public static final class Item {
      final int iconType;
      final String tooltip;
      final boolean bottom;
      final Runnable action;
      final boolean toggle;
      int y;
      int h;

      Item(int iconType, String tooltip, boolean bottom, boolean toggle, Runnable action) {
         this.iconType = iconType;
         this.tooltip = tooltip;
         this.bottom = bottom;
         this.toggle = toggle;
         this.action = action;
      }
   }

   private final List<Item> items = new ArrayList<Item>();
   private int selected = 0;
   private int hovered = -1;
   private final int barWidth = 48;
   private final int itemHeight = 48;

   public ActivityBar() {
      setOpaque(true);
      setPreferredSize(new Dimension(barWidth, 100));
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      applyTheme();

      MouseAdapter ma = new MouseAdapter() {
         public void mouseMoved(MouseEvent e) {
            int idx = indexAt(e.getPoint());
            if (idx != hovered) {
               hovered = idx;
               setToolTipText(idx >= 0 ? items.get(idx).tooltip : null);
               repaint();
            }
         }

         public void mouseExited(MouseEvent e) {
            hovered = -1;
            repaint();
         }

         public void mousePressed(MouseEvent e) {
            int idx = indexAt(e.getPoint());
            if (idx >= 0) {
               Item it = items.get(idx);
               if (it.toggle) {
                  selected = idx;
               }
               repaint();
               if (it.action != null) {
                  it.action.run();
               }
            }
         }
      };
      addMouseListener(ma);
      addMouseMotionListener(ma);
   }

   public void applyTheme() {
      setBackground(Theme.p().activityBar);
      repaint();
   }

   /** Nézetváltó elem a sáv tetején. */
   public int addView(int iconType, String tooltip, Runnable action) {
      items.add(new Item(iconType, tooltip, false, true, action));
      revalidate();
      repaint();
      return items.size() - 1;
   }

   /** Parancsgomb a sáv alján (nem vált nézetet). */
   public void addBottomAction(int iconType, String tooltip, Runnable action) {
      items.add(new Item(iconType, tooltip, true, false, action));
      revalidate();
      repaint();
   }

   public void setSelected(int index) {
      selected = index;
      repaint();
   }

   public int getSelected() {
      return selected;
   }

   private void layoutItems() {
      int top = 0;
      int bottom = getHeight();
      for (int i = 0; i < items.size(); i++) {
         Item it = items.get(i);
         if (!it.bottom) {
            it.y = top;
            it.h = itemHeight;
            top += itemHeight;
         }
      }
      for (int i = items.size() - 1; i >= 0; i--) {
         Item it = items.get(i);
         if (it.bottom) {
            bottom -= itemHeight;
            it.y = bottom;
            it.h = itemHeight;
         }
      }
   }

   private int indexAt(Point pt) {
      layoutItems();
      for (int i = 0; i < items.size(); i++) {
         Item it = items.get(i);
         if (pt.y >= it.y && pt.y < it.y + it.h) {
            return i;
         }
      }
      return -1;
   }

   public Dimension getPreferredSize() {
      return new Dimension(barWidth, super.getPreferredSize().height);
   }

   protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      Theme.Palette p = Theme.p();

      g2.setColor(p.activityBar);
      g2.fillRect(0, 0, getWidth(), getHeight());

      layoutItems();

      for (int i = 0; i < items.size(); i++) {
         Item it = items.get(i);
         boolean isSel = it.toggle && i == selected;
         boolean isHover = i == hovered;

         Color fg = isSel ? p.activityBarActiveFg
                          : (isHover ? p.activityBarActiveFg : p.activityBarFg);

         if (isSel) {
            g2.setColor(p.activityBarActiveBorder);
            g2.fillRect(0, it.y, 2, it.h);
         }

         VSIcons.VIcon ic = VSIcons.icon(it.iconType, 24, fg);
         int x = (getWidth() - 24) / 2;
         int y = it.y + (it.h - 24) / 2;
         ic.paintIcon(this, g2, x, y);
      }

      // jobb oldali elválasztó
      g2.setColor(p.border);
      g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());

      g2.dispose();
   }
}
