package com.smart.hgnu;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Presentation for the profile page. */
final class ProfileScreen extends BaseScreen {
    ProfileScreen(
            Activity activity,
            CampusState state,
            NavigationState navigationState,
            ScreenActions actions,
            LinearLayout body) {
        super(activity, state, navigationState, actions, body);
    }

    @Override
    void render() {
        addPageHeading("我的校园", "与你有关的信息，都在这里。");
        LinearLayout container = addCard(body);
        TextView avatar = createText(userDisplayName().substring(0, 1), 29, GREEN, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(roundedBackground(0xffe7eee0, 20));
        container.addView(avatar, new LinearLayout.LayoutParams(dpToPx(68), dpToPx(68)));
        addSpacing(container, 17);
        container.addView(createText(userDisplayName(), 25, INK, true));
        addSpacing(container, 7);
        container.addView(createText(jsonText(state.userProfile, "userId"), 14, MUTED, false));
        addSpacing(container, 15);
        container.addView(
                createText(
                        textOrDefault(jsonText(state.userProfile, "schoolName"), "黄冈师范学院"),
                        12,
                        GREEN,
                        true));
        addSectionHeading("个人信息", "来自登录响应");
        LinearLayout info = addCard(body);
        addProfileField(info, "院系", "department");
        addProfileField(info, "专业", "professionName");
        addProfileField(info, "班级", "className");
        addProfileField(info, "年级", "grade");
        addProfileField(info, "身份", "identity");
        String phone = jsonText(state.userProfile, "mobileNo");
        if (phone.length() > 7)
            phone = phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
        addDetailRow(info, "手机号", phone);
        LinearLayout about = addCard(body);
        about.addView(createText("黄师 · 校园  0.1.0", 15, INK, true));
        addSpacing(about, 8);
        about.addView(createText("SmartHGNU_Unoffical\nDesigned by efface-yyy", 12, MUTED, false));
        addSpacing(body, 5);
        TextView synchronize = createButton("重新同步数据", false, actions::refresh);
        synchronize.setEnabled(!state.isLoading);
        synchronize.setAlpha(state.isLoading ? .5f : 1);
        body.addView(synchronize, new LinearLayout.LayoutParams(-1, dpToPx(48)));
        addSpacing(body, 12);
        body.addView(
                createButton(
                        "退出登录",
                        false,
                        () ->
                                new AlertDialog.Builder(activity)
                                        .setTitle("退出登录？")
                                        .setMessage("将清除本机保存的会话，下次需重新输入账号密码。")
                                        .setNegativeButton("取消", null)
                                        .setPositiveButton(
                                                "退出",
                                                (dialog, which) -> {
                                                    actions.signOut();
                                                })
                                        .show()),
                new LinearLayout.LayoutParams(-1, dpToPx(48)));
    }

    private void addProfileField(LinearLayout container, String label, String key) {
        addDetailRow(container, label, jsonText(state.userProfile, key));
    }

    private void addDetailRow(LinearLayout container, String label, String value) {
        LinearLayout detailRow = createRow();
        detailRow.setPadding(0, dpToPx(12), 0, dpToPx(12));
        detailRow.addView(
                createText(label, 13, MUTED, false), new LinearLayout.LayoutParams(dpToPx(70), -2));
        TextView valueLabel = createText(textOrDefault(value, "未提供"), 13, INK, false);
        valueLabel.setGravity(Gravity.END);
        detailRow.addView(valueLabel, new LinearLayout.LayoutParams(0, -2, 1));
        container.addView(detailRow);
        View line = new View(activity);
        line.setBackgroundColor(LINE);
        container.addView(line, new LinearLayout.LayoutParams(-1, dpToPx(1)));
    }
}
