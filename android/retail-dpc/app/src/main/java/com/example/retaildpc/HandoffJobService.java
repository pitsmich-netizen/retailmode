package com.example.retaildpc;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Handler;
import android.os.Looper;

/** Persistent job retries until both APKs installed AND Android Setup finished. */
public final class HandoffJobService extends JobService {
    @Override public boolean onStartJob(JobParameters params) {
        if (HelperHandoff.autoAttempted(this)) return false;
        if (HelperHandoff.ready(this)) {
            DpcProgress.stage(this, "helper_open", "Otwieram Retail Helper. Wprowadź dane demo…");
            if (HelperHandoff.launchAuto(this)) return false;
            // A thrown startActivity failure can be retried with JobScheduler backoff.
        }
        int attempt = getSharedPreferences("helper_handoff_094", MODE_PRIVATE)
            .getInt("attempt", 0);
        if (attempt >= 20) {
            State.log(this, "Handoff: nie uruchomiono Helpera automatycznie. Użyj przycisku w DPC.");
            return false;
        }
        getSharedPreferences("helper_handoff_094", MODE_PRIVATE).edit()
            .putInt("attempt", attempt + 1).apply();
        // Return true because we complete the pending job asynchronously; Android
        // automatically reschedules it with exponential backoff when needsReschedule=true.
        new Handler(Looper.getMainLooper()).post(() -> jobFinished(params, true));
        return true;
    }
    @Override public boolean onStopJob(JobParameters params) { return true; }
}
