> ARCHIWALNA INSTRUKCJA DLA 0.9.1. Najnowsza konfiguracja: `START_092.md`.

# Retail DPC 0.9.1 — Retail Helper Installer LAB

Wersja rozszerza 0.9.0, która została z powodzeniem sprawdzona na Galaxy S26. **Nie nadpisuje 0.9.0 i nie wymaga resetu działającego S26 do kompilacji.**

## Jak przygotować Helper APK

1. W projekcie `RetailHelper_010_AndroidStudio_LAB` zbuduj **debug** APK (`app/build/outputs/apk/debug/app-debug.apk`).
2. Nazwij kopię APK `RetailHelper.apk` (samo przemianowanie nie zmienia SHA-256).
3. Opublikuj w PUBLICZNYM repo GitHub Releases nowe wydanie o tagu `helper-010-test` i plik `RetailHelper.apk`.
4. Na komputerze policz SHA-256: `Get-FileHash "C:\SCIEZKA\RetailHelper.apk" -Algorithm SHA256`. GitHub Releases pokazuje również digest SHA-256; wartości powinny być takie same.
5. Skopiuj `helper-apk.properties.example` do `helper-apk.properties` w katalogu głównym Wrappera 0.9.1. Wpisz tam opublikowany URL i HASH (64 znaki hex). Nie używaj `PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM` dla Helpera, to osobna instalacja APK.
6. Zbuduj `debug` Wrappera 0.9.1: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
7. Policz SHA-256 NOWEGO Wrappera i opublikuj w osobnym wydaniu `dcp-091-test` jako `RetailDcp.apk`. Generator QR używa nowego adresu i hash (stary QR nadal wskazuje na 0.9.0).

## Jak działa

- DPC podczas QR instaluje Retail Mode dokładnie jak w 0.9.0, osobno kolejkuje pobieranie Helpera.
- Instalacja Helpera jest włączona **tylko gdy plik `helper-apk.properties` był poprawnie skonfigurowany podczas kompilacji**. Bez niego DPC działa jak poprzedni i raportuje `Helper: NIE SKONFIGUROWANY`.
- Instalator Helpera ogranicza pobieranie do HTTPS, maksymalnie 30 MiB, porównuje SHA-256 pliku, weryfikuje package ID `com.example.retailhelper`, a dopiero potem wywołuje `PackageInstaller` w kontekście Device Owner.
- Pojawią się nowe elementy diagnostyki i przyciski **ponów instalację Helpera** oraz **przekaż dane sklepu do Helpera**.
- Zwróć uwagę na wynik `SUKCES: Retail Helper 0.1 zainstalowany`. Sam komunikat o `PackageInstaller.commit` nie oznacza jeszcze sukcesu.
- Przycisk przekazania danych otwiera ekran Helpera, przekazuje `country`, `partner`, `site_id`. Nie przekazuje hasła Retail Mode ani Wi-Fi. Helper zachowuje pola lokalnie i oczekuje świadomego START.
- Jeśli instalacja Helpera została skonfigurowana przy kompilacji, zwolnienie Device Owner jest blokowane, dopóki `com.example.retailhelper` nie jest rzeczywiście zainstalowany.
- Po ręcznym zwolnieniu Device Owner i usunięciu Wrappera użytkownik otwiera Helpera, **ręcznie włącza Accessibility**, weryfikuje sklep i uruchamia test lub tryb pełny. Dopiero po zweryfikowaniu demo wyłącza usługę i ukrywa ikonę.

## Ograniczenia i bezpieczeństwo

- `helper-apk.properties` jest wyłączony z Git; jest lokalną konfiguracją dystrybucji. Nie zawiera żadnych haseł.
- Helper 0.1 domyślnie przyjmuje `site_id`/`partner`/`country` przez eksportowaną Activity; dane nie są sekretami. Na etapie testów użytkownik zawsze weryfikuje je na ekranie Helpera. Podpisowe uwierzytelnienie nadawcy zostanie dodane przed wykorzystaniem produkcyjnym.
- Android może zażądać potwierdzenia instalacji albo zgody na usługę Accessibility. Wrapper NIE wymusza ani nie omija zgód. Zobacz log `Helper wymaga potwierdzenia`.
- Nie wkładaj hasła administratora Retail Mode do APK, GitHuba ani QR.
- Debug APK Helpera może mieć inny podpis na innym komputerze — nie nadpisuj instalacji innym debug key. Stały klucz release przygotujemy później.
- `clearDeviceOwnerApp` jest eksperymentalne; działający test 0.9.0 nie gwarantuje, że 0.9.1 zachowa się identycznie na każdym telefonie.
- Źródła zostały zweryfikowane statycznie. Nie przeprowadzono kompilacji w Android Studio ani testu na S26 w tym środowisku.
