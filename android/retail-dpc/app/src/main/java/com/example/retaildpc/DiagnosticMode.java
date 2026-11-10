package com.example.retaildpc;

import android.app.job.JobScheduler;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.PersistableBundle;

/**
 * Opt-in QR provisioning mode to test stock app recovery without downloading
 * Samsung Retail Mode or registering/downloading Experience demo content.
 * Flag comes only from Android-managed provisioning extras.
 */
final class DiagnosticMode {
    private static final String PREF = "retail_diagnostics_no_demo_v1";
    private static final String KEY = "diagnostics_no_demo";
    private static final String OMC_KEY = "omc_probe_only";
    private DiagnosticMode() { }

    static void accept(Context c, PersistableBundle extras) {
        if (extras == null || !(extras.getBoolean(KEY,false) ||
            extras.getBoolean(OMC_KEY,false) ||
            extras.getBoolean("minimal_owner_probe",false))) return;
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", true)
            .putBoolean("omc_probe_only", extras.getBoolean(OMC_KEY,false) ||
                extras.getBoolean("minimal_owner_probe",false)).commit();
        JobScheduler scheduler = c.getSystemService(JobScheduler.class);
        if (scheduler != null) scheduler.cancel(Constants.INSTALL_JOB_ID);
        State.log(c, "QR: tryb DIAGNOSTYKA APLIKACJI bez Retail Mode / Experience");
    }
    static boolean enabled(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean("enabled", false);
    }
    static boolean probeOnly(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean("omc_probe_only", false);
    }
    static synchronized void run(Context c) {
        if (!enabled(c)) return;
        MinimalOwnerProbe.capture(c,"DIAGNOSTIC_RUN");
        SharedPreferences prefs = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (!probeOnly(c) && SingleAppOwnerRelease.isOwner(c) && !StockAppRecovery.attempted(c)) {
            State.log(c, "Diagnostyka: przywracam fabryczne pakiety jako Device Owner");
            StockAppRecovery.restore(c);
        }
        PostDemoAppRestore.audit(c);
        prefs.edit().putBoolean("run", true).apply();
        State.log(c, "Diagnostyka: raport aplikacji gotowy (bez pobierania demo)");
    }
    // Manual diagnostic retry; never used by full Retail Mode automation.
    static synchronized void retryRestore(Context c) {
        if (!enabled(c) || probeOnly(c)) return;
        if (!SingleAppOwnerRelease.isOwner(c)) {
            State.log(c, "Diagnostyka: nie ponowiono; brak Device Owner");
            return;
        }
        StockAppRecovery.restore(c, true);
        PostDemoAppRestore.audit(c);
        State.log(c, "Diagnostyka: ręcznie ponowiono próby przywracania");
    }
    static String report(Context c) {
        return "TRYB DIAGNOSTYCZNY: bez Retail Mode i bez pobierania Experience\n"
            + "Device Owner: " + SingleAppOwnerRelease.isOwner(c) + "\n\n"
            + (probeOnly(c)
                ? "Tryb OMC: pomijam próby przywracania pakietów (ustalone jako nieskuteczne).\n"
                : StockAppRecovery.report(c))
            + "\n\n" + MinimalOwnerProbe.report(c)
            + "\n\n" + SamsungOmcEnableProbe.report(c)
            + "\n\n" + SamsungProvisioningProbe.report(c)
            + "\n\nAUDYT BRAKUJĄCYCH PAKIETÓW\n"
            + PostDemoAppRestore.report(c);
    }
}
