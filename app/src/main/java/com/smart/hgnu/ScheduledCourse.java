package com.smart.hgnu;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A display record for a single week's meeting, independent of the raw API field names. */
final class ScheduledCourse {
    final String title, location, teacher, weeks;
    final int weekday, firstPeriod, lastPeriod;

    private ScheduledCourse(JSONObject record) {
        title = JsonFields.text(record, "kcmc");
        location = JsonFields.text(record, "skdd");
        String teacherValue = JsonFields.text(record, "skdx");
        teacher = teacherValue.isEmpty() ? JsonFields.text(record, "skls") : teacherValue;
        weeks = JsonFields.text(record, "qsjsz");
        weekday = record.optInt("xqj", 0);
        firstPeriod = record.optInt("ksjc", 0);
        lastPeriod = record.optInt("jsjc", firstPeriod);
    }

    static List<ScheduledCourse> forWeek(JSONArray records, int week) {
        List<ScheduledCourse> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int index = 0; index < records.length(); index++) {
            JSONObject record = records.optJSONObject(index);
            if (record == null
                    || !CourseWeeks.contains(
                            record.optInt("djz", 0), JsonFields.text(record, "qsjsz"), week))
                continue;
            ScheduledCourse course = new ScheduledCourse(record);
            if (course.weekday < 1
                    || course.weekday > 7
                    || course.firstPeriod < 1
                    || course.lastPeriod < course.firstPeriod) continue;
            // Malformed server records must not create unbounded table views.
            if (course.lastPeriod > 24) continue;
            String identity =
                    course.weekday
                            + "|"
                            + course.firstPeriod
                            + "|"
                            + course.lastPeriod
                            + "|"
                            + course.title
                            + "|"
                            + course.location
                            + "|"
                            + course.teacher;
            if (seen.add(identity)) result.add(course);
        }
        result.sort(
                Comparator.comparingInt((ScheduledCourse c) -> c.weekday)
                        .thenComparingInt(c -> c.firstPeriod)
                        .thenComparing(c -> c.title));
        return result;
    }
}
