package com.example.retaildpc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;

public final class HelperInstallResultReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        int status=intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        String detail=intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        if (status==PackageInstaller.STATUS_SUCCESS && HelperStatus.installed(c)) {
            DpcProgress.stage(c, "helper_installed", "Helper zainstalowany. Oczekuję na zakończenie ustawień Androida…");
            State.log(c, "SUKCES: Retail Helper zainstalowany");
            HandoffScheduler.schedule(c);
        } else if (status==PackageInstaller.STATUS_PENDING_USER_ACTION) {
            DpcProgress.stage(c, "helper_user_confirm", "Instalacja Helpera wymaga potwierdzenia");
            State.log(c, "Helper wymaga potwierdzenia instalacji przez użytkownika; " + detail);
        } else {
            State.log(c, "BŁĄD instalacji Helpera status="+status+"; "+detail);
        }
    }
}
