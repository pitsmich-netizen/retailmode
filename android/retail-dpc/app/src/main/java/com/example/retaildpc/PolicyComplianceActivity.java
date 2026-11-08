package com.example.retaildpc;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.os.Bundle;
import android.os.PersistableBundle;

public final class PolicyComplianceActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PersistableBundle extras = getIntent().getParcelableExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,
            PersistableBundle.class);
        if (extras != null) {
            StoreSelection.accept(this, extras);
            QrRetailCredential.accept(this, extras);
            HelperConfig.accept(this, extras);
        }
        State.log(this, "ADMIN_POLICY_COMPLIANCE / Site ID: " + State.site(this));
        InitialDeviceSetup.enableLocation(this);
        DpcProgress.preparePermission(this);
        DpcProgress.stage(this, "provisioned", "Proszę czekać — przygotowuję aplikacje do instalacji");
        Scheduler.schedule(this);
        HandoffScheduler.schedule(this);
        setResult(RESULT_OK);
        finish();
    }
}
