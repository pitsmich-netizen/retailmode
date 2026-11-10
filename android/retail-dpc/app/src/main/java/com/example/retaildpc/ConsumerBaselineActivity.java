package com.example.retaildpc;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Read-only comparison with NORMAL, non-managed Samsung consumer setup.
 * Users install RetailSetup.apk AFTER completing standard setup (NO QR).
 * This Activity never requests device-admin or initiates retail provisioning.
 */
public final class ConsumerBaselineActivity extends Activity {
    private TextView report;
    private String lastReport="";
    private int dp(int v) {
        return (int)(v*getResources().getDisplayMetrics().density+0.5f);
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(10,15,27));
        getWindow().setNavigationBarColor(Color.rgb(10,15,27));

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(10,15,27));
        LinearLayout body=new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20),dp(20),dp(20),dp(28));
        scroll.addView(body);

        TextView title=new TextView(this);
        title.setText("SAMSUNG — TEST ZWYKŁEJ KONFIGURACJI\nRetail Setup 0.11.17");
        title.setTextSize(20);
        title.setTextColor(Color.WHITE);
        title.setPadding(0,0,0,dp(13));
        body.addView(title);
        TextView instructions=new TextView(this);
        instructions.setText("Odczyt bez QR i bez Device Owner. Nie włącza OMC, " +
            "nie uruchamia Retail Mode ani pobierania aplikacji. " +
            "Porównaj stan OMC po zwykłej konfiguracji z wcześniejszym wynikiem QR: " +
            "enabled=false, enabledSetting=2.");
        instructions.setTextSize(14);
        instructions.setTextColor(Color.rgb(180,192,209));
        instructions.setPadding(0,0,0,dp(16));
        body.addView(instructions);
        Button refresh=new Button(this);
        refresh.setText("ODŚWIEŻ STAN APLIKACJI");
        body.addView(refresh,new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,dp(52)));
        refresh.setOnClickListener(v -> read());
        Button copy=new Button(this);
        copy.setText("KOPIUJ RAPORT");
        body.addView(copy,new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,dp(52)));
        copy.setOnClickListener(v -> {
            ClipboardManager clipboard=(ClipboardManager)getSystemService(
                Context.CLIPBOARD_SERVICE);
            if(clipboard!=null)clipboard.setPrimaryClip(
                ClipData.newPlainText("Samsung consumer baseline",lastReport));
        });
        report=new TextView(this);
        report.setTextColor(Color.rgb(225,233,246));
        report.setTextSize(13);
        report.setTextIsSelectable(true);
        report.setPadding(0,dp(20),0,dp(20));
        body.addView(report);
        setContentView(scroll);
        read();
    }
    private void read() {
        StringBuilder b=new StringBuilder();
        b.append("PORÓWNANIE: ZWYKŁA KONFIGURACJA VS QR\n");
        b.append("Data: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()).format(new Date())).append("\n");
        b.append("Device Owner naszej aplikacji: ")
            .append(SingleAppOwnerRelease.isOwner(this)).append("\n");
        b.append("Tryb QR zapisany: ").append(DiagnosticMode.enabled(this))
            .append("\n");
        try {
            b.append("device_provisioned=")
                .append(Settings.Global.getInt(getContentResolver(),"device_provisioned",-1))
                .append("; user_setup_complete=")
                .append(Settings.Secure.getInt(getContentResolver(),"user_setup_complete",-1))
                .append("\n");
        }catch(RuntimeException e) {
            b.append("Status Setup Wizard: błąd odczytu\n");
        }
        b.append("\n");
        b.append(SamsungProvisioningProbe.report(this));
        b.append("\n\nSTAN 12 APLIKACJI PO ZWYKŁEJ KONFIGURACJI\n");
        PostDemoAppRestore.audit(this);
        b.append(PostDemoAppRestore.p(this).getString("audit","Brak audytu"));
        b.append("\n\nUwaga: aplikacja zainstalowana i widoczna dla PackageManager ")
            .append("nie musi oznaczać, że skończyła się aktualizować. ")
            .append("Jeżeli OMC po zwykłej konfiguracji też jest DISABLED, ")
            .append("ważniejsza jest kolejka uruchomiona wcześniej w Setup Wizard.");
        lastReport=b.toString();
        report.setText(lastReport);
    }
}
