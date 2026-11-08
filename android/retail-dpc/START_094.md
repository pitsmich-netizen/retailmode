# START — DPC 0.9.4 / Helper 0.2.3

1. Nie używaj starego QR z DPC 0.9.3.
2. Zbuduj DPC w Android Studio; sprawdź, że APK to `com.example.retaildpc`, versionCode `17`.
3. Opublikuj pod tagiem `dcp-094-test` jako `RetailDcp.apk`.
4. Zweryfikuj SHA-256 opublikowanego APK.
5. Wygeneruj QR Device Owner z NOWYM adresem + sumą DPC, a w admin extras
   podaj `helper_url` = `https://github.com/pitsmich-netizen/retailmode/releases/download/helper-023-test/RetailHelper.apk`
   i `helper_sha256` = `cf5c7b00f3d94a979b3a3be72c4e5610ce8ba5f6b56ead98dffccd091634cd2e`.
6. Na telefonie testowym: po pełnym setupie oczekuj automatycznej próby otwarcia Helpera.
   Jeśli nie nastąpi, otwórz DPC i kliknij `Helper: przekaż dane sklepu i otwórz Helpera`.
7. Po ręcznej zgodzie Accessibility i świadomym START sprawdź rejestrację i wyłączenie usługi po komunikacie o pobieraniu.

DPC 0.9.4 to projekt TESTOWY — zachowaj DPC 0.9.3 i Helper 0.2.1.
