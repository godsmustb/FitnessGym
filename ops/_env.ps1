# Shared environment for ops scripts. Dot-source it: . "$PSScriptRoot\_env.ps1"
$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent

if (-not $env:JAVA_HOME -or -not (Test-Path $env:JAVA_HOME)) {
    $jdk = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -ErrorAction SilentlyContinue |
        Where-Object Name -like "jdk-17*" | Select-Object -First 1
    if ($jdk) { $env:JAVA_HOME = $jdk.FullName }
}
if (-not $env:ANDROID_HOME) { $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk" }

$Adb = Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
if (-not (Test-Path $Adb)) { $Adb = (Get-Command adb -ErrorAction SilentlyContinue).Source }
$Apk = Join-Path $Root "app\build\outputs\apk\release\app-release.apk"

# local.properties tells Gradle where the SDK is (gitignored, machine-specific)
$lp = Join-Path $Root "local.properties"
if (-not (Test-Path $lp)) { "sdk.dir=$($env:ANDROID_HOME -replace '\\','/')" | Set-Content $lp -Encoding ascii }
