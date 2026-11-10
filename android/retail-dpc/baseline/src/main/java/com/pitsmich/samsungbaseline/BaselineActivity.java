package com.pitsmich.samsungbaseline;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Safe standalone read-only diagnostic for consumer Samsung setup, never a DPC.
 * Deliberately requests ZERO Android permissions and has NO network code.
 */
public final class BaselineActivity extends Activity {
    private TextView content;
    private String latest="";
    private static final String[][] PACKAGES = {
        {"Recommended apps (OMC)", "com.samsung.android.app.omcagent"},
        {"Galaxy Store", "com.sec.android.app.samsungapps"},
        {"Samsung Setup Wizard", "com.sec.android.app.SecSetupWizard"},
        {"Google Setup Wizard", "com.google.android.setupwizard"},
        {"Kalendarz Samsung", "com.samsung.android.calendar"},
        {"Kalkulator Samsung", "com.sec.android.app.popupcalculator"},
        {"Zegar Samsung", "com.sec.android.app.clockpackage"},
        {"Samsung Notes", "com.samsung.android.app.notes"},
        {"Samsung Members", "com.samsung.android.voc"},
        {"Bixby", "com.samsung.android.bixby.agent"},
        {"Wskazówki", "com.samsung.android.app.tips"},
        {"Gaming Hub", "com.samsung.android.game.gamehome"},
        {"Samsung Global Goals", "com.samsung.sree"},
        {"Facebook", "com.facebook.katana"},
        {"Instagram", "com.instagram.android"},
        {"Spotify", "com.spotify.music"},
        {"Samsung Find (kandydat)", "com.samsung.android.app.find"}
    };
    private int dp(int n) {
        return (int)(n*getResources().getDisplayMetrics().density+0.5f);
    }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(16,25,46));
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout col=new LinearLayout(this);
        col.setPadding(dp(20),dp(22),dp(20),dp(30));
        col.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(col);
        TextView title=new TextView(this);
        title.setText("Test aplikacji Samsung — bez QR");
        title.setTextSize(22);
        title.setTextColor(Color.rgb(20,42,84));
        title.setPadding(0,0,0,dp(8));
        col.addView(title);
        TextView intro=new TextView(this);
        intro.setText("Tylko odczyt. Brak uprawnień, Internetu, Device Owner, " +
            "Accessibility, pobierania aplikacji i rejestracji demo. " +
            "Testuj po zwykłej konfiguracji telefonu bez kont.");
        intro.setTextSize(14);
        col.addView(intro);
        Button refresh=new Button(this);
        refresh.setText("ODŚWIEŻ RAPORT");
        col.addView(refresh,new LinearLayout.LayoutParams(-1,dp(55)));
        Button copy=new Button(this);
        copy.setText("KOPIUJ RAPORT");
        col.addView(copy,new LinearLayout.LayoutParams(-1,dp(55)));
        content=new TextView(this);
        content.setTextSize(13);
        content.setTextColor(Color.rgb(28,39,64));
        content.setTextIsSelectable(true);
        content.setPadding(0,dp(10),0,0);
        col.addView(content);
        setContentView(scroll);
        refresh.setOnClickListener(v->refresh());
        copy.setOnClickListener(v-> {
            ClipboardManager cb=(ClipboardManager)getSystemService(
                Context.CLIPBOARD_SERVICE);
            if(cb!=null)cb.setPrimaryClip(ClipData.newPlainText("Samsung app audit",latest));
        });
        refresh();
    }

    private String read(String label,String pkg) {
        PackageManager pm=getPackageManager();
        StringBuilder b=new StringBuilder();
        b.append(label).append("\n").append("  ").append(pkg).append("\n");
        try {
            PackageInfo p=pm.getPackageInfo(pkg,PackageManager.MATCH_UNINSTALLED_PACKAGES);
            ApplicationInfo info=p.applicationInfo;
            boolean installed=info!=null&&(info.flags & ApplicationInfo.FLAG_INSTALLED)!=0;
            boolean system=info!=null&&(info.flags &
                (ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0;
            b.append("  version=").append(p.versionName)
                .append("; installed=").append(installed)
                .append("; system=").append(system)
                .append("; enabled=").append(info!=null && info.enabled).append("\n");
            try {
                b.append("  enabledSetting=")
                    .append(pm.getApplicationEnabledSetting(pkg))
                    .append(" (0=DEFAULT, 1=ENABLED, 2=DISABLED, 3=DISABLED_USER, 4=UNTIL_USED)")
                    .append("\n");
            }catch(RuntimeException e) {
                b.append("  enabledSetting: odmowa odczytu ")
                    .append(e.getClass().getSimpleName()).append("\n");
            }
        }catch(PackageManager.NameNotFoundException e) {
            b.append("  BRAK lub niewidoczny\n");
        }catch(RuntimeException e) {
            b.append("  nieznany stan: ").append(e.getClass().getSimpleName())
                .append("\n");
        }
        return b.toString();
    }
    private void refresh() {
        StringBuilder b=new StringBuilder("TEST ZWYKŁEJ KONFIGURACJI 1.0.0\n");
        b.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault())
            .format(new Date())).append("\n");
        try {
            b.append("device_provisioned=")
                .append(Settings.Global.getInt(getContentResolver(),"device_provisioned",-1))
                .append("; user_setup_complete=")
                .append(Settings.Secure.getInt(getContentResolver(),"user_setup_complete",-1))
                .append("\n");
        } catch(RuntimeException e) {
            b.append("Nie udało się odczytać statusu kreatora.\n");
        }
        for(String[] pkg:PACKAGES)b.append("\n").append(read(pkg[0],pkg[1]));
        b.append("\nWynik pokazuje stan pakietu. Nie dowodzi ukończenia pobierania, ")
            .append("jeśli ikona jest nadal wyszarzona. Porównujemy przede wszystkim ")
            .append("OMC z 0.11.16: enabled=false, enabledSetting=2 na zarządzanym S26.");
        latest=b.toString();
        content.setText(latest);
    }
}
