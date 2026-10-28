package com.smart.hgnu;

import android.app.Activity;
import android.widget.LinearLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Presentation for the home page. */
final class HomeScreen extends BaseScreen {
    HomeScreen(
            Activity activity,
            CampusState state,
            NavigationState navigationState,
            ScreenActions actions,
            LinearLayout body) {
        super(activity, state, navigationState, actions, body);
    }

    @Override
    void render() {
        String date = new SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(new Date());
        addPageHeading("你好，" + userDisplayName() + "。", date + "  /  黄冈师范学院");
        addWelcomeCard();
        addShortcuts();
        addCampusIdentity();
        addSyncSummary();
    }

    private void addWelcomeCard() {
        LinearLayout hero = addCard(body);
        hero.setBackground(roundedBackground(GREEN, 22));
        hero.addView(createText("YOUR CAMPUS, CONNECTED", 10, 0xffb9d6c4, true));
        addSpacing(hero, 16);
        hero.addView(createText("把校园，放进口袋。", 25, 0xffffffff, true));
        addSpacing(hero, 9);
        hero.addView(createText("今天也向着喜欢的自己，前进一步。", 12, 0xffd0e5d7, false));
        addSpacing(hero, 22);
        LinearLayout stats = createRow();
        addMetric(stats, String.valueOf(state.services.length()), "校园服务");
        addMetric(stats, String.valueOf(state.navigationItems.length()), "导航入口");
        addMetric(stats, String.valueOf(state.courses.length()), "课程记录");
        hero.addView(stats);
    }

    private void addShortcuts() {
        addSectionHeading("常用入口", "CAMPUS ESSENTIALS");
        LinearLayout shortcuts = createRow();
        addShortcut(shortcuts, "01", "我的课表", "查看学期课程", Page.TIMETABLE);
        addShortcut(shortcuts, "02", "服务大厅", "搜索校园应用", Page.SERVICES);
        body.addView(shortcuts);
        addSpacing(body, 22);
    }

    private void addCampusIdentity() {
        addSectionHeading("校园身份", "已登录");
        LinearLayout identity = addCard(body);
        identity.addView(createText(jsonText(state.userProfile, "department"), 17, INK, true));
        addSpacing(identity, 8);
        identity.addView(
                createText(
                        jsonText(state.userProfile, "professionName")
                                + "  ·  "
                                + jsonText(state.userProfile, "className"),
                        13,
                        MUTED,
                        false));
        addSpacing(identity, 12);
        identity.addView(
                createText("学号  " + jsonText(state.userProfile, "userId"), 12, GREEN, true));
    }

    private void addSyncSummary() {
        addSectionHeading("同步概览", state.isLoading ? "正在更新" : "实时接口");
        LinearLayout sync = addCard(body);
        sync.addView(createText(semesterLabel(), 14, INK, true));
        addSpacing(sync, 8);
        sync.addView(
                createText(
                        state.isLoading
                                ? "正在获取校园数据…"
                                : "已获取 "
                                        + state.services.length()
                                        + " 条目录记录与 "
                                        + state.courses.length()
                                        + " 条课程记录",
                        12,
                        MUTED,
                        false));
        if (!state.persistenceWarning.isEmpty()) {
            addSpacing(sync, 12);
            sync.addView(createText(state.persistenceWarning, 12, 0xffad6143, false));
        }
    }

    private void addMetric(LinearLayout row, String value, String label) {
        LinearLayout container = createColumn();
        container.addView(createText(value, 27, 0xffffffff, true));
        container.addView(createText(label, 11, 0xffc1decc, false));
        row.addView(container, new LinearLayout.LayoutParams(0, -2, 1));
    }

    private void addShortcut(
            LinearLayout row, String index, String title, String subtitle, Page target) {
        LinearLayout container = createColumn();
        setUniformPadding(container, 16);
        container.setBackground(roundedBackground(0xffffffff, 18));
        container.addView(createText(index, 12, GREEN, true));
        addSpacing(container, 14);
        container.addView(createText(title, 16, INK, true));
        addSpacing(container, 5);
        container.addView(createText(subtitle, 11, MUTED, false));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -2, 1);
        if (target == Page.TIMETABLE) layoutParams.rightMargin = dpToPx(10);
        row.addView(container, layoutParams);
        container.setOnClickListener(v -> actions.navigateTo(target));
    }
}
