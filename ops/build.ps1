# Runs the content/engine tests and builds the signed release APK (same key as the GitHub/Obtainium
# builds, so local installs and OTA updates can replace each other).
. "$PSScriptRoot\_env.ps1"
Push-Location $Root
try {
    & .\gradlew.bat :anim:test :core:test :app:testDebugUnitTest :app:assembleRelease --console=plain
    if ($LASTEXITCODE -ne 0) { throw "BUILD-001: Build or tests failed. Fix: read the first 'error:' or 'FAILED' line above, or ask Claude to run ops\build.ps1." }
    if (-not (Test-Path $Apk)) { throw "BUILD-002: APK is unsigned. Fix: the signing key is missing from %USERPROFILE%\.fitnessgym (see BUILD_LOG)." }
    Write-Host "`nOK: APK ready at $Apk" -ForegroundColor Green
} finally { Pop-Location }
