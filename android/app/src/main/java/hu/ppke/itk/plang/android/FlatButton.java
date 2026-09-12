package hu.ppke.itk.plang.android;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;

import hu.ppke.itk.plang.android.Theme;

/**
 * Lapos, VS Code stílusú gomb – a asztali widgets.FlatButton megfelelője.
 */
public final class FlatButton {

    public static final int PRIMARY = 0;
    public static final int SECONDARY = 1;
    public static final int TOOL = 2;

    private FlatButton() {}

    public static Button create(Context ctx, int style, String text) {
        Button b = new Button(ctx, null, 0);
        b.setText(text);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        b.setSingleLine(true);
        int pad = dp(ctx, 10);
        b.setPadding(pad, dp(ctx, 6), pad, dp(ctx, 6));
        applyStyle(b, style);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        return b;
    }

    public static void applyStyle(Button b, int style) {
        Theme.Palette p = Theme.p();
        Context ctx = b.getContext();
        switch (style) {
            case PRIMARY:
                b.setBackground(bg(p.buttonBg, p.buttonHover));
                b.setTextColor(p.buttonFg);
                break;
            case SECONDARY:
                b.setBackground(bg(p.buttonSecondaryBg, p.buttonHover));
                b.setTextColor(p.buttonSecondaryFg);
                break;
            default:
                b.setBackground(bg(Color.TRANSPARENT, p.listHover));
                b.setTextColor(p.sideBarFg);
                int vpad = dp(ctx, 4);
                b.setPadding(dp(ctx, 6), vpad, dp(ctx, 6), vpad);
                b.setMinHeight(dp(ctx, 30));
                break;
        }
    }

    private static Drawable bg(int normal, int pressed) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(pressed));
        s.addState(new int[]{}, new ColorDrawable(normal));
        return s;
    }

    /** Ikonos gomb (eszköztár-stílus). */
    public static Button iconButton(Context ctx, int iconType, int color, String contentDesc) {
        Button b = new Button(ctx, null, 0);
        b.setText("");
        b.setMinWidth(dp(ctx, 34));
        b.setMinHeight(dp(ctx, 30));
        int pad = dp(ctx, 6);
        b.setPadding(pad, pad, pad, pad);
        b.setContentDescription(contentDesc);
        b.setBackground(bg(Color.TRANSPARENT, Theme.p().listHover));
        b.setCompoundDrawablesWithIntrinsicBounds(icon(ctx, iconType, color), null, null, null);
        return b;
    }

    /** Meglévő ikon-gomb színének frissítése (téma váltásnál). */
    public static void setIconButton(Button b, int iconType, int color) {
        if (b == null) {
            return;
        }
        b.setCompoundDrawablesWithIntrinsicBounds(icon(b.getContext(), iconType, color), null, null, null);
    }

    public static Drawable icon(Context ctx, int iconType, int color) {
        int size = dp(ctx, 16);
        return VsIcons.icon(iconType, size, color);
    }

    public static float dpf(Context ctx, float v) {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, v, ctx.getResources().getDisplayMetrics());
    }

    public static int dp(Context ctx, int v) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, v, ctx.getResources().getDisplayMetrics());
    }

    public static void onClick(View v, Runnable r) {
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                r.run();
            }
        });
    }
}
