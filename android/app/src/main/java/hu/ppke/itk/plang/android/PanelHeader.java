package hu.ppke.itk.plang.android;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Panelek fejléce („Kezelő”, „Változók” stb.) – a asztali widgets.PanelHeader
 * megfelelője, opcionális ikonnal és jobb oldali gombokkal.
 */
public class PanelHeader extends LinearLayout {

    private final TextView title;
    private final LinearLayout actions;
    private int iconType = -1;
    private int iconColor = -1;

    public PanelHeader(Context ctx, String text) {
        this(ctx, text, -1, -1);
    }

    public PanelHeader(Context ctx, String text, int iconType, int iconColor) {
        super(ctx);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        int pad = FlatButton.dp(ctx, 8);
        setPadding(pad, pad, pad, pad);
        this.iconType = iconType;
        this.iconColor = iconColor;

        if (iconType >= 0) {
            ImageView iv = new ImageView(ctx);
            iv.setImageDrawable(VsIcons.icon(iconType, FlatButton.dp(ctx, 14),
                                             iconColor == -1 ? Theme.p().sideBarTitleFg : iconColor));
            addView(iv, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            addView(space(ctx, 6));
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

    public void addAction(android.view.View v) {
        actions.addView(v);
    }

    public void applyTheme() {
        Theme.Palette p = Theme.p();
        title.setTextColor(p.sideBarTitleFg);
        for (int i = 0; i < getChildCount(); i++) {
            android.view.View c = getChildAt(i);
            if (c instanceof ImageView && iconType >= 0) {
                ((ImageView) c).setImageDrawable(VsIcons.icon(iconType, FlatButton.dp(getContext(), 14),
                        iconColor == -1 ? p.sideBarTitleFg : iconColor));
            }
        }
        for (int i = 0; i < actions.getChildCount(); i++) {
            android.view.View a = actions.getChildAt(i);
            if (a instanceof android.widget.Button) {
                ((android.widget.Button) a).setTextColor(p.sideBarFg);
            }
        }
        invalidate();
    }

    static android.view.View space(Context ctx, int px) {
        android.view.View v = new android.view.View(ctx);
        LayoutParams lp = new LayoutParams(px, 1);
        v.setLayoutParams(lp);
        return v;
    }
}
