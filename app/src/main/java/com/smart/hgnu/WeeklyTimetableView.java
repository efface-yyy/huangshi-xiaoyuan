package com.smart.hgnu;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Accessible native course blocks spanning their actual periods in a Monday–Sunday grid. */
final class WeeklyTimetableView extends UiComponents {
    interface CourseClick {
        void open(ScheduledCourse course);
    }

    private final List<ScheduledCourse> courses;
    private final long monday, now;
    private final CourseClick click;
    private int axisWidth;
    private int headerHeight;
    private float rowHeight;
    private int periodCount;
    private int measuredGridWidth;
    private int measuredGridHeight;
    private final Map<String, Integer> courseColors = new LinkedHashMap<>();

    WeeklyTimetableView(
            Activity activity,
            List<ScheduledCourse> courses,
            JSONArray semesterCourses,
            long monday,
            long now,
            CourseClick click) {
        super(activity);
        this.courses = courses;
        this.monday = monday;
        this.now = now;
        this.click = click;
        // Use the entire semester, so changing weeks never changes a course's color.
        TreeSet<String> titles = new TreeSet<>();
        for (int i = 0; i < semesterCourses.length(); i++) {
            JSONObject record = semesterCourses.optJSONObject(i);
            if (record != null) titles.add(JsonFields.text(record, "kcmc"));
        }
        int index = 0;
        for (String title : titles) {
            float hue = index++ * 360f / Math.max(1, titles.size());
            courseColors.put(
                    title, android.graphics.Color.HSVToColor(new float[] {hue, .22f, .96f}));
        }
    }

    /** The parent gives this frame the remaining screen height; neither axis scrolls. */
    void addTo(LinearLayout body) {
        FrameLayout grid = new FrameLayout(activity);
        grid.setContentDescription("周课表表格");
        body.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1));
        grid.addOnLayoutChangeListener(
                (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                    int width = right - left, height = bottom - top;
                    if (width <= 0
                            || height <= 0
                            || (width == measuredGridWidth && height == measuredGridHeight)) return;
                    measuredGridWidth = width;
                    measuredGridHeight = height;
                    // Populate after the parent's layout pass so new children get measured.
                    grid.post(
                            () -> {
                                if (grid.getWidth() == width && grid.getHeight() == height)
                                    rebuild(grid, width, height);
                            });
                });
    }

    private void rebuild(FrameLayout grid, int width, int height) {
        grid.removeAllViews();
        periodCount = LessonTimes.PERIODS;
        for (ScheduledCourse course : courses)
            periodCount = Math.max(periodCount, course.lastPeriod);
        axisWidth = Math.max(1, Math.min(dpToPx(48), width / 8));
        headerHeight = Math.max(1, Math.min(dpToPx(32), height / (periodCount + 1)));
        rowHeight = (height - headerHeight) / (float) periodCount;
        int column = Math.max(1, (width - axisWidth) / 7);
        addHeaders(grid, column);
        addGridCells(grid, column, periodCount);
        addCourses(grid, column);
    }

    private int periodTop(int period) {
        return headerHeight + Math.round((period - 1) * rowHeight);
    }

    private int periodBottom(int period) {
        return headerHeight + Math.round(period * rowHeight);
    }

    private TextView fittedText(
            String value, int maxSp, int color, boolean bold, int width, int height, int lines) {
        TextView text = createText(value, maxSp, color, bold);
        text.setIncludeFontPadding(false);
        text.setLineSpacing(0, 1);
        text.setGravity(Gravity.CENTER);
        text.setMaxLines(lines);
        text.setEllipsize(android.text.TextUtils.TruncateAt.END);
        float scaledDensity = activity.getResources().getDisplayMetrics().scaledDensity;
        float px = Math.min(maxSp * scaledDensity, height / (Math.max(1, lines) * 1.2f));
        if (value.contains(":")) px = Math.min(px, width / 3.5f);
        text.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, Math.max(1, px));
        return text;
    }

    private void place(
            FrameLayout parent, android.view.View view, int left, int top, int width, int height) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        params.leftMargin = left;
        params.topMargin = top;
        parent.addView(view, params);
    }

    private GradientDrawable cellBackground(int color) {
        GradientDrawable background = roundedBackground(color, 0);
        background.setStroke(dpToPx(.4f), 0xffe4e9e0);
        return background;
    }

    private void addHeaders(FrameLayout grid, int column) {
        TextView axis = fittedText("节次\n时间", 10, MUTED, true, axisWidth, headerHeight, 2);
        axis.setGravity(Gravity.CENTER);
        axis.setBackground(cellBackground(0xffedf2e9));
        place(grid, axis, 0, 0, axisWidth, headerHeight);
        for (int day = 1; day <= 7; day++) {
            long date = AcademicCalendar.dateInWeek(monday, day);
            boolean today = AcademicCalendar.day(date).equals(AcademicCalendar.day(now));
            TextView label =
                    fittedText(
                            TimetableScreen.weekdayName(day)
                                    + "\n"
                                    + AcademicCalendar.shortDate(date),
                            10,
                            today ? 0xffffffff : INK,
                            true,
                            column,
                            headerHeight,
                            2);
            label.setGravity(Gravity.CENTER);
            label.setBackground(cellBackground(today ? GREEN : 0xffedf2e9));
            place(grid, label, axisWidth + (day - 1) * column, 0, column, headerHeight);
        }
    }

    private void addGridCells(FrameLayout grid, int column, int periods) {
        long sunday = AcademicCalendar.dateInWeek(monday, 7);
        boolean transition = LessonTimes.isSummer(monday) != LessonTimes.isSummer(sunday);
        for (int period = 1; period <= periods; period++) {
            String times =
                    LessonTimes.start(period, monday) + "\n" + LessonTimes.end(period, monday);
            if (transition && period >= 5)
                times =
                        "冬 "
                                + LessonTimes.start(
                                        period, LessonTimes.isSummer(monday) ? sunday : monday)
                                + "\n夏 "
                                + LessonTimes.start(
                                        period, LessonTimes.isSummer(monday) ? monday : sunday);
            TextView axis =
                    fittedText(
                            String.valueOf(period) + "\n" + times,
                            9,
                            MUTED,
                            false,
                            axisWidth,
                            periodBottom(period) - periodTop(period),
                            3);
            axis.setGravity(Gravity.CENTER);
            axis.setBackground(cellBackground(0xfff0f3ec));
            int top = periodTop(period);
            place(grid, axis, 0, top, axisWidth, (periodBottom(period) - periodTop(period)));
            for (int day = 1; day <= 7; day++) {
                TextView cell = new TextView(activity);
                cell.setBackground(cellBackground(0xfffcfdf9));
                cell.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                place(
                        grid,
                        cell,
                        axisWidth + (day - 1) * column,
                        top,
                        column,
                        (periodBottom(period) - periodTop(period)));
            }
        }
    }

    private void addCourses(FrameLayout grid, int column) {
        for (int day = 1; day <= 7; day++) {
            List<ScheduledCourse> daily = new ArrayList<>();
            for (ScheduledCourse course : courses) if (course.weekday == day) daily.add(course);
            // Greedy interval lanes keep overlapping classes visible instead of covering each
            // other.
            List<Integer> laneEnds = new ArrayList<>();
            List<Integer> lanes = new ArrayList<>();
            for (ScheduledCourse course : daily) {
                int lane = 0;
                while (lane < laneEnds.size() && laneEnds.get(lane) >= course.firstPeriod) lane++;
                if (lane == laneEnds.size()) laneEnds.add(course.lastPeriod);
                else laneEnds.set(lane, course.lastPeriod);
                lanes.add(lane);
            }
            for (int index = 0; index < daily.size(); index++) {
                ScheduledCourse course = daily.get(index);
                int laneWidth = column / Math.max(1, laneEnds.size());
                int left = axisWidth + (day - 1) * column + lanes.get(index) * laneWidth;
                int top = periodTop(course.firstPeriod);
                int height = (periodBottom(course.lastPeriod) - periodTop(course.firstPeriod));
                int blockWidth = Math.max(1, laneWidth - Math.min(dpToPx(2), laneWidth / 3));
                int blockHeight = Math.max(1, height - Math.min(dpToPx(4), height / 4));
                place(
                        grid,
                        courseBlock(course, blockWidth, blockHeight),
                        left + Math.min(dpToPx(1), laneWidth / 6),
                        top + Math.min(dpToPx(2), height / 8),
                        blockWidth,
                        blockHeight);
            }
        }
    }

    private LinearLayout courseBlock(ScheduledCourse course, int width, int height) {
        long date = AcademicCalendar.dateInWeek(monday, course.weekday);
        LinearLayout block = createColumn();
        int inset = Math.min(dpToPx(3), Math.max(0, height / 12));
        block.setPadding(inset, inset, inset, inset);
        int color = courseColors.get(course.title);
        block.setBackground(roundedBackground(color, 5));
        int available = Math.max(1, height - inset * 2);
        boolean showRoom = !course.location.isEmpty();
        int titleHeight = showRoom ? available * 55 / 100 : available;
        int titleLines = titleHeight >= dpToPx(40) ? 3 : titleHeight >= dpToPx(22) ? 2 : 1;
        TextView title =
                fittedText(
                        course.title,
                        11,
                        INK,
                        true,
                        Math.max(1, width - inset * 2),
                        titleHeight,
                        titleLines);
        block.addView(title, new LinearLayout.LayoutParams(-1, titleHeight));
        if (showRoom) {
            TextView room =
                    fittedRoom(
                            course.location,
                            Math.max(1, width - inset * 2),
                            Math.max(1, available - titleHeight));
            block.addView(
                    room, new LinearLayout.LayoutParams(-1, Math.max(1, available - titleHeight)));
        }
        block.setContentDescription(
                "课程："
                        + course.title
                        + "，"
                        + TimetableScreen.weekdayName(course.weekday)
                        + "，地点："
                        + course.location
                        + "，"
                        + LessonTimes.range(course.firstPeriod, course.lastPeriod, date));
        block.setOnClickListener(view -> click.open(course));
        return block;
    }

    /** Measure actual wrapped text instead of truncating every room to two lines. */
    private TextView fittedRoom(String value, int width, int height) {
        TextView room = createText(value, 9, GREEN, false);
        room.setIncludeFontPadding(false);
        room.setGravity(Gravity.CENTER);
        room.setLineSpacing(0, 1);
        room.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE);
        room.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE);
        float maximum = 9 * activity.getResources().getDisplayMetrics().scaledDensity;
        float low = 1, high = maximum;
        for (int i = 0; i < 12; i++) {
            float size = (low + high) / 2;
            room.getPaint().setTextSize(size);
            android.text.StaticLayout layout =
                    android.text.StaticLayout.Builder.obtain(
                                    value, 0, value.length(), room.getPaint(), width)
                            .setIncludePad(false)
                            .setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE)
                            .setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE)
                            .build();
            if (layout.getHeight() <= height) low = size;
            else high = size;
        }
        room.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, low);
        return room;
    }
}
