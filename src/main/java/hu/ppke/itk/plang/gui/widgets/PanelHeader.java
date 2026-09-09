package hu.ppke.itk.plang.gui.widgets;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import hu.ppke.itk.plang.gui.theme.Theme;

/**
 * Szekciócím a VS Code oldalsávjának mintájára: kicsi, ritkított nagybetűs
 * felirat, opcionális ikonnal és jobb oldali gombokkal.
 */
public class PanelHeader extends JPanel {

   private static final long serialVersionUID = 1L;

   private String title;
   private int iconType = -1;
   private Color iconColor;
   private final JPanel actions;
   private boolean uppercase = true;
   private boolean drawBackground = true;

   public PanelHeader(String title) {
      this(title, -1, null);
   }

   public PanelHeader(String title, int iconType, Color iconColor) {
      super(new BorderLayout());
      this.title = title;
      this.iconType = iconType;
      this.iconColor = iconColor;
      setOpaque(true);
      setFont(Theme.uiSmallBold());
      setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));

      actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
      actions.setOpaque(false);
      add(actions, BorderLayout.EAST);

      applyTheme();
   }

   public void applyTheme() {
      setBackground(Theme.p().sideBar);
      actions.setBackground(Theme.p().sideBar);
      repaint();
   }

   public void setTitle(String t) {
      this.title = t;
      repaint();
   }

   public String getTitle() {
      return title;
   }

   public void setUppercase(boolean b) {
      this.uppercase = b;
      repaint();
   }

   public void setDrawBackground(boolean b) {
      this.drawBackground = b;
      repaint();
   }

   public void addAction(Component c) {
      actions.add(c);
      revalidate();
      repaint();
   }

   public void clearActions() {
      actions.removeAll();
      revalidate();
      repaint();
   }

   public Dimension getPreferredSize() {
      Dimension d = super.getPreferredSize();
      return new Dimension(d.width, Math.max(28, d.height));
   }

   protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                          RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      Theme.Palette p = Theme.p();

      if (drawBackground) {
         g2.setColor(p.sideBar);
         g2.fillRect(0, 0, getWidth(), getHeight());
      }

      int x = 12;
      if (iconType >= 0) {
         VSIcons.icon(iconType, 14, iconColor != null ? iconColor : p.sideBarTitleFg)
                .paintIcon(this, g2, x, (getHeight() - 14) / 2);
         x += 20;
      }

      g2.setFont(getFont());
      FontMetrics fm = g2.getFontMetrics();
      g2.setColor(p.sideBarTitleFg);
      String t = uppercase ? title.toUpperCase() : title;
      // ritkított betűköz a VSC szekciócímek hatásához
      int cx = x;
      for (int i = 0; i < t.length(); i++) {
         String ch = String.valueOf(t.charAt(i));
         g2.drawString(ch, cx, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
         cx += fm.stringWidth(ch) + (uppercase ? 1 : 0);
      }

      g2.dispose();
   }
}
