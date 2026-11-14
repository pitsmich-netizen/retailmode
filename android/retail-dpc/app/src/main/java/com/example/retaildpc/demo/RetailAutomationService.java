package com.example.retaildpc.demo;

import android.accessibilityservice.AccessibilityService;
import com.example.retaildpc.PostDemoAppRestore;
import com.example.retaildpc.GooglePlayVersionAudit;
import com.example.retaildpc.AuditSnapshotStore;
import android.accessibilityservice.InputMethod;
import android.os.Build;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;
import java.util.Locale;

/**
 * Retail Setup 0.11.0: logika 0.7.1, wybór Experience, potem osobny CONFIRM.
 * Tryb pełny wymaga świadomej zgody. Nie obsługuje obchodzenia blokady embargo.
 * Działa wyłącznie na określonym pakiecie i tylko po włączeniu przez użytkownika.
 */
public final class RetailAutomationService extends AccessibilityService {
    private static final String TARGET = "com.samsung.rms.retailagent.global";
    private static final String ROOT_ID = TARGET + ":id/";
    private static final long TIMEOUT_MS = 120000L;
    private static final long DEBOUNCE_MS = 550L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastActionAt = 0L;
    private boolean storeSearchFocused = false;
    private boolean querySent = false;
    private boolean imeSubmitted = false;
    private boolean enterFallbackSent = false;
    private long imeAttemptAt = 0L;
    private long querySetAt = 0L;
    private long lastWaitingLogAt = 0L;
    private int previousPhase = -1;
    private boolean passwordFilled = false;
    private long passwordSetAt = 0L;
    private boolean connected = false;
    private boolean busy = false;
    private static RetailAutomationService liveInstance;
    private final DemoCompletionWatch completionWatch = new DemoCompletionWatch();
    private final StatusToast statusToast = new StatusToast();
    private long lastProfileConfirmAt = 0L;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!connected) return;
            if (AutomationState.active(RetailAutomationService.this)) process();
            handler.postDelayed(this, 850);
        }
    };

    @Override protected void onServiceConnected() {
        connected = true;
        liveInstance = this;
        AutomationState.log(this, "Usługa Accessibility 0.2.14 podłączona");
        handler.removeCallbacks(tick);
        // User explicitly pressed START before granting Accessibility. Do not
        // silently start registration merely because the service was enabled.
        if (PendingStart.beginIfReady(this)) launchRetailFromService();
        handler.postDelayed(tick, 500);
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        if (!TARGET.contentEquals(event.getPackageName())) return;
        if (!AutomationState.active(this)) return;
        // A short-lived Retail Mode downloading toast can disappear before the
        // next 850ms poll. Inspect event text BEFORE the UI action debounce.
        if (AutomationState.phase(this) == AutomationState.WAIT_PROFILE_CONFIRM_RESULT) {
            StringBuilder eventLabel = new StringBuilder();
            if (event.getText() != null) {
                for (CharSequence t : event.getText()) {
                    if (t != null) eventLabel.append(t).append(' ');
                }
            }
            if (event.getContentDescription() != null)
                eventLabel.append(event.getContentDescription());
            if (completionWatch.update(profileConfirmedAt(), TARGET, eventLabel.toString())) {
                finishOnDownloadStart();
                return;
            }
        }
        if (SystemClock.elapsedRealtime() - lastActionAt < DEBOUNCE_MS) return;
        process();
    }
    @Override public void onInterrupt() { }
    @Override public void onDestroy() {
        connected = false;
        if (liveInstance == this) liveInstance = null;
        handler.removeCallbacks(tick);
        super.onDestroy();
    }

    private void process() {
        if (busy || !AutomationState.active(this)) return;
        busy = true;
        // Stop only on a matching download-start message from Retail Mode.
        // A timer or Samsung launcher by itself is not evidence of success.
        if (AutomationState.phase(this) == AutomationState.WAIT_PROFILE_CONFIRM_RESULT) {
            checkDemoCompletion();
            if (!AutomationState.active(this)) { busy = false; return; }
        }
        AccessibilityNodeInfo root = getTargetRoot();
        try {
            if (root == null || root.getPackageName() == null ||
                !TARGET.contentEquals(root.getPackageName())) return;
            // Check BEFORE any click: do not re-register an already active demo.
            int observed=RegistrationStatusDetector.detect(root);
            if(observed!=RegistrationStatusDetector.UNKNOWN){
                finishAlreadyConfigured(observed);
                return;
            }
            int step = AutomationState.phase(this);
            if (step != previousPhase) {
                previousPhase = step;
                statusToast.phase(this, step);
                storeSearchFocused = false;
                querySent = false;
                imeSubmitted = false;
                enterFallbackSent = false;
                imeAttemptAt = 0L;
                querySetAt = 0L;
                lastWaitingLogAt = 0L;
                passwordFilled = false;
                passwordSetAt = 0L;
            }
            long limit = (step == AutomationState.WAIT_AFTER_CONFIRM ||
                step == AutomationState.WAIT_MANUAL_PASSWORD ||
                step == AutomationState.WAIT_PROFILE_CONFIRM_RESULT) ? 30 * 60 * 1000L
                : step == AutomationState.STORE_SEARCH ? 5 * 60 * 1000L : TIMEOUT_MS;
            if (System.currentTimeMillis() - AutomationState.phaseTime(this) > limit) {
                AutomationState.stop(this, "Timeout etapu=" + step + ". Sprawdź ekran Retail Mode; nie wysyłam CONFIRM ponownie.");
                return;
            }
            switch (step) {
                case AutomationState.ENTRY:
                    // Only acknowledge a popup belonging to Retail Mode. NEVER click CANCEL.
                    if (handleStartupPopup(root)) break;
                    if (firstById(root, ROOT_ID + "layoutSubsidary") != null) {
                        AutomationState.move(this, AutomationState.COUNTRY_OPEN,
                            "Wykryto formularz po PROCEED/CLOSE");
                    } else logOccasionally("Oczekiwanie na formularz / ewentualną aktywację administratora");
                    break;
                case AutomationState.COUNTRY_OPEN:
                    if (isDialog(root, "Select country")) {
                        AutomationState.move(this, AutomationState.COUNTRY_CHOOSE, "Lista krajów otwarta");
                        break;
                    }
                    if (clickById(root, ROOT_ID + "layoutSubsidary"))
                        AutomationState.move(this, AutomationState.COUNTRY_CHOOSE, "Otwieram listę krajów");
                    break;
                case AutomationState.COUNTRY_CHOOSE:
                    if (!isDialog(root, "Select country")) break;
                    choose(root, AutomationState.country(this), "pol",
                        AutomationState.PARTNER_OPEN, "Wybrano kraj");
                    break;
                case AutomationState.PARTNER_OPEN:
                    if (isDialog(root, "Retailer/Carrier channel")) {
                        AutomationState.move(this, AutomationState.PARTNER_CHOOSE, "Lista partnerów otwarta");
                        break;
                    }
                    if (clickById(root, ROOT_ID + "layoutChannel"))
                        AutomationState.move(this, AutomationState.PARTNER_CHOOSE, "Otwieram listę partnerów");
                    break;
                case AutomationState.PARTNER_CHOOSE:
                    if (!isDialog(root, "Retailer/Carrier channel")) break;
                    choose(root, AutomationState.partner(this), AutomationState.partner(this),
                        AutomationState.STORE_OPEN, "Wybrano partnera");
                    break;
                case AutomationState.STORE_OPEN:
                    if (isDialog(root, "Retailer/Carrier store")) {
                        AutomationState.move(this, AutomationState.STORE_SEARCH, "Otwarta wyszukiwarka sklepów");
                        break;
                    }
                    if (clickById(root, ROOT_ID + "layoutStore"))
                        AutomationState.move(this, AutomationState.STORE_SEARCH, "Otwieram wybór sklepu");
                    break;
                case AutomationState.STORE_SEARCH:
                    if (!isDialog(root, "Retailer/Carrier store")) break;
                    searchAndChooseStore(root);
                    break;
                case AutomationState.PASSWORD:
                    fillPasswordAndConfirm(root);
                    break;
                case AutomationState.WAIT_AFTER_CONFIRM:
                    handleAfterConfirm(root);
                    break;
                case AutomationState.PROFILE:
                    selectProfile(root);
                    break;
                case AutomationState.PROFILE_CONFIRM:
                    confirmSelectedProfile(root);
                    break;
                case AutomationState.WAIT_PROFILE_CONFIRM_RESULT:
                    // Never re-send CONFIRM. Wait for an explicit download message.
                    logOccasionally("CONFIRM wysłany; czekam na komunikat pobierania Retail Mode. Bez niego Dostępność pozostanie włączona.");
                    break;
                case AutomationState.WAIT_MANUAL_PASSWORD:
                    // User enters and confirms the password screen manually.
                    if (containsTextExact(root, "Select Profile", 0)) {
                        AutomationState.move(this, AutomationState.PROFILE,
                            "Otwarta lista Select Profile; dopasowuję profil do modelu");
                    } else {
                        logOccasionally("Czekam na ręczne wpisanie hasła i Select Profile");
                    }
                    break;
                default: break;
            }
        } catch (RuntimeException e) {
            AutomationState.stop(this, "Błąd Accessibility: " + e.getClass().getSimpleName());
        } finally {
            if (root != null) root.recycle();
            busy = false;
        }
    }


    /** Explicit user fallback for Samsung demos without a recognized launcher transition. */
    static void disableManually(android.content.Context c) {
        RetailAutomationService instance = liveInstance;
        if (instance != null) {
            AutomationState.stop(c, "Użytkownik ręcznie zakończył automatyzację i wyłączył Dostępność");
            instance.disableSelf();
        }
    }

    private void launchRetailFromService() {
        android.content.Intent i = getPackageManager().getLaunchIntentForPackage(TARGET);
        if (i == null) {
            AutomationState.stop(this, "Retail Mode nie jest zainstalowany");
            return;
        }
        i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
            AutomationState.log(this, "START po świadomym włączeniu Dostępności — uruchamiam Retail Mode");
        } catch (RuntimeException e) {
            AutomationState.stop(this, "Nie można automatycznie otworzyć Retail Mode. Otwórz Helper ręcznie.");
        }
    }

    /** Exact confirmation moment, falling back to the saved phase timestamp. */
    private long profileConfirmedAt() {
        return lastProfileConfirmAt > 0L ? lastProfileConfirmAt : AutomationState.phaseTime(this);
    }

    /** Only Retail Mode foreground UI can count as download acknowledgement. */
    private void checkDemoCompletion() {
        AccessibilityNodeInfo activeRoot = getRootInActiveWindow();
        try {
            if (activeRoot == null || activeRoot.getPackageName() == null) return;
            if (!TARGET.contentEquals(activeRoot.getPackageName())) return;
            if (hasDownloadMessage(activeRoot, 0)) finishOnDownloadStart();
        } finally { if (activeRoot != null) activeRoot.recycle(); }
    }

    private boolean hasDownloadMessage(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 24 || !node.isVisibleToUser()) return false;
        String text = node.getText() == null ? "" : node.getText().toString();
        String desc = node.getContentDescription() == null ? "" : node.getContentDescription().toString();
        if (completionWatch.update(profileConfirmedAt(), TARGET, text) ||
            completionWatch.update(profileConfirmedAt(), TARGET, desc)) return true;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                try { if (hasDownloadMessage(child, depth + 1)) return true; }
                finally { child.recycle(); }
            }
        }
        return false;
    }

    private void finishAlreadyConfigured(int observed) {
        if(!AutomationState.active(this))return;
        statusToast.cancel();
        boolean downloading=observed==RegistrationStatusDetector.DOWNLOADING;
        AutomationState.stop(this,downloading
            ? "Retail Mode potwierdza pobieranie demo; nie ponawiam rejestracji."
            : "Retail Mode potwierdza wcześniejszą rejestrację; pomijam formularz.");
        CompletionHider.markAlreadyConfigured(this,downloading);
        PostDemoAppRestore.onDemoDownloadStart(this);
        GooglePlayVersionAudit.demoStarted(this);
        AuditSnapshotStore.save(this,"demo_pobieranie_potwierdzone");
        try{disableSelf();}
        finally{CompletionHider.hideLauncherIfComplete(this);}
    }

    private void finishOnDownloadStart() {
        statusToast.cancel();
        if (AutomationState.phase(this) != AutomationState.WAIT_PROFILE_CONFIRM_RESULT ||
            !AutomationState.active(this)) return;
        AutomationState.stop(this,
            "Po CONFIRM Retail Mode wyświetlił komunikat o rozpoczęciu pobierania. Wyłączam Dostępność.");
        CompletionHider.markDownloadAcknowledged(this);
        PostDemoAppRestore.onDemoDownloadStart(this);
        GooglePlayVersionAudit.demoStarted(this);
        AuditSnapshotStore.save(this,"demo_pobieranie_potwierdzone");
        StatusToast.completion(this);
        // Disable Accessibility before hiding the launcher alias. Hide only the icon,
        // not MainActivity or the application; never ask to uninstall Helper.
        try {
            disableSelf();
        } finally {
            CompletionHider.hideLauncherIfComplete(this);
        }
    }

    /**
     * First use: PROCEED/CANCEL; later: informational WARNING with CLOSE.
     * The presence of a main Retail Mode form wins over popup heuristics.
     */
    private boolean handleStartupPopup(AccessibilityNodeInfo root) {
        if (firstById(root, ROOT_ID + "layoutSubsidary") != null) return false;
        AccessibilityNodeInfo proceed = buttonWithText(root, "PROCEED", 0);
        if (proceed != null) {
            if (performClick(proceed) || tapCenter(proceed))
                AutomationState.log(this, "Okno pierwszego uruchomienia: PROCEED");
            return true;
        }
        // CLOSE is only accepted if dialog is an informational warning in the target package.
        boolean warning = containsTextExact(root, "WARNING!", 0);
        AccessibilityNodeInfo close = buttonWithText(root, "CLOSE", 0);
        if (warning && close != null) {
            if (performClick(close) || tapCenter(close))
                AutomationState.log(this, "Informacja po aktywacji administratora: CLOSE");
            return true;
        }
        return false;
    }

    private AccessibilityNodeInfo buttonWithText(AccessibilityNodeInfo node, String expected, int depth) {
        if (node == null || depth > 25) return null;
        String name = node.getClassName() == null ? "" : node.getClassName().toString();
        String text = node.getText() == null ? "" : node.getText().toString().trim();
        if (node.isClickable() && node.isEnabled() && name.endsWith("Button") &&
            text.equalsIgnoreCase(expected)) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = buttonWithText(node.getChild(i), expected, depth + 1);
            if (child != null) return child;
        }
        return null;
    }
    private boolean containsTextExact(AccessibilityNodeInfo node, String expected, int depth) {
        if (node == null || depth > 25) return false;
        if (node.getText() != null && expected.equalsIgnoreCase(node.getText().toString().trim())) return true;
        for (int i = 0; i < node.getChildCount(); i++)
            if (containsTextExact(node.getChild(i), expected, depth + 1)) return true;
        return false;
    }

    /** Password input identification is conservative. Never take coordinates as fallback. */
    private void fillPasswordAndConfirm(AccessibilityNodeInfo root) {
        if (!AutomationState.fullMode(this)) {
            AutomationState.stop(this, "Tryb testowy nie może zatwierdzać Retail Mode");
            return;
        }
        if (firstById(root, ROOT_ID + "activation_fragment") == null ||
            firstById(root, ROOT_ID + "layoutStore") == null ||
            firstById(root, ROOT_ID + "btnConfirm") == null) {
            logOccasionally("Czekam na właściwy ekran hasła Retail Mode");
            return;
        }
        if (!passwordFilled) {
            java.util.ArrayList<AccessibilityNodeInfo> fields = new java.util.ArrayList<>();
            findEditableNodes(root, fields, 0);
            // Prefer explicitly password-marked nodes; no blind filling of unrelated inputs.
            java.util.ArrayList<AccessibilityNodeInfo> passwordFields = new java.util.ArrayList<>();
            for (AccessibilityNodeInfo f : fields) if (f.isPassword()) passwordFields.add(f);
            if (passwordFields.size() != 1) {
                // If the UI doesn't expose a unique password field, safely fall back to manual entry.
                SecretMemory.clear();
                AutomationState.move(this, AutomationState.WAIT_MANUAL_PASSWORD,
                    "Nie udało się jednoznacznie wskazać pola hasła. Wpisz i zatwierdź hasło ręcznie; wybór profilu pozostaje automatyczny.");
                return;
            }
            AccessibilityNodeInfo field = passwordFields.get(0);
            String password = SecretMemory.takeForInput();
            if (password == null || password.isEmpty()) {
                AutomationState.move(this, AutomationState.WAIT_MANUAL_PASSWORD,
                    "Hasło nie jest już dostępne w pamięci. Wprowadź je ręcznie.");
                return;
            }
            Bundle b = new Bundle();
            b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, password);
            boolean ok = field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b);
            b.clear();
            password = null; // only in-memory transient string; never store or log
            if (!ok) {
                AutomationState.move(this, AutomationState.WAIT_MANUAL_PASSWORD,
                    "Automatyczne wprowadzenie hasła nie powiodło się. Wpisz je ręcznie.");
                return;
            }
            passwordFilled = true;
            passwordSetAt = SystemClock.elapsedRealtime();
            lastActionAt = passwordSetAt;
            AutomationState.log(this, "Hasło wpisane lokalnie (wartość ukryta), czekam przed CONFIRM");
            return;
        }
        if (SystemClock.elapsedRealtime() - passwordSetAt < 900) return;
        if (clickById(root, ROOT_ID + "btnConfirm")) {
            AutomationState.move(this, AutomationState.WAIT_AFTER_CONFIRM,
                "CONFIRM wykonano; obserwuję wynik i zabezpieczenie embargo");
        } else {
            logOccasionally("Nie można kliknąć CONFIRM — nie używam współrzędnych awaryjnych");
        }
    }
    private void findEditableNodes(AccessibilityNodeInfo node,
                                   java.util.ArrayList<AccessibilityNodeInfo> out, int depth) {
        if (node == null || depth > 25) return;
        String name = node.getClassName() == null ? "" : node.getClassName().toString();
        String id = node.getViewIdResourceName() == null ? "" : node.getViewIdResourceName();
        if (node.isEnabled() && (node.isEditable() || name.endsWith("EditText")) &&
            !id.endsWith("search_src_text") && !id.endsWith("searchView")) out.add(node);
        for (int i = 0; i < node.getChildCount(); i++)
            findEditableNodes(node.getChild(i), out, depth + 1);
    }
    private boolean hasAnyEditable(AccessibilityNodeInfo root) {
        java.util.ArrayList<AccessibilityNodeInfo> inputs = new java.util.ArrayList<>();
        findEditableNodes(root, inputs, 0);
        return !inputs.isEmpty();
    }
    private boolean containsTextPart(AccessibilityNodeInfo node, String query, int depth) {
        if (node == null || depth > 30) return false;
        CharSequence value = node.getText();
        if (value != null && value.toString().toLowerCase(Locale.ROOT)
                .contains(query.toLowerCase(Locale.ROOT))) return true;
        for (int i = 0; i < node.getChildCount(); i++)
            if (containsTextPart(node.getChild(i), query, depth+1)) return true;
        return false;
    }
    private void handleAfterConfirm(AccessibilityNodeInfo root) {
        if (containsTextPart(root, "embargo", 0) || containsTextPart(root, "pre-release", 0) ||
            containsTextPart(root, "secondary password", 0)) {
            AutomationState.pauseForAuthorization(this, "Wykryto embargo: wymagane ręczne poświadczenie");
            return;
        }
        if (containsTextExact(root, "Select Profile", 0)) {
            AutomationState.move(this, AutomationState.PROFILE, "Wykryto okno Select Profile");
            return;
        }
        String status = textById(root, ROOT_ID + "txtStatus");
        if (!status.isEmpty()) {
            logOccasionally("Retail Mode pracuje: " + status);
            return;
        }
        if (firstById(root, ROOT_ID + "activation_fragment") == null && hasAnyEditable(root)) {
            AutomationState.pauseForAuthorization(this,
                "Dodatkowe okno autoryzacji / hasła: przejmij je ręcznie. Automatyzacja wstrzymana.");
            return;
        }
        logOccasionally("Oczekuję na ekran profili / wynik CONFIRM (bez ponawiania)");
    }
    /**
     * Select Profile: choose ONLY one exact retail marketing-name match.
     * No generic fallback, no Galaxy S26 hardcode, no Build.MODEL comparison.
     * A tap selects the profile; a separate CONFIRM is required afterward.
     */
    private void selectProfile(AccessibilityNodeInfo root) {
        if (!containsTextExact(root, "Select Profile", 0)) {
            logOccasionally("Czekam na okno Select Profile (bez zgadywania ekranu)");
            return;
        }
        String model = AutomationState.profileModel(this);
        if (model.isEmpty()) {
            AutomationState.stop(this,
                "Nie wykryto nazwy handlowej telefonu. Ustaw S26 / A57 / S10 w Helperze i wznów wybór profilu.");
            return;
        }
        java.util.ArrayList<AccessibilityNodeInfo> matches = new java.util.ArrayList<>();
        findMatchingProfiles(root, model, matches, 0);
        if (matches.isEmpty()) {
            logOccasionally("Brak profilu '" + model + " Experience' na aktualnej liście; czekam na załadowanie");
            return;
        }
        if (matches.size() != 1) {
            AutomationState.stop(this, "Wiele pasujących pozycji dla '" + model +
                " Experience' (" + matches.size() + "). Nie wybieram losowo.");
            return;
        }
        AccessibilityNodeInfo item = matches.get(0);
        String name = item.getText() == null ? "Experience" : item.getText().toString().trim();
        AccessibilityNodeInfo clickTarget = findClickableAncestor(item);
        if (clickTarget != null && (performClick(clickTarget) || tapCenter(clickTarget))) {
            AutomationState.profileSelected(this, name);
        } else logOccasionally("Profil pasuje, ale wiersz nie jest klikalny");
    }

    /**
     * Profile confirmation, not password confirmation. Runs only after we ourselves
     * selected the exact model-specific Experience profile during this run.
     * Never use coordinates here, and NEVER retry after a successful click.
     */
    private void confirmSelectedProfile(AccessibilityNodeInfo root) {
        if (AutomationState.selectedProfile(this).isEmpty()) {
            AutomationState.stop(this, "Brak wybranego profilu — nie potwierdzam niczego w ciemno.");
            return;
        }
        // Give Retail Mode time to close the list and render its confirmation view.
        if (System.currentTimeMillis() - AutomationState.phaseTime(this) < 1000L) return;
        if (isDialog(root, "Select Profile")) {
            logOccasionally("Lista Select Profile wciąż otwarta; czekam na ekran potwierdzenia");
            return;
        }
        // An editable field may mean that we are back on a password/search screen.
        if (hasAnyEditable(root)) {
            logOccasionally("Na ekranie nadal jest pole edycyjne; nie klikam CONFIRM");
            return;
        }
        AccessibilityNodeInfo confirm = buttonWithText(root, "CONFIRM", 0);
        if (confirm == null) {
            AccessibilityNodeInfo byId = firstById(root, ROOT_ID + "btnConfirm");
            // Only accept an ID match when its *visible label* is also CONFIRM.
            if (byId != null && byId.isVisibleToUser() && byId.isEnabled() &&
                ("CONFIRM".equalsIgnoreCase(byId.getText() == null ? "" : byId.getText().toString().trim()) ||
                 "CONFIRM".equalsIgnoreCase(byId.getContentDescription() == null ? "" : byId.getContentDescription().toString().trim()))) {
                confirm = byId;
            }
        }
        if (confirm == null) {
            logOccasionally("Po wyborze profilu czekam na widoczny przycisk CONFIRM");
            return;
        }
        if (!confirm.isVisibleToUser() || !confirm.isEnabled()) return;
        if (performClick(confirm)) {
            lastProfileConfirmAt = System.currentTimeMillis();
            completionWatch.reset();
            AutomationState.profileConfirmSent(this);
        } else {
            logOccasionally("Widać CONFIRM profilu, ale akcja kliknięcia nie powiodła się");
        }
    }

    private void findMatchingProfiles(AccessibilityNodeInfo node, String model,
            java.util.ArrayList<AccessibilityNodeInfo> matches, int depth) {
        if (node == null || depth > 25) return;
        if (node.isEnabled() && node.isVisibleToUser() && node.getText() != null) {
            String label = node.getText().toString().trim();
            if (ProfileMatcher.isExactExperience(label, model) &&
                findClickableAncestor(node) != null) matches.add(node);
        }
        for (int i = 0; i < node.getChildCount(); i++)
            findMatchingProfiles(node.getChild(i), model, matches, depth+1);
    }

    private AccessibilityNodeInfo findClickableAncestor(AccessibilityNodeInfo item) {
        AccessibilityNodeInfo cursor = item;
        for (int i = 0; i < 3 && cursor != null; i++) {
            if (cursor.isClickable() && cursor.isEnabled()) return cursor;
            cursor = cursor.getParent();
        }
        return null;
    }

    /**
     * Identyczny mechanizm we wszystkich trzech oknach:
     * - wykonaj filtr przez SearchView,
     * - po 1.2 s wyślij Szukaj/Gotowe, jeśli wynik nie jest jeszcze widoczny,
     * - kliknij WYŁĄCZNIE dokładną pozycję (kraj, partner lub Site ID),
     * - po maksymalnie dwóch próbach zostaw czytelny log / timeout.
     */
    private void choose(AccessibilityNodeInfo root, String exactText, String query,
                        int nextStep, String success) {
        AccessibilityNodeInfo item = findExactRow(root, exactText);
        if (item != null) {
            if (performClick(item) || tapCenter(item)) {
                AutomationState.move(this, nextStep, success + ": " + exactText);
            } else {
                logOccasionally("Widzę pozycję '" + exactText + "', ale nie mogę jej kliknąć");
            }
            return;
        }
        if (!querySent) {
            if (setQuery(root, query)) {
                querySent = true;
                querySetAt = SystemClock.elapsedRealtime();
                AutomationState.log(this, "Wpisano '" + query + "'; szukam pozycji '" + exactText + "'");
            } else {
                focusSearch(root);
                logOccasionally("Pole wyszukiwania nie jest jeszcze gotowe: " + exactText);
            }
            return;
        }
        submitIfNeeded(root, query, "wybór " + exactText);
    }

    private void searchAndChooseStore(AccessibilityNodeInfo root) {
        String site = AutomationState.site(this);
        String status = textById(root, ROOT_ID + "statusText");
        String upper = status.toUpperCase(Locale.ROOT);
        // Smart Wait: wyszukiwarka przetwarza zapytanie, nie próbuj ponawiać w trakcie.
        if (upper.contains("SEARCHING")) {
            logOccasionally("Smart Wait: Retail Mode szuka sklepu " + site);
            return;
        }
        boolean ready = upper.contains("STORE FOUND") && upper.contains(site.toUpperCase(Locale.ROOT));
        if (ready) {
            AccessibilityNodeInfo list = firstById(root, ROOT_ID + "lstView");
            if (list != null) {
                AccessibilityNodeInfo match = findStore(list, site);
                if (match != null && (performClick(match) || tapCenter(match))) {
                    AutomationState.done(this);
                    return;
                }
            }
            logOccasionally("Wynik wskazuje na sklep " + site + ", ale brak dokładnego wiersza do kliknięcia");
            return;
        }
        if (upper.contains("NO STORE") || upper.contains("0 STORE") || upper.contains("ERROR")) {
            AutomationState.stop(this, "Wyszukiwanie zakończone bez sklepu " + site + ": " + status);
            return;
        }
        if (!querySent) {
            if (setQuery(root, site)) {
                querySent = true;
                querySetAt = SystemClock.elapsedRealtime();
                AutomationState.log(this, "Wpisano Site ID '" + site + "'. Czekam na wynik / wyślę Szukaj");
            } else {
                focusSearch(root);
                logOccasionally("Pole Site ID nie jest jeszcze dostępne");
            }
            return;
        }
        // Trzeci krok korzysta z tej samej procedury zatwierdzania co kraj i partner.
        submitIfNeeded(root, site, "Site ID " + site);
    }

    private void submitIfNeeded(AccessibilityNodeInfo root, String query, String label) {
        long now = SystemClock.elapsedRealtime();
        if (now - querySetAt < 1200L) return; // debounce na odświeżenie SearchView

        if (!imeSubmitted) {
            if (now - querySetAt < 1200L) return;
            String action = submitIme(root, query);
            if (action != null) {
                imeSubmitted = true;
                imeAttemptAt = now;
                AutomationState.log(this, "Zatwierdzono Szukaj/Gotowe [" + label + "] metodą: " + action);
            } else {
                logOccasionally("Nie mogę wysłać Szukaj/Gotowe dla " + label + "; sprawdź klawiaturę");
            }
            return;
        }
        // Jeśli akcja IME została przyjęta, ale lista nie zareagowała,
        // wyślij klasyczny ENTER maksymalnie raz, z InputConnection.
        if (!enterFallbackSent && now - imeAttemptAt > 5500L) {
            if (submitPhysicalEnter(root, query)) {
                enterFallbackSent = true;
                AutomationState.log(this, "Ponowienie: klawisz ENTER dla " + label);
            } else {
                logOccasionally("Brak reakcji na Szukaj dla " + label + "; może być konieczne ręczne potwierdzenie");
            }
        }
    }

    private void focusSearch(AccessibilityNodeInfo root) {
        AccessibilityNodeInfo search = firstById(root, ROOT_ID + "searchView");
        if (search != null) {
            if (performClick(search) || tapCenter(search)) storeSearchFocused = true;
        }
    }

    /**
     * Zatwierdzenie z klawiatury, wyłącznie gdy pole zawiera oczekiwaną wartość.
     * Gdy dostępne, ACTION_IME_ENTER wywołuje akcję pola edycyjnego.
     * Android 13+ daje też oficjalny InputConnection, który może wysłać IME_ACTION_SEARCH.
     */
    private String submitIme(AccessibilityNodeInfo root, String expectedQuery) {
        AccessibilityNodeInfo input = findEdit(root, 0);
        if (input == null || !input.isEnabled()) return null;
        if (!hasExpectedQuery(input, expectedQuery)) return null;
        if (!input.isFocused()) {
            if (input.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) {
                lastActionAt = SystemClock.elapsedRealtime();
                AutomationState.log(this, "Nadano focus polu wyszukiwania");
            }
            return null;
        }
        int actionId = AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.getId();
        for (AccessibilityNodeInfo.AccessibilityAction action : input.getActionList()) {
            if (action.getId() == actionId && input.performAction(actionId)) {
                lastActionAt = SystemClock.elapsedRealtime();
                return "Accessibility ACTION_IME_ENTER";
            }
        }
        AccessibilityNodeInfo go = firstById(root, ROOT_ID + "search_go_btn");
        if (go == null) go = firstById(root, "android:id/search_go_btn");
        if (go != null && (performClick(go) || tapCenter(go))) return "SearchView search_go_btn";

        if (Build.VERSION.SDK_INT >= 33) {
            try {
                InputMethod inputMethod = getInputMethod();
                if (inputMethod != null) {
                    InputMethod.AccessibilityInputConnection conn = inputMethod.getCurrentInputConnection();
                    if (conn != null) {
                        conn.performEditorAction(EditorInfo.IME_ACTION_SEARCH);
                        lastActionAt = SystemClock.elapsedRealtime();
                        return "InputConnection IME_ACTION_SEARCH";
                    }
                }
            } catch (RuntimeException e) {
                AutomationState.log(this, "Brak InputConnection: " + e.getClass().getSimpleName());
            }
        }
        return null;
    }

    private boolean submitPhysicalEnter(AccessibilityNodeInfo root, String expectedQuery) {
        AccessibilityNodeInfo input = findEdit(root, 0);
        if (input == null || !input.isFocused() || !hasExpectedQuery(input, expectedQuery)) return false;
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                InputMethod im = getInputMethod();
                InputMethod.AccessibilityInputConnection conn = im == null ? null : im.getCurrentInputConnection();
                if (conn != null) {
                    conn.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                    conn.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
                    lastActionAt = SystemClock.elapsedRealtime();
                    return true;
                }
            } catch (RuntimeException e) {
                AutomationState.log(this, "Nie wysłano ENTER: " + e.getClass().getSimpleName());
            }
        }
        return false;
    }

    private boolean hasExpectedQuery(AccessibilityNodeInfo input, String expected) {
        CharSequence text = input.getText();
        // Nie wysyłaj klawisza do innego pola, np. hasła.
        return text != null && expected.equalsIgnoreCase(text.toString().trim());
    }

    private void logOccasionally(String message) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastWaitingLogAt > 4500L) {
            lastWaitingLogAt = now;
            AutomationState.log(this, message);
        }
    }

    /** Wybiera okno Retail Mode, nawet gdy focus przejmie klawiatura ekranowa. */
    private AccessibilityNodeInfo getTargetRoot() {
        AccessibilityNodeInfo active = getRootInActiveWindow();
        if (active != null && active.getPackageName() != null &&
            TARGET.contentEquals(active.getPackageName())) return active;
        // Na Androidzie z aktywnym IME root okna aktywnego bywa rootem klawiatury.
        // FLAG_RETRIEVE_INTERACTIVE_WINDOWS w xml jest konieczny dla getWindows().
        List<AccessibilityWindowInfo> windows = getWindows();
        if (windows != null) {
            for (AccessibilityWindowInfo window : windows) {
                AccessibilityNodeInfo candidate = window.getRoot();
                if (candidate != null && candidate.getPackageName() != null &&
                    TARGET.contentEquals(candidate.getPackageName())) return candidate;
                if (candidate != null) candidate.recycle();
            }
        }
        if (active != null) active.recycle();
        return null;
    }

    private boolean setQuery(AccessibilityNodeInfo root, String value) {
        AccessibilityNodeInfo input = findEdit(root, 0);
        if (input == null) return false;
        try {
            if (!input.isEnabled()) return false;
            if (!input.isFocused()) input.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value);
            boolean result = input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
            if (result) lastActionAt = SystemClock.elapsedRealtime();
            return result;
        } finally {
            // node pochodzi z drzewa root; jego cykl życia jest związany z tym odczytem
        }
    }
    private AccessibilityNodeInfo findEdit(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 15) return null;
        String className = node.getClassName() == null ? "" : node.getClassName().toString();
        String id = node.getViewIdResourceName() == null ? "" : node.getViewIdResourceName();
        if (node.isEnabled() && (className.endsWith("EditText") || id.endsWith("search_src_text")))
            return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findEdit(node.getChild(i), depth + 1);
            if (found != null) return found;
        }
        return null;
    }
    private AccessibilityNodeInfo findStore(AccessibilityNodeInfo node, String site) {
        if (node == null) return null;
        CharSequence value = node.getText();
        if (value != null && node.isClickable() &&
            value.toString().toUpperCase(Locale.ROOT).contains("(" + site.toUpperCase(Locale.ROOT) + ")"))
            return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = findStore(node.getChild(i), site);
            if (child != null) return child;
        }
        return null;
    }
    private boolean isDialog(AccessibilityNodeInfo root, String header) {
        String text = textById(root, ROOT_ID + "txtHeader");
        return text.trim().equalsIgnoreCase(header);
    }
    private String textById(AccessibilityNodeInfo root, String id) {
        AccessibilityNodeInfo n = firstById(root, id);
        return n == null || n.getText() == null ? "" : n.getText().toString();
    }
    private AccessibilityNodeInfo findExactRow(AccessibilityNodeInfo root, String exact) {
        List<AccessibilityNodeInfo> list = root.findAccessibilityNodeInfosByViewId("android:id/text1");
        for (AccessibilityNodeInfo node : list) {
            if (node.isEnabled() && node.isClickable() && node.getText() != null &&
                exact.equalsIgnoreCase(node.getText().toString().trim())) return node;
        }
        return null;
    }
    private AccessibilityNodeInfo firstById(AccessibilityNodeInfo root, String id) {
        List<AccessibilityNodeInfo> found = root.findAccessibilityNodeInfosByViewId(id);
        return found.isEmpty() ? null : found.get(0);
    }
    private boolean clickById(AccessibilityNodeInfo root, String id) {
        return performClick(firstById(root, id));
    }

    /** Bounded coordinate fallback based on the accessibility node, never a fixed screen location. */
    private boolean tapCenter(AccessibilityNodeInfo node) {
        if (node == null || !node.isEnabled()) return false;
        Rect rect = new Rect();
        node.getBoundsInScreen(rect);
        if (rect.isEmpty() || rect.width() < 20 || rect.height() < 20) return false;
        if (SystemClock.elapsedRealtime() - lastActionAt < DEBOUNCE_MS) return false;
        Path path = new Path();
        path.moveTo(rect.exactCenterX(), rect.exactCenterY());
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(path, 0, 100)).build();
        boolean started = dispatchGesture(gesture, null, null);
        if (started) lastActionAt = SystemClock.elapsedRealtime();
        return started;
    }

    private boolean performClick(AccessibilityNodeInfo node) {
        if (node == null || !node.isEnabled() || !node.isClickable()) return false;
        if (SystemClock.elapsedRealtime() - lastActionAt < DEBOUNCE_MS) return false;
        boolean ok = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        if (ok) lastActionAt = SystemClock.elapsedRealtime();
        return ok;
    }
}
