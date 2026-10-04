# Fitness Gym: feature brainstorm (draft for approval)

Each feature has an ID so you can approve, cut or reorder it, then we build them one by one.

Priority: **M** = MVP (the first usable app on your Pixel), **V1** = next, **V2** = later, **X** = idea parked.

The "Fixes" column names the complaint it answers, from `RESEARCH.md` §3 (C1–C10).

## A. Setup and equipment

| ID | Feature | Pri | Fixes |
|---|---|---|---|
| A1 | **Onboarding:** goal (muscle, strength, fat-loss, general), experience, days per week, session length, injuries or areas to avoid | M | C3 |
| A2 | **PAR-Q+ safety screen**, with a "talk to a professional" flag on a positive answer; it never blocks the app | M | — |
| A3 | **Equipment profiles** chosen from icon grids: Home, Commercial gym, Hotel and so on. Covers barbell, dumbbells, kettlebells, cables, machines, bands, pull-up bar, bench, bodyweight. | M | C9 |
| A4 | **Exact increments per profile:** dumbbell pairs owned (for example 5–25 kg in 2.5 kg steps), plates owned, machine stack step, band colours | M | C9 |
| A5 | **"Never show me" exclusions** and an injury filter (for example "no overhead pressing") | M | C9 |
| A6 | Quick switch between gym profiles from the workout screen, for travel | V1 | C9 |

## B. Exercise library and visual form (core differentiator)

| ID | Feature | Pri | Fixes |
|---|---|---|---|
| B1 | **Curated library**, about 150 exercises at launch, growing to 400+. Each has equipment, movement pattern, primary and secondary muscles, level, and an "anchors at long length" tag. Seeded from free-exercise-db, with instructions in our own words. | M | — |
| B2 | **Animated anatomical figure with visible muscles**: a looping 2D figure in Compose Canvas, with tempo matching the cue (2–3 s lowering). Front/side toggle, pause and slow-motion. | M | — |
| B3 | **Live muscle activation:** muscles glow as they work during the rep (prime movers red, synergists amber, stabilisers faint), strongest on the lifting phase. Shown with a legend, and you can tap a muscle for its name. The same art drives the front/back body map. | M | — |
| B4 | **Form card**: 3 key cues (external focus), 3 common mistakes, a breathing cue, and safety notes | M | — |
| B5 | **Browse by muscle**: tap a muscle on the body map to see exercises filtered to your current equipment | M | — |
| B6 | **"Why this exercise"** chip: the pattern, the muscle, and why it was chosen today | V1 | C3 |
| B7 | Custom exercises, with your own photo or video and a muscle tag | V1 | C9 |
| B8 | Capture studio (laptop): record yourself, MediaPipe extracts keyframes, and the result becomes a new mannequin animation | V2 | — |
| B9 | 3D mannequin with a rotatable view (Mesh2Motion CC0) | X | — |

## C. Workout design (generator and programs)

| ID | Feature | Pri | Fixes |
|---|---|---|---|
| C1 | **Workout generator, rules-based.** It works from your goal, level, days and equipment, and picks a split: full body, upper/lower or PPL. It fills movement-pattern slots (squat, hinge, push horizontal and vertical, pull horizontal and vertical, isolation). Sets, reps, RIR and rest come from the evidence table. | M | C3 |
| C2 | **Variety budget:** a rotation rule so it doesn't serve the same exercise every session, while keeping anchor lifts stable for progression | M | C3 |
| C3 | **Manual routine builder**: drag and drop, supersets and circuits, unlimited routines | M | C2 |
| C4 | **Weekly volume planner**: sets per muscle per week, with fractional counting, shown against a target zone | V1 | — |
| C5 | **Program templates** built from public methods: full-body beginner, PPL, upper/lower, 5/3/1-style, GZCL-style, home dumbbell, bands only | V1 | — |
| C6 | **Mesocycles** of 4–8 weeks with set progression and reactive deloads | V1 | C10 |
| C7 | "15-minute / no-time" minimal session option | V1 | — |

## D. Workout player and logging (must feel fast)

| ID | Feature | Pri | Fixes |
|---|---|---|---|
| D1 | **GO button**: today's workout, one tap to start (GuitarNoodle pattern) | M | — |
| D2 | **Set logging in 3 taps or fewer**: values pre-filled from last time, a ✓ to confirm, ± steppers sized to your equipment increments | M | C5 |
| D3 | **Next-set cursor** that moves automatically. Reps × sets × weight are logged per exercise, per day. | M | C5 |
| D4 | **Rest timer** as a foreground service: notification, vibration, lock-screen controls, auto-start on ✓. Rest length is set per exercise. | M | C6 |
| D5 | Edit mid-workout: swap (with an equipment-aware substitute list), add, skip, reorder | M | C5 |
| D6 | Optional RIR/RPE per set, plus set types: warm-up, working, drop, failure | M | — |
| D7 | **Write-ahead save**: every ✓ goes straight to the DB, and a crash restores the session | M | C4 |
| D8 | Warm-up ramp calculator and plate calculator | V1 | — |
| D9 | Notes per exercise, such as seat height or grip, which reappear next time | V1 | — |
| D10 | Busy-gym swap: one tap gives the same pattern with a different machine | V1 | — |
| D11 | Voice logging ("12 at 40") | V2 | — |

## E. Progress and history

| ID | Feature | Pri | Fixes |
|---|---|---|---|
| E1 | **Calendar and day log**: every exercise × sets × reps × weight for any day | M | — |
| E2 | Exercise history with a chart: estimated 1RM, best set, volume. **Unlimited time range, always free.** | M | C2 |
| E3 | Automatic PRs, with a celebration | M | — |
| E4 | **Muscle heat-map**: the body map coloured by weekly sets per muscle | V1 | — |
| E5 | **Explainable progression**: "Next time: 42.5 kg × 8–12 because…" | M | C3 |
| E6 | Body metrics: weight, measurements, progress photos (private, on device) | V1 | — |
| E7 | Export and import CSV/JSON, plus import from Strong and Hevy | V1 | C4 |
| E8 | Weekly report card | V2 | — |

## F. Readiness, safety and recovery

| ID | Feature | Pri | Fixes |
|---|---|---|---|
| F1 | 10-second check-in: sleep, soreness map, energy. A bad day trims volume and never blocks. | V1 | C10 |
| F2 | Pain flag on a set leads to a suggested substitute and notes the area | V1 | — |
| F3 | Reactive deload trigger: performance drops twice in a row, or fatigue is reported | V1 | C10 |

## G. Motivation (gamification on, guilt off)

| ID | Feature | Pri | Fixes |
|---|---|---|---|
| G1 | XP, levels and a weekly streak (counted in weeks, so rest days are fine). Streak freezes. | M | — |
| G2 | Ghost League: you against last month's you | V1 | — |
| G3 | Milestones and badges, such as first bodyweight bench or 100 workouts | V1 | — |
| G4 | 12-week onboarding arc with weekly unlocks | V2 | — |

## H. Integrations and devices

| ID | Feature | Pri | Fixes |
|---|---|---|---|
| H1 | Health Connect: write workouts, read bodyweight and heart rate | V1 | — |
| H2 | Wear OS: current set, rest timer, ✓ from the wrist | V2 | C6 |
| H3 | Camera rep counter plus 2–3 angle cues (MediaPipe), with scores labelled as estimates | V2 | — |
| H4 | Laptop AI coach over Tailscale (Ollama): explains your plan and progress from a data snapshot. It never writes the plan. | V2 | C7 |
| H5 | Home-screen widget: today's workout and the GO button | V2 | — |

## I. Platform hygiene (inherited from GuitarNoodle)

| ID | Feature | Pri |
|---|---|---|
| I1 | Status/health screen and error codes (`AREA-NNN`) with plain-English fixes | M |
| I2 | Exercises and programs as JSON content validated by schema tests. Adding an exercise means editing data, not code. | M |
| I3 | OTA updates: GitHub Actions builds a signed APK, then a Release, then Obtainium on the Pixel | M |
| I4 | Auto backup to a file, or to the laptop over Tailscale | V1 |

---

## MVP in one sentence

> **Pick your equipment once. Tap GO. Get a sensible workout. Every exercise shows an animated figure with the target muscles glowing. Log each set in 3 taps. See every day's sets × reps × weight, and get told exactly what to lift next time.**

MVP = A1–A5, B1–B5, C1–C3, D1–D7, E1–E3, E5, G1, I1–I3.
