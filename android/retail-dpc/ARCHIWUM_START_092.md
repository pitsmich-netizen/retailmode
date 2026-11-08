# Retail DPC Wrapper 0.9.2 — Helper Installer LAB

**Status:** kod źródłowy sprawdzony statycznie; nie zbudowany tutaj przez Android SDK i nie testowany na Galaxy S26. Bazuje na 0.9.1, ale posiada nową numerację i czytelną diagnostykę ustawień Helpera.

## Konfiguracja Helpera: gotowa

Plik `helper-apk.properties` **jest dołączony** do tej paczki, obok `settings.gradle`:

```properties
helper.url=https://github.com/pitsmich-netizen/retailmode/releases/download/helper-010-test/RetailHelper.apk
helper.sha256=772f093758880d5df4243e6de11292e0c166dcbc7fec7cc2e6b8dc414a671c71
```

Gradle ładuje te wartości do `BuildConfig.HELPER_URL` oraz `BuildConfig.HELPER_SHA256`. Kod `HelperInstallJobService` pobiera APK po HTTPS, porównuje SHA-256 i sprawdza `com.example.retailhelper` przed instalacją przez `PackageInstaller` (w ramach Device Owner). Same metadane wpisane w kod **nie gwarantują**, że instalacja na S26 zakończy się poprawnie; potwierdzi to status diagnostyczny i test.

Nie zmieniaj Helper APK bez zmiany sumy SHA-256. Ta suma dotyczy **Helpera**, a nie Wrappera.

## Nowy numer wersji w zawartości projektu

- `app/build.gradle`: `versionCode 14`, `versionName 0.9.2-helper-installer-lab`
- `AndroidManifest.xml`: etykieta `Retail DPC LAB 0.9.2`
- `MainActivity.java`: numer wersji jest widoczny w nagłówku UI, a poniżej sekcji Helper pojawia się status skonfigurowania, URL i SHA
- Nazwa katalogu: `RetailDPC_092_HelperInstaller_CONFIGURED`
- **Application ID i DeviceAdminReceiver pozostają bez zmian:** `com.example.retaildpc` i `.AdminReceiver`.

## Kompilacja i nowy QR

1. Rozpakuj ZIP jako **osobny projekt**, nie nadpisuj działającej wersji 0.9.0.
2. Otwórz Android Studio i uruchom `Build > Build Bundle(s)/APK(s) > Build APK(s)` z wariantem **debug**. Nie wybieraj wydania release bez podpisu.
3. Po `BUILD SUCCESSFUL` skopiuj `app/build/outputs/apk/debug/app-debug.apk`, nazwij kopię `RetailDcp.apk`.
4. Ustaw osobne testowe wydanie GitHub Releases `dcp-092-test` (nie nadpisuj 0.9.0) i dodaj APK.
5. Sprawdź sumę SHA-256 nowego **Wrappera** w metadanych GitHub Release (albo PowerShell `Get-FileHash ... -Algorithm SHA256`). Wpisz NOWY adres i SHA w generatorze QR w trybie sumy pliku APK.
6. Przed resetem telefonu sprawdź dostępność **obu** linków (`RetailDcp.apk`, `RetailHelper.apk`) z przeglądarki incognito. Sprawdź też Retail Mode `rmtest`.

## Przebieg testu na uprawnionym telefonie

- Wrapper 0.9.2 po QR instaluje Retail Mode oraz Helpera; diagnostyka musi wykazać **obie** instalacje.
- Przed zwolnieniem Device Owner w UI Wrappera użyj `Helper: przekaż dane sklepu i otwórz Helpera`. Przekazywane są tylko `country`, `partner`, `site_id`, nie hasła.
- Usługę Accessibility trzeba włączyć ręcznie; telefon może też wymagać dodatkowego zezwolenia Androida. Nie omijamy tych zgód.
- Dopiero po potwierdzeniu instalacji Helpera i poprawnej diagnostyki zwolnij Device Owner, usuń Wrapper i przeprowadź test konfiguracji Retail Mode.
- `clearDeviceOwnerApp()` pozostaje ścieżką testową i jest przestarzałe. Nie używaj na produkcyjnym telefonie.

## Podpis i bezpieczeństwo

Plik `release-signing.properties` oraz klucz `.jks` pozostają lokalne. `helper-apk.properties` nie jest tajny, lecz jest ignorowany przez `.gitignore`; **w dostarczonym ZIP jest dołączony**. Zachowaj kopie działających APK i QR.
