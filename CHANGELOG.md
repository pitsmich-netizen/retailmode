# CHANGELOG — retailmode

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
