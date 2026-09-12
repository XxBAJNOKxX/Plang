package hu.ppke.itk.plang.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.List;

/**
 * A PLanG tablet-alkalmazás főablaka: menüsor + a teljes munkafelület.
 * A fájlműveletek a Storage Access Frameworkön keresztül mennek.
 */
public class MainActivity extends Activity implements IdeView.Host {

    private static final int REQ_OPEN = 11;
    private static final int REQ_CREATE = 12;

    private IdeView ide;
    private LinearLayout menuBar;
    private TextView titleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppPrefs.init(this);
        Theme.setMode(AppPrefs.getThemeMode());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Theme.p().editorBg);

        root.addView(buildMenuBar());

        ide = new IdeView(this, this);
        root.addView(ide, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        applyWindowChrome();
    }

    /* ==================== menüsor ==================== */

    private LinearLayout buildMenuBar() {
        menuBar = new LinearLayout(this);
        menuBar.setOrientation(LinearLayout.HORIZONTAL);
        menuBar.setGravity(Gravity.CENTER_VERTICAL);
        menuBar.setBackgroundColor(Theme.p().titleBar);

        titleView = new TextView(this);
        titleView.setText("PLanG");
        titleView.setTextColor(Theme.p().titleBarFg);
        titleView.setTextSize(14);
        titleView.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        int pad = FlatButton.dp(this, 12);
        titleView.setPadding(pad, 0, pad, 0);
        menuBar.addView(titleView);

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout menus = new LinearLayout(this);
        menus.setOrientation(LinearLayout.HORIZONTAL);
        menus.setGravity(Gravity.CENTER_VERTICAL);

        addMenu(menus, "Fájl", new Object[][]{
            {"Új program", "new"},
            {"Megnyitás…", "open"},
            {"Mentés", "save"},
            {"Mentés másként…", "saveAs"},
            {"Legutóbbi fájlok…", "recent"},
            {"Kilépés", "exit"},
        });
        addMenu(menus, "Szerkesztés", new Object[][]{
            {"Visszavonás", "undo"},
            {"Újra", "redo"},
            {"Keresés", "find"},
            {"Csere", "replace"},
            {"Ugrás sorra…", "goto"},
            {"Megjegyzés ki/be", "comment"},
            {"Sor megkettőzése", "dup"},
            {"Sor mozgatása fel", "moveUp"},
            {"Sor mozgatása le", "moveDown"},
            {"Betűméret növelése", "fontUp"},
            {"Betűméret csökkentése", "fontDown"},
        });
        addMenu(menus, "Futtatás", new Object[][]{
            {"Értelmezés", "parse"},
            {"Futtatás", "run"},
            {"Futtatás vége", "stop"},
            {"Töréspont ki/be", "breakpoint"},
            {"Lépés", "step"},
            {"Folytatás", "cont"},
            {"Szerkesztés", "edit"},
            {"Értelmezett átmásolása", "copy"},
            {"Belépés alprogramba", "enter"},
            {"Alprogram elhagyása", "leave"},
        });
        addMenu(menus, "Nézet", new Object[][]{
            {"Téma váltása", "theme"},
            {"Oldalsáv be/ki", "side"},
            {"Vizsgálópanel be/ki", "inspector"},
            {"Beállítások", "prefs"},
        });
        addMenu(menus, "Súgó", new Object[][]{
            {"Súgó", "help"},
        });

        scroll.addView(menus, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        menuBar.addView(scroll, lp);
        return menuBar;
    }

    private void addMenu(LinearLayout parent, String title, final Object[][] items) {
        TextView b = new TextView(this);
        b.setText(title);
        b.setTextColor(Theme.p().titleBarFg);
        b.setTextSize(13);
        int pad = FlatButton.dp(this, 10);
        b.setPadding(pad, 0, pad, 0);
        b.setGravity(Gravity.CENTER);
        b.setBackground(new ColorDrawable(Color.TRANSPARENT));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu(v, items);
            }
        });
        parent.addView(b);
    }

    private void showMenu(View anchor, Object[][] items) {
        anchorOf = anchor;
        PopupMenu pm = new PopupMenu(this, anchor);
        buildMenuItems(pm, items);
        pm.show();
    }

    @SuppressWarnings("deprecation")
    private void buildMenuItems(PopupMenu pm, Object[][] items) {
        pm.getMenu().clear();
        for (final Object[] item : items) {
            final String label = (String) item[0];
            final String cmd = (String) item[1];
            if (("enter".equals(cmd) || "leave".equals(cmd)) && !AppPrefs.getSubprograms()) {
                continue;
            }
            if ("recent".equals(cmd)) {
                pm.getMenu().add(label).setOnMenuItemClickListener(
                        new android.view.MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(android.view.MenuItem mi) {
                        showRecentMenu(anchorOf != null ? anchorOf : menuBar);
                        return true;
                    }
                });
                continue;
            }
            pm.getMenu().add(label).setOnMenuItemClickListener(
                    new android.view.MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(android.view.MenuItem mi) {
                    runCommand(cmd);
                    return true;
                }
            });
        }
    }

    private View anchorOf;

    private void showRecentMenu(View anchor) {
        PopupMenu pm = new PopupMenu(this, anchor);
        List<Uri> recents = FileStore.getRecent(this);
        if (recents.isEmpty()) {
            pm.getMenu().add("(nincs legutóbbi fájl)").setEnabled(false);
        } else {
            for (int i = 0; i < recents.size(); i++) {
                final Uri uri = recents.get(i);
                pm.getMenu().add((i + 1) + ". " + FileStore.fileName(this, uri))
                  .setOnMenuItemClickListener(new android.view.MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(android.view.MenuItem mi) {
                        ide.onFileOpened(uri);
                        return true;
                    }
                });
            }
            pm.getMenu().add("Lista törlése")
              .setOnMenuItemClickListener(new android.view.MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(android.view.MenuItem mi) {
                    FileStore.clearRecent();
                    return true;
                }
            });
        }
        pm.show();
    }

    private void runCommand(String cmd) {
        switch (cmd) {
            case "new": ide.doNew(); break;
            case "open": ide.doLoad(); break;
            case "save": ide.doSave(); break;
            case "saveAs": ide.doSaveAs(); break;
            case "recent": showRecentMenu(anchorOf != null ? anchorOf : menuBar); break;
            case "exit": confirmExit(); break;
            case "undo": ide.doUndo(); break;
            case "redo": ide.doRedo(); break;
            case "find": ide.doFind(); break;
            case "replace": ide.doReplace(); break;
            case "goto": ide.doGotoLine(); break;
            case "comment": ide.doComment(); break;
            case "dup": ide.doDuplicateLine(); break;
            case "moveUp": ide.doMoveLine(-1); break;
            case "moveDown": ide.doMoveLine(1); break;
            case "fontUp": ide.increaseFont(); break;
            case "fontDown": ide.decreaseFont(); break;
            case "parse": ide.doParse(); break;
            case "run": ide.doRun(); break;
            case "stop": ide.doStop(); break;
            case "breakpoint": ide.doToggleBreakpoint(); break;
            case "step": ide.doStep(); break;
            case "cont": ide.doContinue(); break;
            case "edit": ide.doEdit(); break;
            case "copy": ide.doCopy(); break;
            case "enter": ide.doEnter(); break;
            case "leave": ide.doLeave(); break;
            case "theme": ide.toggleTheme(); break;
            case "side": ide.toggleSideBar(); break;
            case "inspector": ide.toggleInspector(); break;
            case "prefs": ide.showPreferences(); break;
            case "help": ide.showHelp(); break;
        }
    }

    /* ==================== Host megvalósítás ==================== */

    @Override
    public void launchOpen() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                   | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(i, REQ_OPEN);
    }

    @Override
    public void launchCreate(String suggestedName) {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                   | Intent.FLAG_GRANT_READ_URI_PERMISSION
                   | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        i.putExtra(Intent.EXTRA_TITLE, FileStore.ensurePlangExtension(suggestedName));
        startActivityForResult(i, REQ_CREATE);
    }

    @Override
    public void onThemeChanged() {
        applyWindowChrome();
        menuBar.setBackgroundColor(Theme.p().titleBar);
        for (int i = 0; i < menuBar.getChildCount(); i++) {
            View c = menuBar.getChildAt(i);
            if (c instanceof TextView) {
                ((TextView) c).setTextColor(Theme.p().titleBarFg);
            }
        }
        titleView.setText(ide.hasUnsavedChanges() ? "PLanG – " + ide.currentFileName() + " •" : ide.currentFileName() + " – PLanG");
    }

    private void applyWindowChrome() {
        getWindow().setStatusBarColor(Theme.p().titleBar);
        getWindow().setNavigationBarColor(Theme.p().activityBar);
        getWindow().setBackgroundDrawable(new ColorDrawable(Theme.p().editorBg));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        if (requestCode == REQ_OPEN) {
            ide.onFileOpened(uri);
        } else if (requestCode == REQ_CREATE) {
            ide.onSaveTarget(uri);
        }
    }

    /* ==================== visszagomb ==================== */

    @Override
    public void onBackPressed() {
        if (!ide.hasUnsavedChanges()) {
            super.onBackPressed();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Kilépés")
                .setMessage("A programszöveg változásai nincsenek elmentve. Biztosan kilépsz?")
                .setPositiveButton("Igen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        finish();
                    }
                })
                .setNegativeButton("Nem", null)
                .show();
    }

    private void confirmExit() {
        onBackPressed();
    }
}
