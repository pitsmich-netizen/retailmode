package com.example.retaildpc;

import android.app.job.JobParameters;
import android.app.job.JobService;

/** Battery-conscious, passive one-shot samples; no networking, no Play intents. */
public final class GooglePlayVersionJobService extends JobService {
    @Override public boolean onStartJob(JobParameters params) {
        int id=params.getJobId();
        String phase=id==GooglePlayVersionAudit.JOB_AFTER_15_MIN
            ? GooglePlayVersionAudit.PHASE_15
            : id==GooglePlayVersionAudit.JOB_AFTER_60_MIN
                ? GooglePlayVersionAudit.PHASE_60 : null;
        if(phase!=null) GooglePlayVersionAudit.checkpoint(this,phase);
        return false;
    }
    @Override public boolean onStopJob(JobParameters params) { return false; }
}
