# Renders motion templates to preview-out\<id>.png contact sheets (no phone needed).
#   ops\preview.ps1 squat lunge        ops\preview.ps1 all        ops\preview.ps1 ex:dumbbell-bench-press
. "$PSScriptRoot\_env.ps1"
Push-Location $Root
try {
    & .\gradlew.bat :preview:installDist --console=plain -q
    & "$Root\preview\build\install\preview\bin\preview.bat" @args
} finally { Pop-Location }
