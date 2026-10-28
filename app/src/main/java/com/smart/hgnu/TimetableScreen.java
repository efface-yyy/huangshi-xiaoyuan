package com.smart.hgnu;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Semester/week controls shared by a weekly grid and a filtered course list. */
final class TimetableScreen extends BaseScreen {
    private int displayWeek;
    private int currentWeek;
    private int totalWeeks;
    private long monday;
    private List<ScheduledCourse> weekCourses;

    TimetableScreen(
            Activity activity,
            CampusState state,
            NavigationState navigationState,
            ScreenActions actions,
            LinearLayout body) {
        super(activity, state, navigationState, actions, body);
    }

    @Override
    void render() {
        prepareWeek();
        if (!navigationState.timetableTableMode && navigationState.selectedWeekday < 0)
            navigationState.selectedWeekday = todayWeekday();
        addHeadingAndViewToggle();
        addCompactSelectors();
        if (monday == 0) {
            addEmptyState(state.isLoading ? "正在同步校历" : "暂无校历信息", "连接服务器后将自动获取当前学期与教学周。");
            return;
        }
        if (state.isLoading && state.courses.length() == 0) {
            addEmptyState("正在同步课表", "正在获取所选学期的课程。");
            return;
        }
        if (navigationState.timetableTableMode) {
            new WeeklyTimetableView(
                            activity,
                            weekCourses,
                            state.courses,
                            monday,
                            state.campusNow(),
                            this::showCourseDetails)
                    .addTo(body);
        } else {
            addWeekdayFilter();
            addCourseList();
        }
    }

    private void prepareWeek() {
        String signature =
                jsonText(state.currentSemester, "xn")
                        + "/"
                        + jsonText(state.currentSemester, "xq")
                        + "/"
                        + jsonText(state.currentSemester, "ksrq");
        if (!signature.equals(navigationState.timetableSemester)) {
            navigationState.timetableSemester = signature;
            navigationState.selectedWeek = 0;
            navigationState.selectedWeekday = -1;
        }
        String start = jsonText(state.currentSemester, "ksrq");
        String end = jsonText(state.currentSemester, "jsrq");
        currentWeek = AcademicCalendar.currentWeek(start, end, state.campusNow());
        totalWeeks = Math.max(1, AcademicCalendar.weekCount(start, end));
        for (int index = 0; index < state.courses.length(); index++) {
            JSONObject record = state.courses.optJSONObject(index);
            int week = record == null ? 0 : record.optInt("djz", 0);
            if (week <= 60) totalWeeks = Math.max(totalWeeks, week);
        }
        displayWeek =
                navigationState.selectedWeek > 0
                        ? navigationState.selectedWeek
                        : Math.max(1, currentWeek);
        displayWeek = Math.min(totalWeeks, displayWeek);
        monday = AcademicCalendar.weekStart(start, displayWeek);
        weekCourses = ScheduledCourse.forWeek(state.courses, displayWeek);
    }

    private void addHeadingAndViewToggle() {
        LinearLayout header = createRow();
        header.addView(createText("我的课表", 22, INK, true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView semesterButton = createSemesterButton();
        LinearLayout.LayoutParams semesterParams =
                new LinearLayout.LayoutParams(dpToPx(54), dpToPx(32));
        semesterParams.rightMargin = dpToPx(6);
        header.addView(semesterButton, semesterParams);
        LinearLayout toggle = createRow();
        toggle.setBackground(roundedBackground(0xffe6ede4, 12));
        setUniformPadding(toggle, 3);
        addModeButton(toggle, "列表", false);
        addModeButton(toggle, "表格", true);
        header.addView(toggle);
        body.addView(header);
        addSpacing(body, 8);
    }

    private void addModeButton(LinearLayout parent, String label, boolean tableMode) {
        boolean selected = navigationState.timetableTableMode == tableMode;
        TextView button = createText(label, 12, selected ? 0xffffffff : GREEN, true);
        button.setGravity(Gravity.CENTER);
        button.setContentDescription(label + "模式");
        button.setSelected(selected);
        if (selected) button.setBackground(roundedBackground(GREEN, 9));
        button.setOnClickListener(
                view -> {
                    if (!tableMode && navigationState.timetableTableMode)
                        navigationState.selectedWeekday = todayWeekday();
                    navigationState.timetableTableMode = tableMode;
                    actions.redraw();
                });
        parent.addView(button, new LinearLayout.LayoutParams(dpToPx(44), dpToPx(30)));
    }

    private TextView createSemesterButton() {
        List<String> semesterLabels = new ArrayList<>();
        int selectedSemester = 0;
        for (int index = 0; index < state.semesters.length(); index++) {
            JSONObject semester = state.semesters.optJSONObject(index);
            semesterLabels.add(
                    jsonText(semester, "xn") + " · 第 " + jsonText(semester, "xq") + " 学期");
            if (jsonText(semester, "xlh").equals(jsonText(state.currentSemester, "xlh")))
                selectedSemester = index;
        }
        TextView semester = compactButton("学期 ▾", "选择学期");
        int initialSemester = selectedSemester;
        semester.setOnClickListener(
                view -> {
                    if (state.isLoading || semesterLabels.isEmpty()) return;
                    new AlertDialog.Builder(activity)
                            .setTitle("选择学期")
                            .setSingleChoiceItems(
                                    semesterLabels.toArray(new String[0]),
                                    initialSemester,
                                    (dialog, position) -> {
                                        dialog.dismiss();
                                        if (position != initialSemester)
                                            actions.selectSemester(
                                                    state.semesters.optJSONObject(position));
                                    })
                            .setNegativeButton("取消", null)
                            .show();
                });
        return semester;
    }

    /** Week navigation uses larger arrow buttons now that the semester lives in the header. */
    private void addCompactSelectors() {
        LinearLayout controls = createRow();
        TextView previous = compactButton("‹", "上一周");
        previous.setTextSize(26);
        previous.setEnabled(displayWeek > 1);
        previous.setAlpha(displayWeek > 1 ? 1 : .3f);
        previous.setOnClickListener(view -> selectWeek(displayWeek - 1));
        controls.addView(previous, new LinearLayout.LayoutParams(dpToPx(48), dpToPx(40)));
        TextView week = compactButton("第 " + displayWeek + " 周 ▾", "选择教学周");
        week.setOnClickListener(
                view -> {
                    String[] labels = new String[totalWeeks];
                    for (int index = 0; index < totalWeeks; index++)
                        labels[index] =
                                "第 "
                                        + (index + 1)
                                        + " 周"
                                        + (index + 1 == currentWeek ? " · 本周" : "");
                    new AlertDialog.Builder(activity)
                            .setTitle("选择教学周")
                            .setSingleChoiceItems(
                                    labels,
                                    displayWeek - 1,
                                    (dialog, position) -> {
                                        dialog.dismiss();
                                        selectWeek(position + 1);
                                    })
                            .setNegativeButton("取消", null)
                            .show();
                });
        controls.addView(week, new LinearLayout.LayoutParams(0, dpToPx(40), 1));
        TextView next = compactButton("›", "下一周");
        next.setTextSize(26);
        next.setEnabled(displayWeek < totalWeeks);
        next.setAlpha(displayWeek < totalWeeks ? 1 : .3f);
        next.setOnClickListener(view -> selectWeek(displayWeek + 1));
        controls.addView(next, new LinearLayout.LayoutParams(dpToPx(48), dpToPx(40)));
        TextView current = compactButton("本周", "返回本周");
        current.setEnabled(currentWeek > 0);
        current.setAlpha(currentWeek > 0 ? 1 : .3f);
        current.setOnClickListener(view -> selectWeek(0));
        controls.addView(current, new LinearLayout.LayoutParams(dpToPx(48), dpToPx(40)));
        body.addView(controls);
        addSpacing(body, 8);
    }

    private TextView compactButton(String label, String description) {
        TextView button = createText(label, 11, GREEN, true);
        button.setSingleLine(true);
        button.setEllipsize(android.text.TextUtils.TruncateAt.END);
        button.setGravity(Gravity.CENTER);
        button.setContentDescription(description);
        button.setBackground(roundedBackground(0xffe9eee5, 9));
        return button;
    }

    private void selectWeek(int week) {
        navigationState.selectedWeek = week;
        actions.redraw();
    }

    private int todayWeekday() {
        java.util.Calendar today = AcademicCalendar.day(state.campusNow());
        return (today.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 + 1;
    }

    private void addWeekdayFilter() {
        String[] labels = {"全部", "一", "二", "三", "四", "五", "六", "日"};
        LinearLayout days = createRow();
        for (int index = 0; index < labels.length; index++) {
            int day = index;
            boolean selected = navigationState.selectedWeekday == day;
            TextView button = createText(labels[index], 12, selected ? 0xffffffff : GREEN, true);
            button.setGravity(Gravity.CENTER);
            button.setBackground(roundedBackground(selected ? GREEN : 0xffe8eee5, 10));
            button.setOnClickListener(
                    view -> {
                        navigationState.selectedWeekday = day;
                        actions.redraw();
                    });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dpToPx(38), 1);
            params.rightMargin = dpToPx(3);
            days.addView(button, params);
        }
        body.addView(days);
        addSpacing(body, 16);
    }

    private void addCourseList() {
        int visible = 0;
        for (ScheduledCourse course : weekCourses) {
            if (navigationState.selectedWeekday != 0
                    && course.weekday != navigationState.selectedWeekday) continue;
            visible++;
            long date = AcademicCalendar.dateInWeek(monday, course.weekday);
            LinearLayout card = addCard(body);
            card.addView(
                    createText(
                            weekdayName(course.weekday) + "  " + AcademicCalendar.shortDate(date),
                            12,
                            GREEN,
                            true));
            addSpacing(card, 9);
            card.addView(createText(course.title, 18, INK, true));
            addSpacing(card, 10);
            card.addView(
                    createText(
                            LessonTimes.range(course.firstPeriod, course.lastPeriod, date),
                            16,
                            GREEN,
                            true));
            addSpacing(card, 5);
            card.addView(
                    createText(
                            "第 "
                                    + course.firstPeriod
                                    + "–"
                                    + course.lastPeriod
                                    + " 节  ·  第 "
                                    + displayWeek
                                    + " 周",
                            12,
                            MUTED,
                            false));
            addSpacing(card, 8);
            card.addView(
                    createText("地点  " + textOrDefault(course.location, "未提供"), 13, MUTED, false));
            if (!course.teacher.isEmpty())
                card.addView(createText("教师  " + course.teacher, 13, MUTED, false));
            card.setOnClickListener(view -> showCourseDetails(course));
        }
        if (visible == 0) addEmptyState("当天暂无课程", "所选教学周和星期下没有课程记录。");
    }

    private void showCourseDetails(ScheduledCourse course) {
        long date = AcademicCalendar.dateInWeek(monday, course.weekday);
        String detail =
                "第 "
                        + displayWeek
                        + " 周 · "
                        + weekdayName(course.weekday)
                        + " "
                        + AcademicCalendar.shortDate(date)
                        + "\n"
                        + LessonTimes.range(course.firstPeriod, course.lastPeriod, date)
                        + "\n第 "
                        + course.firstPeriod
                        + "–"
                        + course.lastPeriod
                        + " 节 · "
                        + LessonTimes.season(date)
                        + "\n\n地点："
                        + textOrDefault(course.location, "未提供")
                        + "\n教师："
                        + textOrDefault(course.teacher, "未提供")
                        + "\n上课周次："
                        + course.weeks;
        new AlertDialog.Builder(activity)
                .setTitle(course.title)
                .setMessage(detail)
                .setPositiveButton("知道了", null)
                .show();
    }

    static String weekdayName(int day) {
        return new String[] {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"}[day];
    }
}
