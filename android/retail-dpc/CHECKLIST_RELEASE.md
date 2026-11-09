# Lista kontrolna — Retail Setup All-In-One 0.11.0-ci

- [ ] CI publikuje **jeden** podpisany `RetailSetup.apk` wraz z SHA-256.
- [ ] QR pobiera dokładnie ten APK przez niezmienny URL RAW i weryfikuje checksum.
- [ ] Galaxy S26: provisioning Device Owner i Wi-Fi przechodzą poprawnie.
- [ ] Po zakończeniu kreatora Androida wyświetla się **Retail Setup** jako tymczasowy HOME (bez twardego kiosku).
- [ ] Jeden START inicjuje albo kontynuuje cały proces.
- [ ] Widoczny jest prawdziwy procent pobierania Retail Mode i bieżący etap.
- [ ] Po instalacji Retail APK przywracany jest systemowy HOME i zwalniany Device Owner oraz aktywny administrator.
- [ ] Działa ta sama aplikacja; nie ma Helpera ani pytania o odinstalowanie DPC.
- [ ] Ręczne włączenie Retail Setup w Dostępności i automatyzacja kraju/partnera/sklepu/hasła/profilu Experience działają.
- [ ] Po rozpoczęciu pobierania demo Accessibility wyłącza się i znika ikona Retail Setup.
- [ ] Retail Mode działa po restarcie; nie pozostały trwałe ograniczenia Androida.
- [ ] Awaryjne przywracanie pulpitu w opcjach zaawansowanych nie blokuje urządzenia.
