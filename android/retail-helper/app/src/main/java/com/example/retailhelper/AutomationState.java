package com.example.retailhelper;

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
    static final int WAIT_MANUAL_PASSWORD = 12;
    static final int PROFILE_CONFIRM = 13;
    static final int WAIT_PROFILE_CONFIRM_RESULT = 14;
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
    static String profileModel(Context c) { return p(c).getString("profileModel", ""); }
    static synchronized void setProfileModel(Context c, String model) {
        String canonical = ProfileMatcher.canonicalModel(model);
        p(c).edit().putString("profileModel", canonical).apply();
    }
    static synchronized void startProfileOnly(Context c) {
        p(c).edit().putBoolean("active", true).putInt("phase", PROFILE)
            .remove("selectedProfile")
            .putLong("phaseTime", System.currentTimeMillis())
            .putString("info", "Oczekuję na Select Profile; wybiorę wyłącznie zgodny model Experience")
            .apply();
        log(c, "WZNÓW: wybór profilu dla " + profileModel(c));
    }
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
            .putBoolean("fullMode", fullMode)
            .putBoolean("active", true).putInt("phase", ENTRY)
            .remove("selectedProfile")
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
        move(c, WAIT_MANUAL_PASSWORD,
            "Kraj/partner/sklep wybrane. Wprowadź hasło i zatwierdź je ręcznie. " +
            "Następnie Helper wybierze profil Experience dla wskazanego modelu.");
        log(c, "TRZY WYBORY GOTOWE; HASŁO I JEGO ZATWIERDZENIE RĘCZNE. " +
            "OCZEKUJĘ NA OKNO SELECT PROFILE.");
    }
    static synchronized void profileSelected(Context c, String profile) {
        // Do not stop here: Samsung shows a separate CONFIRM after choosing a profile.
        p(c).edit().putString("selectedProfile", profile).apply();
        move(c, PROFILE_CONFIRM,
            "Wybrano profil '" + profile + "'. Oczekuję na osobny przycisk CONFIRM.");
    }
    static String selectedProfile(Context c) {
        return p(c).getString("selectedProfile", "");
    }
    static synchronized void profileConfirmSent(Context c) {
        move(c, WAIT_PROFILE_CONFIRM_RESULT,
            "Wysłano CONFIRM po wyborze profilu (jeden raz). Sprawdź aktywację Retail Mode.");
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
