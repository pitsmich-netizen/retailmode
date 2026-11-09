package com.example.retaildpc;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

/**
 * Restore existing, factory-preloaded apps only while Retail Setup is still DO.
 * Never sideloads an APK, fetches a store account or changes user consent.
 * Some Galaxy titles are delivered later through Galaxy/Play Store and
 * cannot be restored if no preload package is present on the device.
 */
final class StockAppRecovery {
    private static final String PREF = "retail_stock_apps_recovery_0117";
    // Exact package names are intentionally allowlisted. Unknown apps are never touched.
    private static final String[][] APPS = {
        {"Kalendarz Samsung", "com.samsung.android.calendar"},
        {"Kalkulator Samsung", "com.sec.android.app.popupcalculator"},
        {"Zegar Samsung", "com.sec.android.app.clockpackage"},
        {"Samsung Notes", "com.samsung.android.app.notes"},
        {"Samsung Members", "com.samsung.android.voc"},
        {"Bixby", "com.samsung.android.bixby.agent"},
        {"Wskazówki (Tips)", "com.samsung.android.app.tips"},
        {"Gaming Hub", "com.samsung.android.game.gamehome"},
        {"Samsung Global Goals", "com.samsung.sree"},
        {"Facebook", "com.facebook.katana"},
        {"Instagram", "com.instagram.android"},
        {"Spotify", "com.spotify.music"}
    };
    private StockAppRecovery() { }

    static boolean attempted(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean("attempted", false);
    }

    static synchronized void restore(Context c) {
        if (attempted(c)) return;
        DevicePolicyManager dpm = c.getSystemService(DevicePolicyManager.class);
        if (dpm == null || !dpm.isDeviceOwnerApp(c.getPackageName())) return;
        ComponentName admin = new ComponentName(c, AdminReceiver.class);
        PackageManager pm = c.getPackageManager();
        int already = 0, restored = 0, absent = 0, failed = 0;
        StringBuilder record = new StringBuilder();

        for (String[] entry : APPS) {
            String name = entry[0], pkg = entry[1];
            String before = state(pm, pkg);
            String result;
            if ("ZAINSTALOWANA".equals(before)) {
                already++;
                result = "już dostępna";
            } else {
                try {
                    // Public DPM method can only restore a factory system
                    // package; it does not download a missing third-party APK.
                    dpm.enableSystemApp(admin, pkg);
                    String after = state(pm, pkg);
                    if ("ZAINSTALOWANA".equals(after)) {
                        restored++;
                        result = "przywrócona";
                    } else {
                        absent++;
                        result = "brak fabrycznego pakietu / nadal niewidoczna";
                    }
                } catch (RuntimeException ex) {
                    if ("BRAK".equals(before)) absent++;
                    else failed++;
                    result = "nieprzywrócona (" + ex.getClass().getSimpleName() + ")";
                }
            }
            record.append(name).append(": ").append(result)
                .append(" [").append(pkg).append("]").append('\n');
            State.log(c, "StockAppRecovery: " + name + " - " + result);
        }
        String summary = "Już dostępne: " + already + "; przywrócone: " + restored
            + "; nadal brak/niewidoczne: " + absent + "; błędy: " + failed;
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putBoolean("attempted", true)
            .putString("summary", summary)
            .putString("results", record.toString())
            .commit();
        State.log(c, "StockAppRecovery: " + summary);
    }

    private static String state(PackageManager pm, String pkg) {
        try {
            ApplicationInfo app = pm.getApplicationInfo(pkg,
                PackageManager.MATCH_UNINSTALLED_PACKAGES);
            if ((app.flags & ApplicationInfo.FLAG_INSTALLED) == 0)
                return "NIEZAINSTALOWANA DLA UŻYTKOWNIKA";
            if (!app.enabled) return "WYŁĄCZONA";
            return "ZAINSTALOWANA";
        } catch (PackageManager.NameNotFoundException ex) {
            return "BRAK";
        } catch (RuntimeException ex) {
            return "NIEUSTALONY";
        }
    }

    static String report(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        StringBuilder out = new StringBuilder();
        out.append("PRZYWRACANIE PRZED ZWOLNIENIEM DEVICE OWNER\n")
            .append(p.getBoolean("attempted", false)
                ? p.getString("summary", "Brak podsumowania")
                : "Nie wykonano jeszcze (lub aplikacja została uruchomiona bez tej funkcji)")
            .append("\n\nSTAN BIEŻĄCY [pakiet]:\n");
        PackageManager pm = c.getPackageManager();
        for (String[] entry : APPS) {
            out.append(entry[0]).append(": ").append(state(pm, entry[1]))
                .append(" [").append(entry[1]).append("]\n");
        }
        if (p.getBoolean("attempted", false))
            out.append("\nWYNIKI PRZYWRACANIA:\n")
                .append(p.getString("results", "Brak wyników"));
        out.append("\nBrak pakietu oznacza, że trzeba skorzystać z oficjalnej ")
            .append("dystrybucji (Galaxy Store / Google Play); DPC nie pobiera obcych APK.");
        return out.toString();
    }
}
