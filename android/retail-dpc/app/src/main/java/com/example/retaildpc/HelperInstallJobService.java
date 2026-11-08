package com.example.retaildpc;

import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
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

/** Downloads a pinned Helper APK over HTTPS, verifies bytes and package ID, then commits via DO PackageInstaller. */
public final class HelperInstallJobService extends JobService {
    private volatile boolean cancelled = false;

    @Override public boolean onStartJob(JobParameters params) {
        if (CleanupFlow.blocked(this) || !HelperStatus.configured(this)) return false;
        cancelled = false;
        new Thread(() -> {
            boolean retry = false;
            File target = new File(getCacheDir(), "RetailHelper-verified.apk");
            try {
                DevicePolicyManager dpm = getSystemService(DevicePolicyManager.class);
                if (dpm == null || !dpm.isDeviceOwnerApp(getPackageName())) {
                    State.log(this, "STOP Helper: Wrapper nie jest Device Owner");
                } else if (HelperStatus.installed(this)) {
                    State.log(this, "Helper już zainstalowany — pomijam");
                } else {
                    DpcProgress.stage(this, "helper_downloading", "Pobieram Retail Helper…");
                    State.log(this, "Pobieram Helper APK z " + HelperConfig.url(this));
                    downloadAndVerify(target);
                    if (!cancelled && !CleanupFlow.blocked(this)) install(target);
                }
            } catch (Exception e) {
                State.log(this, "BŁĄD Helper APK: " + e.getClass().getSimpleName() + " " + e.getMessage());
                DpcProgress.stage(this, "helper_retry", "Pobieranie Helpera nie powiodło się. Ponawiam…");
                retry = !cancelled && !CleanupFlow.blocked(this);
            } finally {
                target.delete();
                jobFinished(params, retry);
            }
        }, "helper-apk-worker").start();
        return true;
    }

    @Override public boolean onStopJob(JobParameters params) {
        cancelled = true;
        return true;
    }

    private void downloadAndVerify(File target) throws Exception {
        URL next = new URL(HelperConfig.url(this));
        HttpURLConnection conn = null;
        for (int hop=0; hop<8; hop++) {
            if (!"https".equalsIgnoreCase(next.getProtocol()))
                throw new SecurityException("Helper: wymagany HTTPS");
            conn = (HttpURLConnection) next.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(45000);
            conn.setRequestProperty("User-Agent", "RetailDpcHelperInstaller/0.10.1");
            int code = conn.getResponseCode();
            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                String loc = conn.getHeaderField("Location");
                if (loc == null) throw new IllegalStateException("Redirect bez Location");
                next = new URL(next, loc);
                conn.disconnect();
                continue;
            }
            if (code != 200) throw new IllegalStateException("HTTP " + code);
            break;
        }
        if (conn == null || conn.getResponseCode() != 200)
            throw new IllegalStateException("Za dużo przekierowań Helper APK");
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        long count = 0;
        try (InputStream in = conn.getInputStream(); OutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[65536];
            int n;
            while (!cancelled && (n = in.read(buffer)) != -1) {
                count += n;
                if (count > 30L * 1024L * 1024L) throw new IllegalStateException("Helper APK jest za duży");
                sha.update(buffer, 0, n);
                out.write(buffer, 0, n);
            }
        } finally { conn.disconnect(); }
        if (cancelled) throw new IllegalStateException("Przerwano pobieranie Helpera");
        StringBuilder hex = new StringBuilder();
        for (byte b : sha.digest()) hex.append(String.format(Locale.ROOT,"%02x", b & 0xff));
        if (!HelperConfig.sha256(this).equalsIgnoreCase(hex.toString())) {
            target.delete();
            throw new SecurityException("Niepoprawne SHA-256 Helper APK: " + hex);
        }
        State.log(this, "Helper: pobrano " + count + " bajtów; SHA-256 zgodne");
    }

    private void install(File apk) throws Exception {
        PackageInfo info = getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(), 0);
        if (info == null || !Constants.HELPER_PACKAGE.equals(info.packageName))
            throw new SecurityException("APK nie ma oczekiwanego pakietu com.example.retailhelper");
        PackageInstaller installer = getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams sp = new PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        sp.setAppPackageName(Constants.HELPER_PACKAGE);
        if (Build.VERSION.SDK_INT >= 31)
            sp.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
        int id = installer.createSession(sp);
        try (PackageInstaller.Session session = installer.openSession(id)) {
            try (InputStream in = new FileInputStream(apk);
                 OutputStream out = session.openWrite("base.apk", 0, apk.length())) {
                byte[] buffer = new byte[65536]; int n;
                while ((n=in.read(buffer))!=-1) out.write(buffer,0,n);
                session.fsync(out);
            }
            Intent callback = new Intent(this, HelperInstallResultReceiver.class);
            callback.setAction(getPackageName()+".HELPER_INSTALL_RESULT");
            PendingIntent pi = PendingIntent.getBroadcast(this, id, callback,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            State.log(this, "Helper: PackageInstaller.commit dla " + Constants.HELPER_PACKAGE);
            session.commit(pi.getIntentSender());
        }
    }
}
