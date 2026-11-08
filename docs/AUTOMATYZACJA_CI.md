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
- Pierwszy build źródłowy — zatrzymał się na przestarzałym kroku Android SDK `tools`. Workflow zaktualizowano, aby korzystał z SDK runnera; kolejne uruchomienia trzeba oceniać według wyników GitHub Actions.

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
