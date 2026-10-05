# Build log

## 2026-10-05: Phases 2–5 (plus most of 6). The full app is built.

**What changed**

- **`:core`** (new pure-Kotlin module):
  - Evidence-based **workout generator**. It covers slot templates per split (1–7 days), equipment-aware selection with staples, beginner level filters, a variety budget, avoid-area filters, exclusions, interest finishers (cardio, Pilates, kickboxing; cardio is forced for fat loss) and session-length trimming.
  - **Double progression**: hit the top of the range on all sets, then the next weight you actually own. Two bad sessions in a row trigger a reactive deload. Bodyweight, timed and cardio work have their own rules, and kg/lb are converted.
  - Personal records (Epley e1RM), XP and levels, and a forgiving weekly streak.
- **Room database:** profile/settings JSON, the plan, and sessions → exercises → sets.
  - Writes are write-ahead: every tap persists immediately.
  - A workout is resumed after a crash.
  - Un-ticked sets are dropped on finish.
- **Onboarding (P3):**
  - Questions: name, goal, experience, **days per week**, session length, gym/home/both, home equipment and owned dumbbells, kg/lb, extras, areas to protect, PAR-Q+, and optional RIR.
  - Next stays disabled until each step is answered.
  - The same flow is used for "Edit goals".
- **Workout logger (P4):**
  - Logging: GO, set rows prefilled from history with a carry-forward weight, ✓ to log or undo, ± weight steps from the equipment ladder, a next-set cursor, validation with SET-001/002 messages, and a PR banner.
  - Rest timer: a foreground service with a lock-screen countdown and a buzz at the end, plus +15/−15/skip.
  - Editing: swap, skip, add, remove and move exercises, add a set, long-press to delete a set, never-suggest, add exercise.
  - Ending: finish or discard confirmations, an empty workout closes without saving, and a summary screen.
- **Progress (P6):** month calendar with a day log of exercise × sets × reps × weight, session detail with delete, recent PRs, and exercise history with an e1RM chart.
- **Me:** edit goals, plan variation, kg/lb, RIR toggle, hidden exercises, CSV export through the share sheet, status, and reset.
- **Navigation:** 5 tabs (Today, Exercises, Progress, Muscles, Me).

**Bugs found by the end-to-end tests and fixed**

1. "Back to Today" did nothing after a discarded or empty workout (the tab-restore navigation pattern). Fixed with an explicit pop to Today.
2. After finishing a workout, Today still showed the same day instead of the next one in the rotation.
3. The swap picker hid its "Best swaps" section above the list's scroll anchor.
4. After logging set 1, the following sets had empty weight boxes. The weight now carries forward, so each set is one tap.

**Test results** (all on the JVM; Robolectric runs the real app, since the emulator can't run on this laptop)

| Suite | Tests | Result |
|---|---|---|
| `:anim` (content schema, motions, rig) | 11 | ✅ |
| `:core` (generator over every goal × level × 1–7 days × gym/home/bodyweight; progression; records; streaks) | 31 | ✅ |
| `:app` Onboarding E2E | 8 | ✅ |
| `:app` Workout E2E | 17 | ✅ |
| `:app` Progress & settings E2E | 12 | ✅ |
| `:app` Repository edge cases | 17 | ✅ |
| `:app` Rest-timer service | 3 | ✅ |
| `:app` Smoke and screenshots (15 screens rendered and reviewed) | 2 | ✅ |
| **Total** | **101** | **✅ 0 failures** |
| Signed release APK | | ✅ |

**Not verifiable here:** behaviour on the real Pixel. The owner installs it through Obtainium. The emulator needs hardware virtualisation, which is off on this laptop.


## 2026-10-04: Phase 0 (foundation) and Phase 1 (visual engine and exercise library)

**What changed**

- **P0:**
  - Gradle multi-module project:
    - `:anim` is a pure-Kotlin figure engine.
    - `:preview` is a desktop PNG tool.
    - `:app` is Compose.
  - Rulebook (`CLAUDE.md`), `START_HERE.md` with Obtainium steps, `THIRD_PARTY.md`, and the AGPL-3.0 `LICENSE`.
  - Public repo at github.com/godsmustb/FitnessGym.
  - Signing key in `%USERPROFILE%\.fitnessgym\`, with the GitHub secrets `FG_KEYSTORE_B64` and `FG_KEYSTORE_PASSWORD`.
  - Release workflow: tests, then a signed APK, then a GitHub Release, which Obtainium installs.
- **P1, the anatomical figure:**
  - An IK rig: two-bone limbs with pole vectors, a spine, the head, and feet.
  - A 3D figure projected to 2D, painter-sorted, with 19 muscle groups as surface patches that bulge and glow:
    - prime movers in orange-red
    - helpers in amber
    - intensity following an effort curve, so the glow peaks on the lift
  - Props: mat, boxes, cables, bars and pads, discs, dumbbells, gloves, jump rope.
  - Camera views: Coach, Front, Side and Back.
- **P1, content:**
  - **425 exercises** in `content/exercises.json`:
    - 337 from free-exercise-db (public domain)
    - 88 written by us: Pilates, kickboxing, cardio and gym-machine gap fillers
  - Tag counts: bodyweight 108, dumbbell 123, machine 152, pilates 20, kickboxing 22, cardio 34, mat 59.
  - **108 motion templates**, each checked by eye in the preview tool.
- **P1, the app:**
  - **Exercises:** a search and tag filter, with a live thumbnail for each row.
  - **Exercise page:** the animated figure with tap-to-name muscles, ½× speed, pause and 4 views, plus the cues, mistakes and steps.
  - **Muscles:** a front and back body map; tap a muscle to list its exercises.
  - **Status:** version, counts and error codes.

**Test results**

| Check | Result |
|---|---|
| `:anim:test` (content schema, motion sanity, rig math) | ✅ 11/11 |
| `exercises.json` validation (`build_db.py`) | ✅ OK, every motion used |
| `:app:assembleRelease`, signed | ✅ (42 MB, unminified) |
| Device run | ⏳ The owner installs it through Obtainium. The emulator can't run on this laptop. |

**Known approximations** (each is noted in its motion's `notes` field):
- shrug and wrist_curl: the rig has no shoulder-elevation or wrist joint
- incline_press, machine_curl and back_extension: the benches and pads are built from stepped boxes
- leg_press: no moving foot plate
- skater_hop: the sideways travel isn't shown
- the single_leg_circle, saw and swimming Pilates moves are subtle

**Open issues and next steps**
1. Owner feedback on the look, from the Pixel. That is the P1 go/no-go.
2. APK size: enable R8 minify and switch from icons-extended to a few vector icons.
3. Polish: tilted boxes for incline benches, then shoulder-elevation and wrist joints.
4. P2–P4 from `docs/PLAN.md`: Room DB, equipment profiles, then the workout logger (sets × reps × weight per day).
