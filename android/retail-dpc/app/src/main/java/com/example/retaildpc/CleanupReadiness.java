package com.example.retaildpc;

import android.app.admin.DeviceAdminInfo;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

/**
 * TYLKO diagnostyka. Brak transferOwnership, clearDeviceOwnerApp,
 * wipeData, uninstallPackage czy zmian uprawnien.
 */
public final class CleanupReadiness {
    private static final String RETAIL_PACKAGE = "com.samsung.rms.retailagent.global";

    private CleanupReadiness() { }

    public static String check(Context context) {
        StringBuilder out = new StringBuilder();
        out.append("RETAIL DPC ").append(BuildConfig.VERSION_NAME).append(" — BEZPIECZNA DIAGNOSTYKA CLEANUP\n");
        out.append("Android: ").append(Build.VERSION.RELEASE)
           .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        out.append("Model: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        out.append("Wrapper: ").append(context.getPackageName()).append("\n");
        out.append("Retail Mode: ").append(RETAIL_PACKAGE).append("\n\n");

        DevicePolicyManager policy = context.getSystemService(DevicePolicyManager.class);
        if (policy == null) return out.append("ERROR: brak DevicePolicyManager\n").toString();
        boolean owner = policy.isDeviceOwnerApp(context.getPackageName());
        boolean retailOwner = policy.isDeviceOwnerApp(RETAIL_PACKAGE);
        out.append("Wrapper jest Device Owner: ").append(owner ? "TAK" : "NIE").append("\n");
        out.append("Retail Mode jest Device Owner: ").append(retailOwner ? "TAK" : "NIE").append("\n");
        // Publiczne Android SDK nie udostepnia getDeviceOwnerComponentOnCallingUser().
        // Pokazujemy komponent zadeklarowany przez NASZ Wrapper, a rolę DO
        // weryfikujemy niezaleznie przez publiczne isDeviceOwnerApp().
        ComponentName wrapperAdmin = new ComponentName(context, AdminReceiver.class);
        out.append("Komponent administratora Wrappera (lokalny): ")
           .append(wrapperAdmin.flattenToShortString()).append("\n");
        try {
            out.append("Komponent Wrappera aktywny: ")
               .append(policy.isAdminActive(wrapperAdmin) ? "TAK" : "NIE")
               .append("\n");
        } catch (SecurityException exception) {
            out.append("Komponent Wrappera aktywny: BRAK DOSTEPU DO ODCZYTU\n");
        }
        PackageManager pm = context.getPackageManager();
        PackageInfo info;
        try {
            // Standardowe GET_RECEIVERS; widocznosc pakietu wpisana w manifest <queries>.
            info = pm.getPackageInfo(RETAIL_PACKAGE, PackageManager.GET_RECEIVERS | PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            out.append("Retail APK: NIE ZNALEZIONO / NIE WIDAC W PACKAGE MANAGER\n");
            out.append("Wniosek: brak mozliwosci weryfikacji transferu.\n");
            return out.toString();
        }
        out.append("Retail APK: ZAINSTALOWANE\n");
        out.append("Wersja Retail APK: ").append(info.versionName == null ? "?" : info.versionName).append("\n");
        ActivityInfo[] receivers = info.receivers;
        if (receivers == null || receivers.length == 0) {
            out.append("Receivery: BRAK\n");
            out.append("Wniosek: brak deklarowanego DeviceAdminReceiver w Retail Mode.\n");
            return out.toString();
        }
        out.append("Odbiorniki aplikacji: ").append(receivers.length).append("\n");
        int admins = 0;
        int transferReady = 0;
        int transferReadyActive = 0;
        for (ActivityInfo ai : receivers) {
            if (ai.metaData == null || !ai.metaData.containsKey("android.app.device_admin")) continue;
            admins++;
            ComponentName component = new ComponentName(RETAIL_PACKAGE, ai.name);
            out.append("\nKandydat administratora: ").append(component.flattenToShortString()).append("\n");
            out.append("BIND_DEVICE_ADMIN: ").append("android.permission.BIND_DEVICE_ADMIN".equals(ai.permission) ? "TAK" : "NIE").append("\n");
            boolean active = false;
            try {
                active = policy.isAdminActive(component);
                out.append("Administrator aktywny: ").append(active ? "TAK" : "NIE").append("\n");
            } catch (SecurityException ex) {
                out.append("Administrator aktywny: BRAK DOSTEPU DO ODCZYTU\n");
            }
            try {
                ResolveInfo ri = new ResolveInfo();
                ri.activityInfo = ai;
                DeviceAdminInfo da = new DeviceAdminInfo(context, ri);
                boolean supports = da.supportsTransferOwnership();
                out.append("support-transfer-ownership: ").append(supports ? "TAK" : "NIE").append("\n");
                if (supports && "android.permission.BIND_DEVICE_ADMIN".equals(ai.permission)) {
                    transferReady++;
                    if (active) transferReadyActive++;
                }
            } catch (Exception ex) {
                out.append("Metadane administratora: BLAD ODCZYTU ")
                   .append(ex.getClass().getSimpleName()).append("\n");
            }
        }
        out.append("\nLiczba receiverow DeviceAdmin: ").append(admins).append("\n");
        if (!owner) {
            out.append("WYNIK: Wrapper nie jest DO. Nie uruchamiaj procedury cleanup.\n");
        } else if (transferReadyActive > 0) {
            out.append("WYNIK: WSTEPNIE mozliwy transfer. NIE wykonano go. ")
               .append("Potrzebne jeszcze potwierdzenie zgodnosci polityk na urzadzeniu.\n");
        } else if (transferReady > 0) {
            out.append("WYNIK: Retail deklaruje transfer, ale docelowy administrator nie jest aktywny. ")
               .append("Standardowy transfer nie jest jeszcze gotowy.\n");
        } else if (admins == 0) {
            out.append("WYNIK: nie znaleziono docelowego DeviceAdminReceiver; brak podstaw do transferu.\n");
        } else {
            out.append("WYNIK: nie potwierdzono obslugiwanej, aktywnej sciezki transferu.\n");
        }
        out.append("\nWAZNE: test niczego nie usuwa i nie przenosi. Nie uruchamia clearDeviceOwnerApp().\n");
        return out.toString();
    }
}
