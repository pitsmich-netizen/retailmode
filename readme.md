# Retail Setup — Samsung Galaxy (All-In-One)

**Aktualny test: 0.11.0-ci.** Jedna aplikacja Android zamiast pary DPC + Helper.

## Proces na uprawnionym urządzeniu testowym

1. QR ustawia Wi-Fi, instaluje Retail Setup i nadaje aplikacji rolę Device Owner.
2. Na końcu kreatora Androida Retail Setup jest tymczasowym ekranem HOME; widoczny jest ciemny interfejs, prawdziwy postęp pobrania Retail Mode i przycisk **START**.
3. START uruchamia / kontynuuje instalację; po potwierdzeniu zainstalowania Retail Mode wycofuje tymczasowy HOME, anuluje instalator i zwalnia rolę Device Owner oraz administratora.
4. Ta **sama aplikacja** kontynuuje konfigurację jako normalny APK. Użytkownik świadomie włącza jej usługę Accessibility w Ustawieniach Androida.
5. Automatyzacja wybiera kraj, partnera, sklep, hasło z QR (jeżeli dostępne) i profil Experience. Po rozpoznaniu rzeczywistego rozpoczęcia pobierania demo usługa Accessibility sama się wyłącza.
6. Ikona aplikacji jest od początku wyłączona w launcherze; aplikacja pozostaje normalnie widoczna w Ustawienia → Aplikacje.

Opcje zaawansowane są zwinięte: zmiana modelu, Site ID, opcjonalne hasło ręczne, ponowienie, Wi-Fi, ustawienia Accessibility, diagnostyka, awaryjne przywrócenie pulpitu.

**Nie używamy twardego Lock Task/kiosku** w tym pierwszym wydaniu. Tymczasowy HOME można awaryjnie przywrócić przyciskiem w opcjach zaawansowanych.

## Katalogi

- `android/retail-dpc/` — jedyny aktywnie kompilowany projekt Android. Nowa automatyzacja znajduje się w `.../com/example/retaildpc/demo/`.
- `provisioning/` — historyczne, przypięte APK, do których mogą nadal odnosić się używane kody QR. **Nie usuwać tych plików.**
- `docs/` — informacje techniczne i historia projektu.
- `.github/workflows/retail-build-from-source.yml` — jedyny aktywny build; publikuje pojedynczy `RetailSetup.apk` wraz z SHA-256.

Wcześniejszy Helper i klasy starego instalatora zostały usunięte z bieżącej gałęzi. Nadal można je odzyskać z historii Git i starszych Releases.

## Bezpieczeństwo i testowanie

- Nie umieszczaj Wi-Fi, haseł Samsung Retail Mode, QR zawierających hasła ani keystore w publicznym repozytorium.
- Wbudowane komponenty aktywują Accessibility **wyłącznie po zgodzie na ekranie Androida**.
- Używany mechanizm `clearDeviceOwnerApp()` jest przestarzałą funkcją testową i może pozostawiać niektóre polityki. Pełny reset urządzenia jest wymagany przy ponownym wdrażaniu QR Device Owner.
- Pomyślna kompilacja **nie potwierdza** działania na fizycznym Samsungu S26. Sprawdź działanie HOME, zdjęcie roli DO, UI, demo oraz restart.
