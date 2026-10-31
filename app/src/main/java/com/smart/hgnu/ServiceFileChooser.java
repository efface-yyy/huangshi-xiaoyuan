package com.smart.hgnu;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** User-mediated file selection for HTML file inputs. No paths or data are stored by the app. */
final class ServiceFileChooser {
    static final int REQUEST_CODE = 9021;

    private final Activity activity;
    private ValueCallback<Uri[]> pending;
    private boolean allowMultiple;

    ServiceFileChooser(Activity activity) {
        this.activity = activity;
    }

    boolean show(ValueCallback<Uri[]> callback, WebChromeClient.FileChooserParams params) {
        // The result of an earlier picker must never be delivered to a later request.
        if (pending != null) {
            callback.onReceiveValue(null);
            return true;
        }
        pending = callback;
        if (params.getMode() != WebChromeClient.FileChooserParams.MODE_OPEN
                && params.getMode() != WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
            cancel();
            return true;
        }

        allowMultiple = params.getMode() == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);
        String[] types = acceptedMimeTypes(params.getAcceptTypes());
        intent.setType(types.length == 1 ? types[0] : "*/*");
        if (types.length > 1) intent.putExtra(Intent.EXTRA_MIME_TYPES, types);
        try {
            activity.startActivityForResult(intent, REQUEST_CODE);
        } catch (ActivityNotFoundException | SecurityException exception) {
            cancel();
        }
        return true;
    }

    void onActivityResult(int resultCode, Intent data) {
        if (pending == null) return;
        ValueCallback<Uri[]> callback = pending;
        pending = null;
        ArrayList<Uri> results = new ArrayList<>();
        if (resultCode == Activity.RESULT_OK && data != null) {
            ClipData clip = data.getClipData();
            if (clip != null) {
                int count = allowMultiple ? clip.getItemCount() : Math.min(1, clip.getItemCount());
                for (int i = 0; i < count; i++) addContentUri(results, clip.getItemAt(i).getUri());
            } else addContentUri(results, data.getData());
        }
        callback.onReceiveValue(results.isEmpty() ? null : results.toArray(new Uri[0]));
    }

    void cancel() {
        if (pending != null) {
            ValueCallback<Uri[]> callback = pending;
            pending = null;
            callback.onReceiveValue(null);
        }
    }

    private static void addContentUri(ArrayList<Uri> results, Uri uri) {
        if (uri != null && "content".equalsIgnoreCase(uri.getScheme())) results.add(uri);
    }

    private static String[] acceptedMimeTypes(String[] accepted) {
        Set<String> types = new LinkedHashSet<>();
        if (accepted != null) {
            for (String item : accepted) {
                if (item == null) continue;
                for (String raw : item.split(",")) {
                    String type = raw.trim().toLowerCase(Locale.ROOT);
                    if (type.startsWith("."))
                        type =
                                MimeTypeMap.getSingleton()
                                        .getMimeTypeFromExtension(type.substring(1));
                    if (type != null && type.matches("[a-z0-9!#$&^_.+*-]+/[a-z0-9!#$&^_.+*-]+"))
                        types.add(type);
                }
            }
        }
        return types.isEmpty() ? new String[] {"*/*"} : types.toArray(new String[0]);
    }
}
