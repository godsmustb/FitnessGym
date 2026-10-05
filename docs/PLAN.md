# Fitness Gym: implementation plan

Nothing gets built until you approve this plan and `FEATURES.md`.

## Owner decisions (Oct 4, 2026)

| Topic | Decision |
|---|---|
| Purpose | **Personal use only. Not for sale, and no paywall.** |
| Name | **Fitness Gym** |
| Platform | Native Kotlin APK, installed and updated through **Obtainium from GitHub Releases** (same as GuitarNoodle) |
| Repo | **Public, open-source GitHub repo** |
| Animation look | **Anatomical figure with visible muscles.** The working muscles light up as they contract during the movement. |

### What these decisions change

- **License.** Because the repo is open source and personal-use, we can license it **AGPL-3.0**. Copyleft sources then become usable, as long as each one is credited in `THIRD_PARTY.md`:
  - wger exercise and muscle data (CC-BY-SA)
  - openGym's ideas, data mappings and logic (AGPL)
  - Everkinetic (CC-BY-SA)
- **Still not allowed:** the ExerciseDB / Gym visual GIFs. Their ownership is disputed, and committing them to a *public* repo would redistribute them. They stay out of the repo. Our own animated figure replaces them anyway.
- **Secrets.** The signing keystore stays outside the repo, in `%USERPROFILE%\.fitnessgym\`, and reaches CI as GitHub Actions secrets (the GuitarNoodle pattern). A public repo also means Obtainium needs no token.
- **Billing.** Billing, WHOP and the "honest billing" items are dropped from scope.

## 1. How the two sister apps are built, and which way this one should go

| | Coach Québec (`french-coach`) | GuitarNoodle (`guitar-noodle`) |
|---|---|---|
| Form | PWA served from the laptop | Native Kotlin APK on the Pixel 8 |
| Where it runs | The laptop does the work; the phone reaches it over Tailscale | The phone does the work; the laptop is an optional companion |
| Why | Heavy local AI (Whisper, Ollama, TTS) on the RTX 4060 | Low-latency audio needs native code |
| Patterns | GO button, module registry, JSON content, pure engines, error codes, health screen, gamification without guilt | Same patterns, plus Room, OTA through GitHub Actions and Obtainium, and phase prompts |

**Recommendation: follow GuitarNoodle and build a native Kotlin + Jetpack Compose APK, offline-first.**

A gym needs:
- a phone that works with **no signal** (basements)
- a rest timer that rings on the **lock screen**
- camera pose later
- Health Connect and Wear OS

A laptop-served PWA does all of these badly. The heavy work in this app is light (rules engine, Canvas animation), so nothing needs the laptop. The laptop becomes an optional companion for two jobs:
- turning your videos into animations (B8)
- an Ollama "explain my progress" coach (H4)

We also reuse GuitarNoodle's work directly:
- the Gradle and toolchain setup
- the OTA pipeline: Actions builds the signed APK, publishes a Release, and Obtainium installs it
- the gamification engine
- the health screen and error-code pattern
- Tokens and theme

## 2. Stack

| Layer | Choice |
|---|---|
| UI | Kotlin 2.x, Compose, Material 3, Navigation-Compose; dark-first, with big touch targets for sweaty hands |
| Storage | Room (SQLite) plus DataStore. Every logged set is written immediately. |
| Content | JSON in `assets/content/` (exercises, animations, programs), checked against schemas in unit tests |
| Animation | Our own **skeletal mannequin in Compose Canvas**, driven by joint-angle keyframes (§4) |
| Body map | MuscleMap (MIT) paths converted to Compose `Path`s, front and back |
| Engines | Pure Kotlin, unit-tested: Generator, Progression, Volume, Substitution, PlateCalc, Gamification |
| Timer | Foreground service, notification, vibration |
| Later | Health Connect, Wear OS module, MediaPipe Pose with CameraX, laptop companion (FastAPI over Tailscale) |
| Licenses | Rules carried over from GuitarNoodle: no GPL or AGPL code in the APK, no ExerciseDB GIFs, everything recorded in `THIRD_PARTY.md` |

## 3. Data model (core)

- `Exercise(id, name, equipment[], pattern, primaryMuscles[], secondaryMuscles[], level, longLengthBias, animId, cues[], mistakes[])`. Loaded from JSON; your custom ones are stored in the DB.
- `EquipmentProfile(id, name, items[], dumbbellSet[], plates[], stackStep, bands[])`
- `Program` → `Routine(day)` → `RoutineSlot(exerciseId, sets, repMin, repMax, targetRir, restSec, supersetGroup)`
- `WorkoutSession(id, date, profileId, start, end, readiness)`
- `SetLog(sessionId, exerciseId, order, type, weight, reps, rir?, ts)`. **This is the reps × sets × weight record for every day.**
- `PersonalRecord`, `BodyMetric`, `Exclusion`, `Note(exerciseId)`

## 4. The animated form visuals (the hardest part, and how we keep it cheap)

Drawing 400 hand-made GIFs is impossible, and using other people's GIFs is a legal trap. Instead:

1. **One parametric anatomical figure, chosen by the owner.** It is a 2D "visible muscles" figure with about 15 joints, drawn in Canvas. About 40 named muscle shapes (pecs, front delts, lats, quads, glutes, hamstrings and so on) are attached to the bones, so they stretch and move with the body.
   - **Muscle activation is animated over the rep:**
     - Prime movers glow red/orange.
     - Synergists glow amber.
     - Stabilisers get a faint tint.
   - Glow intensity follows the phase: strongest while the muscle shortens under load (lifting), softer while it lengthens (lowering).
   - A legend under the figure names each glowing muscle. Tapping a muscle shows its name and role.
   - The same muscle shapes drive the static front/back body map and the weekly heat-map, so there is one set of art for all three.
2. **About 25 motion templates, not 400 animations.** Most exercises are a handful of movement patterns:
   - squat
   - hinge
   - lunge
   - horizontal push and pull
   - vertical push and pull
   - curl, extension, raise, fly
   - carry, core, and so on

   Each template is a short keyframe file of joint angles over time. Each exercise is **template + implement + tweaks**, for example `pattern: horizontal_push, implement: dumbbell, bench: incline 30°`. That way 150 exercises need only about 25 animation files.
3. **Implements are drawn as props**: barbell, dumbbell, kettlebell, cable with handle, band, bench and so on.
4. **Tempo comes from the cue.** The lowering phase plays slower, matching the "2–3 s down" coaching.
5. **Later (B8):** you film yourself on the laptop, MediaPipe extracts joint angles, and a new keyframe file is created. That grows the library from real movement, and we own 100% of it.

We must prove early that this looks good, so it's Phase P1. If you hate the look, the fallback is a paid commercial media licence (ExerciseDB or RepDB) with written terms, or your own ContentGenAI illustrations for static key poses. I don't recommend AI-generated *videos*: the form errors are a safety risk.

## 5. Build phases

The rules follow GuitarNoodle: Sonnet subagents, at most 3 per phase, a fresh session per phase, and each phase ends with tests green, an APK on your Pixel, and the checklist ticked.

| Phase | Scope | Proof you see |
|---|---|---|
| **P0** ✅ | Docs, `CLAUDE.md` rulebook, git, Gradle skeleton (copied from GuitarNoodle), OTA pipeline, a hello APK on the Pixel | The app icon appears through Obtainium |
| **P1** ✅ | **Visual engine and library** (expanded on Oct 4): an anatomical figure with visible muscles, IK rig, 108 motion templates, live muscle glow, a body map, and a **425-exercise library** (Bodyweight, Dumbbells, Machines, Pilates, Kickboxing, Cardio) | You judge the animations on the phone: go or no-go on the approach |
| **P2** ✅ | App shell (5 tabs: Today, Workouts, Exercises, Progress, Me), Room DB, content JSON and validator, health screen, error codes | You browse the library by muscle and equipment |
| **P3** ✅ | Onboarding plus PAR-Q+, equipment profiles with increments, exclusions | Your real home gym and commercial gym, set up |
| **P4** ✅ | **Logger:** GO, workout player, 3-tap sets, next-set cursor, lock-screen rest timer, mid-workout swap, crash recovery | You do a real workout and log it |
| **P5** ✅ | **Engines:** generator, double progression plus RIR, variety budget, substitutions, "next time" explanation | The app writes next week's workouts on its own |
| **P6** ◐ | Progress: calendar day log, charts, PRs, muscle heat-map, export; gamification | Your history is visible day by day |
| **P7+** | Fill out V1 (templates, mesocycles, readiness, plate and warm-up calculators, Health Connect), then V2 (Wear OS, camera rep counter, laptop coach, capture studio) | — |

The MVP is P0–P6.

## 6. Decisions

Decided on Oct 4: platform, name, audience, repo, and animation look. See the top of this file.

Decided on Oct 4, second round: the library covers **bodyweight, dumbbells and gym machines** (GoodLife / LA Fitness), plus **Pilates, kickboxing and cardio**. Floor work is on a yoga mat. Phase 0 and Phase 1 are done.

Phases 2–5 and most of 6 were built on Oct 5 (see BUILD_LOG). At first launch the app asks the goal, experience, days per week, session length, equipment, extras, areas to protect and the PAR-Q+ safety check. Still open from P6: the muscle heat-map (E4) and body metrics (E6). After that come the V1/V2 items.
