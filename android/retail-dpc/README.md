# Retail DPC 0.10.2-ci — eksperymentalny build automatyczny

- Pakiet: `com.example.retaildpc`
- `versionCode 25`
- Helper: `com.example.retailhelper` 0.2.9 (versionCode 15)
- Kompilacja: [workflow Android CI](../../.github/workflows/retail-build-from-source.yml)

## Co robi DPC

Przyjmuje QR Device Owner, przygotowuje telefon, pobiera i instaluje Samsung Retail Mode i Helpera, przekazuje sterowanie Helperowi i zwalnia uprawnienia Device Ownera przed konfiguracją demo. Próba cichej deinstalacji DPC nadal **nie** działała na S26; system może wymagać potwierdzenia.

Poza tym udostępnia testowe komunikaty o postępie i diagnostykę.

## Helper

CI najpierw buduje Helpera, wylicza jego SHA-256, a potem ustawia w pliku ignorowanym przez Git `helper-apk.properties` adres RAW z dokładnym SHA commitu. DPC weryfikuje adres, SHA-256 i zgodność z oczekiwanym APK.

Domyślne ustawienia (przy lokalnym budowaniu bez tego pliku) pochodzą z `helper-apk.defaults.properties`. Nie wpisuj haseł do publicznego repo.

## Testowanie

`DPC 0.10.2-ci` to **nieprzetestowany na telefonie wariant źródłowy**. Po zakończonej kompilacji przeprowadzić kontrolę podpisów, danych provisioning, pobrania wszystkich APK oraz pełny test na Galaxy S26. Wcześniejsza wersja 0.10.1 korzystała z Helpera 0.2.9 przez RAW.

Nie przekazywać Device Ownera Helperowi. Helper po rozpoczęciu pobierania demo ma wyłączyć Dostępność i ukryć ikonę, ale pozostać zainstalowany.
