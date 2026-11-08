# Retail Helper 0.2.9 — One UI, domyślnie niewidoczna ikona

**Test źródłowy; kompilacja oraz wynik na Galaxy S26 do weryfikacji.**

Nowa zmiana: alias LAUNCHER jest **wyłączony już w AndroidManifest.xml** (`android:enabled="false"`), zamiast deklaracji włączonej i późniejszego wyłączania. Android 10+ wstrzykuje „syntetyczną” ikonę kierującą do informacji o aplikacji, jeśli ukryto ikonę aplikacji, która miała ją domyślnie włączoną. DPC otwiera Helpera przez jawny komponent `.MainActivity` i nie potrzebuje ikony. Nie wyłączamy pakietu ani usługi Accessibility; po starcie pobierania zachowujemy jej wyłączenie i zapis stanu.

Uwaga: brak ikony również **przed** rozpoczęciem demo. Jeśli DPC nie uruchomi Helpera, trzeba skorzystać z ręcznego otwarcia go w DPC podczas gdy Wrapper nadal jest zainstalowany. Urządzenia OEM mogą zachować własne zachowania i wymagają testu.

Próba nawigacji do „Zainstalowane aplikacje” jest warunkowa: wyłącznie eksportowane i dostępne aktywności Settings, w przeciwnym razie standardowa strona Dostępności. Nie ma automatycznego włączania usługi ani animowanego podświetlania elementów systemowego UI przed zgodą.

# Retail Helper 0.2.8 — odświeżanie ikony w One UI

**Status: tylko projekt źródłowy, niekompilowany i nietestowany na Galaxy S26.**

**Nowość 0.2.8:** po starcie pobierania demo i wyłączeniu Accessibility komponent `LauncherActivity` jest wyłączany z flagą `0` zamiast `DONT_KILL_APP`. Android może wtedy zakończyć proces, co ma pomóc odświeżyć listę wszystkich aplikacji One UI. To hipoteza do sprawdzenia, nie gwarancja działania na Samsungu. Pozostałe komponenty i pakiet Helpera pozostają zainstalowane.

## Cel

Zachowuje sprawdzony automatyczny START i konfigurację Retail Mode z 0.2.5 oraz ukrywanie ikony z projektu 0.2.6. **Helper nie odinstalowuje się**: po potwierdzeniu pobierania wyłącza Accessibility i wyłącza tylko komponent ikony.

### Nowy protokół DPC 0.9.8

1. Helper potwierdza rzeczywiste otwarcie i pozwala DPC zwolnić Device Owner.
2. DPC wysyła `RELEASE_RESULT_095` z `silent_probe_098=true`.
3. Helper weryfikuje podpis i nonce, zapisuje stan zwolnienia trwale i potwierdza odbiór przez `RELEASE_RECEIVED_098`.
4. DPC dopiero teraz może spróbować `PackageInstaller.uninstall(self)`; Helper czeka maksymalnie 9 s, nie otwierając konkurencyjnego okna.
5. Jeśli DPC nadal jest zainstalowany, Helper przechodzi do **systemowego potwierdzenia odinstalowania**. Jeśli DPC zniknął, Helper nie otwiera okna.

To jest eksperyment: Android może wymagać zgody; nie ma gwarancji cichej deinstalacji. Nie przekazujemy Device Ownera do Helpera.

### Kompilacja i wydanie

W Android Studio otwórz projekt i wybierz `Build > Build APK(s)`. Po skompilowaniu prześlij APK do weryfikacji podpisu i wersji. Publikację oznacz `helper-027-test`. Następnie w kolejnym DPC należy przypiąć URL i SHA-256 opublikowanego pliku (protokół pozostaje zgodny z 0.9.8).

### Bezpieczeństwo i zakres

Wyłącznie autoryzowane urządzenie testowe. DPC 0.9.5 + Helper 0.2.5 pozostają potwierdzoną bazą. Funkcje nowego protokołu i ukrywania ikony wymagają testu na S26.
