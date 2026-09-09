package hu.ppke.itk.plang.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import hu.ppke.itk.plang.gui.theme.Theme;
import hu.ppke.itk.plang.plang;

/**
 * A beállítások perzisztálása {@link Preferences} segítségével.
 * Minden érték hibatűrően töltődik vissza: hiányzó vagy sérült érték
 * esetén az alapértelmezés marad.
 */
public final class AppPrefs {

    private static Preferences prefs() {
        try {
            return Preferences.userNodeForPackage(plang.class);
        } catch (Exception e) {
            // ha valamiért nem elérhető, egy ideiglenes csomópontot használunk
            return Preferences.userRoot().node("hu/ppke/itk/plang");
        }
    }

    /* ---- betűtípus ---- */

    public static String getFontFamily() {
        try {
            return prefs().get("fontFamily", Theme.monoFamily());
        } catch (Exception e) {
            return Theme.monoFamily();
        }
    }

    public static int getFontSize() {
        try {
            return prefs().getInt("fontSize", 13);
        } catch (Exception e) {
            return 13;
        }
    }

    public static void setFontFamily(String family) {
        try {
            if (family != null) prefs().put("fontFamily", family);
        } catch (Exception e) {}
    }

    public static void setFontSize(int size) {
        try {
            if (size >= 8 && size <= 72) prefs().putInt("fontSize", size);
        } catch (Exception e) {}
    }

    /* ---- lépésszám ---- */

    public static int getStepNum() {
        try {
            return prefs().getInt("stepNum", 10000);
        } catch (Exception e) {
            return 10000;
        }
    }

    public static void setStepNum(int n) {
        try {
            prefs().putInt("stepNum", n);
        } catch (Exception e) {}
    }

    /* ---- téma ---- */

    public static int getThemeMode() {
        try {
            int m = prefs().getInt("themeMode", Theme.DARK);
            return (m == Theme.LIGHT) ? Theme.LIGHT : Theme.DARK;
        } catch (Exception e) {
            return Theme.DARK;
        }
    }

    public static void setThemeMode(int mode) {
        try {
            prefs().putInt("themeMode", mode);
        } catch (Exception e) {}
    }

    /* ---- behúzás-segédvonalak ---- */

    public static boolean getIndentGuides() {
        try {
            return prefs().getBoolean("indentGuides", true);
        } catch (Exception e) {
            return true;
        }
    }

    public static void setIndentGuides(boolean b) {
        try {
            prefs().putBoolean("indentGuides", b);
        } catch (Exception e) {}
    }

    /* ---- automatikus leállás a program végén ---- */

    public static boolean getAutoStop() {
        try {
            return prefs().getBoolean("autoStop", false);
        } catch (Exception e) {
            return false;
        }
    }

    public static void setAutoStop(boolean b) {
        try {
            prefs().putBoolean("autoStop", b);
        } catch (Exception e) {}
    }

    /* ---- ablakméret / pozíció ---- */

    public static int getWindowWidth() {
        try { return prefs().getInt("windowWidth", 1440); } catch (Exception e) { return 1440; }
    }

    public static int getWindowHeight() {
        try { return prefs().getInt("windowHeight", 876); } catch (Exception e) { return 876; }
    }

    public static int getWindowX() {
        try { return prefs().getInt("windowX", -1); } catch (Exception e) { return -1; }
    }

    public static int getWindowY() {
        try { return prefs().getInt("windowY", -1); } catch (Exception e) { return -1; }
    }

    public static void setWindowSize(int w, int h) {
        try {
            prefs().putInt("windowWidth", w);
            prefs().putInt("windowHeight", h);
        } catch (Exception e) {}
    }

    public static void setWindowPosition(int x, int y) {
        try {
            prefs().putInt("windowX", x);
            prefs().putInt("windowY", y);
        } catch (Exception e) {}
    }

    /* ---- osztópanelek ---- */

    public static int getDividerMain() {
        try { return prefs().getInt("dividerMain", 260); } catch (Exception e) { return 260; }
    }

    public static int getDividerCenter() {
        try { return prefs().getInt("dividerCenter", -1); } catch (Exception e) { return -1; }
    }

    public static int getDividerRight() {
        try { return prefs().getInt("dividerRight", -1); } catch (Exception e) { return -1; }
    }

    public static int getDividerInspect() {
        try { return prefs().getInt("dividerInspect", -1); } catch (Exception e) { return -1; }
    }

    public static int getDividerConsole() {
        try { return prefs().getInt("dividerConsole", -1); } catch (Exception e) { return -1; }
    }

    public static void setDividers(int main, int center, int right, int inspect, int console) {
        try {
            Preferences p = prefs();
            p.putInt("dividerMain", main);
            p.putInt("dividerCenter", center);
            p.putInt("dividerRight", right);
            p.putInt("dividerInspect", inspect);
            p.putInt("dividerConsole", console);
        } catch (Exception e) {}
    }

    /* ---- legutóbbi fájlok ---- */

    public static List<File> getRecentFiles() {
        List<File> list = new ArrayList<File>();
        try {
            Preferences p = prefs();
            for (int i = 0; i < 8; i++) {
                String path = p.get("recent" + i, null);
                if (path != null) {
                    File f = new File(path);
                    if (f.exists() && f.isFile()) {
                        list.add(f);
                    }
                }
            }
        } catch (Exception e) {}
        return list;
    }

    public static void setRecentFiles(List<File> files) {
        try {
            Preferences p = prefs();
            for (int i = 0; i < 8; i++) {
                if (i < files.size()) {
                    p.put("recent" + i, files.get(i).getAbsolutePath());
                } else {
                    p.remove("recent" + i);
                }
            }
        } catch (Exception e) {}
    }

    public static void flush() {
        try {
            prefs().flush();
        } catch (Exception e) {}
    }

    private AppPrefs() {}
}
