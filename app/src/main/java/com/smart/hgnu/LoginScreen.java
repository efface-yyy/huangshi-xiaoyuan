package com.smart.hgnu;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Login presentation. Requests live in the controller; passwords never enter saved state. */
final class LoginScreen extends UiComponents {
    private static final String LOGIN_LABEL = "登录并进入校园  →";
    private final CampusState state;
    private final ScreenActions actions;

    LoginScreen(Activity activity, CampusState state, ScreenActions actions) {
        super(activity);
        this.state = state;
        this.actions = actions;
    }

    void renderInto(LinearLayout root) {
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
        LinearLayout body = createColumn();
        body.setPadding(dpToPx(28), dpToPx(20), dpToPx(28), dpToPx(20));
        scroll.addView(body);
        addIntroduction(body);
        addLoginForm(body);
        addSpacing(body, 6);
        TextView note = createText("个人预览版 · 非学校官方客户端\n会话在本机加密保存，不保存登录密码", 11, MUTED, false);
        note.setGravity(Gravity.CENTER);
        body.addView(note);
    }

    private void addIntroduction(LinearLayout body) {
        TextView edition = createText("HGNU   /   CAMPUS COMPANION", 11, GREEN, true);
        edition.setLetterSpacing(.15f);
        body.addView(edition);
        addSpacing(body, 22);
        body.addView(
                new CampusIllustrationView(activity),
                new LinearLayout.LayoutParams(-1, dpToPx(112)));
        addSpacing(body, 18);
        body.addView(createText("你好，黄师同学。", 28, INK, true));
        addSpacing(body, 10);
        body.addView(createText("课表、校园服务与个人信息\n让校园生活，更从容一点。", 14, MUTED, false));
        addSpacing(body, 18);
    }

    private void addLoginForm(LinearLayout body) {
        LinearLayout form = addCard(body);
        form.addView(createText("登录校园账号", 18, INK, true));
        addSpacing(form, 16);
        EditText accountInput = createInput("学号 / 工号", false);
        accountInput.setContentDescription("账号");
        accountInput.setText(state.loginAccount);
        accountInput.setEnabled(!state.isSigningIn);
        form.addView(accountInput, new LinearLayout.LayoutParams(-1, dpToPx(52)));
        addSpacing(form, 12);
        EditText passwordInput = createInput("密码", true);
        passwordInput.setContentDescription("密码");
        passwordInput.setEnabled(!state.isSigningIn);
        form.addView(passwordInput, new LinearLayout.LayoutParams(-1, dpToPx(52)));
        addSpacing(form, 12);
        TextView status =
                createText(
                        statusText(), 12, state.loginError.isEmpty() ? MUTED : 0xffb25341, false);
        form.addView(status);
        addSpacing(form, 18);
        TextView submit =
                createButton(LOGIN_LABEL, true, () -> submit(accountInput, passwordInput, status));
        if (state.isSigningIn) {
            submit.setText("正在连接校园…");
            submit.setEnabled(false);
            submit.setAlpha(.6f);
        }
        form.addView(submit, new LinearLayout.LayoutParams(-1, dpToPx(50)));
    }

    private String statusText() {
        if (state.isSigningIn) return "正在建立会话，请稍候";
        return state.loginError.isEmpty() ? "使用智慧黄师账号登录" : state.loginError;
    }

    private void submit(EditText accountInput, EditText passwordInput, TextView status) {
        String account = accountInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        if (account.isEmpty() || password.isEmpty()) {
            status.setText("请填写账号和密码");
            return;
        }
        InputMethodManager keyboard =
                (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keyboard != null) keyboard.hideSoftInputFromWindow(passwordInput.getWindowToken(), 0);
        passwordInput.setText("");
        actions.signIn(account, password);
    }
}
