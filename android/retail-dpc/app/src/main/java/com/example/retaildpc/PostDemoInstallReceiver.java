package com.example.retaildpc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInstaller;
import android.os.Build;

/** PackageInstaller status; prompts user visibly instead of bypassing Android consent. */
public final class PostDemoInstallReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent) {
        if(intent==null)return;
        String pkg=intent.getStringExtra("pkg");
        if(pkg==null||pkg.isEmpty())return;
        SharedPreferences p=PostDemoAppRestore.p(c);
        int status=intent.getIntExtra(PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE);
        if(status==PackageInstaller.STATUS_PENDING_USER_ACTION){
            Intent approval=Build.VERSION.SDK_INT>=33
                ?intent.getParcelableExtra(Intent.EXTRA_INTENT,Intent.class)
                :(Intent)intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if(approval==null){
                p.edit().putBoolean("waiting_"+pkg,false)
                    .putBoolean("failed_"+pkg,true)
                    .putString("result_"+pkg,"Brak systemowego okna potwierdzenia").apply();
                PostDemoAppRestore.schedule(c);
                return;
            }
            p.edit().putString("state","Instalacja wymaga potwierdzenia użytkownika")
                .putString("result_"+pkg,"dotknij powiadomienia, aby potwierdzić").apply();
            showApproval(c,pkg,approval);
            return;
        }
        p.edit().putBoolean("waiting_"+pkg,false)
            .putBoolean("done_"+pkg,status==PackageInstaller.STATUS_SUCCESS)
            .putBoolean("failed_"+pkg,status!=PackageInstaller.STATUS_SUCCESS)
            .putString("result_"+pkg,status==PackageInstaller.STATUS_SUCCESS
                ?"zainstalowana":"nie zainstalowano (status "+status+")").apply();
        State.log(c,"PostDemo instalacja "+pkg+" status="+status);
        PostDemoAppRestore.schedule(c);
    }
    private static void showApproval(Context c,String pkg,Intent approval){
        try {
            NotificationManager nm=c.getSystemService(NotificationManager.class);
            if(nm==null)return;
            String channel="post_demo_app_approval";
            nm.createNotificationChannel(new NotificationChannel(channel,
                "Potwierdzenie instalacji aplikacji",NotificationManager.IMPORTANCE_HIGH));
            Intent open=new Intent(c,PostDemoApprovalActivity.class)
                .putExtra("system_approval",approval).putExtra("pkg",pkg);
            PendingIntent pi=PendingIntent.getActivity(c,728,open,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder b=new Notification.Builder(c,channel)
                .setSmallIcon(R.drawable.retail_progress)
                .setContentTitle("Zatwierdź instalację aplikacji")
                .setContentText(pkg)
                .setContentIntent(pi).setAutoCancel(true);
            nm.notify(22814,b.build());
        }catch(RuntimeException ex){
            State.log(c,"PostDemo powiadomienie: "+ex.getClass().getSimpleName());
        }
    }
}
