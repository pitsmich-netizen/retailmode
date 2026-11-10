package com.example.retaildpc;

import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.os.Build;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

/**
 * Downloads only fixed HTTPS APK URLs with pinned SHA-256, one at a time.
 * Without Device Owner, Android may require the user to approve installation.
 */
public final class PostDemoAppsJobService extends JobService {
    private volatile boolean stopped;
    private static final long MAX_BYTES=250L*1024L*1024L;
    @Override public boolean onStartJob(JobParameters params) {
        stopped=false;
        new Thread(()->{
            boolean reschedule=false;
            try {process();}
            catch(Exception ex){
                State.log(this,"PostDemo job: "+ex.getClass().getSimpleName());
                PostDemoAppRestore.p(this).edit().putString("state",
                    "Błąd pobierania: "+ex.getClass().getSimpleName()).apply();
                reschedule=!stopped;
            } finally {jobFinished(params,reschedule);}
        },"post-demo-apk-download").start();
        return true;
    }
    @Override public boolean onStopJob(JobParameters params) {
        stopped=true;
        return true;
    }
    private void process() throws Exception {
        SharedPreferences p=PostDemoAppRestore.p(this);
        if(!p.getBoolean("started",false))return;
        for(ApprovedApkCatalog.Source s:ApprovedApkCatalog.entries()){
            if(stopped)return;
            String pkg=s.packageName;
            if(PostDemoAppRestore.available(this,pkg)){
                p.edit().putString("result_"+pkg,"już zainstalowana")
                    .putBoolean("done_"+pkg,true).apply();
                continue;
            }
            if(p.getBoolean("done_"+pkg,false)||p.getBoolean("failed_"+pkg,false))
                continue;
            if(p.getBoolean("waiting_"+pkg,false))return;
            if(!s.valid()){
                p.edit().putString("result_"+pkg,"odrzucono nieprawidłowe źródło")
                    .putBoolean("failed_"+pkg,true).apply();
                continue;
            }
            p.edit().putString("state","Pobieram: "+s.label)
                .putString("result_"+pkg,"pobieranie").commit();
            File apk=new File(getCacheDir(),"postdemo-"+Integer.toHexString(pkg.hashCode())+".apk");
            try {
                downloadVerified(s,apk);
                if(stopped)return;
                PackageInfo info=getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(),0);
                if(info==null||!pkg.equals(info.packageName))
                    throw new SecurityException("APK ma inną nazwę pakietu");
                PackageInstaller installer=getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams sp=new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                sp.setAppPackageName(pkg);
                if(Build.VERSION.SDK_INT>=31)
                    sp.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
                int sessionId=installer.createSession(sp);
                try(PackageInstaller.Session session=installer.openSession(sessionId)) {
                    // PackageInstaller requires all APK write streams to be
                    // CLOSED before commit(), otherwise the commit can fail.
                    try(FileInputStream in=new FileInputStream(apk);
                        java.io.OutputStream out=session.openWrite("base.apk",0,apk.length())) {
                        byte[] bytes=new byte[65536];int n;
                        while((n=in.read(bytes))!=-1)out.write(bytes,0,n);
                        session.fsync(out);
                    }
                    Intent done=new Intent(this,PostDemoInstallReceiver.class)
                        .setAction(getPackageName()+".POST_DEMO_RESULT")
                        .putExtra("pkg",pkg);
                    PendingIntent pi=PendingIntent.getBroadcast(this,sessionId,done,
                        PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE);
                    p.edit().putBoolean("waiting_"+pkg,true)
                        .putString("result_"+pkg,"czekam na wynik instalacji").commit();
                    session.commit(pi.getIntentSender());
                }
                // Only one pending installation at a time. Callback schedules the next.
                return;
            } catch(Exception ex){
                State.log(this,"PostDemo "+pkg+": "+ex.getClass().getSimpleName());
                p.edit().putBoolean("failed_"+pkg,true)
                    .putString("result_"+pkg,"błąd: "+ex.getClass().getSimpleName()).apply();
            } finally {apk.delete();}
        }
        p.edit().putString("state","Sprawdzono wszystkie zatwierdzone APK").apply();
        PostDemoAppRestore.audit(this);
    }
    private void downloadVerified(ApprovedApkCatalog.Source s,File out) throws Exception {
        HttpURLConnection conn=null;
        try {
            URL url=new URL(s.url);
            conn=(HttpURLConnection)url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(15000);conn.setReadTimeout(25000);
            if(conn.getResponseCode()!=200)
                throw new java.io.IOException("HTTPS status "+conn.getResponseCode());
            long length=conn.getContentLengthLong();
            if(length>MAX_BYTES)throw new java.io.IOException("APK za duże");
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            long count=0;
            try(InputStream in=conn.getInputStream();FileOutputStream fos=new FileOutputStream(out)){
                byte[] bytes=new byte[65536];int n;
                while((n=in.read(bytes))!=-1){
                    if(stopped)throw new java.io.IOException("Przerwano");
                    count+=n;
                    if(count>MAX_BYTES)throw new java.io.IOException("APK za duże");
                    digest.update(bytes,0,n);
                    fos.write(bytes,0,n);
                }
            }
            if(count==0)throw new java.io.IOException("Pusty APK");
            StringBuilder hex=new StringBuilder();
            for(byte b:digest.digest())hex.append(String.format("%02x",b&0xff));
            if(!hex.toString().equalsIgnoreCase(s.sha256))
                throw new SecurityException("Niezgodny SHA-256 APK");
        } finally {if(conn!=null)conn.disconnect();}
    }
}
