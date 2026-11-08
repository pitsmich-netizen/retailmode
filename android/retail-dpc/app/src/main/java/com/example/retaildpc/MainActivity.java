package com.example.retaildpc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.net.Uri;
import android.app.job.JobScheduler;
import android.view.View;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.text.Editable;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Arrays;

/** TEST ONLY: Helper handoff can release Device Owner and request uninstallation. */
public final class MainActivity extends Activity {
    private TextView status;
    private EditText countryInput, partnerInput, siteInput, passwordInput;
    private CheckBox fullMode;
    private String cleanupReport = "Najpierw kliknij: Sprawdz gotowosc cleanup.";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(28, 32, 28, 36);
        scroll.addView(layout);
        TextView title = new TextView(this);
        title.setText("RETAIL DPC LAB " + BuildConfig.VERSION_NAME + "\nInstalacja + przekazanie Helperowi");
        title.setTextSize(20f);
        layout.addView(title);
        countryInput = field(layout, "Kraj", StoreSelection.country(this));
        partnerInput = field(layout, "Partner", StoreSelection.partner(this));
        String site = State.site(this);
        if (site.equalsIgnoreCase("BRAK") || site.equalsIgnoreCase("TEST001") || site.trim().isEmpty())
            site = StoreSelection.site(this);
        siteInput = field(layout, "Site ID", site);
        passwordInput = field(layout, "Hasło Retail Mode (opcjonalnie: nadpisuje lokalny sekret z kompilacji)", "");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setSingleLine(true);
        fullMode = new CheckBox(this);
        fullMode.setText("TRYB PEŁNY: hasło → CONFIRM → profil Experience → końcowy CONFIRM");
        fullMode.setChecked(false);
        layout.addView(fullMode);
        TextView initialResult = new TextView(this);
        initialResult.setText("Konfiguracja początkowa: lokalizacja z callbacku DO; uprawnienia po instalacji Retail APK.");
        layout.addView(initialResult);
        Button checkInitial = button(layout, "0.9.6 LAB: Włącz lokalizację i sprawdź uprawnienia Retail Mode");
        checkInitial.setOnClickListener(v -> initialResult.setText(
            InitialDeviceSetup.enableLocation(this) + "\n" +
            InitialDeviceSetup.grantRetailLocationPermissions(this)));
        Button accessibility = button(layout, "1. Dostępność — włącz usługę Retail DPC 0.9.6");
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        Button start = button(layout, "2. START: rozpoznaj PROCEED/CLOSE → kraj → partner → sklep");
        start.setOnClickListener(v -> {
            boolean full = fullMode.isChecked();
            if (full && passwordInput.length() == 0 && BuildConfig.LOCAL_TEST_RETAIL_PASSWORD.isEmpty()) {
                Toast.makeText(this, "Brak lokalnego hasła Retail Mode (wpisz je w aplikacji)", Toast.LENGTH_LONG).show();
                return;
            }
            if (full) {
                new AlertDialog.Builder(this)
                    .setTitle("Aktywacja Retail Mode")
                    .setMessage("Tryb PEŁNY może zatwierdzić konfigurację i aktywować tryb ekspozycyjny. " +
                        "Może to spowodować ograniczenia w telefonie. Uruchomić wyłącznie na uprawnionym urządzeniu testowym?")
                    .setNegativeButton("Anuluj", (d, which) -> {})
                    .setPositiveButton("Tak, aktywuj", (d, which) -> begin(true))
                    .show();
            } else begin(false);
        });
        Button resume = button(layout, "WZNÓW po ręcznej autoryzacji embargo / dodatkowej zgody");
        resume.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Wznowienie po autoryzacji")
                .setMessage("Użyj tylko wtedy, gdy uprawniony pracownik zakończył wymagane potwierdzenie, a ekran Retail Mode przeszedł dalej. Program nie wprowadzi drugiego hasła.")
                .setNegativeButton("Anuluj", (d, w) -> {})
                .setPositiveButton("Wznów", (d, w) -> {
                    AutomationState.resumeAfterAuthorization(this);
                    launchDemo();
                    refresh();
                }).show();
        });
        Button stop = button(layout, "STOP automatyzacji / wyczyść hasło z pamięci");
        stop.setOnClickListener(v -> {
            AutomationState.stop(this, "Ręcznie zatrzymane przez użytkownika");
            passwordInput.getText().clear();
            refresh();
        });
        Button open = button(layout, "Uruchom Retail Mode ręcznie");
        open.setOnClickListener(v -> launchDemo());
        Button refresh = button(layout, "Odśwież diagnostykę");
        refresh.setOnClickListener(v -> refresh());
        Button retry = button(layout, "Ponów instalację demo (tylko gdy brak APK)");
        retry.setOnClickListener(v -> {
            if (CleanupFlow.blocked(this) || !CleanupFlow.isOwner(this)) {
                Toast.makeText(this, "Instalator niedostępny po rozpoczęciu zwolnienia Device Owner", Toast.LENGTH_LONG).show();
                return;
            }
            State.installed(this, false); Scheduler.schedule(this); refresh();
        });
        TextView helperTitle = new TextView(this);
        helperTitle.setText("\nHELPER — INSTALACJA I PRZEKAZANIE DANYCH\n" +
            "Zainstaluj Helpera przez Device Owner przed zwolnieniem administratora. " +
            "Nie włączamy Accessibility automatycznie.");
        helperTitle.setTextSize(16f);
        layout.addView(helperTitle);
        TextView helperConfigInfo = new TextView(this);
        helperConfigInfo.setText("Wersja Wrappera: " + BuildConfig.VERSION_NAME +
            "\nInstalacja Helpera: " + (HelperStatus.configured(this) ? "SKONFIGUROWANA" : "NIE SKONFIGUROWANA") +
            "\nHelper URL: " + HelperConfig.url(this) +
            "\nHelper SHA-256: " + HelperConfig.sha256(this));
        helperConfigInfo.setTextSize(12f);
        layout.addView(helperConfigInfo);
        Button helperRetry = button(layout, "Helper: ponów instalację (gdy brak APK)");
        helperRetry.setOnClickListener(v -> {
            if (!CleanupFlow.isOwner(this) || CleanupFlow.blocked(this)) {
                Toast.makeText(this, "Wymagany aktywny Device Owner", Toast.LENGTH_LONG).show();
                return;
            }
            if (!HelperStatus.configured(this)) {
                Toast.makeText(this, "Brak poprawnej konfiguracji Helpera", Toast.LENGTH_LONG).show();
                return;
            }
            Scheduler.scheduleHelper(this);
            refresh();
        });
        Button handoff = button(layout, "Helper: przekaż dane sklepu i otwórz Helpera");
        handoff.setOnClickListener(v -> {
            if (!HelperStatus.installed(this)) {
                Toast.makeText(this, "Najpierw zainstaluj Retail Helper", Toast.LENGTH_LONG).show();
                return;
            }
            String country = countryInput.getText().toString().trim();
            String partner = partnerInput.getText().toString().trim();
            String siteId = siteInput.getText().toString().trim();
            if (!siteId.matches("[A-Za-z0-9_-]{4,40}") || partner.isEmpty() || country.isEmpty() ||
                    partner.length() > 80 || country.length() > 80) {
                Toast.makeText(this, "Sprawdź dane kraju, partnera i Site ID", Toast.LENGTH_LONG).show();
                return;
            }
            // Signed handoff is required before releasing Device Owner.
            if (!HelperHandoff.launchManual(this, country, partner, siteId)) {
                Toast.makeText(this, "Nie mogę przekazać Helperowi — sprawdź Device Owner i instalację", Toast.LENGTH_LONG).show();
            }
        });
        TextView cleanupTitle = new TextView(this);
        cleanupTitle.setText("\nCLEANUP — TYLKO DIAGNOSTYKA\nBez usuwania aplikacji, resetu i zmiany Device Owner");
        cleanupTitle.setTextSize(16f);
        layout.addView(cleanupTitle);
        TextView cleanupStatus = new TextView(this);
        cleanupStatus.setText("Kliknij ponizszy przycisk. Raport nie zawiera hasel.");
        cleanupStatus.setTextSize(13f);
        layout.addView(cleanupStatus);
        Button inspectCleanup = button(layout, "Sprawdz gotowosc przekazania Device Owner");
        inspectCleanup.setOnClickListener(v -> {
            cleanupReport = CleanupReadiness.check(this);
            cleanupStatus.setText(cleanupReport);
        });
        Button copyCleanup = button(layout, "Kopiuj raport cleanup (bez hasel)");
        copyCleanup.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("Retail DPC Cleanup Readiness", cleanupReport));
                Toast.makeText(this, "Raport skopiowany", Toast.LENGTH_SHORT).show();
            }
        });
        TextView testTitle = new TextView(this);
        testTitle.setText("\nLAB 0.9.6 — USTAWIENIA SYSTEMOWE I ZWOLNIENIE DEVICE OWNER\n" +
            "UWAGA: zwolnienie Device Owner jest NIEODWRACALNE bez nowego provisioningu. " +
            "Metoda clearDeviceOwnerApp() jest przeznaczona wyłącznie do testów.");
        testTitle.setTextSize(16f);
        layout.addView(testTitle);
        TextView releaseReport = new TextView(this);
        releaseReport.setTextSize(13f);
        releaseReport.setText("Najpierw sprawdź stan urządzenia.");
        layout.addView(releaseReport);
        Button snapshot = button(layout, "A. Sprawdź status systemu, Retail Mode i aplikacji");
        snapshot.setOnClickListener(v -> releaseReport.setText(CleanupFlow.snapshot(this)));
        Button restore = button(layout, "B. Przywróć dostępne fabryczne aplikacje z ikonami");
        restore.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle("Przywrócenie fabrycznych aplikacji")
            .setMessage("Android spróbuje przywrócić aplikacje obecne w obrazie fabrycznym Samsunga. " +
                "Nie pobierze z Internetu programów brakujących w ROM. Kontynuować na telefonie testowym?")
            .setNegativeButton("Anuluj", (d,w) -> {})
            .setPositiveButton("Przywróć", (d,w) -> {
                String result = CleanupFlow.restoreLauncherSystemApps(this);
                releaseReport.setText(result + "\n\n" + CleanupFlow.snapshot(this));
            }).show());
        CheckBox checkedRetail = new CheckBox(this);
        checkedRetail.setText("Sprawdziłem: Retail Mode jest zainstalowany i uruchamia się.");
        layout.addView(checkedRetail);
        CheckBox checkedApps = new CheckBox(this);
        checkedApps.setText("Sprawdziłem podstawowe aplikacje i Wi-Fi po ich przywróceniu.");
        layout.addView(checkedApps);
        CheckBox checkedReset = new CheckBox(this);
        checkedReset.setText("Akceptuję ryzyko resetu fabrycznego i brak możliwości cofnięcia Device Owner bez nowego provisioningu.");
        layout.addView(checkedReset);
        TextView phraseLabel = new TextView(this);
        phraseLabel.setText("Aby odblokować eksperymentalne zwolnienie, wpisz: ZWOLNIJ OWNER");
        layout.addView(phraseLabel);
        EditText phrase = new EditText(this);
        phrase.setSingleLine(true);
        phrase.setHint("ZWOLNIJ OWNER");
        layout.addView(phrase);
        Button release = button(layout, "C. LAB: ZWOLNIJ DEVICE OWNER (nie usuwa APK)");
        release.setOnClickListener(v -> {
            if (!checkedRetail.isChecked() || !checkedApps.isChecked() || !checkedReset.isChecked() ||
                    !CleanupFlow.CONFIRM_TEXT.equals(phrase.getText().toString().trim())) {
                Toast.makeText(this, "Zaznacz trzy potwierdzenia i przepisz wymagane hasło kontrolne", Toast.LENGTH_LONG).show();
                return;
            }
            if (!CleanupFlow.isOwner(this)) {
                Toast.makeText(this, "Wrapper nie jest już Device Owner", Toast.LENGTH_LONG).show();
                releaseReport.setText(CleanupFlow.snapshot(this));
                return;
            }
            if (!CleanupFlow.retailInstalled(this) || AutomationState.active(this)) {
                Toast.makeText(this, "Retail musi być zainstalowany, a automat zatrzymany", Toast.LENGTH_LONG).show();
                return;
            }
            if (HelperStatus.configured(this) && !HelperStatus.installed(this)) {
                Toast.makeText(this, "Najpierw zainstaluj Helpera — nie zwalniaj Device Owner", Toast.LENGTH_LONG).show();
                return;
            }
            new AlertDialog.Builder(this)
                .setTitle("OSTATECZNE POTWIERDZENIE — TEST LAB")
                .setMessage("To trwale zwolni Device Owner testowego Wrappera i może pozostawić polityki w systemie. " +
                    "Po operacji nie wykonamy provisioningu ponownie bez resetu. " +
                    "Retail Mode NIE przejmie automatycznie Device Owner. " +
                    "Żaden APK nie zostanie odinstalowany. Wykonać test?")
                .setNegativeButton("NIE — wróć", (d,w) -> {})
                .setPositiveButton("TAK — ZWOLNIJ", (d,w) -> {
                    releaseReport.setText(CleanupFlow.releaseTest(this) + "\n\n" + CleanupFlow.snapshot(this));
                    phrase.getText().clear();
                    checkedRetail.setChecked(false);
                    checkedApps.setChecked(false);
                    checkedReset.setChecked(false);
                    refresh();
                }).show();
        });
        Button deactivate = button(layout, "D. Po zwolnieniu: wyłącz starego Device Administrator Wrappera");
        deactivate.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle("Wyłączenie administratora Wrappera")
            .setMessage("Tylko jeżeli raport pokazuje Device Owner: NIE, a Retail Mode działa. Kontynuować?")
            .setNegativeButton("Anuluj", (d,w)->{})
            .setPositiveButton("Wyłącz", (d,w) -> releaseReport.setText(
                CleanupFlow.deactivateAdminAfterRelease(this) + "\n\n" + CleanupFlow.snapshot(this)))
            .show());
        Button appDetails = button(layout, "E. Otwórz ustawienia Wrappera (ręczna deinstalacja)");
        appDetails.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
        Button copyRelease = button(layout, "Kopiuj raport diagnostyczny (bez hasła)");
        copyRelease.setOnClickListener(v -> {
            ClipboardManager clipboard = getSystemService(ClipboardManager.class);
            if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText(
                "Retail DPC 0.9.3 — raport", CleanupFlow.snapshot(this)));
            Toast.makeText(this, "Raport skopiowany", Toast.LENGTH_SHORT).show();
        });
        status = new TextView(this);
        status.setTextSize(14f);
        layout.addView(status);
        setContentView(scroll);
        refresh();
    }
    private void begin(boolean full) {
        char[] secret = null;
        try {
            if (full) {
                Editable e = passwordInput.getText();
                if (e.length() > 0) {
                    secret = new char[e.length()];
                    e.getChars(0, e.length(), secret, 0);
                } else {
                    // Sekret do TESTU pochodzi z nieśledzonego pliku lokalnego; w APK można go odtworzyć.
                    secret = BuildConfig.LOCAL_TEST_RETAIL_PASSWORD.toCharArray();
                }
            }
            AutomationState.start(this, countryInput.getText().toString(),
                    partnerInput.getText().toString(), siteInput.getText().toString(), full);
            SecretMemory.set(secret);
            passwordInput.getText().clear();
            refresh();
            launchDemo();
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (secret != null) Arrays.fill(secret, '\0');
        }
    }
    private EditText field(LinearLayout box, String hint, String defaultValue) {
        TextView label = new TextView(this);
        label.setText("\n" + hint);
        box.addView(label);
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(defaultValue);
        box.addView(input);
        return input;
    }
    private Button button(LinearLayout box, String name) {
        Button button = new Button(this);
        button.setText(name);
        box.addView(button);
        return button;
    }
    private void launchDemo() {
        String target = State.pkg(this);
        if (target == null || target.isEmpty()) target = "com.samsung.rms.retailagent.global";
        Intent i = getPackageManager().getLaunchIntentForPackage(target);
        if (i != null) startActivity(i);
        else Toast.makeText(this, "Nie znaleziono Retail Mode: " + target, Toast.LENGTH_LONG).show();
    }
    @Override protected void onResume() {
        super.onResume();
        if (status != null) {
            refresh();
            HelperHandoff.launchAuto(this);
        }
    }
    private void refresh() {
        DevicePolicyManager dpm = getSystemService(DevicePolicyManager.class);
        boolean owner = dpm != null && dpm.isDeviceOwnerApp(getPackageName());
        String step = AutomationState.active(this) ? "TRWA (etap " + AutomationState.phase(this) + ")"
            : AutomationState.phase(this) == AutomationState.DONE ? "KOŃCOWE CONFIRM WYSŁANE / SPRAWDŹ AKTYWACJĘ" :
              AutomationState.phase(this) == AutomationState.EMBARGO_PAUSED ? "PAUZA: DODATKOWA AUTORYZACJA" : "STOP";
        status.setText("\nDevice Owner: " + (owner ? "TAK" : "NIE") +
            "\nRetail APK: " + (State.installed(this) ? "ZAINSTALOWANE" : "niepotwierdzone") +
            "\nHelper: " + (HelperStatus.installed(this) ? "ZAINSTALOWANY" :
                (HelperStatus.configured(this) ? "BRAK / oczekuje na instalację" : "NIE SKONFIGUROWANY")) +
            "\nParametr QR Site ID: " + State.site(this) +
            "\nPartner QR: " + StoreSelection.partner(this) +
            "\n\nAUTOMAT: " + step + "\n" + AutomationState.info(this) +
            "\n\nLog automatu:" + AutomationState.logs(this) +
            "\n\nDIAGNOSTYKA STARTU:\n" + DpcProgress.lastSetupDiagnostic(this) +
            "\n\nLog instalatora:" + State.logs(this) +
            "\n\nLokalny sekret kompilacji: " + (!BuildConfig.LOCAL_TEST_RETAIL_PASSWORD.isEmpty() ? "TAK (test; odtwarzalny z APK)" : "BRAK") +
            "\n\nTRYB CLEANUP: " + (CleanupFlow.blocked(this) ? "INSTALATOR ZABLOKOWANY" : "nieuruchomiony") +
            "\n\nUWAGA: w trakcie pracy hasło nie jest logowane ani zapisywane w SharedPreferences. " +
            "Hasło embargo wymaga osobnej ręcznej autoryzacji. Nie usuwa Device Owner.");
    }
}
