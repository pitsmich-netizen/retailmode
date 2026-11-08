package com.example.retaildpc;

import android.content.Context;
import android.content.SharedPreferences;

final class State {
    private State() {}
    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("retail_dpc_test", Context.MODE_PRIVATE);
    }
    static void setSite(Context c, String id) {
        prefs(c).edit().putString(Constants.SITE_KEY, id).apply();
    }
    static String site(Context c) { return prefs(c).getString(Constants.SITE_KEY, "BRAK"); }
    static void setPackage(Context c, String p) { prefs(c).edit().putString("demo_package", p).apply(); }
    static String pkg(Context c) { return prefs(c).getString("demo_package", ""); }
    static void installed(Context c, boolean done) { prefs(c).edit().putBoolean("installed", done).apply(); }
    static boolean installed(Context c) { return prefs(c).getBoolean("installed", false); }
    static void log(Context c, String text) {
        SharedPreferences p = prefs(c);
        String old = p.getString("log", "");
        String updated = old + "\n" + java.time.LocalDateTime.now() + " | " + text;
        if (updated.length() > 15000) updated = updated.substring(updated.length() - 15000);
        p.edit().putString("log", updated).apply();
    }
    static String logs(Context c) { return prefs(c).getString("log", "Brak zdarzeń"); }
}
