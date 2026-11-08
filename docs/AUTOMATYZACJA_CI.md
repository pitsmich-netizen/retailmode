# Automatyzacja Retail Mode — stan 2026-11-08

## Już przetestowane: automatyczna publikacja bez ręcznego wgrywania APK

Workflow: `.github/workflows/publish-verified-test-apks.yml`.

Test `ci-smoke-20261108` zakończył się sukcesem: GitHub Actions wziął już istniejące, zweryfikowane pliki `provisioning/RetailDcp-100.apk` i `provisioning/RetailHelper-029.apk`, sprawdził ich SHA-256 i sam opublikował prerelease wraz z `SHA256SUMS.txt`.

To był test **automatycznej publikacji**, a nie kompilacji z kodu.

## Dodane: automatyczna kompilacja i publikacja z kodu

Workflow: `.github/workflows/retail-build-from-source.yml`.

Uruchamia się po jednorazowym imporcie ZIP do `bootstrap/retail-ci-sources.zip`, po kolejnych zmianach kodu w `android/retail-dpc/` lub `android/retail-helper/`, a także ręcznie (`workflow_dispatch`).

Po imporcie automatyzuje:
- budowę obu aplikacji przez Android Gradle Plugin i JDK 17;
- użycie jednego klucza debug do obu APK;
- obliczenie SHA-256 Helpera i wpisanie go do konfiguracji DPC;
- zapis obydwu APK pod wersjonowanymi, niezmiennymi adresami GitHub RAW, z przypięciem commit SHA;
- sprawdzenie kryptograficznego podpisu obu plików oraz zgodności certyfikatów;
- publikację testowego GitHub prerelease z manifestem sum i adresów `DEPLOYMENT.txt`.

Ten etap **czeka na jednorazowy import źródeł** i nie został jeszcze przetestowany end-to-end.

## Klucze i ograniczenia

* Zmienna `RETAIL_CI_DEBUG_KEYSTORE_B64` jest opcjonalnym sekretem GitHub Actions z zapisanym w base64 plikiem debug.keystore. Nie wpisuj zawartości tego pliku do repozytorium.
* Bez sekretu Gradle tworzy nowy klucz debug dla danego uruchomienia. Wersje z różnych uruchomień mogą mieć różne podpisy i nie nadadzą się do aktualizowania zainstalowanych aplikacji.
* Zgodne certyfikaty DPC i Helpera są wymagane dla ich komunikacji. Workflow przerwie publikację w przypadku rozbieżności.
* `DPC 0.10.2-ci` to nowa, **eksperymentalna** odmiana źródeł dopuszczająca tylko zaufany, commit-pinned URL Helpera. Nie została jeszcze przetestowana na S26.
* Nie podłączamy klucza produkcyjnego i nie publikujemy automatycznie żadnych haseł.
* Wydania Releases mogą nadal zwracać HTTP 504 u części klientów. Do QR używamy adresów RAW podanych w `DEPLOYMENT.txt`, dopiero po sprawdzeniu możliwości pobrania.
* QR z hasłem Wi-Fi należy przygotować poza publicznym CI albo użyć prywatnych sekretów po świadomej konfiguracji. Obecny workflow celowo nie generuje publicznie QR zawierającego hasła.
* Nic w automatyzacji GitHub nie potwierdza poprawnego działania funkcji Device Owner i Accessibility na fizycznym telefonie; taki test pozostaje oddzielny.

## Status testów

- [x] Kopiowanie i weryfikacja APK z GitHub Releases przez API.
- [x] Automatyczna publikacja istniejących APK w nowym wydaniu testowym.
- [x] Udostępnienie aktualnych DPC 0.10.0 i Helpera 0.2.9 przez RAW.
- [ ] Import projektów Android Studio do repozytorium.
- [ ] Automatyczna kompilacja i podpisanie obu aplikacji.
- [ ] Nowy testowy QR wskazujący automatycznie zbudowany DPC.
- [ ] Test całego scenariusza na Galaxy S26.
