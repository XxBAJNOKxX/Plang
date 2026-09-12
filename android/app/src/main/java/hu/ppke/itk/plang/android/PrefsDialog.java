package hu.ppke.itk.plang.android;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;

/**
 * Beállítások – a asztali PrefDialog portja: betűtípus és -méret, lépésszám,
 * színséma, behúzás-segédvonalak, automatikus visszaállás a szerkesztőbe,
 * valamint (Androidon) az alprogram-mód kapcsolója.
 */
public final class PrefsDialog {

    /** A beállítás-értékek egy csomagja a párbeszéd eredményeként. */
    public static final class Values {
        public String fontFamily;
        public int fontSize;
        public int stepNum;
        public int themeMode;
        public boolean indentGuides;
        public boolean autoStop;
        public boolean subprograms;
    }

    public interface ApplyListener {
        void onApply(Values v);
    }

    private PrefsDialog() {}

    public static void show(final Context ctx, final ApplyListener listener) {
        Theme.Palette p = Theme.p();

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout panel = new LinearLayout(ctx);
        panel.setOrientation(LinearLayout.VERTICAL);
        int pad = FlatButton.dp(ctx, 18);
        panel.setPadding(pad, pad, pad, pad);
        scroll.addView(panel);

        /* ---- betűtípus ---- */
        panel.addView(label(ctx, "Betűtípus"));
        final Spinner fontCombo = new Spinner(ctx);
        List<String> families = Theme.monoFamilies();
        ArrayAdapter<String> fa = new ArrayAdapter<String>(ctx,
                android.R.layout.simple_spinner_dropdown_item, families);
        fontCombo.setAdapter(fa);
        String cur = AppPrefs.getFontFamily();
        for (int i = 0; i < families.size(); i++) {
            if (families.get(i).equals(cur)) {
                fontCombo.setSelection(i);
            }
        }
        panel.addView(fontCombo);

        /* ---- betűméret ---- */
        panel.addView(label(ctx, "Betűméret"));
        final EditText fontSize = new EditText(ctx);
        fontSize.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        fontSize.setText(String.valueOf(AppPrefs.getFontSize()));
        styleField(ctx, fontSize);
        panel.addView(fontSize);

        /* ---- lépésszám ---- */
        panel.addView(label(ctx, "Lépésszám"));
        final EditText stepNum = new EditText(ctx);
        stepNum.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        stepNum.setText(String.valueOf(AppPrefs.getStepNum()));
        styleField(ctx, stepNum);
        panel.addView(stepNum);

        /* ---- színséma ---- */
        panel.addView(label(ctx, "Színséma"));
        final Spinner themeCombo = new Spinner(ctx);
        ArrayAdapter<String> ta = new ArrayAdapter<String>(ctx,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Sötét (Dark+)", "Világos (Light+)"});
        themeCombo.setAdapter(ta);
        themeCombo.setSelection(Theme.mode() == Theme.LIGHT ? 1 : 0);
        panel.addView(themeCombo);

        /* ---- kapcsolók ---- */
        final CheckBox indentGuides = check(ctx, "Behúzás-segédvonalak megjelenítése",
                AppPrefs.getIndentGuides());
        panel.addView(indentGuides);

        final CheckBox autoStop = check(ctx, "Futás után lépjen vissza a szerkesztőbe",
                AppPrefs.getAutoStop());
        panel.addView(autoStop);

        final CheckBox subprograms = check(ctx, "Alprogramok (eljárás, függvény) engedélyezése",
                AppPrefs.getSubprograms());
        panel.addView(subprograms);

        AlertDialog dlg = new AlertDialog.Builder(ctx)
                .setTitle("Beállítások")
                .setView(scroll)
                .setPositiveButton("Rendben", null)
                .setNegativeButton("Mégse", null)
                .create();

        dlg.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
            @Override
            public void onShow(final android.content.DialogInterface d) {
                Button ok = ((AlertDialog) d).getButton(AlertDialog.BUTTON_POSITIVE);
                ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Values val = new Values();
                        try {
                            val.fontFamily = (String) fontCombo.getSelectedItem();
                            val.fontSize = clamp(parseInt(fontSize.getText().toString(), 14), 8, 42);
                            val.stepNum = clamp(parseInt(stepNum.getText().toString(), 10000), 10, 100000);
                        } catch (Exception e) {
                            return;
                        }
                        val.themeMode = themeCombo.getSelectedItemPosition() == 1 ? Theme.LIGHT : Theme.DARK;
                        val.indentGuides = indentGuides.isChecked();
                        val.autoStop = autoStop.isChecked();
                        val.subprograms = subprograms.isChecked();

                        AppPrefs.setFontFamily(val.fontFamily);
                        AppPrefs.setFontSize(val.fontSize);
                        AppPrefs.setStepNum(val.stepNum);
                        AppPrefs.setThemeMode(val.themeMode);
                        AppPrefs.setIndentGuides(val.indentGuides);
                        AppPrefs.setAutoStop(val.autoStop);
                        AppPrefs.setSubprograms(val.subprograms);
                        AppPrefs.flush();

                        if (listener != null) {
                            listener.onApply(val);
                        }
                        d.dismiss();
                    }
                });
            }
        });
        dlg.show();
    }

    private static TextView label(Context ctx, String text) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextSize(12);
        t.setTextColor(Theme.p().sideBarFg);
        int top = FlatButton.dp(ctx, 12);
        t.setPadding(0, top, 0, FlatButton.dp(ctx, 4));
        return t;
    }

    private static CheckBox check(Context ctx, String text, boolean checked) {
        CheckBox c = new CheckBox(ctx);
        c.setText(text);
        c.setChecked(checked);
        c.setTextColor(Theme.p().sideBarFg);
        c.setTextSize(13);
        int top = FlatButton.dp(ctx, 8);
        c.setPadding(FlatButton.dp(ctx, 4), top, 0, top);
        return c;
    }

    private static void styleField(Context ctx, EditText e) {
        e.setTextColor(Theme.p().inputFg);
        e.setBackground(new android.graphics.drawable.ColorDrawable(Theme.p().inputBg));
        int pad = FlatButton.dp(ctx, 8);
        e.setPadding(pad, pad / 2, pad, pad / 2);
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
