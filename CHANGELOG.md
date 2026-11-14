## 2026-11-14 — Retail Setup 0.11.19: kopie diagnostyki odporne na czyszczenie danych aplikacji

- Bez zmian w logice pobierania Retail Mode, rejestracji demo, wyłączania Accessibility i zwalniania Device Owner.
- Zachowano `StockAppRecovery.restore()` przed zwolnieniem Device Ownera.
- Pasywny raport Google Play pokazuje **bieżące** wersje 13 pakietów nawet po utracie wcześniejszego punktu pomiarowego.
- Raport sesji zawiera `UserHandle`, `Process UID`, lokalny znacznik danych aplikacji oraz rzeczywisty stan Device Owner/Admin. Porównanie różnych zrzutów pomaga odróżnić zmianę sesji od wyczyszczenia prywatnych danych. Nie daje stuprocentowej diagnozy przyczyny.
- Bez automatycznej transmisji: w trakcie przygotowania, przed START oraz po potwierdzeniu pobierania demo powstają krótkie pliki w `Pobrane/RetailSetup`. To **wyłącznie** wersje/status dozwolonych aplikacji i metadane sesji — bez QR, haseł, danych sklepu i pełnych logów.
- Dodano przycisk ręcznego zapisu oraz informację o ostatnim pliku w panelu serwisowym.
- Pliki w Pobranych mogą być niedostępne po zmianie użytkownika lub usunięte przez politykę retail/factory reset; jest to kopia pomocnicza, nie gwarantowany trwały rejestr.
- Nowe: `AuditSnapshotStore`, poprawiony `GooglePlayVersionAudit.report`.

## 2026-11-10 — Retail Setup 0.11.18: pasywna kontrola wersji Google Play

- Zachowujemy działającą instalację Retail Mode, wybór Experience, rejestrację i wykrywanie rozpoczęcia demo.
- Zachowujemy `StockAppRecovery.restore()` przed zwolnieniem Device Ownera — nie usuwamy tej funkcji.
- Jednorazowo zapisujemy wersje 13 aplikacji Google/Samsung podczas provisioningu, potem po zwolnieniu Device Ownera, po potwierdzonym starcie pobierania demo oraz automatycznie po ok. 15 i 60 minutach przez JobScheduler (bez sieci).
- W panelu serwisowym dostępne są przyciski `Pokaż raport wersji aplikacji` i `Odśwież porównanie wersji Google Play`; raport dołącza także do `Kopiuj raport`.
- Nie pobieramy aplikacji, nie otwieramy Play Store, nie potrzebujemy logowania ani EMM. Zmiana wersji nie rozstrzyga, skąd pochodzi aktualizacja.
- Z panelu usuwamy przycisk ponawiania listy zatwierdzonych APK po demo; nasz katalog tych źródeł pozostaje pusty.
- Instalacja kompletnego katalogu jak w konsumenckim Setup Wizard nadal nie jest odtwarzana.

## 2026-11-10 — Retail Setup 0.11.17: normalny Setup Wizard — porównanie bez QR

- Wersja 0.11.16 na S26 potwierdziła: OMC było ENABLED w GET_PROVISIONING_MODE i ADMIN_POLICY_COMPLIANCE, ale DISABLED po user_setup_complete. Zwolnienie Device Owner nie odwróciło tego stanu.
- Dodano **osobną** uruchamialną ikonę `Test aplikacji Samsung` (ConsumerBaselineActivity) do testów po standardowej, niezarządzanej konfiguracji bez QR.
- Po ręcznym zainstalowaniu APK z GitHuba tester widzi stan OMC, Setup Wizard, Galaxy Store oraz audyt 12 pakietów — bez żadnych operacji Device Owner, bez rejestracji retail, bez pobierania demo lub aktualizacji.
- Celem jest sprawdzenie, czy OMC po zakończeniu **zwykłej** konfiguracji też staje się DISABLED; sam OMC nie jest zweryfikowanym interfejsem do ponownego uruchamiania kolejki aplikacji.
- Dodatkowa ikona testowa jest tymczasowa i zostanie usunięta z wersji produkcyjnej. Dotychczasowe tryby QR nie zmieniają działania.

## 2026-11-10 — Retail Setup 0.11.16: minimalny Device Owner / test A-B OMC

- Przy zwykłej konfiguracji bez QR Samsung przygotowuje kolejkę Recommended apps, a Samsung Find pobiera się bez logowania; przy konfiguracji pełnego Android Enterprise brakuje 9 aplikacji.
- QR `minimal_owner_probe=true` (plus `diagnostics_no_demo=true`) aktywuje wariant testowy: minimalny Device Owner. Nie ustawia HOME, lokalizacji, powiadomień, nie pobiera Retail Mode/Experience i nie dotyka fabrycznych aplikacji.
- Raport czasu rzeczywistego rejestruje stan OMC (installed/enabled/enabledSetting), DO i flagi Setup Wizard w fazach GET_PROVISIONING_MODE, DEVICE_ADMIN_ENABLED, PROFILE_PROVISIONING_COMPLETE, ADMIN_POLICY_COMPLIANCE i MAIN_ACTIVITY_CREATED.
- Włącza tylko własną ikonę Retail Setup, aby raport był dostępny bez nadpisywania launchera.
- Pozostałe tryby i działająca automatyzacja pełnego Retail Mode pozostają bez zmian.
- Znacznik LEAVE_ALL_SYSTEM_APPS_ENABLED=true był już obecny w dotychczasowym QR; nie odpowiada za pobieranie aplikacji sklepowych.
- Wynik testu służy ustaleniu, czy wyłączenie OMC jest cechą Android Enterprise na S26, czy efektem aktywnych polityk DPC.

## 2026-11-10 — Retail Setup 0.11.15: aktywacja OMC jako osobny test

- OMC (Samsung Recommended apps) na S26 po provisioningu QR: aplikacja systemowa, zainstalowana dla user, ale enabled=false; Galaxy Store i SetupWizard enabled=true.
- Dedykowany przycisk testowy z jawnym potwierdzeniem. Tylko dla `omc_probe_only` na testowym Device Owner: próba `DevicePolicyManager.enableSystemApp(admin, com.samsung.android.app.omcagent)`, a gdy nadal disabled, publiczne `PackageManager.setApplicationEnabledSetting` (system może odmówić `SecurityException`).
- Raport obejmuje enabledSetting (odróżnienie explicit DISABLED / USER / DEFAULT), przed/po, wyjątki i liczbę testów. Dodatkowo pobieramy info o receivers i disabled components z PackageManager.
- Brak wywoływania nieudokumentowanych aktywności, force-stop, usuwania kont, ukrytych API, instalacji APK, pobierania Retail/Experience i zwolnienia Device Owner.
- Samo włączenie OMC nie gwarantuje pobrania aplikacji; potrzebny test Galaxy S26.

## 2026-11-10 — Retail Setup 0.11.14: Samsung Recommended apps / OMC diagnostics

- Added opt-in QR flag `omc_probe_only=true`: diagnostics without Retail Mode, Experience, any APK download, or retries of `enableSystemApp` (known unsuccessful on test S26).
- Read-only report: Samsung Recommended apps (`com.samsung.android.app.omcagent`), Galaxy Store, Samsung/Google Setup Wizard, per-user install/enabled/system state, component counts, version, and setup-complete flags.
- Existing full Retail Mode QR behavior unchanged. Existing restore diagnostic QR remains supported when `omc_probe_only` is absent.
- Source of OMC package mapping is community device package inventory, not a documented OEM installation API. Code intentionally avoids unsupported intents or automatic activation.
- Validation still required on physical Galaxy S26 configured via QR to compare with normal consumer provisioning.

## 2026-11-10 — Retail Setup 0.11.13-ci: niezależny test przywracania pakietów

- Tryb diagnostyczny QR bez pobierania Retail Mode/Experience pozostaje aktywny.
- Wcześniej StockAppRecovery odrzucał brakujące pakiety już na etapie PackageManager; teraz dla każdego brakującego pakietu z jawnej listy wykonuje rzeczywistą próbę `DevicePolicyManager.enableSystemApp(admin, packageName)`.
- Raport pokazuje stan przed i po, informację o fabrycznym pakiecie, dokładny typ wyjątku oraz liczby prób / przywróceń.
- Dodano przycisk ręcznego ponowienia **samych prób przywracania**, bez resetu i bez pobierania demo.
- Nadal nie ma pobierania APK z niezweryfikowanych źródeł. `enableSystemApp` może przywrócić wyłącznie odpowiednie aplikacje fabryczne; brak wyniku nie oznacza błędu QR.
- Zwolnienie Device Owner pozostaje wyłącznie ręczne w diagnostyce. Potrzebny test na fizycznym S26.

# CHANGELOG — retailmode

## 2026-11-09 — Retail Setup 0.11.5-ci: stabilny ekran Dostępności

- Wyniki testu 0.11.4 na S26: provisioning QR, instalacja, START i zwolnienie Device Owner = OK. Po START ekran Accessibility miga i znika; ręczne otwarcie ustawień pozwalało uruchomić automat.
- Zrezygnowano z nieoficjalnych skrótów do eksportowanych ekranów One UI i ukrytych aktywności Settings. Używana jest wyłącznie oficjalna akcja `Settings.ACTION_ACCESSIBILITY_SETTINGS` otwierana po świadomym START.
- Usunięto wyłączanie aliasu HOME w `MainActivity.onPause()`, które mogło resetować zadanie ustawień podczas przejścia na pierwszy plan.
- Na głównym ekranie, w stanie oczekiwania na zgodę, widoczny jest przycisk «OTWÓRZ PONOWNIE DOSTĘPNOŚĆ». Nie trzeba otwierać diagnostyki, aby powtórzyć przejście.
- Własny alias HOME jest teraz wyłączany przy bezpiecznym powrocie na pulpit lub po potwierdzonym początku pobierania demo. Wcześniej: nie wyłączamy go automatycznie podczas otwierania ustawień.
- Pozostałe funkcje z 0.11.4 bez zmian; w szczególności instalacja, release Device Owner i automatyzacja Retail Mode.
- CI = jedyny APK All-In-One; test telefonu nadal konieczny, szczególnie widoczność ustawień i przejście do aktywacji Accessibility.

## 2026-11-09 — Retail Setup 0.11.4-ci: gotowy START pozostaje na ekranie

- Na S26 użytkownik potwierdził, że przygotowanie i demo działają, lecz przy automatycznym zwolnieniu Device Ownera gotowy ekran START minimalizował się do pulpitu.
- Przyczyną w logice 0.11.3 było wyłączenie aliasu HOME podczas utraty Device Ownera. Wersja 0.11.4 przed zwolnieniem roli otwiera ekran przygotowania w osobnym, nie-HOME zadaniu `ReadyActivity`.
- W ścieżce automatycznej HomeActivity nie jest wyłączana podczas zwolnienia Device Ownera; polityka preferowanego HOME jest nadal czyszczona PRZED zwolnieniem uprawnień. Alias jest wyłączany dopiero po świadomym START i przejściu do innej aktywności lub przez awaryjne «Wyjdź na pulpit Androida».
- START pozostaje wyszarzony podczas przygotowania, a potem staje się aktywny na widocznym ekranie; nie zmieniono kroków automatyzacji Samsung Retail Mode ani włączenia Accessibility.
- Utrzymano działającą opcję powrotu do One UI i powiadomienie «Retail Setup — gotowe do START». Zmieniono tylko przejście między przygotowaniem a gotowością.
- **Wymaga testu na S26:** płynne przejście do START, brak minimalizacji, opcja awaryjnego pulpitu i kompletne demo.

## 2026-11-09 — Retail Setup 0.11.3-ci: powrót do START z pulpitu

- Dodatkowe zabezpieczenie wariantu 0.11.2: po automatycznym zwolnieniu Device Ownera aplikacja pozostawia powiadomienie «Retail Setup — gotowe do START», prowadzące do właściwej aktywności bez ikony.
- Po ręcznym kliknięciu START powiadomienie jest chowane. Jeśli urządzenie uruchomi się ponownie przed uruchomieniem demo, BootReceiver odtwarza drogę powrotu, a po zakończeniu demo może wyłączyć się przy restarcie.
- Nie zmieniono wartości QR Wi-Fi, Site ID, hasła Retail Mode, hash APK Samsung, ani automatyzacji Experience.
- Wymaga testu fizycznego na Galaxy S26; działanie powiadomień po oddaniu administratora jest zależne od zgody systemowej.

## 2026-11-09 — Retail Setup 0.11.2-ci: START gated, pulpitu awaryjny i Accessibility hint

- START wyłączony i wyszarzony do **zakończenia pobierania/instalacji Retail Mode ORAZ faktycznego zwolnienia Device Ownera i aktywnego administratora**. Nie można go nacisnąć za wcześnie.
- Zwolnienie Device Ownera jest automatycznie rozpoczynane z widocznego ekranu Retail Setup, kiedy PackageInstaller potwierdzi instalację; START nie zwalnia Device Ownera ani nie uruchamia się sam.
- Nowy bezpieczny przycisk «Wyjdź na pulpit Androida» w Zaawansowanych: kasuje preferowaną aktywność HOME, wyłącza alias HOME, zapobiega ponownemu narzuceniu HOME i uruchamia zwykły launcher, **bez zatrzymania pobierania i bez pozbawienia aplikacji Device Ownera**.
- Osobna awaryjna opcja «Zwolnij Device Owner» po świadomym potwierdzeniu; po zwolnieniu również przechodzi na pulpit.
- Pod START umieszczono krótki tekst informacyjny o włączeniu Accessibility: Zainstalowane aplikacje → Retail Setup — automatyzacja demo → Włącz. Komunikat zmienia się w zależności od etapu.
- Dostępny dotychczasowy ciemny UI, pasek pobierania APK i powiadomienia systemowe. Brak instruktażowych toastów.
- Nadal wymaga sprawdzenia na fizycznym S26: automatyczny release, szary START, działanie przycisku wyjścia, powrót przez powiadomienie, Accessibility i demo.

## 2026-11-09 — Retail Setup 0.11.1-ci: niezawodniejsze uruchamianie interfejsu

- Audyt QR All-In-One 0.11.0: wskazuje wyłącznie podpisany RetailSetup.apk (SHA-256 poprawne), nie ma instalatora ani pakietu Helpera w nowym kodzie.
- Ustalono błąd: kod wersji 0.11.0 ustanawiał tymczasowe HOME, ale **nigdzie jawnie nie uruchamiał** ekranu START. Na Samsungu S26 HOME nie otwierało się samoczynnie.
- Dodano obsługę oficjalnej aktywności Androida `ACTION_PROVISIONING_SUCCESSFUL` dla nowego Device Ownera oraz własny zapasowy `RetailUiStarter` po `device_provisioned=1` i `user_setup_complete=1`. Zapasowo pozostaje kliknięcie powiadomienia.
- Przy końcowej instalacji Retail Mode ponawiana jest próba otwarcia ekranu (ograniczona częstotliwość). DPC nie próbuje przejmować ekranu po zwolnieniu Device Ownera.
- Na ciemnym ekranie i w diagnostyce widać dokładny numer wersji 0.11.1.
- Bez zmian w SHA-256 Retail Mode, Wi-Fi, danych sklepu, automatyzacji, ręcznej zgodzie Accessibility i sekwencji zwalniania Device Owner. Nie włączano twardego kiosku.
- Wymaga testu na fizycznym Galaxy S26 po pełnym provisioningu nowym QR.
- Poprawiono błąd kompilatora w pierwszej próbie 0.11.1: po wcześniejszym odchudzeniu Gradle trzeba jawnie włączyć `buildFeatures.buildConfig`, aby wyświetlać wersję APK. Dodano testy CI manifestu: wymagane komponenty DPC/Accessibility, brak Helpera, dokładnie jeden APK.

## 2026-11-09 — Retail Setup 0.11.0-ci: All-In-One (eksperyment)

- Powiadomienie pobierania jest również klikalnym wejściem do ekranu START, na wypadek gdy Samsung nie wyświetli tymczasowego HOME po kreatorze.

- Po pierwszej udanej kompilacji usunięto z aktualnego drzewa 16 nieużywanych klas starego Wrappera i cały moduł Helpera, bez usuwania wcześniejszych wydanych APK/QR. Usunięto też niepotrzebne referencje Gradle do pomocniczego APK.

- Jeden APK łączy Device Owner, instalator Retail Mode i sprawdzoną automatyzację przeniesioną z Helpera do podpakietu demo.
- Ciemny ekran START, bieżące etapy i prawdziwy procent pobranych danych; zwijane opcje diagnostyczne; brak instruktażowych Toastów w aktywnej ścieżce.
- Tymczasowy ekran HOME bez Lock Task; przed zwolnieniem Device Owner przywracana zwykła obsługa HOME.
- Wciśnięcie START przed zakończeniem pobierania zapamiętuje intencję; po pobraniu i weryfikacji APK zwalniany jest Device Owner, a aplikacja kontynuuje jako zwykła.
- Ręczna zgoda na Accessibility pozostaje konieczna. Po rozpoczęciu pobierania zawartości demo Accessibility zostaje wyłączona, ikona aplikacji jest domyślnie ukryta.
- Hasło Retail Mode pochodzi z lokalnego QR przez Keystore; nie zapisano haseł w publicznym repo. Poprzedni wariant DPC + Helper pozostaje w archiwalnych wydaniach.
- To wstępna wersja wymagająca testu Galaxy S26, szczególnie pierwszego HOME, przejścia START → zgoda, modelu Experience i restartu.

## 2026-11-09 — DPC 0.10.14-ci + Helper 0.2.15: Wrapper pozostaje bez administratora i bez ikony

- Po instalacji obu APK i podpisanym potwierdzeniu Helpera DPC zwalnia Device Owner i aktywnego Device Administratora, anuluje zadania i czyści powiadomienia jak wcześniej.
- Helper akceptuje stan po **zwolnieniu uprawnień**, nie wymaga już fizycznej deinstalacji pakietu DPC. Nie otwiera systemowego okna deinstalacji.
- Helper potwierdza zwolnienie podpisanym ACK; dopiero po jego odebraniu i ponownej weryfikacji braku obu uprawnień DPC wyłącza swoją ikonę Launchera, BootReceiver i nieużywaną usługę Accessibility. DPC zostaje zainstalowany i jest dostępny z Ustawienia → Aplikacje; MainActivity pozostaje adresowalna jawnie.
- Niezmienione: sprawdzanie SHA-256, szybkie próby pobierania Retail, diagnostyka, eksperymentalne expedited jobs 0.10.13, dane QR, automatyzacja Retail Mode i wyłączanie Accessibility Helpera.
- W Helperze zachowana opcjonalna ręczna deinstalacja DPC w Zaawansowanych. Jeśli ikona nie ukryje się na Samsungu, nie blokuje to rozpoczęcia demo.
- Nowe wersje są **eksperymentalne**; wymagają testu na fizycznym Galaxy S26, w tym testu zachowania po restarcie i braku ikony DPC.

## 2026-11-08 — DPC 0.10.13-ci + Helper 0.2.14: eksperyment JobScheduler expedited

- Pomiar na S26 w 0.10.12 pokazał 41,522 s od pierwszego zaplanowania Retail jobu do wejścia do JobService i tylko 0,001 s na flagi: obie były już ustawione. 41,522 s obejmuje czas kreatora konfiguracji, więc nie jest bezpośrednim pomiarem opóźnienia po wyświetleniu pulpitu.
- Retail i Helper są teraz w pierwszej kolejności zgłaszane jako `JobInfo.Builder.setExpedited(true)` (API 31+), z dotychczasowym wymaganiem sieci, retry exponential i `setPersisted(true)`.
- Jeżeli OS odrzuci expedited przy budowaniu lub `JobScheduler.schedule()` zwróci `RESULT_FAILURE`, natychmiast wracamy do sprawdzonego standardowego `JobScheduler` dla tego samego jobu.
- Log przy uruchomieniu zapisuje `isExpeditedJob()` i pozostawia istniejące podsumowanie czasów w widoku DPC oraz powiadomieniu. Bramka flag bez zmian; nie ma dodatkowego testu Internetu.
- Uwaga: tryb expedited ma limity i Android nadal może opóźniać lub przerywać zadanie; to eksperyment, nie gwarancja kilku sekund. Nie zmieniono Helpera 0.2.14, SHA-256, haseł QR, potwierdzenia usunięcia DPC ani Accessibility.

## 2026-11-08 — DPC 0.10.12-ci + Helper 0.2.14: trwały wynik diagnostyki Setup Wizard

- Naprawiono brak widocznych powiadomień diagnostycznych: 0.10.11 usuwała je natychmiast po wykryciu gotowości; gdy obie flagi były już ustawione przy starcie JobService, nie powstawały wcale.
- Teraz po pomyślnym sprawdzeniu flag pojawia się osobne powiadomienie „Diagnostyka startu — wynik”, które pozostaje widoczne podczas pobierania Retail Mode do czasu zwolnienia i usunięcia Wrappera.
- Wynik rozdziela czas od zaplanowania JobSchedulera do uruchomienia zadania i czas oczekiwania w bramce. Informuje też, czy każda flaga była już ustawiona przy pierwszym odczycie, czy została zaobserwowana później; nie twierdzi, że zna rzeczywisty moment zmiany sprzed pierwszego odczytu.
- Wynik zapisany również w lokalnym widoku diagnostyki DPC i logach, na wypadek ukrycia powiadomień przez system.
- Bez zmian warunków rozpoczęcia pobierania i Helpera 0.2.14. QR, szybkość retry, SHA-256, pasek pobierania, ręczne usuwanie DPC i wyłączanie Accessibility zachowane.

## 2026-11-08 — DPC 0.10.11-ci + Helper 0.2.14: diagnostyka opóźnienia flag Setup Wizard

- Bez zmian warunków uruchomienia: nadal wymagane `device_provisioned=1` i `user_setup_complete=1`, dostępność sieci nadzorowana przez JobScheduler (bez dodatkowego testu Internetu).
- Powiadomienie diagnostyczne na Galaxy S26 pokazuje, która flaga nie jest ustawiona oraz czas oczekiwania; po osiągnięciu gotowości znika, ustępując paskowi pobierania.
- Logowane są oddzielnie wartości 0/1 flag i czas ich pierwszego wykrycia na 1, a także czas między pierwszym zaplanowaniem jobu i wejściem do bramki.
- Ograniczono odświeżanie diagnostyki do 2,4 s; nie zapisuje haseł ani danych sieci i nie zmienia niezawodności SHA-256/retry.
- Bez zmian w Helperze 0.2.14, przekazywaniu Device Owner, potwierdzeniu usunięcia DPC i sprzątaniu.
- To wersja diagnostyczna. Zachowano stabilny wariant referencyjny 0.10.10-ci.

## 2026-11-08 — DPC 0.10.10-ci + Helper 0.2.14: szybsza bramka Setup Wizard

- Pobieranie Retail Mode i Helpera czeka tylko na rzeczywiste flagi końca konfiguracji Androida: `device_provisioned` oraz `user_setup_complete`.
- Usunięto dodatkowy test `NET_CAPABILITY_VALIDATED` i 2,5 s stabilizacji Internetu. Po poprawnym QR dostęp do sieci nadal jest warunkiem `JobScheduler.NETWORK_TYPE_ANY` oraz rzeczywistego pobierania HTTPS.
- Częstotliwość sprawdzania końca konfiguracji zwiększona z ~1,4 s do 0,7 s. Log zawiera czas oczekiwania w ms, bez haseł i SSID.
- Przy dłuższym kreatorze zachowano limit 4 minut i późniejsze ponawianie przez JobScheduler. Zachowano też 3 szybkie próby pobrania Retail Mode i integralność SHA-256.
- Helper wciąż 0.2.14: bez zmian w zgodach Accessibility, starcie demo, handoff, potwierdzaniu deinstalacji Wrappera i sprzątaniu.
- Poprzedni DPC 0.10.9-ci pozostaje punktem odniesienia sprawdzonym na S26. Nowy wariant wymaga testu na telefonie.

## 2026-11-08 — DPC 0.10.9-ci + Helper 0.2.14: pobieranie po konfiguracji Androida

- Oba joby APK czekają na zakończenie Setup Wizard (device_provisioned i user_setup_complete) oraz zweryfikowaną przez Android sieć (INTERNET + VALIDATED).
- Internet musi być stabilny przez co najmniej 2,5 s; nie opieramy gotowości na arbitralnym czasie od skanowania QR.
- Sprawdzenie w wątku JobService nie blokuje kreatora. Jeśli konfiguracja trwa ponad 4 min, JobScheduler podejmuje kolejną próbę.
- Logi pokazują tylko powód oczekiwania (setup/internet/stabilizacja) i czas; bez haseł, SSID i danych sklepu.
- Zachowano szybkie retry Retail Mode, dwa paski postępu, przekazanie hasła przez QR, automatyzację rejestracji i ręczne potwierdzenie usunięcia DPC.
- Nie zmieniono Helpera 0.2.14; nowy eksperymentalny build wymaga testu S26.

## 2026-11-08 — DPC 0.10.8-ci + Helper 0.2.14: szybkie retry, pasek, potwierdzenie usunięcia

- Retail Mode APK: maksymalnie 3 lokalne próby pobrania przy przejściowych błędach I/O lub HTTP 408/429/5xx, z przerwami 3 s i 8 s. Po ich wyczerpaniu nadal pozostaje JobScheduler, bez utraty sprawdzania SHA-256.
- Oddzielne powiadomienia postępu Retail Mode i Retail Helper (dwa niezależne ID, ograniczone aktualizacje) eliminują problem odrzucania postępu jednego APK przez stan drugiego pobierania. Sprawdzanie uprawnień powiadomień wykonywane już w callbackach DO.
- Po zwolnieniu Device Owner DPC nie wykonuje eksperymentalnej próby cichej deinstalacji i nie odczekuje 9 sekund. Helper od razu próbuje otworzyć systemowe okno potwierdzenia.
- Przed potwierdzonym usunięciem DPC główny START Helpera jest zablokowany: użytkownik nie może uciec do Retail Mode zanim zobaczy wymagane okno. W razie anulowania przycisk pozwala ponowić usuwanie bez resetu.
- Pakiet DPC wykrywany jest przez PackageManager.getPackageInfo, a nie obecność aktywności LAUNCHER. Zgoda usunięcia pozostaje świadoma i ręczna.
- Nie zmieniano konfiguracji Wi-Fi, hasła Retail Mode w QR, automatyzacji formularzy Samsung Retail Mode ani wyłączania Accessibility.
- Kompilacja nie potwierdza jeszcze zachowania na fizycznym Galaxy S26; nowy build eksperymentalny.

## 2026-11-08 — DPC 0.10.7-ci + Helper 0.2.13: hasło Retail Mode w lokalnym QR

- QR może przekazać opcjonalną wartość `retail_password` w `PROVISIONING_ADMIN_EXTRAS_BUNDLE`; **nie** ma hasła w publicznym kodzie GitHub ani w kompilowanym APK.
- DPC odbiera sekret podczas provisioningu, przechowuje go tymczasowo szyfrowany przez Android Keystore i przekazuje tylko do jawnie wskazanej aktywności Helpera. Czyści kopię przy udanym uruchomieniu Helpera; DPC znika przed demo.
- Gdy Android Keystore jest niedostępny, pamięć RAM jest wariantem awaryjnym, a po utracie procesu można wpisać hasło ręcznie.
- Helper odbiera hasło do ulotnej pamięci, nie pokazuje go na ekranie i automatycznie zaznacza tryb hasła. Jeden świadomy START uruchamia istniejący automat; zgoda Accessibility i potwierdzenie odinstalowania DPC pozostają ręczne.
- Wi-Fi może zostać skonfigurowane przez standardowe pola QR `PROVISIONING_WIFI_*`; dane sieci i hasło Retail Mode pojawiają się wyłącznie w lokalnym QR testowym, nigdy w repo.
- Rejestracja profilu Experience, weryfikacja APK, postęp pobierania i ścieżki nawigacji Androida niezmienione. Wersja wymaga testu na Galaxy S26.

## 2026-11-08 — DPC 0.10.6-ci + Helper 0.2.12: rzeczywisty postęp i mniej kliknięć

- Powiadomienie DPC pokazuje rzeczywiste bajty pobrane przez połączenie HTTPS; procent i pasek są widoczne tylko dla znanego rozmiaru. To NIE jest fikcyjny pasek postępu instalacji.
- Odświeżanie postępu jest ograniczone do ok. 1,6 s, aby nie obciążać UI. Etapy: pobieranie, sprawdzanie SHA-256, instalacja; bez zmiany pinów APK i certyfikatów.
- Toasty DPC pokazują wyłącznie najnowszy etap; poprzednie oczekujące komunikaty są anulowane, a wyświetlanie jest ograniczone do 1/4,5 s. Android może je dodatkowo opóźniać w tle.
- Helper wykonuje konfigurację po jednym świadomym kliknięciu ROZPOCZNIJ DEMO zamiast wymagać kolejnego potwierdzenia w oknie dialogowym. Zgoda na Dostępność i potwierdzenie usunięcia DPC pozostają ręczne.
- Logika automatycznej rejestracji Retail Mode, przekazanie sterowania, usuwanie DPC oraz nawigacja OEM Dostępności bez zmian.
- Wersje testowe: DPC 0.10.6-ci (versionCode 29), Helper 0.2.12 (versionCode 18); nieprzetestowane jeszcze na S26.

## 2026-11-08 — DPC 0.10.5-ci + Helper 0.2.11: nawigacja Samsung Accessibility

- Użytkownik potwierdził sukces Retail Mode i 5/6 kontroli na S26 dla DPC 0.10.4-ci + Helper 0.2.10; bezpośredni skrót Accessibility nadal przenosił do strony ogólnej.
- Helper najpierw sprawdza obsługę Samsungowego intentu `com.samsung.accessibility.installed_service` bez wymuszania pakietu `com.android.settings`.
- Aby uniknąć przechwycenia, uruchamia tylko eksportowane aktywności oznaczone jako systemowe, bez wymaganych niedostępnych uprawnień.
- Dodano widoczność intentu w `<queries>` i diagnostykę nawigacji. Jeśli ekran Samsung nie jest dostępny, zachowano poprzednie ścieżki i ogólną Dostępność.
- Nie zmieniano aktywacji Retail Mode, handoff, zachowania Device Owner, usług Accessibility ani Toastów DPC.
- Uwaga: niepubliczny skrót Samsunga wymaga osobnego testu na S26; Android nie gwarantuje bezpośredniego ekranu.

## 2026-11-08 — DPC 0.10.4-ci + Helper 0.2.10: Toasty i ustawienia Dostępności

- Po potwierdzonym przez użytkownika pełnym uruchomieniu Retail Mode na Galaxy S26 w wersji DPC 0.10.3-ci nie zmieniano logiki instalacji ani rejestracji demo.
- DPC wyświetla ograniczone częstotliwościowo komunikaty Toast także podczas kreatora konfiguracji, bez warunku `user_setup_complete=1`. Powiadomienie pozostaje podstawowym wskaźnikiem, jeśli system blokuje Toasty w tle.
- Helper próbuje najpierw otworzyć samsungową listę zainstalowanych usług Dostępności przez `com.samsung.accessibility.installed_service`, a gdy nie działa, stosuje dotychczasowe bezpieczne ścieżki i ekran ogólny.
- Dostępność nadal włącza użytkownik ręcznie. Brak zmian w profilach, haśle demo, handoff, zwalnianiu Device Owner i automatyce Samsung Retail Mode.
- Wersje testowe: DPC `0.10.4-ci` (`versionCode` 27), Helper `0.2.10` (`versionCode` 16); wymagają testu na S26.

## 2026-11-08 — DPC 0.10.3-ci: Retail Mode RAW

- Naprawiono pobieranie Samsung Retail Mode: zamiast zawodnego GitHub Releases DPC używa niezmiennej kopii RAW pod commitem `58bed6b59596e1ed82bb2526bbdb202f114a3391`.
- Kopia APK (34 192 143 B) została pobrana przez GitHub API i zweryfikowana SHA-256 `da5ede9aa02aa278fa82b97caba8eb2e79a4e6fea9bdc5e3b50ebb3e5bc245c4`.
- Podniesiono DPC `versionCode` 25 → 26, `versionName` na `0.10.3-ci-retail-raw`.
- CI sprawdza pobranie całego 34 MB pliku z RAW i sumę kontrolną **przed** publikacją DPC.
- Bez zmian w Helperze, logice handoff, automatyzacji Samsung Retail Mode i zwalnianiu Device Owner.
- Kompilacja CI i test na S26 wymagają osobnego potwierdzenia; aktualnie już zainstalowany DPC 0.10.2-ci sam nie zmienia adresu Retail Mode.


## 2026-11-08 — porządki i CI

- Oba projekty Android Studio zaimportowane do `android/`.
- Usunięto zbędny ZIP bootstrap, jednorazowe workflowy kopiowania/publikacji oraz stare, powielone instrukcje wersji DPC. Historię zachowano w Git.
- Pozostawiono stare Releases i trzy binaria RAW wykorzystywane przez wcześniejsze kody QR.
- Naprawiono inicjalizację Android SDK w workflow źródłowej kompilacji (poprzednio próba instalacji wycofanego `tools`).
- Zaktualizowano README, dokumentację CI i instrukcje bieżącej wersji DPC 0.10.2-ci + Helpera 0.2.9.
- Pierwsza automatyczna publikacja **bez kompilacji** zakończyła się sukcesem: `ci-smoke-20261108`.
- Po naprawie SDK dwa pełne buildy z kodu zakończyły się sukcesem: `ci-build-42474697059` i `ci-build-42475090083` (oba zawierają DPC, Helper, SHA256SUMS i DEPLOYMENT).
- Kod bieżącej wersji nadal wymaga pełnego testu na Galaxy S26.
