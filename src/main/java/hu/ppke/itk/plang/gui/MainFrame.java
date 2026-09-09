package hu.ppke.itk.plang.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import hu.ppke.itk.plang.gui.theme.Theme;

/**
 * A PLanG fejlesztőkörnyezet főablaka.
 *
 * Az érdemi felület a {@link Workbench} panelen él, ez az osztály csupán
 * ablakot ad neki – így a munkafelület önállóan is beágyazható és
 * tesztelhető.
 */
public class MainFrame extends JFrame {

   private static final long serialVersionUID = 1L;

   private final Workbench workbench;

   public MainFrame() {
      super("PLanG");

      this.workbench = new Workbench(this);
      setLayout(new BorderLayout());
      add(this.workbench, BorderLayout.CENTER);
      setJMenuBar(this.workbench.getMenuBar());

      this.workbench.setExitHandler(new Runnable() {
         public void run() {
            processWindowEvent(new WindowEvent(MainFrame.this, WindowEvent.WINDOW_CLOSING));
         }
      });

      setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
      addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent e) {
            if (!MainFrame.this.workbench.hasUnsavedChanges()
                || JOptionPane.showConfirmDialog(MainFrame.this,
                      "A programszöveg változásai nincsenek elmentve. Biztosan ki akarsz lépni?",
                      "Kilépés", JOptionPane.YES_NO_OPTION,
                      JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
               MainFrame.this.dispose();
            }
         }
      });

      getContentPane().setBackground(Theme.p().editorBg);
      setMinimumSize(new Dimension(940, 600));
      setPreferredSize(new Dimension(1440, 900));
      pack();
      setLocationRelativeTo(null);
   }

   /** A főablak munkafelülete. */
   public Workbench getWorkbench() {
      return this.workbench;
   }
}
