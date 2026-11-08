package com.example.retaildpc;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Locale;

/** Logi bez haseł; stan testu jest lokalny i wymaga świadomego uruchomienia. */
final class AutomationState {
    static final int ENTRY = -1;
    static final int STOPPED = 0;
    static final int COUNTRY_OPEN = 1;
    static final int COUNTRY_CHOOSE = 2;
    static final int PARTNER_OPEN = 3;
    static final int PARTNER_CHOOSE = 4;
    static final int STORE_OPEN = 5;
    static final int STORE_SEARCH = 6;
    static final int DONE = 7;
    static final int PASSWORD = 8;
    static final int WAIT_AFTER_CONFIRM = 9;
    static final int PROFILE = 10;
    static final int EMBARGO_PAUSED = 11;
    static final int PROFILE_FINAL_CONFIRM = 12;
    private AutomationState() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("retail_auto_06", Context.MODE_PRIVATE);
    }
    static boolean active(Context c) { return p(c).getBoolean("active", false); }
    static int phase(Context c) { return p(c).getInt("phase", STOPPED); }
    static boolean fullMode(Context c) { return p(c).getBoolean("fullMode", false); }
    static String country(Context c) { return p(c).getString("country", "Poland (SEPOL)"); }
    static String partner(Context c) { return p(c).getString("partner", "AUCHAN"); }
    static String site(Context c) { return p(c).getString("site", "C008660707"); }
    static long phaseTime(Context c) { return p(c).getLong("phaseTime", System.currentTimeMillis()); }
    static String info(Context c) { return p(c).getString("info", "Test nieuruchomiony"); }
    static String logs(Context c) { return p(c).getString("logs", "Brak logów"); }

    static synchronized void start(Context c, String country, String partner, String site, boolean fullMode) {
        if (site == null || !site.trim().matches("[A-Za-z0-9_-]{4,40}"))
            throw new IllegalArgumentException("Site ID: od 4 do 40 znaków (litery, cyfry, _ lub -)");
        if (partner == null || partner.trim().isEmpty() || partner.length() > 80)
            throw new IllegalArgumentException("Wpisz partnera z listy Retail Mode");
        if (country == null || country.trim().isEmpty() || country.length() > 80)
            throw new IllegalArgumentException("Wpisz kraj z listy Retail Mode");
        p(c).edit().putString("country", country.trim()).putString("partner", partner.trim())
            .putString("site", site.trim().toUpperCase(Locale.ROOT))
            .putString("chosenProfile", "")
            .putBoolean("fullMode", fullMode)
            .putBoolean("active", true).putInt("phase", ENTRY)
            .putLong("phaseTime", System.currentTimeMillis())
            .putString("info", "Rozpoznaję okno PROCEED/CANCEL lub informacyjne CLOSE")
            .apply();
        log(c, "START: kraj=" + country + "; partner=" + partner + "; site=" + site +
            "; tryb=" + (fullMode ? "PELNY" : "TESTOWY") + "; haslo nie jest logowane");
    }
    static synchronized void move(Context c, int target, String detail) {
        if (!active(c)) return;
        p(c).edit().putInt("phase", target).putLong("phaseTime", System.currentTimeMillis())
            .putString("info", detail).apply();
        log(c, "KROK " + target + ": " + detail);
    }
    static synchronized void stop(Context c, String detail) {
        SecretMemory.clear();
        p(c).edit().putBoolean("active", false).putInt("phase", STOPPED)
            .putString("info", detail).apply();
        log(c, "STOP: " + detail);
    }
    static synchronized void pauseForAuthorization(Context c, String detail) {
        SecretMemory.clear();
        p(c).edit().putBoolean("active", false).putInt("phase", EMBARGO_PAUSED)
            .putString("info", detail).apply();
        log(c, "PAUZA: " + detail);
    }
    static synchronized void resumeAfterAuthorization(Context c) {
        if (phase(c) != EMBARGO_PAUSED) {
            log(c, "WZNÓW: brak oczekującej autoryzacji — nic nie zmieniono");
            return;
        }
        p(c).edit().putBoolean("active", true).putInt("phase", WAIT_AFTER_CONFIRM)
            .putLong("phaseTime", System.currentTimeMillis())
            .putString("info", "Wznowienie: oczekuję na wybór profilu").apply();
        log(c, "WZNOWIONO po autoryzacji ręcznej");
    }
    static synchronized void done(Context c) {
        if (fullMode(c)) {
            move(c, PASSWORD, "Sklep wybrany. Przygotowuję lokalne hasło Retail Mode.");
            return;
        }
        SecretMemory.clear();
        p(c).edit().putBoolean("active", false).putInt("phase", DONE)
            .putString("info", "Kraj/partner/sklep wybrane; tryb TESTOWY zatrzymał się PRZED HASŁEM i CONFIRM.").apply();
        log(c, "GOTOWE: trzy wybory. ZATRZYMANO PRZED HASŁEM I CONFIRM.");
    }
    static synchronized void profileChosen(Context c, String profile) {
        // Nie oznaczamy sukcesu po samym kliknieciu Experience. Jest jeszcze
        // osobne, ostatnie CONFIRM.
        p(c).edit().putString("chosenProfile", profile).apply();
        move(c, PROFILE_FINAL_CONFIRM, "Wybrano " + profile + "; oczekuje na koncowe CONFIRM");
    }
    static synchronized void finalConfirmDispatched(Context c) {
        String profile = p(c).getString("chosenProfile", "Experience");
        SecretMemory.clear();
        p(c).edit().putBoolean("active", false).putInt("phase", DONE)
            .putString("info", "Wyslano koncowy CONFIRM dla " + profile +
                ". Sprawdz recznie, czy Retail Mode ukonczyl aktywacje.").apply();
        log(c, "KONCOWY CONFIRM klikniety tylko raz. Wynik aktywacji jeszcze niepotwierdzony.");
    }
    static synchronized void log(Context c, String message) {
        String old = p(c).getString("logs", "");
        String line = "\n" + new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(new java.util.Date()) + " | " + message;
        String next = old + line;
        if (next.length() > 10000) next = next.substring(next.length() - 10000);
        p(c).edit().putString("logs", next).apply();
    }
}
