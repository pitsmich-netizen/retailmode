package com.example.retaildpc;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;

public final class AdminReceiver extends DeviceAdminReceiver {
    @Override public void onEnabled(Context c, Intent i) {
        State.log(c,"Device admin enabled");
        MinimalOwnerProbe.capture(c,"DEVICE_ADMIN_ENABLED");
        if (MinimalOwnerProbe.enabled(c)) return;
        DpcProgress.preparePermission(c);
        RetailHomePolicy.ensure(c);
        // Do not schedule the download before QR extras are parsed.
        // onProfileProvisioningComplete / PolicyComplianceActivity do this later.
    }
    @Override public void onProfileProvisioningComplete(Context c,Intent i) {
        PersistableBundle b=i.getParcelableExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,
            PersistableBundle.class);
        if(b!=null) {
            MinimalOwnerProbe.accept(c,b);
            DiagnosticMode.accept(c,b);
            if(!MinimalOwnerProbe.enabled(c)) {
                StoreSelection.accept(c,b);
                QrRetailCredential.accept(c,b);
            }
        }
        MinimalOwnerProbe.capture(c,"PROFILE_PROVISIONING_COMPLETE");
        if (MinimalOwnerProbe.enabled(c)) {
            DiagnosticMode.run(c);
            return;
        }
        if (!DiagnosticMode.enabled(c)) GooglePlayVersionAudit.baseline(c);
        InitialDeviceSetup.enableLocation(c);
        DpcProgress.preparePermission(c);
        RetailHomePolicy.ensure(c);
        if (DiagnosticMode.enabled(c)) DiagnosticMode.run(c);
        else Scheduler.schedule(c);
    }
}
