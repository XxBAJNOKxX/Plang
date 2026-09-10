package hu.ppke.itk.plang.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import hu.ppke.itk.plang.gui.theme.Theme;
import hu.ppke.itk.plang.gui.widgets.FlatButton;
import hu.ppke.itk.plang.gui.widgets.PanelHeader;

/**
 * A beállítások párbeszédablaka, a VS Code beállításlapjának stílusában.
 *
 * Az eredeti funkciók (betűtípus, betűméret, lépésszám) megmaradtak, és
 * kiegészültek a téma, valamint a behúzás-segédvonalak kapcsolójával.
 */
public class PrefDialog extends JDialog {

   private static final long serialVersionUID = 1L;

   private JComboBox fontCombo;
   private JSpinner fontSize;
   private JSpinner stepNum;
   private JComboBox themeCombo;
   private JCheckBox indentGuides;
   private JCheckBox autoStop;

   private boolean result;
   private int font;
   private int size;
   private int step;
   private int theme;
   private boolean guides;
   private boolean stopAtEnd;

   PrefDialog(JFrame owner) {
      super(owner, "Beállítások", true);

      JPanel root = new JPanel(new BorderLayout());
      root.setBackground(Theme.p().widgetBg);

      PanelHeader header = new PanelHeader("Beállítások");
      header.setUppercase(false);
      root.add(header, BorderLayout.NORTH);

      JPanel panel = new JPanel(new GridBagLayout());
      panel.setBackground(Theme.p().widgetBg);
      panel.setBorder(BorderFactory.createEmptyBorder(14, 18, 10, 18));

      GridBagConstraints gc = new GridBagConstraints();
      gc.insets = new Insets(6, 0, 6, 10);
      gc.anchor = GridBagConstraints.WEST;
      gc.fill = GridBagConstraints.HORIZONTAL;

      int row = 0;

      // --- betűtípus ---
      gc.gridx = 0;
      gc.gridy = row;
      gc.weightx = 0;
      panel.add(label("Betűtípus"), gc);

      List<String> families = Theme.monoFamilies();
      this.fontCombo = new JComboBox(families.toArray());
      this.fontCombo.setSelectedItem(Theme.monoFamily());
      this.fontCombo.setFont(Theme.uiPlain());
      this.fontCombo.setRenderer(new DefaultListCellRenderer() {
         private static final long serialVersionUID = 1L;

         public Component getListCellRendererComponent(JList list, Object value, int index,
                                                       boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected,
                                                             cellHasFocus);
            c.setFont(new Font(String.valueOf(value), Font.PLAIN, 13));
            return c;
         }
      });
      gc.gridx = 1;
      gc.weightx = 1;
      panel.add(this.fontCombo, gc);
      row++;

      // --- betűméret ---
      gc.gridx = 0;
      gc.gridy = row;
      gc.weightx = 0;
      panel.add(label("Betűméret"), gc);
      this.fontSize = new JSpinner(new SpinnerNumberModel(13, 8, 42, 1));
      this.fontSize.setFont(Theme.uiPlain());
      gc.gridx = 1;
      gc.weightx = 1;
      panel.add(this.fontSize, gc);
      row++;

      // --- lépésszám (az eredeti beállítás) ---
      gc.gridx = 0;
      gc.gridy = row;
      gc.weightx = 0;
      panel.add(label("Lépésszám"), gc);
      this.stepNum = new JSpinner(new SpinnerNumberModel(10000, 10, 100000, 1000));
      this.stepNum.setFont(Theme.uiPlain());
      gc.gridx = 1;
      gc.weightx = 1;
      panel.add(this.stepNum, gc);
      row++;

      // --- téma ---
      gc.gridx = 0;
      gc.gridy = row;
      gc.weightx = 0;
      panel.add(label("Színséma"), gc);
      this.themeCombo = new JComboBox(new Object[] { "Sötét (Dark+)", "Világos (Light+)" });
      this.themeCombo.setFont(Theme.uiPlain());
      this.themeCombo.setSelectedIndex(Theme.mode());
      gc.gridx = 1;
      gc.weightx = 1;
      panel.add(this.themeCombo, gc);
      row++;

      // --- behúzás-segédvonalak ---
      this.indentGuides = new JCheckBox("Behúzás-segédvonalak megjelenítése", true);
      this.indentGuides.setFont(Theme.uiPlain());
      this.indentGuides.setOpaque(false);
      this.indentGuides.setForeground(Theme.p().sideBarFg);
      gc.gridx = 1;
      gc.gridy = row;
      gc.weightx = 1;
      panel.add(this.indentGuides, gc);
      row++;

      // --- automatikus leállítás ---
      this.autoStop = new JCheckBox("Futás után lépjen vissza a szerkesztőbe", false);
      this.autoStop.setFont(Theme.uiPlain());
      this.autoStop.setOpaque(false);
      this.autoStop.setForeground(Theme.p().sideBarFg);
      this.autoStop.setToolTipText("A futtatás befejeztével a környezet visszaáll szerkesztő "
            + "módba. Kikapcsolva az értelmezett program és az állapottábla marad elöl, "
            + "hogy az eredményt vissza lehessen nézni.");
      gc.gridx = 1;
      gc.gridy = row;
      gc.weightx = 1;
      panel.add(this.autoStop, gc);
      row++;

      root.add(panel, BorderLayout.CENTER);

      // --- gombok ---
      JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
      btnPanel.setBackground(Theme.p().widgetBg);

      FlatButton ok = new FlatButton(FlatButton.PRIMARY, "OK");
      ok.setPadding(18, 6);
      ok.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            synchronized (PrefDialog.this) {
               PrefDialog.this.setVisible(false);
               PrefDialog.this.result = true;
               PrefDialog.this.notifyAll();
            }
         }
      });

      FlatButton cancel = new FlatButton(FlatButton.SECONDARY, "Mégsem");
      cancel.setPadding(14, 6);
      cancel.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            synchronized (PrefDialog.this) {
               PrefDialog.this.setVisible(false);
               PrefDialog.this.notifyAll();
            }
         }
      });

      btnPanel.add(cancel);
      btnPanel.add(ok);
      root.add(btnPanel, BorderLayout.SOUTH);

      setContentPane(root);
      pack();
      setMinimumSize(new Dimension(430, getPreferredSize().height));
      setLocationRelativeTo(owner);
   }

   private JLabel label(String s) {
      JLabel l = new JLabel(s);
      l.setFont(Theme.uiPlain());
      l.setForeground(Theme.p().sideBarFg);
      return l;
   }

   Font getTextFont() {
      return new Font(String.valueOf(this.fontCombo.getSelectedItem()), Font.PLAIN,
                      ((Integer) this.fontSize.getValue()).intValue());
   }

   String getFontFamily() {
      Object sel = this.fontCombo.getSelectedItem();
      return sel == null ? Theme.monoFamily() : String.valueOf(sel);
   }

   int getFontSizeValue() {
      return ((Integer) this.fontSize.getValue()).intValue();
   }

   int getStepNum() {
      return ((Integer) this.stepNum.getValue()).intValue();
   }

   /** A választott téma ({@link Theme#DARK} vagy {@link Theme#LIGHT}). */
   int getThemeMode() {
      return this.themeCombo.getSelectedIndex();
   }

   void setThemeMode(int mode) {
      this.themeCombo.setSelectedIndex(mode);
      this.theme = mode;
   }

   boolean isShowIndentGuides() {
      return this.indentGuides.isSelected();
   }

   /** Igaz, ha a futás végén a környezet magától leáll. */
   boolean isAutoStop() {
      return this.autoStop.isSelected();
   }

   void setAutoStop(boolean b) {
      this.autoStop.setSelected(b);
   }

   void setFontFamily(String family) {
      if (family != null) {
         this.fontCombo.setSelectedItem(family);
         // ha nincs a listában, hozzáadjuk
         boolean found = false;
         for (int i = 0; i < this.fontCombo.getItemCount(); i++) {
            if (family.equals(this.fontCombo.getItemAt(i))) {
               found = true;
               break;
            }
         }
         if (!found) {
            this.fontCombo.addItem(family);
            this.fontCombo.setSelectedItem(family);
         }
      }
   }

   void setFontSizeValue(int size) {
      this.fontSize.setValue(Integer.valueOf(size));
   }

   void setStepNumValue(int n) {
      this.stepNum.setValue(Integer.valueOf(n));
   }

   void setIndentGuides(boolean b) {
      this.indentGuides.setSelected(b);
   }

   void setValues(Font font, int stepNum, int themeMode, boolean guides) {
      setValues(font, stepNum, themeMode, guides, isAutoStop());
   }

   void setValues(Font font, int stepNum, int themeMode, boolean guides, boolean autoStop) {
      if (font != null) {
         setFontFamily(font.getFamily());
         setFontSizeValue(font.getSize());
      }
      setStepNumValue(stepNum);
      setThemeMode(themeMode);
      setIndentGuides(guides);
      setAutoStop(autoStop);
   }

   private void save() {
      this.font = this.fontCombo.getSelectedIndex();
      this.size = ((Integer) this.fontSize.getValue()).intValue();
      this.step = ((Integer) this.stepNum.getValue()).intValue();
      this.theme = this.themeCombo.getSelectedIndex();
      this.guides = this.indentGuides.isSelected();
      this.stopAtEnd = this.autoStop.isSelected();
   }

   private void restore() {
      this.fontCombo.setSelectedIndex(this.font);
      this.fontSize.setValue(Integer.valueOf(this.size));
      this.stepNum.setValue(Integer.valueOf(this.step));
      this.themeCombo.setSelectedIndex(this.theme);
      this.indentGuides.setSelected(this.guides);
      this.autoStop.setSelected(this.stopAtEnd);
   }

   /** Az eredeti, blokkoló megjelenítés – a hívó logika változatlan maradhat. */
   synchronized boolean showDlg() {
      this.result = false;
      this.save();
      refreshTheme();
      this.setVisible(true);

      while (this.isVisible()) {
         try {
            this.wait();
         } catch (InterruptedException var2) {
         }
      }

      if (!this.result) {
         this.restore();
      }

      return this.result;
   }

   /** A párbeszédablak színeinek frissítése a témaváltás után. */
   private void refreshTheme() {
      getContentPane().setBackground(Theme.p().widgetBg);
      recolor(getContentPane());
      javax.swing.SwingUtilities.updateComponentTreeUI(this);
      repaint();
   }

   private void recolor(Component c) {
      if (c instanceof PanelHeader) {
         ((PanelHeader) c).applyTheme();
      } else if (c instanceof JPanel) {
         c.setBackground(Theme.p().widgetBg);
      } else if (c instanceof JLabel) {
         c.setForeground(Theme.p().sideBarFg);
      } else if (c instanceof JCheckBox) {
         c.setForeground(Theme.p().sideBarFg);
      }
      if (c instanceof java.awt.Container) {
         Component[] kids = ((java.awt.Container) c).getComponents();
         for (int i = 0; i < kids.length; i++) {
            recolor(kids[i]);
         }
      }
   }
}
