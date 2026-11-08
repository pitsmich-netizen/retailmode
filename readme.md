# retailmode — Samsung Retail Mode (projekt testowy)

Projekt: **DPC Wrapper → Retail Helper → Samsung Retail Mode**, uruchamiany przez QR Device Owner na autoryzowanym urządzeniu testowym.

## Aktualna struktura

- `android/retail-dpc/` — źródła Wrappera, który przygotowuje telefon, pobiera pozostałe aplikacje i zwalnia Device Ownera przed uruchomieniem demo.
- `android/retail-helper/` — źródła Helpera, który po ręcznym nadaniu Dostępności prowadzi konfigurację demo, a następnie wyłącza usługę i ma ukryć ikonę.
- `provisioning/` — celowo zachowane, wcześniej zweryfikowane APK pod adresami RAW. **Nie usuwać**, bo wcześniejsze kody QR wskazują na te ścieżki.
- `docs/AUTOMATYZACJA_CI.md` — kompilacja, podpisy i testowe publikacje.

## GitHub Actions

Jedyny aktywny pipeline Android: [Build and publish Retail test APKs from sources](.github/workflows/retail-build-from-source.yml). Po zmianie źródeł automatycznie przygotowuje parę APK, przypina SHA-256 Helpera w DPC i publikuje **testowe** wydanie GitHub Releases z `DEPLOYMENT.txt` oraz linkami RAW.

**Automatyczna kompilacja i publikacja źródłowa zakończyła się sukcesem** w testach [ci-build-42474697059](https://github.com/pitsmich-netizen/retailmode/releases/tag/ci-build-42474697059) i [ci-build-42475090083](https://github.com/pitsmich-netizen/retailmode/releases/tag/ci-build-42475090083). W obu przypadkach powstały APK, sumy SHA-256 i plik DEPLOYMENT.txt. Były to kompilacje testowe; nie oznaczają potwierdzenia na fizycznym telefonie.

Starszy test automatycznej publikacji istniejących APK również się udał: [ci-smoke-20261108](https://github.com/pitsmich-netizen/retailmode/releases/tag/ci-smoke-20261108).

## Status urządzenia

- Wcześniej sprawdzona baza: **DPC 0.9.5 + Helper 0.2.5** — instalacja, przekazanie sterowania, automatyczna konfiguracja Retail Mode i odinstalowanie DPC z potwierdzeniem.
- Sprawdzona na S26 baza: **DPC 0.10.4-ci + Helper 0.2.10** (działa Retail Mode, Toasty, usunięcie z potwierdzeniem, wyłączenie Accessibility i brak ikon). Wersja 0.10.5-ci + Helper 0.2.11 również zakończyła aktywację pomyślnie, ale bez poprawy głębokiego skrótu Dostępności. **Najnowsze źródła DPC 0.10.9-ci + Helper 0.2.14**: pobieranie APK dopiero po zakończeniu kreatora Androida i zweryfikowaniu sieci; krótkie retry, paski postępu, hasła QR i odinstalowanie DPC pozostają bez zmian. Wymaga testu na S26.
- Cicha deinstalacja DPC **nie została osiągnięta**; Android może wyświetlić okno potwierdzenia.
- Ukrycie ikony Helpera 0.2.9 i pełna stabilność dystrybucji przez RAW wymagają testu S26.
- Samsung Retail Mode to niezmienione APK `rmtest`, ale pobierane przez **GitHub RAW** (34 MB, z SHA-256 i preflightem CI), aby ominąć błędy 504 pobierania z Releases.

## Bezpieczeństwo

Repozytorium jest **publiczne**. Nie dodawaj tu haseł Wi-Fi, kodów QR zawierających hasło, haseł Retail Mode, plików `.keystore`/`.jks`, prywatnych kluczy ani danych sklepu. Workflow używa wyłącznie testowego podpisywania. Nie jest to wersja produkcyjna.

Historyczne źródła i dokumenty usunięte z bieżącego drzewa pozostają w historii commitów. Starsze Releases zachowano jako punkty odniesienia.
