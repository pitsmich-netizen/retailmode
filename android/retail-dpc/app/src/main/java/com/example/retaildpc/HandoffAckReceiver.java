package com.example.retaildpc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/** Only a signature-matched Helper with our per-install nonce can request owner release. */
public final class HandoffAckReceiver extends BroadcastReceiver {
    static final String ACTION = "com.example.retaildpc.action.HELPER_READY_095";
    static final String ACTION_RELEASE_RECEIVED = "com.example.retaildpc.action.RELEASE_RECEIVED_098";
    @Override public void onReceive(Context c, Intent i) {
        if (i == null || !(ACTION.equals(i.getAction()) || ACTION_RELEASE_RECEIVED.equals(i.getAction()))) return;
        if (c.getPackageManager().checkSignatures(c.getPackageName(), Constants.HELPER_PACKAGE)
                != PackageManager.SIGNATURE_MATCH) {
            State.log(c, "Handoff: odmowa — certyfikaty DPC i Helpera różnią się");
            return;
        }
        String nonce = i.getStringExtra(HelperHandoff.EXTRA_NONCE);
        if (!HelperHandoff.validate(c, nonce)) {
            State.log(c, "Handoff: odmowa — brak poprawnego identyfikatora sesji");
            return;
        }
        // Confirmed foreground Helper reception. No password/data is sent in broadcast.
        // DevicePolicyManager is a binder call; don't hold the broadcast main thread.
        final PendingResult pending = goAsync();
        new Thread(() -> {
            try {
                if (ACTION_RELEASE_RECEIVED.equals(i.getAction())) {
                    OwnerReleaseFlow.onConfirmedReleaseReceived(c.getApplicationContext(), nonce);
                } else {
                    OwnerReleaseFlow.onConfirmedHelper(c.getApplicationContext(), nonce);
                }
            }
            finally { pending.finish(); }
        }, "retail-owner-release").start();
    }
}
