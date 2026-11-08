package com.example.retailhelper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.ComponentName;
import android.provider.Settings;
import java.util.Locale;

/** One-shot handoff: only armed by an explicit START confirmation in Helper UI. */
final class PendingStart {
    private static final String PREF = "helper_pending_start_022";
    private static final long EXPIRE_MS = 5 * 60 * 1000L;
    private PendingStart() { }
    static synchronized void arm(Context c, String site, boolean full) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString("site", site).putBoolean("full", full)
            .putLong("at", System.currentTimeMillis()).commit();
    }
    static synchronized void clear(Context c) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().commit();
    }
    static synchronized boolean armed(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        long at = p.getLong("at", 0);
        if (at <= 0) return false;
        if (System.currentTimeMillis() - at > EXPIRE_MS) {
            clear(c);
            SecretMemory.clear();
            return false;
        }
        return true;
    }
    static boolean accessibilityEnabled(Context c) {
        String enabled = Settings.Secure.getString(c.getContentResolver(),
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        ComponentName own = new ComponentName(c, RetailAutomationService.class);
        for (String part : enabled.split(":")) {
            ComponentName candidate = ComponentName.unflattenFromString(part);
            if (own.equals(candidate)) return true;
        }
        return false;
    }
    static synchronized boolean beginIfReady(Context c) {
        if (!armed(c) || !accessibilityEnabled(c)) return false;
        SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String site = p.getString("site", "");
        boolean full = p.getBoolean("full", false);
        if (full && !SecretMemory.available()) {
            clear(c);
            AutomationState.stop(c, "Hasło z pamięci tymczasowej wygasło. Uruchom START ponownie.");
            return false;
        }
        if (AutomationState.active(c)) {
            clear(c);
            return false;
        }
        try {
            AutomationState.start(c, "Poland (SEPOL)", "AUCHAN", site, full);
            clear(c);
            return true;
        } catch (IllegalArgumentException e) {
            clear(c);
            SecretMemory.clear();
            AutomationState.log(c, "Nie uruchomiono automatu: sprawdź Site ID");
            return false;
        }
    }
}
