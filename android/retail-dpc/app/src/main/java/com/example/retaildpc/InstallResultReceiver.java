package com.example.retaildpc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;

public final class InstallResultReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE);
        String detail = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        if (status == PackageInstaller.STATUS_SUCCESS) {
            State.installed(c, true);
            DpcProgress.stage(c, "retail_installed", "Retail Mode zainstalowany. Kończę przygotowanie…");
            State.log(c, "SUKCES: demo zainstalowano; package=" + State.pkg(c));
            InitialDeviceSetup.grantRetailLocationPermissions(c);
            HandoffScheduler.schedule(c);
        } else if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            DpcProgress.stage(c, "retail_user_confirm", "Instalacja Retail Mode wymaga potwierdzenia");
            State.log(c, "WYMAGA POTWIERDZENIA przez użytkownika: " + detail);
        } else {
            DpcProgress.stage(c, "retail_error", "Problem z instalacją Retail Mode — sprawdź DPC");
            State.log(c, "BŁĄD PackageInstaller status=" + status + "; " + detail);
        }
    }
}
