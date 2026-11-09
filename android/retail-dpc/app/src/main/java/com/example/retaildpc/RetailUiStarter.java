package com.example.retaildpc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;

/**
 * Last-resort UI entry point for OEM Setup Wizards that do not honor the
 * temporary HOME preference. Only launches while DPC is Device Owner, after
 * Android setup flags are ready; never relaunches the UI after owner release.
 */
final class RetailUiStarter {
    private static final String PREF = "retail_setup_ui_starter_0111";
    private static final long MIN_RETRY_MS = 9_000L;
    private RetailUiStarter() { }

    static synchronized void tryOpen(Context c, String reason) {
        if (!SingleAppOwnerRelease.isOwner(c) || SingleAppOwnerRelease.blocked(c)
                || RetailHomePolicy.manualExitRequested(c)) return;
        boolean provisioned = Settings.Global.getInt(c.getContentResolver(),
            Settings.Global.DEVICE_PROVISIONED, 0) == 1;
        boolean setupDone = Settings.Secure.getInt(c.getContentResolver(),
            "user_setup_complete", 0) == 1;
        if (!provisioned || !setupDone) {
            State.log(c, "Retail UI: czekam na zakończenie kreatora Androida");
            return;
        }

        SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        long now = SystemClock.elapsedRealtime();
        long last = p.getLong("last_attempt_elapsed", -1L);
        if (last >= 0 && now >= last && now - last < MIN_RETRY_MS) return;
        p.edit().putLong("last_attempt_elapsed", now)
            .putString("last_trigger", reason).commit();

        try {
            Intent ui = new Intent(c, MainActivity.class);
            ui.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            c.startActivity(ui);
            State.log(c, "Retail UI: jawne otwarcie po konfiguracji; trigger=" + reason);
        } catch (RuntimeException e) {
            State.log(c, "Retail UI: Samsung blokuje otwarcie; "
                + e.getClass().getSimpleName() + "; trigger=" + reason
                + "; otwórz z powiadomienia");
        }
    }
}
