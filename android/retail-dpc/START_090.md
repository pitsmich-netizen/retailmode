# Retail DPC 0.9.0 — laboratoryjna wersja źródłowa

Wariant oparty na DZIAŁAJĄCYM DPC 0.8.2. **Nie aktualizuj działającego QR i nie resetuj S26 bez nowego testu.**

## Nowe
- Podczas provisioningu (callback DO) jednorazowo włącza systemową lokalizację przez `DevicePolicyManager.setLocationEnabled`.
- Po zainstalowaniu Retail Mode sprawdza, czy APK deklaruje `ACCESS_COARSE_LOCATION`/`ACCESS_FINE_LOCATION` i próbuje nadać tylko te uprawnienia. Nie nadaje wszystkich uprawnień bez kontroli.
- QR: zapisuje bez sekretów partnera, kraj i Site ID w pamięci Wrappera. Nie przesyła jeszcze danych do Helpera.
- Przycisk kontrolny w aplikacji pozwala ponowić konfigurację i zobaczyć wynik.
- Przykład podpisywania release (plik `release-signing.properties` poza Git).
- Zachowano pełną diagnostykę oraz RĘCZNE laboratoryjne zwolnienie Device Owner z 0.8.2.

## Podpis release (Windows, klucz tworzysz na SWOIM komputerze)
1. Stwórz osobny katalog, np. `C:\Users\<user>\retail-signing`.
2. W konsoli Android Studio (JDK 17) wykonaj `keytool -genkeypair -v -keystore "C:\Users\<user>\retail-signing\retail-dpc-release.jks" -alias retail-dpc -keyalg RSA -keysize 3072 -validity 10000`. Podaj silne hasło i zachowaj je w menedżerze haseł.
3. Skopiuj `release-signing.properties.example` do `release-signing.properties` i wpisz prawdziwą ścieżkę, alias i hasła. Plik jest wykluczony z Git.
4. `Build > Generate Signed Bundle / APK` lub Gradle `:app:assembleRelease`. Plik: `app/build/outputs/apk/release/app-release.apk`.
5. W PowerShell uruchom skrypt `tools/Verify-RetailApkSignature.ps1 -Apk ".\app\build\outputs\apk\release\app-release.apk"`. Skrypt poda SHA-256 APK i Base64URL SHA-256 CERTYFIKATU do QR.
6. Utwórz dwie zabezpieczone kopie JKS; nigdy nie wrzucaj go do repo. Utrata klucza uniemożliwia zachowanie podpisu w przyszłych QR.

## Ważne ograniczenia
- Generator 1.1.1 nadal używa SHA pliku; do sumy certyfikatu wymagany jest Generator 1.2.
- Zmieniony podpis APK może wymagać odinstalowania starej wersji DPC przed testem na innej instancji. Nie instaluj release z innym kluczem na istniejącym DO.
- To źródła; brak możliwości kompilacji/testu na Galaxy S26 w tym środowisku. Przed publikacją wymagany build w Android Studio i próba na telefonie testowym.
- Włączenie lokalizacji nie jest zatwierdzeniem wszystkich usług lokalizacyjnych Google/Samsung.
- Czasowe uprawnienia Device Owner i metoda jego zwolnienia pozostają eksperymentalne.
