package com.example.retailhelper;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Accessibility activation is ALWAYS manual and must be approved in Android Settings.
 * Android only standardizes the generic Accessibility entry point. Several OEM
 * shortcuts are intentionally best-effort and are not a supported API contract.
 */
final class AccessibilityNavigator {
    private static final String SETTINGS_PACKAGE = "com.android.settings";
    private AccessibilityNavigator() { }

    static void open(Activity activity) {
        if (PendingStart.accessibilityEnabled(activity)) {
            Toast.makeText(activity, "Dostępność Retail Helper jest już włączona", Toast.LENGTH_SHORT).show();
            return;
        }
        ComponentName target = new ComponentName(activity, RetailAutomationService.class);
        Toast.makeText(activity,
            "Włącz: Zainstalowane aplikacje → Retail Helper 0.2.10 → Zezwól",
            Toast.LENGTH_LONG).show();

        // Samsung-specific installed accessibility services action. This is an
        // undocumented OEM shortcut, so it must never be the sole path.
        // The fragment arguments are advisory highlighting only, NOT consent.
        try {
            Intent samsung = new Intent("com.samsung.accessibility.installed_service");
            samsung.setPackage(SETTINGS_PACKAGE);
            String serviceKey = target.flattenToString();
            Bundle highlight = new Bundle();
            highlight.putString(":settings:fragment_args_key", serviceKey);
            samsung.putExtra(":settings:fragment_args_key", serviceKey);
            samsung.putExtra(":settings:show_fragment_args", highlight);
            activity.startActivity(samsung);
            AutomationState.log(activity, "Nawigacja Dostępność: lista zainstalowanych usług Samsung (OEM)");
            return;
        } catch (RuntimeException unavailable) {
            AutomationState.log(activity, "Skrót Samsung niedostępny: " + unavailable.getClass().getSimpleName());
        }

        // Legacy Samsung/Android Settings activity for installed services.
        // Only attempt it if present, exported and not permission-protected.
        // On modern One UI it is often absent; never assume this is universal.
        ComponentName legacy = new ComponentName(SETTINGS_PACKAGE,
            "com.android.settings.Settings$AccessibilityInstalledServiceActivity");
        if (launchExported(activity, legacy, target)) return;

        // Additional best-effort One UI / OEM search. The actual installed-services
        // screen is not exposed through a stable public Android intent. Only try
        // explicit activities that Settings itself declares exported and whose
        // permission is granted; never use hidden APIs, shell, or UI auto-clicks.
        if (openExportedInstalledServicesScreen(activity, target)) return;

        // OEM-specific implicit shortcut (unsupported, safely restricted to Settings).
        // If not present, Android will throw ActivityNotFoundException and we fall back.
        try {
            Intent installed = new Intent("android.settings.ACCESSIBILITY_INSTALLED_SERVICES_SETTINGS");
            installed.setPackage(SETTINGS_PACKAGE);
            installed.putExtra(Intent.EXTRA_COMPONENT_NAME, target.flattenToString());
            activity.startActivity(installed);
            AutomationState.log(activity, "Nawigacja Dostępność: skrót listy usług (OEM)");
            return;
        } catch (RuntimeException unavailable) {
            AutomationState.log(activity, "Skrót listy usług niedostępny: " + unavailable.getClass().getSimpleName());
        }

        // The direct-details action is permission protected on many Android builds.
        try {
            Intent details = new Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS");
            details.setPackage(SETTINGS_PACKAGE);
            details.putExtra(Intent.EXTRA_COMPONENT_NAME, target.flattenToString());
            activity.startActivity(details);
            AutomationState.log(activity, "Nawigacja Dostępność: szczegóły usługi");
            return;
        } catch (RuntimeException unavailable) {
            AutomationState.log(activity, "Szczegóły Dostępności niedostępne: " + unavailable.getClass().getSimpleName());
        }

        // Public API.  Do not automate taps/toggles on the OS permission screen.
        try {
            activity.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            AutomationState.log(activity, "Nawigacja Dostępność: ustawienia ogólne (fallback)");
        } catch (RuntimeException unavailable) {
            Toast.makeText(activity, "Otwórz Ustawienia → Dostępność → Zainstalowane aplikacje",
                Toast.LENGTH_LONG).show();
        }
    }

    private static boolean openExportedInstalledServicesScreen(Activity activity, ComponentName service) {
        try {
            android.content.pm.PackageInfo packageInfo = activity.getPackageManager().getPackageInfo(
                SETTINGS_PACKAGE, PackageManager.GET_ACTIVITIES);
            if (packageInfo.activities == null) return false;
            for (ActivityInfo info : packageInfo.activities) {
                if (info.name == null || !info.exported) continue;
                String n = info.name.toLowerCase(java.util.Locale.ROOT);
                boolean accessibility = n.contains("accessibility");
                boolean installed = n.contains("installed") || n.contains("service");
                boolean details = n.contains("detail") || n.contains("toggle") || n.contains("shortcut");
                if (!accessibility || !installed || details) continue;
                if (launchExported(activity, new ComponentName(SETTINGS_PACKAGE, info.name), service))
                    return true;
            }
        } catch (RuntimeException | PackageManager.NameNotFoundException ignored) {
            // Old and custom OEM settings must always fall back to public ACTION_ACCESSIBILITY_SETTINGS.
        }
        return false;
    }

    private static boolean launchExported(Activity activity, ComponentName destination, ComponentName service) {
        try {
            ActivityInfo ai = activity.getPackageManager().getActivityInfo(destination, 0);
            if (!ai.exported || (ai.permission != null &&
                    activity.checkSelfPermission(ai.permission) != PackageManager.PERMISSION_GRANTED))
                return false;
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setComponent(destination);
            intent.putExtra(Intent.EXTRA_COMPONENT_NAME, service.flattenToString());
            intent.putExtra("preference_key", service.flattenToString());
            activity.startActivity(intent);
            AutomationState.log(activity, "Nawigacja Dostępność: ekran usługi (OEM)");
            return true;
        } catch (RuntimeException | PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }
}
