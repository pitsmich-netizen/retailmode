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
public final class MainActivity extends Activity {
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
    private final Runnable update=new Runnable(){
        @Override public void run(){
            if(isFinishing()||isDestroyed())return;
            refresh();
            if(pressed()&&State.installed(MainActivity.this)
                    &&!SingleAppOwnerRelease.released(MainActivity.this)&&!busy)
                releaseAndContinue(false);
            handler.postDelayed(this,850);
        }
    };
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        draw();
        State.log(this,"Retail Setup UI otwarty; wersja " + BuildConfig.VERSION_NAME
            + "; action=" + getIntent().getAction());
        if(SingleAppOwnerRelease.isOwner(this)&&!State.installed(this))Scheduler.schedule(this);
        refresh();
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
        text(page,"RETAIL  /  SETUP    •    0.11.1",14,BLUE,true);gap(page,14);
        text(page,"Przygotowanie urządzenia",26,WHITE,true);gap(page,8);
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
                State.logs(this)+"\n"+DemoUiBridge.logs(this)));
        });
        button(extra,"Awaryjnie przywróć pulpit Androida",PANEL).setOnClickListener(v->
            new AlertDialog.Builder(this)
                .setTitle("Wyjście awaryjne")
                .setMessage("Zwolnić Device Owner i przywrócić pulpit? " +
                    "To kończy możliwość instalacji jako administrator bez nowego QR.")
                .setNegativeButton("Anuluj",null)
                .setPositiveButton("Przywróć",(d,w)->releaseAndContinue(true)).show());
        setContentView(scroll);
    }
    private boolean pressed(){
        return getSharedPreferences(PREF,MODE_PRIVATE).getBoolean("start",false);
    }
    private void markPressed(boolean value){
        getSharedPreferences(PREF,MODE_PRIVATE).edit().putBoolean("start",value).commit();
    }
    private void startPressed(){
        if(DemoUiBridge.completed(this))return;
        markPressed(true);
        if(!SingleAppOwnerRelease.released(this)){
            if(!State.installed(this))Scheduler.schedule(this);
            else releaseAndContinue(false);
        }else continueDemo();
        refresh();
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
                        markPressed(false);
                        Intent home=new Intent(Intent.ACTION_MAIN);
                        home.addCategory(Intent.CATEGORY_HOME);
                        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(home);
                    }else title.setText("Błąd przywrócenia pulpitu — zobacz diagnostykę");
                }else if(success)continueDemo();
                else title.setText("Nie udało się zwolnić DPC — zobacz diagnostykę");
            });
        },"retail-release-owner").start();
    }
    private void continueDemo(){
        if(!SingleAppOwnerRelease.released(this)||DemoUiBridge.completed(this))return;
        String secret=QrRetailCredential.read(this);
        if((secret==null||secret.isEmpty())&&password.length()>0)
            secret=password.getText().toString();
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
        boolean waiting=DemoUiBridge.status(this).startsWith("Włącz Retail Setup");
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
        if(done){start.setText("GOTOWE");start.setEnabled(false);}
        else if(busy){start.setText("PRZYGOTOWUJĘ…");start.setEnabled(false);}
        else if(pressed()&&!released){start.setText(installed?
                "ZWALNIAM ADMINISTRATORA…":"PRZYGOTOWUJĘ…");start.setEnabled(false);}
        else if(released&&(active||waiting)){
            start.setText("TRWA KONFIGURACJA DEMO");start.setEnabled(false);
        }else{start.setText("START");start.setEnabled(true);}
        if(report.getVisibility()==View.VISIBLE)
            report.setText("Wersja: "+BuildConfig.VERSION_NAME+"\nDevice Owner: "+owner+"\nRetail APK: "+installed+
                "\n\n"+DpcProgress.lastSetupDiagnostic(this)+
                "\n\nInstalator:\n"+State.logs(this)+
                "\n\nAutomatyzacja:\n"+DemoUiBridge.logs(this));
    }
    @Override protected void onResume(){
        super.onResume();
        if(start==null)return;
        DemoUiBridge.onResume(this);
        handler.removeCallbacks(update);handler.post(update);
    }
    @Override protected void onPause(){
        handler.removeCallbacks(update);super.onPause();
    }
}
