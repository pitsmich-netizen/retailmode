# Jednorazowy import źródeł Android

Repozytorium początkowo zawiera APK, ale nie zawiera projektów Android Studio.

Aby uruchomić pełną automatyzację, prześlij tutaj plik pod nazwą:

`retail-ci-sources.zip`

Pakiet powinien zawierać:
- `android/retail-dpc/` — projekt Gradle DPC (wersja 0.10.2-ci test)
- `android/retail-helper/` — projekt Gradle Helper (wersja 0.2.9)
- `CI_START.md`

Workflow `.github/workflows/retail-build-from-source.yml` wykryje ten plik, automatycznie:
1. rozpakowuje źródła i zapisuje je w repozytorium,
2. kompiluje Helpera,
3. zapisuje niezmienny plik APK pod unikalnym adresem RAW,
4. przypina w DPC URL i SHA-256 dokładnie tego Helpera,
5. kompiluje DPC z tym ustawieniem,
6. porównuje certyfikaty obu APK,
7. publikuje eksperymentalne wydanie `ci-build-<run-id>` i adresy RAW.

**Bezpieczeństwo:** repozytorium jest publiczne. Nigdy nie dodawaj hasła Wi-Fi, hasła Retail Mode, kluczy .jks/.keystore ani innych sekretów.

**Podpisy:** domyślnie workflow używa tymczasowego klucza debug na czas jednego uruchomienia (wymagany czysty telefon). Można przekazać stabilny *testowy* klucz przez sekret Actions `RETAIL_CI_DEBUG_KEYSTORE_B64`. Nie jest to podpis produkcyjny.

Każde następne zapisanie zmian w źródłach `android/**` będzie samo uruchamiać nową kompilację i testowe wydanie. Nie ma potrzeby ręcznego wrzucania pojedynczych APK.
