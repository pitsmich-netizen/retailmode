package com.example.retaildpc;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.PersistableBundle;

/** Configured Helper APK source. Only accepts a known release path and a SHA-256 checksum. */
final class HelperConfig {
    private static final String PREFS = "retail_helper_source_v1";
    private static final String KEY_URL = "url";
    private static final String KEY_SHA = "sha256";
    private static final String PINNED_RAW_HELPER =
        "https://raw.githubusercontent.com/pitsmich-netizen/retailmode/main/provisioning/RetailHelper-029.apk";

    private HelperConfig() {}

    static boolean valid(String url, String sha) {
        if (url == null || sha == null) return false;
        // Two explicit trusted sources: verified release URL or the pinned raw GitHub mirror.
        boolean release = url.matches(
            "https://github\\.com/pitsmich-netizen/retailmode/releases/download/[A-Za-z0-9._-]+/RetailHelper\\.apk");
        boolean pinnedCiRaw = url.matches(
            "https://raw\\.githubusercontent\\.com/pitsmich-netizen/retailmode/[0-9a-f]{40}/provisioning/ci/[0-9]+/RetailHelper\\.apk");
        return (release || pinnedCiRaw || PINNED_RAW_HELPER.equals(url)) && sha.matches("(?i)[0-9a-f]{64}");
    }

    /** Invoked only from Android Enterprise provisioning callbacks. */
    static void accept(Context c, PersistableBundle extras) {
        if (extras == null) return;
        String url = extras.getString("helper_url", "");
        String digest = extras.getString("helper_sha256", "");
        if (url.isEmpty() && digest.isEmpty()) return;
        if (!valid(url, digest)) {
            State.log(c, "QR: Helper URL lub SHA-256 nieprawidlowe; zachowuje domyslne");
            return;
        }
        SharedPreferences prefs = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_URL, url).putString(KEY_SHA, digest.toLowerCase(java.util.Locale.ROOT)).apply();
        State.log(c, "QR: przyjeto adres APK Helpera: " + url);
    }

    static String url(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String candidate = p.getString(KEY_URL, "");
        String digest = p.getString(KEY_SHA, "");
        return valid(candidate, digest) ? candidate : BuildConfig.HELPER_URL;
    }
    static String sha256(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String candidate = p.getString(KEY_URL, "");
        String digest = p.getString(KEY_SHA, "");
        return valid(candidate, digest) ? digest : BuildConfig.HELPER_SHA256;
    }
    static boolean configured(Context c) { return valid(url(c), sha256(c)); }
}
