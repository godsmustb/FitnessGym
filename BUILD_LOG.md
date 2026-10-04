# Build log

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
