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
 * EXPLICIT, test-device-only diagnostic.
 * Makes a single narrowly scoped attempt to re-enable Samsung's *preloaded*
 * Recommended apps / OMC package. Does not launch hidden OEM components.
 */
final class SamsungOmcEnableProbe {
    private static final String OMC="com.samsung.android.app.omcagent";
    private static final String PREF="retail_omc_enable_probe_01115";
    private SamsungOmcEnableProbe() {}

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
    }
    private static String state(PackageManager pm) {
        try {
            ApplicationInfo a=pm.getApplicationInfo(OMC,
                PackageManager.MATCH_UNINSTALLED_PACKAGES);
            int enabledSetting=pm.getApplicationEnabledSetting(OMC);
            boolean installed=(a.flags & ApplicationInfo.FLAG_INSTALLED)!=0;
            boolean system=(a.flags &
                (ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0;
            return "obecny=TAK; installed="+installed+"; system="+system
                +"; enabled="+a.enabled+"; enabledSetting="+enabledSetting
                +" ("+settingName(enabledSetting)+")";
        } catch(PackageManager.NameNotFoundException e) {
            return "NIE WIDAC pakietu OMC";
        } catch(RuntimeException e) {
            return "BŁĄD STANU: "+e.getClass().getSimpleName();
        }
    }
    private static String settingName(int v) {
        switch(v) {
            case PackageManager.COMPONENT_ENABLED_STATE_DEFAULT:return "DEFAULT";
            case PackageManager.COMPONENT_ENABLED_STATE_ENABLED:return "ENABLED";
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED:return "DISABLED";
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER:return "DISABLED_USER";
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED:
                return "DISABLED_UNTIL_USED";
            default:return "INNY";
        }
    }
    static synchronized void attempt(Context c) {
        SharedPreferences p=prefs(c);
        PackageManager pm=c.getPackageManager();
        String before=state(pm);
        StringBuilder b=new StringBuilder("TEST OMC 0.11.15\n");
        b.append("PRZED: ").append(before).append("\n");
        if(!DiagnosticMode.enabled(c)||!DiagnosticMode.probeOnly(c)) {
            b.append("NIE WYKONANO: to nie QR diagnostyki OMC");
        } else {
            DevicePolicyManager dpm=c.getSystemService(DevicePolicyManager.class);
            if(dpm==null||!dpm.isDeviceOwnerApp(c.getPackageName())) {
                b.append("NIE WYKONANO: brak Device Owner");
            } else {
                try {
                    ApplicationInfo a=pm.getApplicationInfo(OMC,
                        PackageManager.MATCH_UNINSTALLED_PACKAGES);
                    boolean system=(a.flags&(ApplicationInfo.FLAG_SYSTEM |
                        ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0;
                    boolean installed=(a.flags&ApplicationInfo.FLAG_INSTALLED)!=0;
                    if(!installed||!system) {
                        b.append("NIE WYKONANO: OMC nie jest zainstalowaną aplikacją systemową");
                    } else if(a.enabled) {
                        b.append("POMINIĘTO: OMC już włączony");
                    } else {
                        try {
                            dpm.enableSystemApp(new ComponentName(c,AdminReceiver.class),OMC);
                            b.append("DPM.enableSystemApp: wywołanie OK\n");
                        } catch(RuntimeException ex) {
                            b.append("DPM.enableSystemApp: ")
                                .append(ex.getClass().getSimpleName()).append("\n");
                        }
                        b.append("PO DPM: ").append(state(pm)).append("\n");
                        try {
                            ApplicationInfo after=pm.getApplicationInfo(OMC,
                                PackageManager.MATCH_UNINSTALLED_PACKAGES);
                            if(!after.enabled) {
                                // Android may reject this for a third-party DPC.
                                // Never bypass OS permission checks.
                                try {
                                    pm.setApplicationEnabledSetting(OMC,
                                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,0);
                                    b.append("PackageManager.setApplicationEnabledSetting: OK\n");
                                } catch(RuntimeException ex) {
                                    b.append("PackageManager.setApplicationEnabledSetting: ")
                                        .append(ex.getClass().getSimpleName()).append("\n");
                                }
                            }
                        } catch(PackageManager.NameNotFoundException ex) {
                            b.append("Nie ma pakietu po pierwszej próbie\n");
                        }
                    }
                } catch(PackageManager.NameNotFoundException ex) {
                    b.append("NIE WYKONANO: brak systemowego pakietu OMC");
                }
            }
        }
        b.append("\nPO WSZYSTKICH PRÓBACH: ").append(state(pm)).append("\n");
        b.append("UWAGA: zmiana stanu OMC nie musi inicjować pobierania.")
            .append(" Nie uruchamiamy nieudokumentowanych usług Samsung.");
        p.edit().putLong("at",System.currentTimeMillis())
            .putInt("count",p.getInt("count",0)+1)
            .putString("last",b.toString()).commit();
        PostDemoAppRestore.audit(c);
        State.log(c,"Samsung OMC test: "+b.toString().replace('\n',' '));
    }
    static String report(Context c) {
        SharedPreferences p=prefs(c);
        StringBuilder b=new StringBuilder("TEST WŁĄCZENIA OMC (TYLKO NA ŻĄDANIE)\n");
        b.append("Stan: ").append(state(c.getPackageManager())).append("\n");
        long at=p.getLong("at",0L);
        if(at==0)return b.append("Jeszcze nie uruchomiono testu.\n").toString();
        b.append("Numer próby: ").append(p.getInt("count",0)).append("\n");
        b.append("Czas: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()).format(new Date(at))).append("\n");
        b.append(p.getString("last","brak danych")).append("\n");
        return b.toString();
    }
}
