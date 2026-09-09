package hu.ppke.itk.plang.gui.widgets;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import hu.ppke.itk.plang.gui.theme.Theme;

/**
 * Lapos, VS Code stílusú gomb: nincs 3D keret, csak finom hover-háttér.
 * Ikonos (eszköztár) és feliratos (elsődleges / másodlagos) változatban is
 * használható.
 */
public class FlatButton extends JComponent {

   private static final long serialVersionUID = 1L;

   public static final int TOOL = 0;      // csak ikon, átlátszó háttér
   public static final int PRIMARY = 1;   // kék, kitöltött
   public static final int SECONDARY = 2; // szürke, kitöltött
   public static final int TEXT = 3;      // csak szöveg

   private final int variant;
   private Icon icon;
   private String text;
   private String tooltip;
   private Action action;
   private boolean hover;
   private boolean pressed;
   private boolean enabledState = true;
   private int padX = 10;
   private int padY = 5;
   private int gap = 6;
   private Color accent;

   public FlatButton(int variant) {
      this.variant = variant;
      setOpaque(false);
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      setFont(Theme.uiPlain());
      addMouseListener(new MouseAdapter() {
         public void mouseEntered(MouseEvent e) {
            if (enabledState) {
               hover = true;
               repaint();
            }
         }

         public void mouseExited(MouseEvent e) {
            hover = false;
            pressed = false;
            repaint();
         }

         public void mousePressed(MouseEvent e) {
            if (enabledState && e.getButton() == MouseEvent.BUTTON1) {
               pressed = true;
               repaint();
            }
         }

         public void mouseReleased(MouseEvent e) {
            boolean wasPressed = pressed;
            pressed = false;
            repaint();
            if (wasPressed && enabledState && contains(e.getPoint())) {
               fire();
            }
         }
      });
   }

   public FlatButton(int variant, Icon icon, String tooltip) {
      this(variant);
      this.icon = icon;
      this.tooltip = tooltip;
      setToolTipText(tooltip);
   }

   public FlatButton(int variant, String text) {
      this(variant);
      this.text = text;
   }

   public void setAction(Action a) {
      this.action = a;
      if (a != null) {
         Object nm = a.getValue(Action.NAME);
         Object sd = a.getValue(Action.SHORT_DESCRIPTION);
         if (text == null && nm != null && variant != TOOL) {
            text = nm.toString();
         }
         if (sd != null) {
            tooltip = sd.toString();
            setToolTipText(tooltip);
         } else if (nm != null) {
            setToolTipText(nm.toString());
         }
         setEnabled(a.isEnabled());
         a.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
               if ("enabled".equals(evt.getPropertyName())) {
                  setEnabled(((Boolean) evt.getNewValue()).booleanValue());
               }
            }
         });
      }
   }

   private void fire() {
      if (action != null) {
         action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "click"));
      }
      for (ActionListener l : listeners) {
         l.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "click"));
      }
   }

   private final java.util.List<ActionListener> listeners = new java.util.ArrayList<ActionListener>();

   public void addActionListener(ActionListener l) {
      listeners.add(l);
   }

   public void setIcon(Icon i) {
      this.icon = i;
      repaint();
   }

   public Icon getIcon() {
      return icon;
   }

   public void setText(String t) {
      this.text = t;
      revalidate();
      repaint();
   }

   public String getText() {
      return text;
   }

   public void setAccent(Color c) {
      this.accent = c;
      repaint();
   }

   public void setPadding(int x, int y) {
      this.padX = x;
      this.padY = y;
      revalidate();
   }

   public void setEnabled(boolean b) {
      this.enabledState = b;
      super.setEnabled(b);
      setCursor(Cursor.getPredefinedCursor(b ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
      repaint();
   }

   public boolean isEnabled() {
      return enabledState;
   }

   public Dimension getPreferredSize() {
      FontMetrics fm = getFontMetrics(getFont());
      int w = padX * 2;
      int h = padY * 2;
      int ih = 0;
      if (icon != null) {
         w += icon.getIconWidth();
         ih = icon.getIconHeight();
      }
      if (text != null && text.length() > 0) {
         if (icon != null) {
            w += gap;
         }
         w += fm.stringWidth(text);
         ih = Math.max(ih, fm.getHeight());
      }
      h += ih;
      return new Dimension(w, h);
   }

   public Dimension getMinimumSize() {
      return getPreferredSize();
   }

   protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                          RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

      Theme.Palette p = Theme.p();
      int w = getWidth();
      int h = getHeight();

      Color bg = null;
      Color fg;

      if (variant == PRIMARY) {
         bg = pressed ? p.buttonHover : (hover ? p.buttonHover : p.buttonBg);
         fg = p.buttonFg;
      } else if (variant == SECONDARY) {
         bg = hover ? p.listHover : p.buttonSecondaryBg;
         fg = p.buttonSecondaryFg;
      } else {
         bg = hover ? p.listHover : null;
         fg = accent != null ? accent : p.titleBarFg;
      }

      if (!enabledState) {
         fg = p.gutterFg;
         if (variant == PRIMARY || variant == SECONDARY) {
            bg = Theme.alpha(p.buttonSecondaryBg, 90);
         } else {
            bg = null;
         }
      }

      if (bg != null) {
         g2.setColor(bg);
         g2.fillRoundRect(0, 0, w, h, variant == TOOL ? 5 : 4, variant == TOOL ? 5 : 4);
      }

      FontMetrics fm = g2.getFontMetrics(getFont());
      int cw = 0;
      if (icon != null) {
         cw += icon.getIconWidth();
      }
      if (text != null && text.length() > 0) {
         if (icon != null) {
            cw += gap;
         }
         cw += fm.stringWidth(text);
      }
      int x = (w - cw) / 2;

      if (icon != null) {
         Icon ic = icon;
         if (ic instanceof VSIcons.VIcon) {
            ((VSIcons.VIcon) ic).setColor(fg);
         }
         int iy = (h - ic.getIconHeight()) / 2;
         ic.paintIcon(this, g2, x, iy);
         x += ic.getIconWidth() + gap;
      }

      if (text != null && text.length() > 0) {
         g2.setFont(getFont());
         g2.setColor(fg);
         int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
         g2.drawString(text, x, ty);
      }

      g2.dispose();
   }
}
