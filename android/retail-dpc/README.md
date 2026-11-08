# Retail DPC 0.10.1 — Helper 0.2.9 z alternatywnego serwera GitHub

## Cel

Naprawa blokady pobierania Helpera przy błędach HTTP 504 z GitHub Releases.
Skompilowany Helper 0.2.9 nie jest zmieniany; GitHub Actions zweryfikował SHA-256 oryginalnego APK przed zapisaniem kopii do repozytorium.

- DPC: `com.example.retaildpc`, versionCode **24**, versionName `0.10.1-helper029-rawmirror`.
- Helper: `com.example.retailhelper`, **0.2.9**, versionCode 15.
- Domyślny URL Helpera: `https://raw.githubusercontent.com/pitsmich-netizen/retailmode/main/provisioning/RetailHelper-029.apk`
- SHA-256 Helpera: `5382663fd48cd77ee805bb4ad272ebcd8e8ddc1ef1e978a1f40dc7cf59351220`
- Zapasowa ścieżka: pobieranie z GitHub Releases (po zmianie helper-apk.properties).
- Zaufane źródła wymienione w `HelperConfig.java` są ograniczone do konkretnego repozytorium.
- Po pobraniu DPC sprawdza SHA-256 i nazwę pakietu APK przed instalacją.

## Zachowane elementy

Instalacja Samsung Retail Mode, powiadomienia o postępie, komunikaty Toast, przekazanie sterowania Helperowi i jego automatyczny START, zwolnienie Device Ownera przed demo oraz próba samousuwania DPC z fallbackiem do systemowego potwierdzenia. Żadnych zmian w protokole handoff ani w automatyzacji demo.

**Uwaga:** ciche odinstalowanie Wrappera nadal nie działało w dotychczasowych testach na S26. Helper ma pozostać zainstalowany z niewidoczną ikoną (działanie 0.2.9 nie jest jeszcze zweryfikowane na S26).

## Budowa i test

1. Rozpakuj projekt, uruchom Android Studio i wybierz **Build APK(s)**.
2. Zweryfikuj pakiet, versionCode, podpis i SHA-256 APK; dopiero potem opublikuj jako `dcp-101-test`.
3. Skorzystaj z QR Device Owner z nowym adresem pobierania DPC z `raw.githubusercontent.com`, a w `PROVISIONING_ADMIN_EXTRAS_BUNDLE` ustaw powyższy URL oraz SHA-256 Helpera.
4. Jeśli Helper nie instaluje się, odczytaj log w DPC **przed** ręcznym zwolnieniem Device Ownera.

**Status:** źródła przygotowane. Nie przetestowano kompilacji ani działania na S26.
