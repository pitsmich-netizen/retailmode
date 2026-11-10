package com.example.retaildpc;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import java.util.ArrayList;

public final class ProvisioningModeActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ArrayList<Integer> allowed = getIntent().getIntegerArrayListExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_ALLOWED_PROVISIONING_MODES);
        if (allowed != null && !allowed.contains(
                DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE)) {
            State.log(this, "ERROR: Fully Managed Device nie jest dozwolony");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        Intent response = new Intent();
        response.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_MODE,
            DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE);
        // Na przyszle provisioningi zachowaj fabryczne aplikacje systemowe.
        response.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED, true);
        PersistableBundle incoming = getIntent().getParcelableExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,
            PersistableBundle.class);
        if (incoming != null) {
            // Capture before Android Enterprise finishes provisioning.
            MinimalOwnerProbe.accept(this,incoming);
            DiagnosticMode.accept(this,incoming);
            MinimalOwnerProbe.capture(this,"GET_PROVISIONING_MODE");
            if (!MinimalOwnerProbe.enabled(this)) {
                StoreSelection.accept(this, incoming);
                QrRetailCredential.accept(this, incoming);
            }
            response.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE, incoming);
        }
        State.log(this, "GET_PROVISIONING_MODE / Fully managed");
        setResult(RESULT_OK, response);
        finish();
    }
}
