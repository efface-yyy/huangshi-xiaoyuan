package com.smart.hgnu;

import android.os.Build;
import android.util.Base64;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP and JSON adapter for the recovered HGSF 3.2.0 protocol. Calls belong on CampusController's
 * worker. SessionStore encrypts session snapshots for process-restart recovery.
 */
public final class CampusApi {
    private static final int CONNECT_TIMEOUT_MILLIS = 12_000;
    private static final int READ_TIMEOUT_MILLIS = 20_000;
    private static final int MAX_RESPONSE_BYTES = 8 * 1024 * 1024;
    private byte[] key;
    private String uniqueCode = "";
    private JSONObject sessionUser = new JSONObject();
    private String deviceId = Long.toHexString(new SecureRandom().nextLong());
    private long serverTimeMillis;
    private long serverTimeReceivedAt;

    long currentServerTimeMillis() {
        return serverTimeMillis > 0
                ? serverTimeMillis
                        + (android.os.SystemClock.elapsedRealtime() - serverTimeReceivedAt)
                : System.currentTimeMillis();
    }

    public static class ApiException extends Exception {
        public final String code;

        public ApiException(String code, String message) {
            super(message);
            this.code = code;
        }
    }

    private String encrypt(String plaintext) throws Exception {
        return ProtocolCrypto.encryptAes(plaintext, key);
    }

    private String decrypt(String ciphertext) throws Exception {
        return ProtocolCrypto.decryptAes(ciphertext, key);
    }

    private JSONObject sendRequest(
            String method, String path, Map<String, String> headers, String body) throws Exception {
        HttpURLConnection connection =
                (HttpURLConnection) new URL(CampusEndpoints.BASE_URL + path).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod(method);
        connection.setRequestProperty("AppType", "ydxy");
        connection.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
        for (Map.Entry<String, String> entry : headers.entrySet())
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        try {
            if (body != null) {
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(bytes.length);
                try (java.io.OutputStream out = connection.getOutputStream()) {
                    out.write(bytes);
                }
            }
            int status = connection.getResponseCode();
            long responseDate = connection.getDate();
            if (responseDate > 0) {
                serverTimeMillis = responseDate;
                serverTimeReceivedAt = android.os.SystemClock.elapsedRealtime();
            }
            if (status < 200 || status >= 300)
                throw new ApiException("HTTP_" + status, "服务连接失败（HTTP " + status + "）");
            try (InputStream in = connection.getInputStream();
                    ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] bytes = new byte[8192];
                int read;
                while ((read = in.read(bytes)) != -1) {
                    out.write(bytes, 0, read);
                    if (out.size() > MAX_RESPONSE_BYTES) throw new ApiException("SIZE", "响应内容过大");
                }
                return new JSONObject(out.toString("UTF-8"));
            }
        } finally {
            connection.disconnect();
        }
    }

    private Object readSuccessfulData(JSONObject response) throws Exception {
        String code = response.optString("code", "UNKNOWN");
        if (!"0".equals(code)) throw new ApiException(code, response.optString("msg", "请求未成功"));
        return response.opt("data");
    }
    /** GET the public key, register a random AES key, then decrypt the returned Unique-Code. */
    private void establishSession() throws Exception {
        // Once renegotiation starts, an earlier code cannot be paired with a new key.
        if (key != null) java.util.Arrays.fill(key, (byte) 0);
        key = null;
        uniqueCode = "";
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Asym-Key", "public");
        String publicKey =
                String.valueOf(
                        readSuccessfulData(
                                sendRequest("GET", CampusEndpoints.KEY_EXCHANGE, headers, null)));
        byte[] candidateKey = new byte[16];
        new SecureRandom().nextBytes(candidateKey);
        boolean installed = false;
        try {
            String json =
                    new JSONObject()
                            .put("sk", Base64.encodeToString(candidateKey, Base64.NO_WRAP))
                            .toString();
            headers.put("Asym-Key", "private");
            String body = ProtocolCrypto.encryptKeyExchange(json, publicKey);
            Object encryptedCode =
                    readSuccessfulData(
                            sendRequest("POST", CampusEndpoints.KEY_EXCHANGE, headers, body));
            if (!(encryptedCode instanceof String) || ((String) encryptedCode).isEmpty())
                throw new ApiException("KEY", "无法建立安全会话");
            String candidateCode = ProtocolCrypto.decryptAes((String) encryptedCode, candidateKey);
            if (candidateCode.isEmpty()) throw new ApiException("KEY", "无法建立安全会话");
            key = candidateKey;
            uniqueCode = candidateCode;
            installed = true;
        } finally {
            if (!installed) java.util.Arrays.fill(candidateKey, (byte) 0);
        }
    }

    private Map<String, String> createAccessHeaders() throws Exception {
        JSONObject token = new JSONObject();
        token.put("userId", JsonFields.text(sessionUser, "userId"))
                .put("userType", JsonFields.text(sessionUser, "userType"));
        token.put("sessionKey", JsonFields.text(sessionUser, "session_key"))
                .put("xxdm", JsonFields.text(sessionUser, "schoolId"));
        token.put("clientVersion", CampusEndpoints.PROTOCOL_CLIENT_VERSION)
                .put("deviceModel", Build.MODEL);
        token.put("currentTime", String.valueOf(System.currentTimeMillis()));
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Access-Token", encrypt(token.toString()));
        headers.put("Unique-Code", uniqueCode);
        return headers;
    }
    /**
     * POST bodies and Access-Token are ciphertext; GET parameters use a single encrypted data
     * field.
     */
    private synchronized Object requestEncrypted(String method, String path, JSONObject payload)
            throws Exception {
        try {
            return requestWithCurrentKey(method, path, payload);
        } catch (ApiException exception) {
            // Official NetUtils treats 0006 as a transport-key failure, not a logged-out user.
            // Rebuild the request from plaintext once; never recursively re-encrypt its body.
            if (!"0006".equals(exception.code)) throw exception;
            establishSession();
            return requestWithCurrentKey(method, path, payload);
        }
    }

    private Object requestWithCurrentKey(String method, String path, JSONObject payload)
            throws Exception {
        if (key == null || uniqueCode.isEmpty()) establishSession();
        String body = null;
        if (payload != null) {
            String encrypted = encrypt(payload.toString());
            if ("GET".equals(method)) path += "?data=" + URLEncoder.encode(encrypted, "UTF-8");
            else body = encrypted;
        }
        Object data = readSuccessfulData(sendRequest(method, path, createAccessHeaders(), body));
        if (data instanceof String && !((String) data).isEmpty()) {
            String plain = decrypt((String) data);
            if (plain.isEmpty()) return "";
            return new JSONTokener(plain).nextValue();
        }
        return data;
    }

    public synchronized JSONObject login(String account, String password) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("userId", account)
                .put("passw", password)
                .put("schoolId", CampusEndpoints.SCHOOL_ID);
        payload.put("city", "")
                .put("province", "")
                .put("clientVersion", CampusEndpoints.PROTOCOL_CLIENT_VERSION)
                .put("serverApiVersion", CampusEndpoints.PROTOCOL_CLIENT_VERSION);
        payload.put("androidClientVersionCode", CampusEndpoints.PROTOCOL_VERSION_CODE)
                .put("deviceModel", Build.MODEL)
                .put("deviceType", "1");
        payload.put("deviceVersion", Build.VERSION.RELEASE).put("deviceId", deviceId);
        payload.put("latitude", "").put("longitude", "").put("imei", "");
        Object data = requestEncrypted("POST", CampusEndpoints.LOGIN, payload);
        if (!(data instanceof JSONObject)) throw new ApiException("FORMAT", "登录数据格式异常");
        JSONObject candidate = (JSONObject) data;
        if (JsonFields.text(candidate, "session_key").isEmpty()
                || JsonFields.text(candidate, "userId").isEmpty())
            throw new ApiException("VERIFY", "登录尚需进一步验证，请使用学校官方入口完成验证");
        sessionUser = candidate;
        return sessionUser;
    }

    Object getNavigation() throws Exception {
        return requestEncrypted("GET", CampusEndpoints.NAVIGATION, null);
    }

    Object getServices() throws Exception {
        // allList includes disabled and demonstration apps; list is the official account catalog.
        Object data =
                requestEncrypted(
                        "GET",
                        CampusEndpoints.SERVICES,
                        new JSONObject()
                                .put("zdlx", "3")
                                .put("khdbb", CampusEndpoints.PROTOCOL_CLIENT_VERSION)
                                .put("type", "1"));
        if (!(data instanceof org.json.JSONArray)) throw new ApiException("FORMAT", "服务目录格式异常");
        org.json.JSONArray services = new org.json.JSONArray();
        org.json.JSONArray groups = (org.json.JSONArray) data;
        for (int i = 0; i < groups.length(); i++) {
            JSONObject group = groups.optJSONObject(i);
            if (group == null) continue;
            org.json.JSONArray items = group.optJSONArray("DATA");
            if (items == null) continue;
            for (int j = 0; j < items.length(); j++) {
                JSONObject item = items.optJSONObject(j);
                if (item == null) continue;
                item.put("serviceCategory", JsonFields.text(group, "YYFL"));
                services.put(item);
            }
        }
        return services;
    }

    Object getGradeYears() throws Exception {
        return requestEncrypted("GET", "/api/grade/schoolYears", null);
    }

    Object getGrades(String year) throws Exception {
        return requestEncrypted("GET", "/api/grade/gradeList", new JSONObject().put("xn", year));
    }

    Object getSemesters() throws Exception {
        return requestEncrypted("GET", CampusEndpoints.SEMESTERS, null);
    }

    Object getCourses(JSONObject semester) throws Exception {
        JSONObject parameters =
                new JSONObject()
                        .put("xn", JsonFields.text(semester, "xn"))
                        .put("xq", JsonFields.text(semester, "xq"));
        return requestEncrypted("GET", CampusEndpoints.COURSES, parameters);
    }

    /** Match official LX=2 springboard and LX=4/6 direct-H5 routing without opening a browser. */
    synchronized WebLaunchPlan prepareService(JSONObject service) throws Exception {
        String type = JsonFields.text(service, "LX");
        String id = JsonFields.text(service, "YYID");
        String url;
        try {
            url = ServiceRouting.resolve(type, id, JsonFields.text(service, "URL"));
        } catch (IllegalArgumentException exception) {
            throw new ApiException("SERVICE", exception.getMessage());
        }
        Map<String, String> headers = new LinkedHashMap<>();
        Map<String, String> cookies = new LinkedHashMap<>();
        String cookieOrigin = "";
        // Only the known campus service origin receives campus authentication material.
        // Redirects to third-party sites rely on the springboard's own SSO, never blanket headers.
        if (ServiceRouting.isCampusCredentialOrigin(url)) {
            if (key == null || uniqueCode.isEmpty()) establishSession();
            JSONObject token =
                    new JSONObject()
                            .put("userId", JsonFields.text(sessionUser, "userId"))
                            .put("userType", JsonFields.text(sessionUser, "userType"))
                            .put("sessionKey", JsonFields.text(sessionUser, "session_key"))
                            .put("xxdm", JsonFields.text(sessionUser, "schoolId"));
            String accessToken = encrypt(token.toString());
            headers.put("Access-Token", accessToken);
            headers.put("Unique-Code", uniqueCode);
            headers.put("AppType", "ydxy");
            // Light apps authenticate through the springboard headers and redirect parameters.
            // Root campus cookies override commonmodule's own auth and cause error 0006.
            // Expire cookies left by older builds before launching; direct H5 still needs them.
            boolean springboard = "2".equals(type);
            cookies.put("accessToken", springboard ? "" : accessToken);
            cookies.put("uniqueCode", springboard ? "" : uniqueCode);
            cookies.put("AppType", springboard ? "" : "ydxy");
            cookieOrigin = CampusEndpoints.WEB_ORIGIN + "/";
        }
        return new WebLaunchPlan(url, cookieOrigin, headers, cookies);
    }

    /**
     * Only UI fields and authentication identifiers are retained; password-like fields are omitted.
     */
    synchronized JSONObject sessionSnapshot() throws Exception {
        JSONObject profile = new JSONObject();
        String[] fields = {
            "userId",
            "userType",
            "session_key",
            "schoolId",
            "userName",
            "schoolName",
            "department",
            "professionName",
            "className",
            "grade",
            "identity",
            "mobileNo"
        };
        for (String field : fields) profile.put(field, JsonFields.text(sessionUser, field));
        return new JSONObject()
                .put("version", 1)
                .put("school", CampusEndpoints.SCHOOL_ID)
                .put("user", profile)
                .put("aesKey", Base64.encodeToString(key, Base64.NO_WRAP))
                .put("uniqueCode", uniqueCode)
                .put("deviceId", deviceId);
    }

    synchronized JSONObject restoreSession(JSONObject snapshot) throws Exception {
        if (snapshot.getInt("version") != 1)
            throw new IllegalArgumentException("Unknown session version");
        JSONObject profile = snapshot.getJSONObject("user");
        byte[] savedKey = Base64.decode(snapshot.getString("aesKey"), Base64.NO_WRAP);
        String savedCode = snapshot.getString("uniqueCode");
        if (savedKey.length != 16
                || savedCode.isEmpty()
                || JsonFields.text(profile, "userId").isEmpty()
                || JsonFields.text(profile, "session_key").isEmpty()
                || JsonFields.text(profile, "schoolId").isEmpty()
                || !CampusEndpoints.SCHOOL_ID.equals(snapshot.optString("school")))
            throw new IllegalArgumentException("Incomplete saved session");
        key = savedKey;
        uniqueCode = savedCode;
        deviceId = snapshot.getString("deviceId");
        sessionUser = profile;
        return profile;
    }

    public synchronized void clear() {
        if (key != null) java.util.Arrays.fill(key, (byte) 0);
        key = null;
        uniqueCode = "";
        sessionUser = new JSONObject();
    }
}
