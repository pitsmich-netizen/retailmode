package com.example.retaildpc;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

/**
 * Test-only: leave the de-privileged Wrapper installed but dormant.
 * Never hide the launcher until Device Owner AND device admin are both gone
 * AND the signed Helper has acknowledged the release.
 *
 * This does NOT disable the whole package or grant hidden system privileges.
 * Manual uninstall remains available in Android Settings / Helper advanced UI.
 */
final class WrapperDormancy {
    private static final String PREF = "retail_dpc_dormancy_014";
    private WrapperDormancy() { }

    static synchronized boolean enter(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (p.getBoolean("done", false)) return true;
        DevicePolicyManager dpm = c.getSystemService(DevicePolicyManager.class);
        if (dpm == null || dpm.isDeviceOwnerApp(c.getPackageName())
                || dpm.isAdminActive(new ComponentName(c, AdminReceiver.class))) {
            State.log(c, "Dormancy: odmowa — Wrapper nadal jest administratorem");
            return false;
        }
        if (!HelperStatus.installed(c) || !State.installed(c) || !CleanupFlow.blocked(c)) {
            State.log(c, "Dormancy: brak kompletu APK lub potwierdzenia przygotowania");
            return false;
        }
        // The installer jobs were already cancelled by OwnerReleaseFlow.
        DpcProgress.clear(c);
        QrRetailCredential.clear(c);

        // Disable background triggers, but keep the real MainActivity
        // addressable explicitly for recovery/diagnostics.
        PackageManager pm = c.getPackageManager();
        int state = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        int flags = PackageManager.DONT_KILL_APP;
        try {
            pm.setComponentEnabledSetting(new ComponentName(c, BootReceiver.class), state, flags);
        } catch (RuntimeException e) {
            State.log(c, "Dormancy: BootReceiver " + e.getClass().getSimpleName());
        }
        try {
            pm.setComponentEnabledSetting(new ComponentName(c, RetailAutomationService.class), state, flags);
        } catch (RuntimeException e) {
            State.log(c, "Dormancy: Accessibility " + e.getClass().getSimpleName());
        }
        // Keep only this DPC's launcher alias hidden. Never disable the whole app:
        // an enabled MainActivity and a manually accessible Settings > Apps
        // entry are needed for recovery.
        ComponentName alias = new ComponentName(c.getPackageName(),
            c.getPackageName() + ".LauncherActivity");
        try {
            pm.setComponentEnabledSetting(alias, state, flags);
            boolean hidden = pm.getComponentEnabledSetting(alias) == state;
            p.edit().putBoolean("done", hidden).commit();
            State.log(c, hidden
                ? "Dormancy: launcher DPC ukryty; bez Device Owner i admina"
                : "Dormancy: nie potwierdzono ukrycia ikony DPC");
            return hidden;
        } catch (RuntimeException e) {
            State.log(c, "Dormancy: ukrywanie ikony nieudane: "
                + e.getClass().getSimpleName());
            return false;
        }
    }
}
