@echo off
@rem Small Gradle Wrapper launcher. Official wrapper JAR downloaded + SHA256-verified by tools/prepare-gradle.ps1.
setlocal
set "APP_HOME=%~dp0"
if not exist "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" (
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%APP_HOME%tools\prepare-gradle.ps1"
  if errorlevel 1 exit /b 1
)
if defined JAVA_HOME (
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA_EXE=java.exe"
)
"%JAVA_EXE%" -Dorg.gradle.appname=gradlew -classpath "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
