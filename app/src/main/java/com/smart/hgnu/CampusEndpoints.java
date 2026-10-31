package com.smart.hgnu;

/** Paths and compatibility version values from the original client. */
final class CampusEndpoints {
    static final String BASE_URL = "http://ydxy.hgnu.edu.cn:8090/mobileapi_ydxy";
    static final String KEY_EXCHANGE = "/open/security/asym/key";
    static final String LOGIN = "/open/auth/login";
    static final String NAVIGATION = "/api/getUserTabs";
    static final String SERVICES = "/api/app/list";
    static final String SEMESTERS = "/api/schedule/termParams";
    static final String COURSES = "/api/schedule/scheduleOfTerm";
    static final String SERVICE_SPRINGBOARD = "/api/lapp/springboard";
    static final String WEB_ORIGIN = "http://ydxy.hgnu.edu.cn:8090";
    static final String SCHOOL_ID = "HGSF";
    static final String PROTOCOL_CLIENT_VERSION = "3.2.0";
    static final String PROTOCOL_VERSION_CODE = "21";

    private CampusEndpoints() {}
}
