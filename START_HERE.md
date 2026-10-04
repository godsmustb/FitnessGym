# Start here: Fitness Gym on your Pixel

## Install it and get automatic updates (Obtainium), about 3 minutes

Every `git push` to `main` makes GitHub do three things:
- run the tests
- build and sign the app
- publish it as a Release

Obtainium on your phone sees each new Release and installs it. **The repo is public, so you don't need a token.**

1. On the Pixel, install **Obtainium** from https://github.com/ImranR98/Obtainium/releases (the `app-arm64-v8a-release.apk` file).
   - If you already use it for GuitarNoodle, skip this step.
2. In Obtainium, tap **Add App**.
   - URL: `https://github.com/godsmustb/FitnessGym`
   - Tap **Add**, then **Install**. Allow "install unknown apps" for Obtainium if Android asks.
3. That's it. New builds show up in Obtainium as updates.

**Signing key:** all builds share one signing key. It lives on the laptop in `%USERPROFILE%\.fitnessgym\` and is not in git.
- **Back that folder up.** If it's lost, future updates can't install over the app, and you'd have to uninstall and reinstall. You would lose nothing in v0.1, but later versions will hold your workout history.

## What's in the app (v0.1, Phase 1)

| Tab | What it does |
|---|---|
| **Exercises** | About 425 exercises, filtered by Bodyweight, Dumbbells, Machines (GoodLife/LA Fitness style), Pilates, Kickboxing and Cardio. You can also search by name or muscle. |
| **Exercise page** | An animated anatomical figure with visible muscles. Working muscles glow orange-red and helper muscles amber, brightest on the lifting phase. Tap any muscle to name it. Coach, Front, Side and Back views, plus ½× speed and pause. Then the coaching cues, common mistakes and step-by-step instructions. |
| **Muscles** | Front and back body map. Tap a muscle to list every exercise that trains it. |
| **Status** | Version, content counts, and any content problems with an error code. |

Set and rep logging is the next phase (see `docs/PLAN.md`).

## Laptop commands (PowerShell, from this folder)

| Command | What it does |
|---|---|
| `ops\build.ps1` | Runs the tests and builds the signed APK |
| `ops\install.ps1` | Installs over USB (needs USB debugging) |
| `ops\preview.ps1 squat` | Renders an animation to `preview-out\squat.png`, no phone needed |
