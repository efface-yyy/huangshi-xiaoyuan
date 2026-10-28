package com.smart.hgnu;

import java.io.IOException;
import java.net.SocketTimeoutException;

/** User-facing errors never include response bodies or credentials. */
final class ErrorMessages {
    private ErrorMessages() {}

    static String describe(Exception exception) {
        if (exception instanceof CampusApi.ApiException) {
            CampusApi.ApiException apiException = (CampusApi.ApiException) exception;
            return apiException.getMessage() + "（" + apiException.code + "）";
        }
        if (exception instanceof SocketTimeoutException) return "连接超时，请检查网络";
        if (exception instanceof IOException) return "暂时无法连接校园服务，请检查网络";
        return "暂时无法同步数据";
    }

    static boolean isNetworkFailure(Exception exception) {
        return exception instanceof IOException
                || (exception instanceof CampusApi.ApiException
                        && ((CampusApi.ApiException) exception).code.startsWith("HTTP_"));
    }

    static boolean isExpiredSession(Exception exception) {
        if (!(exception instanceof CampusApi.ApiException)) return false;
        String code = ((CampusApi.ApiException) exception).code;
        return "1000".equals(code) || "1003".equals(code) || "1006".equals(code);
    }
}
