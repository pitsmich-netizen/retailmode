package com.example.retaildpc;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.SystemClock;
import android.provider.Settings;
import java.util.function.BooleanSupplier;

/**
 * Diagnostic-only release: keep the exact 0.10.10 setup readiness conditions.
 * Observe both flags separately and show which one is still missing.
 * No Internet-validation gate; network requirement stays with JobScheduler.
 */
final class SetupNetworkGate {
    private static final long MAX_WAIT_MS = 4L * 60L * 1000L;
    private static final long POLL_MS = 700L;
    private static final long DIAGNOSTIC_MS = 2400L;
    private SetupNetworkGate() {}

    static boolean await(Context c, String task, BooleanSupplier cancelled) {
        final long started = SystemClock.elapsedRealtime();
        long provisionedAt = -1L;
        long setupCompleteAt = -1L;
        long lastDiagnosticAt = -1L;
        int previousBits = -1;
        State.log(c, "Diagnostyka " + task + ": JobScheduler -> watek bramki; "
            + "opoznienie od pierwszego kolejkowania " + queuedForMs(c, task) + " ms");
        try {
            while (SystemClock.elapsedRealtime() - started < MAX_WAIT_MS) {
                if (cancelled.getAsBoolean() || CleanupFlow.blocked(c)) return false;
                DevicePolicyManager dpm = c.getSystemService(DevicePolicyManager.class);
                if (dpm == null || !dpm.isDeviceOwnerApp(c.getPackageName())) {
                    State.log(c, "Gotowosc " + task + ": Wrapper nie jest Device Owner");
                    return false;
                }
                boolean provisioned = Settings.Global.getInt(c.getContentResolver(),
                    Settings.Global.DEVICE_PROVISIONED, 0) == 1;
                boolean finished = Settings.Secure.getInt(c.getContentResolver(),
                    "user_setup_complete", 0) == 1;
                long now = SystemClock.elapsedRealtime();
                long elapsed = now - started;

                if (provisioned && provisionedAt < 0) provisionedAt = elapsed;
                if (finished && setupCompleteAt < 0) setupCompleteAt = elapsed;
                int bits = (provisioned ? 1 : 0) | (finished ? 2 : 0);
                if (bits != previousBits) {
                    State.log(c, "Flagi " + task + " +" + elapsed + " ms: "
                        + "device_provisioned=" + (provisioned ? 1 : 0)
                        + ", user_setup_complete=" + (finished ? 1 : 0)
                        + "; first_1= " + provisionedAt + " / " + setupCompleteAt + " ms");
                    previousBits = bits;
                }
                if (provisioned && finished) {
                    State.log(c, "Gotowosc " + task + ": obie flagi 1 po " + elapsed
                        + " ms; device_provisioned od +" + provisionedAt
                        + " ms; user_setup_complete od +" + setupCompleteAt + " ms");
                    return true;
                }

                if (lastDiagnosticAt < 0 || now - lastDiagnosticAt >= DIAGNOSTIC_MS) {
                    lastDiagnosticAt = now;
                    DpcProgress.setupFlagDiagnostics(c, task, provisioned, finished,
                        elapsed, provisionedAt, setupCompleteAt);
                }
                SystemClock.sleep(POLL_MS);
            }
            State.log(c, "Gotowosc " + task + ": timeout 240 s; ponowi JobScheduler");
            return false;
        } finally {
            DpcProgress.clearSetupFlagDiagnostics(c, task);
        }
    }

    private static final String PREF = "retail_dpc_gate_timing_011";
    static synchronized void markQueued(Context c, String task) {
        android.content.SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (!p.contains(task)) {
            p.edit().putLong(task, SystemClock.elapsedRealtime()).commit();
            State.log(c, "Diagnostyka " + task + ": zadanie zaplanowane");
        }
    }
    private static long queuedForMs(Context c, String task) {
        long when = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong(task, -1L);
        long now = SystemClock.elapsedRealtime();
        return when < 0 || now < when ? -1L : now - when;
    }
}
