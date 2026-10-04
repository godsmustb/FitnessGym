# Installs (or updates) Fitness Gym on the connected Pixel over USB and launches it.
. "$PSScriptRoot\_env.ps1"
if (-not (Test-Path $Apk)) { & "$PSScriptRoot\build.ps1" }
$devices = & $Adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\tdevice$" }
if (-not $devices) { throw "ADB-001: No phone found. Fix: plug in the Pixel, unlock it, and tap 'Allow USB debugging'." }
& $Adb install -r $Apk
if ($LASTEXITCODE -ne 0) { throw "ADB-002: Install failed. Fix: uninstall Fitness Gym from the phone and run this again." }
& $Adb shell am start -n com.nunna.fitnessgym/.MainActivity | Out-Null
Write-Host "OK: Fitness Gym installed and launched on the phone." -ForegroundColor Green
