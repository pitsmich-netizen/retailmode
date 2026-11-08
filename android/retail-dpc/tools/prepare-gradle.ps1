$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$targetDir = Join-Path $root 'gradle\wrapper'
$jarPath = Join-Path $targetDir 'gradle-wrapper.jar'

# Oficjalny plik Gradle 8.13 z repozytorium gradle/gradle, tag v8.13.0.
# Link https://services.gradle.org/distributions/gradle-8.13-wrapper.jar NIE istnieje (404).
$uri = 'https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradle/wrapper/gradle-wrapper.jar'
$expected = '81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f'
New-Item -ItemType Directory -Path $targetDir -Force | Out-Null

if (Test-Path $jarPath) {
    $present = (Get-FileHash -Path $jarPath -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($present -eq $expected) {
        Write-Host 'OK: prawidlowy Gradle Wrapper JAR 8.13 znajduje sie w projekcie.' -ForegroundColor Green
        exit 0
    }
    throw ('Znaleziono niezgodny gradle-wrapper.jar (SHA-256: ' + $present + '). Sprawdz go i usun przed ponowna proba.')
}

$temp = $jarPath + '.tmp'
try {
    Write-Host ('Pobieram Gradle Wrapper JAR z oficjalnego GitHuba: ' + $uri)
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $uri -OutFile $temp -UseBasicParsing
    $actual = (Get-FileHash -Path $temp -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $expected) {
        throw ('BLAD SHA-256! Oczekiwano: ' + $expected + ', uzyskano: ' + $actual)
    }
    Move-Item -Path $temp -Destination $jarPath -Force
    Write-Host 'OK: pobrano i zweryfikowano oficjalny gradle-wrapper.jar 8.13.' -ForegroundColor Green
    Write-Host 'Otworz projekt w Android Studio, ustaw Gradle JDK 17, nastepnie Sync.'
}
finally {
    if (Test-Path $temp) { Remove-Item -Force $temp }
}
