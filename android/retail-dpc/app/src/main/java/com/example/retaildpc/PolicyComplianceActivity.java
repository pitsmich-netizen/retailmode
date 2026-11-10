package com.example.retaildpc;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.os.Bundle;
import android.os.PersistableBundle;
public final class PolicyComplianceActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        PersistableBundle x=getIntent().getParcelableExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,
            PersistableBundle.class);
        if(x!=null){DiagnosticMode.accept(this,x);StoreSelection.accept(this,x);QrRetailCredential.accept(this,x);}
        State.log(this,"ADMIN_POLICY_COMPLIANCE");
        InitialDeviceSetup.enableLocation(this);
        DpcProgress.preparePermission(this);
        RetailHomePolicy.ensure(this);
        if (DiagnosticMode.enabled(this)) {
            DpcProgress.stage(this,"diagnostic_ready","Diagnoza fabrycznych aplikacji — bez demo");
            DiagnosticMode.run(this);
        } else {
            DpcProgress.stage(this,"provisioned","Przygotowuję Retail Mode…");
            Scheduler.schedule(this);
        }
        setResult(RESULT_OK);finish();
    }
}
