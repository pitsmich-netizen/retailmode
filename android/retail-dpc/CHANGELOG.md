# 0.10.2-ci (experimental source build)
- Allows immutable commit-pinned Helper APK in trusted GitHub provisioning/ci path; retains signature and sha checks.
- CI testing only; not tested on S26.

# CHANGELOG — Retail DPC

## 0.10.1 — testowy raw mirror (2026-11-08)

- VersionCode 24, versionName `0.10.1-helper029-rawmirror`.
- Domyślne pobieranie Helpera 0.2.9 przeniesione z GitHub Releases na dokładnie `https://raw.githubusercontent.com/pitsmich-netizen/retailmode/main/provisioning/RetailHelper-029.apk`.
- `HelperConfig.valid()` akceptuje wąsko określony adres raw repozytorium (oraz istniejącą ścieżkę Releases); zachowano SHA-256.
- `app/build.gradle` dopuszcza poprawnie zweryfikowany raw mirror.
- Skopiowany APK ma zweryfikowany SHA-256 `5382663fd48cd77ee805bb4ad272ebcd8e8ddc1ef1e978a1f40dc7cf59351220`.
- Bez zmian w zwalnianiu Device Ownera, kolejności handoff i logice instalacji Retail Mode.
- Status: źródła przygotowane; APK jeszcze nie zbudowany i nieprzetestowany.

# CHANGELOG

## 0.10.0 (2026-11-08, skonfigurowany projekt; APK jeszcze nieskompilowany i nieprzetestowany)
- Przejściowe powiadomienie stanu instalacji i tekstowe Toasty w tle na kluczowych etapach.
- Brak dodatkowego overlay/Accessibility, brak obiecywania nieprzerwanego Toastu.
- Tymczasowe nadanie POST_NOTIFICATIONS przez DPC, anulowanie powiadomienia przy zwolnieniu Ownera.
- Bazą DPC 0.9.9; przypięto opublikowany Helper 0.2.9 (73 627 B, SHA-256 `5382663fd48cd77ee805bb4ad272ebcd8e8ddc1ef1e978a1f40dc7cf59351220`).
- `versionCode 23`, `versionName 0.10.0-helper029-progress`. QR generator, protokół handoff i samousuwanie DPC bez zmian.

# 0.9.x (archiwalne wpisy)

## 0.9.9 — przetestowano na S26 (2026-11-08)
- Przypięto opublikowany i zweryfikowany Helper 0.2.8: `helper-028-test`, SHA-256 `c7e52b3f676c81cd12368aa9179af04181e24e84a32b6517b8a3469ef68ffe1d`.
- `versionCode 22`, `versionName 0.9.9-helper028-uninstall-probe-handshake`.
- Zachowano kod i protokół zwalniania Device Owner oraz `PackageInstaller` z 0.9.8; nie próbowano zmieniać polityk Androida.
- Celem testu jest ukrycie ikony Helpera na Samsung One UI; 0.2.8 został następnie przetestowany na S26: ikona pozostała w szufladzie aplikacji i otwierała informacje o aplikacji.

## 0.9.8 — przetestowano na Galaxy S26 (2026-11-08)
- DPC przekazuje sterowanie Helperowi 0.2.7; Helper automatycznie konfiguruje demo.
- Potwierdzono: próba bezobsługowego usunięcia DPC nie działała; pojawiało się systemowe okno potwierdzenia.
- Helper 0.2.7 wyłączał możliwość uruchomienia z ikony, ale ikona nadal była widoczna na liście aplikacji.
- Protokół `RELEASE_RECEIVED_098` usuwa błąd kolejności zdarzeń w wersji 0.9.7.

## 0.9.7 — eksperyment, NIE publikować
- Eksperyment `PackageInstaller.uninstall(self)` miał błąd wyścigu; zastąpiony 0.9.8.

## 0.9.5 — przetestowano na Galaxy S26
- Handoff, zwolnienie Device Owner i systemowa deinstalacja DPC z potwierdzeniem.
