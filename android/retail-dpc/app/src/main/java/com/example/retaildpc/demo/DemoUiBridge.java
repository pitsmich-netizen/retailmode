package com.example.retaildpc.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import java.util.Arrays;

/** Public, intentionally tiny interface between the Device Owner UI and demo automator. */
public final class DemoUiBridge {
    private static final String RETAIL = "com.samsung.rms.retailagent.global";
    private DemoUiBridge() { }

    public static boolean accessibilityEnabled(Context c) {
        return PendingStart.accessibilityEnabled(c);
    }
    public static boolean active(Context c) { return AutomationState.active(c); }
    public static boolean completed(Context c) { return CompletionHider.downloadAcknowledged(c); }
    public static String status(Context c) {
        if (completed(c)) return "Pobieranie demo rozpoczęte. Dostępność została wyłączona.";
        if (PendingStart.armed(c)) return "Włącz Retail Setup w systemowych ustawieniach Dostępności.";
        if (active(c)) return AutomationState.info(c);
        return "Gotowy do uruchomienia konfiguracji demo.";
    }
    public static String logs(Context c) { return AutomationState.logs(c); }
    public static String detectModel(Context c) {
        String fromName = "";
        try { fromName = ProfileMatcher.canonicalModel(
            Settings.Global.getString(c.getContentResolver(), "device_name")); }
        catch (RuntimeException ignored) { }
        return !fromName.isEmpty() ? fromName : ProfileMatcher.canonicalModel(Build.MODEL);
    }
    public static void prepare(Activity a, String model, String country, String partner,
                               String site, String password) {
        if (site == null || !site.matches("[A-Za-z0-9_-]{4,40}"))
            throw new IllegalArgumentException("Nieprawidlowe Site ID");
        String canonical = ProfileMatcher.canonicalModel(model);
        if (canonical.isEmpty()) throw new IllegalArgumentException("Nie rozpoznano modelu");
        AutomationState.setProfileModel(a, canonical);
        if (password != null && !password.isEmpty()) {
            char[] chars = password.toCharArray();
            try { SecretMemory.set(chars); }
            finally { Arrays.fill(chars, '\0'); }
        }
        boolean full = SecretMemory.available();
        if (PendingStart.accessibilityEnabled(a)) {
            AutomationState.start(a, country, partner, site, full);
            launchRetail(a);
        } else {
            PendingStart.arm(a, country, partner, site, full);
            AccessibilityNavigator.open(a);
        }
    }
    public static void onResume(Activity a) {
        if (PendingStart.beginIfReady(a) ||
                (PendingStart.accessibilityEnabled(a) && AutomationState.active(a)
                && AutomationState.phase(a) == AutomationState.ENTRY)) {
            launchRetail(a);
        }
    }
    public static void openAccessibility(Activity a) { AccessibilityNavigator.open(a); }
    public static void stop(Context c) {
        PendingStart.clear(c);
        AutomationState.stop(c, "Zatrzymano recznie");
    }
    public static void launchRetail(Activity a) {
        Intent i = a.getPackageManager().getLaunchIntentForPackage(RETAIL);
        if (i != null) a.startActivity(i);
        else AutomationState.log(a, "Retail Mode nie jest jeszcze zainstalowany");
    }
}
