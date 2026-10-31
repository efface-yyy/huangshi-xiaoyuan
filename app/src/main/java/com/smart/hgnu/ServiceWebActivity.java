package com.smart.hgnu;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.webkit.*;
import android.widget.*;

import org.json.JSONObject;

import java.util.Map;

/** School service browser; launch credentials are never persisted in activity state. */
public final class ServiceWebActivity extends Activity {
    static final String EXTRA_SERVICE_ID = "service_id";
    static final String EXTRA_SERVICE_TYPE = "service_type";
    private final Handler handler = new Handler();
    private WebView browser;
    private ProgressBar progress;
    private TextView title;
    private ServiceFileChooser fileChooser;
    private boolean stopped;
    private long deadline;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xfff5f7f2);
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(12), dp(6), dp(12), dp(6));
        Button back = new UiComponents(this).createBackButton(this::finish);
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(dp(80), dp(48));
        backParams.setMarginEnd(dp(12));
        toolbar.addView(back, backParams);
        title = new TextView(this);
        title.setText("校园服务");
        title.setTextColor(0xff173d34);
        title.setTextSize(17);
        title.setSingleLine(true);
        toolbar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(60)));
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(3)));
        browser = new WebView(this);
        fileChooser = new ServiceFileChooser(this);
        WebSettings settings = browser.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setGeolocationEnabled(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setUserAgentString(
                settings.getUserAgentString()
                        + " lyAppVersion/3.2.0_21 lySource/lyydxy SmartHGNU/1.0");
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(browser, true);
        browser.setWebChromeClient(
                new WebChromeClient() {
                    @Override
                    public void onProgressChanged(WebView view, int value) {
                        progress.setProgress(value);
                        progress.setVisibility(value == 100 ? View.GONE : View.VISIBLE);
                    }

                    @Override
                    public boolean onShowFileChooser(
                            WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                        return fileChooser.show(callback, params);
                    }
                });
        browser.setWebViewClient(
                new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(
                            WebView view, WebResourceRequest request) {
                        String scheme = request.getUrl().getScheme();
                        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                            return false;
                        Toast.makeText(ServiceWebActivity.this, "此入口需要外部应用支持", Toast.LENGTH_SHORT)
                                .show();
                        return true;
                    }

                    @Override
                    public void onReceivedError(
                            WebView view, WebResourceRequest request, WebResourceError error) {
                        if (request.isForMainFrame()) fail("网络异常，暂时无法打开服务");
                    }

                    @Override
                    public void onReceivedHttpError(
                            WebView view,
                            WebResourceRequest request,
                            WebResourceResponse response) {
                        if (request.isForMainFrame())
                            fail("服务暂不可用（" + response.getStatusCode() + "）");
                    }

                    @Override
                    public void onReceivedSslError(
                            WebView view, SslErrorHandler ssl, android.net.http.SslError error) {
                        ssl.cancel();
                        fail("服务证书异常，无法打开页面");
                    }
                });
        root.addView(browser, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        deadline = android.os.SystemClock.elapsedRealtime() + 25000;
        openService();
    }

    private void openService() {
        if (stopped) return;
        CampusState state = (CampusState) getApplication();
        String id = getIntent().getStringExtra(EXTRA_SERVICE_ID);
        String type = getIntent().getStringExtra(EXTRA_SERVICE_TYPE);
        JSONObject service = null;
        for (int i = 0; i < state.services.length(); i++) {
            JSONObject candidate = state.services.optJSONObject(i);
            if (candidate != null
                    && candidate.optString("YYID").equals(id)
                    && candidate.optString("LX").equals(type)) {
                service = candidate;
                break;
            }
        }
        if (service == null) {
            if ((state.isRestoringSession || state.isLoading)
                    && android.os.SystemClock.elapsedRealtime() < deadline)
                handler.postDelayed(this::openService, 250);
            else fail("服务目录尚未就绪，请重新进入");
            return;
        }
        title.setText(service.optString("YYMC", "校园服务"));
        state.controller.prepareService(
                service,
                new CampusController.ServiceCallback() {
                    @Override
                    public void ready(WebLaunchPlan plan) {
                        if (stopped) return;
                        if (plan.cookies.isEmpty()) {
                            browser.loadUrl(plan.url, plan.headers);
                            return;
                        }
                        int[] remaining = {plan.cookies.size()};
                        boolean[] rejected = {false};
                        for (Map.Entry<String, String> cookie : plan.cookies.entrySet()) {
                            CookieManager.getInstance()
                                    .setCookie(
                                            plan.cookieOrigin,
                                            cookie.getKey()
                                                    + "="
                                                    + cookie.getValue()
                                                    + "; Path=/; SameSite=Lax"
                                                    + (cookie.getValue().isEmpty()
                                                            ? "; Max-Age=0"
                                                            : ""),
                                            accepted -> {
                                                if (!Boolean.TRUE.equals(accepted))
                                                    rejected[0] = true;
                                                if (--remaining[0] == 0 && !stopped) {
                                                    CookieManager.getInstance().flush();
                                                    if (rejected[0]) fail("无法建立服务网页会话，请重新进入");
                                                    else browser.loadUrl(plan.url, plan.headers);
                                                }
                                            });
                        }
                    }

                    @Override
                    public void failed(String message) {
                        fail(message);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ServiceFileChooser.REQUEST_CODE)
            fileChooser.onActivityResult(resultCode, data);
    }

    private void fail(String message) {
        if (stopped || isFinishing()) return;
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onBackPressed() {
        if (browser.canGoBack()) browser.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        stopped = true;
        handler.removeCallbacksAndMessages(null);
        fileChooser.cancel();
        browser.stopLoading();
        browser.destroy();
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
