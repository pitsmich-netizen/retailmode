package com.example.retaildpc;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
/** Temporary HOME during setup. Never hard Lock Task in the first trial. */
final class RetailHomePolicy {
    private RetailHomePolicy(){}
    static boolean ensure(Context c){
        DevicePolicyManager dpm=c.getSystemService(DevicePolicyManager.class);
        if(dpm==null||!dpm.isDeviceOwnerApp(c.getPackageName())
            ||SingleAppOwnerRelease.blocked(c))return false;
        try{
            IntentFilter f=new IntentFilter(Intent.ACTION_MAIN);
            f.addCategory(Intent.CATEGORY_HOME);f.addCategory(Intent.CATEGORY_DEFAULT);
            dpm.addPersistentPreferredActivity(
                new ComponentName(c,AdminReceiver.class),f,
                new ComponentName(c.getPackageName(),c.getPackageName()+".HomeActivity"));
            State.log(c,"Retail Setup: tymczasowy HOME");
            return true;
        }catch(RuntimeException e){
            State.log(c,"HOME niedostępny: "+e.getClass().getSimpleName());return false;
        }
    }
    static void clear(Context c){
        DevicePolicyManager dpm=c.getSystemService(DevicePolicyManager.class);
        if(dpm!=null&&dpm.isDeviceOwnerApp(c.getPackageName()))
            dpm.clearPackagePersistentPreferredActivities(
                new ComponentName(c,AdminReceiver.class),c.getPackageName());
    }
}
