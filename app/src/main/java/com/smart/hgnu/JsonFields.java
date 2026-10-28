package com.smart.hgnu;

import org.json.JSONArray;
import org.json.JSONObject;

/** Null-safe access to the original API field names. */
final class JsonFields {
    private JsonFields() {}

    static String text(JSONObject object, String field) {
        return object == null || object.isNull(field) ? "" : object.optString(field, "");
    }

    static JSONArray array(Object value) {
        return value instanceof JSONArray ? (JSONArray) value : new JSONArray();
    }
}
