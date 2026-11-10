package com.example.retaildpc;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Conservative, allowlisted system-app recovery probe for factory-reset testing.
 * The key change in 0.11.13: try DevicePolicyManager.enableSystemApp even when
 * PackageManager cannot find the package. The previous precheck prevented every
 * missing Samsung app from ever reaching the API. This does NOT download APKs.
 */
final class StockAppRecovery {
    private static final String PREF = "retail_stock_apps_recovery_01113";
    private static final String[][] APPS = {
        {"Kalendarz Samsung", "com.samsung.android.calendar"},
        {"Kalkulator Samsung", "com.sec.android.app.popupcalculator"},
        {"Zegar Samsung", "com.sec.android.app.clockpackage"},
        {"Samsung Notes", "com.samsung.android.app.notes"},
        {"Samsung Members", "com.samsung.android.voc"},
        {"Bixby", "com.samsung.android.bixby.agent"},
        {"Wskazówki (Tips)", "com.samsung.android.app.tips"},
        {"Gaming Hub", "com.samsung.android.game.gamehome"},
        {"Samsung Global Goals", "com.samsung.sree"},
        {"Facebook", "com.facebook.katana"},
        {"Instagram", "com.instagram.android"},
        {"Spotify", "com.spotify.music"}
    };
    private StockAppRecovery() { }

    static boolean attempted(Context c) {
        return prefs(c).getBoolean("attempted", false);
    }
    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    static synchronized void restore(Context c) {
        restore(c, false);
    }
    /**
     * Repeat an actual restoration attempt only after the user explicitly
     * requests it in the diagnostic panel; do not repeatedly mutate packages
     * on every Activity or boot lifecycle callback.
     */
    static synchronized void restore(Context c, boolean force) {
        if (!force && attempted(c)) return;
        DevicePolicyManager dpm = c.getSystemService(DevicePolicyManager.class);
        if (dpm == null || !dpm.isDeviceOwnerApp(c.getPackageName())) {
            State.log(c,"StockAppProbe: odmowa; brak aktywnego Device Owner");
            return;
        }
        ComponentName admin = new ComponentName(c, AdminReceiver.class);
        PackageManager pm = c.getPackageManager();
        int already = 0, recovered = 0, missing = 0, failed = 0, attempted = 0;
        StringBuilder record = new StringBuilder();
        for (String[] entry : APPS) {
            String name = entry[0], pkg = entry[1];
            String before = state(pm, pkg);
            String after = before;
            String result;
            if ("ZAINSTALOWANA".equals(before)) {
                already++;
                result = "już dostępna; bez ingerencji";
            } else {
                attempted++;
                String factory = factoryFlag(pm,pkg);
                String outcome;
                try {
                    // The public Device Owner API itself validates whether a
                    // factory-preloaded system package can be re-enabled.
                    // Critically: never short-circuit when PM says BRAK.
                    dpm.enableSystemApp(admin, pkg);
                    outcome = "enableSystemApp: wywołanie OK";
                } catch (SecurityException ex) {
                    outcome = "enableSystemApp: SecurityException";
                } catch (IllegalArgumentException ex) {
                    outcome = "enableSystemApp: IllegalArgumentException";
                } catch (RuntimeException ex) {
                    outcome = "enableSystemApp: " + ex.getClass().getSimpleName();
                }
                after = state(pm, pkg);
                if ("ZAINSTALOWANA".equals(after)) {
                    recovered++;
                    result = "PRZYWRÓCONA";
                } else {
                    missing++;
                    if (!outcome.endsWith("wywołanie OK")) failed++;
                    result = "NIEPRZYWRÓCONA";
                }
                result += " | przed=" + before + " | po=" + after
                    + " | system=" + factory + " | " + outcome;
            }
            record.append(name).append(": ").append(result)
                .append(" [").append(pkg).append("]\n");
            State.log(c,"StockAppProbe: "+name+" - "+result);
        }
        String summary = "Dostępne wcześniej: " + already
            + "; próby: " + attempted + "; przywrócone: " + recovered
            + "; nadal brak/niewidoczne: " + missing
            + "; wyjątki API: " + failed;
        SharedPreferences p=prefs(c);
        p.edit().putBoolean("attempted",true)
            .putInt("attempts",p.getInt("attempts",0)+1)
            .putLong("timestamp",System.currentTimeMillis())
            .putString("summary",summary)
            .putString("results",record.toString()).commit();
        State.log(c,"StockAppProbe: "+summary);
    }

    private static String factoryFlag(PackageManager pm,String pkg) {
        try {
            ApplicationInfo app=pm.getApplicationInfo(pkg,PackageManager.MATCH_UNINSTALLED_PACKAGES);
            return (app.flags & (ApplicationInfo.FLAG_SYSTEM |
                ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0?"TAK":"NIE";
        } catch (PackageManager.NameNotFoundException e) {
            return "NIEUSTALONY (brak widoczności pakietu)";
        } catch (RuntimeException e) {
            return "BŁĄD ("+e.getClass().getSimpleName()+")";
        }
    }

    private static String state(PackageManager pm,String pkg) {
        try {
            ApplicationInfo a=pm.getApplicationInfo(pkg,
                PackageManager.MATCH_UNINSTALLED_PACKAGES);
            if ((a.flags & ApplicationInfo.FLAG_INSTALLED)==0)
                return "NIEZAINSTALOWANA DLA UŻYTKOWNIKA";
            if(!a.enabled)return "WYŁĄCZONA";
            return "ZAINSTALOWANA";
        } catch (PackageManager.NameNotFoundException e) {
            return "BRAK";
        } catch (RuntimeException e) {
            return "NIEUSTALONY ("+e.getClass().getSimpleName()+")";
        }
    }

    static String report(Context c) {
        SharedPreferences p=prefs(c);
        StringBuilder out=new StringBuilder();
        out.append("TEST ENABLE_SYSTEM_APP (0.11.13) PRZY AKTYWNYM DEVICE OWNER\n");
        if(p.getBoolean("attempted",false)) {
            out.append(p.getString("summary","Brak podsumowania")).append("\n");
            long timestamp=p.getLong("timestamp",0L);
            if(timestamp>0)out.append("Ostatni test: ")
                .append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault())
                    .format(new Date(timestamp))).append("\n");
            out.append("Numer próby: ").append(p.getInt("attempts",0)).append("\n");
            out.append("\nWYNIKI PRÓB\n").append(p.getString("results","Brak wyników"));
        } else {
            out.append("Jeszcze nie wykonano próby przywrócenia\n");
        }
        out.append("\nSTAN BIEŻĄCY\n");
        PackageManager pm=c.getPackageManager();
        for(String[] a:APPS)
            out.append(a[0]).append(": ").append(state(pm,a[1]))
                .append(" [").append(a[1]).append("]\n");
        out.append("\nBRAK może oznaczać pakiet nieobecny albo niewidoczny dla aplikacji. ")
            .append("enableSystemApp działa tylko na odpowiednich aplikacjach fabrycznych; ")
            .append("wywołanie API nie pobiera APK ani nie dowodzi, że pakiet istnieje.");
        return out.toString();
    }
}
