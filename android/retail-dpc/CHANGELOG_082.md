# 0.8.2 — TEST RELEASE (nieprodukcyjny)

- `CleanupFlow.java`: raport, przywracanie fabrycznych aplikacji z ikonami i kontrolowany `clearDeviceOwnerApp()`.
- Interfejs `MainActivity`: dwa potwierdzenia, 3 checkboxy, fraza kontrolna, diagnostyka i ręczna dezaktywacja Device Administrator dopiero po zwolnieniu DO.
- Scheduler/BootReceiver/InstallJobService: nie uruchamiają instalacji po rozpoczęciu procesu cleanup.
- Manifest: dodatkowa widoczność popularnych aplikacji systemowych.
- Nie dodano automatycznego odinstalowania ani nowego hasła.
