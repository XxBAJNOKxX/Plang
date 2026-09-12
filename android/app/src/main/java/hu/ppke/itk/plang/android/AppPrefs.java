package hu.ppke.itk.plang.android;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * A beállítások perzisztálása {@link SharedPreferences} segítségével –
 * az asztali {@code java.util.prefs}-es AppPrefs tükörmása, ugyanazokkal
 * a kulcsnevekkel. A legutóbbi fájlokat itt tartalmi URI-ként tároljuk.
 */
public final class AppPrefs {

    private static SharedPreferences prefs = null;

    public static void init(Context ctx) {
        prefs = ctx.getApplicationContext()
                   .getSharedPreferences("hu.ppke.itk.plang", Context.MODE_PRIVATE);
    }

    private static SharedPreferences p() {
        return prefs;
    }

    /* ---- betűtípus ---- */

    public static String getFontFamily() {
        try { return p().getString("fontFamily", "monospace"); }
        catch (Exception e) { return "monospace"; }
    }

    public static int getFontSize() {
        try { return p().getInt("fontSize", 14); }
        catch (Exception e) { return 14; }
    }

    public static void setFontFamily(String family) {
        try { if (family != null) p().edit().putString("fontFamily", family).apply(); }
        catch (Exception e) {}
    }

    public static void setFontSize(int size) {
        try { if (size >= 8 && size <= 72) p().edit().putInt("fontSize", size).apply(); }
        catch (Exception e) {}
    }

    /* ---- lépésszám ---- */

    public static int getStepNum() {
        try { return p().getInt("stepNum", 10000); }
        catch (Exception e) { return 10000; }
    }

    public static void setStepNum(int n) {
        try { p().edit().putInt("stepNum", n).apply(); }
        catch (Exception e) {}
    }

    /* ---- téma ---- */

    public static int getThemeMode() {
        try {
            int m = p().getInt("themeMode", Theme.DARK);
            return (m == Theme.LIGHT) ? Theme.LIGHT : Theme.DARK;
        } catch (Exception e) {
            return Theme.DARK;
        }
    }

    public static void setThemeMode(int mode) {
        try { p().edit().putInt("themeMode", mode).apply(); }
        catch (Exception e) {}
    }

    /* ---- behúzás-segédvonalak ---- */

    public static boolean getIndentGuides() {
        try { return p().getBoolean("indentGuides", true); }
        catch (Exception e) { return true; }
    }

    public static void setIndentGuides(boolean b) {
        try { p().edit().putBoolean("indentGuides", b).apply(); }
        catch (Exception e) {}
    }

    /* ---- automatikus visszaállás a szerkesztőbe ---- */

    public static boolean getAutoStop() {
        try { return p().getBoolean("autoStop", false); }
        catch (Exception e) { return false; }
    }

    public static void setAutoStop(boolean b) {
        try { p().edit().putBoolean("autoStop", b).apply(); }
        catch (Exception e) {}
    }

    /* ---- alprogramok (a asztali -Dhu.ppke.itk.plang.subprograms kapcsoló
            megfelelője: itt beállításban kapcsolható) ---- */

    public static boolean getSubprograms() {
        try { return p().getBoolean("subprograms", false); }
        catch (Exception e) { return false; }
    }

    public static void setSubprograms(boolean b) {
        try { p().edit().putBoolean("subprograms", b).apply(); }
        catch (Exception e) {}
    }

    /* ---- legutóbbi fájlok (tartalmi URI-k) ---- */

    public static List<String> getRecentFiles() {
        List<String> list = new ArrayList<String>();
        try {
            SharedPreferences p = p();
            for (int i = 0; i < 8; i++) {
                String uri = p.getString("recent" + i, null);
                if (uri != null) {
                    list.add(uri);
                }
            }
        } catch (Exception e) {}
        return list;
    }

    public static void setRecentFiles(List<String> uris) {
        try {
            SharedPreferences.Editor ed = p().edit();
            for (int i = 0; i < 8; i++) {
                if (i < uris.size()) {
                    ed.putString("recent" + i, uris.get(i));
                } else {
                    ed.remove("recent" + i);
                }
            }
            ed.apply();
        } catch (Exception e) {}
    }

    public static void flush() {
        // az apply() már aszinkron perzisztál; a kompatibilitás miatt megmarad
    }

    private AppPrefs() {}
}
