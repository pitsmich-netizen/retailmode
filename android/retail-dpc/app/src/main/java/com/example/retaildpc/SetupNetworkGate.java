package com.example.retaildpc;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.SystemClock;
import android.provider.Settings;
import java.util.function.BooleanSupplier;

/**
 * Wait for completed Setup Wizard and Android-validated Internet before downloading.
 * Runs in a JobService worker thread; after four minutes JobScheduler takes over.
 */
final class SetupNetworkGate {
    private static final long MAX_WAIT_MS = 4L * 60L * 1000L;
    private static final long POLL_MS = 1400L;
    private static final long STABLE_MS = 2500L;
    private SetupNetworkGate() {}

    static boolean await(Context c, String task, BooleanSupplier cancelled) {
        final long started = SystemClock.elapsedRealtime();
        long stableFrom = 0L;
        String lastReason = "";
        while (SystemClock.elapsedRealtime() - started < MAX_WAIT_MS) {
            if (cancelled.getAsBoolean() || CleanupFlow.blocked(c)) return false;
            DevicePolicyManager dpm = c.getSystemService(DevicePolicyManager.class);
            if (dpm == null || !dpm.isDeviceOwnerApp(c.getPackageName())) {
                State.log(c, "Gotowość " + task + ": utracono Device Owner");
                return false;
            }
            boolean provisioned = Settings.Global.getInt(c.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0) == 1;
            boolean finished = Settings.Secure.getInt(c.getContentResolver(),
                "user_setup_complete", 0) == 1;
            boolean validated = false;
            try {
                ConnectivityManager cm = c.getSystemService(ConnectivityManager.class);
                if (cm != null) {
                    Network active = cm.getActiveNetwork();
                    NetworkCapabilities cap = cm.getNetworkCapabilities(active);
                    validated = cap != null
                        && cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                }
            } catch (RuntimeException ex) {
                State.log(c, "Gotowość " + task + ": problem odczytu sieci "
                    + ex.getClass().getSimpleName());
            }
            long now = SystemClock.elapsedRealtime();
            String reason = !provisioned || !finished ? "setup"
                : !validated ? "internet" : "stabilizacja";
            if (provisioned && finished && validated) {
                if (stableFrom == 0) stableFrom = now;
                if (now - stableFrom >= STABLE_MS) {
                    State.log(c, "Gotowość " + task + ": Setup Wizard zakończony i Internet "
                        + "zweryfikowany; oczekiwanie " + ((now - started) / 1000) + " s");
                    return true;
                }
            } else stableFrom = 0L;
            if (!reason.equals(lastReason)) {
                lastReason = reason;
                String msg = "setup".equals(reason)
                    ? "Czekam na zakończenie konfiguracji Androida…"
                    : "internet".equals(reason)
                    ? "Czekam na zweryfikowany dostęp do Internetu…"
                    : "Internet gotowy — sprawdzam stabilność połączenia…";
                State.log(c, "Gotowość " + task + ": " + reason);
                DpcProgress.stage(c, task + "_waiting_" + reason, msg);
            }
            SystemClock.sleep(POLL_MS);
        }
        State.log(c, "Gotowość " + task + ": limit 240 s; ponowienie przez JobScheduler");
        return false;
    }
}
