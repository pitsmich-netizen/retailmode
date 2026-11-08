package com.example.retaildpc;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * TYLKO LABORATORYJNY TEST. clearDeviceOwnerApp() jest przestarzałe i
 * przeznaczone wyłącznie do testowania. Nigdy nie wykonuj automatycznie.
 */
public final class CleanupFlow {
    private static final String RETAIL = "com.samsung.rms.retailagent.global";
    private static final String ADMIN_CLASS = "com.samsung.sta.mi.rm.receivers.EnterpriseDeviceAdminReceiver";
    static final String CONFIRM_TEXT = "ZWOLNIJ OWNER";
    private static final String[] COMMON_APPS = {
        "com.sec.android.gallery3d",       // Galeria Samsung (zależnie od modelu)
        "com.sec.android.app.sbrowser",    // Samsung Internet
        "com.sec.android.app.myfiles",     // Moje pliki
        "com.android.chrome",              // Chrome (jeśli występuje w ROM)
        "com.android.vending",             // Sklep Play (jeśli występuje w ROM)
        "com.google.android.apps.photos"   // Zdjęcia (jeśli systemowe)
    };
    private static final String PREFS = "retail_dpc_cleanup_082";

    private CleanupFlow() {}
    static boolean blocked(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean("release_prepared", false);
    }
    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
    private static DevicePolicyManager policy(Context context) {
        return context.getSystemService(DevicePolicyManager.class);
    }
    private static ComponentName wrapperAdmin(Context context) {
        return new ComponentName(context, AdminReceiver.class);
    }
    static boolean isOwner(Context context) {
        DevicePolicyManager p = policy(context);
        return p != null && p.isDeviceOwnerApp(context.getPackageName());
    }
    static boolean retailInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(RETAIL, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    static String snapshot(Context context) {
        DevicePolicyManager p = policy(context);
        StringBuilder sb = new StringBuilder();
        sb.append("RETAIL DPC ").append(BuildConfig.VERSION_NAME).append(" — STAN LABORATORYJNY\n")
            .append("System: ").append(Build.VERSION.RELEASE).append(" / API ").append(Build.VERSION.SDK_INT)
            .append(" / ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n")
            .append("Wrapper pakiet: ").append(context.getPackageName()).append("\n")
            .append("Wrapper Device Owner: ").append(isOwner(context) ? "TAK" : "NIE").append("\n")
            .append("Automatyzacja aktywna: ").append(AutomationState.active(context) ? "TAK" : "NIE").append("\n")
            .append("Instalator zablokowany: ").append(blocked(context) ? "TAK" : "NIE").append("\n")
            .append("Retail zainstalowany: ").append(retailInstalled(context) ? "TAK" : "NIE").append("\n")
            .append("Helper: ").append(HelperStatus.installed(context) ? "ZAINSTALOWANY" : "BRAK").append("\n");
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(RETAIL, 0);
            sb.append("Retail wersja: ").append(info.versionName).append("\n");
        } catch (Exception ignored) {}
        if (p != null) {
            sb.append("Admin Wrapper aktywny: ").append(p.isAdminActive(wrapperAdmin(context)) ? "TAK" : "NIE").append("\n");
            sb.append("Retail Device Owner: ").append(p.isDeviceOwnerApp(RETAIL) ? "TAK" : "NIE").append("\n");
            ComponentName retailAdmin = new ComponentName(RETAIL, ADMIN_CLASS);
            try {sb.append("Retail Device Administrator aktywny: ")
                .append(p.isAdminActive(retailAdmin) ? "TAK" : "NIE").append("\n");}
            catch (RuntimeException e) {sb.append("Retail Device Administrator: brak dostępu\n");}
        }
        sb.append("\nAPLIKACJE SYSTEMOWE (widocznosc zalezy od OEM):\n");
        for (String name : COMMON_APPS) sb.append(name).append(": ").append(appStatus(context, name)).append("\n");
        sb.append("\nData ostatniej próby zwolnienia: ").append(prefs(context).getString("lastAttempt", "brak")).append("\n")
          .append("Wynik próby: ").append(prefs(context).getString("lastResult", "brak")).append("\n")
          .append("\nUwaga: NIE potwierdza, czy Retail Mode ukończył rejestrację sklepu lub zachowa ustawienia po restarcie.\n");
        return sb.toString();
    }
    private static String appStatus(Context context, String pkg) {
        try {
            ApplicationInfo a = context.getPackageManager().getApplicationInfo(pkg,
                    PackageManager.MATCH_UNINSTALLED_PACKAGES | PackageManager.MATCH_DISABLED_COMPONENTS);
            boolean system = (a.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
            return (system ? "systemowa" : "niesystemowa") + ", " + (a.enabled ? "wlaczona" : "wylaczona") +
                 ", zainstalowana dla uzytkownika: " + ((a.flags & ApplicationInfo.FLAG_INSTALLED) != 0 ? "TAK" : "NIE");
        } catch (PackageManager.NameNotFoundException e) {
            return "nieobecna / niewidoczna dla PackageManager";
        }
    }
    /** Wyłącznie fabryczne aplikacje uruchamiane z launchera; nie pobiera plików z Internetu. */
    static String restoreLauncherSystemApps(Context context) {
        if (!isOwner(context)) return "ODMOWA: Wrapper nie jest Device Owner.";
        if (blocked(context)) return "ODMOWA: rozpoczęto zwalnianie Device Owner.";
        DevicePolicyManager p = policy(context);
        if (p == null) return "BŁĄD: DevicePolicyManager niedostępny";
        StringBuilder s = new StringBuilder("Przywracanie aplikacji systemowych:\n");
        Intent launcher = new Intent(Intent.ACTION_MAIN);
        launcher.addCategory(Intent.CATEGORY_LAUNCHER);
        try {
            int count = p.enableSystemApp(wrapperAdmin(context), launcher);
            s.append("Przywrócone aktywności z launcherem: ").append(count).append("\n");
        } catch (RuntimeException e) {
            s.append("Przywracanie launcherem nieudane: ").append(e.getClass().getSimpleName()).append("\n");
        }
        // Dodatkowe typowe pakiety OEM, jeżeli są fabrycznymi aplikacjami w tym ROM.
        for (String app : COMMON_APPS) {
            try {
                ApplicationInfo info = context.getPackageManager().getApplicationInfo(app,
                        PackageManager.MATCH_UNINSTALLED_PACKAGES | PackageManager.MATCH_DISABLED_COMPONENTS);
                boolean system = (info.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
                if (!system) {s.append(app).append(": pominięto, nie jest systemowa\n"); continue;}
                p.enableSystemApp(wrapperAdmin(context), app);
                s.append(app).append(": OK / spróbuj sprawdzić ikonę\n");
            } catch (Exception e) {
                s.append(app).append(": brak lub błąd (").append(e.getClass().getSimpleName()).append(")\n");
            }
        }
        s.append("\nTo NIE instaluje aplikacji nieobecnych w obrazie fabrycznym. Sprawdź ręcznie ikonę i działanie każdej z nich.");
        return s.toString();
    }
    static String releaseTest(Context context) {
        if (!isOwner(context)) return "ODMOWA: Wrapper nie ma statusu Device Owner.";
        if (!retailInstalled(context)) return "ODMOWA: Retail Mode nie jest zainstalowany.";
        if (HelperStatus.configured(context) && !HelperStatus.installed(context))
            return "ODMOWA: skonfigurowany Helper nie jest jeszcze zainstalowany.";
        if (AutomationState.active(context)) return "ODMOWA: zatrzymaj automatyzację przed zwolnieniem Device Owner.";
        DevicePolicyManager p = policy(context);
        if (p == null) return "BŁĄD: brak DevicePolicyManager.";
        // Blokada musi być zapisana PRZED zwolnieniem uprawnień.
        prefs(context).edit().putBoolean("release_prepared", true)
                .putString("lastAttempt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new Date()))
                .putString("lastResult", "Próba rozpoczęta").commit();
        JobScheduler js = context.getSystemService(JobScheduler.class);
        if (js != null) {
            js.cancel(Constants.INSTALL_JOB_ID);
            js.cancel(Constants.HELPER_INSTALL_JOB_ID);
        }
        AutomationState.stop(context, "Przygotowanie zwolnienia Device Owner (LAB)");
        SecretMemory.clear();
        try {
            // DEPRECATED / TEST ONLY; NIE wywoływać automatycznie.
            p.clearDeviceOwnerApp(context.getPackageName());
            boolean remaining = p.isDeviceOwnerApp(context.getPackageName());
            String outcome = remaining
                    ? "WYWOLANO API, ale Wrapper WCIĄŻ jest Device Owner. Niczego nie odinstalowuj."
                    : "Wrapper nie jest już Device Owner. Nie oznacza to, że polityki systemowe zostały wyczyszczone.";
            prefs(context).edit().putString("lastResult", outcome).apply();
            return outcome + "\nZrestartuj telefon, sprawdź Retail Mode, Wi-Fi i aplikacje. Dopiero potem rozważ odinstalowanie Wrappera.";
        } catch (RuntimeException e) {
            String outcome = "BŁĄD " + e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage());
            prefs(context).edit().putString("lastResult", outcome).apply();
            return outcome + "\nDevice Owner nadal: " + (isOwner(context) ? "TAK" : "NIE") +
                ". Blokada instalatora pozostaje włączona, żeby nie uruchamiać go po niepewnej próbie.";
        }
    }
    static String deactivateAdminAfterRelease(Context context) {
        if (isOwner(context)) return "ODMOWA: nadal jest Device Owner. Nie można wyłączyć administratora.";
        DevicePolicyManager p = policy(context);
        if (p == null) return "Brak DevicePolicyManager";
        ComponentName component = wrapperAdmin(context);
        if (!p.isAdminActive(component)) return "Administrator Wrappera już nieaktywny. Można spróbować zwykłej deinstalacji w Ustawieniach.";
        try {
            p.removeActiveAdmin(component);
            return "Wysłano usunięcie uprawnień Device Administrator Wrappera. Sprawdź ponownie status i odinstaluj zwyczajnie w Ustawieniach.";
        } catch (RuntimeException e) {
            return "Nie udało się zdezaktywować administratora: " + e.getClass().getSimpleName() + ". Nie odinstalowuj na siłę.";
        }
    }
}
