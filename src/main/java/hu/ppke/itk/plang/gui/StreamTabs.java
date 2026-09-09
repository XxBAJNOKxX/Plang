package hu.ppke.itk.plang.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;

import hu.ppke.itk.plang.gui.theme.Theme;
import hu.ppke.itk.plang.gui.widgets.EditorTabBar;
import hu.ppke.itk.plang.gui.widgets.FlatScrollBarUI;
import hu.ppke.itk.plang.gui.widgets.VSIcons;
import hu.ppke.itk.plang.prog.StreamKind;

/**
 * A be- vagy kimeneti csatornákat megjelenítő, VS Code stílusú fülekkel
 * ellátott panel.
 *
 * Az eredeti {@code JTabbedPane} helyére lép: ugyanazokat a műveleteket
 * kínálja (fülek szinkronizálása a program csatornáival, dokumentumok
 * elérése index szerint), de a megjelenése a témát követi.
 */
public class StreamTabs extends JPanel {

   private static final long serialVersionUID = 1L;

   private final StreamKind kind;
   private final EditorTabBar tabs = new EditorTabBar();
   private final CardLayout cards = new CardLayout();
   private final JPanel pages = new JPanel(cards);
   private final List<String> titles = new ArrayList<String>();
   private final List<JTextPane> panes = new ArrayList<JTextPane>();
   private final List<JScrollPane> scrolls = new ArrayList<JScrollPane>();
   private final JPanel empty = new JPanel(new BorderLayout());
   private final JLabel emptyLabel = new JLabel("", SwingConstants.CENTER);
   private Font textFont = Theme.mono(Font.PLAIN, 13);

   public StreamTabs(StreamKind kind) {
      super(new BorderLayout());
      this.kind = kind;

      tabs.setStyle(EditorTabBar.STYLE_PANEL);
      tabs.setListener(new EditorTabBar.Listener() {
         public void tabSelected(String id) {
            cards.show(pages, id);
         }

         public void tabClosed(String id) {
         }
      });

      emptyLabel.setFont(Theme.uiPlain());
      emptyLabel.setText(kind == StreamKind.INPUT
         ? "Nincs bemeneti csatorna – értelmezd a programot"
         : "Nincs kimeneti csatorna – értelmezd a programot");
      empty.add(emptyLabel, BorderLayout.CENTER);

      pages.add(empty, "__empty__");

      add(tabs, BorderLayout.NORTH);
      add(pages, BorderLayout.CENTER);
      applyTheme();
   }

   public void applyTheme() {
      Theme.Palette p = Theme.p();
      setBackground(p.editorBg);
      pages.setBackground(p.editorBg);
      empty.setBackground(p.editorBg);
      emptyLabel.setForeground(p.gutterFg);
      tabs.applyTheme();
      for (int i = 0; i < panes.size(); i++) {
         JTextPane t = panes.get(i);
         t.setBackground(p.editorBg);
         t.setForeground(p.editorFg);
         t.setCaretColor(p.caret);
         t.setSelectionColor(p.selection);
         StreamDocument d = (StreamDocument) t.getDocument();
         d.reapplyTheme();
         JScrollPane sc = scrolls.get(i);
         FlatScrollBarUI.install(sc);
         sc.getViewport().setBackground(p.editorBg);
      }
      repaint();
   }

   public void setTextFont(Font f) {
      this.textFont = f;
      for (int i = 0; i < panes.size(); i++) {
         panes.get(i).setFont(f);
      }
      repaint();
   }

   /** A fülek száma (az eredeti getComponentCount() megfelelője). */
   public int count() {
      return titles.size();
   }

   public String getTitleAt(int i) {
      return titles.get(i);
   }

   public StreamDocument getDocument(int i) {
      return (StreamDocument) panes.get(i).getDocument();
   }

   public void setEditable(boolean b) {
      for (int i = 0; i < panes.size(); i++) {
         panes.get(i).setEditable(b);
      }
   }

   /** A megjelenítési attribútumokat alaphelyzetbe állítja (Stop után). */
   public void resetAttributes() {
      for (int i = 0; i < panes.size(); i++) {
         panes.get(i).setCharacterAttributes(StreamDocument.getNormalAttr(), true);
      }
   }

   /**
    * A füleket a program csatornáihoz igazítja: a meglévő, továbbra is
    * szükséges csatornák tartalma megmarad, a fölöslegesek eltűnnek.
    */
   public void sync(Set<String> streams, boolean editable, Font font) {
      this.textFont = font;

      // a már nem szükséges fülek eltávolítása
      for (int i = titles.size() - 1; i >= 0; i--) {
         if (!streams.contains(titles.get(i))) {
            tabs.removeTab(titles.get(i));
            pages.remove(scrolls.get(i));
            titles.remove(i);
            panes.remove(i);
            scrolls.remove(i);
         }
      }

      // az új csatornák felvétele (rendezett sorrendben)
      Iterator<String> it = streams.iterator();
      while (it.hasNext()) {
         String name = it.next();
         if (titles.contains(name)) {
            continue;
         }
         JTextPane text = new JTextPane(new StreamDocument());
         text.setFont(this.textFont);
         text.setCharacterAttributes(StreamDocument.getNormalAttr(), true);
         text.setEditable(editable);
         text.setBackground(Theme.p().editorBg);
         text.setForeground(Theme.p().editorFg);
         text.setCaretColor(Theme.p().caret);
         text.setSelectionColor(Theme.p().selection);
         text.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

         JScrollPane sc = new JScrollPane(text);
         FlatScrollBarUI.install(sc);
         sc.getViewport().setBackground(Theme.p().editorBg);

         // beszúrás a névsorrend szerinti helyre
         int pos = 0;
         while (pos < titles.size() && titles.get(pos).compareTo(name) < 0) {
            pos++;
         }
         titles.add(pos, name);
         panes.add(pos, text);
         scrolls.add(pos, sc);
         pages.add(sc, name);
      }

      // a fülsor újraépítése a végleges sorrenddel
      tabs.clearTabs();
      for (int i = 0; i < titles.size(); i++) {
         tabs.addTab(new EditorTabBar.Tab(titles.get(i), titles.get(i),
            kind == StreamKind.INPUT ? VSIcons.INPUT : VSIcons.OUTPUT, false));
      }

      if (titles.isEmpty()) {
         cards.show(pages, "__empty__");
      } else {
         tabs.select(titles.get(0));
         cards.show(pages, titles.get(0));
      }

      setTextFont(this.textFont);
      revalidate();
      repaint();
   }

   protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setColor(Theme.p().editorBg);
      g2.fillRect(0, 0, getWidth(), getHeight());
      g2.dispose();
   }
}
