# CHANGELOG — retailmode

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
