package com.smart.hgnu;

import android.app.Activity;
import android.widget.LinearLayout;

import org.json.JSONObject;

/** Shared page layout helpers. Network work stays in CampusController. */
abstract class BaseScreen extends UiComponents {
    protected final CampusState state;
    protected final NavigationState navigationState;
    protected final ScreenActions actions;
    protected final LinearLayout body;

    BaseScreen(
            Activity activity,
            CampusState state,
            NavigationState navigationState,
            ScreenActions actions,
            LinearLayout body) {
        super(activity);
        this.state = state;
        this.navigationState = navigationState;
        this.actions = actions;
        this.body = body;
    }

    abstract void render();

    protected void addPageHeading(String title, String subtitle) {
        body.addView(createText(title, 27, INK, true));
        addSpacing(body, 7);
        body.addView(createText(subtitle, 13, MUTED, false));
        addSpacing(body, 24);
    }

    protected void addSectionHeading(String title, String right) {
        LinearLayout headingRow = createRow();
        headingRow.addView(
                createText(title, 18, INK, true), new LinearLayout.LayoutParams(0, -2, 1));
        headingRow.addView(createText(right, 12, MUTED, false));
        body.addView(headingRow);
        addSpacing(body, 12);
    }

    protected String jsonText(JSONObject object, String key) {
        return JsonFields.text(object, key);
    }

    protected String userDisplayName() {
        String displayName = jsonText(state.userProfile, "userName");
        return displayName.isEmpty() ? "同学" : displayName;
    }

    protected String textOrDefault(String value, String other) {
        return value.isEmpty() ? other : value;
    }

    protected void addEmptyState(String title, String description) {
        LinearLayout card = addCard(body);
        addSpacing(card, 16);
        card.addView(createText(title, 18, INK, true));
        addSpacing(card, 10);
        card.addView(createText(description, 13, MUTED, false));
        addSpacing(card, 16);
    }

    protected String semesterLabel() {
        String xn = jsonText(state.currentSemester, "xn"),
                xq = jsonText(state.currentSemester, "xq");
        return xn.isEmpty() ? "学期信息同步中" : xn + " 学年 · 第 " + xq + " 学期";
    }
}
