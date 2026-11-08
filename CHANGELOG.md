# CHANGELOG — retailmode repository

## 2026-11-08 — GitHub Actions automation (test)

- Automatyczna publikacja prerelease `ci-smoke-20261108`, zweryfikowana sukcesem workflow.
- Dodano źródłowy pipeline `retail-build-from-source.yml`, uruchamiany po imporcie projektów lub zmianach kodu.
- Pipeline planuje automatyczne budowanie obu APK, przypięcie SHA-256 Helpera w DPC, zgodność certyfikatów, niezmienne GitHub RAW i publikację testową.
- Przygotowano pakiet źródeł DPC 0.10.2-ci + Helper 0.2.9 do jednorazowego importu.
- Źródłowy pipeline nie jest jeszcze uruchomiony na GitHubie, do czasu wprowadzenia źródeł.
- Obecne, przetestowane na telefonie wersje i ich QR pozostają bez zmian.
