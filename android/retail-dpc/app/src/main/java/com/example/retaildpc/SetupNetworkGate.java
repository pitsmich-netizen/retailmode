package com.example.retaildpc;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.SystemClock;
import android.provider.Settings;
import java.util.function.BooleanSupplier;

/**
 * DPC test provisioning gate: wait only for Android Setup Wizard completion.
 * JobScheduler already requires an available network; HTTP and retry code
 * handle temporary connectivity failures. No additional NET_CAPABILITY_VALIDATED
 * or arbitrary 2.5 s network stabilization hold.
 */
final class SetupNetworkGate {
    private static final long MAX_WAIT_MS = 4L * 60L * 1000L;
    private static final long POLL_MS = 700L;
    private SetupNetworkGate() {}

    static boolean await(Context c, String task, BooleanSupplier cancelled) {
        final long started = SystemClock.elapsedRealtime();
        boolean waitLogged = false;
        while (SystemClock.elapsedRealtime() - started < MAX_WAIT_MS) {
            if (cancelled.getAsBoolean() || CleanupFlow.blocked(c)) return false;
            DevicePolicyManager dpm = c.getSystemService(DevicePolicyManager.class);
            if (dpm == null || !dpm.isDeviceOwnerApp(c.getPackageName())) {
                State.log(c, "Gotowość " + task + ": Wrapper nie jest Device Owner");
                return false;
            }
            boolean provisioned = Settings.Global.getInt(c.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0) == 1;
            boolean finished = Settings.Secure.getInt(c.getContentResolver(),
                "user_setup_complete", 0) == 1;

            if (provisioned && finished) {
                State.log(c, "Gotowość " + task + ": Android zakończył konfigurację; "
                    + "oczekiwanie " + (SystemClock.elapsedRealtime() - started) + " ms");
                return true;
            }
            if (!waitLogged) {
                State.log(c, "Gotowość " + task + ": oczekuję na koniec Setup Wizard");
                DpcProgress.stage(c, task + "_waiting_setup",
                    "Czekam na zakończenie konfiguracji Androida…");
                waitLogged = true;
            }
            SystemClock.sleep(POLL_MS);
        }
        State.log(c, "Gotowość " + task + ": limit 240 s; ponowi JobScheduler");
        return false;
    }
}
