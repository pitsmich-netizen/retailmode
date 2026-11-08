package com.example.retailhelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.content.ComponentName;
import android.view.View;
import android.os.Build;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.widget.*;
import java.util.Arrays;

/** UI: one primary action, explicit opt-in for the password automation. */
public final class MainActivity extends Activity {
    private TextView status;
    private EditText profileModel, passwordInput, siteInput;
    private CheckBox autoPassword;
    private TextView technicalStatus;
    private LinearLayout advanced;
    private Button advancedToggle;
    private final android.os.Handler handoffHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private int handoffPolls = 0;
    private final Runnable handoffPoll = new Runnable() {
        @Override public void run() {
            if (isFinishing() || isDestroyed()) return;
            if (HandoffBridge.released(MainActivity.this)) {
                HandoffBridge.launchSystemUninstall(MainActivity.this);
                refresh();
                if (HandoffBridge.waitingForSilentProbe(MainActivity.this) && handoffPolls++ < 30) {
                    handoffHandler.postDelayed(this, 800);
                }
                return;
            }
            if (HandoffBridge.hasSession(MainActivity.this) && handoffPolls++ < 30) {
                HandoffBridge.confirmForeground(MainActivity.this);
                handoffHandler.postDelayed(this, 800);
            }
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        HandoffBridge.acceptFromDpc(this, getIntent());
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 36, 32, 24);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        TextView title = new TextView(this);
        title.setText("Retail Helper 0.2.11\nKonfiguracja demo");
        title.setTextSize(19);
        layout.addView(title);
        TextView notice = new TextView(this);
        notice.setText("1. Sprawdź model i sklep, wpisz hasło.\n" +
            "2. Naciśnij ROZPOCZNIJ DEMO. Jeśli potrzeba, otworzą się ustawienia Dostępności.\n" +
            "3. Włącz Retail Helper. Po zgodzie proces wznowi się automatycznie.\n" +
            "4. Po rozpoczęciu pobierania wyłączę Dostępność. Helper pozostanie bez ikony w menu.\n" +
            "Usługa może odczytywać ekran i wykonywać kliknięcia wyłącznie w Retail Mode. " +
            "Korzystaj tylko na uprawnionym urządzeniu.");
        layout.addView(notice);

        text(layout, "Model telefonu (np. S26 lub A57)");
        profileModel = new EditText(this);
        profileModel.setSingleLine(true);
        profileModel.setHint("np. S26");
        String fromQr = getIntent().getStringExtra("model");
        String saved = AutomationState.profileModel(this);
        profileModel.setText(!saved.isEmpty() ? saved : fromQr != null ? fromQr : detectMarketingName());
        layout.addView(profileModel);

        text(layout, "Site ID sklepu");
        siteInput = new EditText(this);
        siteInput.setSingleLine(true);
        String fromIntent = getIntent().getStringExtra("site_id");
        siteInput.setText(fromIntent != null && !fromIntent.trim().isEmpty() ? fromIntent : AutomationState.site(this));
        layout.addView(siteInput);

        autoPassword = new CheckBox(this);
        autoPassword.setChecked(false);
        autoPassword.setText("Automatycznie wpisz i zatwierdź hasło Retail Mode");
        layout.addView(autoPassword);
        passwordInput = new EditText(this);
        passwordInput.setSingleLine(true);
        passwordInput.setHint("Hasło Retail Mode — tylko dla trybu automatycznego");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null && s.length() > 0) autoPassword.setChecked(true);
            }
            @Override public void afterTextChanged(Editable e) { }
        });
        layout.addView(passwordInput);

        add(layout, "ROZPOCZNIJ DEMO", this::confirmAndStart);
        status = new TextView(this);
        status.setTextSize(16);
        layout.addView(status);
        advancedToggle = new Button(this);
        advancedToggle.setText("Opcje zaawansowane ▸");
        advancedToggle.setOnClickListener(v -> {
            boolean show = advanced.getVisibility() != View.VISIBLE;
            advanced.setVisibility(show ? View.VISIBLE : View.GONE);
            advancedToggle.setText(show ? "Ukryj opcje zaawansowane ▾" : "Opcje zaawansowane ▸");
            if (show) refresh();
        });
        layout.addView(advancedToggle);
        advanced = new LinearLayout(this);
        advanced.setOrientation(LinearLayout.VERTICAL);
        advanced.setVisibility(View.GONE);
        layout.addView(advanced);
        add(advanced, "WZNÓW — wybierz profil na otwartym Select Profile", () -> {
            if (!saveModel()) return;
            AutomationState.startProfileOnly(this);
            refresh();
            Toast.makeText(this, "Przełącz się z powrotem do ekranu Select Profile", Toast.LENGTH_LONG).show();
        });
        add(advanced, "WZNÓW po ręcznej autoryzacji dodatkowej", () -> {
            AutomationState.resumeAfterAuthorization(this);
            refresh();
        });
        add(advanced, "STOP — wyczyść hasło i zatrzymaj automat", () -> {
            PendingStart.clear(this);
            AutomationState.stop(this, "Zatrzymano przez użytkownika");
            passwordInput.getText().clear();
            refresh();
        });
        add(advanced, "ZAKOŃCZ i wyłącz usługę Dostępności", () -> {
            if (!AutomationState.active(this)) {
                RetailAutomationService.disableManually(this);
                Toast.makeText(this, "Jeśli usługa pozostała aktywna, wyłącz ją w Ustawieniach", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Najpierw naciśnij STOP, aby przerwać aktywny przebieg", Toast.LENGTH_LONG).show();
            }
        });
        add(advanced, "Usuń Wrapper po zwolnieniu Device Owner", () -> {
            if (HandoffBridge.released(this)) {
                // A previous canceled attempt can be retried from Android Settings.
                Intent i = new Intent(Intent.ACTION_DELETE,
                    android.net.Uri.parse("package:com.example.retaildpc"));
                try { startActivity(i); } catch (RuntimeException e) {
                    Toast.makeText(this, "Usuń Retail DPC ręcznie w Ustawieniach", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Najpierw Wrapper musi zwolnić Device Owner", Toast.LENGTH_LONG).show();
            }
        });
        add(advanced, "Otwórz ustawienia Dostępności", this::openAccessibilitySettings);
        add(advanced, "Otwórz Retail Mode", this::launchRetail);
        add(advanced, "Odśwież status", this::refresh);
        technicalStatus = new TextView(this);
        advanced.addView(technicalStatus);
        setContentView(scroll);
        refresh();
        if (getIntent().getBooleanExtra("from_dpc", false) &&
                !PendingStart.accessibilityEnabled(this)) {
            Toast.makeText(this,
                "Helper gotowy. Uzupełnij dane i naciśnij ROZPOCZNIJ DEMO.",
                Toast.LENGTH_LONG).show();
        }
    }

    private void confirmAndStart() {
        if (!saveModel()) return;
        String site = siteInput.getText().toString().trim();
        if (!site.matches("[A-Za-z0-9_-]{4,40}")) {
            Toast.makeText(this, "Sprawdź Site ID", Toast.LENGTH_LONG).show(); return;
        }
        if (autoPassword.isChecked() && passwordInput.length() == 0) {
            Toast.makeText(this, "Wpisz hasło lub wyłącz automatyczne hasło", Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("Uruchomić konfigurację demo?")
            .setMessage("Użyj tylko na urządzeniu, na którym masz prawo aktywować Retail Mode. " +
                (autoPassword.isChecked() ? "Helper spróbuje wpisać hasło i kliknąć CONFIRM na ekranie hasła. " :
                    "Wpiszesz i zatwierdzisz hasło ręcznie. ") +
                "Helper wybierze właściwy profil Experience i zatwierdzi go osobnym CONFIRM.")
            .setNegativeButton("Anuluj", (d, w) -> {})
            .setPositiveButton("START", (d, w) -> begin(site, autoPassword.isChecked()))
            .show();
    }
    private void begin(String site, boolean full) {
        CompletionHider.resetForExplicitStart(this); // Explicit restart clears marker and restores the icon.
        char[] credential = null;
        try {
            if (full) {
                Editable e = passwordInput.getText();
                credential = new char[e.length()];
                e.getChars(0, e.length(), credential, 0);
            }
            SecretMemory.set(credential);
            passwordInput.getText().clear();
            if (PendingStart.accessibilityEnabled(this)) {
                PendingStart.clear(this);
                AutomationState.start(this, "Poland (SEPOL)", "AUCHAN", site, full);
                refresh();
                launchRetail();
            } else {
                PendingStart.arm(this, site, full);
                Toast.makeText(this, "Włącz usługę Retail Helper. Rejestracja rozpocznie się po zgodzie.", Toast.LENGTH_LONG).show();
                openAccessibilitySettings();
                refresh();
            }
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (credential != null) Arrays.fill(credential, '\0');
        }
    }
    /** Only opens a system settings screen; user enables the service manually. */
    private void openAccessibilitySettings() {
        AccessibilityNavigator.open(this);
    }

    private boolean saveModel() {
        String canonical = ProfileMatcher.canonicalModel(profileModel.getText().toString());
        if (canonical.isEmpty()) {
            Toast.makeText(this, "Wpisz model handlowy: S26, A57, S10...", Toast.LENGTH_LONG).show();
            return false;
        }
        AutomationState.setProfileModel(this, canonical);
        profileModel.setText(canonical);
        return true;
    }
    private String detectMarketingName() {
        String fromName = "";
        try { fromName = ProfileMatcher.canonicalModel(
            Settings.Global.getString(getContentResolver(), "device_name")); }
        catch (RuntimeException ignored) {}
        return !fromName.isEmpty() ? fromName : ProfileMatcher.canonicalModel(Build.MODEL);
    }
    private void launchRetail() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(
            "com.samsung.rms.retailagent.global");
        if (intent != null) startActivity(intent);
        else Toast.makeText(this, "Retail Mode nie jest jeszcze zainstalowane przez DPC", Toast.LENGTH_LONG).show();
    }
    private void text(LinearLayout parent, String text) {
        TextView t = new TextView(this);
        t.setText("\n" + text);
        parent.addView(t);
    }
    private void add(LinearLayout layout, String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setOnClickListener(v -> action.run());
        layout.addView(b);
    }
    @Override protected void onResume() {
        super.onResume();
        HandoffBridge.confirmForeground(this);
        handoffHandler.removeCallbacks(handoffPoll);
        handoffPolls = 0;
        if (HandoffBridge.hasSession(this)) handoffHandler.postDelayed(handoffPoll, 500);
        if (status != null) {
            if (PendingStart.beginIfReady(this)) {
                Toast.makeText(this, "Dostępność włączona. Uruchamiam Retail Mode…", Toast.LENGTH_SHORT).show();
                launchRetail();
            } else if (PendingStart.accessibilityEnabled(this) && AutomationState.active(this) &&
                       AutomationState.phase(this) == AutomationState.ENTRY) {
                launchRetail(); // foreground fallback if service background launch was blocked
            }
            refresh();
        }
    }
    @Override protected void onPause() {
        handoffHandler.removeCallbacks(handoffPoll);
        super.onPause();
    }
    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        HandoffBridge.acceptFromDpc(this, i);
    }
    private void refresh() {
        String accessibility = PendingStart.accessibilityEnabled(this) ? "włączona" : "wyłączona";
        String process = CompletionHider.downloadAcknowledged(this) ?
            "Pobieranie demo rozpoczęte. Dostępność wyłączona; ikona Helpera ukryta." :
            PendingStart.armed(this) ? "Oczekuję na włączenie Dostępności" :
            AutomationState.active(this) ? "Trwa konfiguracja demo" : "Gotowy do rozpoczęcia";
        status.setText("\nDostępność: " + accessibility + "\n" + process +
            (AutomationState.active(this) ? "\n" + AutomationState.info(this) : ""));
        technicalStatus.setText("\nModel: " + AutomationState.profileModel(this) +
            "\nOczekujący START: " + (PendingStart.armed(this) ? "TAK" : "NIE") +
            "\nAutomat: " + (AutomationState.active(this) ? "aktywny" : "zatrzymany") +
            "\nWrapper: " + HandoffBridge.status(this) +
            "\nKońcowy status Helpera: " + CompletionHider.status(this) +
            "\nEtap: " + AutomationState.phase(this) +
            "\nStatus: " + AutomationState.info(this) +
            "\n\nLogi (bez haseł):\n" + AutomationState.logs(this));
    }
}
