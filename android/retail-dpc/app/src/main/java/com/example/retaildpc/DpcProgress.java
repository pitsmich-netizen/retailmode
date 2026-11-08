package com.example.retaildpc;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;

/** Progress for the period before Helper is visible. Never reports a stage that has not occurred.
 * Toast = best effort because Android rate-limits background toasts.
 * Ongoing notification = durable fallback (visible if POST_NOTIFICATIONS granted by DO).
 * Both are removed when Helper assumes control; neither impacts provisioning decisions.
 */
final class DpcProgress {
    private static final String CHANNEL = "retail_preparation_100";
    private static final int ID = 10200;
    private static final String PREF = "retail_progress_100";
    private static final long TOAST_GAP_MS = 5500L;
    private DpcProgress() { }

    static void preparePermission(Context c) {
        if (Build.VERSION.SDK_INT < 33) return;
        try {
            DevicePolicyManager dpm = c.getSystemService(DevicePolicyManager.class);
            if (dpm == null || !dpm.isDeviceOwnerApp(c.getPackageName())) return;
            dpm.setPermissionGrantState(new ComponentName(c, AdminReceiver.class),
                c.getPackageName(), Manifest.permission.POST_NOTIFICATIONS,
                DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        } catch (RuntimeException ex) {
            State.log(c, "Powiadomienia: brak możliwości nadania zgody: "
                + ex.getClass().getSimpleName());
        }
    }

    static void stage(Context c, String eventKey, String message) {
        if (CleanupFlow.blocked(c)) return;
        if (c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("last_stage", "").equals(eventKey)) return;
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString("last_stage", eventKey).putString("last_message", message).apply();
        try {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm != null) {
                if (Build.VERSION.SDK_INT >= 26) {
                    NotificationChannel channel = new NotificationChannel(CHANNEL,
                        "Przygotowanie Retail Mode", NotificationManager.IMPORTANCE_HIGH);
                    channel.setDescription("Tymczasowy stan instalacji urządzenia testowego");
                    channel.setSound(null, null);
                    channel.enableVibration(false);
                    nm.createNotificationChannel(channel);
                }
                if (Build.VERSION.SDK_INT < 33 || c.checkSelfPermission(
                        Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                        ? new Notification.Builder(c, CHANNEL) : new Notification.Builder(c);
                    Notification notification = builder
                        .setSmallIcon(R.drawable.retail_progress)
                        .setContentTitle("Przygotowanie Retail Mode")
                        .setContentText(message)
                        .setOnlyAlertOnce(true)
                        .setOngoing(true)
                        .setCategory(Notification.CATEGORY_STATUS)
                        .setShowWhen(false)
                        .build();
                    nm.notify(ID, notification);
                }
            }
        } catch (RuntimeException e) { State.log(c, "UI postępu: " + e.getClass().getSimpleName()); }
        try {
            // Request lightweight toasts even during Android Setup Wizard. On some
            // OEM builds the OS may suppress background toasts; the ongoing
            // notification remains the durable indicator in that case.
            long now = SystemClock.elapsedRealtime();
            long last = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getLong("toast_at", 0L);
            if (last > 0L && now - last < TOAST_GAP_MS) return;
            c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putLong("toast_at", now).apply();
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    Toast.makeText(c.getApplicationContext(), message, Toast.LENGTH_LONG).show();
                } catch (RuntimeException ex) {
                    State.log(c, "Toast UI: " + ex.getClass().getSimpleName());
                }
            });
        } catch (RuntimeException e) { State.log(c, "Toast: " + e.getClass().getSimpleName()); }
    }

    static void clear(Context c) {
        try {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm != null) nm.cancel(ID);
        } catch (RuntimeException ignored) { }
    }
}
