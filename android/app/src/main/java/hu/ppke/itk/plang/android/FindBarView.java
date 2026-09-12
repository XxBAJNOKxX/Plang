package hu.ppke.itk.plang.android;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.view.View;
import android.widget.TextView;

/**
 * Keresősáv cserével – a asztali widgets.FindBar portja: keresőmező,
 * kis-/nagybetű kapcsoló, találatszámláló, előre/hátra, csere és csere mindet.
 */
public class FindBarView extends LinearLayout {

    private final CodeEditorView editor;
    private final EditText findField;
    private final EditText replaceField;
    private final TextView countLabel;
    private final CheckBox caseBox;
    private final Button replaceToggle;
    private final LinearLayout replaceRow;
    private final Button prevBtn, nextBtn, closeBtn, repBtn, repAllBtn;
    private boolean replaceOpen = false;

    private CloseListener closeListener;

    public interface CloseListener {
        void onClose();
    }

    public FindBarView(Context ctx, CodeEditorView editor) {
        super(ctx);
        this.editor = editor;
        setOrientation(VERTICAL);
        int pad = FlatButton.dp(ctx, 4);
        setPadding(pad, pad, pad, 0);

        LinearLayout findRow = new LinearLayout(ctx);
        findRow.setOrientation(HORIZONTAL);
        findRow.setGravity(Gravity.CENTER_VERTICAL);

        replaceToggle = FlatButton.iconButton(ctx, VsIcons.CHEVRON_RIGHT, Theme.p().sideBarFg,
                "Csere megnyitása");
        replaceToggle.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                replaceOpen = !replaceOpen;
                replaceRow.setVisibility(replaceOpen ? VISIBLE : GONE);
                FlatButton.setIconButton(replaceToggle,
                        replaceOpen ? VsIcons.CHEVRON_DOWN : VsIcons.CHEVRON_RIGHT,
                        Theme.p().sideBarFg);
            }
        });
        findRow.addView(replaceToggle);

        findField = new EditText(ctx);
        styleField(findField);
        findField.setHint("Keresés");
        findField.setSingleLine(true);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        findRow.addView(findField, flp);

        caseBox = new CheckBox(ctx);
        caseBox.setText("Aa");
        caseBox.setTextColor(Theme.p().sideBarFg);
        findRow.addView(caseBox);

        countLabel = new TextView(ctx);
        countLabel.setText("");
        countLabel.setTextColor(Theme.p().sideBarFg);
        countLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        countLabel.setMinWidth(FlatButton.dp(ctx, 56));
        countLabel.setGravity(Gravity.CENTER);
        findRow.addView(countLabel);

        Button prev = FlatButton.iconButton(ctx, VsIcons.ARROW_UP, Theme.p().sideBarFg, "Előző találat");
        prevBtn = prev;
        prev.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                runSearch();
                editor.prevMatch();
                updateCount();
            }
        });
        findRow.addView(prev);

        Button next = FlatButton.iconButton(ctx, VsIcons.ARROW_DOWN, Theme.p().sideBarFg, "Következő találat");
        nextBtn = next;
        next.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                runSearch();
                editor.nextMatch();
                updateCount();
            }
        });
        findRow.addView(next);

        Button close = FlatButton.iconButton(ctx, VsIcons.CLOSE, Theme.p().sideBarFg, "Bezárás");
        closeBtn = close;
        close.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.clearSearch();
                setVisibility(GONE);
                if (closeListener != null) {
                    closeListener.onClose();
                }
            }
        });
        findRow.addView(close);

        addView(findRow, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        replaceRow = new LinearLayout(ctx);
        replaceRow.setOrientation(HORIZONTAL);
        replaceRow.setGravity(Gravity.CENTER_VERTICAL);
        replaceRow.setVisibility(GONE);

        replaceField = new EditText(ctx);
        styleField(replaceField);
        replaceField.setHint("Csere erre");
        replaceField.setSingleLine(true);
        replaceRow.addView(replaceField, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        Button rep = FlatButton.create(ctx, FlatButton.SECONDARY, "Csere");
        repBtn = rep;
        rep.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                runSearch();
                if (editor.replaceCurrent(replaceField.getText().toString())) {
                    updateCount();
                }
            }
        });
        replaceRow.addView(rep);

        Button repAll = FlatButton.create(ctx, FlatButton.SECONDARY, "Mind");
        repAllBtn = repAll;
        repAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                runSearch();
                int n = editor.replaceAll(findField.getText().toString(),
                                          replaceField.getText().toString(),
                                          caseBox.isChecked());
                countLabel.setText(n + " csere");
            }
        });
        replaceRow.addView(repAll);

        addView(replaceRow, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        findField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                runSearch();
                updateCount();
            }
        });
        caseBox.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                runSearch();
                updateCount();
            }
        });

        applyTheme();
    }

    public void setCloseListener(CloseListener l) {
        closeListener = l;
    }

    private void runSearch() {
        String needle = findField.getText().toString();
        if (needle.length() == 0) {
            editor.clearSearch();
            countLabel.setText("");
            return;
        }
        editor.search(needle, caseBox.isChecked());
    }

    private void updateCount() {
        int n = editor.matchCount();
        if (n == 0) {
            countLabel.setText("Nincs találat");
        } else {
            countLabel.setText((editor.activeMatchIndex() + 1) + " / " + n);
        }
    }

    /** A sáv megjelenítése, előre kitöltve a kijelölt szöveggel. */
    public void showBar(String selection) {
        setVisibility(VISIBLE);
        if (selection != null && selection.length() > 0 && selection.indexOf('\n') < 0) {
            findField.setText(selection);
            findField.setSelection(selection.length());
        }
        findField.requestFocus();
        updateCount();
    }

    public void showBarWithReplace(String selection) {
        showBar(selection);
        replaceOpen = true;
        replaceRow.setVisibility(VISIBLE);
        FlatButton.setIconButton(replaceToggle, VsIcons.CHEVRON_DOWN, Theme.p().sideBarFg);
    }

    private void styleField(EditText f) {
        Theme.Palette p = Theme.p();
        f.setTextColor(p.inputFg);
        f.setHintTextColor(p.gutterFg);
        f.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        f.setBackground(new android.graphics.drawable.ColorDrawable(p.inputBg));
        int pad = FlatButton.dp(getContext(), 6);
        f.setPadding(pad, pad, pad, pad);
        f.setTypeface(Typeface.SANS_SERIF);
    }

    public void applyTheme() {
        Theme.Palette p = Theme.p();
        setBackgroundColor(p.widgetBg);
        styleField(findField);
        styleField(replaceField);
        countLabel.setTextColor(p.sideBarFg);
        caseBox.setTextColor(p.sideBarFg);
        if (repBtn != null) {
            FlatButton.applyStyle(repBtn, FlatButton.SECONDARY);
            FlatButton.applyStyle(repAllBtn, FlatButton.SECONDARY);
            FlatButton.setIconButton(prevBtn, VsIcons.ARROW_UP, p.sideBarFg);
            FlatButton.setIconButton(nextBtn, VsIcons.ARROW_DOWN, p.sideBarFg);
            FlatButton.setIconButton(closeBtn, VsIcons.CLOSE, p.sideBarFg);
            FlatButton.setIconButton(replaceToggle,
                    replaceOpen ? VsIcons.CHEVRON_DOWN : VsIcons.CHEVRON_RIGHT, p.sideBarFg);
        }
        invalidate();
    }
}
