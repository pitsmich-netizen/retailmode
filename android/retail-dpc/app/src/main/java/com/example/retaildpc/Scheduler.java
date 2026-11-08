package com.example.retaildpc;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

final class Scheduler {
    private Scheduler() {}
    static void schedule(Context c) {
        if (CleanupFlow.blocked(c)) { State.log(c, "Installer zablokowany po przygotowaniu zwolnienia DO"); return; }
        JobInfo job = new JobInfo.Builder(Constants.INSTALL_JOB_ID,
            new ComponentName(c, InstallJobService.class))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setBackoffCriteria(30000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
            .setPersisted(true)
            .build();
        JobScheduler js = c.getSystemService(JobScheduler.class);
        if (js == null) {
            State.log(c, "BŁĄD: JobScheduler niedostępny");
            return;
        }
        try {
            int result = js.schedule(job);
            if (result == JobScheduler.RESULT_SUCCESS) SetupNetworkGate.markQueued(c, "retail");
            State.log(c, "Instalacja Retail zakolejkowana, result=" + result);
            scheduleHelper(c);
        } catch (RuntimeException e) {
            // Wersja testowa: zachowaj UI diagnostyczne zamiast zamykać aplikację.
            Log.e("RetailDpcTest", "Błąd planowania instalacji", e);
            State.log(c, "BŁĄD JobScheduler: " + e.getClass().getSimpleName()
                + " " + e.getMessage());
        }
    }

    static void scheduleHelper(Context c) {
        if (CleanupFlow.blocked(c) || !CleanupFlow.isOwner(c)) return;
        if (!HelperStatus.configured(c)) {
            State.log(c, "Helper nie skonfigurowany: sprawdź helper-apk.defaults.properties i dane QR");
            return;
        }
        if (HelperStatus.installed(c)) return;
        JobScheduler js = c.getSystemService(JobScheduler.class);
        if (js == null) { State.log(c, "BŁĄD: brak JobScheduler dla Helpera"); return; }
        JobInfo job = new JobInfo.Builder(Constants.HELPER_INSTALL_JOB_ID,
            new ComponentName(c, HelperInstallJobService.class))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setBackoffCriteria(30000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
            .setPersisted(true)
            .build();
        try {
            int result = js.schedule(job);
            if (result == JobScheduler.RESULT_SUCCESS) SetupNetworkGate.markQueued(c, "helper");
            State.log(c, "Instalacja Helpera zakolejkowana, result=" + result);
        } catch (RuntimeException e) {
            State.log(c, "BŁĄD JobScheduler Helper: " + e.getClass().getSimpleName());
        }
    }
}
