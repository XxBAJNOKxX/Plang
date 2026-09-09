package hu.ppke.itk.plang.gui.widgets;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import hu.ppke.itk.plang.gui.theme.Theme;

/**
 * Vékony, díszítés nélküli osztott panel – a VS Code-ban is csak egy
 * egypixeles vonal választja el a területeket, amely húzáskor kék lesz.
 */
public class FlatSplitPane extends JSplitPane {

   private static final long serialVersionUID = 1L;

   public FlatSplitPane(int orientation, Component a, Component b) {
      super(orientation, true, a, b);
      setBorder(BorderFactory.createEmptyBorder());
      setDividerSize(5);
      setContinuousLayout(true);
      setOpaque(true);
      setBackground(Theme.p().border);
      installUI();
   }

   private void installUI() {
      setUI(new BasicSplitPaneUI() {
         public BasicSplitPaneDivider createDefaultDivider() {
            return new BasicSplitPaneDivider(this) {
               private static final long serialVersionUID = 1L;

               public void setBorder(javax.swing.border.Border b) {
                  // nincs keret
               }

               public void paint(Graphics g) {
                  Graphics2D g2 = (Graphics2D) g.create();
                  Theme.Palette p = Theme.p();
                  g2.setColor(p.editorBg);
                  g2.fillRect(0, 0, getWidth(), getHeight());
                  g2.setColor(p.border);
                  if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                     int x = getWidth() / 2;
                     g2.drawLine(x, 0, x, getHeight());
                  } else {
                     int y = getHeight() / 2;
                     g2.drawLine(0, y, getWidth(), y);
                  }
                  g2.dispose();
               }
            };
         }
      });
      setBorder(BorderFactory.createEmptyBorder());
   }

   /** A téma váltása után frissíti a színeket. */
   public void applyTheme() {
      setBackground(Theme.p().border);
      installUI();
      setDividerSize(5);
      repaint();
   }
}
