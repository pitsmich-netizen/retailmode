# Retail DPC 0.9.3 — AKTUALNA WERSJA

**Zacznij od [START_093.md](START_093.md).** Dalej znajduje się dokumentacja archiwalna dotycząca wersji 0.9.2.

---

**Aktualny przewodnik: START_093.md.**

# Aktualna wersja: Retail DPC Wrapper 0.9.2 — Helper Installer LAB

**Zacznij od `START_092.md` i `VERSION.txt`.** Poprzednie sekcje poniżej dotyczą historycznych testów 0.8.2 i 0.9.1. Nowe wydanie używa publicznych zasobów GitHub Releases.

# UWAGA: test zachowania aplikacji systemowych (05.11.2026)

DPC 0.8.2 działa jako instalowany przez QR Device Owner i zwraca `EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED = true` w `ACTION_GET_PROVISIONING_MODE`. To jest punkt testowy; **nie gwarantuje**, że przejdzie pełny kreator Samsung One UI ani że Automatyczna blokada będzie widoczna.

Przed nowym testem upewnij się, że publicznie osiągalny jest **zarówno nowy APK DPC**, jak i APK Retail Mode ze stałego URL w `Constants.java`. Podpis i SHA-256 nowego DPC muszą zgadzać się z tym, co wpiszesz w generatorze. DPC jest testową wersją LAB z ręcznym zwolnieniem Device Owner.

---

# Retail DPC 0.8.2 — test przywracania aplikacji i zwolnienia Device Owner

> **TYLKO TELEFON TESTOWY.** Bazą jest działająca wersja 0.8.1/0.7.2. Zmiana Device Owner jest nieodwracalna bez ponownego provisioningu. Android zastrzega, że `clearDeviceOwnerApp()` jest przeznaczone wyłącznie do testowania i może pozostawić polityki w systemie.

## Co zawiera

- Dotychczasowy działający Wrapper: provisioning QR, instalacja Retail APK, automat Country/Partner/Site/Password/Experience.
- **A. Raport**: bieżący status Device Owner, Retail Mode, admina Retail i kilku popularnych pakietów systemowych.
- **B. Przywrócenie aplikacji systemowych**: `DevicePolicyManager.enableSystemApp(admin, ACTION_MAIN + CATEGORY_LAUNCHER)` oraz próba ponownego aktywowania wybranych aplikacji systemowych (Galeria, przeglądarka, Moje pliki, sklep Play, Zdjęcia). Dotyczy wyłącznie aplikacji obecnych w fabrycznym obrazie, nie pobiera dodatkowego oprogramowania.
- **C. Eksperymentalne zwolnienie Device Owner**: 3 pola potwierdzające + ręcznie wpisana fraza `ZWOLNIJ OWNER` + drugi dialog. Nie działa automatycznie, nie odinstalowuje Wrappera i **nie aktywuje admina Retail Mode**.
- **D. Osobna dezaktywacja administratora Wrappera** po potwierdzeniu zwolnienia.
- **E. Ustawienia aplikacji** do ewentualnej ręcznej deinstalacji, dopiero gdy telefon działa poprawnie po restarcie.
- Blokada instalatora przy `release_prepared` oraz anulowanie odroczonego zadania instalacji przed zwolnieniem Device Owner.
- Wersja `versionCode=11`, `applicationId=com.example.retaildpc`, ten sam komponent `.AdminReceiver`.

## Kompilacja / instalacja

1. Rozpakuj ZIP do osobnego folderu. Nie podmieniaj starej aplikacji na telefonie przez odinstalowanie — **zaktualizuj ją**.
2. Uruchom `PRZYGOTUJ_GRADLE.bat`, który pobiera oficjalny `gradle-wrapper.jar` z GitHub Gradle i weryfikuje SHA-256.
3. W Android Studio otwórz katalog zawierający `settings.gradle`, ustaw Gradle JDK 17 albo 21. Projekt używa Gradle 8.13, AGP 8.13.2 i SDK 36.
4. Zbuduj **Build → Generate App Bundles or APKs → Generate APKs** i uzyskaj `app/build/outputs/apk/debug/app-debug.apk`.
5. Zainstaluj jako aktualizację istniejącego Retail DPC, **na tym samym komputerze z tym samym kluczem debug** (albo na stałym kluczu release, jeśli go skonfigurowano). Jeśli wystąpi błąd zgodności podpisu, **nie odinstalowuj aktualnego DPC**.
6. Aktualizacja przez USB nie wymaga zmieniania prywatności repozytorium GitHub ani resetowania telefonu.

## Kolejność testu na istniejącym S26

1. Otwórz Retail DPC 0.8.2 i wybierz **A: stan urządzenia**. Zapisz/kopiuj raport.
2. Kliknij **B: przywracanie aplikacji systemowych**, zatwierdź. Sprawdź, czy wróciła Galeria/przeglądarka, oraz Wi-Fi. Aplikacje, których nie ma w ROM, nie powstaną z niczego.
3. **Przed zwolnieniem** upewnij się, że Retail Mode jest zainstalowany, a automatyzacja jest zatrzymana. Sprawdź również, czy jesteś gotowy na ponowny reset fabryczny. Jeśli chcesz badać scenariusz *zwolnienia DO przed kreatorem*, musisz zacząć od jeszcze nieskonfigurowanego Retail Mode; test po aktywacji na obecnym S26 sprawdza inną kolejność.
4. Opcjonalny krok wysokiego ryzyka: zaznacz trzy pola, wpisz `ZWOLNIJ OWNER`, naciśnij **C**, a następnie potwierdź drugi dialog. Przeczytaj rezultat. **Nie wykonuj tej operacji na telefonie produkcyjnym**.
5. Po zwolnieniu sprawdź komunikaty o zarządzaniu w systemie, status Retail Mode i Wi-Fi. **Uruchom ponownie telefon** i ponów te testy.
6. Jeżeli Retail Mode działa prawidłowo, a Wrapper jest już zwykłą aplikacją, możesz wyłączyć ewentualny Device Administrator Wrappera (przycisk **D**), a potem odinstalować aplikację ręcznie przez **E**. Przed usunięciem zapisz raport.

## Uwagi o dalszej automatyzacji

- Po zwolnieniu Device Owner Wrapper nie może już wykonywać cichej instalacji kolejnych APK. Usługa Accessibility może nadal działać, ale jeśli ją wyłączono lub cofnięto uprawnienia, konieczne będzie ręczne włączenie.
- Retail Mode ma komponent Device Administrator, ale wcześniejszy raport wykazał `Administrator aktywny: NIE` i `support-transfer-ownership: NIE`. Zwolnienie DO nie powoduje automatycznego przejęcia uprawnień przez Retail.
- `clearDeviceOwnerApp()` nie przywraca procesu pierwszej konfiguracji Androida ani nie zapewnia fabrycznego zestawu aplikacji.
- Przy następnym **pełnym** provisioningu skrypt `tools/generate_provisioning_qr.py` i odpowiedź `GET_PROVISIONING_MODE` ustawiają `android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED: true`. Użyj jednak QR wygenerowanego z nowego APK i jego zgodnym publicznym adresem pobrania. Ale to nie pomoże wstecz na już skonfigurowanym S26.
- W czasie starszych testów repozytorium było prywatne; później zostało ustawione jako publiczne. Dla provisioningu zawsze upewnij się, że adres pobrania APK działa bez logowania.

## Bezpieczeństwo

- `retail-test-secret.properties` **nie jest dołączony**. Możesz wpisywać hasło Retail Mode lokalnie w aplikacji. Nie umieszczaj prawdziwych haseł w publicznych plikach GitHub ani w logach.
- Nie wyłączamy Device Owner automatycznie. Nie wywołujemy `wipeData()` ani automatycznej deinstalacji.
- Nie modyfikujemy apk Samsung Retail Mode, nie podszywamy się pod administratora Retail i nie omijamy hasła embargo.

## Status

Kod projektu sprawdzony statycznie; **pełnej kompilacji Android SDK i testu na S26 nie przeprowadzono**. Jeśli wystąpi błąd, podaj pierwszą linię komunikatu Java/Gradle.


## 0.9.1 — Helper Installer LAB
Zacznij od `START_091.md`; domyślnie brak instalacji Helpera aż do podania jego zweryfikowanego URL+SHA w `helper-apk.properties`.


## 0.9.2 — Helper Installer CONFIGURED (aktualny)
Zobacz `START_092.md`. Helper jest skonfigurowany z publicznym adresem GitHub Releases i SHA-256 w `helper-apk.properties`, a wersja aplikacji to `versionCode 14`, `versionName 0.9.2-helper-installer-lab`.
