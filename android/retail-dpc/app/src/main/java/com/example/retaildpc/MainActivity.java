package com.example.retaildpc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import com.example.retaildpc.demo.DemoUiBridge;

/** A single dark screen instead of a collection of instructions and Toasts. */
public class MainActivity extends Activity {
    private static final int BG=Color.rgb(9,15,28),PANEL=Color.rgb(23,34,53);
    private static final int WHITE=Color.rgb(240,245,250),MUTED=Color.rgb(164,182,201);
    private static final int BLUE=Color.rgb(63,121,240);
    private static final String PREF="retail_setup_one_button_011";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private TextView title,stage,subtitle,progressInfo,report,storeInfo;
    private ProgressBar progress;
    private Button start,options;
    private EditText model,site,password;
    private LinearLayout extra;
    private boolean busy,optionsShown;
    private boolean switchingToReadyTask;
    private long lastReleaseAttemptAt = 0L;
    private TextView accessibilityHint;
    private boolean servicePanelUnlocked;
    private Button accessibilityRetry;
    private TextView stockAppsReport;
    private final Runnable update=new Runnable(){
        @Override public void run(){
            if(isFinishing()||isDestroyed())return;
            refresh();
            // Preparing the handset must be automatic; START is only for demo.
            // Throttle retries if Samsung temporarily rejects owner release.
            long now = android.os.SystemClock.elapsedRealtime();
            if(State.installed(MainActivity.this)
                    &&SingleAppOwnerRelease.isOwner(MainActivity.this)
                    &&!busy&&now-lastReleaseAttemptAt>6000L){
                lastReleaseAttemptAt=now;
                if (MainActivity.this instanceof ReadyActivity) {
                    // Only a separate non-HOME task may perform owner release.
                    // Disabling the HOME alias from its own foreground task
                    // caused Samsung to minimize the setup UI in 0.11.3.
                    releaseAndContinue(false);
                } else {
                    openReadyTask();
                }
            }
            handler.postDelayed(this,850);
        }
    };
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        if (DemoUiBridge.completed(this)) {
            showServicePin();
        } else {
            draw();
        }
        State.log(this,"Retail Setup UI otwarty; wersja " + BuildConfig.VERSION_NAME
            + "; action=" + getIntent().getAction());
        if(SingleAppOwnerRelease.isOwner(this)&&!State.installed(this))Scheduler.schedule(this);
        // PIN screen intentionally has no setup controls: do not refresh
        // MainActivity views before the user unlocks the service panel.
        if (start != null) refresh();
    }
    private void showServicePin() {
        servicePanelUnlocked = false;
        start = null;
        handler.removeCallbacks(update);
        ServicePanelPin.show(this, () -> {
            servicePanelUnlocked = true;
            draw();
            refresh();
            handler.removeCallbacks(update);
            handler.post(update);
        });
    }
    private int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable rounded(int color,int radius){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;
    }
    private LinearLayout column(){
        LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);return x;
    }
    private void gap(LinearLayout x,int height){
        x.addView(new View(this),new LinearLayout.LayoutParams(1,dp(height)));
    }
    private TextView text(LinearLayout p,String label,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(label);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(null,Typeface.BOLD);
        p.addView(t);return t;
    }
    private Button button(LinearLayout p,String value,int color){
        Button b=new Button(this);b.setText(value);b.setAllCaps(false);
        b.setTextColor(WHITE);b.setTextSize(15);b.setBackground(rounded(color,14));
        LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,dp(52));
        layout.topMargin=dp(11);p.addView(b,layout);return b;
    }
    private EditText field(LinearLayout p,String hint,String value,boolean secret){
        EditText e=new EditText(this);e.setHint(hint);e.setText(value);
        e.setTextColor(WHITE);e.setHintTextColor(MUTED);e.setSingleLine(true);
        if(secret)e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        p.addView(e,new LinearLayout.LayoutParams(-1,dp(52)));
        return e;
    }
    private void draw(){
        ScrollView scroll=new ScrollView(this);
        scroll.setBackgroundColor(BG);scroll.setFillViewport(true);
        LinearLayout page=column();page.setPadding(dp(23),dp(34),dp(23),dp(36));
        scroll.addView(page);
        TextView brand = text(page,"RETAIL  /  SETUP    •    0.11.9",14,BLUE,true);
        brand.setLetterSpacing(0.12f);
        gap(page,14);
        text(page, DemoUiBridge.completed(this) ? "Panel administracyjny" :
            "Przygotowanie urządzenia",26,WHITE,true);gap(page,8);
        subtitle=text(page,"Instalacja i konfiguracja Samsung Retail Mode",14,MUTED,false);
        gap(page,25);
        LinearLayout card=column();card.setBackground(rounded(PANEL,18));
        card.setPadding(dp(18),dp(20),dp(18),dp(20));
        page.addView(card,new LinearLayout.LayoutParams(-1,-2));
        title=text(card,"Sprawdzam telefon…",20,WHITE,true);gap(card,12);
        stage=text(card,"Uruchamiam konfigurację",14,MUTED,false);
        gap(card,17);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);progress.setProgressTintList(ColorStateList.valueOf(BLUE));
        card.addView(progress,new LinearLayout.LayoutParams(-1,dp(7)));
        gap(card,11);
        progressInfo=text(card,"",12,MUTED,false);
        gap(page,21);
        storeInfo=text(page,"",13,MUTED,false);gap(page,10);
        start=button(page,"START",BLUE);start.setOnClickListener(v->startPressed());
        start.setEnabled(false);
        start.setAlpha(0.48f);
        gap(page,10);
        accessibilityHint=text(page,
            "Po naciśnięciu START otworzą się ustawienia Dostępności. " +
            "Wybierz Zainstalowane aplikacje → Retail Setup — automatyzacja demo → Włącz. " +
            "Potwierdź zgodę, a konfiguracja wznowi się automatycznie.",
            13,MUTED,false);
        // Recovery stays in the main UI; no need to open developer diagnostics
        // if Samsung briefly closes Accessibility Settings.
        accessibilityRetry=button(page,"OTWÓRZ PONOWNIE DOSTĘPNOŚĆ",PANEL);
        accessibilityRetry.setOnClickListener(v->DemoUiBridge.openAccessibility(this));
        accessibilityRetry.setVisibility(View.GONE);
        gap(page,13);
        options=button(page,"Opcje zaawansowane  ▾",PANEL);
        options.setOnClickListener(v->{
            optionsShown=!optionsShown;extra.setVisibility(optionsShown?View.VISIBLE:View.GONE);
            options.setText(optionsShown?"Ukryj opcje zaawansowane  ▴":"Opcje zaawansowane  ▾");
        });
        extra=column();extra.setVisibility(View.GONE);
        extra.setPadding(dp(8),dp(15),dp(8),dp(7));page.addView(extra);
        text(extra,"Parametry demo",16,WHITE,true);gap(extra,7);
        model=field(extra,"Model telefonu",DemoUiBridge.detectModel(this),false);
        site=field(extra,"Site ID",StoreSelection.site(this),false);
        password=field(extra,"Hasło Retail Mode (gdy brak w QR)","",true);
        button(extra,"Ponów pobieranie Retail Mode",PANEL).setOnClickListener(v->{
            if(!SingleAppOwnerRelease.isOwner(this))return;
            State.installed(this,false);Scheduler.schedule(this);refresh();
        });
        button(extra,"Ustawienia Wi-Fi",PANEL).setOnClickListener(v->
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        button(extra,"Ustawienia Dostępności",PANEL).setOnClickListener(v->
            DemoUiBridge.openAccessibility(this));
        button(extra,"Zatrzymaj automat",PANEL).setOnClickListener(v->{
            DemoUiBridge.stop(this);refresh();
        });
        button(extra,"Ponów samą rejestrację Retail Mode",PANEL)
            .setOnClickListener(v->retryRegistrationPressed());
        button(extra,"Pokaż raport aplikacji fabrycznych",PANEL).setOnClickListener(v->{
            boolean visible=stockAppsReport.getVisibility()==View.VISIBLE;
            stockAppsReport.setVisibility(visible?View.GONE:View.VISIBLE);
            if(!visible)stockAppsReport.setText(StockAppRecovery.report(this));
        });
        stockAppsReport=text(extra,"",12,MUTED,false);
        stockAppsReport.setTextIsSelectable(true);
        stockAppsReport.setVisibility(View.GONE);
        button(extra,"Pokaż / ukryj diagnostykę",PANEL).setOnClickListener(v->{
            report.setVisibility(report.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);
            refresh();
        });
        report=text(extra,"",12,MUTED,false);report.setTextIsSelectable(true);
        report.setVisibility(View.GONE);
        button(extra,"Kopiuj raport",PANEL).setOnClickListener(v->{
            ClipboardManager cb=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
            if(cb!=null)cb.setPrimaryClip(ClipData.newPlainText("Retail Setup",
                DpcProgress.lastSetupDiagnostic(this)+"\n"+
                State.logs(this)+"\n"+DemoUiBridge.logs(this)
                    +"\n\n"+StockAppRecovery.report(this)));
        });
        button(extra,"Wyjdź na pulpit Androida",PANEL).setOnClickListener(v -> {
            // Safe at any stage: restore One UI and keep downloading in the background.
            boolean ok=RetailHomePolicy.exitToAndroidHome(this);
            if(!ok) title.setText("Nie otwarto pulpitu — sprawdź diagnostykę");
        });
        button(extra,"Awaryjnie zwolnij Device Owner",PANEL).setOnClickListener(v->
            new AlertDialog.Builder(this)
                .setTitle("Awaryjne zakończenie zarządzania")
                .setMessage("Zwolnić administratora przed zakończeniem konfiguracji? " +
                    "Instalacja może zostać przerwana i wymagany będzie kolejny reset/QR.")
                .setNegativeButton("Anuluj",null)
                .setPositiveButton("Zwolnij",(d,w)->releaseAndContinue(true)).show());
        gap(page,28);
        View separator = new View(this);
        separator.setBackgroundColor(Color.rgb(48,62,82));
        page.addView(separator,new LinearLayout.LayoutParams(-1,dp(1)));
        gap(page,19);
        TextView footer = text(page,
            "TYLKO DO UŻYTKU WEWNĘTRZNEGO\n"
            + "Autor i opracowanie: Piotr Michalik\n"
            + "Narzędzie testowe • Nie jest oficjalnym produktem Samsung\n"
            + "Używaj tylko na urządzeniach, do których masz uprawnienia.\n"
            + "QR może zawierać poufne dane konfiguracji — nie udostępniaj go.",11,MUTED,false);
        footer.setLineSpacing(dp(3),1.0f);
        setContentView(scroll);
    }
    private void startPressed(){
        // A click during provisioning must never release DO or skip install.
        if(busy || !State.installed(this) || !SingleAppOwnerRelease.released(this)
                || DemoUiBridge.completed(this) || DemoUiBridge.active(this)
                || DemoUiBridge.pending(this)) return;
        DpcProgress.dismissReady(this);
        continueDemo();
        // Do NOT change HOME component enabled state on transition to Settings.
        // Samsung One UI may close the just-opened Settings task.
        // HOME is cleaned up only by the emergency exit or demo completion.
        refresh();
    }

    private void openReadyTask() {
        if (switchingToReadyTask || isFinishing() || isDestroyed()) return;
        switchingToReadyTask = true;
        try {
            Intent ready = new Intent(this, ReadyActivity.class);
            ready.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(ready);
            State.log(this, "Retail Ready: otwarto osobne zadanie poza HOME");
            // MainActivity may have been launched by the HOME alias. We must
            // finish that task's UI only AFTER starting the separate ready task.
            finish();
        } catch (RuntimeException ex) {
            switchingToReadyTask = false;
            State.log(this, "Retail Ready: nie można otworzyć ekranu: "
                + ex.getClass().getSimpleName());
            title.setText("Nie otwarto ekranu gotowości — sprawdź diagnostykę");
        }
    }
    private void releaseAndContinue(boolean emergency){
        if(busy)return;
        busy=true;refresh();
        new Thread(()->{
            boolean success=SingleAppOwnerRelease.release(getApplicationContext(),emergency);
            runOnUiThread(()->{
                busy=false;refresh();
                if(emergency){
                    if(success){
                        if(!RetailHomePolicy.exitToAndroidHome(this))
                            title.setText("Nie otwarto pulpitu — sprawdź diagnostykę");
                    }else title.setText("Nie udało się zwolnić Device Ownera — diagnostyka");
                }else if(!success) {
                    title.setText("Nie udało się zwolnić Device Ownera — diagnostyka");
                }
                // START is deliberately NOT pressed automatically after release.
            });
        },"retail-release-owner").start();
    }
    private void retryRegistrationPressed() {
        if(DemoUiBridge.completed(this)){
            new AlertDialog.Builder(this)
                .setTitle("Demo zostało już zarejestrowane")
                .setMessage("Retail Setup ma potwierdzenie pobierania lub rejestracji. " +
                    "Nie uruchamiam ponownie formularza, żeby nie zakłócać działającego demo.")
                .setPositiveButton("OK",null).show();
            return;
        }
        if(!State.installed(this)||!SingleAppOwnerRelease.released(this)){
            new AlertDialog.Builder(this)
                .setTitle("Rejestracja jeszcze niedostępna")
                .setMessage("Retail Mode musi być zainstalowany, a Device Owner zwolniony.")
                .setPositiveButton("OK",null).show();
            return;
        }
        // Use the original QR password from Android Keystore when available.
        // If it expired or could not be secured, provide a password input
        // directly in this retry dialog, not a hidden/remote settings field.
        final boolean remembered=RetryRetailCredential.available(this);
        final EditText input=new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint(remembered?"Opcjonalnie: nowe hasło Retail Mode"
            :"Wpisz hasło Retail Mode (dla pełnego automatu)");
        int margin=dp(22);
        LinearLayout form=column();
        form.setPadding(margin,dp(5),margin,0);
        form.addView(input,new LinearLayout.LayoutParams(-1,dp(58)));
        new AlertDialog.Builder(this)
            .setTitle("Ponowić samą rejestrację?")
            .setMessage("Bez ponownej instalacji APK i bez resetowania telefonu. " +
                "Automat najpierw sprawdzi stan rejestracji.\n\n" +
                (remembered
                    ?"Hasło Retail Mode z QR jest zapisane szyfrowane i zostanie użyte automatycznie. "
                    :"Hasło z QR jest niedostępne lub wygasło. Możesz podać je poniżej. ") +
                "PIN 5444 służy wyłącznie do panelu serwisowego.")
            .setView(form)
            .setNegativeButton("Anuluj",null)
            .setPositiveButton("Ponów rejestrację",(dialog,which)->{
                String typed=input.getText().toString();
                String previous=password.getText().toString();
                input.getText().clear();
                password.getText().clear();
                String secret=!typed.isEmpty()?typed:!previous.isEmpty()?previous:
                    RetryRetailCredential.read(this);
                if(secret==null)secret="";
                if(!secret.isEmpty())RetryRetailCredential.save(this,secret);
                try{
                    DemoUiBridge.retryRegistration(this,model.getText().toString(),
                        StoreSelection.country(this),StoreSelection.partner(this),
                        site.getText().toString(),secret);
                }catch(IllegalArgumentException ex){
                    title.setText(ex.getMessage());
                }
                refresh();
            }).show();
    }
    private void continueDemo(){
        if(!SingleAppOwnerRelease.released(this)||DemoUiBridge.completed(this))return;
        String secret=QrRetailCredential.read(this);
        if((secret==null||secret.isEmpty())&&password.length()>0)
            secret=password.getText().toString();
        // Save a separately encrypted short-lived copy for explicit retries.
        // The QR handoff credential is still immediately cleared afterwards.
        if(secret!=null&&!secret.isEmpty())RetryRetailCredential.save(this,secret);
        QrRetailCredential.clear(this);password.getText().clear();
        try{
            DemoUiBridge.prepare(this,model.getText().toString(),
                StoreSelection.country(this),StoreSelection.partner(this),
                site.getText().toString(),secret);
        }catch(IllegalArgumentException e){
            title.setText(e.getMessage());optionsShown=true;
            extra.setVisibility(View.VISIBLE);options.setText("Ukryj opcje zaawansowane  ▴");
        }
        refresh();
    }
    private void refresh(){
        boolean installed=State.installed(this);
        boolean owner=SingleAppOwnerRelease.isOwner(this);
        boolean released=SingleAppOwnerRelease.released(this);
        boolean done=DemoUiBridge.completed(this);
        boolean active=DemoUiBridge.active(this);
        boolean waiting=DemoUiBridge.pending(this);
        if(done)title.setText("Demo gotowe");
        else if(active)title.setText("Konfiguruję Retail Mode");
        else if(waiting)title.setText("Włącz usługę Dostępności");
        else if(released)title.setText("Gotowy do uruchomienia demo");
        else if(installed)title.setText("Retail Mode zainstalowany");
        else if(owner)title.setText("Przygotowuję Retail Mode");
        else title.setText("Urządzenie jest konfigurowane");
        stage.setText(owner&&!installed?DpcProgress.currentStage(this):
            !released?"Kończę przygotowanie urządzenia":DemoUiBridge.status(this));
        int percent=owner&&!installed?DpcProgress.retailPercent(this):100;
        if(percent<0)progress.setIndeterminate(true);
        else{progress.setIndeterminate(false);progress.setProgress(percent);}
        progressInfo.setText(owner&&!installed?
            percent<0?"Oczekuję na pobranie APK":"Pobieranie APK  •  "+percent+"%":
            done?"Zakończono":installed?"APK zainstalowane":"");
        storeInfo.setText("Model: "+DemoUiBridge.detectModel(this)+
            "     Sklep: "+StoreSelection.site(this));
        boolean ready = installed && released && !busy && !done && !active && !waiting;
        if(done)start.setText("GOTOWE");
        else if(busy||installed&&!released)start.setText("KOŃCZĘ PRZYGOTOWANIE…");
        else if(!installed)start.setText("PRZYGOTOWUJĘ…");
        else if(active||waiting)start.setText("TRWA KONFIGURACJA…");
        else start.setText("START");
        start.setEnabled(ready);
        start.setAlpha(ready ? 1f : 0.48f);
        accessibilityHint.setVisibility(done || active ? View.GONE : View.VISIBLE);
        accessibilityRetry.setVisibility(waiting && !done && !active
            ? View.VISIBLE : View.GONE);
        if(!released || !installed)
            accessibilityHint.setText("START będzie dostępny po instalacji Retail Mode " +
                "i zwolnieniu administratora. Następnie pokażemy, jak włączyć Dostępność.");
        else if(waiting)
            accessibilityHint.setText("W ustawieniach wybierz Zainstalowane aplikacje → " +
                "Retail Setup — automatyzacja demo → Włącz, potwierdź zgodę i wróć.");
        else
            accessibilityHint.setText("Po START otworzą się ustawienia Dostępności. " +
                "Wybierz Zainstalowane aplikacje → Retail Setup — automatyzacja demo → Włącz. " +
                "Potwierdź zgodę, a konfiguracja wznowi się automatycznie.");
        if(report.getVisibility()==View.VISIBLE)
            report.setText("Wersja: "+BuildConfig.VERSION_NAME+"\nDevice Owner: "+owner+"\nRetail APK: "+installed+
                "\n\n"+DpcProgress.lastSetupDiagnostic(this)+
                "\n\nInstalator:\n"+State.logs(this)+
                "\n\nAutomatyzacja:\n"+DemoUiBridge.logs(this)
                +"\n\nAplikacje fabryczne:\n"+StockAppRecovery.report(this));
    }
    @Override protected void onResume(){
        super.onResume();
        if(start==null)return;
        DemoUiBridge.onResume(this);
        handler.removeCallbacks(update);handler.post(update);
    }
    @Override protected void onPause(){
        handler.removeCallbacks(update);
        // Never disable a system HOME alias here: onPause also runs when
        // Accessibility Settings comes to foreground.
        super.onPause();
    }
    @Override protected void onStop(){
        handler.removeCallbacks(update);
        // After the demo has completed, require PIN again on every
        // exit from the administrative panel, including HOME / Recents.
        if (DemoUiBridge.completed(this) && servicePanelUnlocked) {
            showServicePin();
        }
        super.onStop();
    }
}
