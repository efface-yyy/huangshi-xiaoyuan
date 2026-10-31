package com.smart.hgnu;

/** Executable with javac/java; requires no Android device or account. */
public final class ServiceRoutingTest {
    private static int assertions;

    private static void check(boolean value) {
        assertions++;
        if (!value) throw new AssertionError("Assertion " + assertions + " failed");
    }

    private static void rejected(String type, String url, String expectedMessage) throws Exception {
        try {
            ServiceRouting.resolve(type, "service", url);
            throw new AssertionError("Expected rejection for " + type + ": " + url);
        } catch (IllegalArgumentException exception) {
            check(exception.getMessage().equals(expectedMessage));
        }
    }

    public static void main(String[] args) throws Exception {
        String springboard = ServiceRouting.resolve("2", "a b&c", "https://ignored.example");
        check(
                springboard.equals(
                        CampusEndpoints.BASE_URL
                                + CampusEndpoints.SERVICE_SPRINGBOARD
                                + "?yyid=a+b%26c"));
        check(ServiceRouting.isCampusCredentialOrigin(springboard));
        check(
                ServiceRouting.resolve("6", "x", "/path")
                        .equals(CampusEndpoints.WEB_ORIGIN + "/path"));
        check(
                ServiceRouting.resolve("4", "x", "//example.org/path")
                        .equals("http://example.org/path"));
        check(
                ServiceRouting.resolve("4", "x", "https://example.org/path")
                        .equals("https://example.org/path"));
        check(ServiceRouting.isCampusCredentialOrigin("HTTP://YDXY.HGNU.EDU.CN:8090/path"));
        check(!ServiceRouting.isCampusCredentialOrigin("http://ydxy.hgnu.edu.cn/path"));
        check(!ServiceRouting.isCampusCredentialOrigin("http://ydxy.hgnu.edu.cn:8080/path"));
        check(!ServiceRouting.isCampusCredentialOrigin("https://ydxy.hgnu.edu.cn:8090/path"));
        check(
                !ServiceRouting.isCampusCredentialOrigin(
                        "http://ydxy.hgnu.edu.cn.example.org:8090/path"));
        rejected("4", "http://ydxy.hgnu.edu.cn@evil.example:8090/", "该服务不是可内嵌的网页入口");
        rejected("4", "javascript:alert(1)", "该服务不是可内嵌的网页入口");
        rejected("4", "http://ydxy.hgnu.edu.cn:8090/contains space", "该服务不是可内嵌的网页入口");
        rejected("8", "https://example.org/", "第三方单点登录服务暂未接入");
        rejected("3", "https://example.org/", "该服务类型暂未接入");
        rejected("", "https://example.org/", "该服务类型暂未接入");
        System.out.println("Passed " + assertions + " service routing assertions.");
    }
}
