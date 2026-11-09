package com.example.retaildpc;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;

public final class AdminReceiver extends DeviceAdminReceiver {
    @Override public void onEnabled(Context c, Intent i) {
        State.log(c,"Device admin enabled");
        DpcProgress.preparePermission(c);
        RetailHomePolicy.ensure(c);
        Scheduler.schedule(c);
    }
    @Override public void onProfileProvisioningComplete(Context c,Intent i) {
        PersistableBundle b=i.getParcelableExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,
            PersistableBundle.class);
        if(b!=null){StoreSelection.accept(c,b);QrRetailCredential.accept(c,b);}
        InitialDeviceSetup.enableLocation(c);
        DpcProgress.preparePermission(c);
        RetailHomePolicy.ensure(c);
        Scheduler.schedule(c);
    }
}
