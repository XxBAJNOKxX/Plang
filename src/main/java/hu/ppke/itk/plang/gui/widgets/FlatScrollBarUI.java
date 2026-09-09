package hu.ppke.itk.plang.gui.widgets;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicScrollBarUI;

import hu.ppke.itk.plang.gui.theme.Theme;

/**
 * Keskeny, nyomógomb nélküli görgetősáv – ahogy a VS Code-ban látható.
 */
public class FlatScrollBarUI extends BasicScrollBarUI {

   private final int thickness;

   public FlatScrollBarUI() {
      this(12);
   }

   public FlatScrollBarUI(int thickness) {
      this.thickness = thickness;
   }

   /** A megadott görgetőpanel mindkét sávjára felteszi a lapos megjelenést. */
   public static void install(JScrollPane sp) {
      sp.getVerticalScrollBar().setUI(new FlatScrollBarUI());
      sp.getHorizontalScrollBar().setUI(new FlatScrollBarUI());
      sp.getVerticalScrollBar().setUnitIncrement(16);
      sp.getHorizontalScrollBar().setUnitIncrement(16);
      sp.setBorder(javax.swing.BorderFactory.createEmptyBorder());
      sp.getViewport().setOpaque(true);
      sp.setOpaque(true);
      Color bg = Theme.p().editorBg;
      sp.getViewport().setBackground(bg);
      sp.setBackground(bg);
      sp.getVerticalScrollBar().setOpaque(false);
      sp.getHorizontalScrollBar().setOpaque(false);
   }

   protected JButton createDecreaseButton(int orientation) {
      return zeroButton();
   }

   protected JButton createIncreaseButton(int orientation) {
      return zeroButton();
   }

   private JButton zeroButton() {
      JButton b = new JButton();
      b.setPreferredSize(new Dimension(0, 0));
      b.setMinimumSize(new Dimension(0, 0));
      b.setMaximumSize(new Dimension(0, 0));
      b.setFocusable(false);
      b.setBorder(javax.swing.BorderFactory.createEmptyBorder());
      return b;
   }

   protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
      // a sáv háttere megegyezik a tartalom hátterével
      Color bg = c.getParent() != null ? c.getParent().getBackground() : Theme.p().editorBg;
      g.setColor(bg != null ? bg : Theme.p().editorBg);
      g.fillRect(r.x, r.y, r.width, r.height);
   }

   protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
      if (r.isEmpty() || !scrollbar.isEnabled()) {
         return;
      }
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      Theme.Palette p = Theme.p();
      boolean active = isThumbRollover() || isDragging;
      g2.setColor(active ? p.scrollThumbHover : p.scrollThumb);
      int inset = 3;
      if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
         g2.fillRoundRect(r.x + inset, r.y + 1, Math.max(4, r.width - inset * 2), r.height - 2, 6, 6);
      } else {
         g2.fillRoundRect(r.x + 1, r.y + inset, r.width - 2, Math.max(4, r.height - inset * 2), 6, 6);
      }
      g2.dispose();
   }

   private boolean isDragging;

   protected void setThumbBounds(int x, int y, int width, int height) {
      super.setThumbBounds(x, y, width, height);
   }

   public Dimension getPreferredSize(JComponent c) {
      if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
         return new Dimension(thickness, 48);
      }
      return new Dimension(48, thickness);
   }
}
