# 0.8 — Read-only cleanup readiness

- Weryfikacja własnego Device Owner i informacji o docelowym Retail Mode.
- Weryfikacja deklaracji `DeviceAdminInfo.supportsTransferOwnership()` dla każdego admin receivera Retail Mode.
- Informacja, czy dany komponent jest aktywnym administratorem.
- Raport kopiowany do schowka; bez sekretów, bez Wi-Fi i bez Site ID.
- Nie wywołuje żadnych metod zmieniających stan zarządzania.
- `versionCode 9`, `applicationId` i nazwa AdminReceiver bez zmian.
- Brak poprzedniego lokalnego pliku z hasłem testowym w ZIP.
