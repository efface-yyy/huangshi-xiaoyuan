package com.smart.hgnu;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Short-lived browser launch information. Never serialized into an Intent, log, or saved state. */
final class WebLaunchPlan {
    final String url;
    final String cookieOrigin;
    final Map<String, String> headers;
    // An empty value expires a legacy root cookie before navigation.
    final Map<String, String> cookies;

    WebLaunchPlan(
            String url,
            String cookieOrigin,
            Map<String, String> headers,
            Map<String, String> cookies) {
        this.url = url;
        this.cookieOrigin = cookieOrigin;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        this.cookies = Collections.unmodifiableMap(new LinkedHashMap<>(cookies));
    }
}
