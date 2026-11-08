@echo off
cd /d "%~dp0"
echo Przygotowanie Gradle Wrapper JAR 8.13 z oficjalnego repozytorium GitHub...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\prepare-gradle.ps1"
if errorlevel 1 (
  echo BLAD: Sprawdz polaczenie z internetem lub komunikat powyzej.
) else (
  echo GOTOWE: Otworz ten folder w Android Studio, ustaw Gradle JDK 17, potem Sync.
)
pause
