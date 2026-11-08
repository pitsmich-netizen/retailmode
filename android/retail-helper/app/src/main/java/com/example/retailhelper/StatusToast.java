package com.example.retailhelper;

import android.content.Context;
import android.os.SystemClock;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/** Small, rate-limited user-visible stage hints; no passwords or site details. */
final class StatusToast {
    private long lastAt;
    private int lastPhase = Integer.MIN_VALUE;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pending;
    private static final long MIN_INTERVAL_MS = 3200L;

    void phase(Context context, int phase) {
        if (phase == lastPhase) return;
        String message = messageFor(phase);
        if (message == null) return;
        lastPhase = phase;
        // The service reports a phase change only once. Delaying rather than
        // dropping it avoids losing the only status toast for a quick transition.
        if (pending != null) handler.removeCallbacks(pending);
        long now = SystemClock.elapsedRealtime();
        long delay = Math.max(0L, MIN_INTERVAL_MS - (now - lastAt));
        pending = () -> {
            lastAt = SystemClock.elapsedRealtime();
            Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        };
        handler.postDelayed(pending, delay);
    }
    static String messageFor(int phase) {
        switch (phase) {
            case AutomationState.ENTRY: return "Uruchamiam Retail Mode…";
            case AutomationState.COUNTRY_CHOOSE: return "Wybieram kraj…";
            case AutomationState.PARTNER_CHOOSE: return "Wybieram partnera…";
            case AutomationState.STORE_SEARCH: return "Wyszukuję sklep…";
            case AutomationState.PASSWORD: return "Uzupełniam dane rejestracji…";
            case AutomationState.WAIT_MANUAL_PASSWORD: return "Wpisz i zatwierdź hasło w Retail Mode";
            case AutomationState.WAIT_AFTER_CONFIRM: return "Czekam na wybór profilu…";
            case AutomationState.PROFILE: return "Dobieram profil urządzenia…";
            case AutomationState.PROFILE_CONFIRM: return "Zatwierdzam profil demo…";
            case AutomationState.WAIT_PROFILE_CONFIRM_RESULT: return "Czekam na rozpoczęcie pobierania…";
            default: return null;
        }
    }
    void cancel() {
        if (pending != null) handler.removeCallbacks(pending);
        pending = null;
    }
    static void completion(Context context) {
        Toast.makeText(context,
            "Pobieranie demo rozpoczęte. Wyłączam Dostępność i ukrywam ikonę Helpera.",
            Toast.LENGTH_LONG).show();
    }
}
