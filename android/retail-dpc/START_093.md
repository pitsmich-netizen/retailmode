# Retail DPC 0.9.3 — naprawa instalacji Helpera

Wersja bazowa: dostarczony projekt 0.9.2 (ten sam `applicationId` i `AdminReceiver`).
**Nie modyfikuje kodu działającego Helpera 0.2.1.**

## Co zmieniono
1. Domyślny Helper to `helper-021-stable` (nie niepoprawny `helper-010-test`).
2. SHA-256: `a0423d56d505f1335415f6860ba845bbed8fe217952cfc5c2de7976cd7b5ced6`.
3. `helper-apk.defaults.properties` jest w projekcie i nie jest ignorowany przez Git;
   budowa w GitHub Actions nie traci zatem konfiguracji Helpera.
4. DPC odczytuje `helper_url` i `helper_sha256` z `PROVISIONING_ADMIN_EXTRAS_BUNDLE` w callbackach
   `GET_PROVISIONING_MODE`, `ADMIN_POLICY_COMPLIANCE` i `PROFILE_PROVISIONING_COMPLETE`.
5. Adres z QR musi wskazywać plik `RetailHelper.apk` w wydaniach repozytorium
   `pitsmich-netizen/retailmode`, a suma SHA-256 musi mieć 64 znaki hex.
6. Instalator zawsze weryfikuje SHA-256 pobranego APK oraz pakiet `com.example.retailhelper`.

## Jak zbudować
- Rozpakuj ZIP i otwórz folder `RetailDPC_093_Helper021_AndroidStudio` w Android Studio.
- `Build > Build APK(s)` (debug). Wynik: `app/build/outputs/apk/debug/app-debug.apk`.
- To APK musi być opublikowane w nowym GitHub Release np. `dcp-093-test` jako `RetailDcp.apk`.
- **Nie używaj starego QR.** Nowy QR musi zawierać URL i SHA-256 **nowo zbudowanego DPC 0.9.3**.
- Nie wolno używać SHA-256 Helpera jako checksum dla DPC.
- Hash Helpera jest już wpisany w projekt; hash DPC poznasz dopiero po kompilacji.

## Ochrona przed zbędnym resetem
1. S26 może pozostać w obecnym stanie do czasu zbudowania i opublikowania DPC 0.9.3.
2. Nie resetuj go tylko po to, by ponowić instalację 0.9.2 — ona wciąż używa starego linku.
3. Na nowym provisioningu sprawdź w diagnostyce, czy DPC wskazuje `helper-021-stable`.
4. Potwierdź instalację Retail Mode i Retail Helper; Android może nadal wymagać
   ręcznego włączenia Accessibility po instalacji.
5. Dalej użyj przetestowanego Helpera 0.2.1 i naciśnij START.

## Uwaga o QR i bezpieczeństwie
- QR z hasłem Wi-Fi zawiera je w zwykłym tekście; nie publikuj go na GitHubie.
- QR z innym Helperem może wskazywać inną wersję — weryfikuj źródło i SHA przy każdej zmianie.
- Nowy kod nie jest jeszcze przetestowany na urządzeniu ani skompilowany w tym środowisku.
