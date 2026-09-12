package hu.ppke.itk.plang.android;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import hu.ppke.itk.plang.gui.ExprNode;
import hu.ppke.itk.plang.gui.GuiBridge;
import hu.ppke.itk.plang.gui.ProgramLine;
import hu.ppke.itk.plang.prog.Lexer;
import hu.ppke.itk.plang.prog.MainProgram;
import hu.ppke.itk.plang.prog.State;
import hu.ppke.itk.plang.prog.StreamData;
import hu.ppke.itk.plang.prog.StreamKind;

/**
 * A PLanG fejlesztőkörnyezet munkafelülete Androidon – a asztali
 * gui.Workbench teljes portja: tevékenységsáv, oldalsáv (Kezelő,
 * Futtatás és hibakeresés, Keresés), szerkesztő fül-sorral, be-/kimeneti
 * csatornák, vizsgálópanel (állapottábla, kifejezésfa, hívási verem),
 * állapotsor. Ugyanazokkal az állapotátmenetekkel és műveletekkel.
 */
public class IdeView extends LinearLayout {

    /** A gazda-felület (fájl-választók, üzenetek). */
    public interface Host {
        void launchOpen();
        void launchCreate(String suggestedName);
        void onThemeChanged();
    }

    private final Host host;
    private final Handler handler = new Handler();

    /* ---- komponensek ---- */
    private ActivityBar activityBar;
    private FrameLayout sideBar;
    private LinearLayout explorerPanel;
    private LinearLayout runPanel;
    private LinearLayout searchPanel;
    private int sideWidth;
    private boolean sideOpen = true;

    private EditorTabBar editorTabs;
    private FrameLayout progPanel;
    private LinearLayout editorHolder;
    private CodeEditorView progText;
    private GutterView gutter;
    private FindBarView findBar;
    private ProgramListView progList;

    private StreamPanesView inpPanes;
    private StreamPanesView outPanes;
    private LinearLayout consoleRow;
    private LinearLayout editorArea;

    private LinearLayout inspector;
    private int inspectorWidth;
    private boolean inspectorOpen = true;
    private LinearLayout callStackBox;
    private StateTableView stateTable;
    private ExprTreeView exprTree;
    private CallStackView callStack;
    private Button enterBtn;
    private Button leaveBtn;

    private OutlineView outline;
    private StatusBarView statusBar;

    /* ---- állapot (a Workbench mezői) ---- */
    private MainProgram parsedProgram;
    private Uri currentFile;
    private String currentFileName = "névtelen.plang";
    private boolean progTextChanged = false;
    private boolean running = false;
    private int lastStepCount = 0;
    private boolean backToEditor = false;

    /* ---- hibák ---- */
    private int errorCount;
    private final List<Object[]> errorList = new ArrayList<Object[]>();
    private int errorCursor = -1;
    private int[] parsedToSource;

    /* ---- üzenet-időzítő ---- */
    private Runnable msgClear;

    /* ---- gombok, amelyeket az állapotátmenetek engedélyeznek/tiltanak ---- */
    private Button runBigBtn, parseBtn, stopBtn, editBtn, copyBtn, saveBtn;
    private Button stepBtn, continueBtn, breakpointBtn;
    /* a kifejezésfa fejlécének ikon-gombjai (alprogramba lépés/kilépés) */
    private Button headerEnterBtn, headerLeaveBtn;
    /* a konzol- és vizsgálópanelek (téma váltásnál újraszínezve) */
    private LinearLayout inputWrap, outputWrap, varsPanelBox, exprPanelBox;
    private TextView searchHint;
    private final java.util.List<PanelHeader> headers = new ArrayList<PanelHeader>();
    /* (gomb, stílus, ikon) hármasok: téma váltásnál újra stílust kapnak */
    private final java.util.List<Object[]> styledButtons = new ArrayList<Object[]>();
    /* az elválasztóvonalak (téma váltásnál átszínezve) */
    private final java.util.List<View> dividers = new ArrayList<View>();

    /** Látható elválasztó a panelek között (1dp vonal). */
    private View divider(Context ctx, boolean vertical) {
        View d = new View(ctx);
        d.setBackgroundColor(Theme.p().border);
        dividers.add(d);
        return d;
    }

    public IdeView(Context ctx, Host host) {
        super(ctx);
        this.host = host;
        setOrientation(VERTICAL);

        sideWidth = FlatButton.dp(ctx, 210);
        inspectorWidth = FlatButton.dp(ctx, 250);

        try {
            backToEditor = AppPrefs.getAutoStop();
        } catch (Exception e) {
            backToEditor = false;
        }

        buildComponents();
        buildLayout();
        applyThemeToAll();
        updateFont();
        editState();
        updateStatus();
    }

    /* ==================== felépítés ==================== */

    private void buildComponents() {
        Context ctx = getContext();

        progText = new CodeEditorView(ctx);
        progText.setChangeListener(new CodeEditorView.TextChangeListener() {
            @Override
            public void onTextChanged() {
                progTextChanged = true;
                editorTabs.setDirty("editor", true);
                clearErrorMarks();
                refreshOutline();
                updateStatus();
                updateUndoRedo();
            }
        });

        gutter = new GutterView(ctx, progText);
        progText.setGutterView(gutter);

        progList = new ProgramListView(ctx);
        progList.setListener(new ProgramListView.ItemSelectListener() {
            @Override
            public void onSelected(int index) {
                ProgramLine line = progList.getAt(index);
                exprTree.setRoot(line == null ? null : line.getExpr(null));
                exprTree.expandAll();
            }

            @Override
            public void onDoubleTap(int index) {
                doEdit();
                progText.gotoLine(sourceLineForParsedIndex(index));
                progText.requestFocus();
            }
        });

        stateTable = new StateTableView(ctx);
        exprTree = new ExprTreeView(ctx);
        callStack = new CallStackView(ctx, stateTable, AppPrefs.getStepNum());

        inpPanes = new StreamPanesView(ctx, StreamKind.INPUT);
        outPanes = new StreamPanesView(ctx, StreamKind.OUTPUT);
        outline = new OutlineView(ctx);
        outline.setListener(new OutlineView.GotoListener() {
            @Override
            public void onGoto(int line) {
                showEditor();
                progText.gotoLine(line);
                progText.requestFocus();
            }
        });
        statusBar = new StatusBarView(ctx);

        /* ---- kijelölések szinkronizálása ---- */

        stateTable.setListener(new StateTableView.RowSelectListener() {
            @Override
            public void onRowSelected(int row) {
                onStateSelected();
            }
        });

        exprTree.setListener(new ExprTreeView.NodeSelectListener() {
            @Override
            public void onNodeSelected(ExprNode node) {
                setEnterEnabled(GuiBridge.subStates(node) != null);
            }
        });
        exprTree.setEnterListener(new ExprTreeView.EnterListener() {
            @Override
            public void onEnterRequested(ExprNode node) {
                doEnterNode(node);
            }
        });

        editorTabs = new EditorTabBar(ctx);
        editorTabs.addTab(new EditorTabBar.Tab("editor", "névtelen.plang", VsIcons.NEW, false));
        editorTabs.addTab(new EditorTabBar.Tab("parsed", "Értelmezett program", VsIcons.PARSE, false));
        editorTabs.setListener(new EditorTabBar.Listener() {
            @Override
            public void tabSelected(String id) {
                if ("editor".equals(id)) {
                    showEditor();
                } else {
                    showParsed();
                }
                updateStatus();
            }
            @Override
            public void tabClosed(String id) {}
        });

        findBar = new FindBarView(ctx, progText);

        buildStatusBarCells();
    }

    private void showEditor() {
        progPanel.getChildAt(0).setVisibility(VISIBLE);
        progPanel.getChildAt(1).setVisibility(GONE);
        editorTabs.select("editor");
    }

    private void showParsed() {
        progPanel.getChildAt(0).setVisibility(GONE);
        progPanel.getChildAt(1).setVisibility(VISIBLE);
        editorTabs.select("parsed");
    }

    private void buildStatusBarCells() {
        StatusBarView.Cell run = statusBar.add("run", "Futtatás", false);
        run.iconType = VsIcons.PLAY;
        run.action = new Runnable() {
            @Override
            public void run() {
                if (canRun()) {
                    doRun();
                } else if (parseBtn.isEnabled()) {
                    doParse();
                }
            }
        };
        StatusBarView.Cell diag = statusBar.add("diag", "Nincs hiba", false);
        diag.iconType = VsIcons.CHECK;
        diag.action = new Runnable() {
            @Override
            public void run() {
                gotoNextError();
            }
        };
        statusBar.add("steps", "", false);
        statusBar.add("msg", "", false);
        StatusBarView.Cell pos = statusBar.add("pos", "Sor 1, Oszlop 1", true);
        statusBar.add("enc", "ISO-8859-2", true);
        statusBar.add("lang", "PLanG", true);
        StatusBarView.Cell theme = statusBar.add("theme", "Sötét téma", true);
        theme.action = new Runnable() {
            @Override
            public void run() {
                toggleTheme();
            }
        };
    }

    private void buildLayout() {
        Context ctx = getContext();

        /* ---- tevékenységsáv ---- */
        activityBar = new ActivityBar(ctx);
        activityBar.addView(VsIcons.FILES, "Kezelő", new Runnable() {
            @Override
            public void run() {
                switchSide(explorerPanel);
            }
        });
        activityBar.addView(VsIcons.RUN, "Futtatás és hibakeresés", new Runnable() {
            @Override
            public void run() {
                switchSide(runPanel);
            }
        });
        activityBar.addView(VsIcons.SEARCH, "Keresés", new Runnable() {
            @Override
            public void run() {
                switchSide(searchPanel);
                doFind();
            }
        });
        activityBar.addBottomAction(VsIcons.THEME, "Világos / sötét téma", new Runnable() {
            @Override
            public void run() {
                toggleTheme();
            }
        });
        activityBar.addBottomAction(VsIcons.SETTINGS, "Beállítások", new Runnable() {
            @Override
            public void run() {
                showPreferences();
            }
        });
        activityBar.setSelected(0);

        /* ---- oldalsáv ---- */
        sideBar = new FrameLayout(ctx);
        explorerPanel = buildExplorerPanel();
        runPanel = buildRunPanel();
        searchPanel = buildSearchPanel();
        sideBar.addView(explorerPanel);
        sideBar.addView(runPanel);
        runPanel.setVisibility(GONE);
        sideBar.addView(searchPanel);
        searchPanel.setVisibility(GONE);
        LayoutParams sideLp = new LayoutParams(sideWidth, LayoutParams.MATCH_PARENT);
        sideBar.setLayoutParams(sideLp);

        /* ---- szerkesztőterület ---- */
        LinearLayout editorRow = new LinearLayout(ctx);
        editorRow.setOrientation(HORIZONTAL);
        editorRow.addView(gutter, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        editorRow.addView(divider(ctx, true), new LayoutParams(FlatButton.dp(ctx, 1), LayoutParams.MATCH_PARENT));
        editorRow.addView(progText, new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));

        editorHolder = new LinearLayout(ctx);
        editorHolder.setOrientation(VERTICAL);
        editorHolder.addView(findBar, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        findBar.setVisibility(GONE);
        // A szerkesztő vízszintesen maga görget (setHorizontallyScrolling),
        // függőlegesen is belső görgetést használ – így a sorszámsáv
        // mellette marad, és a scrollY-t követi.
        editorHolder.addView(editorRow, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, 0, 1f));

        /* A lista önállóan is görget; ScrollView-ba csomagolva csak az első
           sor renderselődött le, ezért közvetlenül kerül a nézetváltóba. */
        progPanel = new FrameLayout(ctx);
        progPanel.addView(editorHolder);
        progPanel.addView(progList);
        progList.setVisibility(GONE);

        editorArea = new LinearLayout(ctx);
        editorArea.setOrientation(VERTICAL);
        editorArea.addView(editorTabs, new LayoutParams(LayoutParams.MATCH_PARENT, FlatButton.dp(ctx, 34)));
        editorArea.addView(progPanel, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, 0, 1f));

        /* ---- konzolsáv ---- */
        LinearLayout inputWrap = wrapStreamPanel(inpPanes, "Bemenet", VsIcons.INPUT);
        LinearLayout outputWrap = wrapStreamPanel(outPanes, "Kimenet", VsIcons.OUTPUT);
        consoleRow = new LinearLayout(ctx);
        consoleRow.setOrientation(HORIZONTAL);
        consoleRow.addView(inputWrap, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
        consoleRow.addView(divider(ctx, true), new LinearLayout.LayoutParams(FlatButton.dp(ctx, 1), LayoutParams.MATCH_PARENT));
        consoleRow.addView(outputWrap, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));

        LinearLayout centerCol = new LinearLayout(ctx);
        centerCol.setOrientation(VERTICAL);
        centerCol.addView(editorArea, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 0.62f));
        centerCol.addView(divider(ctx, false), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, FlatButton.dp(ctx, 1)));
        centerCol.addView(consoleRow, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 0.38f));

        /* ---- vizsgálópanel ---- */
        inspector = buildInspector();

        LinearLayout content = new LinearLayout(ctx);
        content.setOrientation(HORIZONTAL);
        content.addView(activityBar, new LayoutParams(FlatButton.dp(ctx, 46), LayoutParams.MATCH_PARENT));
        content.addView(sideBar, new LayoutParams(sideWidth, LayoutParams.MATCH_PARENT));
        content.addView(divider(ctx, true), new LayoutParams(FlatButton.dp(ctx, 1), LayoutParams.MATCH_PARENT));
        content.addView(centerCol, new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
        content.addView(divider(ctx, true), new LayoutParams(FlatButton.dp(ctx, 1), LayoutParams.MATCH_PARENT));
        content.addView(inspector, new LayoutParams(inspectorWidth, LayoutParams.MATCH_PARENT));

        addView(content, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        addView(statusBar, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    private void switchSide(LinearLayout target) {
        boolean already = sideOpen
                && target.getVisibility() == VISIBLE
                && target == visibleSide();
        if (already) {
            // ugyanarra koppintva a panel összecsukása
            sideBar.setVisibility(GONE);
            sideOpen = false;
            activityBar.setSelected(-1);
            return;
        }
        for (View v : new View[]{explorerPanel, runPanel, searchPanel}) {
            v.setVisibility(v == target ? VISIBLE : GONE);
        }
        sideBar.setVisibility(VISIBLE);
        sideOpen = true;
    }

    private View visibleSide() {
        if (explorerPanel.getVisibility() == VISIBLE) return explorerPanel;
        if (runPanel.getVisibility() == VISIBLE) return runPanel;
        if (searchPanel.getVisibility() == VISIBLE) return searchPanel;
        return null;
    }

    public void toggleSideBar() {
        sideBar.setVisibility(sideOpen ? GONE : VISIBLE);
        sideOpen = !sideOpen;
    }

    public void toggleInspector() {
        inspector.setVisibility(inspectorOpen ? GONE : VISIBLE);
        inspectorOpen = !inspectorOpen;
    }

    private LinearLayout buildExplorerPanel() {
        Context ctx = getContext();
        LinearLayout p = panel(ctx, Theme.p().sideBar);
        PanelHeader explorerHead = new PanelHeader(ctx, "Kezelő");
        headers.add(explorerHead);
        p.addView(explorerHead);

        ScrollView sc = new ScrollView(ctx);
        LinearLayout content = new LinearLayout(ctx);
        content.setOrientation(VERTICAL);
        int pad = FlatButton.dp(ctx, 10);
        content.setPadding(pad, pad, pad, pad);

        Button newBtn = registerBtn(FlatButton.create(ctx, FlatButton.SECONDARY, "Új program"),
                FlatButton.SECONDARY, VsIcons.NEW);
        newBtn.setCompoundDrawablesWithIntrinsicBounds(FlatButton.icon(ctx, VsIcons.NEW, Theme.p().buttonSecondaryFg), null, null, null);
        newBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                doNew();
            }
        });
        Button openBtn = registerBtn(FlatButton.create(ctx, FlatButton.SECONDARY, "Megnyitás…"),
                FlatButton.SECONDARY, VsIcons.OPEN);
        openBtn.setCompoundDrawablesWithIntrinsicBounds(FlatButton.icon(ctx, VsIcons.OPEN, Theme.p().buttonSecondaryFg), null, null, null);
        openBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                doLoad();
            }
        });
        saveBtn = registerBtn(FlatButton.create(ctx, FlatButton.SECONDARY, "Mentés…"),
                FlatButton.SECONDARY, VsIcons.SAVE);
        saveBtn.setCompoundDrawablesWithIntrinsicBounds(FlatButton.icon(ctx, VsIcons.SAVE, Theme.p().buttonSecondaryFg), null, null, null);
        saveBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                doSave();
            }
        });
        for (Button b : new Button[]{newBtn, openBtn, saveBtn}) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            lp.topMargin = FlatButton.dp(ctx, 4);
            content.addView(b, lp);
        }

        content.addView(outlineHeader(ctx));
        outline = (OutlineView) outline;
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, 0, 1f);
        content.addView(outline, olp);
        sc.addView(content);
        p.addView(sc, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        return p;
    }

    private PanelHeader outlineHeader(Context ctx) {
        PanelHeader h = new PanelHeader(ctx, "Vázlat", VsIcons.TREE, PanelHeader.ROLE_TITLE);
        headers.add(h);
        int top = FlatButton.dp(ctx, 14);
        h.setPadding(h.getPaddingLeft(), top, h.getPaddingRight(), h.getPaddingBottom());
        return h;
    }

    private LinearLayout buildRunPanel() {
        Context ctx = getContext();
        LinearLayout p = panel(ctx, Theme.p().sideBar);
        PanelHeader runHead = new PanelHeader(ctx, "Futtatás és hibakeresés");
        headers.add(runHead);
        p.addView(runHead);

        ScrollView sc = new ScrollView(ctx);
        LinearLayout content = new LinearLayout(ctx);
        content.setOrientation(VERTICAL);
        int pad = FlatButton.dp(ctx, 10);
        content.setPadding(pad, pad, pad, pad);

        runBigBtn = registerBtn(FlatButton.create(ctx, FlatButton.PRIMARY, "Program futtatása"),
                FlatButton.PRIMARY, VsIcons.PLAY);
        runBigBtn.setCompoundDrawablesWithIntrinsicBounds(FlatButton.icon(ctx, VsIcons.PLAY, Theme.p().buttonFg), null, null, null);
        runBigBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                doRun();
            }
        });
        content.addView(runBigBtn);

        parseBtn = sideButton(ctx, VsIcons.PARSE, "Értelmezés", new OnClickListener() {
            @Override
            public void onClick(View v) {
                doParse();
            }
        });
        stopBtn = sideButton(ctx, VsIcons.STOP, "Futtatás vége", new OnClickListener() {
            @Override
            public void onClick(View v) {
                doStop();
            }
        });
        stepBtn = sideButton(ctx, VsIcons.STEP_INTO, "Lépés", new OnClickListener() {
            @Override
            public void onClick(View v) {
                doStep();
            }
        });
        continueBtn = sideButton(ctx, VsIcons.PLAY, "Folytatás", new OnClickListener() {
            @Override
            public void onClick(View v) {
                doContinue();
            }
        });
        breakpointBtn = sideButton(ctx, VsIcons.ERROR, "Töréspont", new OnClickListener() {
            @Override
            public void onClick(View v) {
                doToggleBreakpoint();
            }
        });
        editBtn = sideButton(ctx, VsIcons.EDIT, "Szerkesztés", new OnClickListener() {
            @Override
            public void onClick(View v) {
                doEdit();
            }
        });
        copyBtn = sideButton(ctx, VsIcons.COPY, "Értelmezett átmásolása", new OnClickListener() {
            @Override
            public void onClick(View v) {
                doCopy();
            }
        });
        LinearLayout top = content;
        for (Button b : new Button[]{parseBtn, stopBtn}) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            lp.topMargin = FlatButton.dp(ctx, 4);
            top.addView(b, lp);
        }
        LinearLayout.LayoutParams sepLp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, FlatButton.dp(ctx, 10));
        content.addView(space(ctx), sepLp);
        for (Button b : new Button[]{stepBtn, continueBtn, breakpointBtn}) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            lp.topMargin = FlatButton.dp(ctx, 4);
            content.addView(b, lp);
        }
        content.addView(space(ctx), new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, FlatButton.dp(ctx, 10)));
        for (Button b : new Button[]{editBtn, copyBtn}) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            lp.topMargin = FlatButton.dp(ctx, 4);
            content.addView(b, lp);
        }
        enterBtn = sideButton(ctx, VsIcons.STEP_INTO, "Belépés alprogramba", new OnClickListener() {
            @Override
            public void onClick(View v) {
                doEnter();
            }
        });
        leaveBtn = sideButton(ctx, VsIcons.STEP_OUT, "Alprogram elhagyása", new OnClickListener() {
            @Override
            public void onClick(View v) {
                doLeave();
            }
        });
        enterBtn.setEnabled(false);
        enterBtn.setAlpha(0.4f);
        leaveBtn.setEnabled(false);
        leaveBtn.setAlpha(0.4f);
        if (subProgramsEnabled()) {
            for (Button b : new Button[]{enterBtn, leaveBtn}) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                lp.topMargin = FlatButton.dp(ctx, 4);
                content.addView(b, lp);
            }
        } else {
            // a gombok a kifejezésfa fejlécére kerülnek
        }
        sc.addView(content);
        p.addView(sc, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        return p;
    }

    private View space(Context ctx) {
        return new View(ctx);
    }

    private LinearLayout buildSearchPanel() {
        Context ctx = getContext();
        LinearLayout p = panel(ctx, Theme.p().sideBar);
        PanelHeader searchHead = new PanelHeader(ctx, "Keresés");
        headers.add(searchHead);
        p.addView(searchHead);
        LinearLayout content = new LinearLayout(ctx);
        content.setOrientation(VERTICAL);
        int pad = FlatButton.dp(ctx, 10);
        content.setPadding(pad, pad, pad, pad);
        searchHint = new TextView(ctx);
        searchHint.setText("A kereséshez használd a szerkesztő fölött megjelenő keresősávot.");
        searchHint.setTextColor(Theme.p().sideBarFg);
        searchHint.setTextSize(12);
        Button open = registerBtn(FlatButton.create(ctx, FlatButton.SECONDARY, "Keresősáv megnyitása"),
                FlatButton.SECONDARY, VsIcons.SEARCH);
        open.setCompoundDrawablesWithIntrinsicBounds(FlatButton.icon(ctx, VsIcons.SEARCH, Theme.p().buttonSecondaryFg), null, null, null);
        open.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                doFind();
            }
        });
        content.addView(searchHint);
        content.addView(open, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        p.addView(content, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        return p;
    }

    private Button sideButton(Context ctx, int icon, String label, OnClickListener action) {
        Button b = FlatButton.create(ctx, FlatButton.SECONDARY, label);
        b.setCompoundDrawablesWithIntrinsicBounds(
                FlatButton.icon(ctx, icon, Theme.p().buttonSecondaryFg), null, null, null);
        b.setOnClickListener(action);
        b.setMinHeight(FlatButton.dp(ctx, 34));
        return registerBtn(b, FlatButton.SECONDARY, icon);
    }

    private LinearLayout panel(Context ctx, int bg) {
        LinearLayout p = new LinearLayout(ctx);
        p.setOrientation(VERTICAL);
        p.setBackgroundColor(bg);
        return p;
    }

    private LinearLayout wrapStreamPanel(StreamPanesView tabs, String title, int icon) {
        Context ctx = getContext();
        LinearLayout p = panel(ctx, Theme.p().panelBg);
        p.setOrientation(VERTICAL);
        /* A fül saját maga írja a csatorna nevét (BEMENET/KIMENET), a külső
           fejléc csak megkettőzné – ezért nincs külön PanelHeader. */
        tabs.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        p.addView(tabs);
        if (icon == VsIcons.INPUT) {
            inputWrap = p;
        } else {
            outputWrap = p;
        }
        return p;
    }

    private LinearLayout buildInspector() {
        Context ctx = getContext();
        LinearLayout p = panel(ctx, Theme.p().panelBg);
        p.setOrientation(VERTICAL);

        if (subProgramsEnabled()) {
            callStackBox = panel(ctx, Theme.p().panelBg);
            callStackBox.setOrientation(VERTICAL);
            PanelHeader csHead = new PanelHeader(ctx, "Hívási verem", VsIcons.CALLSTACK, PanelHeader.ROLE_SYNCTRL);
            headers.add(csHead);
            callStackBox.addView(csHead);
            callStack.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, FlatButton.dp(ctx, 80)));
            callStackBox.addView(callStack);
            p.addView(callStackBox);
            p.addView(divider(ctx, false), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, FlatButton.dp(ctx, 1)));
        }

        varsPanelBox = panel(ctx, Theme.p().panelBg);
        varsPanelBox.setOrientation(VERTICAL);
        PanelHeader varsHead = new PanelHeader(ctx, "Változók", VsIcons.VARIABLES, PanelHeader.ROLE_SYNTYPE);
        headers.add(varsHead);
        varsPanelBox.addView(varsHead);
        varsPanelBox.addView(stateTable, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 0.55f));

        exprPanelBox = panel(ctx, Theme.p().panelBg);
        exprPanelBox.setOrientation(VERTICAL);
        PanelHeader exprHeader = new PanelHeader(ctx, "Kifejezés kiértékelése", VsIcons.TREE, PanelHeader.ROLE_SYNFUNC);
        headers.add(exprHeader);
        /* A fejlécben csak ikon-gombok vannak (mint az asztali TOOL-gombok),
           hogy a cím elférjen mellette; a szöveges párjuk a Futtatás panelre
           kerül, ha az alprogram-mód be van kapcsolva. */
        headerEnterBtn = FlatButton.iconButton(ctx, VsIcons.STEP_INTO, Theme.p().sideBarFg, "Belépés alprogramba");
        headerLeaveBtn = FlatButton.iconButton(ctx, VsIcons.STEP_OUT, Theme.p().sideBarFg, "Alprogram elhagyása");
        headerEnterBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                doEnter();
            }
        });
        headerLeaveBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                doLeave();
            }
        });
        headerEnterBtn.setEnabled(false);
        headerEnterBtn.setAlpha(0.4f);
        headerLeaveBtn.setEnabled(false);
        headerLeaveBtn.setAlpha(0.4f);
        exprHeader.addAction(headerEnterBtn);
        exprHeader.addAction(headerLeaveBtn);
        exprPanelBox.addView(exprHeader);
        exprPanelBox.addView(exprTree, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 0.45f));

        p.addView(varsPanelBox, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        p.addView(divider(ctx, false), new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, FlatButton.dp(ctx, 1)));
        p.addView(exprPanelBox, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        return p;
    }

    /* ==================== állapotátmenetek ==================== */

    /** Gomb bejegyzése a téma-váltáskor újra stílust kapó gombok közé. */
    private Button registerBtn(Button b, int style, int icon) {
        styledButtons.add(new Object[]{b, Integer.valueOf(style), Integer.valueOf(icon)});
        return b;
    }

    private void setEnabled(Button b, boolean en) {
        if (b != null) {
            b.setEnabled(en);
            b.setAlpha(en ? 1f : 0.45f);
        }
    }

    /** A belépés-alprogramba gombok (futtatás panel + fejléc) együtt kezelve. */
    private void setEnterEnabled(boolean en) {
        setEnabled(enterBtn, en);
        setEnabled(headerEnterBtn, en);
    }

    /** A kilépés gombok (futtatás panel + fejléc) együtt kezelve. */
    private void setLeaveEnabled(boolean en) {
        setEnabled(leaveBtn, en);
        setEnabled(headerLeaveBtn, en);
    }

    private void editState() {
        setEnabled(saveBtn, true);
        setEnabled(parseBtn, true);
        setEnabled(editBtn, false);
        setEnabled(copyBtn, false);
        setEnabled(runBigBtn, false);
        showEditor();
        updateStatus();
        updateUndoRedo();
        progText.requestFocus();
    }

    private void listState() {
        setEnabled(saveBtn, true);
        setEnabled(parseBtn, false);
        setEnabled(editBtn, true);
        showParsed();
        updateStatus();
    }

    private void runState() {
        setEnabled(editBtn, false);
        setEnabled(copyBtn, false);
        setEnabled(runBigBtn, false);
        setEnabled(stopBtn, true);
        setEnabled(parseBtn, false);
        running = true;
        inpPanes.setEditable(false);
        updateStatus();
    }

    private void stopState() {
        setEnabled(editBtn, true);
        setEnabled(copyBtn, true);
        setEnabled(runBigBtn, true);
        setEnabled(stopBtn, false);
        setEnabled(parseBtn, true);
        setEnterEnabled(false);
        setLeaveEnabled(false);
        running = false;
        inpPanes.setEditable(true);
        inpPanes.resetAttributes();
        progList.setCurrentLine(-1);
        progText.setRunningLine(-1);
        updateStatus();
    }

    private void finishedState(boolean hadError) {
        setEnabled(editBtn, true);
        setEnabled(copyBtn, true);
        setEnabled(runBigBtn, true);
        setEnabled(stopBtn, true);
        setEnabled(parseBtn, false);
        running = false;
        inpPanes.setEditable(true);
        showTransientMessage(hadError
                ? "A program futása hiba miatt megszakadt – leállítva."
                : "A program lefutott – leállítva.");
        updateStatus();
    }

    /* ==================== műveletek ==================== */

    public void doNew() {
        ifNewOkay("Biztosan új programot kezdesz?", new Runnable() {
            @Override
            public void run() {
                doNewNow();
            }
        });
    }

    private void doNewNow() {
        progText.setProgramText(
            "** Új PLanG program\n"
            + "PROGRAM ujprogram\n"
            + "VÁLTOZÓK:\n"
            + "  x: EGÉSZ\n"
            + "\n"
            + "  BE: x\n"
            + "  KI: x\n"
            + "PROGRAM_VÉGE\n");
        progTextChanged = false;
        currentFile = null;
        currentFileName = "névtelen.plang";
        editorTabs.setTitle("editor", "névtelen.plang");
        editorTabs.setDirty("editor", false);
        host.onThemeChanged(); // címsor frissítése
        editState();
        refreshOutline();
        updateUndoRedo();
    }

    public void doLoad() {
        ifNewOkay("Biztosan be akarsz tölteni egy új fájlt?", new Runnable() {
            @Override
            public void run() {
                host.launchOpen();
            }
        });
    }

    /** Megmentetlen változások esetén megerősítést kér, majd futtatja a műveletet. */
    private void ifNewOkay(String question, final Runnable action) {
        if (!progTextChanged) {
            action.run();
            return;
        }
        new AlertDialog.Builder(getContext())
                .setTitle("Betöltés")
                .setMessage("A programszöveg változásai nincsenek elmentve. " + question)
                .setPositiveButton("Igen", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        action.run();
                    }
                })
                .setNegativeButton("Nem", null)
                .show();
    }

    /** A gazda hívja, ha a felhasználó fájlt választott. */
    public void onFileOpened(Uri uri) {
        try {
            String text = FileStore.load(getContext(), uri);
            progText.setProgramText(text);
            progTextChanged = false;
            currentFile = uri;
            currentFileName = FileStore.fileName(getContext(), uri);
            editorTabs.setTitle("editor", currentFileName);
            editorTabs.setDirty("editor", false);
            showEditor();
            refreshOutline();
            updateStatus();
            updateUndoRedo();
            FileStore.addRecent(getContext(), uri);
        } catch (java.io.FileNotFoundException e) {
            errorBox("Hiba a megnyitás során",
                     "Nem sikerült az olvasás a következő fájlból: " + e.getMessage());
        } catch (Exception e) {
            errorBox("Hiba az olvasás során",
                     "Nem sikerült az olvasás a következő fájlból: " + e.getMessage());
        }
    }

    public void doSave() {
        if (currentFile != null) {
            saveFile(currentFile);
        } else {
            host.launchCreate("nevtelen.plang");
        }
    }

    public void doSaveAs() {
        host.launchCreate(currentFileName);
    }

    /** A gazda hívja, ha a mentési cél létrejött. */
    public void onSaveTarget(Uri uri) {
        saveFile(uri);
    }

    private void saveFile(Uri uri) {
        try {
            FileStore.save(getContext(), uri, progText.programText());
            progTextChanged = false;
            currentFile = uri;
            currentFileName = FileStore.fileName(getContext(), uri);
            editorTabs.setTitle("editor", currentFileName);
            editorTabs.setDirty("editor", false);
            updateStatus();
            FileStore.addRecent(getContext(), uri);
            showTransientMessage("Mentve: " + currentFileName);
        } catch (Exception e) {
            errorBox("Hiba a mentés során",
                     "Nem sikerült a mentés a következő fájlba: " + e.getMessage());
        }
    }

    /* ---- értelmezés ---- */

    public void doParse() {
        final String src = progText.programText();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final MainProgram prog = MainProgram.parseMainProgram(new Lexer(new StringReader(src)));
                    final List<ProgramLine> lines = prog.getLines();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            applyParsed(prog, lines);
                        }
                    });
                } catch (final Throwable t) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            errorBox("HIBA", "Belső hiba történt a szöveg értelmezése közben.");
                        }
                    });
                }
            }
        }).start();
    }

    private void applyParsed(MainProgram prog, List<ProgramLine> lines) {
        parsedProgram = prog;
        progList.setLines(lines);
        boolean ok = !prog.hasError();
        setEnabled(runBigBtn, ok);
        setEnabled(copyBtn, ok);
        listState();
        if (ok) {
            inpPanes.sync(prog.getStreams(StreamKind.INPUT), true);
            outPanes.sync(prog.getStreams(StreamKind.OUTPUT), false);
        }
        markErrors(prog, lines);
        refreshOutline();
        updateStatus();
    }

    /* ---- értelmezett program visszamásolása ---- */

    public void doCopy() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < progList.getLines().size(); i++) {
            sb.append(ProgRender.stripHtml(progList.getAt(i).toString())[0]).append("\n");
        }
        progText.setProgramText(sb.toString());
        progTextChanged = true;
        editorTabs.setDirty("editor", true);
        editState();
        refreshOutline();
        updateStatus();
        updateUndoRedo();
    }

    /* ---- futtatás ---- */

    private boolean canRun() {
        return parsedProgram != null && !parsedProgram.hasError();
    }

    public void doRun() {
        if (running || !canRun()) {
            return;
        }
        final MainProgram prog = parsedProgram;
        final Map<String, String> input = new HashMap<String, String>();
        for (int i = 0; i < inpPanes.count(); i++) {
            input.put(inpPanes.getTitleAt(i), inpPanes.getText(inpPanes.getTitleAt(i)));
        }
        runState();
        final int maxSteps = callStack.getMaxSteps();
        new Thread(new Runnable() {
            @Override
            public void run() {
                CallStackView.RunResult result = null;
                Throwable failure = null;
                try {
                    result = CallStackView.computeProgram(prog, input, maxSteps);
                } catch (OutOfMemoryError e) {
                    failure = new RuntimeException("memória");
                } catch (Throwable t) {
                    failure = t;
                }
                final CallStackView.RunResult fResult = result;
                final Throwable fFail = failure;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        finishRun(fResult, fFail);
                    }
                });
            }
        }).start();
    }

    private void finishRun(CallStackView.RunResult result, Throwable failure) {
        if (failure != null) {
            if ("memória".equals(failure.getMessage())) {
                errorBox("Hiba",
                        "Elfogyott a memória a program futásának szimulációja során.\n"
                        + "Próbálkozz kevesebb lépést végrehajtani, vagy kisebb tömböket használni.");
            } else {
                errorBox("HIBA", "Belső hiba történt a program futásának szimulációja közben.");
            }
            stopState();
            return;
        }
        Map<String, StreamData> output = new TreeMap<String, StreamData>();
        callStack.applyRunResult(result, output);
        State last = result.last;
        for (int i = 0; i < outPanes.count(); i++) {
            outPanes.setStream(outPanes.getTitleAt(i), output.get(outPanes.getTitleAt(i)));
        }
        lastStepCount = stateTable.rowCount();
        stateTable.setSelectedRow(lastStepCount - 1);
        updateStatus();

        boolean hadError = last != null && last.getError() != null;
        finishedState(hadError);
        if (backToEditor) {
            editState();
        }
        if (hadError) {
            errorBox("Futási hiba",
                     "A program futása a következő hiba miatt megszakadt:\n" + last.getError());
        }
    }

    public void doStop() {
        callStack.clearProgram();
        for (int i = 0; i < inpPanes.count(); i++) {
            inpPanes.setState(inpPanes.getTitleAt(i), null);
        }
        for (int i = 0; i < outPanes.count(); i++) {
            outPanes.setStream(outPanes.getTitleAt(i), null);
        }
        stopState();
    }

    /* ---- belépés / kilépés alprogramba ---- */

    private void doEnterNode(ExprNode n) {
        if (n == null || GuiBridge.subStates(n) == null) {
            return;
        }
        callStack.enter(n.toString(), GuiBridge.subStates(n));
        setLeaveEnabled(true);
        updateStatus();
    }

    public void doEnter() {
        doEnterNode(exprTree.getSelectedNode());
    }

    public void doLeave() {
        callStack.leave();
        if (callStack.stackDepth() <= 1) {
            setLeaveEnabled(false);
        }
        updateStatus();
    }

    /* ---- töréspont, lépés, folytatás ---- */

    public void doToggleBreakpoint() {
        int line = progText.caretLine() - 1;
        progText.toggleBreakpoint(line);
        showTransientMessage(progText.isBreakpoint(line)
                ? "Töréspont a " + (line + 1) + ". soron."
                : "Töréspont törölve a " + (line + 1) + ". sorról.");
    }

    private void ensureStates() {
        if (stateTable.rowCount() == 0 && canRun()) {
            doRun();
            stateTable.setSelectedRow(-1);
        }
    }

    public void doStep() {
        ensureStates();
        int rows = stateTable.rowCount();
        if (rows == 0) {
            showTransientMessage("Nincs futtatható program – előbb értelmezd.");
            return;
        }
        int sel = stateTable.getSelectedRow();
        int next = (sel < 0) ? 0 : Math.min(sel + 1, rows - 1);
        stateTable.setSelectedRow(next);
        onStateSelected();
        updateStatus();
    }

    public void doContinue() {
        ensureStates();
        int rows = stateTable.rowCount();
        if (rows == 0) {
            showTransientMessage("Nincs futtatható program – előbb értelmezd.");
            return;
        }
        int sel = stateTable.getSelectedRow();
        int target = -1;
        for (int r = sel + 1; r < rows; r++) {
            State s = stateTable.getState(r);
            if (s != null && progText.isBreakpoint(sourceLineForParsedIndex(s.getLine()))) {
                target = r;
                break;
            }
        }
        if (target < 0) {
            target = rows - 1;
        }
        stateTable.setSelectedRow(target);
        onStateSelected();
        updateStatus();
        showTransientMessage(target == rows - 1
                ? "Nincs további töréspont – a program végére értem."
                : "Megállás a " + (target + 1) + ". lépésnél (töréspont).");
    }

    /* ---- keresés, ugrás, betűméret, undo ---- */

    public void doFind() {
        showEditor();
        findBar.showBar(progText.selectedText());
    }

    public void doReplace() {
        showEditor();
        findBar.showBarWithReplace(progText.selectedText());
    }

    public void doGotoLine() {
        final android.widget.EditText input = new android.widget.EditText(getContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setSingleLine(true);
        int pad = FlatButton.dp(getContext(), 18);
        android.widget.LinearLayout box = new android.widget.LinearLayout(getContext());
        box.setOrientation(VERTICAL);
        box.setPadding(pad, pad, pad, 0);
        TextView hint = new TextView(getContext());
        hint.setText("Sor száma (1-" + progText.lineCount() + "):");
        hint.setTextColor(Theme.p().sideBarFg);
        box.addView(hint);
        box.addView(input);

        new AlertDialog.Builder(getContext())
                .setTitle("Ugrás sorra")
                .setView(box)
                .setPositiveButton("Rendben", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        try {
                            int line = Integer.parseInt(input.getText().toString().trim());
                            if (line >= 1 && line <= progText.lineCount()) {
                                showEditor();
                                progText.gotoLine(line - 1);
                                progText.requestFocus();
                            }
                        } catch (Exception e) {
                            // figyelmen kívül
                        }
                    }
                })
                .setNegativeButton("Mégse", null)
                .show();
    }

    public void doUndo() {
        progText.undo();
        updateUndoRedo();
        updateStatus();
    }

    public void doRedo() {
        progText.redo();
        updateUndoRedo();
        updateStatus();
    }

    public void doComment() {
        progText.toggleComment();
    }

    public void doDuplicateLine() {
        progText.duplicateLine();
    }

    public void doMoveLine(int dir) {
        progText.moveLine(dir);
    }

    public void increaseFont() {
        int cur = AppPrefs.getFontSize();
        int newSize = Math.min(42, cur + 1);
        if (newSize != cur) {
            AppPrefs.setFontFamily(AppPrefs.getFontFamily());
            AppPrefs.setFontSize(newSize);
            updateFont();
            updateStatus();
        }
    }

    public void decreaseFont() {
        int cur = AppPrefs.getFontSize();
        int newSize = Math.max(8, cur - 1);
        if (newSize != cur) {
            AppPrefs.setFontSize(newSize);
            updateFont();
            updateStatus();
        }
    }

    private void updateUndoRedo() {
        // a gombok menüből érhetők el; itt nincs teendő, de a későbbiekben
        // ide kerülne a gombállapot-frissítés
    }

    /* ---- beállítások, téma, súgó ---- */

    public void showPreferences() {
        PrefsDialog.show(getContext(), new PrefsDialog.ApplyListener() {
            @Override
            public void onApply(PrefsDialog.Values v) {
                boolean themeChanged = v.themeMode != Theme.mode();
                progText.setShowIndentGuides(v.indentGuides);
                backToEditor = v.autoStop;
                callStack.setMaxSteps(v.stepNum);
                if (themeChanged) {
                    Theme.setMode(v.themeMode);
                    applyThemeToAll();
                }
                updateFont();
                updateStatus();
                host.onThemeChanged();
            }
        });
    }

    public void toggleTheme() {
        Theme.toggleMode();
        applyThemeToAll();
        updateStatus();
        AppPrefs.setThemeMode(Theme.mode());
        host.onThemeChanged();
    }

    public void showHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>PLanG fejlesztőkörnyezet</h2>")
          .append("<p>PPKE ITK – programozási alapismeretek</p>")
          .append("<h3>Műveletek</h3>")
          .append("<p><b>Értelmezés</b> – a programszöveget végrehajtható alakba alakítja, jelzi a hibákat és létrehozza a csatornákat.</p>")
          .append("<p><b>Futtatás</b> – a program lefut; az állapottábla soronként mutatja a változókat, a kimenet a KIMENET fülön jelenik meg.</p>")
          .append("<p><b>Töréspont</b> – koppintás a sorszámsávra; a <b>Folytatás</b> a következő töréspontig fut, a <b>Lépés</b> egy állapotot halad.</p>")
          .append("<p>Az állapottábla sorára koppintva a szerkesztő kiemeli a sort, a kifejezésfa pedig lebontja a lépést.</p>")
          .append("<h3>Nyelvi elemek</h3>")
          .append("<p>PROGRAM … PROGRAM_VÉGE, VÁLTOZÓK:, HA … AKKOR … KÜLÖNBEN … HA_VÉGE, CIKLUS … AMÍG … CIKLUS_VÉGE, BE:, KI:</p>")
          .append("<p>Típusok: EGÉSZ, VALÓS, SZÖVEG, KARAKTER, LOGIKAI, BEFÁJL, KIFÁJL</p>")
          .append("<h3>Hardveres billentyűzet</h3>")
          .append("<p>Tab/Shift+Tab behúzás, Ctrl+/ megjegyzés, Ctrl+D sor megkettőzése, Alt+↑/↓ sor mozgatása, Ctrl+Space kódkiegészítés.</p>");
        TextView tv = new TextView(getContext());
        tv.setText(android.text.Html.fromHtml(sb.toString()));
        int pad = FlatButton.dp(getContext(), 18);
        ScrollView sc = new ScrollView(getContext());
        sc.addView(tv);
        android.widget.LinearLayout box = new android.widget.LinearLayout(getContext());
        box.setOrientation(VERTICAL);
        box.setPadding(pad, pad, pad, 0);
        box.addView(sc);
        new AlertDialog.Builder(getContext())
                .setTitle("Súgó")
                .setView(box)
                .setPositiveButton("Rendben", null)
                .show();
    }

    /* ==================== hibajelölés ==================== */

    private int[] mapParsedToSourceLines(List<ProgramLine> lines) {
        String[] src = progText.programText().split("\n", -1);
        int[] map = new int[lines.size()];
        int at = 0;
        for (int i = 0; i < map.length; i++) {
            String want = normLine(ProgRender.stripHtml(lines.get(i).toString())[0]);
            if (want.length() == 0) {
                map[i] = at < src.length ? at : -1;
                continue;
            }
            int found = -1;
            for (int j = at; j < src.length; j++) {
                String have = normLine(src[j]);
                if (have.length() == 0) {
                    continue;
                }
                if (want.startsWith(have) || have.startsWith(want)) {
                    found = j;
                    break;
                }
            }
            if (found < 0) {
                map[i] = at < src.length ? at : -1;
            } else {
                map[i] = found;
                at = found + 1;
            }
        }
        return map;
    }

    private static String normLine(String s) {
        if (s == null) {
            return "";
        }
        String t = hu.ppke.itk.plang.gui.editor.PlangSyntax.deacc(s.trim());
        StringBuilder b = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (!Character.isWhitespace(c)) {
                b.append(c);
            }
        }
        return b.toString();
    }

    private int sourceLineForParsedIndex(int idx) {
        if (parsedToSource == null || idx < 0 || idx >= parsedToSource.length) {
            return idx;
        }
        return parsedToSource[idx] >= 0 ? parsedToSource[idx] : idx;
    }

    private void markErrors(MainProgram prog, List<ProgramLine> lines) {
        errorCount = 0;
        errorList.clear();
        errorCursor = -1;
        int firstError = -1;
        parsedToSource = mapParsedToSourceLines(lines);
        for (int i = 0; i < lines.size(); i++) {
            ProgramLine pl = lines.get(i);
            if (GuiBridge.hasError(pl)) {
                errorCount++;
                if (firstError < 0) {
                    firstError = i;
                }
                errorList.add(new Object[] { Integer.valueOf(sourceLineForParsedIndex(i)),
                                             GuiBridge.error(pl) == null ? "Szintaktikai hiba." : GuiBridge.error(pl) });
            }
        }
        int[] linesArr = new int[errorList.size()];
        for (int i = 0; i < linesArr.length; i++) {
            linesArr[i] = ((Integer) errorList.get(i)[0]).intValue();
        }
        progText.setErrorLines(linesArr);
        if (firstError >= 0) {
            progList.setSelectedIndex(firstError);
            exprTree.setRoot(lines.get(firstError).getExpr(null));
            exprTree.expandAll();
        }
    }

    /** A következő hibára ugrik, és kiírja az üzenetét. */
    public void gotoNextError() {
        if (errorList.isEmpty()) {
            showTransientMessage("Nincs jelzett hiba.");
            return;
        }
        errorCursor = (errorCursor + 1) % errorList.size();
        Object[] e = errorList.get(errorCursor);
        int line = ((Integer) e[0]).intValue();
        doEdit();
        progText.gotoLine(line);
        progText.requestFocus();
        showTransientMessage("Hiba " + (errorCursor + 1) + "/" + errorList.size()
                             + " (sor " + (line + 1) + "): " + e[1]);
    }

    private void clearErrorMarks() {
        if (errorCount == 0 && errorList.isEmpty() && parsedToSource == null) {
            return;
        }
        errorCount = 0;
        errorList.clear();
        errorCursor = -1;
        parsedToSource = null;
        progText.setErrorLines(null);
    }

    /* ==================== kijelölés-visszacsatolás ==================== */

    private void onStateSelected() {
        State state = stateTable.getState(stateTable.getSelectedRow());
        if (state == null) {
            progList.setSelectedIndex(-1);
            exprTree.setRoot(ExprNode.EMPTY);
            progList.setCurrentLine(-1);
            progText.setRunningLine(-1);
        } else {
            ProgramLine line = progList.getAt(state.getLine());
            if (state.getError() == null) {
                progList.setSelectedIndex(state.getLine());
                exprTree.setRoot(line == null ? null : line.getExpr(state));
            } else {
                progList.setSelectedIndex(state.getLine());
                exprTree.setRoot(new ExprNode(
                        "<font color=\"" + Theme.errorHex() + "\">" + state.getError() + "</font>",
                        new ExprNode[0], null));
            }
            progList.setCurrentLine(state.getLine());
            progText.setRunningLine(sourceLineForParsedIndex(state.getLine()));
            for (int i = 0; i < inpPanes.count(); i++) {
                inpPanes.setState(inpPanes.getTitleAt(i), state.getStreamState(inpPanes.getTitleAt(i)));
            }
            for (int i = 0; i < outPanes.count(); i++) {
                outPanes.setState(outPanes.getTitleAt(i), state.getStreamState(outPanes.getTitleAt(i)));
            }
        }
        exprTree.expandAll();
        updateStatus();
    }

    /* ==================== státusz, üzenetek ==================== */

    private void refreshOutline() {
        outline.rebuild(progText.programText());
    }

    private void updateFont() {
        progText.updateEditorFont();
        gutter.syncFont();
        inpPanes.applyTheme();
        outPanes.applyTheme();
        progList.applyTheme();
        exprTree.applyTheme();
        stateTable.applyTheme();
        callStack.applyTheme();
    }

    private void updateStatus() {
        statusBar.setText("pos", "Sor " + progText.caretLine() + ", Oszlop " + progText.caretColumn());
        statusBar.setText("theme", Theme.isDark() ? "Sötét téma" : "Világos téma");
        statusBar.setText("diag", errorCount == 0 ? "Nincs hiba" : (errorCount + " hiba"));
        statusBar.setIcon("diag", errorCount == 0 ? VsIcons.CHECK : VsIcons.ERROR,
                errorCount == 0 ? null : Integer.valueOf(Theme.p().statusFg));
        int rows = stateTable.rowCount();
        if (running) {
            statusBar.setText("run", "Fut – " + lastStepCount + " lépés");
            statusBar.setIcon("run", VsIcons.STOP, null);
            statusBar.setText("steps", "Lépés " + (stateTable.getSelectedRow() + 1) + " / " + rows);
        } else if (rows > 0) {
            statusBar.setText("run", "Kész – " + rows + " lépés");
            statusBar.setIcon("run", VsIcons.STOP, null);
            int sel = stateTable.getSelectedRow();
            statusBar.setText("steps", sel >= 0 ? "Lépés " + (sel + 1) + " / " + rows : "");
        } else {
            statusBar.setText("run", "Futtatás");
            statusBar.setIcon("run", VsIcons.PLAY, null);
            statusBar.setText("steps", "");
        }
    }

    /** Rövid ideig látszó üzenet az állapotsorban. */
    private void showTransientMessage(String text) {
        statusBar.setText("msg", text);
        if (msgClear != null) {
            handler.removeCallbacks(msgClear);
        }
        msgClear = new Runnable() {
            @Override
            public void run() {
                statusBar.setText("msg", "");
            }
        };
        handler.postDelayed(msgClear, 3500);
    }

    private void errorBox(String title, String message) {
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Rendben", null)
                .show();
    }

    /* ==================== téma ==================== */

    public void applyThemeToAll() {
        Theme.Palette p = Theme.p();
        setBackgroundColor(p.editorBg);
        activityBar.invalidate();
        explorerPanel.setBackgroundColor(p.sideBar);
        runPanel.setBackgroundColor(p.sideBar);
        searchPanel.setBackgroundColor(p.sideBar);
        statusBar.applyTheme();
        editorTabs.applyTheme();
        findBar.applyTheme();
        progText.applyTheme();
        gutter.applyTheme();
        progPanel.setBackgroundColor(p.editorBg);
        progList.applyTheme();
        inpPanes.applyTheme();
        outPanes.applyTheme();
        stateTable.applyTheme();
        exprTree.applyTheme();
        if (callStackBox != null) {
            callStackBox.setBackgroundColor(p.panelBg);
        }
        callStack.applyTheme();
        outline.applyTheme();

        /* A panelek és fejlécek az építéskor kapott színt őrizték, ezért
           témaváltásnál sötét maradtak világos módban: most minden rögzített
           szín az aktuális palettáról frissül. */
        if (inputWrap != null) inputWrap.setBackgroundColor(p.panelBg);
        if (outputWrap != null) outputWrap.setBackgroundColor(p.panelBg);
        if (varsPanelBox != null) varsPanelBox.setBackgroundColor(p.panelBg);
        if (exprPanelBox != null) exprPanelBox.setBackgroundColor(p.panelBg);
        for (PanelHeader h : headers) {
            h.applyTheme();
        }
        for (Object[] sb : styledButtons) {
            Button b = (Button) sb[0];
            int style = ((Integer) sb[1]).intValue();
            int icon = ((Integer) sb[2]).intValue();
            FlatButton.applyStyle(b, style);
            if (icon >= 0) {
                int tint = (style == FlatButton.PRIMARY) ? p.buttonFg : p.buttonSecondaryFg;
                FlatButton.setIconButton(b, icon, tint);
            }
        }
        FlatButton.setIconButton(headerEnterBtn, VsIcons.STEP_INTO, p.sideBarFg);
        FlatButton.setIconButton(headerLeaveBtn, VsIcons.STEP_OUT, p.sideBarFg);
        if (searchHint != null) {
            searchHint.setTextColor(p.sideBarFg);
        }
        if (inspector != null) {
            inspector.setBackgroundColor(p.panelBg);
        }
        for (View d : dividers) {
            d.setBackgroundColor(p.border);
        }
        invalidate();
    }

    /* ==================== lekérdezések a gazdának ==================== */

    public boolean hasUnsavedChanges() {
        return progTextChanged;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void gotoError() {
        gotoNextError();
    }

    public String currentFileName() {
        return currentFileName;
    }

    public Uri currentFile() {
        return currentFile;
    }

    /** Vissza a szerkesztőbe (menü). */
    public void doEdit() {
        editState();
    }

    private boolean subProgramsEnabled() {
        return AppPrefs.getSubprograms();
    }
}
