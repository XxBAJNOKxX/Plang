package hu.ppke.itk.plang.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreePath;

import hu.ppke.itk.plang.gui.editor.CodeEditor;
import hu.ppke.itk.plang.gui.editor.LineNumberGutter;
import hu.ppke.itk.plang.gui.theme.Theme;
import hu.ppke.itk.plang.gui.widgets.ActivityBar;
import hu.ppke.itk.plang.gui.widgets.EditorTabBar;
import hu.ppke.itk.plang.gui.widgets.FlatButton;
import hu.ppke.itk.plang.gui.widgets.FlatScrollBarUI;
import hu.ppke.itk.plang.gui.widgets.FlatSplitPane;
import hu.ppke.itk.plang.gui.widgets.FindBar;
import hu.ppke.itk.plang.gui.widgets.PanelHeader;
import hu.ppke.itk.plang.gui.widgets.StatusBar;
import hu.ppke.itk.plang.gui.widgets.VSIcons;
import hu.ppke.itk.plang.prog.Lexer;
import hu.ppke.itk.plang.prog.MainProgram;
import hu.ppke.itk.plang.prog.State;
import hu.ppke.itk.plang.prog.StreamData;
import hu.ppke.itk.plang.prog.StreamKind;

/**
 * A PLanG fejlesztőkörnyezet munkafelülete – Visual Studio Code ihletésű
 * elrendezéssel.
 */
public class Workbench extends JPanel {

   private static final long serialVersionUID = 1L;

   private final JFrame owner;
   private javax.swing.JMenuBar menuBar;
   private Runnable exitHandler;

   private JFileChooser fileChooser;
   private Action loadAction;
   private Action saveAction;
   private Action saveAsAction;
   private Action newAction;
   private Action parseAction;
   private Action editAction;
   private Action copyAction;
   private Action runAction;
   private Action stopAction;
   private Action enterAction;
   private Action leaveAction;
   private Action showPreferences;
   private Action toggleThemeAction;
   private Action findAction;
   private Action helpAction;
   private Action undoAction;
   private Action redoAction;
   private Action gotoLineAction;
   private Action increaseFontAction;
   private Action decreaseFontAction;
   private Action replaceAction;

   private PrefDialog prefDialog;

   private static final int DEFAULT_STEPS = 10000;

   private ListSelectionListener selProgLine;
   private ListSelectionListener selProgState;
   private TreeSelectionListener selExprNode;

   private JList progList;
   private CodeEditor progText;
   private boolean progTextChanged;

   private static final String PROGLIST = "ProgList";
   private static final String PROGTEXT = "ProgText";
   private JPanel progPanel;
   private CardLayout progCards;

   private JList callStack;
   private JTable stateTable;
   private JTree exprTree;

   private StreamTabs inpPanes;
   private StreamTabs outPanes;

   private Font textFont;

   private ActivityBar activityBar;
   private JPanel sideBar;
   private CardLayout sideCards;
   private EditorTabBar editorTabs;
   private StatusBar statusBar;
   private FindBar findBar;
   private LineNumberGutter gutter;
   private JScrollPane editorScroll;
   private JScrollPane listScroll;
   private PanelHeader explorerHeader;
   private PanelHeader varsHeader;
   private PanelHeader exprHeader;
   private PanelHeader stackHeader;
   private JPanel callStackBox;
   /* FlatSplitPane típus, hogy a témaváltás után az egyedi osztópanel-UI
      visszaállítható legyen (updateComponentTreeUI lecserélné a LAF-éra). */
   private FlatSplitPane mainSplit;
   private FlatSplitPane rightSplit;
   private FlatSplitPane inspectSplit;
   private FlatSplitPane consoleSplit;
   private FlatSplitPane centerSplit;
   private JPanel outlinePanel;
   private JPanel varsPanel;
   private JPanel exprPanel;
   private JPanel inspectorPanel;
   private JPanel inpWrap;
   private JPanel outWrap;
   private JScrollPane tableScrl;
   private JScrollPane exprScrl;
   private JScrollPane csScroll;
   private JScrollPane outlineScrl;
   private JPanel rootPanel;
   private JPanel bodyPanel;
   private JPanel editorAreaPanel;
   private JPanel editorWrapPanel;
   private JPanel editorHolderPanel;
   private OutlineList outline;
   private ProgLineRenderer progRenderer;
   private ExprRenderer exprRenderer;
   private StateCellRenderer stateRenderer;
   private JLabel emptyStateLabel;

   private File currentFile;
   private String lastSearch = "";

   private int lastStepCount;
   private boolean running;
   /** A futás végén magától kilép-e a futtatási módból (beállítás). */
   private boolean autoStop = true;

   /* ---- új mezők ---- */
   private JMenu recentMenu;
   private List<File> recentFiles = new ArrayList<File>();
   private StatusBar.Cell msgCell;
   private Timer msgClearTimer;

   /* ================= állapotátmenetek ================= */

   private void editState() {
      this.loadAction.setEnabled(true);
      this.saveAction.setEnabled(true);
      this.saveAsAction.setEnabled(true);
      this.parseAction.setEnabled(true);
      this.editAction.setEnabled(false);
      this.copyAction.setEnabled(false);
      this.runAction.setEnabled(false);
      this.progCards.show(this.progPanel, PROGTEXT);
      this.editorTabs.select("editor");
      updateStatus();
      updateUndoRedo();
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            progText.requestFocusInWindow();
         }
      });
   }

   private void listState() {
      this.loadAction.setEnabled(false);
      /* A programszöveg értelmezett nézetben is menthető: a szerkesztő
         tartalma változatlanul rendelkezésre áll. */
      this.saveAction.setEnabled(true);
      this.saveAsAction.setEnabled(true);
      this.parseAction.setEnabled(false);
      this.editAction.setEnabled(true);
      this.progCards.show(this.progPanel, PROGLIST);
      this.editorTabs.select("parsed");
      updateStatus();
   }

   private void runState() {
      this.editAction.setEnabled(false);
      this.copyAction.setEnabled(false);
      this.runAction.setEnabled(false);
      this.stopAction.setEnabled(true);
      /* Futás közben is menthető a programszöveg. */
      this.saveAction.setEnabled(true);
      this.saveAsAction.setEnabled(true);
      this.running = true;
      this.statusBar.setRunning(true);
      this.inpPanes.setEditable(false);
      updateStatus();
   }

   private void stopState() {
      this.editAction.setEnabled(true);
      this.copyAction.setEnabled(true);
      this.runAction.setEnabled(true);
      this.stopAction.setEnabled(false);
      this.saveAction.setEnabled(true);
      this.saveAsAction.setEnabled(true);
      this.enterAction.setEnabled(false);
      this.leaveAction.setEnabled(false);
      this.running = false;
      this.statusBar.setRunning(false);
      this.inpPanes.setEditable(true);
      this.inpPanes.resetAttributes();
      if (progRenderer != null) {
         progRenderer.setCurrentLine(-1);
      }
      if (progList != null) {
         progList.repaint();
      }
      progText.setRunningLine(-1);
      updateStatus();
   }

   /**
    * A futás befejeződött: a vezérlők visszaállnak, de a lefutás eredménye
    * (állapottábla, kimeneti csatornák) a képernyőn marad, hogy vissza
    * lehessen nézni. A „Stop” ezután üríti ki a nézetet.
    */
   private void finishedState(boolean hadError) {
      this.editAction.setEnabled(true);
      this.copyAction.setEnabled(true);
      this.runAction.setEnabled(true);
      this.stopAction.setEnabled(true);
      this.saveAction.setEnabled(true);
      this.saveAsAction.setEnabled(true);
      this.running = false;
      if (this.statusBar != null) {
         this.statusBar.setRunning(false);
      }
      this.inpPanes.setEditable(true);
      showTransientMessage(hadError
            ? "A program futása hiba miatt megszakadt – leállítva."
            : "A program lefutott – leállítva.");
      updateStatus();
   }

   /* ================= csatornakezelés ================= */

   private static StreamDocument getDocument(StreamTabs panes, int ind) {
      return panes.getDocument(ind);
   }

   private void updatePanes(StreamTabs panes, Set<String> streams, boolean edit) {
      panes.sync(streams, edit, this.textFont);
   }

   private static void setStreamStates(StreamTabs panes, State state) {
      for (int i = 0; i < panes.count(); i++) {
         panes.getDocument(i).setState(state == null ? null : state.getStreamState(panes.getTitleAt(i)));
      }
   }

   private void updateFont() {
      if (progRenderer != null) {
         progRenderer.setFont(this.textFont);
      }
      this.progList.setFixedCellHeight(
         this.progList.getFontMetrics(this.textFont).getHeight() + 4);
      this.stateTable.setFont(this.textFont);
      this.stateTable.setRowHeight(
         this.stateTable.getFontMetrics(this.textFont).getHeight() + 8);
      this.stateTable.getTableHeader().setFont(Theme.uiSmallBold());
      if (stateRenderer != null) {
         stateRenderer.setFont(this.textFont);
      }
      this.progText.setEditorFont(this.textFont);
      if (gutter != null) {
         gutter.setFont(this.textFont);
         gutter.revalidate();
      }
      if (exprRenderer != null) {
         exprRenderer.setFont(this.textFont);
      }
      this.exprTree.setRowHeight(this.exprTree.getFontMetrics(this.textFont).getHeight() + 6);
      this.inpPanes.setTextFont(this.textFont);
      this.outPanes.setTextFont(this.textFont);
      this.callStack.setFont(this.textFont);
      if (outline != null) {
         outline.setFont(Theme.uiPlain());
      }
      repaint();
   }

   /* ========================= felépítés ========================= */

   public Workbench(JFrame owner) {
      super(new BorderLayout());
      this.owner = owner;

      Theme.installUIDefaults();

      // betűtípus betöltése prefs-ből
      try {
         String fam = AppPrefs.getFontFamily();
         int sz = AppPrefs.getFontSize();
         this.textFont = new Font(fam, Font.PLAIN, sz);
      } catch (Exception e) {
         this.textFont = Theme.mono(Font.PLAIN, 13);
      }

      this.fileChooser = new JFileChooser();
      /* Az "All Files" szűrő helyett a Plang-szűrő legyen az alapértelmezett. */
      this.fileChooser.setAcceptAllFileFilterUsed(false);
      PlangFilter pf = new PlangFilter(null);
      this.fileChooser.addChoosableFileFilter(pf);
      this.fileChooser.setFileFilter(pf);

      try {
         this.autoStop = AppPrefs.getAutoStop();
      } catch (Exception e) {
         this.autoStop = true;
      }

      // recent files betöltése
      loadRecentFiles();

      buildActions();

      this.progTextChanged = false;

      buildComponents();
      buildLayout();
      buildMenu();
      installShortcuts();

      applyThemeToAll();
      updateFont();

      int ww = 1440;
      int wh = 876;
      try {
         ww = AppPrefs.getWindowWidth();
         wh = AppPrefs.getWindowHeight();
      } catch (Exception e) {}
      setPreferredSize(new Dimension(ww, wh));

      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            // osztópanelek betöltése prefs-ből, ha van
            int mainDiv = AppPrefs.getDividerMain();
            int centerDiv = AppPrefs.getDividerCenter();
            int rightDiv = AppPrefs.getDividerRight();
            int inspectDiv = AppPrefs.getDividerInspect();
            int consoleDiv = AppPrefs.getDividerConsole();

            if (mainDiv > 0) {
               mainSplit.setDividerLocation(mainDiv);
            } else {
               mainSplit.setDividerLocation(260);
            }
            if (centerDiv > 0) {
               centerSplit.setDividerLocation(centerDiv);
            } else {
               centerSplit.setDividerLocation(0.62);
            }
            if (rightDiv > 0) {
               rightSplit.setDividerLocation(rightDiv);
            } else {
               rightSplit.setDividerLocation(0.68);
            }
            if (inspectDiv > 0) {
               inspectSplit.setDividerLocation(inspectDiv);
            } else {
               inspectSplit.setDividerLocation(0.55);
            }
            if (consoleDiv > 0) {
               consoleSplit.setDividerLocation(consoleDiv);
            } else {
               consoleSplit.setDividerLocation(0.5);
            }
         }
      });

      this.editState();
      updateStatus();
      updateRecentMenu();
   }

   /* ------------------------- műveletek ------------------------- */

   private void buildActions() {
      this.loadAction = new AbstractAction("Megnyitás…") {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent ev) {
            if (!Workbench.this.progTextChanged
                || JOptionPane.showConfirmDialog(Workbench.this,
                      "A programszöveg változásai nincsenek elmentve. Biztosan be akarsz tölteni egy új fájlt?",
                      "Betöltés", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == 0) {
               if (Workbench.this.fileChooser.showOpenDialog(Workbench.this)
                      == JFileChooser.APPROVE_OPTION) {
                  Workbench.this.loadFile(Workbench.this.fileChooser.getSelectedFile());
               }
            }
         }
      };

      this.saveAction = new AbstractAction("Mentés") {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent ev) {
            if (Workbench.this.currentFile != null) {
               File target = ensurePlangExtension(Workbench.this.currentFile);
               Workbench.this.saveFile(target);
            } else {
               saveAsAction.actionPerformed(ev);
            }
         }
      };

      this.saveAsAction = new AbstractAction("Mentés másként…") {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent ev) {
            if (Workbench.this.fileChooser.showSaveDialog(Workbench.this)
                   == JFileChooser.APPROVE_OPTION) {
               File chosen = Workbench.this.fileChooser.getSelectedFile();
               File target = ensurePlangExtension(chosen);
               if (target.exists()) {
                  if (JOptionPane.showConfirmDialog(Workbench.this,
                        "A fájl létezik, felülírjam?", "Létező fájl",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != 0) {
                     return;
                  }
               }
               Workbench.this.saveFile(target);
            }
         }
      };

      this.newAction = new AbstractAction("Új program") {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent ev) {
            if (Workbench.this.progTextChanged
                && JOptionPane.showConfirmDialog(Workbench.this,
                      "A programszöveg változásai nincsenek elmentve. Biztosan új programot kezdesz?",
                      "Új program", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != 0) {
               return;
            }
            Workbench.this.progText.setText(
               "** Új PLanG program\n"
               + "PROGRAM ujprogram\n"
               + "VÁLTOZÓK:\n"
               + "  x: EGÉSZ\n"
               + "\n"
               + "  BE: x\n"
               + "  KI: x\n"
               + "PROGRAM_VÉGE\n");
            Workbench.this.progText.discardUndoHistory();
            Workbench.this.progText.setCaretPosition(0);
            Workbench.this.progTextChanged = false;
            Workbench.this.currentFile = null;
            Workbench.this.editorTabs.setTitle("editor", "névtelen.plang");
            Workbench.this.editorTabs.setDirty("editor", false);
            Workbench.this.setFrameTitle("PLanG");
            Workbench.this.editState();
            Workbench.this.refreshOutline();
            Workbench.this.updateUndoRedo();
         }
      };

      this.undoAction = new AbstractAction("Visszavonás") {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent e) {
            Workbench.this.progText.undo();
            Workbench.this.updateUndoRedo();
            Workbench.this.updateStatus();
         }
      };

      this.redoAction = new AbstractAction("Újra") {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent e) {
            Workbench.this.progText.redo();
            Workbench.this.updateUndoRedo();
            Workbench.this.updateStatus();
         }
      };

      this.gotoLineAction = new AbstractAction("Ugrás sorra…") {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent e) {
            String input = JOptionPane.showInputDialog(Workbench.this,
                  "Sor száma (1-" + progText.lineCount() + "):", "Ugrás sorra",
                  JOptionPane.QUESTION_MESSAGE);
            if (input != null) {
               try {
                  int line = Integer.parseInt(input.trim());
                  if (line >= 1 && line <= progText.lineCount()) {
                     progText.gotoLine(line - 1);
                     progText.requestFocusInWindow();
                  }
               } catch (NumberFormatException ex) {
                  // figyelmen kívül
               }
            }
         }
      };

      this.increaseFontAction = new AbstractAction("Nagyítás") {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent e) {
            Font f = textFont;
            int newSize = Math.min(42, f.getSize() + 1);
            if (newSize != f.getSize()) {
               textFont = new Font(f.getFamily(), f.getStyle(), newSize);
               updateFont();
               AppPrefs.setFontFamily(textFont.getFamily());
               AppPrefs.setFontSize(newSize);
               AppPrefs.flush();
               updateStatus();
            }
         }
      };

      this.decreaseFontAction = new AbstractAction("Kicsinyítés") {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent e) {
            Font f = textFont;
            int newSize = Math.max(8, f.getSize() - 1);
            if (newSize != f.getSize()) {
               textFont = new Font(f.getFamily(), f.getStyle(), newSize);
               updateFont();
               AppPrefs.setFontFamily(textFont.getFamily());
               AppPrefs.setFontSize(newSize);
               AppPrefs.flush();
               updateStatus();
            }
         }
      };

      this.parseAction = new AbstractAction("Értelmez") {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent ev) {
            try {
               MainProgram prog = MainProgram.parseMainProgram(
                  new Lexer(new StringReader(Workbench.this.progText.getText())));
               ((ProgramList) Workbench.this.progList.getModel()).setProgram(prog);
               Workbench.this.listState();
               boolean ok = !prog.hasError();
               Workbench.this.runAction.setEnabled(ok);
               Workbench.this.copyAction.setEnabled(ok);
               if (ok) {
                  Workbench.this.updatePanes(Workbench.this.inpPanes,
                                             prog.getStreams(StreamKind.INPUT), true);
                  Workbench.this.updatePanes(Workbench.this.outPanes,
                                             prog.getStreams(StreamKind.OUTPUT), false);
               }
               Workbench.this.markErrors(prog);
               Workbench.this.refreshOutline();
               Workbench.this.updateStatus();
            } catch (Throwable t) {
               if ("off".equals(System.getProperty("hu.ppke.itk.plang.errorDlg"))) {
                  throw new RuntimeException(t);
               }
               JOptionPane.showMessageDialog(Workbench.this,
                  "Belső hiba történt a szöveg értelmezése közben.", "HIBA",
                  JOptionPane.ERROR_MESSAGE);
            }
         }
      };

      this.editAction = new AbstractAction("Szerkeszt") {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent ev) {
            Workbench.this.editState();
         }
      };

      this.copyAction = new AbstractAction("Másol") {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent ev) {
            ProgramList lst = (ProgramList) Workbench.this.progList.getModel();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < lst.getSize(); ++i) {
               sb.append(ProgLineRenderer.stripHtml(lst.getElementAt(i).toString())[0])
                 .append("\n");
            }
            Workbench.this.progText.setText(sb.toString());
            Workbench.this.progText.discardUndoHistory();
            Workbench.this.progText.setCaretPosition(0);
            Workbench.this.progTextChanged = false;
            Workbench.this.editorTabs.setDirty("editor", true);
            Workbench.this.editState();
            Workbench.this.refreshOutline();
            Workbench.this.updateStatus();
            Workbench.this.updateUndoRedo();
         }
      };

      this.runAction = new AbstractAction("Start") {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent ev) {
            MainProgram prog = ((ProgramList) Workbench.this.progList.getModel()).getProgram();
            if (prog != null && !prog.hasError()) {
               Map<String, String> input = new HashMap<String, String>();
               for (int i = 0; i < Workbench.this.inpPanes.count(); ++i) {
                  input.put(Workbench.this.inpPanes.getTitleAt(i),
                            getDocument(Workbench.this.inpPanes, i).getText());
               }

               CallStack cs = (CallStack) Workbench.this.callStack.getModel();
               try {
                  Map<String, StreamData> output = new TreeMap<String, StreamData>();
                  State last = cs.runProgram(prog, input, output);

                  for (int i = 0; i < Workbench.this.outPanes.count(); ++i) {
                     getDocument(Workbench.this.outPanes, i)
                        .setStream(output.get(Workbench.this.outPanes.getTitleAt(i)));
                  }

                  Workbench.this.runState();
                  Workbench.this.lastStepCount = Workbench.this.stateTable.getRowCount();
                  Workbench.this.stateTable.setRowSelectionInterval(
                     Workbench.this.stateTable.getRowCount() - 1,
                     Workbench.this.stateTable.getRowCount() - 1);
                  Workbench.this.stateTable.scrollRectToVisible(
                     Workbench.this.stateTable.getCellRect(
                        Workbench.this.stateTable.getSelectedRow(),
                        Math.max(0, Workbench.this.stateTable.getSelectedColumn()), true));
                  Workbench.this.autoSizeStateColumns();
                  Workbench.this.updateStatus();

                  boolean hadError = last.getError() != null;
                  /* A futás végén (illetve hibára mindenképp) kilépünk a
                     futtatási módból – az eredmény a képernyőn marad. */
                  if (hadError || Workbench.this.autoStop) {
                     Workbench.this.finishedState(hadError);
                  }

                  if (hadError) {
                     JOptionPane.showMessageDialog(Workbench.this,
                        "A program futása a következő hiba miatt megszakadt:\n" + last.getError(),
                        "Futási hiba", JOptionPane.WARNING_MESSAGE);
                  }
               } catch (OutOfMemoryError var8) {
                  JOptionPane.showMessageDialog(Workbench.this,
                     "Elfogyott a memória a program futásának szimulációja során.\n"
                     + "Próbálkozz kevesebb lépést végrehajtani, vagy kisebb tömböket használni.",
                     "Hiba", JOptionPane.ERROR_MESSAGE);
               } catch (Throwable t) {
                  if ("off".equals(System.getProperty("hu.ppke.itk.plang.errorDlg"))) {
                     throw new RuntimeException(t);
                  }
                  JOptionPane.showMessageDialog(Workbench.this,
                     "Belső hiba történt a program futásának szimulációja közben.", "HIBA",
                     JOptionPane.ERROR_MESSAGE);
               }
            }
         }
      };

      this.stopAction = new AbstractAction("Stop") {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent e) {
            CallStack cs = (CallStack) Workbench.this.callStack.getModel();
            cs.runProgram(null, null, null);
            setStreamStates(Workbench.this.inpPanes, null);
            for (int i = 0; i < Workbench.this.outPanes.count(); ++i) {
               getDocument(Workbench.this.outPanes, i).setStream(null);
            }
            Workbench.this.stopState();
         }
      };

      this.enterAction = new AbstractAction("Belépés") {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent e) {
            TreePath path = Workbench.this.exprTree.getSelectionPath();
            if (path == null) {
               return;
            }
            ExprNode n = (ExprNode) path.getLastPathComponent();
            List<State> sp = n.getSubStates();
            if (sp != null) {
               ((CallStack) Workbench.this.callStack.getModel()).enter(n.toString(), sp);
            }
            Workbench.this.leaveAction.setEnabled(true);
            Workbench.this.updateStatus();
         }
      };

      this.leaveAction = new AbstractAction("Kilépés") {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent e) {
            ((CallStack) Workbench.this.callStack.getModel()).leave();
            if (Workbench.this.callStack.getModel().getSize() <= 1) {
               Workbench.this.leaveAction.setEnabled(false);
            }
            Workbench.this.updateStatus();
         }
      };

      this.showPreferences = new AbstractAction("Beállítások") {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent e) {
            // prefs dialog értékeinek szinkronizálása aktuális állapottal
            PrefDialog pd = Workbench.this.prefs();
            pd.setValues(Workbench.this.textFont,
                         ((CallStack) Workbench.this.callStack.getModel()).getMaxSteps(),
                         Theme.mode(),
                         Workbench.this.progText.isShowIndentGuides(),
                         Workbench.this.autoStop);
            if (pd.showDlg()) {
               Workbench.this.textFont = pd.getTextFont();
               Workbench.this.progText.setShowIndentGuides(pd.isShowIndentGuides());
               Workbench.this.updateFont();
               ((CallStack) Workbench.this.callStack.getModel())
                  .setMaxSteps(pd.getStepNum());
               Workbench.this.autoStop = pd.isAutoStop();
               if (pd.getThemeMode() != Theme.mode()) {
                  Theme.setMode(pd.getThemeMode());
                  Workbench.this.applyThemeToAll();
               }
               Workbench.this.updateStatus();
               // mentés prefs-be
               AppPrefs.setFontFamily(pd.getFontFamily());
               AppPrefs.setFontSize(pd.getFontSizeValue());
               AppPrefs.setStepNum(pd.getStepNum());
               AppPrefs.setThemeMode(pd.getThemeMode());
               AppPrefs.setIndentGuides(pd.isShowIndentGuides());
               AppPrefs.setAutoStop(pd.isAutoStop());
               AppPrefs.flush();
            }
         }
      };

      this.toggleThemeAction = new AbstractAction("Téma váltása") {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent e) {
            Theme.toggleMode();
            if (Workbench.this.prefDialog != null) {
               Workbench.this.prefDialog.setThemeMode(Theme.mode());
            }
            Workbench.this.applyThemeToAll();
            Workbench.this.updateStatus();
            AppPrefs.setThemeMode(Theme.mode());
            AppPrefs.flush();
         }
      };

      this.findAction = new AbstractAction("Keresés") {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent e) {
            Workbench.this.progCards.show(Workbench.this.progPanel, PROGTEXT);
            Workbench.this.editorTabs.select("editor");
            Workbench.this.findBar.showBar(Workbench.this.progText.getSelectedText());
         }
      };

      this.replaceAction = new AbstractAction("Csere") {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent e) {
            Workbench.this.progCards.show(Workbench.this.progPanel, PROGTEXT);
            Workbench.this.editorTabs.select("editor");
            Workbench.this.findBar.showBarWithReplace(Workbench.this.progText.getSelectedText());
         }
      };

      this.helpAction = new AbstractAction("Súgó") {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent e) {
            showHelp();
         }
      };

      this.loadAction.putValue(Action.SHORT_DESCRIPTION, "Betöltés  (Ctrl+O)");
      this.saveAction.putValue(Action.SHORT_DESCRIPTION, "Mentés  (Ctrl+S)");
      this.saveAsAction.putValue(Action.SHORT_DESCRIPTION, "Mentés másként  (Ctrl+Shift+S)");
      this.newAction.putValue(Action.SHORT_DESCRIPTION, "Új program  (Ctrl+N)");
      this.parseAction.putValue(Action.SHORT_DESCRIPTION, "Programszöveg értelmezése  (Ctrl+B)");
      this.editAction.putValue(Action.SHORT_DESCRIPTION, "Szerkesztés");
      this.copyAction.putValue(Action.SHORT_DESCRIPTION, "Értelmezett program szerkesztése");
      this.runAction.putValue(Action.SHORT_DESCRIPTION, "Futtatás  (F5)");
      this.stopAction.putValue(Action.SHORT_DESCRIPTION, "Futtatás vége  (Shift+F5)");
      this.stopAction.setEnabled(false);
      this.enterAction.putValue(Action.SHORT_DESCRIPTION, "Belépés alprogramba  (F11)");
      this.enterAction.setEnabled(false);
      this.leaveAction.putValue(Action.SHORT_DESCRIPTION, "Alprogram elhagyása  (Shift+F11)");
      this.leaveAction.setEnabled(false);
      this.showPreferences.putValue(Action.SHORT_DESCRIPTION, "Beállítások  (Ctrl+,)");
      this.toggleThemeAction.putValue(Action.SHORT_DESCRIPTION, "Világos / sötét téma");
      this.findAction.putValue(Action.SHORT_DESCRIPTION, "Keresés  (Ctrl+F)");
      this.replaceAction.putValue(Action.SHORT_DESCRIPTION, "Csere  (Ctrl+H)");
      this.helpAction.putValue(Action.SHORT_DESCRIPTION, "Nyelvi súgó  (F1)");
      this.undoAction.putValue(Action.SHORT_DESCRIPTION, "Visszavonás  (Ctrl+Z)");
      this.redoAction.putValue(Action.SHORT_DESCRIPTION, "Újra  (Ctrl+Y)");
      this.gotoLineAction.putValue(Action.SHORT_DESCRIPTION, "Ugrás sorra  (Ctrl+G)");
      this.increaseFontAction.putValue(Action.SHORT_DESCRIPTION, "Nagyítás  (Ctrl++)");
      this.decreaseFontAction.putValue(Action.SHORT_DESCRIPTION, "Kicsinyítés  (Ctrl+-)");
   }

   /* ------------------------ komponensek ------------------------ */

   private void buildComponents() {
      this.progRenderer = new ProgLineRenderer(this.textFont);
      this.progList = new JList(new ProgramList());
      this.progList.setCellRenderer(this.progRenderer);
      this.progList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      this.progList.setBackground(Theme.p().editorBg);
      this.progList.setFixedCellHeight(20);

      this.progText = new CodeEditor();
      this.progText.setEditorFont(this.textFont);
      try {
         this.progText.setShowIndentGuides(AppPrefs.getIndentGuides());
      } catch (Exception e) {}
      this.progText.getDocument().addDocumentListener(new DocumentListener() {
         public void changedUpdate(DocumentEvent e) { markChanged(); }
         public void insertUpdate(DocumentEvent e) { markChanged(); }
         public void removeUpdate(DocumentEvent e) { markChanged(); }
         private void markChanged() {
            Workbench.this.progTextChanged = true;
            Workbench.this.editorTabs.setDirty("editor", true);
            Workbench.this.refreshOutline();
            Workbench.this.updateStatus();
            Workbench.this.updateUndoRedo();
         }
      });
      this.progText.addCaretListener(new javax.swing.event.CaretListener() {
         public void caretUpdate(javax.swing.event.CaretEvent e) {
            updateStatus();
         }
      });

      // Ctrl+egérgörgő betűméret
      this.progText.addMouseWheelListener(new MouseWheelListener() {
         public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.isControlDown()) {
               e.consume();
               if (e.getWheelRotation() < 0) {
                  increaseFontAction.actionPerformed(null);
               } else {
                  decreaseFontAction.actionPerformed(null);
               }
            }
         }
      });

      this.exprRenderer = new ExprRenderer(this.textFont);
      this.exprTree = new JTree(new ExprTree());
      this.exprTree.setCellRenderer(this.exprRenderer);
      this.exprTree.setShowsRootHandles(true);
      this.exprTree.setRootVisible(true);
      this.exprTree.setBackground(Theme.p().panelBg);
      this.exprTree.getSelectionModel().setSelectionMode(
         javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION);

      this.stateTable = new JTable(new StateList());
      this.stateRenderer = new StateCellRenderer(this.textFont);
      this.stateTable.setDefaultRenderer(Object.class, this.stateRenderer);
      this.stateTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      this.stateTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
      this.stateTable.setShowGrid(false);
      this.stateTable.setIntercellSpacing(new Dimension(0, 0));
      this.stateTable.setBackground(Theme.p().panelBg);
      this.stateTable.setFillsViewportHeight(true);
      JTableHeader th = this.stateTable.getTableHeader();
      th.setReorderingAllowed(false);
      th.setDefaultRenderer(new HeaderRenderer());
      th.setPreferredSize(new Dimension(10, 26));

      this.callStack = new JList(new CallStack((StateList) this.stateTable.getModel(),
                                               AppPrefs.getStepNum()));
      this.callStack.setBackground(Theme.p().panelBg);
      this.callStack.setCellRenderer(new CallStackRenderer());
      this.callStack.setFixedCellHeight(22);

      this.inpPanes = new StreamTabs(StreamKind.INPUT);
      this.outPanes = new StreamTabs(StreamKind.OUTPUT);

      this.selProgLine = new ListSelectionListener() {
         public void valueChanged(ListSelectionEvent ev) {
            if (!ev.getValueIsAdjusting()) {
               ProgramLine line = (ProgramLine) Workbench.this.progList.getSelectedValue();
               if (line != null) {
                  ((ExprTree) Workbench.this.exprTree.getModel()).setRoot(line.getExpr(null));
               } else {
                  ((ExprTree) Workbench.this.exprTree.getModel()).setRoot(null);
               }
               expandExprTree();
            }
         }
      };
      this.progList.addListSelectionListener(this.selProgLine);

      this.selProgState = new ListSelectionListener() {
         public void valueChanged(ListSelectionEvent ev) {
            if (!ev.getValueIsAdjusting()) {
               State state = ((StateList) Workbench.this.stateTable.getModel())
                  .getState(Workbench.this.stateTable.getSelectedRow());
               if (state == null) {
                  Workbench.this.progList.setSelectedIndex(-1);
                  ((ExprTree) Workbench.this.exprTree.getModel()).setRoot(ExprNode.EMPTY);
                  Workbench.this.progRenderer.setCurrentLine(-1);
                  Workbench.this.progText.setRunningLine(-1);
               } else {
                  ProgramLine line = (ProgramLine) Workbench.this.progList.getModel()
                     .getElementAt(state.getLine());
                  if (state.getError() == null) {
                     Workbench.this.progList.setSelectedIndex(state.getLine());
                     Workbench.this.progList.ensureIndexIsVisible(
                        Workbench.this.progList.getSelectedIndex());
                     ((ExprTree) Workbench.this.exprTree.getModel()).setRoot(line.getExpr(state));
                  } else {
                     Workbench.this.progList.setSelectedIndex(state.getLine());
                     ((ExprTree) Workbench.this.exprTree.getModel()).setRoot(
                        new ExprNode("<font color=\"" + Theme.errorHex() + "\">"
                                     + state.getError() + "</font>", new ExprNode[0], (String) null));
                  }
                  Workbench.this.progRenderer.setCurrentLine(state.getLine());
                  Workbench.this.progList.repaint();
                  setStreamStates(Workbench.this.inpPanes, state);
                  setStreamStates(Workbench.this.outPanes, state);
               }
               expandExprTree();
               Workbench.this.updateStatus();
            }
         }
      };
      this.stateTable.getSelectionModel().addListSelectionListener(this.selProgState);

      this.selExprNode = new TreeSelectionListener() {
         public void valueChanged(TreeSelectionEvent e) {
            TreePath p = Workbench.this.exprTree.getSelectionPath();
            if (p != null) {
               ExprNode node = (ExprNode) p.getLastPathComponent();
               Workbench.this.enterAction.setEnabled(node.getSubStates() != null);
            } else {
               Workbench.this.enterAction.setEnabled(false);
            }
         }
      };
      this.exprTree.getSelectionModel().addTreeSelectionListener(this.selExprNode);
      this.exprTree.addMouseListener(new MouseAdapter() {
         public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
               if (Workbench.this.enterAction.isEnabled()) {
                  Workbench.this.enterAction.actionPerformed(null);
               }
            }
         }
      });

      this.progList.addMouseListener(new MouseAdapter() {
         public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() >= 2) {
               int idx = Workbench.this.progList.getSelectedIndex();
               if (idx >= 0) {
                  Workbench.this.editAction.actionPerformed(null);
                  Workbench.this.progText.gotoLine(idx);
               }
            }
         }
      });
   }

   /* -------------------------- elrendezés -------------------------- */

   private void buildLayout() {
      rootPanel = new JPanel(new BorderLayout());
      rootPanel.setBackground(Theme.p().editorBg);
      JPanel root = rootPanel;

      activityBar = new ActivityBar();
      activityBar.addView(VSIcons.FILES, "Kezelő  (Ctrl+Shift+E)", new Runnable() {
         public void run() { sideCards.show(sideBar, "explorer"); }
      });
      activityBar.addView(VSIcons.RUN, "Futtatás és hibakeresés  (Ctrl+Shift+D)", new Runnable() {
         public void run() { sideCards.show(sideBar, "run"); }
      });
      activityBar.addView(VSIcons.SEARCH, "Keresés  (Ctrl+Shift+F)", new Runnable() {
         public void run() {
            sideCards.show(sideBar, "search");
            findAction.actionPerformed(null);
         }
      });
      activityBar.addBottomAction(VSIcons.THEME, "Világos / sötét téma", new Runnable() {
         public void run() { toggleThemeAction.actionPerformed(null); }
      });
      activityBar.addBottomAction(VSIcons.SETTINGS, "Beállítások  (Ctrl+,)", new Runnable() {
         public void run() { showPreferences.actionPerformed(null); }
      });

      sideCards = new CardLayout();
      sideBar = new JPanel(sideCards);
      sideBar.setBackground(Theme.p().sideBar);
      sideBar.add(buildExplorerPanel(), "explorer");
      sideBar.add(buildRunPanel(), "run");
      sideBar.add(buildSearchPanel(), "search");

      editorTabs = new EditorTabBar();
      editorTabs.addTab(new EditorTabBar.Tab("editor", "névtelen.plang", VSIcons.NEW, false));
      editorTabs.addTab(new EditorTabBar.Tab("parsed", "Értelmezett program", VSIcons.PARSE, false));
      editorTabs.setListener(new EditorTabBar.Listener() {
         public void tabSelected(String id) {
            if ("editor".equals(id)) {
               if (editAction.isEnabled()) {
                  editAction.actionPerformed(null);
               } else {
                  progCards.show(progPanel, PROGTEXT);
               }
            } else {
               progCards.show(progPanel, PROGLIST);
            }
            updateStatus();
         }
         public void tabClosed(String id) {}
      });

      progCards = new CardLayout();
      progPanel = new JPanel(progCards);
      progPanel.setBackground(Theme.p().editorBg);

      gutter = new LineNumberGutter(progText);
      gutter.setFont(textFont);
      editorWrapPanel = new JPanel(new BorderLayout());
      editorWrapPanel.setBackground(Theme.p().editorBg);
      editorWrapPanel.add(progText, BorderLayout.CENTER);
      editorScroll = new JScrollPane(editorWrapPanel);
      editorScroll.setRowHeaderView(gutter);
      editorScroll.getRowHeader().setBackground(Theme.p().editorBg);
      editorScroll.setBorder(BorderFactory.createEmptyBorder());
      FlatScrollBarUI.install(editorScroll);
      editorScroll.addMouseWheelListener(new MouseWheelListener() {
         public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.isControlDown()) {
               e.consume();
               if (e.getWheelRotation() < 0) {
                  increaseFontAction.actionPerformed(null);
               } else {
                  decreaseFontAction.actionPerformed(null);
               }
            }
         }
      });

      editorHolderPanel = new JPanel(new BorderLayout());
      editorHolderPanel.setBackground(Theme.p().editorBg);
      findBar = new FindBar(progText);
      editorHolderPanel.add(findBar, BorderLayout.NORTH);
      editorHolderPanel.add(editorScroll, BorderLayout.CENTER);
      JPanel editorHolder = editorHolderPanel;

      listScroll = new JScrollPane(progList);
      FlatScrollBarUI.install(listScroll);
      listScroll.getViewport().setBackground(Theme.p().editorBg);

      progPanel.add(editorHolder, PROGTEXT);
      progPanel.add(listScroll, PROGLIST);

      editorAreaPanel = new JPanel(new BorderLayout());
      editorAreaPanel.setBackground(Theme.p().editorBg);
      editorAreaPanel.add(editorTabs, BorderLayout.NORTH);
      editorAreaPanel.add(progPanel, BorderLayout.CENTER);

      inpWrap = wrapStreamPanel(inpPanes, "Bemenet", VSIcons.INPUT);
      outWrap = wrapStreamPanel(outPanes, "Kimenet", VSIcons.OUTPUT);
      consoleSplit = new FlatSplitPane(JSplitPane.HORIZONTAL_SPLIT, inpWrap, outWrap);
      consoleSplit.setResizeWeight(0.5);

      centerSplit = new FlatSplitPane(JSplitPane.VERTICAL_SPLIT, editorAreaPanel, consoleSplit);
      centerSplit.setResizeWeight(0.65);

      varsPanel = new JPanel(new BorderLayout());
      varsPanel.setBackground(Theme.p().panelBg);
      varsHeader = new PanelHeader("Változók", VSIcons.VARIABLES, Theme.p().synType);
      tableScrl = new JScrollPane(stateTable);
      tableScrl.setColumnHeaderView(stateTable.getTableHeader());
      FlatScrollBarUI.install(tableScrl);
      tableScrl.getViewport().setBackground(Theme.p().panelBg);
      tableScrl.getColumnHeader().setBackground(Theme.p().tableHeaderBg);
      varsPanel.add(varsHeader, BorderLayout.NORTH);
      varsPanel.add(tableScrl, BorderLayout.CENTER);

      exprPanel = new JPanel(new BorderLayout());
      exprPanel.setBackground(Theme.p().panelBg);
      exprHeader = new PanelHeader("Kifejezés kiértékelése", VSIcons.TREE, Theme.p().synFunction);
      FlatButton enterBtn = new FlatButton(FlatButton.TOOL,
         VSIcons.icon(VSIcons.STEP_INTO, 16, Theme.p().sideBarFg), "Belépés alprogramba");
      enterBtn.setAction(enterAction);
      enterBtn.setPadding(4, 4);
      FlatButton leaveBtn = new FlatButton(FlatButton.TOOL,
         VSIcons.icon(VSIcons.STEP_OUT, 16, Theme.p().sideBarFg), "Alprogram elhagyása");
      leaveBtn.setAction(leaveAction);
      leaveBtn.setPadding(4, 4);
      exprHeader.addAction(enterBtn);
      exprHeader.addAction(leaveBtn);
      exprScrl = new JScrollPane(exprTree);
      FlatScrollBarUI.install(exprScrl);
      exprScrl.getViewport().setBackground(Theme.p().panelBg);
      exprPanel.add(exprHeader, BorderLayout.NORTH);
      exprPanel.add(exprScrl, BorderLayout.CENTER);

      inspectSplit = new FlatSplitPane(JSplitPane.VERTICAL_SPLIT, varsPanel, exprPanel);
      inspectSplit.setResizeWeight(0.55);

      callStackBox = new JPanel(new BorderLayout());
      callStackBox.setBackground(Theme.p().panelBg);
      stackHeader = new PanelHeader("Hívási verem", VSIcons.CALLSTACK, Theme.p().synControl);
      csScroll = new JScrollPane(callStack);
      FlatScrollBarUI.install(csScroll);
      csScroll.getViewport().setBackground(Theme.p().panelBg);
      csScroll.setPreferredSize(new Dimension(200, 92));
      callStackBox.add(stackHeader, BorderLayout.NORTH);
      callStackBox.add(csScroll, BorderLayout.CENTER);

      inspectorPanel = new JPanel(new BorderLayout());
      inspectorPanel.setBackground(Theme.p().panelBg);
      callStackBox.setVisible(subProgramsEnabled());
      inspectorPanel.add(callStackBox, BorderLayout.NORTH);
      inspectorPanel.add(inspectSplit, BorderLayout.CENTER);

      rightSplit = new FlatSplitPane(JSplitPane.HORIZONTAL_SPLIT, centerSplit, inspectorPanel);
      rightSplit.setResizeWeight(0.68);

      mainSplit = new FlatSplitPane(JSplitPane.HORIZONTAL_SPLIT, sideBar, rightSplit);
      mainSplit.setResizeWeight(0.0);

      bodyPanel = new JPanel(new BorderLayout());
      bodyPanel.setBackground(Theme.p().editorBg);
      bodyPanel.add(activityBar, BorderLayout.WEST);
      bodyPanel.add(mainSplit, BorderLayout.CENTER);
      JPanel body = bodyPanel;

      statusBar = new StatusBar();
      StatusBar.Cell run = statusBar.add("run", "Futtatás", false);
      run.iconType = VSIcons.PLAY;
      run.tooltip = "Program futtatása (F5)";
      run.action = new Runnable() {
         public void run() {
            if (runAction.isEnabled()) {
               runAction.actionPerformed(null);
            } else if (parseAction.isEnabled()) {
               parseAction.actionPerformed(null);
            }
         }
      };
      StatusBar.Cell diag = statusBar.add("diag", "0 hiba", false);
      diag.iconType = VSIcons.ERROR;
      diag.tooltip = "Fordítási hibák";
      StatusBar.Cell steps = statusBar.add("steps", "", false);
      steps.tooltip = "Végrehajtott lépések";
      msgCell = statusBar.add("msg", "", false);
      msgCell.tooltip = "Üzenet";

      StatusBar.Cell pos = statusBar.add("pos", "Sor 1, Oszlop 1", true);
      pos.tooltip = "A kurzor helye";
      StatusBar.Cell enc = statusBar.add("enc", "ISO-8859-2", true);
      enc.tooltip = "Fájl kódolása";
      StatusBar.Cell lang = statusBar.add("lang", "PLanG", true);
      lang.tooltip = "Nyelv";
      StatusBar.Cell theme = statusBar.add("theme", "Sötét téma", true);
      theme.tooltip = "Téma váltása";
      theme.action = new Runnable() {
         public void run() { toggleThemeAction.actionPerformed(null); }
      };

      root.add(body, BorderLayout.CENTER);
      root.add(statusBar, BorderLayout.SOUTH);

      add(root, BorderLayout.CENTER);
   }

   public javax.swing.JMenuBar getMenuBar() { return menuBar; }
   public void setExitHandler(Runnable r) { this.exitHandler = r; }
   public boolean hasUnsavedChanges() { return progTextChanged; }
   public File getCurrentFile() { return currentFile; }

   private void setFrameTitle(String t) {
      if (owner != null) { owner.setTitle(t); }
   }

   /* ---- .plang kiterjesztés ---- */
   private File ensurePlangExtension(File f) {
      if (f == null) return null;
      String name = f.getName();
      int dot = name.lastIndexOf('.');
      if (dot < 0) {
         return new File(f.getParentFile(), name + ".plang");
      }
      if (dot == name.length() - 1) {
         return new File(f.getParentFile(), name + "plang");
      }
      // ha nincs kiterjesztésnek tekinthető rész (pl. nincs pont), már kezeltük
      // ha van pont, de a kiterjesztés üres, szintén hozzáadjuk – egyébként marad
      return f;
   }

   /* ---- mentés visszajelzés ---- */
   private void showSaveFeedback(String fileName) {
      showTransientMessage("Mentve: " + fileName);
   }

   /** Rövid ideig látszó üzenet az állapotsorban. */
   private void showTransientMessage(String text) {
      if (statusBar == null || msgCell == null) return;
      statusBar.setText("msg", text);
      if (msgClearTimer != null) {
         msgClearTimer.stop();
      }
      msgClearTimer = new Timer(3500, new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            statusBar.setText("msg", "");
         }
      });
      msgClearTimer.setRepeats(false);
      msgClearTimer.start();
   }

   /* ---- legutóbbi fájlok ---- */
   private void loadRecentFiles() {
      try {
         recentFiles = AppPrefs.getRecentFiles();
         if (recentFiles == null) recentFiles = new ArrayList<File>();
      } catch (Exception e) {
         recentFiles = new ArrayList<File>();
      }
   }

   private void saveRecentFiles() {
      try {
         AppPrefs.setRecentFiles(recentFiles);
         AppPrefs.flush();
      } catch (Exception e) {}
   }

   private void addRecentFile(File f) {
      if (f == null) return;
      File abs = f.getAbsoluteFile();
      Iterator<File> it = recentFiles.iterator();
      while (it.hasNext()) {
         File ex = it.next();
         if (ex.getAbsolutePath().equals(abs.getAbsolutePath())) {
            it.remove();
         }
      }
      recentFiles.add(0, abs);
      while (recentFiles.size() > 8) {
         recentFiles.remove(recentFiles.size() - 1);
      }
      saveRecentFiles();
      updateRecentMenu();
   }

   private void updateRecentMenu() {
      if (recentMenu == null) return;
      recentMenu.removeAll();
      if (recentFiles.isEmpty()) {
         JMenuItem empty = new JMenuItem("(nincs legutóbbi fájl)");
         empty.setEnabled(false);
         recentMenu.add(empty);
      } else {
         for (int i = 0; i < recentFiles.size(); i++) {
            final File file = recentFiles.get(i);
            JMenuItem item = new JMenuItem((i + 1) + ". " + file.getName());
            item.setToolTipText(file.getAbsolutePath());
            item.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                  if (!file.exists()) {
                     JOptionPane.showMessageDialog(Workbench.this,
                        "A fájl már nem létezik: " + file.getAbsolutePath(),
                        "Hiba", JOptionPane.ERROR_MESSAGE);
                     recentFiles.remove(file);
                     saveRecentFiles();
                     updateRecentMenu();
                     return;
                  }
                  if (!progTextChanged
                      || JOptionPane.showConfirmDialog(Workbench.this,
                            "A programszöveg változásai nincsenek elmentve. Biztosan be akarsz tölteni egy új fájlt?",
                            "Betöltés", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == 0) {
                     loadFile(file);
                  }
               }
            });
            recentMenu.add(item);
         }
         recentMenu.addSeparator();
         JMenuItem clear = new JMenuItem("Lista törlése");
         clear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               recentFiles.clear();
               saveRecentFiles();
               updateRecentMenu();
            }
         });
         recentMenu.add(clear);
      }
   }

   private void updateUndoRedo() {
      if (progText == null) return;
      boolean canUndo = progText.getUndoManager().canUndo();
      boolean canRedo = progText.getUndoManager().canRedo();
      if (undoAction != null) undoAction.setEnabled(canUndo);
      if (redoAction != null) redoAction.setEnabled(canRedo);
   }

   /* ---- fájlműveletek ---- */

   /**
    * Betölti a megadott fájlt a szerkesztőbe.
    * Publikus, hogy parancssorból és tesztekből is hívható legyen.
    */
   public void openFile(File f) {
      loadFile(f);
   }

   private void loadFile(File f) {
      if (f == null) return;
      BufferedReader rd = null;
      try {
         rd = new BufferedReader(new InputStreamReader(new FileInputStream(f), "ISO-8859-2"));
         StringBuffer sb = new StringBuffer();
         for (String line = rd.readLine(); line != null; line = rd.readLine()) {
            sb.append(line).append("\n");
         }
         this.progText.setText(sb.toString());
         this.progText.discardUndoHistory();
         this.progText.setCaretPosition(0);
         this.progTextChanged = false;
         this.currentFile = f;
         this.editorTabs.setTitle("editor", f.getName());
         this.editorTabs.setDirty("editor", false);
         this.setFrameTitle(f.getName() + " – PLanG");
         this.progCards.show(this.progPanel, PROGTEXT);
         this.editorTabs.select("editor");
         this.refreshOutline();
         this.updateStatus();
         this.updateUndoRedo();
         this.addRecentFile(f);
      } catch (FileNotFoundException e) {
         JOptionPane.showMessageDialog(this,
            "Nem sikerült az olvasás a következö fájlból: " + e.getMessage(),
            "Hiba a megnyitás során", JOptionPane.ERROR_MESSAGE);
         System.err.println(e.getMessage());
      } catch (IOException e) {
         JOptionPane.showMessageDialog(this,
            "Nem sikerült az olvasás a következő fájlból: " + e.getMessage(),
            "Hiba az olvasás során", JOptionPane.ERROR_MESSAGE);
         System.err.println(e.getMessage());
      } finally {
         if (rd != null) {
            try { rd.close(); } catch (IOException e) {}
         }
      }
   }

   private void saveFile(File f) {
      if (f == null) return;
      File target = ensurePlangExtension(f);
      try {
         PrintWriter wr = new PrintWriter(
            new OutputStreamWriter(new FileOutputStream(target), "ISO-8859-2"));
         wr.print(this.progText.getText());
         wr.close();
         this.progTextChanged = false;
         this.currentFile = target;
         this.editorTabs.setTitle("editor", target.getName());
         this.editorTabs.setDirty("editor", false);
         this.setFrameTitle(target.getName() + " – PLanG");
         this.updateStatus();
         this.addRecentFile(target);
         this.showSaveFeedback(target.getName());
      } catch (IOException e) {
         JOptionPane.showMessageDialog(this,
            "Nem sikerült a mentés a következő fájlba: " + e.getMessage(),
            "Hiba a mentés során", JOptionPane.ERROR_MESSAGE);
         System.err.println(e.getMessage());
      }
   }

   public void savePrefs() {
      try {
         if (textFont != null) {
            AppPrefs.setFontFamily(textFont.getFamily());
            AppPrefs.setFontSize(textFont.getSize());
         }
         if (callStack != null && callStack.getModel() instanceof CallStack) {
            AppPrefs.setStepNum(((CallStack) callStack.getModel()).getMaxSteps());
         }
         AppPrefs.setThemeMode(Theme.mode());
         if (progText != null) {
            AppPrefs.setIndentGuides(progText.isShowIndentGuides());
         }
         AppPrefs.setAutoStop(autoStop);
         int mainDiv = -1, centerDiv = -1, rightDiv = -1, inspectDiv = -1, consoleDiv = -1;
         if (mainSplit != null) mainDiv = mainSplit.getDividerLocation();
         if (centerSplit != null) centerDiv = centerSplit.getDividerLocation();
         if (rightSplit != null) rightDiv = rightSplit.getDividerLocation();
         if (inspectSplit != null) inspectDiv = inspectSplit.getDividerLocation();
         if (consoleSplit != null) consoleDiv = consoleSplit.getDividerLocation();
         AppPrefs.setDividers(mainDiv, centerDiv, rightDiv, inspectDiv, consoleDiv);
         AppPrefs.setRecentFiles(recentFiles);
         AppPrefs.flush();
      } catch (Exception e) {}
   }

   private static int shortcutMask() {
      try {
         return Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
      } catch (Throwable t) {
         return java.awt.event.InputEvent.CTRL_MASK;
      }
   }

   private PrefDialog prefs() {
      if (this.prefDialog == null) {
         this.prefDialog = new PrefDialog(owner);
      }
      return this.prefDialog;
   }

   private boolean subProgramsEnabled() {
      return "on".equals(System.getProperty("hu.ppke.itk.plang.subprograms"));
   }

   private JPanel buildExplorerPanel() {
      JPanel p = new JPanel(new BorderLayout());
      p.setBackground(Theme.p().sideBar);
      PanelHeader head = new PanelHeader("Kezelő");
      explorerHeader = head;
      JPanel content = new JPanel(new BorderLayout());
      content.setBackground(Theme.p().sideBar);
      JPanel actions = new JPanel(new GridLayout(0, 1, 0, 4));
      actions.setBackground(Theme.p().sideBar);
      actions.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));
      actions.add(sideButton(newAction, VSIcons.NEW, "Új program"));
      actions.add(sideButton(loadAction, VSIcons.OPEN, "Megnyitás…"));
      actions.add(sideButton(saveAction, VSIcons.SAVE, "Mentés…"));
      PanelHeader outlineHead = new PanelHeader("Vázlat", VSIcons.TREE, null);
      outline = new OutlineList();
      outlineScrl = new JScrollPane(outline);
      outlineScrl.setBorder(BorderFactory.createEmptyBorder());
      FlatScrollBarUI.install(outlineScrl);
      outlineScrl.getViewport().setBackground(Theme.p().sideBar);
      outlinePanel = new JPanel(new BorderLayout());
      outlinePanel.setBackground(Theme.p().sideBar);
      outlinePanel.add(outlineHead, BorderLayout.NORTH);
      outlinePanel.add(outlineScrl, BorderLayout.CENTER);
      content.add(actions, BorderLayout.NORTH);
      content.add(outlinePanel, BorderLayout.CENTER);
      p.add(head, BorderLayout.NORTH);
      p.add(content, BorderLayout.CENTER);
      return p;
   }

   private JPanel buildRunPanel() {
      JPanel p = new JPanel(new BorderLayout());
      p.setBackground(Theme.p().sideBar);
      p.add(new PanelHeader("Futtatás és hibakeresés"), BorderLayout.NORTH);
      JPanel content = new JPanel();
      content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
      content.setBackground(Theme.p().sideBar);
      content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      FlatButton runBig = new FlatButton(FlatButton.PRIMARY, "Program futtatása");
      runBig.setIcon(VSIcons.icon(VSIcons.PLAY, 15, Theme.p().buttonFg));
      runBig.setAction(runAction);
      runBig.setPadding(12, 7);
      runBig.setAlignmentX(Component.LEFT_ALIGNMENT);
      runBig.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
      FlatButton parseBtn = sideButton(parseAction, VSIcons.PARSE, "Értelmezés");
      FlatButton stopBtn = sideButton(stopAction, VSIcons.STOP, "Futtatás vége");
      FlatButton editBtn = sideButton(editAction, VSIcons.EDIT, "Szerkesztés");
      FlatButton copyBtn = sideButton(copyAction, VSIcons.COPY, "Értelmezett átmásolása");
      content.add(runBig);
      content.add(javax.swing.Box.createVerticalStrut(8));
      content.add(parseBtn);
      content.add(javax.swing.Box.createVerticalStrut(4));
      content.add(stopBtn);
      content.add(javax.swing.Box.createVerticalStrut(12));
      content.add(editBtn);
      content.add(javax.swing.Box.createVerticalStrut(4));
      content.add(copyBtn);
      if (subProgramsEnabled()) {
         content.add(javax.swing.Box.createVerticalStrut(12));
         content.add(sideButton(enterAction, VSIcons.STEP_INTO, "Belépés alprogramba"));
         content.add(javax.swing.Box.createVerticalStrut(4));
         content.add(sideButton(leaveAction, VSIcons.STEP_OUT, "Alprogram elhagyása"));
      }
      content.add(javax.swing.Box.createVerticalGlue());
      JPanel wrap = new JPanel(new BorderLayout());
      wrap.setBackground(Theme.p().sideBar);
      wrap.add(content, BorderLayout.NORTH);
      p.add(wrap, BorderLayout.CENTER);
      return p;
   }

   private JPanel buildSearchPanel() {
      JPanel p = new JPanel(new BorderLayout());
      p.setBackground(Theme.p().sideBar);
      p.add(new PanelHeader("Keresés"), BorderLayout.NORTH);
      JPanel content = new JPanel(new BorderLayout());
      content.setBackground(Theme.p().sideBar);
      content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      JLabel hint = new JLabel("<html><body style='width:170px'>"
         + "A kereséshez nyomd meg a <b>Ctrl+F</b> billentyűt, "
         + "vagy használd a szerkesztő fölött megjelenő keresősávot.</body></html>");
      hint.setForeground(Theme.p().sideBarFg);
      hint.setFont(Theme.uiPlain());
      FlatButton open = new FlatButton(FlatButton.SECONDARY, "Keresősáv megnyitása");
      open.setIcon(VSIcons.icon(VSIcons.SEARCH, 14, Theme.p().buttonSecondaryFg));
      open.setAction(findAction);
      JPanel box = new JPanel(new BorderLayout(0, 10));
      box.setBackground(Theme.p().sideBar);
      box.add(hint, BorderLayout.NORTH);
      box.add(open, BorderLayout.CENTER);
      content.add(box, BorderLayout.NORTH);
      p.add(content, BorderLayout.CENTER);
      return p;
   }

   private FlatButton sideButton(Action a, int icon, String label) {
      FlatButton b = new FlatButton(FlatButton.SECONDARY, label);
      b.setIcon(VSIcons.icon(icon, 14, Theme.p().buttonSecondaryFg));
      b.setAction(a);
      b.setText(label);
      b.setPadding(10, 6);
      b.setAlignmentX(Component.LEFT_ALIGNMENT);
      b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
      return b;
   }

   private JPanel wrapStreamPanel(StreamTabs tabs, String title, int icon) {
      JPanel p = new JPanel(new BorderLayout());
      p.setBackground(Theme.p().panelBg);
      PanelHeader h = new PanelHeader(title, icon,
         icon == VSIcons.INPUT ? Theme.p().info : Theme.p().success);
      p.add(h, BorderLayout.NORTH);
      p.add(tabs, BorderLayout.CENTER);
      return p;
   }

   /* ------------------------- menü, gyorsbillentyűk ------------------------- */

   private void buildMenu() {
      javax.swing.JMenuBar mb = new javax.swing.JMenuBar();
      mb.setBackground(Theme.p().titleBar);
      mb.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.p().border));

      JMenu file = new JMenu("Fájl");
      file.add(menuItem(newAction, KeyEvent.VK_N));
      file.add(menuItem(loadAction, KeyEvent.VK_O));
      file.add(menuItem(saveAction, KeyEvent.VK_S));
      JMenuItem saveAsItem = menuItem(saveAsAction, 0);
      saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
            shortcutMask() | InputEvent.SHIFT_DOWN_MASK));
      file.add(saveAsItem);
      recentMenu = new JMenu("Legutóbbi fájlok");
      file.add(recentMenu);
      file.addSeparator();
      JMenuItem exit = new JMenuItem("Kilépés");
      exit.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            if (exitHandler != null) exitHandler.run();
         }
      });
      file.add(exit);

      JMenu edit = new JMenu("Szerkesztés");
      edit.add(menuItem(undoAction, KeyEvent.VK_Z));
      JMenuItem redoItem = menuItem(redoAction, KeyEvent.VK_Y);
      // Ctrl+Shift+Z is is redo
      edit.add(redoItem);
      edit.addSeparator();
      edit.add(menuItem(findAction, KeyEvent.VK_F));
      JMenuItem replaceItem = menuItem(replaceAction, KeyEvent.VK_H);
      edit.add(replaceItem);
      edit.add(menuItem(gotoLineAction, KeyEvent.VK_G));
      edit.addSeparator();
      JMenuItem comment = new JMenuItem("Megjegyzés ki/be   Ctrl+/");
      comment.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            progText.getActionMap().get("plang-comment").actionPerformed(e);
         }
      });
      edit.add(comment);
      edit.addSeparator();
      edit.add(menuItem(increaseFontAction, KeyEvent.VK_EQUALS));
      edit.add(menuItem(decreaseFontAction, KeyEvent.VK_MINUS));

      JMenu runMenu = new JMenu("Futtatás");
      runMenu.add(menuItem(parseAction, 0));
      runMenu.add(menuItem(runAction, 0));
      runMenu.add(menuItem(stopAction, 0));
      runMenu.addSeparator();
      runMenu.add(menuItem(editAction, 0));
      runMenu.add(menuItem(copyAction, 0));
      if (subProgramsEnabled()) {
         runMenu.addSeparator();
         runMenu.add(menuItem(enterAction, 0));
         runMenu.add(menuItem(leaveAction, 0));
      }

      JMenu view = new JMenu("Nézet");
      view.add(menuItem(toggleThemeAction, 0));
      view.add(menuItem(showPreferences, 0));

      JMenu help = new JMenu("Súgó");
      help.add(menuItem(helpAction, 0));

      mb.add(file);
      mb.add(edit);
      mb.add(runMenu);
      mb.add(view);
      mb.add(help);
      this.menuBar = mb;
   }

   private JMenuItem menuItem(Action a, int key) {
      JMenuItem mi = new JMenuItem(a);
      if (key != 0) {
         mi.setAccelerator(KeyStroke.getKeyStroke(key, shortcutMask()));
      }
      return mi;
   }

   private void installShortcuts() {
      JComponent root = this;
      int mask = shortcutMask();

      bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "run", runAction);
      bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_F5, InputEvent.SHIFT_DOWN_MASK), "stop", stopAction);
      bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_B, mask), "parse", parseAction);
      bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_S, mask), "save", saveAction);
      bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_S, mask | InputEvent.SHIFT_DOWN_MASK), "saveAs", saveAsAction);
      bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_O, mask), "open", loadAction);
      bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_N, mask), "new", newAction);
      bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_F, mask), "find", findAction);
      bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_H, mask), "replace", replaceAction);
      bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_G, mask), "goto", gotoLineAction);
      bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, mask), "prefs", showPreferences);
      bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_Z, mask), "undo", undoAction);
      bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_Y, mask), "redo", redoAction);
      bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_Z, mask | InputEvent.SHIFT_DOWN_MASK), "redo2", redoAction);
      bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, mask), "incFont", increaseFontAction);
      bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, mask), "incFont2", increaseFontAction);
      bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_ADD, mask), "incFont3", increaseFontAction);
      bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, mask), "decFont", decreaseFontAction);
      bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, mask), "decFont2", decreaseFontAction);
      bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "help", helpAction);
      bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "enter", enterAction);
      bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_F11, InputEvent.SHIFT_DOWN_MASK), "leave", leaveAction);
   }

   private void bind(JComponent c, KeyStroke ks, String name, final Action a) {
      c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, name);
      c.getActionMap().put(name, new AbstractAction() {
         private static final long serialVersionUID = 1L;
         public void actionPerformed(ActionEvent e) {
            if (a.isEnabled()) a.actionPerformed(e);
         }
      });
   }

   /* --------------------------- segédműveletek --------------------------- */

   private void expandExprTree() {
      for (int i = 0; i < exprTree.getRowCount(); i++) {
         exprTree.expandRow(i);
      }
   }

   private int errorCount;

   private void markErrors(MainProgram prog) {
      errorCount = 0;
      int firstError = -1;
      ProgramList lst = (ProgramList) progList.getModel();
      for (int i = 0; i < lst.getSize(); i++) {
         ProgramLine pl = lst.getElementAt(i);
         if (pl.hasError()) {
            errorCount++;
            if (firstError < 0) firstError = i;
         }
      }
      progText.setErrorLine(-1);
      if (firstError >= 0) {
         progList.setSelectedIndex(firstError);
         progList.ensureIndexIsVisible(firstError);
      }
   }

   private void autoSizeStateColumns() {
      for (int c = 0; c < stateTable.getColumnCount(); c++) {
         TableColumn col = stateTable.getColumnModel().getColumn(c);
         int w = c == 0 ? 62 : 96;
         java.awt.FontMetrics fm = stateTable.getFontMetrics(textFont);
         int header = stateTable.getFontMetrics(Theme.uiSmallBold())
                                .stringWidth(String.valueOf(stateTable.getColumnName(c))) + 26;
         w = Math.max(w, header);
         for (int r = 0; r < Math.min(stateTable.getRowCount(), 200); r++) {
            Object v = stateTable.getValueAt(r, c);
            if (v != null) {
               String s = ProgLineRenderer.stripHtml(String.valueOf(v))[0];
               w = Math.max(w, fm.stringWidth(s) + 22);
            }
         }
         col.setPreferredWidth(Math.min(w, 260));
         col.setWidth(Math.min(w, 260));
      }
   }

   private void refreshOutline() {
      if (outline != null) outline.rebuild(progText.getText());
   }

   private void updateStatus() {
      if (statusBar == null) return;
      statusBar.setText("pos", "Sor " + progText.caretLine() + ", Oszlop " + progText.caretColumn());
      statusBar.setText("theme", Theme.isDark() ? "Sötét téma" : "Világos téma");
      statusBar.setText("diag", errorCount == 0 ? "Nincs hiba" : (errorCount + " hiba"));
      statusBar.setIcon("diag", errorCount == 0 ? VSIcons.CHECK : VSIcons.ERROR,
                        errorCount == 0 ? null : Theme.p().statusFg);
      if (running) {
         statusBar.setText("run", "Fut – " + lastStepCount + " lépés");
         statusBar.setIcon("run", VSIcons.STOP, null);
         statusBar.setText("steps", "Lépés " + (stateTable.getSelectedRow() + 1)
                                    + " / " + stateTable.getRowCount());
      } else {
         statusBar.setText("run", "Futtatás");
         statusBar.setIcon("run", VSIcons.PLAY, null);
         statusBar.setText("steps", "");
      }
      // msgCell-t nem írjuk felül itt
   }

   private void showHelp() {
      String msg =
         "<html><body style='width: 420px; font-family: sans-serif;'>"
         + "<h2 style='margin-bottom:4px'>PLanG fejlesztőkörnyezet</h2>"
         + "<p style='margin-top:0'>PPKE ITK – programozási alapismeretek</p>"
         + "<h3>Billentyűparancsok</h3>"
         + "<table cellpadding='2'>"
         + "<tr><td><b>Ctrl+N</b></td><td>Új program</td></tr>"
         + "<tr><td><b>Ctrl+O</b></td><td>Megnyitás</td></tr>"
         + "<tr><td><b>Ctrl+S</b></td><td>Mentés</td></tr>"
         + "<tr><td><b>Ctrl+Shift+S</b></td><td>Mentés másként</td></tr>"
         + "<tr><td><b>Ctrl+Z</b></td><td>Visszavonás</td></tr>"
         + "<tr><td><b>Ctrl+Y / Ctrl+Shift+Z</b></td><td>Újra</td></tr>"
         + "<tr><td><b>Ctrl+B</b></td><td>Értelmezés</td></tr>"
         + "<tr><td><b>F5</b></td><td>Futtatás</td></tr>"
         + "<tr><td><b>Shift+F5</b></td><td>Futtatás vége</td></tr>"
         + "<tr><td><b>Ctrl+F</b></td><td>Keresés</td></tr>"
         + "<tr><td><b>Ctrl+H</b></td><td>Csere</td></tr>"
         + "<tr><td><b>Ctrl+G</b></td><td>Ugrás sorra</td></tr>"
         + "<tr><td><b>Ctrl+/</b></td><td>Megjegyzés ki/be</td></tr>"
         + "<tr><td><b>Ctrl+D</b></td><td>Sor megkettőzése</td></tr>"
         + "<tr><td><b>Alt+↑ / Alt+↓</b></td><td>Sor mozgatása</td></tr>"
         + "<tr><td><b>Tab / Shift+Tab</b></td><td>Behúzás növelése / csökkentése</td></tr>"
         + "<tr><td><b>Ctrl+ + / Ctrl+ -</b></td><td>Betűméret</td></tr>"
         + "<tr><td><b>Ctrl+,</b></td><td>Beállítások</td></tr>"
         + "</table>"
         + "<h3>Nyelvi elemek</h3>"
         + "<p><code>PROGRAM … PROGRAM_VÉGE</code>, <code>VÁLTOZÓK:</code>, "
         + "<code>HA … AKKOR … KÜLÖNBEN … HA_VÉGE</code>, "
         + "<code>CIKLUS … AMÍG … CIKLUS_VÉGE</code>, <code>BE:</code>, <code>KI:</code></p>"
         + "<p>Típusok: <code>EGÉSZ, VALÓS, SZÖVEG, KARAKTER, LOGIKAI, BEFÁJL, KIFÁJL</code></p>"
         + "</body></html>";
      JOptionPane.showMessageDialog(this, msg, "Súgó", JOptionPane.INFORMATION_MESSAGE);
   }

   /* ---------------------------- témaváltás ---------------------------- */

   public void applyThemeToAll() {
      Theme.installUIDefaults();
      StreamDocument.refreshStyles();
      Theme.Palette p = Theme.p();

      /* Először a LAF delegáltjait frissítjük (a menü, a fájlválasztó és a
         beállítások ablaka külön ablakfában él, azok nem részei ennek a
         komponensfának), és csak utána állítjuk vissza az egyedi megjelenést.
         Fordított sorrendben az updateComponentTreeUI felülírná a saját UI-kat
         – például a lapos osztópanelekét, amitől világos osztóvonalak
         maradnának sötét módban. */
      try { SwingUtilities.updateComponentTreeUI(this); } catch (Exception e) {}
      if (menuBar != null) {
         try { SwingUtilities.updateComponentTreeUI(menuBar); } catch (Exception e) {}
      }
      if (fileChooser != null) {
         try { SwingUtilities.updateComponentTreeUI(fileChooser); } catch (Exception e) {}
      }
      if (prefDialog != null) {
         try { SwingUtilities.updateComponentTreeUI(prefDialog); } catch (Exception e) {}
      }

      setBackground(p.editorBg);
      if (rootPanel != null) rootPanel.setBackground(p.editorBg);
      if (bodyPanel != null) bodyPanel.setBackground(p.editorBg);
      if (editorAreaPanel != null) editorAreaPanel.setBackground(p.editorBg);
      if (editorWrapPanel != null) editorWrapPanel.setBackground(p.editorBg);
      if (editorHolderPanel != null) editorHolderPanel.setBackground(p.editorBg);
      if (activityBar != null) activityBar.applyTheme();
      if (sideBar != null) sideBar.setBackground(p.sideBar);
      if (editorTabs != null) editorTabs.applyTheme();
      if (statusBar != null) statusBar.applyTheme();
      if (findBar != null) findBar.applyTheme();
      if (progText != null) progText.applyTheme();
      if (gutter != null) gutter.applyTheme();
      if (progPanel != null) progPanel.setBackground(p.editorBg);
      if (progList != null) progList.setBackground(p.editorBg);
      if (explorerHeader != null) explorerHeader.applyTheme();
      if (varsHeader != null) varsHeader.applyTheme();
      if (exprHeader != null) exprHeader.applyTheme();
      if (stackHeader != null) stackHeader.applyTheme();
      if (outlinePanel != null) outlinePanel.setBackground(p.sideBar);
      if (outline != null) outline.setBackground(p.sideBar);
      if (outlineScrl != null) {
         outlineScrl.getViewport().setBackground(p.sideBar);
         outlineScrl.setBackground(p.sideBar);
         outlineScrl.setBorder(BorderFactory.createEmptyBorder());
      }
      if (varsPanel != null) varsPanel.setBackground(p.panelBg);
      if (exprPanel != null) exprPanel.setBackground(p.panelBg);
      if (inspectorPanel != null) inspectorPanel.setBackground(p.panelBg);
      if (callStackBox != null) callStackBox.setBackground(p.panelBg);
      if (inpWrap != null) inpWrap.setBackground(p.panelBg);
      if (outWrap != null) outWrap.setBackground(p.panelBg);
      if (exprTree != null) exprTree.setBackground(p.panelBg);
      if (stateTable != null) {
         stateTable.setBackground(p.panelBg);
         stateTable.getTableHeader().setBackground(p.tableHeaderBg);
      }
      if (callStack != null) callStack.setBackground(p.panelBg);
      if (tableScrl != null) {
         tableScrl.getViewport().setBackground(p.panelBg);
         tableScrl.setBackground(p.panelBg);
         tableScrl.getColumnHeader().setBackground(p.tableHeaderBg);
         tableScrl.setBorder(BorderFactory.createEmptyBorder());
      }
      if (exprScrl != null) {
         exprScrl.getViewport().setBackground(p.panelBg);
         exprScrl.setBackground(p.panelBg);
         exprScrl.setBorder(BorderFactory.createEmptyBorder());
      }
      if (csScroll != null) {
         csScroll.getViewport().setBackground(p.panelBg);
         csScroll.setBackground(p.panelBg);
         csScroll.setBorder(BorderFactory.createEmptyBorder());
      }
      if (editorScroll != null) {
         editorScroll.getViewport().setBackground(p.editorBg);
         editorScroll.getRowHeader().setBackground(p.editorBg);
         editorScroll.setBackground(p.editorBg);
         editorScroll.setBorder(BorderFactory.createEmptyBorder());
      }
      if (listScroll != null) {
         listScroll.getViewport().setBackground(p.editorBg);
         listScroll.setBackground(p.editorBg);
         listScroll.setBorder(BorderFactory.createEmptyBorder());
      }
      /* Az osztópanelek egyedi UI-ja az updateComponentTreeUI alatt elveszett,
         itt kapják vissza. */
      if (mainSplit != null) mainSplit.applyTheme();
      if (rightSplit != null) rightSplit.applyTheme();
      if (centerSplit != null) centerSplit.applyTheme();
      if (inspectSplit != null) inspectSplit.applyTheme();
      if (consoleSplit != null) consoleSplit.applyTheme();
      if (inpPanes != null) inpPanes.applyTheme();
      if (outPanes != null) outPanes.applyTheme();
      reinstallCustomUI();
      /* Végül azok a panelek, amelyekre nincs direkt hivatkozásunk: ha a
         háttérszín a másik témából maradt itt, a panel szerepe szerint
         javítjuk. */
      try { fixAllPanelBackgrounds(this, p); } catch (Exception e) {}
      repaint();
   }

   /**
    * Végigjárja a komponensfát, és azokat a paneleket, amelyek a másik témából
    * maradt háttérszínt őriznek, az aktuális palettához igazítja. A panel
    * szerepét a szülői lánc alapján döntjük el.
    */
   private void fixAllPanelBackgrounds(Container cont, Theme.Palette pal) {
      Component[] kids = cont.getComponents();
      for (int i = 0; i < kids.length; i++) {
         Component c = kids[i];
         if (c instanceof PanelHeader) {
            /* A szekciócímek (köztük a Bemenet/Kimenet paneleké, amelyekre
               nincs külön mezőnk) a LAF-csere után visszakapják a gyári
               hátteret, ezért újra kell színezni őket. */
            ((PanelHeader) c).applyTheme();
         } else if (c instanceof JPanel) {
            Color bg = c.getBackground();
            if (bg != null) {
               int avg = (bg.getRed() + bg.getGreen() + bg.getBlue()) / 3;
               if (!Theme.isDark()) {
                  if (avg < 60) {
                     if (isDescendantOf(c, progPanel) || isDescendantOf(c, editorAreaPanel)
                         || isDescendantOf(c, editorHolderPanel)
                         || isDescendantOf(c, editorWrapPanel)) {
                        c.setBackground(pal.editorBg);
                     } else if (isDescendantOf(c, varsPanel) || isDescendantOf(c, exprPanel)
                                || isDescendantOf(c, inspectorPanel)
                                || isDescendantOf(c, callStackBox) || isDescendantOf(c, inpWrap)
                                || isDescendantOf(c, outWrap)) {
                        c.setBackground(pal.panelBg);
                     } else {
                        c.setBackground(pal.sideBar);
                     }
                  }
               } else {
                  if (avg > 220) {
                     if (isDescendantOf(c, progPanel) || isDescendantOf(c, editorAreaPanel)) {
                        c.setBackground(pal.editorBg);
                     } else if (isDescendantOf(c, varsPanel) || isDescendantOf(c, exprPanel)) {
                        c.setBackground(pal.panelBg);
                     } else {
                        c.setBackground(pal.sideBar);
                     }
                  }
               }
            }
         } else if (c instanceof JScrollPane) {
            ((JScrollPane) c).setBorder(BorderFactory.createEmptyBorder());
         }
         if (c instanceof Container) {
            fixAllPanelBackgrounds((Container) c, pal);
         }
      }
   }

   private boolean isDescendantOf(Component child, Container ancestor) {
      if (ancestor == null || child == null) return false;
      Container p = child.getParent();
      while (p != null) {
         if (p == ancestor) return true;
         p = p.getParent();
      }
      return false;
   }

   private void reinstallCustomUI() {
      Theme.Palette p = Theme.p();
      if (editorScroll != null) FlatScrollBarUI.install(editorScroll);
      if (listScroll != null) FlatScrollBarUI.install(listScroll);
      if (tableScrl != null) FlatScrollBarUI.install(tableScrl);
      if (exprScrl != null) FlatScrollBarUI.install(exprScrl);
      if (csScroll != null) FlatScrollBarUI.install(csScroll);
      if (outlineScrl != null) FlatScrollBarUI.install(outlineScrl);
      if (progText != null) {
         progText.applyTheme();
         progText.setEditorFont(textFont);
      }
      if (gutter != null) gutter.applyTheme();
      if (inpPanes != null) inpPanes.applyTheme();
      if (outPanes != null) outPanes.applyTheme();
      if (stateTable != null) {
         stateTable.getTableHeader().setDefaultRenderer(new HeaderRenderer());
         stateTable.setDefaultRenderer(Object.class, stateRenderer);
         stateTable.setShowGrid(false);
         stateTable.setIntercellSpacing(new Dimension(0, 0));
      }
      if (callStack != null) callStack.setCellRenderer(new CallStackRenderer());
      if (progList != null) progList.setCellRenderer(progRenderer);
      if (exprTree != null) exprTree.setCellRenderer(exprRenderer);
      if (menuBar != null) {
         menuBar.setBackground(p.titleBar);
         menuBar.setBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, p.border));
      }
      if (fileChooser != null) fileChooser.setBackground(p.sideBar);
   }

   /* ------------------------- belső segédosztályok ------------------------- */

   private final class HeaderRenderer extends JComponent
         implements javax.swing.table.TableCellRenderer {
      private static final long serialVersionUID = 1L;
      private String text = "";
      public Component getTableCellRendererComponent(JTable table, Object value,
                                                     boolean isSelected, boolean hasFocus,
                                                     int row, int column) {
         this.text = value == null ? "" : String.valueOf(value);
         return this;
      }
      public Dimension getPreferredSize() { return new Dimension(60, 26); }
      protected void paintComponent(Graphics g) {
         Graphics2D g2 = (Graphics2D) g.create();
         g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         Theme.Palette p = Theme.p();
         g2.setColor(p.tableHeaderBg);
         g2.fillRect(0, 0, getWidth(), getHeight());
         g2.setColor(p.border);
         g2.drawLine(getWidth() - 1, 4, getWidth() - 1, getHeight() - 4);
         g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
         g2.setFont(Theme.uiSmallBold());
         java.awt.FontMetrics fm = g2.getFontMetrics();
         g2.setColor(p.sideBarTitleFg);
         String t = text;
         if (fm.stringWidth(t) > getWidth() - 10) {
            while (t.length() > 1 && fm.stringWidth(t + "…") > getWidth() - 10) {
               t = t.substring(0, t.length() - 1);
            }
            t = t + "…";
         }
         g2.drawString(t, 6, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
         g2.dispose();
      }
   }

   private final class CallStackRenderer extends JComponent
         implements javax.swing.ListCellRenderer {
      private static final long serialVersionUID = 1L;
      private String text = "";
      private boolean sel;
      private boolean top;
      public Component getListCellRendererComponent(JList list, Object value, int index,
                                                    boolean isSelected, boolean cellHasFocus) {
         this.text = ProgLineRenderer.stripHtml(String.valueOf(value))[0];
         this.sel = isSelected;
         this.top = (index == list.getModel().getSize() - 1);
         return this;
      }
      public Dimension getPreferredSize() { return new Dimension(120, 22); }
      protected void paintComponent(Graphics g) {
         Graphics2D g2 = (Graphics2D) g.create();
         g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         Theme.Palette p = Theme.p();
         g2.setColor(sel ? p.listSelection : p.panelBg);
         g2.fillRect(0, 0, getWidth(), getHeight());
         if (top) {
            VSIcons.icon(VSIcons.CHEVRON_RIGHT, 14, p.warning).paintIcon(this, g2, 4, 4);
         }
         g2.setFont(Theme.uiPlain());
         java.awt.FontMetrics fm = g2.getFontMetrics();
         g2.setColor(sel ? p.listSelectionFg : (top ? p.editorFg : p.gutterFg));
         g2.drawString(text, 22, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
         g2.dispose();
      }
   }

   private final class OutlineList extends JList {
      private static final long serialVersionUID = 1L;
      private final javax.swing.DefaultListModel model = new javax.swing.DefaultListModel();
      OutlineList() {
         setModel(model);
         setBackground(Theme.p().sideBar);
         setFixedCellHeight(22);
         setCellRenderer(new javax.swing.ListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index,
                                                          boolean isSelected, boolean focus) {
               return new OutlineCell((Object[]) value, isSelected);
            }
         });
         addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
               int i = getSelectedIndex();
               if (i >= 0) {
                  Object[] row = (Object[]) model.get(i);
                  int line = ((Integer) row[2]).intValue();
                  progCards.show(progPanel, PROGTEXT);
                  editorTabs.select("editor");
                  progText.gotoLine(line);
                  progText.requestFocusInWindow();
               }
            }
         });
      }
      void rebuild(String src) {
         model.clear();
         if (src == null) return;
         String[] lines = src.split("\n", -1);
         for (int i = 0; i < lines.length; i++) {
            String t = lines[i].trim();
            String low = hu.ppke.itk.plang.gui.editor.PlangSyntax.deacc(t);
            if (low.startsWith("program ")) {
               model.addElement(new Object[] { t.substring(8).trim(),
                                               Integer.valueOf(VSIcons.NEW), Integer.valueOf(i) });
            } else if (low.startsWith("eljaras ")) {
               model.addElement(new Object[] { t.substring(8).trim(),
                                               Integer.valueOf(VSIcons.RUN), Integer.valueOf(i) });
            } else if (low.startsWith("fuggveny ")) {
               model.addElement(new Object[] { t.substring(9).trim(),
                                               Integer.valueOf(VSIcons.PARSE), Integer.valueOf(i) });
            } else if (low.startsWith("valtozok")) {
               model.addElement(new Object[] { "Változók",
                                               Integer.valueOf(VSIcons.VARIABLES), Integer.valueOf(i) });
            }
         }
         revalidate();
         repaint();
      }
   }

   private static final class OutlineCell extends JComponent {
      private static final long serialVersionUID = 1L;
      private final Object[] row;
      private final boolean sel;
      OutlineCell(Object[] row, boolean sel) {
         this.row = row;
         this.sel = sel;
      }
      public Dimension getPreferredSize() { return new Dimension(150, 22); }
      protected void paintComponent(Graphics g) {
         Graphics2D g2 = (Graphics2D) g.create();
         g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         Theme.Palette p = Theme.p();
         g2.setColor(sel ? p.listSelection : p.sideBar);
         g2.fillRect(0, 0, getWidth(), getHeight());
         int icon = ((Integer) row[1]).intValue();
         VSIcons.icon(icon, 14, sel ? p.listSelectionFg : p.synFunction)
                .paintIcon(this, g2, 8, (getHeight() - 14) / 2);
         g2.setFont(Theme.uiPlain());
         java.awt.FontMetrics fm = g2.getFontMetrics();
         g2.setColor(sel ? p.listSelectionFg : p.sideBarFg);
         g2.drawString(String.valueOf(row[0]), 28,
                       (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
         g2.dispose();
      }
   }

   private class PlangFilter extends FileFilter {
      private PlangFilter() {}
      public boolean accept(File f) {
         if (f.isDirectory()) return true;
         String n = f.getName();
         int dot = n.lastIndexOf('.');
         if (dot < 0) return false;
         return n.substring(dot + 1).toLowerCase().equals("plang");
      }
      public String getDescription() { return "Plang programok (*.plang)"; }
      PlangFilter(PlangFilter var2) { this(); }
   }
}
