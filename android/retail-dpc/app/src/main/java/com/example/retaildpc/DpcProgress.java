package com.example.retaildpc;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;
import java.util.Locale;

/**
 * One truthful progress indicator before handoff to Helper.
 * Download bytes come from the HTTP stream, never from a timer.
 * Background Toast delivery is best-effort and Android may throttle it.
 */
final class DpcProgress {
    private static final String CHANNEL = "retail_preparation_100";
    private static final int ID = 10200;
    private static final int RETAIL_ID = 10201;
    private static final int HELPER_ID = 10202;
    private static final int SETUP_DIAG_ID = 10203;
    private static final String PREF = "retail_progress_100";
    private static final long TOAST_GAP_MS = 4500L;
    private static final long PROGRESS_GAP_MS = 1600L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static Runnable pendingToast;
    private static long lastToastAt;
    private static final long[] lastProgressAt = new long[2];

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

    static synchronized void stage(Context c, String eventKey, String message) {
        if (CleanupFlow.blocked(c)) return;
        SharedPreferences prefs = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (eventKey.equals(prefs.getString("last_stage", ""))) return;
        prefs.edit().putString("last_stage", eventKey)
            .putString("last_message", message).apply();
        // The two download jobs run concurrently: do not share their progress bar.
        if (eventKey.startsWith("retail_") && !eventKey.startsWith("retail_downloading"))
            cancelDownload(c, RETAIL_ID);
        if (eventKey.startsWith("helper_") && !eventKey.startsWith("helper_downloading"))
            cancelDownload(c, HELPER_ID);
        notifyStatus(c, message, eventKey.startsWith("retail_downloading")
            || eventKey.startsWith("helper_downloading"), -1);
        queueLatestToast(c, eventKey, message);
    }

    /** Only called for the active download stage, at most once per ~1.6 seconds. */
    static synchronized void downloadProgress(Context c, String eventKey, String title,
                                               long bytesRead, long totalBytes) {
        if (CleanupFlow.blocked(c)) return;
        // Never reject Retail Mode progress because Helper updated the latest stage.
        final int progressId = eventKey.startsWith("helper_") ? HELPER_ID : RETAIL_ID;
        final int index = progressId == HELPER_ID ? 1 : 0;
        long now = SystemClock.elapsedRealtime();
        boolean complete = totalBytes > 0L && bytesRead >= totalBytes;
        if (!complete && lastProgressAt[index] > 0L
                && now - lastProgressAt[index] < PROGRESS_GAP_MS) return;
        lastProgressAt[index] = now;
        boolean tiny = totalBytes > 0 && totalBytes < 1_000_000L;
        double divisor = tiny ? 1000.0 : 1_000_000.0;
        String unit = tiny ? "KB" : "MB";
        String transferred = String.format(Locale.forLanguageTag("pl-PL"),
            "%.1f", bytesRead / divisor);
        final String text;
        final int percentage;
        if (totalBytes > 0L) {
            percentage = (int) Math.max(0L, Math.min(100L, bytesRead * 100L / totalBytes));
            String target = String.format(Locale.forLanguageTag("pl-PL"),
                "%.1f", totalBytes / divisor);
            text = title + ": " + percentage + "% (" + transferred
                + "/" + target + " " + unit + ")";
        } else {
            percentage = -1;
            text = title + ": pobrano " + transferred + " " + unit;
        }
        notifyDownload(c, progressId, title, text, percentage);
    }

    private static void notifyDownload(Context c, int id, String title, String message, int percent) {
        try {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm == null) return;
            ensureChannel(nm);
            if (Build.VERSION.SDK_INT >= 33 && c.checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
            Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, CHANNEL) : new Notification.Builder(c);
            builder.setSmallIcon(R.drawable.retail_progress)
                .setContentTitle(title + " — pobieranie")
                .setContentText(message)
                .setOnlyAlertOnce(true).setOngoing(true)
                .setCategory(Notification.CATEGORY_STATUS).setShowWhen(false);
            if (percent >= 0) builder.setProgress(100, percent, false);
            else builder.setProgress(0, 0, true);
            nm.notify(id, builder.build());
        } catch (RuntimeException e) {
            State.log(c, "Pasek pobierania: " + e.getClass().getSimpleName());
        }
    }

    private static void cancelDownload(Context c, int id) {
        try {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm != null) nm.cancel(id);
        } catch (RuntimeException ignored) { }
    }

    private static void ensureChannel(NotificationManager nm) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL,
                "Przygotowanie Retail Mode", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Tymczasowy stan instalacji urządzenia testowego");
            channel.setSound(null, null);
            channel.enableVibration(false);
            nm.createNotificationChannel(channel);
        }
    }

    /**
     * Visible experimental diagnostic, limited to the Retail Mode gate.
     * The parallel Helper job records its own flag transitions to State.log,
     * but must not overwrite Retail's notification.
     */
    static synchronized void setupFlagDiagnostics(Context c, String task,
            boolean provisioned, boolean finished, long elapsedMs,
            long provisionedAtMs, long finishedAtMs) {
        if (!"retail".equals(task) || CleanupFlow.blocked(c)) return;
        String pending = !provisioned && !finished ? "obie flagi"
            : !provisioned ? "device_provisioned" : "user_setup_complete";
        String deviceAt = provisionedAtMs >= 0
            ? "+" + (provisionedAtMs / 1000) + " s" : "nadal 0";
        String userAt = finishedAtMs >= 0
            ? "+" + (finishedAtMs / 1000) + " s" : "nadal 0";
        String detail = "Czekam na: " + pending + " | " + (elapsedMs / 1000) + " s"
            + "\n" + "device_provisioned=" + (provisioned ? 1 : 0)
            + " (od " + deviceAt + ")"
            + "\n" + "user_setup_complete=" + (finished ? 1 : 0)
            + " (od " + userAt + ")";
        try {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm == null) return;
            ensureChannel(nm);
            if (Build.VERSION.SDK_INT >= 33 && c.checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
            Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, CHANNEL) : new Notification.Builder(c);
            builder.setSmallIcon(R.drawable.retail_progress)
                .setContentTitle("Android Setup — diagnostyka")
                .setContentText("Oczekuję: " + pending + " (" + (elapsedMs / 1000) + " s)")
                .setStyle(new Notification.BigTextStyle().bigText(detail))
                .setOnlyAlertOnce(true).setOngoing(true)
                .setCategory(Notification.CATEGORY_STATUS).setShowWhen(false);
            nm.notify(SETUP_DIAG_ID, builder.build());
        } catch (RuntimeException e) {
            State.log(c, "Diagnostyka powiadomienia: " + e.getClass().getSimpleName());
        }
    }

    /**
     * Persist the diagnostic after the setup gate finishes. The old 0.10.11
     * cleared it immediately, so the user never saw it if the flags were
     * already ready at JobService start.
     */
    static synchronized void completeSetupFlagDiagnostics(Context c, String task,
            long queuedMs, long gateMs, long deviceAtMs, long userAtMs) {
        if (!"retail".equals(task) || CleanupFlow.blocked(c)) return;
        String job = queuedMs >= 0 ? (queuedMs / 1000.0) + " s" : "niedostępne";
        String device = deviceAtMs == 0L ? "1 przy pierwszym odczycie"
            : deviceAtMs >= 0 ? "1 wykryto po " + (deviceAtMs / 1000.0) + " s" : "nie wykryto";
        String user = userAtMs == 0L ? "1 przy pierwszym odczycie"
            : userAtMs >= 0 ? "1 wykryto po " + (userAtMs / 1000.0) + " s" : "nie wykryto";
        String summary = "Od kolejki do JobService: " + job
            + "\nOczekiwanie na flagi: " + (gateMs / 1000.0) + " s"
            + "\ndevice_provisioned: " + device
            + "\nuser_setup_complete: " + user;
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString("setup_result", summary).apply();
        try {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm == null) return;
            ensureChannel(nm);
            if (Build.VERSION.SDK_INT >= 33 && c.checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                State.log(c, "Diagnostyka: brak zgody na powiadomienia; wynik pozostaje w logu");
                return;
            }
            Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, CHANNEL) : new Notification.Builder(c);
            b.setSmallIcon(R.drawable.retail_progress)
                .setContentTitle("Diagnostyka startu — wynik")
                .setContentText("JobScheduler: " + job
                    + "; flagi: " + (gateMs / 1000.0) + " s")
                .setStyle(new Notification.BigTextStyle().bigText(summary))
                .setOnlyAlertOnce(true).setOngoing(true)
                .setCategory(Notification.CATEGORY_STATUS).setShowWhen(false);
            nm.notify(SETUP_DIAG_ID, b.build());
        } catch (RuntimeException e) {
            State.log(c, "Diagnostyka wyniku: " + e.getClass().getSimpleName());
        }
    }

    static String lastSetupDiagnostic(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("setup_result", "Wynik pomiarów jeszcze niedostępny");
    }

    static synchronized void clearSetupFlagDiagnostics(Context c, String task) {
        if (!"retail".equals(task)) return;
        try {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm != null) nm.cancel(SETUP_DIAG_ID);
        } catch (RuntimeException ignored) { }
    }

    private static void notifyStatus(Context c, String message,
                                     boolean downloading, int percent) {
        try {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm == null) return;
            ensureChannel(nm);
            if (Build.VERSION.SDK_INT >= 33 && c.checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                return;
            Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, CHANNEL) : new Notification.Builder(c);
            builder.setSmallIcon(R.drawable.retail_progress)
                .setContentTitle("Przygotowanie Retail Mode")
                .setContentText(message)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_STATUS)
                .setShowWhen(false);
            if (downloading) {
                if (percent >= 0) builder.setProgress(100, percent, false);
                else builder.setProgress(0, 0, true);
            }
            nm.notify(ID, builder.build());
        } catch (RuntimeException e) {
            State.log(c, "UI postępu: " + e.getClass().getSimpleName());
        }
    }

    /** Coalesce stage notifications; never display a Toast for an obsolete stage. */
    private static void queueLatestToast(Context c, String eventKey, String message) {
        Context app = c.getApplicationContext();
        MAIN.post(() -> {
            if (pendingToast != null) MAIN.removeCallbacks(pendingToast);
            long elapsed = SystemClock.elapsedRealtime();
            long delay = lastToastAt == 0L ? 0L
                : Math.max(0L, TOAST_GAP_MS - (elapsed - lastToastAt));
            pendingToast = () -> {
                pendingToast = null;
                if (CleanupFlow.blocked(app) ||
                    !eventKey.equals(app.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                        .getString("last_stage", ""))) return;
                lastToastAt = SystemClock.elapsedRealtime();
                try {
                    Toast.makeText(app, message, Toast.LENGTH_SHORT).show();
                } catch (RuntimeException e) {
                    State.log(app, "Toast: " + e.getClass().getSimpleName());
                }
            };
            MAIN.postDelayed(pendingToast, delay);
        });
    }

    static synchronized void clear(Context c) {
        MAIN.post(() -> {
            if (pendingToast != null) MAIN.removeCallbacks(pendingToast);
            pendingToast = null;
        });
        try {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.cancel(ID);
                nm.cancel(RETAIL_ID);
                nm.cancel(HELPER_ID);
                nm.cancel(SETUP_DIAG_ID);
            }
        } catch (RuntimeException ignored) { }
    }
}
