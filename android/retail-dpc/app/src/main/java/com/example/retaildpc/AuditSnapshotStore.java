package com.example.retaildpc;

import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.provider.MediaStore;
import android.provider.Settings;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Privacy-limited, best-effort snapshot independent of app-private preferences.
 *
 * Only app versions, allowed system-package status and session metadata are saved.
 * NEVER save QR data, store credentials, Retail passwords, or verbose setup logs.
 * MediaStore Downloads are per Android user; user switches, guest resets,
 * storage restrictions and factory resets may make an earlier file unavailable.
 */
public final class AuditSnapshotStore {
    private static final String PREF = "retail_audit_snapshot_01119";
    private static final String FOLDER = Environment.DIRECTORY_DOWNLOADS + "/RetailSetup/";
    private AuditSnapshotStore() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
    }

    private static String sessionToken(Context c) {
        SharedPreferences p=prefs(c);
        String id=p.getString("session_marker",null);
        if(id==null) {
            id=UUID.randomUUID().toString();
            p.edit().putString("session_marker",id).commit();
        }
        return id;
    }

    static String contextReport(Context c) {
        StringBuilder b=new StringBuilder("SESJA ANDROID — Retail Setup 0.11.19\n");
        b.append("Wersja aplikacji: ").append(BuildConfig.VERSION_NAME).append("\n");
        b.append("UserHandle: ").append(Process.myUserHandle().toString()).append("\n");
        b.append("Process UID: ").append(Process.myUid()).append("\n");
        b.append("Id sesji danych aplikacji: ").append(sessionToken(c)).append("\n");
        b.append("Android API: ").append(Build.VERSION.SDK_INT).append("\n");
        DevicePolicyManager dpm=c.getSystemService(DevicePolicyManager.class);
        b.append("Device Owner: ").append(dpm!=null &&
            dpm.isDeviceOwnerApp(c.getPackageName())).append("\n");
        b.append("Device Admin aktywny: ").append(dpm!=null &&
            dpm.isAdminActive(new android.content.ComponentName(c,AdminReceiver.class)))
            .append("\n");
        b.append("Retail Mode APK: ").append(State.installed(c)).append(" (zapis aplikacji)\n");
        b.append("device_provisioned=").append(Settings.Global.getInt(
            c.getContentResolver(),"device_provisioned",-1)).append("; ");
        b.append("user_setup_complete=").append(Settings.Secure.getInt(
            c.getContentResolver(),"user_setup_complete",-1)).append("\n");
        b.append("Ostatnia kopia: ").append(
            prefs(c).getString("last_snapshot","brak w pamięci bieżącej sesji")).append("\n");
        b.append("Uwaga: inny UserHandle oznacza innego użytkownika Androida; ")
            .append("nowe ID sesji danych oznacza nowy lokalny zapis (np. czyszczenie ")
            .append("danych, nowy użytkownik lub reinstalacja), nie wskazuje samo przyczyny.\n");
        return b.toString();
    }

    private static String safeStage(String input) {
        return input.replaceAll("[^a-zA-Z0-9_-]","_");
    }

    /** Only version and allowlisted-system-app metadata; no private setup logs. */
    static String safeReport(Context c, String stage) {
        return "RETAIL SETUP — KOPIA DIAGNOSTYCZNA\n" +
            "Etap: " + stage + "\n" +
            "Data: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()).format(new Date()) + "\n\n" +
            contextReport(c) + "\n" +
            GooglePlayVersionAudit.report(c) + "\n\n" +
            StockAppRecovery.report(c) + "\n";
    }

    /**
     * Write plaintext diagnostic to shared Downloads (user-visible).
     * Never blocks Retail Mode on exceptions. No storage permission on API 31+.
     * Repeated stages produce distinct files so earlier evidence isn't overwritten.
     */
    public static synchronized String save(Context c, String stage) {
        String stamp=new SimpleDateFormat("yyyyMMdd_HHmmss_SSS",
            Locale.US).format(new Date());
        String name="RetailSetup-01119-"+stamp+"-"+safeStage(stage)+".txt";
        ContentResolver resolver=c.getContentResolver();
        Uri uri=null;
        try {
            ContentValues values=new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME,name);
            values.put(MediaStore.MediaColumns.MIME_TYPE,"text/plain");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH,FOLDER);
            values.put(MediaStore.MediaColumns.IS_PENDING,1);
            uri=resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
            if(uri==null)throw new IllegalStateException("Brak URI MediaStore");
            try(OutputStream output=resolver.openOutputStream(uri,"w")) {
                if(output==null)throw new IllegalStateException("Brak strumienia zapisu");
                output.write(safeReport(c,stage).getBytes(StandardCharsets.UTF_8));
                output.flush();
            }
            ContentValues complete=new ContentValues();
            complete.put(MediaStore.MediaColumns.IS_PENDING,0);
            resolver.update(uri,complete,null,null);
            String status="Pobrane/RetailSetup/"+name;
            prefs(c).edit().putString("last_snapshot",status).commit();
            State.log(c,"AuditSnapshot: zapisano "+status);
            return status;
        } catch(Exception ex) {
            if(uri!=null)try {resolver.delete(uri,null,null);}catch(Exception ignored){}
            String status="Nie udało się zapisać kopii w Pobranych: "+
                ex.getClass().getSimpleName();
            State.log(c,"AuditSnapshot: "+status);
            return status;
        }
    }
}
