package hu.ppke.itk.plang.android;

import java.util.ArrayList;
import java.util.List;

/**
 * A PLanG színpalettái – az asztali {@code gui.theme.Theme} változatlan
 * átvitele az Android grafikára (ARGB intekkel).
 * Dark+ és Light+ sémák, ugyanazokkal a mezőnevekkel.
 */
public final class Theme {

    public static final int DARK = 0;
    public static final int LIGHT = 1;

    public static final class Palette {
        public final int mode;

        public final int titleBar;
        public final int titleBarFg;
        public final int activityBar;
        public final int activityBarFg;
        public final int activityBarActiveFg;
        public final int activityBarActiveBorder;
        public final int sideBar;
        public final int sideBarFg;
        public final int sideBarTitleFg;
        public final int sectionHeader;
        public final int border;
        public final int focusBorder;
        public final int divider;

        public final int editorBg;
        public final int editorFg;
        public final int gutterFg;
        public final int gutterActiveFg;
        public final int currentLine;
        public final int currentLineBorder;
        public final int selection;
        public final int selectionInactive;
        public final int findMatch;
        public final int findMatchActive;
        public final int caret;
        public final int indentGuide;
        public final int ruler;

        public final int tabActiveBg;
        public final int tabInactiveBg;
        public final int tabActiveFg;
        public final int tabInactiveFg;
        public final int tabBorder;
        public final int tabActiveTopBorder;
        public final int tabBarBg;

        public final int panelBg;
        public final int listHover;
        public final int listSelection;
        public final int listSelectionFg;
        public final int listInactiveSelection;
        public final int tableHeaderBg;
        public final int tableGrid;

        public final int statusBg;
        public final int statusFg;
        public final int statusRunBg;
        public final int statusHover;

        public final int buttonBg;
        public final int buttonFg;
        public final int buttonHover;
        public final int buttonSecondaryBg;
        public final int buttonSecondaryFg;

        public final int inputBg;
        public final int inputFg;
        public final int inputBorder;

        public final int widgetBg;
        public final int widgetBorder;
        public final int scrollThumb;
        public final int scrollThumbHover;
        public final int badgeBg;
        public final int badgeFg;

        public final int error;
        public final int warning;
        public final int info;
        public final int success;
        public final int errorLineBg;

        public final int synKeyword;
        public final int synControl;
        public final int synType;
        public final int synString;
        public final int synNumber;
        public final int synComment;
        public final int synFunction;
        public final int synVariable;
        public final int synOperator;
        public final int synConstant;

        private Palette(int mode, int[] c) {
            int i = 0;
            this.mode = mode;
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
    }

    private static Palette current = darkPalette();

    private static int h(String rgb) {
        return (0xFF000000 | Integer.parseInt(rgb, 16));
    }

    public static Palette darkPalette() {
        return new Palette(DARK, new int[] {
            h("3C3C3C"), h("CCCCCC"),
            h("333333"), h("858585"), h("FFFFFF"), h("FFFFFF"),
            h("252526"), h("CCCCCC"), h("BBBBBB"), h("333333"),
            h("454545"), h("007FD4"), h("444444"),
            h("1E1E1E"), h("D4D4D4"),
            h("858585"), h("C6C6C6"),
            h("282828"), h("282828"),
            h("264F78"), h("3A3D41"),
            h("613214"), h("9E6A03"),
            h("AEAFAD"), h("404040"), h("5A5A5A"),
            h("1E1E1E"), h("2D2D2D"), h("FFFFFF"), h("969696"),
            h("252526"), h("007ACC"), h("252526"),
            h("1E1E1E"),
            h("2A2D2E"), h("094771"), h("FFFFFF"), h("37373D"),
            h("252526"), h("3C3C3C"),
            h("007ACC"), h("FFFFFF"), h("CC6633"), h("1F8AD2"),
            h("0E639C"), h("FFFFFF"), h("1177BB"),
            h("3A3D41"), h("CCCCCC"),
            h("3C3C3C"), h("CCCCCC"), h("3C3C3C"),
            h("252526"), h("454545"),
            h("4F4F4F"), h("5F5F5F"),
            h("4D4D4D"), h("FFFFFF"),
            h("F14C4C"), h("CCA700"), h("3794FF"), h("89D185"), h("3A1D1D"),
            h("569CD6"), h("C586C0"), h("4EC9B0"), h("CE9178"), h("B5CEA8"),
            h("6A9955"), h("DCDCAA"), h("9CDCFE"), h("D4D4D4"), h("569CD6")
        });
    }

    public static Palette lightPalette() {
        return new Palette(LIGHT, new int[] {
            h("DDDDDD"), h("333333"),
            h("F8F8F8"), h("616161"), h("1F1F1F"), h("1F1F1F"),
            h("F3F3F3"), h("3B3B3B"), h("6F6F6F"), h("3B3B3B"),
            h("E5E5E5"), h("0090F1"), h("CCCCCC"),
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
        current = (mode == LIGHT) ? lightPalette() : darkPalette();
    }

    public static void toggleMode() {
        setMode(isDark() ? LIGHT : DARK);
    }

    public static int alpha(int color, int a) {
        return (color & 0x00FFFFFF) | (a << 24);
    }

    /** A hibaszín #RRGGBB alakban (a beépített HTML-ekhez). */
    public static String errorHex() {
        int c = current.error;
        return String.format("#%02x%02x%02x", (c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
    }

    /** A mono betűtípus-családok Androidon (a beállítás párbeszédhez). */
    public static List<String> monoFamilies() {
        List<String> l = new ArrayList<String>();
        l.add("monospace");
        l.add("sans-serif");
        l.add("serif");
        return l;
    }

    public static String monoFamily() {
        return "monospace";
    }

    private Theme() {}
}
