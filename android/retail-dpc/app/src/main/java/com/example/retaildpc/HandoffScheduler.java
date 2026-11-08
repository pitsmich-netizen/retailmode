package com.example.retaildpc;

import android.content.Context;
import android.content.ComponentName;
import android.app.job.JobScheduler;
import android.app.job.JobInfo;

final class HandoffScheduler {
    private static final int ID = 2121;
    private HandoffScheduler() { }
    static void schedule(Context c, long delay) {
        if (HelperHandoff.autoAttempted(c)) return;
        JobScheduler js = c.getSystemService(JobScheduler.class);
        if (js == null) return;
        JobInfo job = new JobInfo.Builder(ID, new ComponentName(c, HandoffJobService.class))
            .setBackoffCriteria(30000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
            .setMinimumLatency(delay)
            .setOverrideDeadline(delay + 60000L)
            .setPersisted(true)
            .build();
        try { js.schedule(job); }
        catch (RuntimeException e) {
            State.log(c, "Nie mogę zaplanować przekazania Helperowi: " + e.getClass().getSimpleName());
        }
    }
    static void schedule(Context c) { schedule(c, 25000L); }
}
