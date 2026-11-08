# Automatyzacja CI — Retail Mode

## Bieżący workflow

`.github/workflows/retail-build-from-source.yml`

Wyzwalacze:
- zmiany w `android/retail-dpc/**` i `android/retail-helper/**`;
- aktualizacja samego workflow;
- ręczne uruchomienie `workflow_dispatch`.

Obydwa projekty zostały zaimportowane do repozytorium. Jednorazowy plik bootstrap został usunięty, ponieważ nie jest potrzebny przy kolejnych buildach.

### Przebieg

1. Przygotowanie JDK 17, Android SDK i Gradle.
2. Kompilacja Helpera w trybie **Debug / TEST**.
3. Zapis jego APK pod ścieżką `provisioning/ci/<run-id>/RetailHelper.apk` i obliczenie SHA-256.
4. Przypięcie niezmiennego URL Helpera (z konkretnym commit SHA) i checksumy do konfiguracji DPC.
5. Kompilacja DPC w trybie Debug.
6. Weryfikacja podpisów APK oraz zgodności certyfikatów.
7. Zapis APK DPC pod ścieżką `provisioning/ci/<run-id>/RetailDcp.apk`.
8. Automatyczna publikacja testowego `ci-build-<run-id>` z plikami `RetailDcp.apk`, `RetailHelper.apk`, `SHA256SUMS.txt` oraz `DEPLOYMENT.txt`.

**Ważne:** publikacja w CI nie potwierdza instalacji Device Owner na telefonie. Po każdym istotnym buildzie testować na autoryzowanym Samsungu.

## Podpisy

Opcjonalny sekret repozytorium `RETAIL_CI_DEBUG_KEYSTORE_B64` umożliwia stosowanie stałego **testowego** klucza podpisywania. Bez niego debug.keystore może być generowany od nowa, przez co APK z różnych uruchomień nie będą kompatybilne jako aktualizacje.

Nie commitować kluczy, haseł ani `release-signing.properties`. Nie publikować QR z hasłem Wi-Fi.

## Przetestowane funkcje infrastruktury

- Kopiowanie APK z Releases przez API GitHub i kontrola SHA-256 — sukces.
- Automatyczna publikacja istniejących, sprawdzonych APK jako wydania `ci-smoke-20261108` — sukces.
- Jednorazowy import obu projektów Android Studio — sukces.
- Pierwszy build źródłowy — zatrzymał się na przestarzałym kroku Android SDK `tools`. Błąd usunięto.
- **Buildy źródłowe po poprawce — sukces:** [42474697059](https://github.com/pitsmich-netizen/retailmode/actions/runs/42474697059) oraz [42475090083](https://github.com/pitsmich-netizen/retailmode/actions/runs/42475090083). Wydania: `ci-build-42474697059` i `ci-build-42475090083`.
- Po usunięciu jednorazowych workflowów repozytorium ma tylko jedną aktywną ścieżkę kompilacji, a poprzednie APK i QR pozostają używalne.

## Co pozostaje do weryfikacji

- Automatyczny build i publikacja obydwu APK end-to-end po imporcie źródeł.
- Stały klucz testowego podpisu przed próbami aktualizacji aplikacji.
- Poprawne pobieranie DPC, Helpera i Retail Mode przez kreator Samsung Setup Wizard.
- Na S26: przekazanie sterowania, zwolnienie Device Ownera, potwierdzenie usunięcia DPC, konfiguracja demo i ukrycie ikony Helpera.

## Zachowane testowe binaria

- `provisioning/RetailDcp-099.apk` — wcześniejszy diagnostyczny QR z URL RAW.
- `provisioning/RetailDcp-100.apk` — wcześniejszy testowy DPC.
- `provisioning/RetailHelper-029.apk` — zweryfikowana kopia Helpera 0.2.9.

Nie usuwać bez uprzedniego wycofania powiązanych QR.

## 2026-11-08 — naprawa HTTP 504 dla Retail Mode

Oryginalny plik z wydania `rmtest` został automatycznie skopiowany do `provisioning/RetailMode.apk` po weryfikacji rozmiaru 34 192 143 bajtów i SHA-256 `da5ede9aa02aa278fa82b97caba8eb2e79a4e6fea9bdc5e3b50ebb3e5bc245c4`.

Od DPC 0.10.3-ci zamiast adresu GitHub Releases używamy commit-pinned adresu RAW:

`https://raw.githubusercontent.com/pitsmich-netizen/retailmode/58bed6b59596e1ed82bb2526bbdb202f114a3391/provisioning/RetailMode.apk`

Workflow kompilacji weryfikuje odczyt pliku z tego RAW i jego SHA-256 przed budowaniem APK. Nie należy usuwać `provisioning/RetailMode.apk` ani wykorzystywanego commita z historii repo. Zmiana nie naprawia już zainstalowanych DPC 0.10.2-ci; wymaga nowej kompilacji i konfiguracji testowego telefonu.
