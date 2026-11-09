# Retail Setup 0.11.7-ci — All-In-One

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
