package com.example.retaildpc;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Starts after Samsung Retail Mode itself confirms download/registration.
 * Device Owner was already relinquished before START; this phase must not
 * assume silent installation or claim that a missing store app is recoverable.
 */
public final class PostDemoAppRestore {
    static final int JOB_ID=20814;
    private static final String PREF="retail_post_demo_restore_01111";
    private static final String[][] TARGETS = {
        {"Kalendarz Samsung","com.samsung.android.calendar"},
        {"Kalkulator Samsung","com.sec.android.app.popupcalculator"},
        {"Zegar Samsung","com.sec.android.app.clockpackage"},
        {"Samsung Notes","com.samsung.android.app.notes"},
        {"Samsung Members","com.samsung.android.voc"},
        {"Bixby","com.samsung.android.bixby.agent"},
        {"Wskazówki","com.samsung.android.app.tips"},
        {"Gaming Hub","com.samsung.android.game.gamehome"},
        {"Samsung Global Goals","com.samsung.sree"},
        {"Facebook","com.facebook.katana"},
        {"Instagram","com.instagram.android"},
        {"Spotify","com.spotify.music"}
    };
    private PostDemoAppRestore() {}

    static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
    }
    public static synchronized void onDemoDownloadStart(Context c) {
        if (p(c).getBoolean("started",false)) return;
        p(c).edit().putBoolean("started",true)
            .putLong("started_at",System.currentTimeMillis()).commit();
        State.log(c,"PostDemo: Samsung potwierdził rozpoczęcie pobierania; uruchamiam audyt");
        audit(c);
        schedule(c);
    }
    static synchronized void retry(Context c) {
        if (!p(c).getBoolean("started",false)) {
            State.log(c,"PostDemo: demo jeszcze nie potwierdzone; nie pobieram aplikacji");
            return;
        }
        SharedPreferences.Editor e=p(c).edit();
        for (ApprovedApkCatalog.Source s:ApprovedApkCatalog.entries()) {
            e.remove("failed_"+s.packageName);
            e.remove("waiting_"+s.packageName);
        }
        e.apply();
        audit(c);
        schedule(c);
    }
    static void resumeIfStarted(Context c) {
        if(p(c).getBoolean("started",false)) schedule(c);
    }
    static synchronized void audit(Context c) {
        PackageManager pm=c.getPackageManager();
        int installed=0,missing=0,disabled=0;
        StringBuilder details=new StringBuilder();
        for(String[] a:TARGETS) {
            String status;
            try {
                ApplicationInfo info=pm.getApplicationInfo(a[1],
                    PackageManager.MATCH_UNINSTALLED_PACKAGES);
                if ((info.flags & ApplicationInfo.FLAG_INSTALLED)==0) {
                    missing++;status="niezainstalowana dla użytkownika";
                } else if(!info.enabled) {disabled++;status="wyłączona";}
                else {installed++;status="dostępna";}
            } catch(PackageManager.NameNotFoundException ex) {
                missing++;status="brak pakietu";
            } catch(RuntimeException ex) {
                status="nie można ustalić";
            }
            details.append(a[0]).append(": ").append(status)
                .append(" [").append(a[1]).append("]\n");
        }
        String summary="Dostępne: "+installed+"; brak: "+missing+"; wyłączone: "+disabled;
        p(c).edit().putString("audit",summary+"\n"+details).apply();
        State.log(c,"PostDemo audyt: "+summary);
    }
    static synchronized void schedule(Context c) {
        if(!p(c).getBoolean("started",false))return;
        ApprovedApkCatalog.Source[] entries=ApprovedApkCatalog.entries();
        if(entries.length==0) {
            p(c).edit().putString("state",
                "Audyt wykonany. Brak zatwierdzonych źródeł APK: niczego nie pobrano. " +
                "Aplikacje sklepowe wymagają autoryzowanego kanału dystrybucji.").apply();
            State.log(c,"PostDemo: 0 zweryfikowanych APK, pomijam pobieranie");
            return;
        }
        JobScheduler js=c.getSystemService(JobScheduler.class);
        if(js==null) {p(c).edit().putString("state","Brak JobScheduler").apply();return;}
        JobInfo job=new JobInfo.Builder(JOB_ID,
            new ComponentName(c,PostDemoAppsJobService.class))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setBackoffCriteria(30000L,JobInfo.BACKOFF_POLICY_EXPONENTIAL)
            .setPersisted(true).build();
        int status=js.schedule(job);
        p(c).edit().putString("state",status==JobScheduler.RESULT_SUCCESS
            ?"Zlecono sprawdzenie i pobieranie zatwierdzonych APK"
            :"Nie udało się zlecić pobierania APK").apply();
    }
    static boolean available(Context c,String pkg) {
        try {
            ApplicationInfo a=c.getPackageManager().getApplicationInfo(pkg,0);
            return a.enabled;
        } catch(Exception e) {return false;}
    }
    public static String report(Context c) {
        SharedPreferences p=p(c);
        StringBuilder b=new StringBuilder("PO DEMO — PRZYWRACANIE APLIKACJI\n");
        b.append("Start: ").append(p.getBoolean("started",false)?"TAK":"NIE").append("\n");
        long at=p.getLong("started_at",0);
        if(at!=0)b.append("Uruchomiono: ").append(new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",Locale.getDefault()).format(new Date(at))).append("\n");
        b.append("Stan: ").append(p.getString("state","Oczekuję na potwierdzenie pobierania demo")).append("\n");
        ApprovedApkCatalog.Source[] entries=ApprovedApkCatalog.entries();
        b.append("Zatwierdzone źródła APK: ").append(entries.length).append("\n");
        for(ApprovedApkCatalog.Source s:entries)
            b.append(s.label).append(" [").append(s.packageName).append("]: ")
                .append(p.getString("result_"+s.packageName,"oczekuje")).append("\n");
        b.append("\n").append(p.getString("audit","Audytu jeszcze nie wykonano"));
        b.append("\nAplikacje fabryczne są przywracane wcześniej, przed zwolnieniem Device Owner.");
        return b.toString();
    }
}
