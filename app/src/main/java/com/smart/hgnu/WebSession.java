package com.smart.hgnu;

import android.webkit.CookieManager;
import android.webkit.WebStorage;

/** Clears embedded-web login state when the campus account is changed or signed out. */
final class WebSession {
    private WebSession() {}

    static void clear(Runnable completed) {
        CookieManager manager = CookieManager.getInstance();
        manager.removeAllCookies(
                removed -> {
                    manager.flush();
                    WebStorage.getInstance().deleteAllData();
                    if (completed != null) completed.run();
                });
    }
}
