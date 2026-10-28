package com.smart.hgnu;

import android.app.Activity;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.Locale;

/** Presentation for the services page. */
final class ServicesScreen extends BaseScreen {
    ServicesScreen(
            Activity activity,
            CampusState state,
            NavigationState navigationState,
            ScreenActions actions,
            LinearLayout body) {
        super(activity, state, navigationState, actions, body);
    }

    @Override
    void render() {
        addPageHeading("校园服务", "找到你需要的校园入口。");
        EditText search = createInput("搜索服务，例如：图书馆、课表", false);
        search.setText(navigationState.serviceQuery);
        body.addView(search, new LinearLayout.LayoutParams(-1, dpToPx(50)));
        addSpacing(body, 16);
        TextView summary = createText("", 12, MUTED, false);
        body.addView(summary);
        addSpacing(body, 14);
        LinearLayout list = createColumn();
        body.addView(list);
        renderServiceList(list, summary);
        search.addTextChangedListener(
                new TextWatcher() {
                    public void beforeTextChanged(
                            CharSequence searchText, int start, int count, int after) {}

                    public void onTextChanged(
                            CharSequence searchText, int start, int before, int count) {
                        navigationState.serviceQuery = searchText.toString();
                        renderServiceList(list, summary);
                    }

                    public void afterTextChanged(Editable editable) {}
                });
    }

    private void renderServiceList(LinearLayout list, TextView summary) {
        list.removeAllViews();
        int count = 0;
        for (int index = 0; index < state.services.length(); index++) {
            JSONObject app = state.services.optJSONObject(index);
            if (app == null) continue;
            String title = jsonText(app, "YYMC");
            if (!title.toLowerCase(Locale.ROOT)
                    .contains(navigationState.serviceQuery.toLowerCase(Locale.ROOT))) continue;
            count++;
            LinearLayout serviceRow = createRow();
            setUniformPadding(serviceRow, 15);
            serviceRow.setBackground(roundedBackground(0xffffffff, 16));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
            layoutParams.bottomMargin = dpToPx(9);
            list.addView(serviceRow, layoutParams);
            TextView icon =
                    createText(title.isEmpty() ? "校" : title.substring(0, 1), 19, GREEN, true);
            icon.setGravity(Gravity.CENTER);
            int[] colors = {0xffe9efe2, 0xffe1ecea, 0xfff3eadb, 0xffe8e8f0};
            icon.setBackground(roundedBackground(colors[index % 4], 12));
            serviceRow.addView(icon, new LinearLayout.LayoutParams(dpToPx(45), dpToPx(45)));
            LinearLayout detail = createColumn();
            detail.setPadding(dpToPx(13), 0, dpToPx(8), 0);
            detail.addView(createText(title, 15, INK, true));
            String desc = jsonText(app, "YYMS");
            if (!desc.isEmpty()) {
                TextView descriptionLabel = createText(desc, 11, MUTED, false);
                descriptionLabel.setMaxLines(1);
                detail.addView(descriptionLabel);
            } else detail.addView(createText("校园服务", 11, MUTED, false));
            serviceRow.addView(detail, new LinearLayout.LayoutParams(0, -2, 1));
            serviceRow.addView(createText("›", 18, MUTED, false));
            serviceRow.setContentDescription("打开服务：" + title);
            serviceRow.setOnClickListener(view -> openService(app));
        }
        summary.setText(count + " 项服务");
        if (count == 0) {
            TextView emptyLabel =
                    createText(state.isLoading ? "正在同步服务目录…" : "没有匹配的服务", 14, MUTED, false);
            setUniformPadding(emptyLabel, 20);
            list.addView(emptyLabel);
        }
    }

    private void openService(JSONObject service) {
        String id = jsonText(service, "YYID");
        String type = jsonText(service, "LX");
        if ("timetable".equals(id)) {
            actions.navigateTo(Page.TIMETABLE);
            return;
        }
        if ("1".equals(type) || "5".equals(type)) {
            Toast.makeText(activity, "此服务为原生或独立应用功能，暂未接入", Toast.LENGTH_SHORT).show();
            return;
        }
        activity.startActivity(
                new Intent(activity, ServiceWebActivity.class)
                        .putExtra(ServiceWebActivity.EXTRA_SERVICE_ID, id));
    }
}
