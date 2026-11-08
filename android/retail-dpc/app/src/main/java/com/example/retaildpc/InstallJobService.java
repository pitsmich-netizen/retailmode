package com.example.retaildpc;

import android.app.admin.DevicePolicyManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.os.Build;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

public final class InstallJobService extends JobService {
    private volatile boolean cancelled = false;

    @Override public boolean onStartJob(JobParameters params) {
        if (CleanupFlow.blocked(this)) { State.log(this, "Instalator zablokowany po próbie cleanup"); return false; }
        cancelled = false;
        new Thread(() -> {
            boolean retry = false;
            try {
                DevicePolicyManager dpm = getSystemService(DevicePolicyManager.class);
                if (dpm == null || !dpm.isDeviceOwnerApp(getPackageName())) {
                    State.log(this, "STOP: aplikacja nie jest Device Owner");
                    retry = true;
                } else if (State.installed(this)) {
                    State.log(this, "Demo już zainstalowane – pomijam");
                } else {
                    DpcProgress.stage(this, "retail_downloading", "Pobieram Samsung Retail Mode…");
                    State.log(this, "Pobieram demo: " + Constants.DEMO_URL);
                    File file = new File(getCacheDir(), "RetailMode-test.apk");
                    downloadAndVerify(file);
                    if (!cancelled) install(file);
                }
            } catch (Exception e) {
                State.log(this, "BŁĄD pobrania/instalacji: " + e.getClass().getSimpleName()
                    + " " + e.getMessage());
                DpcProgress.stage(this, "retail_retry", "Pobieranie Retail Mode nie powiodło się. Ponawiam…");
                retry = !cancelled;
            } finally {
                jobFinished(params, retry);
            }
        }, "retail-dpc-worker").start();
        return true;
    }

    @Override public boolean onStopJob(JobParameters params) {
        cancelled = true;
        return true;
    }

    private void downloadAndVerify(File target) throws Exception {
        URL next = new URL(Constants.DEMO_URL);
        HttpURLConnection conn = null;
        for (int hop = 0; hop < 8; hop++) {
            if (!"https".equalsIgnoreCase(next.getProtocol()))
                throw new SecurityException("Niedozwolony protokół: " + next);
            conn = (HttpURLConnection) next.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(45000);
            conn.setRequestProperty("User-Agent", "RetailDpcTest/0.1");
            int code = conn.getResponseCode();
            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                String location = conn.getHeaderField("Location");
                if (location == null) throw new IllegalStateException("Redirect bez Location");
                next = new URL(next, location);
                conn.disconnect();
                continue;
            }
            if (code != 200) throw new IllegalStateException("HTTP " + code);
            break;
        }
        if (conn == null || conn.getResponseCode() != 200)
            throw new IllegalStateException("Zbyt wiele przekierowań");
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        long count = 0;
        try (InputStream input = conn.getInputStream();
             OutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[65536];
            int n;
            while (!cancelled && (n = input.read(buffer)) != -1) {
                count += n;
                if (count > 150L * 1024L * 1024L)
                    throw new IllegalStateException("APK za duże");
                sha.update(buffer, 0, n);
                output.write(buffer, 0, n);
            }
        } finally {
            conn.disconnect();
        }
        if (cancelled) throw new IllegalStateException("Przerwano przez system");
        StringBuilder hex = new StringBuilder();
        for (byte b : sha.digest()) hex.append(String.format(Locale.ROOT, "%02x", b & 0xff));
        if (!Constants.DEMO_SHA256.equalsIgnoreCase(hex.toString())) {
            target.delete();
            throw new SecurityException("SHA-256 demo różni się od wzorca: " + hex);
        }
        State.log(this, "Pobrano i sprawdzono SHA-256: " + count + " bajtów");
    }

    private void install(File apk) throws Exception {
        PackageInfo info = getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(), 0);
        if (info == null || info.packageName == null)
            throw new IllegalStateException("APK jest nieprawidłowe lub nie można odczytać pakietu");
        State.setPackage(this, info.packageName);
        PackageInstaller installer = getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams sp = new PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        sp.setAppPackageName(info.packageName);
        if (Build.VERSION.SDK_INT >= 31)
            sp.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
        int sessionId = installer.createSession(sp);
        try (PackageInstaller.Session session = installer.openSession(sessionId)) {
            try (InputStream input = new FileInputStream(apk);
                 OutputStream output = session.openWrite("base.apk", 0, apk.length())) {
                byte[] buffer = new byte[65536];
                int n;
                while ((n = input.read(buffer)) != -1) output.write(buffer, 0, n);
                session.fsync(output);
            }
            Intent callback = new Intent(this, InstallResultReceiver.class);
            callback.setAction(getPackageName() + ".INSTALL_RESULT");
            PendingIntent pi = PendingIntent.getBroadcast(this, sessionId, callback,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            State.log(this, "PackageInstaller.commit dla " + info.packageName);
            session.commit(pi.getIntentSender());
        }
    }
}
