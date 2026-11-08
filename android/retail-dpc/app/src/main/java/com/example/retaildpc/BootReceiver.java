package com.example.retaildpc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if (!CleanupFlow.blocked(c)) { Scheduler.schedule(c); HandoffScheduler.schedule(c); }
    }
}
