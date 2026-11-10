package com.example.retaildpc;
import android.app.admin.DevicePolicyManager;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
/**
 * After actual Retail APK installation, drop DO and admin while keeping the
 * very same app for the user-consented Accessibility demo automation.
 */
final class SingleAppOwnerRelease {
    private static final String P="retail_all_in_one_release_011";
    private SingleAppOwnerRelease(){}
    static boolean blocked(Context c){
        return c.getSharedPreferences(P,Context.MODE_PRIVATE)
            .getBoolean("release_prepared",false);
    }
    static boolean isOwner(Context c){
        DevicePolicyManager dpm=c.getSystemService(DevicePolicyManager.class);
        return dpm!=null&&dpm.isDeviceOwnerApp(c.getPackageName());
    }
    static boolean released(Context c){
        DevicePolicyManager dpm=c.getSystemService(DevicePolicyManager.class);
        return dpm!=null&&!dpm.isDeviceOwnerApp(c.getPackageName())&&
            !dpm.isAdminActive(new ComponentName(c,AdminReceiver.class));
    }
    static synchronized boolean release(Context c,boolean emergency){
        if(!emergency&&!State.installed(c))return false;
        DevicePolicyManager dpm=c.getSystemService(DevicePolicyManager.class);
        if(dpm==null)return false;
        ComponentName admin=new ComponentName(c,AdminReceiver.class);
        if(released(c)){
            if (emergency) RetailHomePolicy.disableHomeAlias(c);
            return true;
        }
        // Must run before Device Owner relinquishes policy privileges.
        // Best effort only: failure of a stock app must never block Retail Mode.
        if (!emergency && dpm.isDeviceOwnerApp(c.getPackageName())) {
            try {
                DpcProgress.stage(c, "stock_app_restore",
                    "Przywracam dostępne aplikacje fabryczne…");
                StockAppRecovery.restore(c);
            } catch (RuntimeException ex) {
                State.log(c, "Przywracanie aplikacji fabrycznych: "
                    + ex.getClass().getSimpleName());
            }
        }
        try{
            if(dpm.isDeviceOwnerApp(c.getPackageName())){
                RetailHomePolicy.clear(c); // BEFORE clearDeviceOwnerApp()
                // Do NOT disable HomeActivity here. On Samsung it can close
                // the foreground HOME-backed setup task before START is shown.
                // MainActivity is first promoted into a separate ReadyActivity
                // task. HOME alias is disabled on user START or manual exit.
                c.getSharedPreferences(P,Context.MODE_PRIVATE).edit()
                    .putBoolean("release_prepared",true).commit();
                JobScheduler js=c.getSystemService(JobScheduler.class);
                if(js!=null)js.cancel(Constants.INSTALL_JOB_ID);
                dpm.clearDeviceOwnerApp(c.getPackageName());
            }
            if(!dpm.isDeviceOwnerApp(c.getPackageName())&&dpm.isAdminActive(admin))
                dpm.removeActiveAdmin(admin);
            if(!released(c)){
                State.log(c,"Nie udało się zwolnić uprawnień administratora");
                return false;
            }
            if (emergency) RetailHomePolicy.disableHomeAlias(c);
            // Keep BootReceiver until demo completes: on reboot it can restore
            // the pending START notification; it no longer requests owner policies.
            DpcProgress.clear(c);
            if(!emergency) GooglePlayVersionAudit.checkpoint(c,"release_owner");
            DpcProgress.showReady(c);
            State.log(c,"DPC bez Device Owner, pakiet nadal zainstalowany");
            return true;
        }catch(RuntimeException e){
            State.log(c,"Zwolnienie DPC: "+e.getClass().getSimpleName());
            return false;
        }
    }
    private static void disable(Context c,String suffix){
        try{
            c.getPackageManager().setComponentEnabledSetting(
                new ComponentName(c.getPackageName(),c.getPackageName()+"."+suffix),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        }catch(RuntimeException e){
            State.log(c,"Nie ukryto "+suffix+": "+e.getClass().getSimpleName());
        }
    }
}
