param(
    [Parameter(Mandatory=$true)][string]$Apk
)
$ErrorActionPreference = 'Stop'
$resolved = (Resolve-Path $Apk).Path
$buildTools = Join-Path $env:LOCALAPPDATA 'Android\Sdk\build-tools'
$apksigner = Get-ChildItem $buildTools -Filter apksigner.bat -Recurse -File |
    Sort-Object { [version]($_.Directory.Name -replace '-.*$','') } -Descending | Select-Object -First 1
if (-not $apksigner) { throw "Nie znaleziono apksigner.bat w $buildTools" }
$lines = & $apksigner.FullName verify --print-certs --verbose $resolved 2>&1
if ($LASTEXITCODE -ne 0) { throw "Niepoprawny podpis APK: $($lines -join ' ')" }
$match = [regex]::Match(($lines -join "`n"), 'Signer #1 certificate SHA-256 digest:\s*([0-9a-fA-F]{64})')
if (-not $match.Success) { throw 'Nie znaleziono SHA-256 certyfikatu w wyniku apksigner' }
$hex = $match.Groups[1].Value.ToLowerInvariant()
$bytes = New-Object byte[] 32
for ($i=0; $i -lt 32; $i++) { $bytes[$i] = [Convert]::ToByte($hex.Substring($i*2,2),16) }
$b64 = [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+','-').Replace('/','_')
Write-Host "SHA-256 CAŁEGO APK (hex): $((Get-FileHash $resolved -Algorithm SHA256).Hash.ToLowerInvariant())"
Write-Host "SHA-256 CERTYFIKATU (hex): $hex"
Write-Host "PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM (Base64URL): $b64"
Write-Host 'NIE wpisuj do Generatora 1.2 Base64URL: tam wybierz typ certyfikat i wklej 64 znaki HEX.'
