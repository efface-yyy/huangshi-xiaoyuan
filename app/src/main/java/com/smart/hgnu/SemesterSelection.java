package com.smart.hgnu;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Selects the semester covering today, falling back to the server's first entry. */
final class SemesterSelection {
    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

    private SemesterSelection() {}

    static JSONObject current(JSONArray semesters, long nowMillis) {
        // dqxq means CURRENT SEMESTER, not current teaching week.
        for (int index = 0; index < semesters.length(); index++) {
            JSONObject semester = semesters.optJSONObject(index);
            if ("1".equals(JsonFields.text(semester, "dqxq"))) return semester;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        for (int index = 0; index < semesters.length(); index++) {
            JSONObject semester = semesters.optJSONObject(index);
            try {
                Date start = format.parse(JsonFields.text(semester, "ksrq"));
                Date end = format.parse(JsonFields.text(semester, "jsrq"));
                if (start != null
                        && end != null
                        && nowMillis >= start.getTime()
                        && nowMillis <= end.getTime() + DAY_MILLIS) return semester;
            } catch (java.text.ParseException ignored) {
                // One malformed date must not hide the remaining semester options.
            }
        }
        JSONObject first = semesters.optJSONObject(0);
        return first != null ? first : new JSONObject();
    }
}
