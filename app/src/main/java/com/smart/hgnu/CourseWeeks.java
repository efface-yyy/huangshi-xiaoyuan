package com.smart.hgnu;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** djz is authoritative. Range parsing is only for records without an expanded week field. */
final class CourseWeeks {
    private static final Pattern WEEK_RANGE = Pattern.compile("(\\d+)(?:\\s*[-~至]\\s*(\\d+))?");

    private CourseWeeks() {}

    static boolean contains(int expandedWeek, String range, int selectedWeek) {
        if (expandedWeek > 0) return expandedWeek == selectedWeek;
        if (range == null || range.trim().isEmpty() || selectedWeek < 1) return false;
        boolean odd = range.contains("单"), even = range.contains("双");
        if (odd && !even && selectedWeek % 2 == 0) return false;
        if (even && !odd && selectedWeek % 2 != 0) return false;
        Matcher matcher = WEEK_RANGE.matcher(range);
        while (matcher.find()) {
            int first = Integer.parseInt(matcher.group(1));
            int last = matcher.group(2) == null ? first : Integer.parseInt(matcher.group(2));
            if (selectedWeek >= first && selectedWeek <= last) return true;
        }
        return false;
    }
}
