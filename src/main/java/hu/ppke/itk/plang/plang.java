package hu.ppke.itk.plang;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import hu.ppke.itk.plang.gui.MainFrame;
import hu.ppke.itk.plang.gui.theme.Theme;

/**
 * A PLanG fejlesztőkörnyezet belépési pontja.
 */
public class plang implements Runnable {

   public void run() {
      new MainFrame().setVisible(true);
   }

   public static void main(String[] args) {
      // A rendszer saját kinézete helyett a platformfüggetlen alapot használjuk,
      // hogy a témát mindenhol egységesen tudjuk felülírni.
      try {
         UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
      } catch (Exception e) {
         System.err.println("Nem sikerült beállítani a megjelenést: " + e.getMessage());
      }

      // Élsimított szövegmegjelenítés
      System.setProperty("awt.useSystemAAFontSettings", "on");
      System.setProperty("swing.aatext", "true");

      Theme.setMode(Theme.DARK);

      SwingUtilities.invokeLater(new plang());
   }
}
