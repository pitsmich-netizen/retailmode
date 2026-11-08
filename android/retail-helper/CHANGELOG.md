# 0.2.9 (CI source)
- No runtime behavior changes.
- CI builds will use the same per-run signing cert as DPC.

# CHANGELOG

## 0.2.9 (2026-11-08, nieprzetestowane)
- Poprawka wyjaśnionego przez AOSP przypadku: android.app.AppDetailsActivity jako zastępcza ikona po wyłączeniu wcześniej domyślnie aktywnej ikony LAUNCHER.
- LAUNCHER alias jest w manifeście domyślnie wyłączony; DPC uruchamia jawnie główne Activity Helpera. Usługa Accessibility i demo pozostają jak dotąd.
- Warunkowe wykrywanie dostępnych, eksportowanych ekranów listy usług Dostępności Settings i bezpieczny fallback.
- Naprawiono utratę Toastu przy zmianie etapu w krótszym czasie niż throttling.
- Bez samodeinstalacji, bez zmian w zgodach i w logice automatyzacji Retail Mode.

# CHANGELOG

## 0.2.8 — wersja źródłowa, NIEPRZETESTOWANA
- Korekta ukrywania ikony na Samsung One UI: po potwierdzonym starcie pobierania, zatrzymaniu automatyzacji i wyłączeniu Accessibility, wyłączamy wyłącznie `LauncherActivity` z `flags=0`, a nie z `DONT_KILL_APP`.
- Android może zakończyć proces Helpera, co powinno odświeżyć stan ikony; zachowano marker ukończenia przed wyłączeniem komponentu.
- `MainActivity`, usługa Accessibility i aplikacja pozostają zainstalowane; nie ma próby odinstalowania Helpera.
- Protokół współpracy z DPC 0.9.8, auto-START i rejestracja demo są bez zmian.
- Ta zmiana nie gwarantuje odświeżenia wewnętrznego cache One UI — wymaga testu na Galaxy S26.

## 0.2.7 — projekt, NIEPRZETESTOWANY
- Dwufazowe potwierdzenie otrzymania stanu zwolnienia Device Owner, zanim DPC podejmie samodeinstalację.
- 9 sekund na wynik eksperymentu; bez równoległego okna deinstalacji.
- Awaryjne zwykłe odinstalowanie DPC, gdy nadal jest obecny.
- Zachowano 0.2.6: ukrycie ikony zamiast samodeinstalacji Helpera; auto-START i Accessibility disable.

## 0.2.6 — baza
- Helper ukrywa własną ikonę po rozpoczęciu pobierania demo.

## 0.2.5 — przetestowano na S26
- Automatyczny START po włączeniu Accessibility, obsługa zwolnienia i zwykłej deinstalacji DPC.
