package com.example.retaildpc;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

/**
 * Experimental priority scheduling. Expedited jobs bypass Android's
 * optimization delay when quota/network allow it. Standard persisted jobs
 * remain a fallback when the OS refuses to schedule an expedited job.
 */
final class Scheduler {
    private Scheduler() {}

    static void schedule(Context c) {
        if (CleanupFlow.blocked(c)) {
            State.log(c, "Installer zablokowany po przygotowaniu zwolnienia DO");
            return;
        }
        try {
            scheduleJob(c, Constants.INSTALL_JOB_ID, InstallJobService.class, "retail");
        } finally {
            scheduleHelper(c);
        }
    }

    static void scheduleHelper(Context c) {
        if (CleanupFlow.blocked(c) || !CleanupFlow.isOwner(c)) return;
        if (!HelperStatus.configured(c)) {
            State.log(c, "Helper nie skonfigurowany: sprawdz konfiguracje APK");
            return;
        }
        if (HelperStatus.installed(c)) return;
        scheduleJob(c, Constants.HELPER_INSTALL_JOB_ID, HelperInstallJobService.class, "helper");
    }

    private static void scheduleJob(Context c, int jobId,
                                    Class<? extends JobService> service, String task) {
        JobScheduler js = c.getSystemService(JobScheduler.class);
        if (js == null) {
            State.log(c, "BŁĄD " + task + ": brak JobScheduler");
            return;
        }

        // API 31+: all provisioning devices supported by this DPC.
        // Network and persisted are allowed expedited constraints.
        int urgentResult = JobScheduler.RESULT_FAILURE;
        try {
            JobInfo urgent = makeJob(c, jobId, service, true);
            urgentResult = js.schedule(urgent);
            State.log(c, "Instalacja " + task + ": expedited result=" + urgentResult);
        } catch (RuntimeException e) {
            State.log(c, "Instalacja " + task + ": expedited niedostepny ("
                + e.getClass().getSimpleName() + "); fallback standard");
            Log.w("RetailDpcTest", "Expedited job rejected: " + task, e);
        }
        if (urgentResult == JobScheduler.RESULT_SUCCESS) {
            SetupNetworkGate.markQueued(c, task);
            return;
        }

        // Preserve the well-tested normal path, network constraint,
        // exponential retries, and persistence over reboot.
        try {
            int result = js.schedule(makeJob(c, jobId, service, false));
            if (result == JobScheduler.RESULT_SUCCESS)
                SetupNetworkGate.markQueued(c, task);
            State.log(c, "Instalacja " + task + ": fallback standard result=" + result);
        } catch (RuntimeException e) {
            State.log(c, "BŁĄD JobScheduler " + task + ": "
                + e.getClass().getSimpleName() + " " + e.getMessage());
            Log.e("RetailDpcTest", "Normal JobScheduler fallback failed: " + task, e);
        }
    }

    private static JobInfo makeJob(Context c, int jobId,
                                   Class<? extends JobService> service, boolean expedited) {
        JobInfo.Builder b = new JobInfo.Builder(jobId, new ComponentName(c, service))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setBackoffCriteria(30000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
            .setPersisted(true);
        if (expedited) b.setExpedited(true);
        return b.build();
    }
}
