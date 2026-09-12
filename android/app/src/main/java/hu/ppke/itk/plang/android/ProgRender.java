package hu.ppke.itk.plang.android;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import hu.ppke.itk.plang.gui.editor.PlangSyntax;

/**
 * Közös kirajzolási segédek – a asztali ProgLineRenderer.stripHtml és a
 * szintaxis-színezés portja Android span-ekre.
 */
public final class ProgRender {

    private ProgRender() {}

    /**
     * A HTML jelölést egyszerű szöveggé alakítja, megjegyezve, mely
     * szakaszok voltak hibásra színezve. (Az asztali kód változatlan
     * átvitele.)
     */
    public static String[] stripHtml(String html) {
        StringBuilder text = new StringBuilder();
        StringBuilder flags = new StringBuilder();
        boolean bad = false;
        int i = 0;
        while (i < html.length()) {
            char c = html.charAt(i);
            if (c == '<') {
                int end = html.indexOf('>', i);
                if (end < 0) {
                    break;
                }
                String tag = html.substring(i + 1, end).toLowerCase();
                if (tag.startsWith("font") && tag.indexOf("red") >= 0) {
                    bad = true;
                } else if (tag.equals("/font")) {
                    bad = false;
                }
                i = end + 1;
                continue;
            }
            if (c == '&') {
                int semi = html.indexOf(';', i);
                if (semi > 0) {
                    String ent = html.substring(i + 1, semi);
                    char rep;
                    if (ent.equals("lt")) {
                        rep = '<';
                    } else if (ent.equals("gt")) {
                        rep = '>';
                    } else if (ent.equals("amp")) {
                        rep = '&';
                    } else if (ent.equals("nbsp")) {
                        rep = ' ';
                    } else if (ent.equals("quot")) {
                        rep = '"';
                    } else {
                        rep = '?';
                    }
                    text.append(rep);
                    flags.append(bad ? '1' : '0');
                    i = semi + 1;
                    continue;
                }
            }
            text.append(c);
            flags.append(bad ? '1' : '0');
            i++;
        }
        return new String[] { text.toString(), flags.toString() };
    }

    /** Egy programsor szintaxis-színezett span-jei (a bad jelölésekkel). */
    public static void applySyntax(SpannableStringBuilder b, String line, String badFlags,
                                   Theme.Palette p) {
        java.util.List<PlangSyntax.Token> tokens = PlangSyntax.tokenize(line);
        for (PlangSyntax.Token t : tokens) {
            int color = CodeEditorView.colorOf(p, t.kind);
            if (color != p.editorFg) {
                b.setSpan(new ForegroundColorSpan(color), t.start, t.end,
                          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        // a hibás szakaszok pirosan (a stripHtml jelzői szerint)
        for (int i = 0; i < badFlags.length() && i < line.length(); i++) {
            if (badFlags.charAt(i) == '1') {
                int s = i;
                while (i < badFlags.length() && i < line.length() && badFlags.charAt(i) == '1') {
                    i++;
                }
                b.setSpan(new ForegroundColorSpan(p.error), s, i, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                i--;
            }
        }
    }

    /** "kifejezés<b> = eredmény</b>" szétválasztása az ExprNode.toString()-ből. */
    public static String[] splitResult(String html) {
        boolean hadRed = html.toLowerCase().indexOf("color=\"red\"") >= 0
                         || html.toLowerCase().indexOf("color=red") >= 0;
        String plain = stripHtml(html)[0];
        int idx = plain.lastIndexOf(" = ");
        if (idx > 0) {
            return new String[] { plain.substring(0, idx), plain.substring(idx + 3),
                                  hadRed ? "1" : null };
        }
        return new String[] { plain, null, hadRed ? "1" : null };
    }

    /** Vastag szövegrész span-je. */
    public static void bold(SpannableStringBuilder b, int start, int end) {
        b.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, end,
                  Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}
