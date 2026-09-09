package hu.ppke.itk.plang;

import java.io.File;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import hu.ppke.itk.plang.gui.AppPrefs;
import hu.ppke.itk.plang.gui.MainFrame;
import hu.ppke.itk.plang.gui.theme.Theme;

/**
 * A PLanG fejlesztőkörnyezet belépési pontja.
 */
public class plang implements Runnable {

   private final File initialFile;

   public plang(File initialFile) {
      this.initialFile = initialFile;
   }

   public plang() {
      this(null);
   }

   public void run() {
      MainFrame frame = new MainFrame();
      if (initialFile != null && initialFile.exists() && initialFile.isFile()) {
         frame.getWorkbench().openFile(initialFile);
      }
      frame.setVisible(true);
   }

   public static void main(String[] args) {
      try {
         UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
      } catch (Exception e) {
         System.err.println("Nem sikerült beállítani a megjelenést: " + e.getMessage());
      }

      System.setProperty("awt.useSystemAAFontSettings", "on");
      System.setProperty("swing.aatext", "true");

      // téma betöltése prefs-ből, hibatűrően
      int mode = Theme.DARK;
      try {
         mode = AppPrefs.getThemeMode();
      } catch (Exception e) {
         mode = Theme.DARK;
      }
      Theme.setMode(mode);

      File init = null;
      if (args != null && args.length > 0) {
         try {
            File f = new File(args[0]);
            if (f.exists() && f.isFile()) {
               init = f;
            }
         } catch (Exception e) {
            // nem kritikus
         }
      }

      final File initial = init;
      SwingUtilities.invokeLater(new plang(initial));
   }
}
