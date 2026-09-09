package hu.ppke.itk.plang.gui.theme;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

/**
 * A PLanG IDE színsémája és tipográfiája.
 *
 * A paletta a Visual Studio Code "Dark+" és "Light+" témáit követi, hogy az
 * IDE megjelenése megegyezzen a megszokott VSC felülettel.
 */
public final class Theme {

   public static final int DARK = 0;
   public static final int LIGHT = 1;

   /** Egy teljes színpaletta. */
   public static final class Palette {
      public final int mode;

      /* --- keret / vázszerkezet --- */
      public final Color titleBar;
      public final Color titleBarFg;
      public final Color activityBar;
      public final Color activityBarFg;
      public final Color activityBarActiveFg;
      public final Color activityBarActiveBorder;
      public final Color sideBar;
      public final Color sideBarFg;
      public final Color sideBarTitleFg;
      public final Color sectionHeader;
      public final Color border;
      public final Color focusBorder;
      /** Az osztópanelek elválasztó vonala – a border-nél kicsit erősebb. */
      public final Color divider;

      /* --- szerkesztő --- */
      public final Color editorBg;
      public final Color editorFg;
      public final Color gutterFg;
      public final Color gutterActiveFg;
      public final Color currentLine;
      public final Color currentLineBorder;
      public final Color selection;
      public final Color selectionInactive;
      public final Color findMatch;
      public final Color findMatchActive;
      public final Color caret;
      public final Color indentGuide;
      public final Color ruler;

      /* --- fülek --- */
      public final Color tabActiveBg;
      public final Color tabInactiveBg;
      public final Color tabActiveFg;
      public final Color tabInactiveFg;
      public final Color tabBorder;
      public final Color tabActiveTopBorder;
      public final Color tabBarBg;

      /* --- panelek, listák --- */
      public final Color panelBg;
      public final Color listHover;
      public final Color listSelection;
      public final Color listSelectionFg;
      public final Color listInactiveSelection;
      public final Color tableHeaderBg;
      public final Color tableGrid;

      /* --- állapotsor --- */
      public final Color statusBg;
      public final Color statusFg;
      public final Color statusRunBg;
      public final Color statusHover;

      /* --- vezérlők --- */
      public final Color buttonBg;
      public final Color buttonFg;
      public final Color buttonHover;
      public final Color buttonSecondaryBg;
      public final Color buttonSecondaryFg;
      public final Color inputBg;
      public final Color inputFg;
      public final Color inputBorder;
      public final Color widgetBg;
      public final Color widgetBorder;
      public final Color scrollThumb;
      public final Color scrollThumbHover;
      public final Color badgeBg;
      public final Color badgeFg;

      /* --- jelzések --- */
      public final Color error;
      public final Color warning;
      public final Color info;
      public final Color success;
      public final Color errorLineBg;

      /* --- szintaxis --- */
      public final Color synKeyword;
      public final Color synControl;
      public final Color synType;
      public final Color synString;
      public final Color synNumber;
      public final Color synComment;
      public final Color synFunction;
      public final Color synVariable;
      public final Color synOperator;
      public final Color synConstant;

      private Palette(int mode, Color[] c) {
         this.mode = mode;
         int i = 0;
         titleBar = c[i++];
         titleBarFg = c[i++];
         activityBar = c[i++];
         activityBarFg = c[i++];
         activityBarActiveFg = c[i++];
         activityBarActiveBorder = c[i++];
         sideBar = c[i++];
         sideBarFg = c[i++];
         sideBarTitleFg = c[i++];
         sectionHeader = c[i++];
         border = c[i++];
         focusBorder = c[i++];
         divider = c[i++];
         editorBg = c[i++];
         editorFg = c[i++];
         gutterFg = c[i++];
         gutterActiveFg = c[i++];
         currentLine = c[i++];
         currentLineBorder = c[i++];
         selection = c[i++];
         selectionInactive = c[i++];
         findMatch = c[i++];
         findMatchActive = c[i++];
         caret = c[i++];
         indentGuide = c[i++];
         ruler = c[i++];
         tabActiveBg = c[i++];
         tabInactiveBg = c[i++];
         tabActiveFg = c[i++];
         tabInactiveFg = c[i++];
         tabBorder = c[i++];
         tabActiveTopBorder = c[i++];
         tabBarBg = c[i++];
         panelBg = c[i++];
         listHover = c[i++];
         listSelection = c[i++];
         listSelectionFg = c[i++];
         listInactiveSelection = c[i++];
         tableHeaderBg = c[i++];
         tableGrid = c[i++];
         statusBg = c[i++];
         statusFg = c[i++];
         statusRunBg = c[i++];
         statusHover = c[i++];
         buttonBg = c[i++];
         buttonFg = c[i++];
         buttonHover = c[i++];
         buttonSecondaryBg = c[i++];
         buttonSecondaryFg = c[i++];
         inputBg = c[i++];
         inputFg = c[i++];
         inputBorder = c[i++];
         widgetBg = c[i++];
         widgetBorder = c[i++];
         scrollThumb = c[i++];
         scrollThumbHover = c[i++];
         badgeBg = c[i++];
         badgeFg = c[i++];
         error = c[i++];
         warning = c[i++];
         info = c[i++];
         success = c[i++];
         errorLineBg = c[i++];
         synKeyword = c[i++];
         synControl = c[i++];
         synType = c[i++];
         synString = c[i++];
         synNumber = c[i++];
         synComment = c[i++];
         synFunction = c[i++];
         synVariable = c[i++];
         synOperator = c[i++];
         synConstant = c[i++];
      }

      static Palette dark() {
         return new Palette(DARK, new Color[] {
            h("3C3C3C"), h("CCCCCC"),                       // titleBar, fg
            h("333333"), h("858585"), h("FFFFFF"), h("FFFFFF"), // activityBar
            h("252526"), h("CCCCCC"), h("BBBBBB"), h("CCCCCC"), // sideBar
            h("2B2B2B"), h("007FD4"), h("444444"),          // border, focus, divider
            h("1E1E1E"), h("D4D4D4"),                       // editor
            h("858585"), h("C6C6C6"),                       // gutter
            h("282828"), h("282828"),                       // current line
            h("264F78"), h("3A3D41"),                       // selection
            h("613214"), h("9E6A03"),                       // find
            h("AEAFAD"), h("404040"), h("5A5A5A"),          // caret, guides
            h("1E1E1E"), h("2D2D2D"), h("FFFFFF"), h("969696"), // tabs
            h("252526"), h("007ACC"), h("252526"),
            h("1E1E1E"),                                    // panel
            h("2A2D2E"), h("094771"), h("FFFFFF"), h("37373D"), // lists
            h("252526"), h("3C3C3C"),                       // table
            h("007ACC"), h("FFFFFF"), h("CC6633"), h("1F8AD2"), // status
            h("0E639C"), h("FFFFFF"), h("1177BB"),          // button
            h("3A3D41"), h("CCCCCC"),                       // secondary button
            h("3C3C3C"), h("CCCCCC"), h("3C3C3C"),          // input
            h("252526"), h("454545"),                       // widget
            h("4F4F4F"), h("5F5F5F"),                       // scrollbar
            h("4D4D4D"), h("FFFFFF"),                       // badge
            h("F14C4C"), h("CCA700"), h("3794FF"), h("89D185"), h("3A1D1D"),
            h("569CD6"), h("C586C0"), h("4EC9B0"), h("CE9178"), h("B5CEA8"),
            h("6A9955"), h("DCDCAA"), h("9CDCFE"), h("D4D4D4"), h("569CD6")
         });
      }

      static Palette light() {
         return new Palette(LIGHT, new Color[] {
            h("DDDDDD"), h("333333"),
            h("F8F8F8"), h("616161"), h("1F1F1F"), h("1F1F1F"),
            h("F3F3F3"), h("3B3B3B"), h("6F6F6F"), h("3B3B3B"),
            h("E5E5E5"), h("0090F1"), h("CCCCCC"),          // border, focus, divider
            h("FFFFFF"), h("3B3B3B"),
            h("6E7681"), h("171184"),
            h("F5F5F5"), h("EEEEEE"),
            h("ADD6FF"), h("E5EBF1"),
            h("A8AC94"), h("FFB000"),
            h("005FB8"), h("D3D3D3"), h("D3D3D3"),
            h("FFFFFF"), h("ECECEC"), h("3B3B3B"), h("6F6F6F"),
            h("E5E5E5"), h("005FB8"), h("F8F8F8"),
            h("FFFFFF"),
            h("E8E8E8"), h("0060C0"), h("FFFFFF"), h("E4E6F1"),
            h("F8F8F8"), h("E5E5E5"),
            h("005FB8"), h("FFFFFF"), h("C4571E"), h("1A7FD4"),
            h("005FB8"), h("FFFFFF"), h("0258A8"),
            h("E5E5E5"), h("3B3B3B"),
            h("FFFFFF"), h("3B3B3B"), h("CECECE"),
            h("F8F8F8"), h("C8C8C8"),
            h("C1C1C1"), h("A8A8A8"),
            h("C4C4C4"), h("3B3B3B"),
            h("E51400"), h("BF8803"), h("1A85FF"), h("2EA043"), h("FFEBEB"),
            h("0000FF"), h("AF00DB"), h("267F99"), h("A31515"), h("098658"),
            h("008000"), h("795E26"), h("001080"), h("000000"), h("0000FF")
         });
      }
   }

   private static Palette current = Palette.dark();

   /* ------------------------------------------------------------------ */

   /** A pillanatnyi paletta. */
   public static Palette p() {
      return current;
   }

   public static int mode() {
      return current.mode;
   }

   public static boolean isDark() {
      return current.mode == DARK;
   }

   public static void setMode(int mode) {
      current = (mode == LIGHT) ? Palette.light() : Palette.dark();
      installUIDefaults();
   }

   public static void toggleMode() {
      setMode(isDark() ? LIGHT : DARK);
   }

   /** A hibaszínt HTML-ben használható alakban adja vissza. */
   public static String errorHex() {
      return hex(current.error);
   }

   public static String hex(Color c) {
      return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
   }

   private static Color h(String rgb) {
      return new Color(Integer.parseInt(rgb, 16));
   }

   /** Áttetszőbb változat ugyanabból a színből. */
   public static Color alpha(Color c, int a) {
      return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
   }

   /* ----------------------------- betűk ------------------------------ */

   private static final String[] MONO = {
      "Cascadia Mono", "Cascadia Code", "Consolas", "JetBrains Mono", "Fira Code",
      "SF Mono", "Menlo", "Monaco", "Ubuntu Mono", "DejaVu Sans Mono",
      "Liberation Mono", "Courier New", "Monospaced"
   };

   private static final String[] UI = {
      "Segoe UI", "Inter", "SF Pro Text", "Helvetica Neue", "Ubuntu",
      "Cantarell", "DejaVu Sans", "Dialog", "SansSerif"
   };

   private static Set<String> available;

   private static Set<String> available() {
      if (available == null) {
         available = new HashSet<String>();
         try {
            available.addAll(Arrays.asList(
               GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
         } catch (Throwable t) {
            // fejnélküli környezet – marad a logikai családnév
         }
      }
      return available;
   }

   private static String pick(String[] candidates, String fallback) {
      Set<String> av = available();
      for (int i = 0; i < candidates.length; i++) {
         if (av.contains(candidates[i])) {
            return candidates[i];
         }
      }
      return fallback;
   }

   /** Az elérhető, monospace családnevek listája (a beállítás párbeszédhez). */
   public static List<String> monoFamilies() {
      List<String> l = new LinkedList<String>();
      Set<String> av = available();
      for (int i = 0; i < MONO.length; i++) {
         if (av.contains(MONO[i]) && !l.contains(MONO[i])) {
            l.add(MONO[i]);
         }
      }
      if (!l.contains("Monospaced")) {
         l.add("Monospaced");
      }
      return l;
   }

   public static String monoFamily() {
      return pick(MONO, "Monospaced");
   }

   public static String uiFamily() {
      return pick(UI, "SansSerif");
   }

   public static Font mono(int style, int size) {
      return new Font(monoFamily(), style, size);
   }

   public static Font ui(int style, int size) {
      return new Font(uiFamily(), style, size);
   }

   public static Font uiPlain() {
      return ui(Font.PLAIN, 12);
   }

   public static Font uiBold() {
      return ui(Font.BOLD, 12);
   }

   /** Kis, nagybetűs feliratokhoz (VSC szekciócímek). */
   public static Font uiSmallBold() {
      return ui(Font.BOLD, 11);
   }

   /* -------------------------- UIManager ----------------------------- */

   /**
    * Beállítja a Swing alapértelmezéseket, hogy a párbeszédablakok és a
    * beépített vezérlők is a témát kövessék.
    */
   public static void installUIDefaults() {
      Palette c = current;
      Font f = uiPlain();
      FontUIResource fr = new FontUIResource(f);

      String[] fontKeys = {
         "Button.font", "ToggleButton.font", "RadioButton.font", "CheckBox.font",
         "ColorChooser.font", "ComboBox.font", "Label.font", "List.font",
         "MenuBar.font", "MenuItem.font", "RadioButtonMenuItem.font",
         "CheckBoxMenuItem.font", "Menu.font", "PopupMenu.font", "OptionPane.font",
         "Panel.font", "ProgressBar.font", "ScrollPane.font", "Viewport.font",
         "TabbedPane.font", "Table.font", "TableHeader.font", "TextField.font",
         "PasswordField.font", "TextArea.font", "TextPane.font", "EditorPane.font",
         "TitledBorder.font", "ToolBar.font", "ToolTip.font", "Tree.font",
         "Spinner.font", "FileChooser.listFont"
      };
      for (int i = 0; i < fontKeys.length; i++) {
         UIManager.put(fontKeys[i], fr);
      }

      put("Panel.background", c.sideBar);
      put("Panel.foreground", c.sideBarFg);
      put("OptionPane.background", c.widgetBg);
      put("OptionPane.foreground", c.sideBarFg);
      put("OptionPane.messageForeground", c.sideBarFg);
      put("Label.background", c.sideBar);
      put("Label.foreground", c.sideBarFg);
      put("Label.disabledForeground", c.gutterFg);

      put("Button.background", c.buttonSecondaryBg);
      put("Button.foreground", c.buttonSecondaryFg);
      put("Button.select", c.buttonHover);
      put("Button.focus", c.focusBorder);
      put("Button.disabledText", c.gutterFg);

      put("ToggleButton.background", c.buttonSecondaryBg);
      put("ToggleButton.foreground", c.buttonSecondaryFg);

      put("TextField.background", c.inputBg);
      put("TextField.foreground", c.inputFg);
      put("TextField.caretForeground", c.caret);
      put("TextField.selectionBackground", c.selection);
      put("TextField.selectionForeground", c.editorFg);
      put("TextField.inactiveForeground", c.gutterFg);
      put("FormattedTextField.background", c.inputBg);
      put("FormattedTextField.foreground", c.inputFg);
      put("FormattedTextField.caretForeground", c.caret);
      put("PasswordField.background", c.inputBg);
      put("PasswordField.foreground", c.inputFg);

      put("TextArea.background", c.editorBg);
      put("TextArea.foreground", c.editorFg);
      put("TextArea.caretForeground", c.caret);
      put("TextArea.selectionBackground", c.selection);
      put("TextArea.selectionForeground", c.editorFg);
      put("TextPane.background", c.editorBg);
      put("TextPane.foreground", c.editorFg);
      put("TextPane.caretForeground", c.caret);
      put("TextPane.selectionBackground", c.selection);
      put("TextPane.selectionForeground", c.editorFg);
      put("EditorPane.background", c.editorBg);
      put("EditorPane.foreground", c.editorFg);

      put("List.background", c.panelBg);
      put("List.foreground", c.editorFg);
      put("List.selectionBackground", c.listSelection);
      put("List.selectionForeground", c.listSelectionFg);

      put("Table.background", c.panelBg);
      put("Table.foreground", c.editorFg);
      put("Table.gridColor", c.tableGrid);
      put("Table.selectionBackground", c.listSelection);
      put("Table.selectionForeground", c.listSelectionFg);
      put("Table.focusCellHighlightBorder", javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
      put("TableHeader.background", c.tableHeaderBg);
      put("TableHeader.foreground", c.sideBarTitleFg);

      put("Tree.background", c.panelBg);
      put("Tree.foreground", c.editorFg);
      put("Tree.textBackground", c.panelBg);
      put("Tree.textForeground", c.editorFg);
      put("Tree.selectionBackground", c.listSelection);
      put("Tree.selectionForeground", c.listSelectionFg);
      put("Tree.selectionBorderColor", c.focusBorder);
      put("Tree.hash", c.border);
      put("Tree.line", c.border);

      put("ScrollPane.background", c.editorBg);
      put("ScrollPane.border", javax.swing.BorderFactory.createEmptyBorder());
      put("Viewport.background", c.editorBg);
      put("ScrollBar.background", c.editorBg);
      put("ScrollBar.track", c.editorBg);
      put("ScrollBar.thumb", c.scrollThumb);
      put("ScrollBar.width", Integer.valueOf(14));

      put("SplitPane.background", c.border);
      put("SplitPane.dividerSize", Integer.valueOf(4));
      put("SplitPaneDivider.border", javax.swing.BorderFactory.createEmptyBorder());

      put("TabbedPane.background", c.tabBarBg);
      put("TabbedPane.foreground", c.tabInactiveFg);
      put("TabbedPane.contentAreaColor", c.editorBg);
      put("TabbedPane.selected", c.tabActiveBg);
      put("TabbedPane.darkShadow", c.border);
      put("TabbedPane.shadow", c.border);
      put("TabbedPane.light", c.border);
      put("TabbedPane.highlight", c.border);
      put("TabbedPane.focus", c.focusBorder);
      put("TabbedPane.contentBorderInsets", new java.awt.Insets(0, 0, 0, 0));
      put("TabbedPane.tabsOverlapBorder", Boolean.TRUE);

      put("MenuBar.background", c.titleBar);
      put("MenuBar.foreground", c.titleBarFg);
      put("MenuBar.borderColor", c.border);
      put("Menu.background", c.titleBar);
      put("Menu.foreground", c.titleBarFg);
      put("Menu.selectionBackground", c.listSelection);
      put("Menu.selectionForeground", c.listSelectionFg);
      put("Menu.disabledForeground", c.gutterFg);
      put("MenuItem.background", c.widgetBg);
      put("MenuItem.foreground", c.sideBarFg);
      put("MenuItem.selectionBackground", c.listSelection);
      put("MenuItem.selectionForeground", c.listSelectionFg);
      put("MenuItem.disabledForeground", c.gutterFg);
      put("MenuItem.acceleratorForeground", c.gutterFg);
      put("MenuItem.acceleratorSelectionForeground", c.listSelectionFg);
      put("CheckBoxMenuItem.background", c.widgetBg);
      put("CheckBoxMenuItem.foreground", c.sideBarFg);
      put("CheckBoxMenuItem.selectionBackground", c.listSelection);
      put("CheckBoxMenuItem.selectionForeground", c.listSelectionFg);
      put("RadioButtonMenuItem.background", c.widgetBg);
      put("RadioButtonMenuItem.foreground", c.sideBarFg);
      put("PopupMenu.background", c.widgetBg);
      put("PopupMenu.foreground", c.sideBarFg);
      put("PopupMenu.border", javax.swing.BorderFactory.createLineBorder(c.widgetBorder));
      put("Separator.foreground", c.border);
      put("Separator.background", c.widgetBg);

      put("ComboBox.background", c.inputBg);
      put("ComboBox.foreground", c.inputFg);
      put("ComboBox.selectionBackground", c.listSelection);
      put("ComboBox.selectionForeground", c.listSelectionFg);
      put("ComboBox.buttonBackground", c.inputBg);
      put("ComboBox.buttonDarkShadow", c.inputBorder);
      put("ComboBox.buttonHighlight", c.inputBg);
      put("ComboBox.buttonShadow", c.inputBorder);
      put("ComboBox.disabledForeground", c.gutterFg);

      put("Spinner.background", c.inputBg);
      put("Spinner.foreground", c.inputFg);
      put("Spinner.border", javax.swing.BorderFactory.createLineBorder(c.inputBorder));

      put("CheckBox.background", c.sideBar);
      put("CheckBox.foreground", c.sideBarFg);
      put("RadioButton.background", c.sideBar);
      put("RadioButton.foreground", c.sideBarFg);

      put("ToolTip.background", c.widgetBg);
      put("ToolTip.foreground", c.sideBarFg);
      put("ToolTip.border", javax.swing.BorderFactory.createLineBorder(c.widgetBorder));

      put("ToolBar.background", c.titleBar);
      put("ToolBar.foreground", c.titleBarFg);
      put("ToolBar.borderColor", c.border);

      put("Slider.background", c.sideBar);
      put("ProgressBar.background", c.inputBg);
      put("ProgressBar.foreground", c.buttonBg);

      put("FileChooser.background", c.sideBar);
      put("FileChooser.foreground", c.sideBarFg);
      put("FileView.background", c.sideBar);

      put("controlText", c.sideBarFg);
      put("text", c.editorFg);
      put("window", c.panelBg);
      put("control", c.sideBar);
      put("info", c.widgetBg);
      put("infoText", c.sideBarFg);
      put("nimbusLightBackground", c.editorBg);
   }

   private static void put(String key, Color value) {
      UIManager.put(key, new ColorUIResource(value));
   }

   private static void put(String key, Object value) {
      UIManager.put(key, value);
   }

   private Theme() {
   }
}
