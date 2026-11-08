# Retail Helper 0.2.9 — Samsung One UI

Pakiet: `com.example.retailhelper` • versionCode 15.

Helper jest instalowany i uruchamiany przez DPC (jawny komponent `.MainActivity`). Użytkownik musi świadomie przyznać usłudze Dostępności wymaganą zgodę. Po jej udzieleniu Helper automatycznie rozpoczyna konfigurację Samsung Retail Mode.

## Ukrywanie ikony

Alias LAUNCHER jest **domyślnie wyłączony** w manifeście. Celem jest uniknięcie zastępczej ikony prowadzącej do „Informacje o aplikacji” w Samsung One UI. Helper **pozostaje zainstalowany**, ale po rozpoznaniu początku pobierania demo wyłącza własną usługę Dostępności.

To zachowanie wymaga praktycznego potwierdzenia na S26 w wersji 0.2.9. Nie twierdzimy, że ikona zniknęła bez testu.

## Nawigacja i komunikaty

Helper próbuje otworzyć możliwie bliski ekran ustawień zainstalowanych usług Dostępności; jeśli Samsung go nie udostępnia, otwiera ogólną stronę Dostępności. Nie uzyskuje automatycznie zgody i nie modyfikuje systemowego interfejsu. Zachowano komunikaty etapów i automatyczny START.

## Integracja DPC

Protokół potwierdzenia uruchomienia, zwolnienia Device Owner i próby usuwania DPC jest eksperymentalny. Ciche usunięcie DPC nie udało się na S26; systemowy prompt może być konieczny.

Kompilacja i publikacja testowa: [GitHub Actions](../../.github/workflows/retail-build-from-source.yml). Starsze szczegóły historyczne są w [CHANGELOG.md](CHANGELOG.md).
