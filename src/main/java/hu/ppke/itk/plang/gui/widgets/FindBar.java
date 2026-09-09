package hu.ppke.itk.plang.gui.widgets;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import hu.ppke.itk.plang.gui.editor.CodeEditor;
import hu.ppke.itk.plang.gui.theme.Theme;

/**
 * A VS Code szerkesztőjének jobb felső sarkában megjelenő kereső-/cseresáv
 * egyszerűsített, de teljes értékű változata.
 */
public class FindBar extends JPanel {

   private static final long serialVersionUID = 1L;

   private final CodeEditor editor;
   private final JTextField findField = new JTextField(18);
   private final JTextField replaceField = new JTextField(18);
   private final JLabel countLabel = new JLabel("Nincs találat");
   private final JCheckBox caseBox = new JCheckBox("Aa");
   private boolean replaceVisible;
   private final JPanel replaceRow;

   public FindBar(CodeEditor editor) {
      super(new BorderLayout());
      this.editor = editor;
      setVisible(false);
      setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

      JPanel findRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
      findRow.setOpaque(false);

      FlatButton toggle = new FlatButton(FlatButton.TOOL,
         VSIcons.icon(VSIcons.CHEVRON_RIGHT, 14, Theme.p().sideBarFg), "Csere megjelenítése");
      toggle.setPadding(3, 3);
      toggle.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            replaceVisible = !replaceVisible;
            replaceRow.setVisible(replaceVisible);
            revalidate();
            repaint();
         }
      });

      findField.setColumns(20);
      styleField(findField);
      replaceField.setColumns(20);
      styleField(replaceField);

      caseBox.setToolTipText("Kis- és nagybetű megkülönböztetése");
      caseBox.setOpaque(false);
      caseBox.setFocusable(false);
      caseBox.setFont(Theme.uiPlain());
      caseBox.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            doSearch();
         }
      });

      countLabel.setFont(Theme.uiPlain());

      FlatButton prev = new FlatButton(FlatButton.TOOL,
         VSIcons.icon(VSIcons.ARROW_UP, 14, Theme.p().sideBarFg), "Előző találat  (Shift+Enter)");
      prev.setPadding(4, 4);
      prev.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            editor.prevMatch();
            updateCount();
         }
      });

      FlatButton next = new FlatButton(FlatButton.TOOL,
         VSIcons.icon(VSIcons.ARROW_DOWN, 14, Theme.p().sideBarFg), "Következő találat  (Enter)");
      next.setPadding(4, 4);
      next.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            editor.nextMatch();
            updateCount();
         }
      });

      FlatButton close = new FlatButton(FlatButton.TOOL,
         VSIcons.icon(VSIcons.CLOSE, 14, Theme.p().sideBarFg), "Bezárás  (Esc)");
      close.setPadding(4, 4);
      close.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            hideBar();
         }
      });

      findRow.add(toggle);
      findRow.add(findField);
      findRow.add(caseBox);
      findRow.add(countLabel);
      findRow.add(prev);
      findRow.add(next);
      findRow.add(close);

      replaceRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
      replaceRow.setOpaque(false);
      replaceRow.setVisible(false);
      replaceRow.add(javax.swing.Box.createHorizontalStrut(24));
      replaceRow.add(replaceField);

      FlatButton rep = new FlatButton(FlatButton.SECONDARY, "Csere");
      rep.setPadding(8, 4);
      rep.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            if (editor.replaceCurrent(replaceField.getText())) {
               doSearch();
            }
         }
      });
      FlatButton repAll = new FlatButton(FlatButton.SECONDARY, "Mind");
      repAll.setPadding(8, 4);
      repAll.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            int n = editor.replaceAll(findField.getText(), replaceField.getText(),
                                      caseBox.isSelected());
            countLabel.setText(n + " csere");
         }
      });
      replaceRow.add(rep);
      replaceRow.add(repAll);

      JPanel rows = new JPanel(new BorderLayout());
      rows.setOpaque(false);
      rows.add(findRow, BorderLayout.NORTH);
      rows.add(replaceRow, BorderLayout.CENTER);
      add(rows, BorderLayout.WEST);

      findField.getDocument().addDocumentListener(new DocumentListener() {
         public void changedUpdate(DocumentEvent e) { doSearch(); }
         public void insertUpdate(DocumentEvent e) { doSearch(); }
         public void removeUpdate(DocumentEvent e) { doSearch(); }
      });

      KeyAdapter keys = new KeyAdapter() {
         public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
               hideBar();
            } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
               if (e.isShiftDown()) {
                  editor.prevMatch();
               } else {
                  editor.nextMatch();
               }
               updateCount();
            }
         }
      };
      findField.addKeyListener(keys);
      replaceField.addKeyListener(keys);

      applyTheme();
   }

   private void styleField(JTextField f) {
      f.setFont(Theme.uiPlain());
      f.setBackground(Theme.p().inputBg);
      f.setForeground(Theme.p().inputFg);
      f.setCaretColor(Theme.p().caret);
      f.setBorder(BorderFactory.createCompoundBorder(
         BorderFactory.createLineBorder(Theme.p().inputBorder),
         BorderFactory.createEmptyBorder(3, 6, 3, 6)));
   }

   public void applyTheme() {
      setBackground(Theme.p().widgetBg);
      styleField(findField);
      styleField(replaceField);
      countLabel.setForeground(Theme.p().gutterFg);
      caseBox.setForeground(Theme.p().sideBarFg);
      caseBox.setBackground(Theme.p().widgetBg);
      repaint();
   }

   /** Megjeleníti a sávot, és beírja a megadott kezdőszöveget. */
   public void showBar(String initial) {
      if (initial != null && initial.length() > 0 && initial.indexOf('\n') < 0) {
         findField.setText(initial);
      }
      setVisible(true);
      revalidate();
      repaint();
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            findField.requestFocusInWindow();
            findField.selectAll();
            doSearch();
         }
      });
   }

   /** Megjeleníti a sávot csere-módban. */
   public void showBarWithReplace(String initial) {
      replaceVisible = true;
      replaceRow.setVisible(true);
      showBar(initial);
   }

   public void setReplaceVisible(boolean b) {
      replaceVisible = b;
      replaceRow.setVisible(b);
      revalidate();
      repaint();
   }

   public boolean isReplaceVisible() {
      return replaceVisible;
   }

   public void hideBar() {
      setVisible(false);
      editor.clearSearch();
      revalidate();
      repaint();
      editor.requestFocusInWindow();
   }

   private void doSearch() {
      editor.search(findField.getText(), caseBox.isSelected());
      updateCount();
   }

   private void updateCount() {
      int n = editor.matchCount();
      if (n == 0) {
         countLabel.setText(findField.getText().length() == 0 ? "" : "Nincs találat");
      } else {
         countLabel.setText((editor.activeMatchIndex() + 1) + " / " + n);
      }
   }

   public Dimension getPreferredSize() {
      Dimension d = super.getPreferredSize();
      return new Dimension(d.width, d.height);
   }

   protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      Theme.Palette p = Theme.p();
      g2.setColor(p.widgetBg);
      g2.fillRect(0, 0, getWidth(), getHeight());
      g2.setColor(p.widgetBorder);
      g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
      g2.dispose();
   }
}
