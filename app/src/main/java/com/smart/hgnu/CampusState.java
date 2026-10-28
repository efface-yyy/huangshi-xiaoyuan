package com.smart.hgnu;

import android.app.Application;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main-thread view state. SessionStore separately encrypts the credentials needed to restore a
 * login after process death; page data is fetched again on startup.
 */
public final class CampusState extends Application {
    CampusController controller;
    JSONObject userProfile = new JSONObject();
    JSONArray navigationItems = new JSONArray();
    JSONArray services = new JSONArray();
    JSONArray semesters = new JSONArray();
    JSONArray courses = new JSONArray();
    JSONObject currentSemester = new JSONObject();
    Map<String, String> errors = Collections.emptyMap();
    boolean loggedIn;
    boolean isLoading;
    boolean isSigningIn;
    boolean isRestoringSession;
    String persistenceWarning = "";
    String pendingNotice = "";

    String takeNotice() {
        String notice = pendingNotice;
        pendingNotice = "";
        return notice;
    }

    long serverTimeMillis;
    long serverTimeReceivedAt;

    long campusNow() {
        return serverTimeMillis > 0
                ? serverTimeMillis
                        + (android.os.SystemClock.elapsedRealtime() - serverTimeReceivedAt)
                : System.currentTimeMillis();
    }

    String loginAccount = "";
    String loginError = "";
    private Runnable observer;

    @Override
    public void onCreate() {
        super.onCreate();
        controller = new CampusController(this);
        controller.restoreSavedSession();
    }

    void observe(Runnable newObserver) {
        observer = newObserver;
    }

    void stopObserving(Runnable oldObserver) {
        if (observer == oldObserver) observer = null;
    }

    void notifyChanged() {
        if (observer != null) observer.run();
    }

    void setErrors(Map<String, String> requestErrors) {
        errors = Collections.unmodifiableMap(new LinkedHashMap<>(requestErrors));
    }

    void clearSession() {
        userProfile = new JSONObject();
        navigationItems = new JSONArray();
        services = new JSONArray();
        semesters = new JSONArray();
        courses = new JSONArray();
        currentSemester = new JSONObject();
        errors = Collections.emptyMap();
        loggedIn = false;
        isLoading = false;
        isSigningIn = false;
        isRestoringSession = false;
        persistenceWarning = "";
        pendingNotice = "";
        loginAccount = "";
        loginError = "";
    }
}
