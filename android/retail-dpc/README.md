# Retail Setup 0.11.0-ci — All-In-One

Pakiet Android: `com.example.retaildpc`. **Jeden APK.** Nie ma oddzielnego Retail Helpera.

## Instalacja i obsługa Galaxy S26 (urządzenie testowe)

- QR uruchamia provisioning Device Owner i przekazuje lokalne parametry Wi-Fi, sklepu i hasło demo. Dane poufne nie są publikowane w repo.
- Retail Setup tymczasowo zajmuje ekran HOME, wyświetla ciemny interfejs, bieżące etapy przygotowania i procent pobierania zweryfikowanego APK Samsung Retail Mode.
- **START** zapamiętuje polecenie nawet jeśli pobieranie jeszcze trwa. Po instalacji, bez dodatkowego Helpera, aplikacja przywraca zwykły HOME, zwalnia Device Owner i administratora i przechodzi do uruchamiania demo.
- Użytkownik ręcznie zatwierdza usługę Accessibility aplikacji Retail Setup w ustawieniach Androida; aplikacja nie próbuje omijać zgody.
- Wbudowany automat wybiera kraj, partnera, sklep, hasło i zgodny profil Experience. Po wykryciu realnego początku pobierania demonstracji wyłącza Accessibility i pozostaje bez ikony.
- Aplikacja nadal jest widoczna w systemowym **Ustawienia → Aplikacje**. Nie jest odinstalowywana.

Przycisk **Opcje zaawansowane** zawiera ręczne pola parametrów, ponów, Wi-Fi, Accessibility, logi i awaryjne przywrócenie pulpitu. Nie ma osobnego okna deinstalacji ani instruktażowych Toastów.

## Bezpieczeństwo i ograniczenia

To prototyp do testów na uprawnionym, możliwym do zresetowania Samsungu. Wersja **nie używa Lock Task** (twardego kiosku). Przejęcie tymczasowego HOME i przywrócenie fabrycznego launchera wymagają sprawdzenia na urządzeniu. `clearDeviceOwnerApp()` jest API testowym, przestarzałym i może pozostawić polityki systemowe.

## Kompilacja

`gradle -p android/retail-dpc :app:assembleDebug` lub GitHub Actions `retail-build-from-source.yml`. CI publikuje **tylko** `RetailSetup.apk`, SHA-256 i plik deployment z przypiętym adresem RAW. Archiwalne APK Samsung Retail Mode i wydania DPC/Helper nie zostały usunięte, ponieważ starsze QR mogą ich używać.
