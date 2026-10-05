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

## What's in the app (v0.2)

**First launch** asks the setup questions:
- your goal (build muscle, get stronger, lose fat, general fitness)
- your experience
- **days per week** (1–7) and session length
- where you train: gym, home or both, plus your home equipment and the dumbbells you own
- extras (Pilates, kickboxing, cardio)
- areas to protect (shoulders, knees, lower back, wrists, neck)
- a PAR-Q+ safety check, which gives advice but never blocks you

The app then builds your weekly plan.

| Tab | What it does |
|---|---|
| **Today** | **GO** starts the next workout in your plan. You can pick any other day, or start a quick workout. Level/XP, a weekly streak and "this week 2 / 3" are shown. If you train in both places, Gym/Home switches the plan. A workout in progress shows **Resume**. |
| **Workout** | Every exercise has an animated thumbnail, a target (sets × reps, effort, rest) and a coaching note ("+2.5 kg: you hit 3×12 at 20 kg"). Log a set by tapping ✓, with the values pre-filled from last time and the weight carried forward from set to set; tap ✓ again to undo. Use ± for the next weight you own. The rest timer runs on the lock screen and buzzes when rest is over. The ⋮ menu has swap (equipment-aware suggestions), skip, add set, move, remove and never-suggest. Long-press a set number to delete that set. Everything saves instantly, so a crash loses nothing. |
| **Summary** | Minutes, sets, volume, XP, level-ups and personal records. |
| **Exercises** | 425 exercises. Each page has the animated muscle figure, your history (best set, estimated-1RM chart, recent sessions), "Add to current workout" and "Never suggest this". |
| **Progress** | A month calendar with workout days marked. Tap a day to see every exercise × sets × reps × weight. Also shows recent PRs, and you can open a workout to view or delete it. |
| **Muscles** | Body map. Tap a muscle to see its exercises. |
| **Me** | Edit goals & schedule (rebuilds the plan, keeps your history), new plan variation, kg/lb, optional RIR logging, hidden exercises, **CSV export**, app status, and reset. |

## Laptop commands (PowerShell, from this folder)

| Command | What it does |
|---|---|
| `ops\build.ps1` | Runs all ~100 tests (engine, plan logic, end-to-end app flows) and builds the signed APK |
| `ops\install.ps1` | Installs over USB (needs USB debugging) |
| `ops\preview.ps1 squat` | Renders an animation to `preview-out\squat.png`, no phone needed |
