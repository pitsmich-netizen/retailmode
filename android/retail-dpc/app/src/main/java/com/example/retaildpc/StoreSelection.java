package com.example.retaildpc;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.PersistableBundle;

/** Stores only non-secret configuration. Do not persist retail passwords from QR. */
final class StoreSelection {
    private StoreSelection() {}
    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("retail_store_selection_v1", Context.MODE_PRIVATE);
    }
    static void accept(Context c, PersistableBundle bundle) {
        if (bundle == null) return;
        String site = bundle.getString("site_id", "");
        String partner = bundle.getString("partner", "");
        String country = bundle.getString("country", "");
        if (site != null && site.matches("[A-Za-z0-9_-]{4,40}")) {
            prefs(c).edit().putString("site",site).putString("partner",safe(partner))
                .putString("country",safe(country)).apply();
            State.setSite(c, site);
            State.log(c,"Dane sklepu z QR zapisane: Site ID=" + site + ", partner=" + safe(partner));
        }
    }
    private static String safe(String v) { return v == null ? "" : v.substring(0, Math.min(v.length(), 80)); }
    static String partner(Context c) {
        String value=prefs(c).getString("partner", "");
        return value.isEmpty() ? AutomationState.partner(c) : value;
    }
    static String country(Context c) {
        String value=prefs(c).getString("country", "");
        return value.isEmpty() ? AutomationState.country(c) : value;
    }
    static String site(Context c) {
        String value=prefs(c).getString("site", "");
        return value.isEmpty() ? State.site(c) : value;
    }
}
