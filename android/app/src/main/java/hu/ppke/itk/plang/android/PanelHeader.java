package hu.ppke.itk.plang.android;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Panelek fejléce („Kezelő”, „Változók” stb.) – az asztali widgets.PanelHeader
 * megfelelője, opcionális ikonnal és jobb oldali gombokkal.
 *
 * Az ikon színét szerepkör (role) azonosítja, nem konkrét színként: így a
 * téma váltásakor az {@link #applyTheme()} az aktuális palettáról számolja
 * újra (az építéskor rögzített szín a másik témához csúszna be).
 */
public class PanelHeader extends LinearLayout {

    /** Színszerepkörök (a téma mezőihez rendelve). */
    public static final int ROLE_TITLE = 0;      // sideBarTitleFg
    public static final int ROLE_INFO = 1;       // info
    public static final int ROLE_SUCCESS = 2;    // success
    public static final int ROLE_SYNTYPE = 3;    // synType
    public static final int ROLE_SYNFUNC = 4;    // synFunction
    public static final int ROLE_SYNCTRL = 5;    // synControl

    private final TextView title;
    private final LinearLayout actions;
    private final int iconType;
    private final int colorRole;

    public PanelHeader(Context ctx, String text) {
        this(ctx, text, -1, ROLE_TITLE);
    }

    public PanelHeader(Context ctx, String text, int iconType, int colorRole) {
        super(ctx);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        int pad = FlatButton.dp(ctx, 8);
        setPadding(pad, pad, pad, pad);
        this.iconType = iconType;
        this.colorRole = colorRole;

        if (iconType >= 0) {
            ImageView iv = new ImageView(ctx);
            iv.setImageDrawable(VsIcons.icon(iconType, FlatButton.dp(ctx, 14),
                    colorForRole(colorRole)));
            addView(iv, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            LayoutParams sp = new LayoutParams(FlatButton.dp(ctx, 6), 1);
            addView(new android.view.View(ctx), sp);
        }

        title = new TextView(ctx);
        title.setText(text);
        title.setTextColor(Theme.p().sideBarTitleFg);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setSingleLine(true);
        addView(title, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        actions = new LinearLayout(ctx);
        actions.setOrientation(HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        addView(actions, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    /** A szerepkörhöz tartozó szín az aktuális palettából. */
    public static int colorForRole(int role) {
        Theme.Palette p = Theme.p();
        switch (role) {
            case ROLE_INFO: return p.info;
            case ROLE_SUCCESS: return p.success;
            case ROLE_SYNTYPE: return p.synType;
            case ROLE_SYNFUNC: return p.synFunction;
            case ROLE_SYNCTRL: return p.synControl;
            default: return p.sideBarTitleFg;
        }
    }

    public void addAction(android.view.View v) {
        actions.addView(v);
    }

    public void applyTheme() {
        Theme.Palette p = Theme.p();
        title.setTextColor(p.sideBarTitleFg);
        for (int i = 0; i < getChildCount(); i++) {
            android.view.View c = getChildAt(i);
            if (c instanceof ImageView && iconType >= 0) {
                ((ImageView) c).setImageDrawable(VsIcons.icon(iconType,
                        FlatButton.dp(getContext(), 14), colorForRole(colorRole)));
            }
        }
        invalidate();
    }
}
