package com.example.retaildpc.demo;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

/**
 * Hide ONLY the launcher alias after Retail Mode has confirmed demo downloading.
 * The real MainActivity, Accessibility service and installed package remain intact.
 * No uninstall intent and no OS settings modification are ever attempted here.
 */
final class CompletionHider {
    private static final String PREF = "retail_helper_completion_026";
    private static final String LAUNCHER_ALIAS = "com.example.retaildpc.LauncherActivity";
    private CompletionHider() { }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }
    private static ComponentName alias(Context c) {
        return new ComponentName(c.getPackageName(), LAUNCHER_ALIAS);
    }
    static boolean downloadAcknowledged(Context c) {
        return prefs(c).getBoolean("download_ack", false);
    }
    static String status(Context c) {
        return prefs(c).getString("detail", "Nie zakończono konfiguracji");
    }
    static void markAlreadyConfigured(Context c,boolean downloading) {
        prefs(c).edit().putBoolean("download_ack",true)
            .putLong("download_ack_at",System.currentTimeMillis())
            .putString("registration_state",downloading?"downloading":"registered")
            .putString("detail",downloading
                ? "Retail Mode potwierdził rozpoczęte pobieranie — rejestracja pominięta"
                : "Retail Mode potwierdził wcześniejszą rejestrację — formularz pominięty")
            .commit();
    }
    static void markDownloadAcknowledged(Context c) {
        prefs(c).edit().putBoolean("download_ack", true)
            .putLong("download_ack_at", System.currentTimeMillis())
            .putString("detail", "Pobieranie demo rozpoczęte; chowam ikonę Retail Setup")
            .commit();
    }
    /**
     * Only after download has actually started. Disable the ONLY LAUNCHER entry.
     * Alias is DISABLED BY DEFAULT in AndroidManifest, so Android's synthetic
     * App Details launcher tile should not be injected after this call.
     * Process may be killed, so persist success state before changing it.
     * Accessibility must already have been disabled by RetailAutomationService.
     */
    static boolean hideLauncherIfComplete(Context c) {
        if (!downloadAcknowledged(c) || AutomationState.active(c)) return false;
        PackageManager pm = c.getPackageManager();
        ComponentName launcher = alias(c);
        ComponentName home = new ComponentName(c.getPackageName(),
            c.getPackageName() + ".HomeActivity");
        try {
            // Persistent marker before a call that can terminate the process.
            prefs(c).edit().putString("detail", "Kończę konfigurację; ikona nie jest domyślnie włączona").commit();
            AutomationState.log(c, "Kończę ukrywanie komponentów HOME i LAUNCHER");
            // Only now, after demo download is acknowledged and Accessibility
            // has been disabled, remove HOME. Do not kill the app here.
            pm.setComponentEnabledSetting(home,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
            pm.setComponentEnabledSetting(launcher,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                0);
            // Executed only if Android did not kill this process.
            boolean disabled = pm.getComponentEnabledSetting(launcher)
                == PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            prefs(c).edit().putString("detail", disabled
                ? "Ikona wyłączona (domyślnie wyłączona w manifeście)"
                : "Nie potwierdzono wyłączenia komponentu ikony").commit();
            return disabled;
        } catch (RuntimeException e) {
            String detail = "Nie udało się wyłączyć ikony: " + e.getClass().getSimpleName();
            prefs(c).edit().putString("detail", detail).commit();
            AutomationState.log(c, detail);
            return false;
        }
    }
    /** Explicit START restores the manifest default state (alias still hidden). */
    static void resetForExplicitStart(Context c) {
        prefs(c).edit().clear().commit();
        // Do NOT enable launcher even for a manual restart.  Handoff always
        // opens MainActivity explicitly; retaining the default-disabled alias
        // avoids Android's synthesized App Details icon on unmanaged phones.
        try {
            c.getPackageManager().setComponentEnabledSetting(alias(c),
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP);
        } catch (RuntimeException e) {
            AutomationState.log(c, "Przywrócenie domyślnego stanu aliasu nieudane: "
                + e.getClass().getSimpleName());
        }
    }
}
