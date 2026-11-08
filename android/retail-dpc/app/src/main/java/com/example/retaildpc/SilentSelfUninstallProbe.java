package com.example.retaildpc;

import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInstaller;

/**
 * EXPERIMENTAL ONLY: Can the system uninstall this DPC without user consent?
 * Only invoked AFTER Device Owner and our active admin have both been removed.
 * No root, ADB, Knox, ownership transfer, hidden API, or special system permission.
 * Android can refuse or return STATUS_PENDING_USER_ACTION. No automatic consent.
 */
final class SilentSelfUninstallProbe {
    static final String PREF_PREFIX = "silent_probe_097_";
    static final String ACTION_STATUS = "com.example.retaildpc.SELF_UNINSTALL_STATUS_097";
    private SilentSelfUninstallProbe() { }

    static boolean tryUninstall(Context c) {
        SharedPreferences prefs = HelperHandoff.prefs(c);
        DevicePolicyManager dpm = c.getSystemService(DevicePolicyManager.class);
        ComponentName admin = new ComponentName(c, AdminReceiver.class);
        if (dpm == null || dpm.isDeviceOwnerApp(c.getPackageName()) || dpm.isAdminActive(admin)) {
            State.log(c, "Cicha deinstalacja zablokowana: DPC nadal ma uprawnienia admina.");
            return false;
        }
        prefs.edit().putBoolean(PREF_PREFIX + "waiting", true).commit();
        try {
            Intent callback = new Intent(c, SelfUninstallResultReceiver.class);
            callback.setAction(ACTION_STATUS);
            PendingIntent pending = PendingIntent.getBroadcast(c, 2197, callback,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            PackageInstaller installer = c.getPackageManager().getPackageInstaller();
            installer.uninstall(c.getPackageName(), pending.getIntentSender());
            return true;
        } catch (RuntimeException e) {
            prefs.edit().putBoolean(PREF_PREFIX + "waiting", false)
                .putInt(PREF_PREFIX + "result_status", PackageInstaller.STATUS_FAILURE).commit();
            State.log(c, "Cicha deinstalacja niedostępna: " + e.getClass().getSimpleName());
            return false;
        }
    }
}
