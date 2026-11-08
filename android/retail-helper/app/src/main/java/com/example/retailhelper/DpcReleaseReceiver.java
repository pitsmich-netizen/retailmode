package com.example.retailhelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/** Release reply must be signed by the same certificate and match the session token. */
public final class DpcReleaseReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if (i == null || !"com.example.retaildpc.action.RELEASE_RESULT_095".equals(i.getAction())) return;
        if (c.getPackageManager().checkSignatures(c.getPackageName(), "com.example.retaildpc")
                != PackageManager.SIGNATURE_MATCH) return;
        String token = i.getStringExtra("handoff_nonce");
        boolean probe = i.getBooleanExtra("silent_probe_098", false);
        HandoffBridge.recordRelease(c, token,
            i.getBooleanExtra("released", false), i.getStringExtra("detail"), probe);
        if (probe && HandoffBridge.released(c)) {
            HandoffBridge.acknowledgeStoredRelease(c, token);
        }
    }
}
