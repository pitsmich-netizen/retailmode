package com.example.retaildpc;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Temporary convenience PIN to protect the in-app service UI from casual entry.
 * This is NOT a real authentication boundary: a 4-digit PIN in a local app can
 * be brute-forced or reverse engineered. No secrets, Wi-Fi passwords or
 * device-owner policies should be considered protected by this gate.
 * Gate is displayed only after demo completion has been acknowledged.
 */
final class ServicePanelPin {
    private static final String PIN_DIGEST =
        "e98411c00c81717493c0d305fb982cd9617dc2cb15831c4c78c85d2245bdc66f";
    private static final String PIN_PREFIX = "RetailSetup/panel/temporary/v1:";
    private static final String PREF = "retail_service_panel_pin_0116";
    private static final int BG = Color.rgb(10, 16, 30);
    private static final int PANEL = Color.rgb(24, 35, 54);
    private static final int TEXT = Color.rgb(236, 243, 252);
    private static final int MUTED = Color.rgb(164, 180, 199);
    private static final int BLUE = Color.rgb(72, 133, 249);
    private ServicePanelPin() { }

    private static int dp(Activity a, int px) {
        return (int)(px * a.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static GradientDrawable rounded(Activity a, int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(a, 16));
        return d;
    }

    static void show(Activity a, Runnable onUnlocked) {
        ScrollView scroll = new ScrollView(a);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(a,26), dp(a,46), dp(a,26), dp(a,30));
        scroll.addView(page);
        TextView eyebrow = label(a, page, "RETAIL  /  SETUP    •    SERVICE", 13, BLUE, true);
        eyebrow.setLetterSpacing(0.13f);
        spacer(a,page,24);
        label(a,page,"Panel administracyjny",27,TEXT,true);
        spacer(a,page,9);
        label(a,page,"Wprowadź PIN, aby zobaczyć narzędzia serwisowe.",14,MUTED,false);
        spacer(a,page,23);

        LinearLayout card = new LinearLayout(a);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(a,18),dp(a,22),dp(a,18),dp(a,24));
        card.setBackground(rounded(a,PANEL));
        page.addView(card,new LinearLayout.LayoutParams(-1,-2));
        label(a,card,"DOSTĘP CHRONIONY",12,BLUE,true);
        spacer(a,card,12);
        EditText pin = new EditText(a);
        pin.setSingleLine(true);
        pin.setTextColor(TEXT);
        pin.setHintTextColor(MUTED);
        pin.setHint("4-cyfrowy PIN");
        pin.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pin.setImeOptions(EditorInfo.IME_ACTION_DONE);
        pin.setTextSize(21);
        card.addView(pin,new LinearLayout.LayoutParams(-1,dp(a,56)));
        spacer(a,card,8);
        TextView info = label(a,card,"Dostęp tylko dla upoważnionego personelu.",13,MUTED,false);
        spacer(a,card,15);
        Button open = new Button(a);
        open.setAllCaps(false);
        open.setText("Otwórz panel");
        open.setTextColor(Color.WHITE);
        open.setTextSize(16);
        open.setBackground(rounded(a,BLUE));
        card.addView(open,new LinearLayout.LayoutParams(-1,dp(a,54)));

        Runnable verify = () -> {
            SharedPreferences pref = a.getSharedPreferences(PREF,Context.MODE_PRIVATE);
            long now = SystemClock.elapsedRealtime();
            long blockedUntil = pref.getLong("blocked_until_elapsed",0L);
            if (blockedUntil > now) {
                info.setText("Za dużo prób. Spróbuj ponownie za "
                    + ((blockedUntil - now + 999L)/1000L) + " s.");
                return;
            }
            String input = pin.getText().toString();
            pin.getText().clear();
            if (input.length() == 4 && matches(input)) {
                pref.edit().clear().apply();
                onUnlocked.run();
                return;
            }
            int failed = pref.getInt("failed",0)+1;
            if (failed >= 5) {
                pref.edit().putInt("failed",0)
                    .putLong("blocked_until_elapsed",now+60_000L).apply();
                info.setText("Za dużo prób. Dostęp zablokowany na 60 s.");
            } else {
                pref.edit().putInt("failed",failed).apply();
                info.setText("Nieprawidłowy PIN. Pozostało prób: "+(5-failed));
            }
        };
        open.setOnClickListener(v->verify.run());
        pin.setOnEditorActionListener((v,action,event)->{
            if(action==EditorInfo.IME_ACTION_DONE){verify.run();return true;}
            return false;
        });

        LinearLayout.LayoutParams grow = new LinearLayout.LayoutParams(1,0,1f);
        page.addView(new View(a),grow);
        spacer(a,page,32);
        TextView footer=label(a,page,
            "TYLKO DO UŻYTKU WEWNĘTRZNEGO\n"
            + "Autor i opracowanie: Piotr Michalik\n"
            + "Narzędzie testowe • Nie jest oficjalnym produktem Samsung\n"
            + "Wyłącznie na uprawnionych urządzeniach",11,MUTED,false);
        footer.setGravity(Gravity.CENTER_HORIZONTAL);
        a.setContentView(scroll);
    }

    private static TextView label(Activity a,LinearLayout p,String v,int size,int color,
                                  boolean strong) {
        TextView t = new TextView(a);
        t.setText(v);t.setTextSize(size);t.setTextColor(color);
        if(strong)t.setTypeface(null,Typeface.BOLD);
        p.addView(t);
        return t;
    }
    private static void spacer(Activity a,LinearLayout p,int amount) {
        p.addView(new View(a),new LinearLayout.LayoutParams(1,dp(a,amount)));
    }
    private static boolean matches(String pin) {
        try {
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(
                (PIN_PREFIX+pin).getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(hexDecode(PIN_DIGEST),digest);
        } catch (NoSuchAlgorithmException error) {
            return false;
        }
    }
    private static byte[] hexDecode(String hex) {
        byte[] result=new byte[hex.length()/2];
        for(int i=0;i<result.length;i++) {
            int hi=Character.digit(hex.charAt(i*2),16);
            int lo=Character.digit(hex.charAt(i*2+1),16);
            result[i]=(byte)((hi<<4)|lo);
        }
        return result;
    }
}
