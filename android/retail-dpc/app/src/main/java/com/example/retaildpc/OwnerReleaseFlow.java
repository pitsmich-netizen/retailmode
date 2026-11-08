package com.example.retaildpc;

import android.app.admin.DevicePolicyManager;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/** Test-device-only: relinquish ownership BEFORE Helper registers Samsung Retail Mode.
 * Deprecated Android API may leave policies behind; DO NOT use on real managed fleets.
 */
final class OwnerReleaseFlow {
    private static final String PREF = "retail_dpc_cleanup_082";
    static final String RELEASE_RESULT = "com.example.retaildpc.action.RELEASE_RESULT_095";
    static final String EXTRA_RELEASED = "released";
    private OwnerReleaseFlow() { }

    static synchronized void onConfirmedHelper(Context c, String nonce) {
        DevicePolicyManager dpm = c.getSystemService(DevicePolicyManager.class);
        if (dpm == null || !HelperHandoff.validate(c, nonce)) return;
        SharedPreferences handoff = HelperHandoff.prefs(c);
        ComponentName admin = new ComponentName(c, AdminReceiver.class);
        boolean released = handoff.getBoolean("released_095", false);
        String detail = "";
        if (!released) {
            if (!HelperStatus.installed(c) || !State.installed(c)) {
                State.log(c, "Zwolnienie DO wstrzymane: brak Helpera lub Retail Mode");
                return;
            }
            if (dpm.isDeviceOwnerApp(c.getPackageName())) {
                if (!HelperHandoff.ready(c)) return;
                // Block subsequent install jobs and prevent re-provisioning side effects.
                c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                    .putBoolean("release_prepared", true).commit();
                JobScheduler js = c.getSystemService(JobScheduler.class);
                if (js != null) {
                    js.cancel(Constants.INSTALL_JOB_ID);
                    js.cancel(Constants.HELPER_INSTALL_JOB_ID);
                    js.cancel(2121); // handoff retry job
                }
                try {
                    dpm.clearDeviceOwnerApp(c.getPackageName());
                } catch (RuntimeException e) {
                    detail = "clearDeviceOwnerApp: " + e.getClass().getSimpleName();
                    State.log(c, "Nie udało się zwolnić Device Owner: " + detail);
                }
            }
            if (!dpm.isDeviceOwnerApp(c.getPackageName())) {
                if (dpm.isAdminActive(admin)) {
                    try { dpm.removeActiveAdmin(admin); }
                    catch (RuntimeException e) {
                        detail = "removeActiveAdmin: " + e.getClass().getSimpleName();
                    }
                }
                released = !dpm.isAdminActive(admin);
            }
            if (released) {
                DpcProgress.clear(c);
                handoff.edit().putBoolean("released_095", true).commit();
                State.log(c, "Handoff potwierdzony: Wrapper nie jest Device Owner ani aktywnym administratorem.");
            } else {
                State.log(c, "Handoff: niepełne zwolnienie; zachowano Wrapper. " + detail);
            }
        }
        // IMPORTANT: send the release result FIRST. The Helper persists this result
        // and explicitly ACKs it BEFORE our PackageInstaller request can remove us.
        // Otherwise a successful uninstall kills the sender before the Helper learns
        // that Device Owner was relinquished (bug in experimental 0.9.7).
        boolean probePlanned = released && !handoff.getBoolean(
            SilentSelfUninstallProbe.PREF_PREFIX + "started", false);
        Intent result = new Intent(RELEASE_RESULT);
        result.setClassName(Constants.HELPER_PACKAGE, Constants.HELPER_PACKAGE + ".DpcReleaseReceiver");
        result.putExtra(HelperHandoff.EXTRA_NONCE, nonce);
        result.putExtra(EXTRA_RELEASED, released);
        result.putExtra("silent_probe_098", probePlanned);
        result.putExtra("detail", detail);
        c.sendBroadcast(result);
    }

    /** Called only after the signed Helper confirms it persisted the release result. */
    static synchronized void onConfirmedReleaseReceived(Context c, String nonce) {
        if (!HelperHandoff.validate(c, nonce)) return;
        SharedPreferences handoff = HelperHandoff.prefs(c);
        String pfx = SilentSelfUninstallProbe.PREF_PREFIX;
        if (!handoff.getBoolean("released_095", false)
                || handoff.getBoolean(pfx + "started", false)) return;
        DevicePolicyManager dpm = c.getSystemService(DevicePolicyManager.class);
        ComponentName admin = new ComponentName(c, AdminReceiver.class);
        if (dpm == null || dpm.isDeviceOwnerApp(c.getPackageName())
                || dpm.isAdminActive(admin)) {
            State.log(c, "Nie uruchomiono eksperymentu: Wrapper nadal ma uprawnienia admina.");
            return;
        }
        handoff.edit().putBoolean(pfx + "started", true).commit();
        boolean queued = SilentSelfUninstallProbe.tryUninstall(c);
        State.log(c, queued
            ? "DPC 0.10.1: próba cichego usunięcia po potwierdzeniu Helpera."
            : "DPC 0.10.1: PackageInstaller odmówił; Helper przejdzie do systemowego potwierdzenia.");
    }
}
