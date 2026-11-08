package com.example.retaildpc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;

/** Only a local explicit PendingIntent can deliver the PackageInstaller status. */
public final class SelfUninstallResultReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if (i == null || !SilentSelfUninstallProbe.ACTION_STATUS.equals(i.getAction())) return;
        int status = i.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        HelperHandoff.prefs(c).edit()
            .putBoolean(SilentSelfUninstallProbe.PREF_PREFIX + "waiting", false)
            .putInt(SilentSelfUninstallProbe.PREF_PREFIX + "result_status", status)
            .commit();
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            // Do not start EXTRA_INTENT; Helper 0.2.8 includes the system-uninstall fallback.
            State.log(c, "PackageInstaller: system wymaga zgody; Helper otworzy stary deinstalator.");
        } else if (status == PackageInstaller.STATUS_SUCCESS) {
            State.log(c, "PackageInstaller: system potwierdził odinstalowanie Wrappera.");
        } else {
            State.log(c, "PackageInstaller: cicha deinstalacja nie powiodła się, status=" + status);
        }
    }
}
