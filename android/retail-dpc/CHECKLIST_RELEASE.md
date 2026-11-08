# Lista kontrolna — testowy build CI

- [ ] GitHub Actions `retail-build-from-source.yml` zakończony sukcesem.
- [ ] W wydaniu `ci-build-<run-id>` są obydwa APK, `SHA256SUMS.txt` i `DEPLOYMENT.txt`.
- [ ] SHA-256 DPC i Helpera zgodne z manifestem.
- [ ] Oba pliki są podpisane i mają zgodny certyfikat.
- [ ] DPC ma poprawny URL oraz SHA-256 zbudowanego Helpera.
- [ ] Adresy RAW pobierają się prawidłowo przed wygenerowaniem QR.
- [ ] Samsung Retail Mode APK jest nadal dostępny i pobieralny.
- [ ] S26: konfiguracja QR Device Owner przechodzi instalację.
- [ ] S26: DPC instaluje Retail Mode oraz Helpera i przekazuje sterowanie.
- [ ] S26: DPC zwalnia uprawnienia i usuwa się (może wymagać kliknięcia „Odinstaluj”).
- [ ] S26: po ręcznym nadaniu Accessibility Helper automatycznie konfiguruje demo.
- [ ] S26: Accessibility jest wyłączone po rozpoczęciu pobierania, ikona Helpera nie jest widoczna.
- [ ] Brak pozostawionego Device Ownera i niepożądanych trwałych polityk.
