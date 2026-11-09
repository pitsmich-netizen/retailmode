package com.example.retaildpc;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.provider.Settings;

/**
 * Temporary HOME while preparing Retail Mode. The user can exit to Android
 * launcher WITHOUT dropping Device Owner or cancelling the installation.
 * The user's exit decision prevents Home policy from being re-applied.
 */
final class RetailHomePolicy {
    private static final String PREF = "retail_home_exit_0112";
    private RetailHomePolicy() { }

    static boolean manualExitRequested(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean("manual_exit", false);
    }

    static boolean ensure(Context c) {
        DevicePolicyManager dpm = c.getSystemService(DevicePolicyManager.class);
        if (dpm == null || !dpm.isDeviceOwnerApp(c.getPackageName())
                || SingleAppOwnerRelease.blocked(c) || manualExitRequested(c)) return false;
        try {
            IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
            filter.addCategory(Intent.CATEGORY_HOME);
            filter.addCategory(Intent.CATEGORY_DEFAULT);
            dpm.addPersistentPreferredActivity(new ComponentName(c, AdminReceiver.class),
                filter, new ComponentName(c.getPackageName(), c.getPackageName() + ".HomeActivity"));
            State.log(c, "Retail Setup: tymczasowy HOME");
            return true;
        } catch (RuntimeException e) {
            State.log(c, "HOME niedostępny: " + e.getClass().getSimpleName());
            return false;
        }
    }

    static void clear(Context c) {
        DevicePolicyManager dpm = c.getSystemService(DevicePolicyManager.class);
        if (dpm != null && dpm.isDeviceOwnerApp(c.getPackageName())) {
            dpm.clearPackagePersistentPreferredActivities(
                new ComponentName(c, AdminReceiver.class), c.getPackageName());
            State.log(c, "Retail Setup: usunięto politykę preferowanego HOME");
        }
    }

    static void disableHomeAlias(Context c) {
        try {
            c.getPackageManager().setComponentEnabledSetting(
                new ComponentName(c.getPackageName(), c.getPackageName() + ".HomeActivity"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
            State.log(c, "Retail Setup: własny HOME wyłączony");
        } catch (RuntimeException ex) {
            State.log(c, "Nie wyłączono aliasu HOME: " + ex.getClass().getSimpleName());
        }
    }

    /** Safe fallback: does not revoke admin rights or interrupt an APK download. */
    static boolean exitToAndroidHome(Activity activity) {
        Context c = activity.getApplicationContext();
        SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        p.edit().putBoolean("manual_exit", true).commit();

        try {
            clear(c);
        } catch (RuntimeException e) {
            State.log(c, "Przywracanie preferencji HOME: " + e.getClass().getSimpleName());
        }
        // Disable the alternate HOME before requesting the real launcher.
        // Otherwise ACTION_MAIN/HOME can simply reopen Retail Setup.
        disableHomeAlias(c);

        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            activity.startActivity(home);
            State.log(c, "Użytkownik opuścił Retail Setup: uruchomiono pulpit Androida");
            activity.finish();
            return true;
        } catch (RuntimeException e) {
            State.log(c, "Uruchomienie HOME nieudane: " + e.getClass().getSimpleName());
            // Samsung launcher picker is still available for recovery.
            try {
                activity.startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
                activity.finish();
                return true;
            } catch (RuntimeException fallback) {
                State.log(c, "Wybór launchera nieudany: " + fallback.getClass().getSimpleName());
                return false;
            }
        }
    }
}
