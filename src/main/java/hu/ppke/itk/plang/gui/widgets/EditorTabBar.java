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
 * VS Code stílusú fülsor a szerkesztő felett.
 *
 * A fülek ikonnal, névvel és – módosítás esetén – ponttal jelennek meg;
 * az aktív fül felső élén kék csík fut.
 */
public class EditorTabBar extends JPanel {

   private static final long serialVersionUID = 1L;

   /** Egy fül leírója. */
   public static final class Tab {
      public final String id;
      public String title;
      public int iconType;
      public boolean dirty;
      public boolean closable;
      int x;
      int w;

      public Tab(String id, String title, int iconType, boolean closable) {
         this.id = id;
         this.title = title;
         this.iconType = iconType;
         this.closable = closable;
      }
   }

   /** Fülváltás- és bezárásfigyelő. */
   public interface Listener {
      void tabSelected(String id);
      void tabClosed(String id);
   }

   /** Szerkesztőfül-stílus (kitöltött háttér, felső kék csík). */
   public static final int STYLE_EDITOR = 0;
   /** Panelfül-stílus (csak felirat, alul aláhúzás) – az alsó panelhez. */
   public static final int STYLE_PANEL = 1;

   private final List<Tab> tabs = new ArrayList<Tab>();
   private int selected = 0;
   private int hovered = -1;
   private boolean hoverClose;
   private Listener listener;
   private int style = STYLE_EDITOR;
   private int height = 35;

   public EditorTabBar() {
      setOpaque(true);
      setPreferredSize(new Dimension(100, height));
      setFont(Theme.uiPlain());
      applyTheme();

      MouseAdapter ma = new MouseAdapter() {
         public void mouseMoved(MouseEvent e) {
            int idx = indexAt(e.getPoint());
            boolean hc = idx >= 0 && overClose(idx, e.getPoint());
            if (idx != hovered || hc != hoverClose) {
               hovered = idx;
               hoverClose = hc;
               setCursor(Cursor.getPredefinedCursor(
                  idx >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
               repaint();
            }
         }

         public void mouseExited(MouseEvent e) {
            hovered = -1;
            hoverClose = false;
            repaint();
         }

         public void mousePressed(MouseEvent e) {
            int idx = indexAt(e.getPoint());
            if (idx < 0) {
               return;
            }
            Tab t = tabs.get(idx);
            if (t.closable && overClose(idx, e.getPoint())) {
               if (listener != null) {
                  listener.tabClosed(t.id);
               }
               return;
            }
            selected = idx;
            repaint();
            if (listener != null) {
               listener.tabSelected(t.id);
            }
         }
      };
      addMouseListener(ma);
      addMouseMotionListener(ma);
   }

   public void applyTheme() {
      setBackground(style == STYLE_PANEL ? Theme.p().panelBg : Theme.p().tabBarBg);
      repaint();
   }

   /** Beállítja a fülsor stílusát (szerkesztő- vagy panelfülek). */
   public void setStyle(int style) {
      this.style = style;
      this.height = (style == STYLE_PANEL) ? 30 : 35;
      setPreferredSize(new Dimension(100, height));
      applyTheme();
      revalidate();
      repaint();
   }

   public int getStyle() {
      return style;
   }

   public void setListener(Listener l) {
      this.listener = l;
   }

   public void addTab(Tab t) {
      tabs.add(t);
      revalidate();
      repaint();
   }

   public void removeTab(String id) {
      for (int i = 0; i < tabs.size(); i++) {
         if (tabs.get(i).id.equals(id)) {
            tabs.remove(i);
            if (selected >= tabs.size()) {
               selected = Math.max(0, tabs.size() - 1);
            }
            revalidate();
            repaint();
            return;
         }
      }
   }

   public void clearTabs() {
      tabs.clear();
      selected = 0;
      revalidate();
      repaint();
   }

   public Tab getTab(String id) {
      for (int i = 0; i < tabs.size(); i++) {
         if (tabs.get(i).id.equals(id)) {
            return tabs.get(i);
         }
      }
      return null;
   }

   public int tabCount() {
      return tabs.size();
   }

   public String getSelectedId() {
      return (selected >= 0 && selected < tabs.size()) ? tabs.get(selected).id : null;
   }

   public void select(String id) {
      for (int i = 0; i < tabs.size(); i++) {
         if (tabs.get(i).id.equals(id)) {
            selected = i;
            repaint();
            return;
         }
      }
   }

   public void setDirty(String id, boolean dirty) {
      Tab t = getTab(id);
      if (t != null && t.dirty != dirty) {
         t.dirty = dirty;
         repaint();
      }
   }

   public void setTitle(String id, String title) {
      Tab t = getTab(id);
      if (t != null) {
         t.title = title;
         revalidate();
         repaint();
      }
   }

   private void layoutTabs() {
      FontMetrics fm = getFontMetrics(getFont());
      int x = (style == STYLE_PANEL) ? 8 : 0;
      for (int i = 0; i < tabs.size(); i++) {
         Tab t = tabs.get(i);
         int w;
         if (style == STYLE_PANEL) {
            w = 14 + fm.stringWidth(t.title) + 14 + (t.iconType >= 0 ? 20 : 0);
         } else {
            w = 16 + 16 + 8 + fm.stringWidth(t.title) + (t.closable ? 26 : 12);
         }
         t.x = x;
         t.w = w;
         x += w;
      }
   }

   private int indexAt(Point pt) {
      layoutTabs();
      for (int i = 0; i < tabs.size(); i++) {
         Tab t = tabs.get(i);
         if (pt.x >= t.x && pt.x < t.x + t.w) {
            return i;
         }
      }
      return -1;
   }

   private boolean overClose(int idx, Point pt) {
      Tab t = tabs.get(idx);
      if (!t.closable) {
         return false;
      }
      int cx = t.x + t.w - 22;
      return pt.x >= cx && pt.x <= cx + 16;
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

      g2.setColor(style == STYLE_PANEL ? p.panelBg : p.tabBarBg);
      g2.fillRect(0, 0, getWidth(), getHeight());

      layoutTabs();
      FontMetrics fm = g2.getFontMetrics(getFont());

      if (style == STYLE_PANEL) {
         paintPanelTabs(g2, p, fm);
         g2.dispose();
         return;
      }

      for (int i = 0; i < tabs.size(); i++) {
         Tab t = tabs.get(i);
         boolean isSel = (i == selected);

         g2.setColor(isSel ? p.tabActiveBg : p.tabInactiveBg);
         g2.fillRect(t.x, 0, t.w, height);

         if (isSel) {
            g2.setColor(p.tabActiveTopBorder);
            g2.fillRect(t.x, 0, t.w, 1);
         }

         g2.setColor(p.tabBorder);
         g2.drawLine(t.x + t.w - 1, 0, t.x + t.w - 1, height);

         Color fg = isSel ? p.tabActiveFg : p.tabInactiveFg;

         VSIcons.VIcon ic = VSIcons.icon(t.iconType, 16, iconColor(t.iconType, fg));
         ic.paintIcon(this, g2, t.x + 12, (height - 16) / 2);

         g2.setFont(getFont());
         g2.setColor(fg);
         int tx = t.x + 12 + 16 + 8;
         g2.drawString(t.title, tx, (height - fm.getHeight()) / 2 + fm.getAscent());

         if (t.closable) {
            int cx = t.x + t.w - 22;
            int cy = (height - 16) / 2;
            if (t.dirty && !(hovered == i && hoverClose)) {
               g2.setColor(fg);
               g2.fillOval(cx + 4, cy + 4, 8, 8);
            } else if (isSel || hovered == i) {
               if (hovered == i && hoverClose) {
                  g2.setColor(p.listHover);
                  g2.fillRoundRect(cx - 1, cy - 1, 18, 18, 4, 4);
               }
               VSIcons.icon(VSIcons.CLOSE, 16, fg).paintIcon(this, g2, cx, cy);
            }
         }
      }

      // alsó szegély a füleken túl
      g2.setColor(p.tabBorder);
      g2.drawLine(0, height - 1, getWidth(), height - 1);
      if (!tabs.isEmpty()) {
         Tab sel = tabs.get(Math.min(selected, tabs.size() - 1));
         g2.setColor(p.tabActiveBg);
         g2.drawLine(sel.x, height - 1, sel.x + sel.w - 1, height - 1);
      }

      g2.dispose();
   }

   /** Panelfülek: csak felirat, az aktív alatt vékony csík (VSC alsó panel). */
   private void paintPanelTabs(Graphics2D g2, Theme.Palette p, FontMetrics fm) {
      for (int i = 0; i < tabs.size(); i++) {
         Tab t = tabs.get(i);
         boolean isSel = (i == selected);
         Color fg = isSel ? p.tabActiveFg : p.tabInactiveFg;

         if (hovered == i && !isSel) {
            g2.setColor(p.listHover);
            g2.fillRect(t.x, 2, t.w, height - 4);
         }

         int x = t.x + 14;
         if (t.iconType >= 0) {
            VSIcons.icon(t.iconType, 14, iconColor(t.iconType, fg))
                   .paintIcon(this, g2, x - 4, (height - 14) / 2);
            x += 16;
         }
         g2.setFont(getFont());
         g2.setColor(fg);
         g2.drawString(t.title, x, (height - fm.getHeight()) / 2 + fm.getAscent());

         if (isSel) {
            g2.setColor(p.tabActiveTopBorder);
            g2.fillRect(t.x + 8, height - 2, t.w - 16, 2);
         }
      }
      g2.setColor(p.border);
      g2.drawLine(0, height - 1, getWidth(), height - 1);
   }

   private Color iconColor(int type, Color fallback) {
      Theme.Palette p = Theme.p();
      if (type == VSIcons.FILES || type == VSIcons.NEW) {
         return p.synKeyword;
      }
      if (type == VSIcons.INPUT) {
         return p.info;
      }
      if (type == VSIcons.OUTPUT) {
         return p.success;
      }
      return fallback;
   }
}
