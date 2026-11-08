package com.example.retaildpc;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.admin.DevicePolicyManager;
import android.os.PersistableBundle;

public final class AdminReceiver extends DeviceAdminReceiver {
    @Override public void onEnabled(Context context, Intent intent) {
        State.log(context, "Device admin aktywowany");
        Scheduler.schedule(context);
        HandoffScheduler.schedule(context);
    }
    @Override public void onProfileProvisioningComplete(Context context, Intent intent) {
        PersistableBundle extras = intent.getParcelableExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,
            PersistableBundle.class);
        if (extras != null) {
            StoreSelection.accept(context, extras);
            HelperConfig.accept(context, extras);
        }
        State.log(context, "Zakończono provisioning (callback)");
        InitialDeviceSetup.enableLocation(context);
        Scheduler.schedule(context);
        HandoffScheduler.schedule(context);
    }
}
