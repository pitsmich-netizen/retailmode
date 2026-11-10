package com.example.retaildpc;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import com.example.retaildpc.demo.DemoUiBridge;

/** Preparation retry while owner, or a minimal pending-START notification after owner release. */
public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if (DiagnosticMode.enabled(c)) {
            if (SingleAppOwnerRelease.isOwner(c)) DiagnosticMode.run(c);
            return;
        }
        if (SingleAppOwnerRelease.isOwner(c)) {
            if (!SingleAppOwnerRelease.blocked(c)) {
                RetailHomePolicy.ensure(c);
                Scheduler.schedule(c);
            }
            return;
        }
        if (SingleAppOwnerRelease.released(c) && State.installed(c)) {
            PostDemoAppRestore.resumeIfStarted(c);
            if (!DemoUiBridge.completed(c)) {
                DpcProgress.showReady(c);
            } else {
                // Demo is finished; disable this no-longer-needed boot component.
                try {
                    c.getPackageManager().setComponentEnabledSetting(
                        new ComponentName(c, BootReceiver.class),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
                } catch (RuntimeException ignored) { }
            }
        }
    }
}
