package com.example.retaildpc.demo;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;

/**
 * Use only the supported public Accessibility Settings action.
 *
 * Previous Samsung-specific and internal exported Settings activities could
 * appear briefly and immediately finish in One UI. Never use those shortcuts,
 * and never attempt to enable the service without explicit user consent.
 */
final class AccessibilityNavigator {
    private AccessibilityNavigator() { }

    static void open(Activity activity) {
        if (PendingStart.accessibilityEnabled(activity)) {
            AutomationState.log(activity, "Dostępność Retail Setup już włączona");
            return;
        }
        try {
            Intent settings = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            activity.startActivity(settings);
            AutomationState.log(activity,
                "Nawigacja: otwarto publiczne ustawienia Dostępności Androida");
        } catch (RuntimeException error) {
            AutomationState.log(activity,
                "Nie udało się otworzyć Dostępności: "
                + error.getClass().getSimpleName());
            // A visible Retry button remains in Retail Setup.
        }
    }
}
