package com.example.retailhelper;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;

/** Signed handoff: the actual foreground UI acknowledges DPC; user confirms system uninstall. */
final class HandoffBridge {
    private static final String DPC = "com.example.retaildpc";
    private static final String P = "retail_helper_handoff_095";
    private static final long MAX_MS = 20 * 60 * 1000L;
    private static final long SILENT_PROBE_GRACE_MS = 9000L;
    private static final String EXTRA_NONCE = "handoff_nonce";
    private HandoffBridge() { }
    private static SharedPreferences p(Context c) { return c.getSharedPreferences(P, Context.MODE_PRIVATE); }
    static void acceptFromDpc(Context c, Intent i) {
        if (i == null || !i.getBooleanExtra("from_dpc", false)) return;
        String token = i.getStringExtra(EXTRA_NONCE);
        if (token == null || !token.matches("[a-f0-9]{48}")) return;
        // Preserve the completed handshake when Android re-creates the Activity.
        if (token.equals(p(c).getString("nonce", ""))) return;
        p(c).edit().putString("nonce", token).putLong("at", System.currentTimeMillis())
            .putBoolean("released", false).putBoolean("uninstall_requested", false).commit();
    }
    static boolean hasSession(Context c) {
        String nonce = p(c).getString("nonce", "");
        long at = p(c).getLong("at", 0);
        return nonce.matches("[a-f0-9]{48}") && at > 0 &&
            System.currentTimeMillis() >= at && System.currentTimeMillis() - at < MAX_MS;
    }
    static String nonce(Context c) { return hasSession(c) ? p(c).getString("nonce", "") : ""; }
    static void confirmForeground(Activity a) {
        if (!hasSession(a) || p(a).getBoolean("released", false)) return;
        long last = p(a).getLong("sent", 0);
        if (System.currentTimeMillis() - last < 3500) return;
        Intent ack = new Intent("com.example.retaildpc.action.HELPER_READY_095");
        ack.setClassName(DPC, DPC + ".HandoffAckReceiver");
        ack.putExtra(EXTRA_NONCE, nonce(a));
        p(a).edit().putLong("sent", System.currentTimeMillis()).apply();
        try { a.sendBroadcast(ack); }
        catch (RuntimeException e) { AutomationState.log(a, "Nie wysłano ACK do DPC: " + e.getClass().getSimpleName()); }
    }
    static void recordRelease(Context c, String token, boolean released, String detail, boolean probe) {
        if (!hasSession(c) || !nonce(c).equals(token)) return;
        DevicePolicyManager dpm = c.getSystemService(DevicePolicyManager.class);
        if (dpm == null) return;
        ComponentName admin = new ComponentName(DPC, DPC + ".AdminReceiver");
        // Do not request uninstall if the device owner OR wrapper admin is still active.
        boolean verified = false;
        try {
            verified = released && !dpm.isDeviceOwnerApp(DPC) && !dpm.isAdminActive(admin);
        } catch (RuntimeException e) {
            AutomationState.log(c, "Nie można zweryfikować zwolnienia DPC: " + e.getClass().getSimpleName());
        }
        // Persist before acknowledging: a successful uninstall can terminate DPC instantly.
        p(c).edit().putBoolean("released", verified)
            .putString("detail", detail == null ? "" : detail)
            .putLong("probe_until_elapsed", verified && probe
                ? SystemClock.elapsedRealtime() + SILENT_PROBE_GRACE_MS : 0L)
            .commit();
        AutomationState.log(c, verified ? "DPC potwierdził zwolnienie roli Device Owner" :
            "DPC nie został w pełni zwolniony; nie rozpoczynam deinstalacji");
    }
    static boolean released(Context c) { return hasSession(c) && p(c).getBoolean("released", false); }
    static boolean waitingForSilentProbe(Context c) {
        long until = p(c).getLong("probe_until_elapsed", 0L);
        return released(c) && until > SystemClock.elapsedRealtime();
    }
    static void acknowledgeStoredRelease(Context c, String token) {
        if (!released(c) || !nonce(c).equals(token)) return;
        Intent ack = new Intent("com.example.retaildpc.action.RELEASE_RECEIVED_098");
        ack.setClassName(DPC, DPC + ".HandoffAckReceiver");
        ack.putExtra(EXTRA_NONCE, token);
        try { c.sendBroadcast(ack); }
        catch (RuntimeException e) {
            AutomationState.log(c, "Nie wysłano potwierdzenia odbioru zwolnienia: " + e.getClass().getSimpleName());
        }
    }
    static void launchSystemUninstall(Activity a) {
        if (!released(a) || p(a).getBoolean("uninstall_requested", false)) return;
        if (waitingForSilentProbe(a)) return; // Give PackageInstaller time; no competing UI.
        if (a.getPackageManager().getLaunchIntentForPackage(DPC) == null) {
            // DPC might already have been uninstalled; don't open the package manager again.
            p(a).edit().putBoolean("uninstall_requested", true).apply();
            return;
        }
        Intent uninstall = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + DPC));
        try {
            a.startActivity(uninstall);
            p(a).edit().putBoolean("uninstall_requested", true).apply();
            Toast.makeText(a, "Potwierdź systemowe usunięcie Wrappera, potem wróć do Helpera", Toast.LENGTH_LONG).show();
        } catch (RuntimeException e) {
            AutomationState.log(a, "Nie otwarto odinstalowania Wrappera: " + e.getClass().getSimpleName());
            Toast.makeText(a, "Nie można otworzyć deinstalatora; usuń Retail DPC w Ustawieniach", Toast.LENGTH_LONG).show();
        }
    }
    static String status(Context c) {
        if (!hasSession(c)) return "brak przekazania";
        if (released(c)) return "Device Owner zwolniony";
        return "oczekuję na potwierdzenie zwolnienia DPC";
    }
}
