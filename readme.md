# retailmode — Samsung Retail Mode automation (experimental)

Testowy projekt DPC Wrapper + Retail Helper + QR Device Owner dla urządzeń Samsung.

## Wersje i stan

- DPC 0.10.1 (przesłany APK w rozmowie) + Helper 0.2.9 — testy dystrybucji przez GitHub RAW, brak pełnego testu tej pary na S26.
- DPC 0.10.0 + Helper 0.2.9 — zweryfikowane APK zapisane jako niezmienione kopie w `provisioning/`.
- DPC 0.9.5 + Helper 0.2.5 — wcześniejsza, praktycznie przetestowana podstawa konfiguracji S26.
- Cicha deinstalacja DPC nie została potwierdzona; Android wyświetla systemową zgodę. Późniejsze poprawki ikony Helpera nie są jeszcze zweryfikowane na urządzeniu.

## Automatyzacja GitHub

- **Działająca:** workflow `publish-verified-test-apks.yml` bez udziału użytkownika stworzył i zweryfikował [prerelease ci-smoke-20261108](https://github.com/pitsmich-netizen/retailmode/releases/tag/ci-smoke-20261108) z DPC 0.10.0, Helperem 0.2.9 oraz SHA256SUMS.txt.
- **Przygotowana, jeszcze nieuruchomiona:** `retail-build-from-source.yml` buduje i publikuje obie aplikacje po jednorazowym imporcie źródeł; następne aktualizacje tekstowego kodu będą uruchamiać go automatycznie.
- [Pełna dokumentacja CI](docs/AUTOMATYZACJA_CI.md) i [folder jednorazowego importu](bootstrap/README.md).

Nigdy nie publikuj hasła Wi-Fi, hasła Retail Mode ani klucza podpisu w tym publicznym repozytorium. Wydania CI są wyłącznie testowe i mogą wymagać wyczyszczenia urządzenia przed instalacją.
