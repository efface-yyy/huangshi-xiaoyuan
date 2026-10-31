package com.smart.hgnu;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Reusable programmatic widgets and spacing; all dimensions are in dp. */
class UiComponents {
    protected final Activity activity;
    static final int INK = 0xff183b32;
    static final int MUTED = 0xff75847a;
    static final int GREEN = 0xff146c57;
    static final int PAPER = 0xfff6f7f2;
    static final int LINE = 0xffe7ebe3;

    UiComponents(Activity activity) {
        this.activity = activity;
    }

    protected int dpToPx(float sizeDp) {
        return (int) (activity.getResources().getDisplayMetrics().density * sizeDp + .5f);
    }

    protected LinearLayout createColumn() {
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    protected LinearLayout createRow() {
        LinearLayout layout = new LinearLayout(activity);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    protected void setUniformPadding(View view, int sizeDp) {
        view.setPadding(dpToPx(sizeDp), dpToPx(sizeDp), dpToPx(sizeDp), dpToPx(sizeDp));
    }

    protected GradientDrawable roundedBackground(int color, int radius) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dpToPx(radius));
        return background;
    }

    protected TextView createText(String content, int size, int color, boolean bold) {
        TextView textView = new TextView(activity);
        textView.setText(content);
        textView.setTextSize(size);
        textView.setTextColor(color);
        if (bold) textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        textView.setLineSpacing(dpToPx(3), 1);
        return textView;
    }

    protected void addSpacing(LinearLayout parent, int sizeDp) {
        View view = new View(activity);
        parent.addView(view, new LinearLayout.LayoutParams(1, dpToPx(sizeDp)));
    }

    protected LinearLayout addCard(LinearLayout parent) {
        LinearLayout card = createColumn();
        card.setBackground(roundedBackground(0xffffffff, 20));
        setUniformPadding(card, 20);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dpToPx(14);
        parent.addView(card, layoutParams);
        return card;
    }

    protected TextView createButton(String label, boolean filled, Runnable action) {
        TextView textView = createText(label, 15, filled ? 0xffffffff : GREEN, true);
        textView.setGravity(Gravity.CENTER);
        textView.setMinHeight(dpToPx(49));
        textView.setBackground(roundedBackground(filled ? GREEN : 0xffe9f0e9, 14));
        textView.setOnClickListener(view -> action.run());
        textView.setContentDescription(label);
        return textView;
    }

    protected Button createBackButton(Runnable action) {
        Button button = new Button(activity);
        button.setText("‹ 返回");
        button.setContentDescription("返回服务目录");
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(GREEN);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dpToPx(14), 0, dpToPx(14), 0);
        button.setMinHeight(dpToPx(48));
        button.setMinWidth(dpToPx(80));
        button.setStateListAnimator(null);
        button.setElevation(0);
        button.setBackground(
                new RippleDrawable(
                        ColorStateList.valueOf(0x24146c57),
                        roundedBackground(0xffe9f0e9, 14),
                        null));
        button.setOnClickListener(view -> action.run());
        return button;
    }

    protected EditText createInput(String hint, boolean password) {
        EditText inputView = new EditText(activity);
        inputView.setSingleLine(true);
        inputView.setTextSize(16);
        inputView.setTextColor(INK);
        inputView.setHintTextColor(MUTED);
        inputView.setHint(hint);
        inputView.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        inputView.setBackground(roundedBackground(PAPER, 12));
        inputView.setInputType(
                password
                        ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                        : InputType.TYPE_CLASS_TEXT);
        inputView.setSaveEnabled(false);
        if (android.os.Build.VERSION.SDK_INT >= 26)
            inputView.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        return inputView;
    }
}
