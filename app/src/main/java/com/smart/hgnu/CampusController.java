package com.smart.hgnu;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Owns requests independently of any Activity. One worker serializes requests; only main-thread
 * callbacks update CampusState. Generation checks discard results belonging to an old session.
 */
final class CampusController {
    private final CampusState state;
    private final CampusApi api = new CampusApi();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler mainThread = new Handler(Looper.getMainLooper());
    private final SessionStore sessionStore;
    private int sessionGeneration;

    CampusController(CampusState state) {
        this.state = state;
        sessionStore = new SessionStore(state);
    }

    void restoreSavedSession() {
        int generation = sessionGeneration;
        state.isRestoringSession = true;
        worker.execute(
                () -> {
                    JSONObject userProfile = null;
                    String failure = "";
                    try {
                        JSONObject savedSession = sessionStore.load();
                        if (savedSession != null) userProfile = api.restoreSession(savedSession);
                    } catch (Exception exception) {
                        android.util.Log.w(
                                "CampusSession",
                                "Restore failed: " + exception.getClass().getSimpleName());
                        sessionStore.clear();
                        api.clear();
                        failure = "已保存的会话无法恢复，请重新登录";
                    }
                    JSONObject restoredProfile = userProfile;
                    String restoreFailure = failure;
                    mainThread.post(
                            () -> {
                                if (generation != sessionGeneration) return;
                                state.isRestoringSession = false;
                                if (restoredProfile == null) {
                                    state.loginError = restoreFailure;
                                    state.notifyChanged();
                                } else {
                                    state.userProfile = restoredProfile;
                                    state.loggedIn = true;
                                    refresh();
                                }
                            });
                });
    }

    private String saveSession(int revision) {
        try {
            sessionStore.save(api.sessionSnapshot(), revision);
            return "";
        } catch (Exception exception) {
            return "本次登录正常，但会话未能保存，关闭 App 后可能需要重新登录";
        }
    }

    void signIn(String account, String password) {
        if (state.isSigningIn || state.isRestoringSession || state.loggedIn) return;
        int generation = sessionGeneration;
        int revision = sessionStore.revision();
        state.loginAccount = account;
        state.loginError = "";
        state.isSigningIn = true;
        state.notifyChanged();
        worker.execute(
                () -> {
                    try {
                        JSONObject userProfile = api.login(account, password);
                        String persistenceWarning = saveSession(revision);
                        mainThread.post(
                                () -> {
                                    if (generation != sessionGeneration) return;
                                    WebSession.clear(
                                            () -> {
                                                if (generation != sessionGeneration) return;
                                                state.userProfile = userProfile;
                                                state.isSigningIn = false;
                                                state.loggedIn = true;
                                                state.persistenceWarning = persistenceWarning;
                                                refresh();
                                            });
                                });
                    } catch (Exception exception) {
                        api.clear();
                        mainThread.post(
                                () -> {
                                    if (generation != sessionGeneration) return;
                                    state.isSigningIn = false;
                                    if (ErrorMessages.isNetworkFailure(exception)) {
                                        state.pendingNotice = ErrorMessages.describe(exception);
                                        state.loginError = "";
                                    } else state.loginError = ErrorMessages.describe(exception);
                                    state.notifyChanged();
                                });
                    }
                });
    }

    void refresh() {
        refresh(false);
    }

    void refresh(boolean userInitiated) {
        if (state.isLoading || !state.loggedIn) return;
        int generation = sessionGeneration;
        int revision = sessionStore.revision();
        JSONArray cachedNavigation = state.navigationItems;
        JSONArray cachedServices = state.services;
        JSONArray cachedSemesters = state.semesters;
        JSONArray cachedCourses = state.courses;
        JSONObject cachedSemester = state.currentSemester;
        state.isLoading = true;
        state.notifyChanged();
        worker.execute(
                () -> {
                    RequestBatch batch = new RequestBatch();
                    JSONArray navigationItems =
                            batch.array("导航", api::getNavigation, cachedNavigation);
                    JSONArray services = batch.array("服务目录", api::getServices, cachedServices);
                    JSONArray semesters = batch.array("学期", api::getSemesters, cachedSemesters);
                    JSONObject semester = chooseRefreshedSemester(semesters, cachedSemester);
                    JSONArray previousCourses =
                            JsonFields.text(semester, "xlh")
                                            .equals(JsonFields.text(cachedSemester, "xlh"))
                                    ? cachedCourses
                                    : new JSONArray();
                    JSONArray courses =
                            batch.array("课表", () -> api.getCourses(semester), previousCourses);
                    String persistenceWarning = batch.sessionExpired ? "" : saveSession(revision);
                    mainThread.post(
                            () -> {
                                if (generation != sessionGeneration) return;
                                if (batch.sessionExpired) {
                                    expireSession();
                                    return;
                                }
                                state.navigationItems = navigationItems;
                                state.services = services;
                                state.semesters = semesters;
                                state.currentSemester = semester;
                                state.serverTimeMillis = api.currentServerTimeMillis();
                                state.serverTimeReceivedAt =
                                        android.os.SystemClock.elapsedRealtime();
                                state.courses = courses;
                                state.setErrors(batch.errors);
                                state.persistenceWarning = persistenceWarning;
                                if (!batch.errors.isEmpty())
                                    state.pendingNotice = batch.failureNotice();
                                else if (userInitiated) state.pendingNotice = "数据已同步";
                                state.isLoading = false;
                                state.notifyChanged();
                            });
                });
    }

    void selectSemester(JSONObject semester) {
        if (state.isLoading || !state.loggedIn || semester == null) return;
        int generation = sessionGeneration;
        int revision = sessionStore.revision();
        JSONObject previousSemester = state.currentSemester;
        JSONArray previousCourses = state.courses;
        state.currentSemester = semester;
        state.courses = new JSONArray();
        state.isLoading = true;
        state.notifyChanged();
        worker.execute(
                () -> {
                    RequestBatch batch = new RequestBatch();
                    JSONArray courses = loadCourses(batch, semester);
                    String persistenceWarning = batch.sessionExpired ? "" : saveSession(revision);
                    mainThread.post(
                            () -> {
                                if (generation != sessionGeneration) return;
                                if (batch.sessionExpired) {
                                    expireSession();
                                    return;
                                }
                                Map<String, String> errors = new LinkedHashMap<>(state.errors);
                                errors.remove("课表");
                                errors.putAll(batch.errors);
                                state.setErrors(errors);
                                if (batch.errors.containsKey("课表")) {
                                    state.currentSemester = previousSemester;
                                    state.courses = previousCourses;
                                    state.pendingNotice = batch.failureNotice();
                                } else state.courses = courses;
                                state.persistenceWarning = persistenceWarning;
                                state.isLoading = false;
                                state.notifyChanged();
                            });
                });
    }

    private JSONObject chooseRefreshedSemester(JSONArray semesters, JSONObject previous) {
        String previousId = JsonFields.text(previous, "xlh");
        if (!previousId.isEmpty()) {
            for (int index = 0; index < semesters.length(); index++) {
                JSONObject semester = semesters.optJSONObject(index);
                if (previousId.equals(JsonFields.text(semester, "xlh"))) return semester;
            }
        }
        return SemesterSelection.current(semesters, api.currentServerTimeMillis());
    }

    private JSONArray loadCourses(RequestBatch batch, JSONObject semester) {
        if (JsonFields.text(semester, "xn").isEmpty()) return new JSONArray();
        return batch.array("课表", () -> api.getCourses(semester));
    }

    interface ServiceCallback {
        void ready(WebLaunchPlan plan);

        void failed(String message);
    }

    void prepareService(JSONObject service, ServiceCallback callback) {
        if (!state.loggedIn) {
            callback.failed("请先登录校园账号");
            return;
        }
        int generation = sessionGeneration;
        int revision = sessionStore.revision();
        worker.execute(
                () -> {
                    try {
                        WebLaunchPlan plan = api.prepareService(service);
                        saveSession(revision);
                        mainThread.post(
                                () -> {
                                    if (generation == sessionGeneration && state.loggedIn)
                                        callback.ready(plan);
                                    else callback.failed("会话已失效，请重新登录");
                                });
                    } catch (Exception exception) {
                        mainThread.post(
                                () -> {
                                    if (generation == sessionGeneration
                                            && ErrorMessages.isExpiredSession(exception))
                                        expireSession();
                                    callback.failed(ErrorMessages.describe(exception));
                                });
                    }
                });
    }

    interface GradeCallback {
        void ready(Object data);

        void failed(String message);
    }

    void loadGrades(String year, GradeCallback callback) {
        int generation = sessionGeneration;
        if (!state.loggedIn) {
            callback.failed("请先登录校园账号");
            return;
        }
        worker.execute(
                () -> {
                    try {
                        Object data = year == null ? api.getGradeYears() : api.getGrades(year);
                        mainThread.post(
                                () -> {
                                    if (generation == sessionGeneration && state.loggedIn)
                                        callback.ready(data);
                                    else callback.failed("会话已失效，请重新登录");
                                });
                    } catch (Exception exception) {
                        mainThread.post(
                                () -> {
                                    if (generation != sessionGeneration) {
                                        callback.failed("会话已失效，请重新登录");
                                        return;
                                    }
                                    if (ErrorMessages.isExpiredSession(exception)) expireSession();
                                    callback.failed(ErrorMessages.describe(exception));
                                });
                    }
                });
    }

    void signOut() {
        sessionGeneration++;
        // Deletion and revision invalidation complete before showing the logged-out UI.
        sessionStore.clear();
        WebSession.clear(null);
        state.clearSession();
        // Serialize key cleanup with any request still running on the worker.
        worker.execute(api::clear);
        state.notifyChanged();
    }

    private void expireSession() {
        signOut();
        state.loginError = "会话已失效，请重新登录";
        state.notifyChanged();
    }

    private interface Request {
        Object execute() throws Exception;
    }

    /** Request errors remain worker-local until the completed batch is published. */
    private static final class RequestBatch {
        final Map<String, String> errors = new LinkedHashMap<>();
        boolean sessionExpired;
        boolean networkUnavailable;

        String failureNotice() {
            return networkUnavailable ? "网络异常，暂未更新数据" : "部分数据暂时无法同步";
        }

        JSONArray array(String module, Request request) {
            return array(module, request, new JSONArray());
        }

        JSONArray array(String module, Request request, JSONArray previous) {
            if (sessionExpired || networkUnavailable) return previous;
            try {
                return JsonFields.array(request.execute());
            } catch (Exception exception) {
                sessionExpired = ErrorMessages.isExpiredSession(exception);
                networkUnavailable = ErrorMessages.isNetworkFailure(exception);
                errors.put(module, ErrorMessages.describe(exception));
                return previous;
            }
        }
    }
}
