# Retail Setup 0.11.10-ci — All-In-One

Pakiet Android: `com.example.retaildpc`. **Jeden APK.** Nie ma oddzielnego Retail Helpera.

## Instalacja i obsługa Galaxy S26 (urządzenie testowe)

- QR uruchamia provisioning Device Owner i przekazuje lokalne parametry Wi-Fi, sklepu i hasło demo. Dane poufne nie są publikowane w repo.
- Retail Setup po zakończeniu provisioningu korzysta z `ACTION_PROVISIONING_SUCCESSFUL`, jawnego otwarcia po gotowości flag Androida oraz tymczasowego HOME (z powiadomieniem jako fallback); wyświetla ciemny interfejs, bieżące etapy przygotowania i procent pobierania zweryfikowanego APK Samsung Retail Mode.
- START jest niedostępny, dopóki nie zakończy się instalacja Retail Mode i aplikacja automatycznie nie zwolni Device Ownera oraz administratora. Dopiero wtedy START otwiera instrukcję i ekran zgody Accessibility. Wyjście na zwykły pulpit w Zaawansowanych nie przerywa instalowania APK.
- Użytkownik ręcznie zatwierdza usługę Accessibility aplikacji Retail Setup w ustawieniach Androida; aplikacja nie próbuje omijać zgody.
- Wbudowany automat wybiera kraj, partnera, sklep, hasło i zgodny profil Experience. Po wykryciu realnego początku pobierania demonstracji wyłącza Accessibility i pozostaje bez ikony.
- Aplikacja nadal jest widoczna w systemowym **Ustawienia → Aplikacje**. Nie jest odinstalowywana.

Przycisk **Opcje zaawansowane** zawiera ręczne pola parametrów, ponów, Wi-Fi, Accessibility, logi i awaryjne przywrócenie pulpitu. Nie ma osobnego okna deinstalacji ani instruktażowych Toastów.

## Bezpieczeństwo i ograniczenia

To prototyp do testów na uprawnionym, możliwym do zresetowania Samsungu. Wersja **nie używa Lock Task** (twardego kiosku). Przejęcie tymczasowego HOME i przywrócenie fabrycznego launchera wymagają sprawdzenia na urządzeniu. `clearDeviceOwnerApp()` jest API testowym, przestarzałym i może pozostawić polityki systemowe.

## Kompilacja

`gradle -p android/retail-dpc :app:assembleDebug` lub GitHub Actions `retail-build-from-source.yml`. CI publikuje **tylko** `RetailSetup.apk`, SHA-256 i plik deployment z przypiętym adresem RAW. Archiwalne APK Samsung Retail Mode i wydania DPC/Helper nie zostały usunięte, ponieważ starsze QR mogą ich używać.

## 0.11.7 — brakujące aplikacje (test)

- Przy zwalnianiu Device Ownera, po zainstalowaniu Retail Mode, wykonywany jest **best-effort** odczyt i przywrócenie allowlisty wybranych aplikacji fabrycznych przez publiczne `DevicePolicyManager.enableSystemApp`.
- Bez pobierania/instalowania niezweryfikowanych APK. Pakiety niepreinstalowane (np. Spotify / Facebook zależnie od wariantu) wymagają oficjalnego sklepu.
- Dla pakietów rozróżniamy stany: zainstalowana, wyłączona, niezainstalowana dla bieżącego użytkownika, brak/niewidoczna. Dzięki z góry zadeklarowanym pakietom `<queries>` nie wymagamy `QUERY_ALL_PACKAGES`.
- W ustawieniach za PIN-em **5444** dostępne «Pokaż raport aplikacji fabrycznych» i log z wynikami przywracania. Liczb nie wolno interpretować jako potwierdzenia obecności ikon bez testu na S26.
- Wbudowany proces Retail Mode i funkcje 0.11.6 niezmienione. Po teście porównać Kalendarz, Kalkulator, Zegar, Notes, Members, Bixby i społecznościowe.

## 0.11.8: rejestracja bez powtarzania instalacji

- W zaawansowanym panelu dodano «Ponów samą rejestrację Retail Mode». To świadome ponowienie automatu po wcześniejszym zatrzymaniu; nie pobiera ponownie APK, nie przywraca Device Owner i nie wykonuje resetu.
- Hasło Retail Mode nie jest utrwalane po pierwszej konfiguracji. Aby automatycznie ponowić również hasło, należy wpisać je ponownie w polu zaawansowanym. PIN 5444 otwiera tylko panel serwisowy.
- Potwierdzona lokalnie rejestracja blokuje ponowienie, by nie naruszyć działającego demo.
- Przed klikaniem formularza Accessibility analizuje wyłącznie widoczne komunikaty Samsung Retail Mode, które jednoznacznie wskazują na rejestrację lub pobieranie. Wówczas pomija formularz, zapisuje status i wyłącza Accessibility. Ekran główny, obecność APK lub niejednoznaczny tekst nie są dowodem ukończenia rejestracji.
- Wobec braku udokumentowanego API Samsunga do odczytu rejestracji detekcja jest ostrożna i zależy od wersji UI. Wymaga testu na S26.

## 0.11.9: hasło z QR przy ponowieniu rejestracji

- Retail Mode password przekazane przez QR jest jednorazowo kopiowane do osobnego sejfu AndroidKeyStore AES-GCM na potrzeby **świadomie wybranego** «Ponów samą rejestrację».
- Hasło QR w pierwotnym schowku jest usuwane tak jak dotychczas. Zaszyfrowana kopia wygasa po 72 godzinach i jest automatycznie trwale usuwana po potwierdzonym rozpoczęciu pobierania lub wcześniejszej rejestracji Retail Mode.
- Gdy szyfrowanie lub odczyt nie są możliwe, okno ponowienia ma pole do ręcznego wpisania hasła. Wybór pustego pola nie udaje trybu pełnego.
- PIN 5444 do panelu serwisowego **nie jest** hasłem Retail Mode. Hasło do rejestracji nie pojawia się w logach, UI ani repozytorium.
- Logika automatyzacji i wykrywania ukończonego demo z 0.11.8 bez zmian. Wymaga potwierdzenia na Galaxy S26.

## 0.11.10 — oficjalne sklepy i brakujące aplikacje

- Opcje zaawansowane: «Brakujące aplikacje • oficjalne sklepy». Lista pokazuje pakiety niewykryte lub wyłączone, następnie pozwala otworzyć oficjalną stronę Google Play albo Galaxy Store (Samsung) do świadomej instalacji.
- Brak instalacji po cichu, nie pobieramy obcych APK, a sklep może wymagać konta i obsługi użytkownika.
- Samsung Knox KBA (2026-09-01) potwierdza, że Calendar, Clock i Notes na nowszych Galaxy nie są systemowymi APK do przywrócenia przez enableSystemApp. Automatyczna dystrybucja wymaga Managed Google Play/EMM.
- Zmieniono StockAppRecovery, aby nie wywoływać przewidywalnie błędnej metody dla pakietów niebędących systemowymi. Pozostałe działania, QR, panel z PIN oraz rejestracja bez zmian.
