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
        if(x!=null) {
            MinimalOwnerProbe.accept(this,x);
            DiagnosticMode.accept(this,x);
            if(!MinimalOwnerProbe.enabled(this)) {
                StoreSelection.accept(this,x);
                QrRetailCredential.accept(this,x);
            }
        }
        State.log(this,"ADMIN_POLICY_COMPLIANCE");
        MinimalOwnerProbe.capture(this,"ADMIN_POLICY_COMPLIANCE_BEFORE_POLICIES");
        if(MinimalOwnerProbe.enabled(this)){
            MinimalOwnerProbe.exposeOwnLauncher(this);
            MinimalOwnerProbe.capture(this,"MINIMAL_COMPLIANCE_NO_POLICIES");
            DiagnosticMode.run(this);
            setResult(RESULT_OK);
            finish();
            return;
        }
        if (!DiagnosticMode.enabled(this)) GooglePlayVersionAudit.baseline(this);
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
