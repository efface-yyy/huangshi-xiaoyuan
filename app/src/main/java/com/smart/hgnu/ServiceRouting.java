package com.smart.hgnu;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

/** Resolves documented service types and confines campus credentials to their known origin. */
final class ServiceRouting {
    private ServiceRouting() {}

    static String resolve(String type, String id, String directUrl) throws Exception {
        String url;
        if ("2".equals(type)) {
            url =
                    CampusEndpoints.BASE_URL
                            + CampusEndpoints.SERVICE_SPRINGBOARD
                            + "?yyid="
                            + URLEncoder.encode(id, "UTF-8");
        } else if ("4".equals(type) || "6".equals(type)) {
            url = directUrl.trim();
            if (url.startsWith("//")) url = "http:" + url;
            else if (url.startsWith("/")) url = CampusEndpoints.WEB_ORIGIN + url;
        } else if ("8".equals(type)) {
            throw new IllegalArgumentException("第三方单点登录服务暂未接入");
        } else {
            throw new IllegalArgumentException("该服务类型暂未接入");
        }
        parseWebUrl(url);
        return url;
    }

    private static URI parseWebUrl(String url) {
        try {
            URI uri = new URI(url);
            if (!("http".equalsIgnoreCase(uri.getScheme())
                            || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null
                    || uri.getUserInfo() != null)
                throw new IllegalArgumentException("该服务不是可内嵌的网页入口");
            return uri;
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("该服务不是可内嵌的网页入口");
        }
    }

    static boolean isCampusCredentialOrigin(String url) {
        URI uri = parseWebUrl(url);
        URI origin = URI.create(CampusEndpoints.WEB_ORIGIN);
        return origin.getScheme().equalsIgnoreCase(uri.getScheme())
                && origin.getHost().equalsIgnoreCase(uri.getHost())
                && effectivePort(origin) == effectivePort(uri);
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() != -1) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }
}
