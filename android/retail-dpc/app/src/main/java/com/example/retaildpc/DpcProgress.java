package com.example.retaildpc;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;
import java.util.Locale;

/** Status in the app and optional system notifications. No instructional Toasts. */
final class DpcProgress {
    private static final String CHANNEL="retail_setup_all_in_one";
    private static final int STATUS_ID=12201, PROGRESS_ID=12202;
    private static final String PREF="retail_progress_100";
    private static long lastAt;
    private DpcProgress(){}

    static void preparePermission(Context c){
        if(Build.VERSION.SDK_INT<33)return;
        try{
            DevicePolicyManager d=c.getSystemService(DevicePolicyManager.class);
            if(d!=null&&d.isDeviceOwnerApp(c.getPackageName()))
                d.setPermissionGrantState(new ComponentName(c,AdminReceiver.class),
                    c.getPackageName(),Manifest.permission.POST_NOTIFICATIONS,
                    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        }catch(RuntimeException e){
            State.log(c,"Notifications permission: "+e.getClass().getSimpleName());
        }
    }
    private static SharedPreferences p(Context c){
        return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
    }
    static synchronized void stage(Context c,String key,String message){
        if(SingleAppOwnerRelease.blocked(c))return;
        p(c).edit().putString("last_stage",key).putString("last_message",message).apply();
        if(!key.startsWith("retail_downloading"))cancel(c,PROGRESS_ID);
        notify(c,STATUS_ID,"Retail Setup",message,-1);
    }
    static String currentStage(Context c){
        return p(c).getString("last_message","Przygotowuję aplikację…");
    }
    static int retailPercent(Context c){return p(c).getInt("retail_percent",-1);}
    static synchronized void downloadProgress(Context c,String key,String title,
                                                long read,long total){
        if(SingleAppOwnerRelease.blocked(c))return;
        long now=SystemClock.elapsedRealtime();
        boolean done=total>0&&read>=total;
        if(!done&&lastAt>0&&now-lastAt<1100)return;
        lastAt=now;
        int percent=total>0?(int)Math.max(0,Math.min(100,read*100/total)):-1;
        p(c).edit().putString("last_stage",key)
            .putString("last_message","Pobieranie Retail Mode…")
            .putInt("retail_percent",percent)
            .putLong("retail_bytes",read).putLong("retail_total",total).apply();
        String message=percent>=0?"Retail Mode: "+percent+"% ("
            +String.format(Locale.ROOT,"%.1f",read/1000000.0)+" MB)"
            :"Retail Mode: "+String.format(Locale.ROOT,"%.1f",read/1000000.0)+" MB";
        notify(c,PROGRESS_ID,title,message,percent);
    }
    static String lastSetupDiagnostic(Context c){
        return p(c).getString("setup_result","Brak ukończonego pomiaru");
    }
    static synchronized void setupFlagDiagnostics(Context c,String task,boolean provisioned,
        boolean finished,long elapsed,long provisionedAt,long finishedAt){
        if(!"retail".equals(task)||SingleAppOwnerRelease.blocked(c))return;
        String missing=!provisioned?"device_provisioned":!finished?"user_setup_complete":"gotowe";
        p(c).edit().putString("last_message","Czekam na konfigurację: "+missing).apply();
    }
    static synchronized void completeSetupFlagDiagnostics(Context c,String task,
        long queuedMs,long gateMs,long deviceAt,long userAt){
        if(!"retail".equals(task))return;
        String summary="JobScheduler: "+(queuedMs/1000.0)+" s"
            +"\nCzekanie na flagi: "+(gateMs/1000.0)+" s"
            +"\ndevice_provisioned: "+(deviceAt==0?"gotowa na starcie":"po "+deviceAt+" ms")
            +"\nuser_setup_complete: "+(userAt==0?"gotowa na starcie":"po "+userAt+" ms");
        p(c).edit().putString("setup_result",summary).apply();
    }
    static void clearSetupFlagDiagnostics(Context c,String task){ }
    private static void notify(Context c,int id,String title,String message,int pct){
        try{
            NotificationManager nm=c.getSystemService(NotificationManager.class);
            if(nm==null)return;
            if(Build.VERSION.SDK_INT>=26){
                NotificationChannel channel=new NotificationChannel(CHANNEL,
                    "Przygotowanie Retail Mode",NotificationManager.IMPORTANCE_LOW);
                nm.createNotificationChannel(channel);
            }
            if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
            Notification.Builder b=Build.VERSION.SDK_INT>=26
                ?new Notification.Builder(c,CHANNEL):new Notification.Builder(c);
            b.setSmallIcon(R.drawable.retail_progress)
                .setContentTitle(title).setContentText(message)
                .setSubText("Retail Setup All-in-One • 0.11.1")
                .setOnlyAlertOnce(true).setOngoing(true)
                .setCategory(Notification.CATEGORY_STATUS).setShowWhen(false);
            if(id==PROGRESS_ID){
                if(pct>=0)b.setProgress(100,pct,false);
                else b.setProgress(0,0,true);
            }
            // Backup entry point if an OEM ignores the temporary HOME policy.
            // A user can always tap the existing download/status notification.
            Intent open=new Intent(c,MainActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pending=PendingIntent.getActivity(c,0,open,
                PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
            b.setContentIntent(pending);
            nm.notify(id,b.build());
        }catch(RuntimeException e){State.log(c,"Notyfikacja: "+e.getClass().getSimpleName());}
    }
    private static void cancel(Context c,int id){
        NotificationManager nm=c.getSystemService(NotificationManager.class);
        if(nm!=null)nm.cancel(id);
    }
    static synchronized void clear(Context c){
        cancel(c,STATUS_ID);cancel(c,PROGRESS_ID);
    }
}
