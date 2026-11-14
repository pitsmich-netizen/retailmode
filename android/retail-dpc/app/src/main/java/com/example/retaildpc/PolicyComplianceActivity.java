package com.example.retaildpc;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.os.Bundle;
import android.os.PersistableBundle;

/** Always proceeds through the normal Retail Mode setup path. */
public final class PolicyComplianceActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        PersistableBundle extras=getIntent().getParcelableExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,
            PersistableBundle.class);
        if(extras!=null) {
            StoreSelection.accept(this,extras);
            QrRetailCredential.accept(this,extras);
        }
        State.log(this,"ADMIN_POLICY_COMPLIANCE");
        GooglePlayVersionAudit.baseline(this);
        InitialDeviceSetup.enableLocation(this);
        DpcProgress.preparePermission(this);
        RetailHomePolicy.ensure(this);
        DpcProgress.stage(this,"provisioned","Przygotowuję Retail Mode…");
        Scheduler.schedule(this);
        setResult(RESULT_OK);
        finish();
    }
}
