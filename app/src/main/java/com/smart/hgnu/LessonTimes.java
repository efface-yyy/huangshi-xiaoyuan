package com.smart.hgnu;

import java.util.Calendar;

/** Exact class times transcribed from the user's timetable; dates are inclusive. */
final class LessonTimes {
    static final int PERIODS = 11;
    private static final String[] WINTER_START = {
        "08:00", "08:55", "10:00", "10:55", "14:00", "14:55", "16:00", "16:55", "19:00", "19:50",
        "20:40"
    };
    private static final String[] WINTER_END = {
        "08:45", "09:40", "10:45", "11:40", "14:45", "15:40", "16:45", "17:40", "19:45", "20:35",
        "21:25"
    };
    private static final String[] SUMMER_START = {
        "08:00", "08:55", "10:00", "10:55", "14:20", "15:15", "16:20", "17:15", "19:20", "20:10",
        "21:00"
    };
    private static final String[] SUMMER_END = {
        "08:45", "09:40", "10:45", "11:40", "15:05", "16:00", "17:05", "18:00", "20:05", "20:55",
        "21:45"
    };

    private LessonTimes() {}

    static boolean isSummer(long date) {
        int month = AcademicCalendar.day(date).get(Calendar.MONTH) + 1;
        return month >= 5 && month <= 9;
    }

    static String season(long date) {
        return isSummer(date) ? "夏秋作息" : "冬春作息";
    }

    static String start(int period, long date) {
        return period >= 1 && period <= PERIODS
                ? (isSummer(date) ? SUMMER_START : WINTER_START)[period - 1]
                : "—";
    }

    static String end(int period, long date) {
        return period >= 1 && period <= PERIODS
                ? (isSummer(date) ? SUMMER_END : WINTER_END)[period - 1]
                : "—";
    }

    static String range(int first, int last, long date) {
        if (first < 1 || last < first || last > PERIODS) return "作息时间未提供";
        return start(first, date) + "–" + end(last, date);
    }
}
