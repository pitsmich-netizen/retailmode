package com.example.retaildpc;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;

/**
 * Read-only diagnostic of Samsung's first-boot recommendation ecosystem.
 * No hidden intents, downloads, force-starts, package mutations or account sign-ins.
 */
final class SamsungProvisioningProbe {
    private SamsungProvisioningProbe() {}

    private static final String[][] PACKAGES = {
        {"Samsung Recommended apps (OMC)", "com.samsung.android.app.omcagent"},
        {"Galaxy Store", "com.sec.android.app.samsungapps"},
        {"Samsung Setup Wizard", "com.sec.android.app.SecSetupWizard"},
        {"Google Setup Wizard", "com.google.android.setupwizard"}
    };

    static String report(Context c) {
        PackageManager pm=c.getPackageManager();
        StringBuilder b=new StringBuilder("SAMSUNG: PIERWSZA KONFIGURACJA / OMC — 0.11.17\n");
        b.append("Odczyt diagnostyczny: bez uruchamiania usług OEM i bez APK\n");
        try {
            b.append("device_provisioned=")
                .append(Settings.Global.getInt(c.getContentResolver(),"device_provisioned",-1))
                .append("; user_setup_complete=")
                .append(Settings.Secure.getInt(c.getContentResolver(),"user_setup_complete",-1))
                .append("\n");
        } catch(RuntimeException e) {
            b.append("Status Setup Wizard niedostępny: ")
                .append(e.getClass().getSimpleName()).append("\n");
        }
        b.append("Device Owner: ").append(SingleAppOwnerRelease.isOwner(c))
            .append("\n\n");

        for(String[] item: PACKAGES) {
            String pkg=item[1];
            b.append(item[0]).append("\n");
            b.append("  pakiet: ").append(pkg).append("\n");
            try {
                PackageInfo info=pm.getPackageInfo(pkg,
                    PackageManager.MATCH_UNINSTALLED_PACKAGES |
                    PackageManager.GET_ACTIVITIES |
                    PackageManager.GET_SERVICES |
                    PackageManager.GET_RECEIVERS |
                    PackageManager.MATCH_DISABLED_COMPONENTS);
                ApplicationInfo app=info.applicationInfo;
                b.append("  WIDOCZNY: TAK; wersja=")
                    .append(info.versionName==null?"?":info.versionName)
                    .append("; versionCode=").append(info.getLongVersionCode()).append("\n");
                if(app!=null) {
                    b.append("  zainstalowany dla user: ")
                        .append((app.flags&ApplicationInfo.FLAG_INSTALLED)!=0)
                        .append("; włączony: ").append(app.enabled)
                        .append("; systemowy: ")
                        .append((app.flags&(ApplicationInfo.FLAG_SYSTEM |
                            ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0)
                        .append("\n");
                    b.append("  enabledSetting (0=DEFAULT,1=ENABLED,2=DISABLED,")
                        .append("3=DISABLED_USER,4=UNTIL_USED): ");
                    try {
                        b.append(pm.getApplicationEnabledSetting(pkg)).append("\n");
                    } catch(RuntimeException e) {
                        b.append("NIEDOSTĘPNY: ")
                            .append(e.getClass().getSimpleName()).append("\n");
                    }
                }
                int exported=0,enabled=0;
                if(info.activities!=null) {
                    for(ActivityInfo activity:info.activities) {
                        if(activity.exported)exported++;
                        if(activity.exported && activity.enabled)enabled++;
                    }
                }
                b.append("  activities=")
                    .append(info.activities==null?0:info.activities.length)
                    .append("; exported=").append(exported)
                    .append("; exportedEnabled=").append(enabled)
                    .append("; services=")
                    .append(info.services==null?0:info.services.length)
                    .append("; receivers=")
                    .append(info.receivers==null?0:info.receivers.length)
                    .append("\n");
                b.append("  launcher publiczny: ")
                    .append(pm.getLaunchIntentForPackage(pkg)!=null).append("\n");
            } catch(PackageManager.NameNotFoundException e) {
                b.append("  BRAK/WIDOCZNOŚĆ OGRANICZONA\n");
            } catch(RuntimeException e) {
                b.append("  BŁĄD ODCZYTU: ")
                    .append(e.getClass().getSimpleName()).append("\n");
            }
            b.append("\n");
        }
        b.append("UWAGA: komponent OMC to kandydat z zewnętrznych baz pakietów. ")
            .append("Nie potwierdzono dostępnego publicznego API inicjowania kolejki ")
            .append("Recommended Apps na urządzeniach zarządzanych.");
        return b.toString();
    }
}
