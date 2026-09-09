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
               // beállítások mentése kilépéskor
               try {
                  workbench.savePrefs();
                  AppPrefs.setWindowSize(getWidth(), getHeight());
                  AppPrefs.setWindowPosition(getX(), getY());
                  AppPrefs.flush();
               } catch (Exception ex) {
                  // nem kritikus
               }
               MainFrame.this.dispose();
            }
         }
      });

      getContentPane().setBackground(Theme.p().editorBg);
      setMinimumSize(new Dimension(940, 600));

      // ablakméret és pozíció betöltése prefs-ből
      int ww = 1440;
      int wh = 900;
      int wx = -1;
      int wy = -1;
      try {
         ww = AppPrefs.getWindowWidth();
         wh = AppPrefs.getWindowHeight();
         wx = AppPrefs.getWindowX();
         wy = AppPrefs.getWindowY();
      } catch (Exception ex) {
         // alapértelmezés marad
      }
      setPreferredSize(new Dimension(ww, wh));
      pack();
      if (wx >= 0 && wy >= 0) {
         try {
            setLocation(wx, wy);
         } catch (Exception ex) {
            setLocationRelativeTo(null);
         }
      } else {
         setLocationRelativeTo(null);
      }
   }

   /** A főablak munkafelülete. */
   public Workbench getWorkbench() {
      return this.workbench;
   }
}
