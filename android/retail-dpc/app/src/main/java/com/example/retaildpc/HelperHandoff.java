package com.example.retaildpc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.app.admin.DevicePolicyManager;
import java.security.SecureRandom;

/** Handoff acknowledged by actual Helper foreground UI, never inferred from startActivity(). */
final class HelperHandoff {
    private static final String PREF = "helper_handoff_094"; // persist old handoff attempts across updates
    static final String EXTRA_NONCE = "handoff_nonce";
    static final String EXTRA_DPC = "from_dpc";
    private HelperHandoff() { }

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREF, Context.MODE_PRIVATE); }
    static boolean autoAttempted(Context c) { return prefs(c).getBoolean("auto_attempted", false); }
    static boolean ready(Context c) {
        DevicePolicyManager dpm = c.getSystemService(DevicePolicyManager.class);
        if (dpm == null || !dpm.isDeviceOwnerApp(c.getPackageName())) return false;
        boolean provisioned = Settings.Global.getInt(c.getContentResolver(), "device_provisioned", 0) == 1;
        boolean setupFinished = Settings.Secure.getInt(c.getContentResolver(), "user_setup_complete", 0) == 1;
        boolean demoExists = c.getPackageManager().getLaunchIntentForPackage("com.samsung.rms.retailagent.global") != null;
        return provisioned && setupFinished && HelperStatus.installed(c) && State.installed(c) && demoExists;
    }
    private static String token(Context c) {
        String t = prefs(c).getString("nonce_095", "");
        if (!t.isEmpty()) return t;
        byte[] data = new byte[24]; new SecureRandom().nextBytes(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : data) sb.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        t = sb.toString();
        prefs(c).edit().putString("nonce_095", t).commit();
        return t;
    }
    static boolean validate(Context c, String nonce) {
        String expected = prefs(c).getString("nonce_095", "");
        return nonce != null && nonce.length() == 48 && !expected.isEmpty() && expected.equals(nonce);
    }
    static boolean launchAuto(Context c) {
        if (autoAttempted(c) || !ready(c)) return false;
        return launch(c, StoreSelection.country(c), StoreSelection.partner(c), StoreSelection.site(c), true);
    }
    static boolean launchManual(Context c, String country, String partner, String site) {
        if (!ready(c)) return false;
        return launch(c, country, partner, site, false);
    }
    private static boolean launch(Context c, String country, String partner, String site, boolean automatic) {
        Intent i = new Intent();
        i.setClassName(Constants.HELPER_PACKAGE, Constants.HELPER_PACKAGE + ".MainActivity");
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("site_id", site);
        i.putExtra("country", country);
        i.putExtra("partner", partner);
        i.putExtra(EXTRA_DPC, true);
        i.putExtra(EXTRA_NONCE, token(c));
        try {
            c.startActivity(i);
            if (automatic) prefs(c).edit().putBoolean("auto_attempted", true)
                .putLong("attempt_time_ms", System.currentTimeMillis()).apply();
            State.log(c, "Handoff: wysłano otwarcie Helpera; czekam na podpisane potwierdzenie UI.");
            return true;
        } catch (RuntimeException e) {
            State.log(c, "Handoff: błąd otwierania: " + e.getClass().getSimpleName());
            return false;
        }
    }
}
