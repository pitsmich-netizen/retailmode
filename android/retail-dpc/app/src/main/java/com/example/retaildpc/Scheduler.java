package com.example.retaildpc;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
/** One installer: expedited when possible, otherwise normal persisted job. */
final class Scheduler {
    private Scheduler() {}
    static void schedule(Context c) {
        if(DiagnosticMode.enabled(c)||SingleAppOwnerRelease.blocked(c)||!SingleAppOwnerRelease.isOwner(c)
            ||State.installed(c))return;
        JobScheduler js=c.getSystemService(JobScheduler.class);
        if(js==null)return;
        try {
            if(js.getPendingJob(Constants.INSTALL_JOB_ID)!=null)return;
            int result=JobScheduler.RESULT_FAILURE;
            try {
                result=js.schedule(job(c,true));
                State.log(c,"Retail expedited result="+result);
            }catch(RuntimeException e){
                State.log(c,"Expedited fallback: "+e.getClass().getSimpleName());
            }
            if(result!=JobScheduler.RESULT_SUCCESS)result=js.schedule(job(c,false));
            if(result==JobScheduler.RESULT_SUCCESS)SetupNetworkGate.markQueued(c,"retail");
        }catch(RuntimeException e){
            State.log(c,"JobScheduler: "+e.getClass().getSimpleName());
        }
    }
    private static JobInfo job(Context c,boolean expedited){
        JobInfo.Builder b=new JobInfo.Builder(Constants.INSTALL_JOB_ID,
            new ComponentName(c,InstallJobService.class))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setBackoffCriteria(30000,JobInfo.BACKOFF_POLICY_EXPONENTIAL)
            .setPersisted(true);
        if(expedited)b.setExpedited(true);
        return b.build();
    }
}
