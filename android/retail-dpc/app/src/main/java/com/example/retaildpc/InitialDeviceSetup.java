package com.example.retaildpc;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** One-time changes only. NO user restrictions or permanent device management. */
final class InitialDeviceSetup {
    private InitialDeviceSetup() {}
    private static final String RETAIL_PACKAGE = "com.samsung.rms.retailagent.global";

    static String enableLocation(Context context) {
        DevicePolicyManager manager = context.getSystemService(DevicePolicyManager.class);
        if (manager == null || !manager.isDeviceOwnerApp(context.getPackageName())) {
            return "Nie zmieniono lokalizacji: brak Device Owner";
        }
        try {
            manager.setLocationEnabled(new ComponentName(context, AdminReceiver.class), true);
            LocationManager location = context.getSystemService(LocationManager.class);
            boolean isEnabled = location != null && location.isLocationEnabled();
            String report = "Lokalizacja systemowa: " + (isEnabled ? "WLACZONA" : "NIEPOTWIERDZONA");
            State.log(context, "InitialSetup: " + report);
            return report;
        } catch (RuntimeException e) {
            String report = "Lokalizacja: niepowodzenie " + e.getClass().getSimpleName();
            State.log(context, "InitialSetup: " + report);
            return report;
        }
    }

    /** Only grants the Retail app location permissions actually requested in its manifest. */
    static String grantRetailLocationPermissions(Context context) {
        DevicePolicyManager manager = context.getSystemService(DevicePolicyManager.class);
        if (manager == null || !manager.isDeviceOwnerApp(context.getPackageName()))
            return "Uprawnienia: brak Device Owner";
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(RETAIL_PACKAGE, PackageManager.GET_PERMISSIONS);
            Set<String> requested = new HashSet<>();
            if (info.requestedPermissions != null) requested.addAll(Arrays.asList(info.requestedPermissions));
            ComponentName admin = new ComponentName(context, AdminReceiver.class);
            StringBuilder status = new StringBuilder();
            for (String permission : new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION}) {
                String shortName = permission.substring(permission.lastIndexOf('.') + 1);
                if (!requested.contains(permission)) {
                    status.append(shortName).append(": nie wymagane; ");
                    continue;
                }
                try {
                    boolean accepted = manager.setPermissionGrantState(admin, RETAIL_PACKAGE, permission,
                        DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
                    boolean granted = pm.checkPermission(permission, RETAIL_PACKAGE) == PackageManager.PERMISSION_GRANTED;
                    status.append(shortName).append(": ")
                        .append(accepted && granted ? "NADANE" : "NIEPOTWIERDZONE").append("; ");
                } catch (RuntimeException e) {
                    status.append(shortName).append(": blad ").append(e.getClass().getSimpleName()).append("; ");
                }
            }
            String report = status.toString();
            State.log(context, "InitialSetup Retail permissions: " + report);
            return report;
        } catch (PackageManager.NameNotFoundException e) {
            return "Retail Mode jeszcze nie jest zainstalowany";
        } catch (RuntimeException e) {
            return "Uprawnienia: blad " + e.getClass().getSimpleName();
        }
    }
}
