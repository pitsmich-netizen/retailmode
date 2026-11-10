package com.example.retaildpc;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.PersistableBundle;
import android.provider.Settings;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A/B baseline to isolate managed-device provisioning from Retail Setup policy.
 * Enabled only by the QR extra minimal_owner_probe=true, before DO creation.
 * No OEM package is enabled/disabled; only our own launcher alias is made visible.
 */
final class MinimalOwnerProbe {
    private static final String PREF="retail_minimal_owner_probe_v1";
    private static final String KEY="minimal_owner_probe";
    private static final String OMC="com.samsung.android.app.omcagent";
    private MinimalOwnerProbe() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
    }
    static void accept(Context c, PersistableBundle extras) {
        if(extras==null || !extras.getBoolean(KEY,false))return;
        p(c).edit().putBoolean("enabled",true).commit();
    }
    static boolean enabled(Context c) {
        return p(c).getBoolean("enabled",false);
    }
    static synchronized void capture(Context c,String phase) {
        if(!enabled(c))return;
        PackageManager pm=c.getPackageManager();
        StringBuilder line=new StringBuilder();
        line.append(new SimpleDateFormat("HH:mm:ss.SSS",Locale.getDefault())
            .format(new Date())).append(" ").append(phase);
        try {
            DevicePolicyManager dpm=c.getSystemService(DevicePolicyManager.class);
            line.append(" | owner=").append(dpm!=null &&
                dpm.isDeviceOwnerApp(c.getPackageName()));
            line.append(" | device_provisioned=").append(
                Settings.Global.getInt(c.getContentResolver(),"device_provisioned",-1));
            line.append(" | user_setup_complete=").append(
                Settings.Secure.getInt(c.getContentResolver(),"user_setup_complete",-1));
        }catch(RuntimeException e) {
            line.append(" | setup_error=").append(e.getClass().getSimpleName());
        }
        try {
            ApplicationInfo info=pm.getApplicationInfo(OMC,
                PackageManager.MATCH_UNINSTALLED_PACKAGES);
            line.append(" | omc.installed=")
                .append((info.flags & ApplicationInfo.FLAG_INSTALLED)!=0)
                .append(" | omc.enabled=").append(info.enabled)
                .append(" | omc.enabledSetting=")
                .append(pm.getApplicationEnabledSetting(OMC));
        }catch(PackageManager.NameNotFoundException e) {
            line.append(" | omc.not_visible");
        }catch(RuntimeException e) {
            line.append(" | omc.error=").append(e.getClass().getSimpleName());
        }
        String updated=p(c).getString("timeline","")+line+"\n";
        if(updated.length()>12000)updated=updated.substring(updated.length()-12000);
        p(c).edit().putString("timeline",updated).commit();
        State.log(c,"MinimalOwnerProbe "+line);
    }
    static void exposeOwnLauncher(Context c) {
        if(!enabled(c))return;
        try {
            c.getPackageManager().setComponentEnabledSetting(
                new ComponentName(c,c.getPackageName()+".LauncherActivity"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
            p(c).edit().putString("launcher","Włączono ikonę Retail Setup (własna aplikacja)")
                .apply();
        }catch(RuntimeException e) {
            p(c).edit().putString("launcher","Ikona Retail Setup: "+
                e.getClass().getSimpleName()).apply();
        }
    }
    static String report(Context c) {
        if(!enabled(c))return "";
        return "TEST PORÓWNAWCZY 0.11.16 — MINIMALNY DEVICE OWNER\n"
            +"Bez Retail Mode, Experience, OMC activation, polityki HOME, lokalizacji, "
            +"automatycznego recovery i powiadomień.\n"
            +"QR: LEAVE_ALL_SYSTEM_APPS_ENABLED=true; FULLY_MANAGED_DEVICE.\n"
            +"Launcher: "+p(c).getString("launcher","nie sprawdzono")+"\n\n"
            +"OŚ ZDARZEŃ (OMC na różnych etapach)\n"
            +p(c).getString("timeline","Brak danych")+"\n"
            +"OMC: enabledSetting 0=DEFAULT, 1=ENABLED, 2=DISABLED, "
            +"3=DISABLED_USER, 4=UNTIL_USED.\n"
            +"Jeśli OMC jest DISABLED już na początku, nasze późniejsze "
            +"polityki nie są przyczyną jego wyłączenia.\n";
    }
}
