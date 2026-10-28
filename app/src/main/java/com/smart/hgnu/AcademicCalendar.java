package com.smart.hgnu;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Academic weeks based on the server's semester dates, always in the campus timezone. */
final class AcademicCalendar {
    private static final TimeZone CAMPUS_ZONE = TimeZone.getTimeZone("Asia/Shanghai");
    private static final long DAY_MILLIS = 86_400_000L;

    private AcademicCalendar() {}

    static Calendar day(long millis) {
        Calendar day = Calendar.getInstance(CAMPUS_ZONE, Locale.CHINA);
        day.setTimeInMillis(millis);
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        return day;
    }

    static Calendar parse(String date) {
        if (date == null || date.length() < 10) return null;
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        parser.setTimeZone(CAMPUS_ZONE);
        parser.setLenient(false);
        try {
            return day(parser.parse(date.substring(0, 10)).getTime());
        } catch (ParseException ignored) {
            return null;
        }
    }

    static long weekStart(String semesterStart, int week) {
        Calendar monday = parse(semesterStart);
        if (monday == null || week < 1) return 0;
        int weekday = (monday.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        monday.add(Calendar.DATE, -weekday + (week - 1) * 7);
        return monday.getTimeInMillis();
    }

    static int currentWeek(String start, String end, long serverNow) {
        Calendar first = parse(start), last = parse(end), today = day(serverNow);
        if (first == null || last == null || today.before(first) || today.after(last)) return 0;
        return (int) ((today.getTimeInMillis() - weekStart(start, 1)) / DAY_MILLIS / 7) + 1;
    }

    static int weekCount(String start, String end) {
        Calendar last = parse(end);
        long first = weekStart(start, 1);
        if (last == null || first == 0 || last.getTimeInMillis() < first) return 0;
        return (int) ((last.getTimeInMillis() - first) / DAY_MILLIS / 7) + 1;
    }

    static long dateInWeek(long monday, int weekday) {
        Calendar result = day(monday);
        result.add(Calendar.DATE, weekday - 1);
        return result.getTimeInMillis();
    }

    static String shortDate(long date) {
        SimpleDateFormat format = new SimpleDateFormat("M/d", Locale.CHINA);
        format.setTimeZone(CAMPUS_ZONE);
        return format.format(new Date(date));
    }
}
