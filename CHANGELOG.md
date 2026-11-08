# CHANGELOG — retailmode

## 2026-11-08 — porządki i CI

- Oba projekty Android Studio zaimportowane do `android/`.
- Usunięto zbędny ZIP bootstrap, jednorazowe workflowy kopiowania/publikacji oraz stare, powielone instrukcje wersji DPC. Historię zachowano w Git.
- Pozostawiono stare Releases i trzy binaria RAW wykorzystywane przez wcześniejsze kody QR.
- Naprawiono inicjalizację Android SDK w workflow źródłowej kompilacji (poprzednio próba instalacji wycofanego `tools`).
- Zaktualizowano README, dokumentację CI i instrukcje bieżącej wersji DPC 0.10.2-ci + Helpera 0.2.9.
- Pierwsza automatyczna publikacja **bez kompilacji** zakończyła się sukcesem: `ci-smoke-20261108`.
- Po naprawie SDK dwa pełne buildy z kodu zakończyły się sukcesem: `ci-build-42474697059` i `ci-build-42475090083` (oba zawierają DPC, Helper, SHA256SUMS i DEPLOYMENT).
- Kod bieżącej wersji nadal wymaga pełnego testu na Galaxy S26.
