package com.example.retaildpc;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Passive comparison of Google/stock package versions during QR provisioning.
 * No Play Store intents, no account operations, APKs, downloads or updates.
 * IMPORTANT: a version change does not identify which installer did the update.
 */
public final class GooglePlayVersionAudit {
    private static final String PREF = "retail_google_play_version_audit_01118";
    static final int JOB_AFTER_15_MIN = 20820, JOB_AFTER_60_MIN = 20821;
    static final String PHASE_15 = "po_15_min", PHASE_60 = "po_60_min";

    // Explicit package visibility in AndroidManifest.xml; no QUERY_ALL_PACKAGES.
    private static final String[][] PACKAGES = {
        {"Google Play", "com.android.vending"},
        {"Usługi Google Play", "com.google.android.gms"},
        {"Google", "com.google.android.googlequicksearchbox"},
        {"Chrome", "com.android.chrome"},
        {"YouTube", "com.google.android.youtube"},
        {"Mapy Google", "com.google.android.apps.maps"},
        {"Gmail", "com.google.android.gm"},
        {"Zdjęcia Google", "com.google.android.apps.photos"},
        {"Android System WebView", "com.google.android.webview"},
        {"Kalendarz Samsung", "com.samsung.android.calendar"},
        {"Zegar Samsung", "com.sec.android.app.clockpackage"},
        {"Kalkulator Samsung", "com.sec.android.app.popupcalculator"},
        {"Samsung Notes", "com.samsung.android.app.notes"}
    };

    private GooglePlayVersionAudit() {}
    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }
    private static String sample(Context c, String packageName) {
        try {
            PackageInfo info = c.getPackageManager().getPackageInfo(packageName,
                PackageManager.MATCH_UNINSTALLED_PACKAGES);
            if(info.applicationInfo == null ||
                    (info.applicationInfo.flags & ApplicationInfo.FLAG_INSTALLED) == 0)
                return "NIEZAINSTALOWANA";
            return "v" + info.getLongVersionCode() + " (" +
                (info.versionName == null ? "?" : info.versionName) + ")";
        } catch (PackageManager.NameNotFoundException ex) {
            return "BRAK / NIEWIDOCZNA";
        } catch (RuntimeException ex) {
            return "ODCZYT: " + ex.getClass().getSimpleName();
        }
    }

    private static String readable(String value) {
        return value == null ? "BRAK" : value;
    }
    private static String stamp(long millis) {
        return millis <= 0 ? "—" : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()).format(new Date(millis));
    }

    /** First recorded state is immutable for this provisioning run. */
    static synchronized void baseline(Context c) {
        SharedPreferences p = prefs(c);
        if(p.getBoolean("baseline_taken",false))return;
        SharedPreferences.Editor edit = p.edit();
        for(String[] app:PACKAGES)edit.putString("base_"+app[1],sample(c,app[1]));
        edit.putLong("baseline_at",System.currentTimeMillis());
        edit.putBoolean("baseline_taken",true);
        edit.commit();
        State.log(c,"GooglePlayAudit: zapisano punkt początkowy " + PACKAGES.length + " aplikacji");
    }

    /** Idempotent named checkpoint; 'ręczne' may be refreshed repeatedly. */
    static synchronized void checkpoint(Context c,String phase) {
        if(!prefs(c).getBoolean("baseline_taken",false)) {
            State.log(c,"GooglePlayAudit: brak punktu początkowego; nie tworzę pomiaru porównawczego");
            return;
        }
        if(!"reczne".equals(phase) && prefs(c).getLong("time_"+phase,0)>0)return;
        SharedPreferences.Editor edit = prefs(c).edit();
        for(String[] app:PACKAGES)edit.putString(phase+"_"+app[1],sample(c,app[1]));
        edit.putLong("time_"+phase,System.currentTimeMillis()).commit();
        State.log(c,"GooglePlayAudit: pomiar "+phase);
    }

    /** Triggered only after Retail Mode positively acknowledges demo download. */
    public static synchronized void demoStarted(Context c) {
        checkpoint(c,"demo_start");
        SharedPreferences p=prefs(c);
        if(p.getBoolean("jobs_scheduled",false))return;
        JobScheduler scheduler=c.getSystemService(JobScheduler.class);
        if(scheduler==null){
            State.log(c,"GooglePlayAudit: JobScheduler niedostępny");
            return;
        }
        boolean ok=true;
        for(int i=0;i<2;i++){
            int id=i==0?JOB_AFTER_15_MIN:JOB_AFTER_60_MIN;
            long delay=i==0?15*60*1000L:60*60*1000L;
            JobInfo job=new JobInfo.Builder(id,
                new ComponentName(c,GooglePlayVersionJobService.class))
                .setMinimumLatency(delay)
                .setPersisted(true)
                .build();
            int status=scheduler.schedule(job);
            if(status!=JobScheduler.RESULT_SUCCESS)ok=false;
        }
        p.edit().putBoolean("jobs_scheduled",ok).apply();
        State.log(c,"GooglePlayAudit: pomiary 15/60 min: "+(ok?"zaplanowane":"częściowo niedostępne"));
    }

    static synchronized String report(Context c) {
        SharedPreferences p=prefs(c);
        StringBuilder b=new StringBuilder("PORÓWNANIE WERSJI APLIKACJI — 0.11.18\n");
        b.append("Nie pobieramy ani nie aktualizujemy żadnych aplikacji.\n");
        b.append("Przed QR: zwykły kreator Play może mieć własną kolejkę, ")
            .append("której nie odtwarzamy w Android Enterprise.\n");
        b.append("Początek: ").append(stamp(p.getLong("baseline_at",0))).append("\n");
        if(!p.getBoolean("baseline_taken",false)){
            return b.append("BRAK PUNKTU POCZĄTKOWEGO — nie można ocenić zmian wersji.").toString();
        }
        String[] phases={"release_owner","demo_start",PHASE_15,PHASE_60,"reczne"};
        String[] labels={"Po zwolnieniu Device Owner","Rozpoczęcie pobierania demo",
            "Pomiar po ok. 15 minutach","Pomiar po ok. 60 minutach","Ostatni pomiar ręczny"};
        for(int k=0;k<phases.length;k++){
            String phase=phases[k];
            long at=p.getLong("time_"+phase,0);
            b.append("\n").append(labels[k]).append(": ").append(stamp(at)).append("\n");
            if(at==0){b.append("Oczekuje / nie dotyczy\n");continue;}
            int changed=0,unchanged=0;
            for(String[] app:PACKAGES) {
                String before=readable(p.getString("base_"+app[1],null));
                String after=readable(p.getString(phase+"_"+app[1],null));
                if(before.equals(after))unchanged++;
                else changed++;
            }
            b.append("Zmiana wersji lub stanu pakietu: ").append(changed)
                .append("; bez zmian: ").append(unchanged).append("\n");
        }
        b.append("\nAPLIKACJE (początek → ostatni dostępny pomiar)\n");
        String lastPhase="reczne";
        if(p.getLong("time_"+lastPhase,0)==0) {
            for(int i=phases.length-2;i>=0;i--) {
                if(p.getLong("time_"+phases[i],0)>0){
                    lastPhase=phases[i];break;
                }
            }
        }
        for(String[] app:PACKAGES){
            String before=readable(p.getString("base_"+app[1],null));
            String after=p.getLong("time_"+lastPhase,0)>0
                ?readable(p.getString(lastPhase+"_"+app[1],null))
                :before;
            b.append(app[0]).append(" [").append(app[1]).append("]\n")
                .append("  ").append(before).append(" → ").append(after)
                .append(before.equals(after)?"":"  [ZMIANA]").append("\n");
        }
        b.append("\nUWAGA: ten pomiar wykrywa różnice wersji lub dostępności, ")
            .append("nie rozpoznaje, czy zaktualizował je Google Play, Galaxy Store czy system. ")
            .append("Brak zmiany nie dowodzi, że Play nie działa.");
        return b.toString();
    }
}
