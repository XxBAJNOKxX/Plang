package hu.ppke.itk.plang.gui.widgets;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

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
      /* Az osztó sávja a környezetével azonos színű, így csak az
         elválasztó vonal látszik belőle. */
      setBackground(Theme.p().editorBg);
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
                  drawDividerLine(g2, p, getOrientation(),
                                  0, 0, getWidth(), getHeight());
                  g2.dispose();
               }
            };
         }

         /**
          * Az elválasztó vonalat az UI delegált is felrajzolja. A divider egy
          * AWT Container gyerek, a paint()-je nem minden megjelenítési úton
          * hívódik meg (nyomtatás/headless rendereléskor nem), itt viszont
          * garantáltan sorra kerül.
          */
         public void paint(Graphics g, javax.swing.JComponent c) {
            Component d = getDivider();
            if (d == null) {
               return;
            }
            Rectangle r = d.getBounds();
            drawDividerLine(g, Theme.p(), getOrientation(),
                            r.x, r.y, r.width, r.height);
         }
      });
      setBorder(BorderFactory.createEmptyBorder());
   }

   /** Egyetlen éles pixelnyi elválasztó vonal a sáv közepén. */
   private static void drawDividerLine(Graphics g, Theme.Palette p, int orientation,
                                       int x, int y, int w, int h) {
      g.setColor(p.divider);
      if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
         g.fillRect(x + w / 2, y, 1, h);
      } else {
         g.fillRect(x, y + h / 2, w, 1);
      }
   }

   /** A téma váltása után frissíti a színeket. */
   public void applyTheme() {
      setBackground(Theme.p().editorBg);
      installUI();
      setDividerSize(5);
      repaint();
   }
}
