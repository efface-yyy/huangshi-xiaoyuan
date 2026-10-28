package com.smart.hgnu;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

/** Hosts the page shell and forwards user actions to CampusController. */
public final class MainActivity extends Activity implements ScreenActions {
    private static final String SAVED_PAGE = "page";
    private static final String SAVED_WEEKDAY = "day";
    private static final String SAVED_QUERY = "serviceQuery";
    private final NavigationState navigation = new NavigationState();
    private final Runnable observer = this::redraw;
    private CampusState state;
    private UiComponents ui;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        state = (CampusState) getApplication();
        ui = new UiComponents(this);
        state.observe(observer);
        restoreNavigation(savedState);
        getWindow().setStatusBarColor(UiComponents.PAPER);
        getWindow().setNavigationBarColor(0xffffffff);
        redraw();
    }

    private void restoreNavigation(Bundle savedState) {
        if (savedState == null) return;
        navigation.selectedPage = Page.fromSavedIndex(savedState.getInt(SAVED_PAGE));
        navigation.selectedWeekday = savedState.getInt(SAVED_WEEKDAY, -1);
        navigation.serviceQuery = savedState.getString(SAVED_QUERY, "");
        navigation.selectedWeek = savedState.getInt("teachingWeek", 0);
        navigation.timetableSemester = savedState.getString("timetableSemester", "");
        navigation.timetableTableMode = savedState.getBoolean("timetableTableMode", true);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);
        savedState.putInt(SAVED_PAGE, navigation.selectedPage.ordinal());
        savedState.putInt(SAVED_WEEKDAY, navigation.selectedWeekday);
        savedState.putString(SAVED_QUERY, navigation.serviceQuery);
        savedState.putInt("teachingWeek", navigation.selectedWeek);
        savedState.putString("timetableSemester", navigation.timetableSemester);
        savedState.putBoolean("timetableTableMode", navigation.timetableTableMode);
    }

    @Override
    protected void onDestroy() {
        state.stopObserving(observer);
        super.onDestroy();
    }

    @Override
    public void redraw() {
        if (isFinishing() || isDestroyed()) return;
        String notice = state.takeNotice();
        if (!notice.isEmpty()) Toast.makeText(this, notice, Toast.LENGTH_SHORT).show();
        LinearLayout root = ui.createColumn();
        root.setBackgroundColor(UiComponents.PAPER);
        setContentView(root);
        if (state.isRestoringSession) {
            root.setGravity(Gravity.CENTER);
            root.addView(new ProgressBar(this));
            ui.addSpacing(root, 16);
            root.addView(ui.createText("正在恢复校园会话…", 14, UiComponents.MUTED, false));
            return;
        }
        if (!state.loggedIn) {
            new LoginScreen(this, state, this).renderInto(root);
            return;
        }
        addToolbar(root);
        PullToRefreshLayout refreshLayout = new PullToRefreshLayout(this, this::refresh);
        LinearLayout pageContainer = ui.createColumn();
        refreshLayout.setContent(pageContainer);
        refreshLayout.setRefreshing(state.isLoading);
        root.addView(refreshLayout, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout body =
                isFittedTimetable()
                        ? addFittedBody(pageContainer)
                        : addScrollableBody(pageContainer);
        createScreen(body).render();
        addBottomNavigation(root);
    }

    private void addToolbar(LinearLayout root) {
        LinearLayout toolbar = ui.createRow();
        toolbar.setPadding(ui.dpToPx(24), ui.dpToPx(14), ui.dpToPx(24), ui.dpToPx(12));
        TextView brand = ui.createText("HGNU  /  黄师校园", 14, UiComponents.INK, true);
        brand.setLetterSpacing(.08f);
        toolbar.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(toolbar);
    }

    private boolean isFittedTimetable() {
        return navigation.selectedPage == Page.TIMETABLE && navigation.timetableTableMode;
    }

    private LinearLayout addFittedBody(LinearLayout root) {
        LinearLayout body = ui.createColumn();
        body.setPadding(ui.dpToPx(8), ui.dpToPx(4), ui.dpToPx(8), ui.dpToPx(4));
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        return body;
    }

    private LinearLayout addScrollableBody(LinearLayout root) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout body = ui.createColumn();
        body.setPadding(ui.dpToPx(24), ui.dpToPx(18), ui.dpToPx(24), ui.dpToPx(20));
        scroll.addView(body);
        return body;
    }

    private BaseScreen createScreen(LinearLayout body) {
        switch (navigation.selectedPage) {
            case TIMETABLE:
                return new TimetableScreen(this, state, navigation, this, body);
            case SERVICES:
                return new ServicesScreen(this, state, navigation, this, body);
            case PROFILE:
                return new ProfileScreen(this, state, navigation, this, body);
            default:
                return new HomeScreen(this, state, navigation, this, body);
        }
    }

    private void addBottomNavigation(LinearLayout root) {
        LinearLayout bar = ui.createRow();
        bar.setBackgroundColor(0xffffffff);
        bar.setPadding(ui.dpToPx(12), ui.dpToPx(8), ui.dpToPx(12), ui.dpToPx(10));
        for (Page page : Page.values()) {
            boolean selected = navigation.selectedPage == page;
            int color = selected ? UiComponents.GREEN : UiComponents.MUTED;
            LinearLayout item = ui.createColumn();
            item.setGravity(Gravity.CENTER);
            ui.setUniformPadding(item, 6);
            if (selected) item.setBackground(ui.roundedBackground(0xffeef4ed, 14));
            item.addView(
                    new NavigationIconView(this, page.ordinal(), color),
                    new LinearLayout.LayoutParams(ui.dpToPx(24), ui.dpToPx(24)));
            ui.addSpacing(item, 3);
            TextView label = ui.createText(page.label, 11, color, selected);
            label.setGravity(Gravity.CENTER);
            item.addView(label);
            item.setContentDescription(page.label);
            item.setOnClickListener(view -> navigateTo(page));
            bar.addView(item, new LinearLayout.LayoutParams(0, ui.dpToPx(58), 1));
        }
        root.addView(bar);
    }

    @Override
    public void navigateTo(Page page) {
        navigation.selectedPage = page;
        navigation.serviceQuery = "";
        redraw();
    }

    @Override
    public void refresh() {
        state.controller.refresh(true);
    }

    @Override
    public void selectSemester(JSONObject semester) {
        state.controller.selectSemester(semester);
    }

    @Override
    public void signIn(String account, String password) {
        navigation.selectedPage = Page.HOME;
        state.controller.signIn(account, password);
    }

    @Override
    public void signOut() {
        navigation.selectedPage = Page.HOME;
        navigation.serviceQuery = "";
        state.controller.signOut();
    }

    @Override
    public void onBackPressed() {
        if (state.loggedIn && navigation.selectedPage != Page.HOME) navigateTo(Page.HOME);
        else super.onBackPressed();
    }
}
