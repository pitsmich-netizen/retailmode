package com.example.retaildpc;

import android.accessibilityservice.AccessibilityService;
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
 * Retail DPC 0.7.2: 2 rodzaje ostrzeżenia, 3 sprawdzone wybory, lokalne hasło.
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
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!connected) return;
            if (AutomationState.active(RetailAutomationService.this)) process();
            handler.postDelayed(this, 850);
        }
    };

    @Override protected void onServiceConnected() {
        connected = true;
        AutomationState.log(this, "Usługa Accessibility 0.7.2 podłączona");
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, 500);
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        if (!TARGET.contentEquals(event.getPackageName())) return;
        if (!AutomationState.active(this)) return;
        if (SystemClock.elapsedRealtime() - lastActionAt < DEBOUNCE_MS) return;
        process();
    }
    @Override public void onInterrupt() { }
    @Override public void onDestroy() {
        connected = false;
        handler.removeCallbacks(tick);
        super.onDestroy();
    }

    private void process() {
        if (busy || !AutomationState.active(this)) return;
        busy = true;
        AccessibilityNodeInfo root = getTargetRoot();
        try {
            if (root == null || root.getPackageName() == null ||
                !TARGET.contentEquals(root.getPackageName())) return;
            int step = AutomationState.phase(this);
            if (step != previousPhase) {
                previousPhase = step;
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
            long limit = step == AutomationState.WAIT_AFTER_CONFIRM ? 12 * 60 * 1000L
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
                        AutomationState.PARTNER_OPEN, "Wybrano kraj", false);
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
                        AutomationState.STORE_OPEN, "Wybrano partnera", true);
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
                case AutomationState.PROFILE_FINAL_CONFIRM:
                    handleFinalProfileConfirm(root);
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
            if (fields.size() != 1) {
                AutomationState.stop(this,
                    "Nie potrafię bezpiecznie rozpoznać JEDNEGO pola hasła (pola=" + fields.size() +
                    "). Potrzebna diagnostyka bez wartości haseł.");
                return;
            }
            AccessibilityNodeInfo field = fields.get(0);
            String password = SecretMemory.takeForInput();
            if (password == null || password.isEmpty()) {
                AutomationState.stop(this, "Brak hasła w pamięci — uruchom ponownie z lokalnym hasłem");
                return;
            }
            Bundle b = new Bundle();
            b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, password);
            boolean ok = field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b);
            b.clear();
            password = null; // only in-memory transient string; never store or log
            if (!ok) {
                AutomationState.stop(this, "Pole hasła nie zaakceptowało ACTION_SET_TEXT. Stop bez CONFIRM.");
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
        if (containsTextPart(root, "Experience", 0)) {
            AutomationState.move(this, AutomationState.PROFILE, "Wykryto wybór profilu Experience");
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
     * Zasada: profil zawierający model + Experience, a jeśli go brak, dokładnie
     * jeden zwykły Experience (bez B2B/SmartThings). Wiele pasujących = STOP.
     * Bez współrzędnych statycznych; kliknięcie klikalnego wiersza/rodzica.
     */
    private void selectProfile(AccessibilityNodeInfo root) {
        java.util.ArrayList<AccessibilityNodeInfo> modelMatches = new java.util.ArrayList<>();
        java.util.ArrayList<AccessibilityNodeInfo> genericMatches = new java.util.ArrayList<>();
        String model = android.os.Build.MODEL == null ? "" : android.os.Build.MODEL.toLowerCase(Locale.ROOT);
        findExperienceProfiles(root, model, modelMatches, genericMatches, 0);
        java.util.ArrayList<AccessibilityNodeInfo> candidate =
            !modelMatches.isEmpty() ? modelMatches : genericMatches;
        if (candidate.isEmpty()) {
            // Nie kończ automatu zanim okno/profil doładują się przez sieć.
            logOccasionally("Czekam na listę profili Experience; brak jednoznacznego elementu");
            return;
        }
        if (candidate.size() != 1) {
            AutomationState.stop(this, "Niejednoznaczne profile Experience: " + candidate.size() +
                ". Wybierz ręcznie. Nie klikam pierwszego wyniku.");
            return;
        }
        AccessibilityNodeInfo item = candidate.get(0);
        String name = item.getText() == null ? "Experience" : item.getText().toString();
        AccessibilityNodeInfo clickTarget = findClickableAncestor(item);
        if (clickTarget != null && (performClick(clickTarget) || tapCenter(clickTarget))) {
            AutomationState.profileChosen(this, name);
        } else logOccasionally("Profil znaleziony, ale element nie przyjmuje kliknięcia");
    }

    /**
     * Ostatnie zatwierdzenie po wybraniu profilu Experience. Nie jest to ten sam
     * CONFIRM, ktory zatwierdza kraj/partnera/sklep/haslo w activation_fragment.
     * Klikamy tylko jedna, rozpoznana kontrolke i tylko RAZ na sesje.
     */
    private void handleFinalProfileConfirm(AccessibilityNodeInfo root) {
        if (!AutomationState.fullMode(this)) {
            AutomationState.stop(this, "Konczace CONFIRM jest niedostepne w trybie testowym");
            return;
        }
        // Po wyborze profilu wcisniecie CONFIRM moze aktywowac demo i/lub zmienic
        // zasady urzadzenia. DPC ma juz zatwierdzony tryb pelny przez uzytkownika.
        if (System.currentTimeMillis() - AutomationState.phaseTime(this) < 1500L) return;
        if (containsTextPart(root, "embargo", 0) || containsTextPart(root, "pre-release", 0) ||
            containsTextPart(root, "secondary password", 0)) {
            AutomationState.pauseForAuthorization(this, "Po profilu wymagane dodatkowe haslo / embargo");
            return;
        }
        // Poczekaj, az zniknie popup wyboru profilu — w nim moze byc widoczny
        // CONFIRM z ekranu lezacego POD popupem.
        if (containsTextExact(root, "SELECT PROFILE", 0)) {
            logOccasionally("Profil wybrany; czekam na zamkniecie okna SELECT PROFILE");
            return;
        }
        // Gdy formularz aktywacji nadal jest widoczny, nie klikaj ponownie
        // wczesniejszego btnConfirm: moglby wywolac druga probe aktywacji.
        if (firstById(root, ROOT_ID + "activation_fragment") != null) {
            logOccasionally("Nie klikam starego CONFIRM z formularza aktywacji");
            return;
        }
        AccessibilityNodeInfo confirm = buttonWithText(root, "CONFIRM", 0);
        if (confirm == null) {
            logOccasionally("Czekam na koncowy przycisk CONFIRM po wyborze profilu");
            return;
        }
        if (performClick(confirm) || tapCenter(confirm)) {
            // Zatrzymujemy automat natychmiast po przyjeciu akcji, zeby nie
            // wyslac drugiego CONFIRM nawet przy opoznionej odpowiedzi Retail.
            AutomationState.finalConfirmDispatched(this);
        } else {
            logOccasionally("Koncowy CONFIRM widoczny, ale klikniecie nieudane");
        }
    }

    private void findExperienceProfiles(AccessibilityNodeInfo node, String model,
            java.util.ArrayList<AccessibilityNodeInfo> modelMatches,
            java.util.ArrayList<AccessibilityNodeInfo> genericMatches, int depth) {
        if (node == null || depth > 25) return;
        CharSequence text = node.getText();
        if (node.isEnabled() && text != null) {
            String label = text.toString().trim();
            String low = label.toLowerCase(Locale.ROOT);
            if (low.contains("experience") && !low.contains("b2b") &&
                !low.contains("smartthings") && findClickableAncestor(node) != null) {
                // Docelowy S26 — model systemowy często ma kod SM-..., dlatego używamy
                // członu S26 jawnie zidentyfikowanego podczas testów.
                if (low.contains("galaxy s26") || (!model.isEmpty() && low.contains(model)))
                    modelMatches.add(node);
                else genericMatches.add(node);
            }
        }
        for (int i=0; i<node.getChildCount(); i++)
            findExperienceProfiles(node.getChild(i), model, modelMatches, genericMatches, depth + 1);
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
     * Kraj: mozna wybrac trafienie z widocznej listy.
     * Partner: ZAWSZE najpierw filtruj SearchView, nawet jesli partner jest na poczatku
     * nieprzefiltrowanej listy (np. AUCHAN). Dla obu: zatwierdz Szukaj/Gotowe, jesli
     * wynik nie zostal jeszcze zwrocony; kliknij tylko pozycje o dokladnej nazwie.
     */
    private void choose(AccessibilityNodeInfo root, String exactText, String query,
                        int nextStep, String success, boolean requireSearch) {
        // Sprawdzaj elementy dopiero po wyslaniu zapytania, gdy wymaga tego etap partnera.
        if (!requireSearch || querySent) {
            AccessibilityNodeInfo item = findExactRow(root, exactText);
            if (item != null) {
                if (requireSearch) {
                    AccessibilityNodeInfo input = findEdit(root, 0);
                    if (input == null || !hasExpectedQuery(input, query)) {
                        logOccasionally("Nie potwierdzono filtra partnera; czekam na pole wyszukiwania");
                        return;
                    }
                }
                if (performClick(item) || tapCenter(item)) {
                    AutomationState.move(this, nextStep, success + ": " + exactText);
                } else {
                    logOccasionally("Widze pozycje '" + exactText + "', ale nie moge jej kliknac");
                }
                return;
            }
        }
        if (!querySent) {
            if (setQuery(root, query)) {
                querySent = true;
                querySetAt = SystemClock.elapsedRealtime();
                AutomationState.log(this, "Filtr SearchView: '" + query + "'; oczekiwany wynik '" + exactText + "'");
            } else {
                focusSearch(root);
                logOccasionally("Pole wyszukiwania nie jest jeszcze gotowe: " + exactText);
            }
            return;
        }
        submitIfNeeded(root, query, "wybor " + exactText);
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
