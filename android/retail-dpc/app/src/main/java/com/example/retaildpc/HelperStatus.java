package com.example.retaildpc;

import android.content.Context;
import android.content.pm.PackageManager;

/** Retail Helper installation status. */
final class HelperStatus {
    private HelperStatus() {}
    static boolean configured(Context c) { return HelperConfig.configured(c); }
    static boolean installed(Context c) {
        try { c.getPackageManager().getPackageInfo(Constants.HELPER_PACKAGE, 0); return true; }
        catch (PackageManager.NameNotFoundException e) { return false; }
    }
}
