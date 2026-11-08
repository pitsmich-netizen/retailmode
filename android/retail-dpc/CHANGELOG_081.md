# Retail DPC 0.8.1 — poprawka kompilacji

- Usunieto niedostepne w publicznym SDK `DevicePolicyManager.getDeviceOwnerComponentOnCallingUser()`.
- Diagnostyka wyswietla lokalny `AdminReceiver`, sprawdza aktywnosc (`isAdminActive`) i status Device Owner (`isDeviceOwnerApp`).
- Podniesiono `versionCode` z 9 do 10, `versionName` na `0.8.1-cleanup-readonly`.
- Brak operacji transferu/usuwania i innych zmian polityk.
