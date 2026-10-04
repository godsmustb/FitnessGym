# 01 - Open-Source Landscape for a Native Android Fitness Companion App

Date: 2026-10-04. Stars / last-push / license are from the GitHub API (queried 2026-10-04) unless marked "(unverified)".
Target: native Kotlin + Jetpack Compose, Pixel 8, offline-first, possible later commercialization.

**License legend:** OK = permissive (MIT/Apache/BSD/Unlicense/CC0). COPYLEFT = GPL/AGPL (cannot be linked into a closed-source commercial app; reading for ideas is fine, copying code is not). SA = CC-BY-SA (attribution plus share-alike on the data/adaptations). NC = non-commercial. MEDIA-RISK = code license fine but media provenance unclear.

---

## 0. What is "OpenGym"?

"OpenGym" is ambiguous in 2026. The most likely meaning is **openGym by Duarte Santos**, but there are several same-named projects.

| Candidate | URL | Stars | Last push | License | Notes |
|---|---|---|---|---|---|
| **openGym (Duarte Santos) - most likely** | https://github.com/DuarteSantos8/openGym (site: https://opengym.duarte-santos.ch, demo: /demo/) | 2,699 | 2026-10-04 | AGPL-3.0 (plus app-store exception, see NOTICE.md) | Self-hosted gym and body-weight tracker. Repo created 2026-07-18, so very new and fast-growing. |
| openGym mirrors/forks | https://github.com/alexpcosta/opengym (440 stars), also Joairazabal, Jandresdiaz87, fnsalinas (all AGPL) | - | - | AGPL-3.0 | Mirrors/forks of the above. |
| OpenGym (norrdev) | https://github.com/norrdev/OpenGym | 34 | 2026-05-18 | GPL-3.0 | Flutter (Dart) "fitness log and body parameters". Possibly the Play Store listing "OpenGym: Cross-platform Gym" (pro.filonov.npng) (unverified link). |
| Open-GYM (smartworldarafath) | https://github.com/smartworldarafath/Open-GYM | 8 | 2026-10-04 | GPL-3.0 | Flutter, simple offline Android gym log. |
| opengym3d (AssiamahS) | https://github.com/AssiamahS/opengym3d | 9 | 2026-09-15 | MIT | Different thing: open 3D exercise animation pipeline (Blender in CI -> .glb, Three.js viewer, muscle metadata). Very relevant to our animation problem, see Section 3. |
| OpenGym-Copilot, opengym-fa | various | <20 | 2026 | - | Unrelated (RL gym environments / other). |

### openGym (Duarte Santos) in detail
Source: README https://github.com/DuarteSantos8/openGym and NOTICE.md https://raw.githubusercontent.com/DuarteSantos8/openGym/main/NOTICE.md
- Self-hosted gym + body-weight tracker; no account, no subscription, no ads, no telemetry. `docker compose up`.
- **Features very close to our brief:** library of **1,324 exercises with animated demos**, browsable by muscle on a **body map**, **filter by equipment you own**; weekly routines; 4 starter plans (PPL, Upper/Lower, Full Body, 5x5); supersets, warm-up, drop sets, rest-pause; guided sessions with weights pre-filled from last time, rest timer, PR detection; RIR/RPE; plate math; progression rules (linear, Greyskull LP, double progression); estimated 1RM; muscle map showing volume / recovery / untrained; imports from FitNotes/Strong/Hevy; optional BYOK AI coach; 17 languages.
- **Stack:** React 19 + Vite + Zustand frontend, plain `node:http` API storing JSON; the phone app is the same code wrapped with **Capacitor** (Android APK sideloaded, deliberately not on Play Store). Not native Kotlin.
- **Licensing facts (important for us):**
  - Code: AGPL-3.0. Cannot be copied into a closed commercial app.
  - Body-map path geometry derived from MuscleMap (MIT) - https://github.com/melihcolpan/MuscleMap. We can go to the MIT source directly.
  - Exercise names/instructions/attributes: derived from ExerciseDB v1 via https://github.com/hasaneyldrm/exercises-dataset (MIT claimed on text).
  - **Images/GIFs: openGym's own NOTICE says ownership is "unresolved"**: dataset attributes them to Gym visual, ExerciseDB claims to own them; "treat the media as third-party content licensed to neither openGym nor to you."
- **Takeaway:** best product-level reference for our concept (equipment filter + muscle map + animated demo + logging + progression). Use it as a UX/spec reference only; do not reuse code or media.

---

## 1. Full open-source workout tracker apps

| # | Name / URL | Stars | Last push | License | Stack | Reusable for us |
|---|---|---|---|---|---|---|
| 1 | **wger** https://github.com/wger-project/wger | 7,015 | 2026-10-04 | AGPL-3.0 (docs CC-BY-SA-4.0) | Django/Python backend + REST API; Flutter app https://github.com/wger-project/flutter (977 stars, AGPL + app-store exception) | Best-documented data model (exercise/muscle/equipment/routine/slot/log). Its exercise API is a data source (see Sec 2). Code COPYLEFT. |
| 2 | **openGym (Duarte Santos)** https://github.com/DuarteSantos8/openGym | 2,699 | 2026-10-04 | AGPL-3.0 | React/Vite + Node + Capacitor | Closest feature match; spec/UX reference (see Sec 0). COPYLEFT. |
| 3 | **Workout.cool** https://github.com/Snouzy/workout-cool | 8,547 | 2026-09-20 | MIT | Next.js/TypeScript, Postgres/Prisma, web only | Permissive code, modeling of exercise attributes + CSV import. Its README says video licensing was prohibitively expensive, so exercise media is NOT included/licensed (MEDIA-RISK). Web, not mobile. |
| 4 | **Liftosaur** https://github.com/astashov/liftosaur | 728 | 2026-10-04 | AGPL-3.0 | TypeScript, Preact web + iOS/Android wrappers | Gold standard for programmable progression ("Liftoscript"). Ideas only (COPYLEFT). |
| 5 | **LiftLog** https://github.com/LiamMorrow/LiftLog | 582 | 2026-10-01 | AGPL-3.0 | React Native/Expo, .NET backend, Claude AI planner, offline-first, E2E-encrypted social | Offline-first plus AI-plan UX reference. COPYLEFT. |
| 6 | **Flexify** https://github.com/brandonp2412/Flexify | 440 | 2026-10-04 | MIT | Flutter/Dart, offline-first | Permissive; reference for set logging, graphs, rest timer. Dart, so port concepts not code. |
| 7 | Small native Android trackers: simonalveteg/Workout-Tracker https://github.com/simonalveteg/Workout-Tracker (44, 2025-12, no license = all rights reserved); KenAli77/InFit https://github.com/KenAli77/InFit (71, 2024-11, Apache-2.0, Kotlin); mihretu-dev/HomeWorkoutApp (3, MIT, Compose offline-first); sergio11/fitflextv_android (55, Apache-2.0); LundiNord/KraftLog (2, Compose/Material3, local-first, no license) | small | mixed | see left | Kotlin/Compose/Room | Only small native-Kotlin examples exist; InFit and fitflextv (Apache) are legally reusable for Room schema/Compose patterns. Quality unverified. |
| 8 | **GymRoutines** https://github.com/noahjutz/GymRoutines (Codeberg original) | 16 | 2023-08 | GPL-3.0 | Kotlin, Jetpack Compose, Room | Closest "native Compose + Room" tracker but stale and GPL: reference only. |
| 9 | **Fud AI** https://github.com/apoorvdarshan/fud-ai | 453 | 2026-10-02 | MIT | Kotlin (also iOS) | AI calorie + workout tracker, BYOK; permissive Kotlin example of AI feature wiring (quality unverified). |
| 10 | Everlog (gchristov) https://github.com/gchristov/everlog-kotlin-multiplatform | 1 | 2026-10-04 | none stated | Kotlin Multiplatform | Open-sourced 2020 codebase of a shipped tracker (quality unverified). |
| 11 | F-Droid cardio/other: FitoTrack (https://f-droid.org/en/packages/de.tadris.fitness/), RunnerUp (https://github.com/jonasoreland/runnerup, 956 stars, GPL-3.0), Gym Bott (local routines) | - | - | GPL-ish | Java/Kotlin | Cardio-focused; little relevance to strength logging. |

FitNotes itself is closed source (https://github.com/MakisChristou/verifit is an archived GPL clone, 79 stars). No notable permissive Strong/Hevy clone in native Kotlin was found.

Observation: there is no mature permissively licensed, native Kotlin/Compose "equipment -> generated workouts -> animated form demo -> logging" app. We are building into a gap.

---

## 2. Exercise databases / datasets

| Dataset | URL | Size | Fields | Media | Code/data license | Media license verdict |
|---|---|---|---|---|---|---|
| **free-exercise-db (yuhonas)** | https://github.com/yuhonas/free-exercise-db (1,952 stars, pushed 2026-09-27) | 800+ exercises (the `exercises/` dir lists 1000 entries) | name, force, level, mechanic, equipment, primaryMuscles, secondaryMuscles, instructions, category, images (2 JPGs each) | Static JPG photos (start/end pose), no animation | **Unlicense** (public domain) | **MEDIA-RISK:** repo says public domain but image provenance is undocumented; a 2026 comparison notes "chain of title is fuzzier" for commercial products (https://repdb.co/blog/exercisedb-alternatives-2026/). Derived from https://github.com/wrkout/exercises.json (637 stars, Unlicense). OK for TEXT/metadata; get legal comfort before shipping the photos commercially. |
| **wger exercise API** | https://wger.de/api/v2/exerciseinfo/ (queried live: **917 exercises** with language=en, each with category, `muscles`, `muscles_secondary`, `equipment`, translations, `license`, `license_author`) | 917 | Muscles carry `is_front` plus SVG overlay URLs (`/static/images/muscles/main/muscle-11.svg`) | 378 images total, many exercises have none | App AGPL-3.0; exercise data **CC-BY-SA 3.0/4.0** per record (e.g. record 9 = CC-BY-SA 4, author "deusinvictus"); images carry per-image licenses | **SA:** commercial use allowed with attribution and share-alike on the data (our derived exercise DB would need CC-BY-SA). Per-image license must be honored; some images may be NC (unverified). Best legal metadata + muscle taxonomy. |
| **ExerciseDB (exercisedb.io / AscendAPI)** | https://github.com/ExerciseDB/exercisedb-api (691 stars, pushed 2025-11-25, AGPL-3.0 code); terms https://exercisedb.io/faq | Claims 11,000+ exercises, 5,000+ GIFs, 15,000+ videos (marketing, unverified); one 2026 review says 1,394 entries incl. male/female/angle duplicates | target, secondary muscles, equipment, body part, instructions, GIF | Animated GIFs (opaque) | Code AGPL. Content: paid license | **Do not use free GitHub copies.** Per FAQ: a paid package license allows commercial use and displaying GIFs in your app, self-hosting/bundling OK, but no redistribution of raw files. Ownership of the GIFs is disputed (next row). |
| **hasaneyldrm/exercises-dataset** | https://github.com/hasaneyldrm/exercises-dataset (22,504 stars, pushed 2026-07-16, license "NOASSERTION": MIT + media exception) | 1,324 exercises, GIF + 180x180 thumbnail each, 10 languages | category, bodyPart, equipment, target, muscle groups, instructions | GIFs credited to (c) Gym visual "used with permission" | MIT on text/structure | **MEDIA-RISK / DO NOT SHIP:** permission not transferable; openGym's NOTICE shows the files are really ExerciseDB v1 (filenames embed ExerciseDB ids) and that "Gym visual" vs "ExerciseDB" ownership claims contradict each other. This is the "ExerciseDB GIF licensing issue" in concrete form. Verified from https://raw.githubusercontent.com/DuarteSantos8/openGym/main/NOTICE.md and the dataset README. |
| **Everkinetic data** | https://github.com/everkinetic/data (121 stars, 2026-01) | (unverified) | exercise json + illustrated images | Line illustrations | **CC-BY-SA-4.0** | SA: usable commercially with attribution + share-alike on adaptations. Content count unverified. |
| exercemus/exercises | https://github.com/exercemus/exercises (48 stars, MIT) | curated merge of exercemus, wger.de, exercises.json | json | none | MIT repo, but inherits wger CC-BY-SA terms (unverified) | Metadata only. |
| RepDB (commercial) | https://repdb.co | 609 (497 animated) transparent WebP | - | animated WebP | $499 one-time commercial file license (vendor claim, unverified) | A legit paid route for licensed animation. |
| wrkout.xyz (commercial, from exercises.json author) | https://wrkout.xyz/ | 2,500+ exercises, 10k images, 3.5k videos (vendor claim) | - | images/video with muscles worked | Paid commercial | Alternative paid route (unverified). |
| WorkoutX | (API) | 1,400+ | - | hosted GIF | subscription | A 2026 review observed its GIFs match ExerciseDB artwork: same provenance risk (https://repdb.co/blog/exercisedb-alternatives-2026/). |

**Bottom line on media:** there is no free, clearly commercial-safe, animated exercise media set. Free GIF libraries on GitHub are redistributions of ExerciseDB/Gym visual content with unresolved rights. Safe options: (a) our own animations (Sec 3), (b) CC-BY-SA illustrations (wger / Everkinetic) with share-alike obligations, (c) paid license (ExerciseDB, RepDB).
For **metadata** (names, equipment, muscles, instructions) the cleanest bases are free-exercise-db (Unlicense; confirm text origin) and wger (CC-BY-SA, per-record attribution); write our own instruction text to avoid inheriting either.

---

## 3. Muscle maps, exercise animation sources, and legal ways to make our own

### 3a. Muscle highlight / body map components
| Name | URL | Stars | Last push | License | Stack | Notes |
|---|---|---|---|---|---|---|
| **MuscleMap (melihcolpan)** | https://github.com/melihcolpan/MuscleMap | 264 | 2026-04-20 | MIT | SwiftUI; path data in Swift source | Highlights, heatmaps, sub-groups. openGym already converted its paths to JSON (MIT permits us to do the same into Android path strings). Path data is the valuable part. |
| **react-native-body-highlighter** | https://github.com/HichamELBSI/react-native-body-highlighter | 294 | 2026-09-17 | MIT | RN + SVG; male/female, front/back; ~25 slugs (chest, abs, biceps, quadriceps, hamstring, gluteal, upper-back, etc.) | Path artwork provenance not documented (unverified): confirm before commercial use. |
| react-body-highlighter (giavinh79) | https://github.com/giavinh79/react-body-highlighter | 52 | 2026-09-20 | MIT | React SVG | Web only. |
| react-muscle-highlighter | https://github.com/soroojshehryar/react-muscle-highlighter | 44 | 2026-01 | MIT | React | Web. |
| body-muscles (vulovix) | https://github.com/vulovix/body-muscles | 30 | 2026-04 | Apache-2.0 | zero-dep TS | Framework-agnostic SVG; path source unverified. |
| ExerciseDB muscle-visualizer-api | https://github.com/ExerciseDB/muscle-visualizer-api | 32 | 2026-01 | none | API | No license = do not use. |
| wger muscle SVGs | `https://wger.de/static/images/muscles/main/muscle-N.svg` + `secondary` | - | - | repo AGPL; assets CC-BY-SA (unverified) | SVG overlays per muscle with front/back flag | Ready-made primary/secondary overlays matching the wger muscle taxonomy; SA obligations. |

No native Jetpack Compose body-highlighter library was found. Recommended: render SVG paths with `androidx.compose.ui.graphics.vector.PathParser` in a Compose `Canvas`, tint primary/secondary muscles.

### 3b. Anatomy 3D
- **Z-Anatomy** https://github.com/Z-Anatomy (e.g. Models-of-human-anatomy; based on BodyParts3D). Project stated as **CC BY-SA 4.0**, but the male geometry has **mixed licenses incl. CC BY-NC 4.0 (kidneys) and CC BY-NC-SA 4.0 (inner ear)** (https://anatomytool.org/open3dmodel-about; via search summary). Verdict: SA, and NC parts must be excluded. A muscles-only export is possible but share-alike applies to derived models. Heavy for mobile.
- BodyParts3D (DBCLS Japan) underlies Z-Anatomy, CC BY-SA (exact version unverified).

### 3c. Existing animation sources
- **Mixamo** https://helpx.adobe.com/creative-cloud/faq/mixamo-faq.html: free, royalty-free for commercial apps/games, but **you may not redistribute raw character/animation files** (e.g. as an asset pack) and not use it to train ML models. Few lifting clips. Embedding extractable FBX/GLB in an APK is a risk to flag to counsel.
- **Mesh2Motion** https://github.com/Mesh2Motion/mesh2motion-app (3,364 stars, pushed 2026-09-28): web Mixamo-alternative; code MIT, **art assets (models, rigs, animations) CC0**. Library is general locomotion/idle, not weightlifting.
- **opengym3d** https://github.com/AssiamahS/opengym3d (MIT, 9 stars): CI pipeline turning JSON pose keyframes + muscle metadata into skinned .glb via headless Blender; motion lanes: CC0 (mesh2motion), own video via MediaPipe Pose, Mixamo (kept out of the sold pack). Proves our "pose-keyframe + muscle map" approach; project is tiny and young.
- **MakeHuman** https://github.com/makehumancommunity/makehuman (1,580 stars, last push 2024-08): app is AGPL; exported models are CC0 per MakeHuman policy (unverified current text). Legal source of a base mannequin.
- **Lottie** https://github.com/airbnb/lottie-android (35,736 stars, Apache-2.0, pushed 2026-02) and **Rive** https://github.com/rive-app/rive-android (542, MIT): runtimes for vector animation. No good open exercise-form Lottie set found.
- Free GIF sets: see Sec 2, not safe.

### 3d. Generating our own animations legally (ranked)
1. **Pose-keyframe mannequin in Compose Canvas (recommended MVP).** Each exercise = 2-5 keyframes of joint angles for a stick/capsule figure with `Animatable` interpolation; primary/secondary muscles via the body-map overlay; equipment glyphs as vector paths. 100% our IP, tiny, offline, scales via shared rig + per-exercise JSON. Needs a small keyframe authoring/debug screen.
2. **Own motion capture via MediaPipe Pose** on phone video of a trainer -> keyframes JSON (opengym3d does this) -> same renderer. Realistic form; we own the footage.
3. **Blender + CC0 rig (MakeHuman/Mesh2Motion) -> pre-rendered WebP/Lottie/video** for a premium look; render offline in CI. Heavier pipeline.
4. **AI-generated clips** (video diffusion): inconsistent anatomy/form is a safety/liability issue for form instruction; output license terms vary (unverified). Not recommended for form-critical content.
5. **Paid license** (ExerciseDB / RepDB) as a stop-gap for breadth.

---

## 4. Workout generation / progression engines (open source)

No standalone, permissive, high-quality "generate a workout from equipment" library was found.
- **Liftosaur / Liftoscript** https://github.com/astashov/liftosaur (AGPL-3.0). DSL for progressive programs, built-in library (5/3/1, GZCLP, PPL, nSuns etc.). Extractions: https://github.com/AustinGrey/Liftoscript (AGPL), https://github.com/pbantolas/liftosaur-program-creator (MIT, 6 stars). COPYLEFT: study semantics; reimplement our own small rule model. Training methods (5/3/1, GZCL, PPL) are published ideas; do not copy Liftosaur's exact scripts.
- **openGym progression** (AGPL): linear, Greyskull LP, double progression with "why this number" explanations, deload on stalls. UX reference.
- **wger routines** (AGPL): slot/entry model with per-slot progression rules (weight/reps/RiR/rest). Schema reference.
- jstateri/fitness-routine-generator https://github.com/jstateri/fitness-routine-generator (0 stars, MIT, Deno+SQLite): deterministic generator by muscle; tiny, quality unverified.
- DiceGainz https://github.com/ramzan/DiceGainz (13, GPL-3.0, Kotlin random generator); power_progress (7, MIT, Dart): toy-level.
- LLM planners: LiftLog (AGPL), openGym AI coach (AGPL, BYOK): patterns only.

**Recommendation:** write our own generator: (1) filter exercises by selected equipment, (2) choose split template (full body / upper-lower / PPL) by days/week, (3) fill muscle-group slots from a ranked list (compound first, equipment fit, difficulty), (4) set rep ranges by goal, (5) double progression + deload rules as data. Small, testable Kotlin, our IP.

---

## 5. On-device form / rep counting

| Name | URL | Stars | Last push | License | Notes |
|---|---|---|---|---|---|
| **MediaPipe (Pose Landmarker / BlazePose)** | https://github.com/google-ai-edge/mediapipe (37,162); samples https://github.com/google-ai-edge/mediapipe-samples (2,845, pushed 2026-09-15; `examples/pose_landmarker` has Android, iOS, JS, Python, Raspberry Pi) | | | **Apache-2.0** | 33 landmarks, Tasks API for Android (CameraX live stream); lite/full/heavy models (variant detail unverified). Primary choice. |
| **ML Kit Pose Detection** | https://github.com/googlesamples/mlkit (4,299, 2026-07-21, Apache-2.0); docs https://developers.google.com/ml-kit/vision/pose-detection/android | | | Apache-2.0 samples; SDK under Google terms | 33 landmarks; base ~10 MB (~30 FPS Pixel 3XL) / accurate ~13 MB. Vision quickstart includes pose classification + repetition counting (k-NN) per https://developers.google.com/ml-kit/vision/pose-detection/classifying-poses. |
| TF examples / MoveNet | https://github.com/tensorflow/examples (8,279, Apache-2.0), pose_estimation Android example (path unverified) | | | Apache-2.0 | 17 keypoints; lighter, less detailed than MediaPipe. |
| KouroshEsmaeili/android-pose-estimation | https://github.com/KouroshEsmaeili/android-pose-estimation | small | ? | unverified | CameraX + ML Kit, landmark overlay, joint-angle analysis. Check license first. |
| StronGo | https://github.com/KaarisMoiLeCrane/StronGo | 5 | 2025-08 | none | Kotlin offline rep counting with local AI; reference only. |
| expo-body-vision | https://github.com/rbayuokt/expo-body-vision | 5 | 2026-10 | MIT | Native pose rules + exercise counting for React Native; rule-definition ideas. |
| Python/OpenCV + MediaPipe counters (DapCodes Pose-Recognition-Exercise-Counter MIT, pullups-counter MIT, Pose-Form-Rep-Coach) | various | <60 | 2026 | mixed | Show angle-threshold up/down state machines. |
| Ultralytics YOLO-pose | https://github.com/ultralytics/ultralytics (62,201) | | | **AGPL-3.0** (enterprise license sold) | **Avoid** for a commercial closed app. |

Approach: landmark angles (hip/knee/elbow) -> hysteresis state machine per exercise to count reps; form flags (knee valgus, back rounding) are heuristic, so present as guidance, not safety-critical. Pixel 8 (Tensor G3) should handle MediaPipe in real time (unverified). Treat as v2; manual logging is the core.

---

## 6. Health platform integration

| Resource | URL | Stars | Last push | License | Notes |
|---|---|---|---|---|---|
| **android/health-samples** | https://github.com/android/health-samples | 349 | 2026-09-16 | Apache-2.0 | Official Health Connect/Health Services samples (Kotlin). |
| **android/platform-samples** | https://github.com/android/platform-samples | 1,750 | 2026-10-01 | Apache-2.0 | May include Health Connect samples (unverified). |
| **android/wear-os-samples** | https://github.com/android/wear-os-samples | 1,406 | 2026-10-02 | Apache-2.0 | Wear OS Compose + Health Services patterns. |
| Health Connect Jetpack | `androidx.health.connect:connect-client` | - | stable **1.1.0 (2025-10-08)**, alpha **1.2.0-alpha06 (2026-08-26)**, minSdk 24 (https://developer.android.com/jetpack/androidx/releases/health-connect) | Apache-2.0 | On Android 14+ Health Connect is a framework module. Write `ExerciseSessionRecord` (+ segments), read weight/steps/HR. Needs Play permission declaration (https://developer.android.com/health-and-fitness/health-connect/experiences/workouts). Training plans doc: https://developer.android.com/health-and-fitness/health-connect/features/training-plans |
| OlgaDery/health_connect_multiplatform | https://github.com/OlgaDery/health_connect_multiplatform | 8 | 2024 | none | Stale. |

Plan: v1 write finished workouts as `ExerciseSessionRecord(STRENGTH_TRAINING)` and read `WeightRecord`; Wear OS companion optional later.

---

## 7. Recommended building blocks (what we would actually reuse)

| Need | Choice | License verdict |
|---|---|---|
| Exercise metadata seed (names, equipment, primary/secondary muscles, level, category) | free-exercise-db JSON (https://github.com/yuhonas/free-exercise-db) normalized into Room; rewrite instructions in our own words; cross-check muscle taxonomy with wger | Unlicense: OK for metadata. Images: do not ship until provenance cleared. |
| Muscle taxonomy + overlays | wger muscle IDs/slugs (reference); optionally wger SVG overlays | SA: only if we accept CC-BY-SA on that asset; otherwise draw our own |
| Body map artwork | MuscleMap path data (https://github.com/melihcolpan/MuscleMap) converted to Android path strings, rendered in Compose Canvas (as openGym did) | MIT: OK with notice retained |
| Exercise animations (MVP) | Own joint-angle keyframe mannequin in Compose Canvas, optionally authored from own MediaPipe-captured video (opengym3d concept) | 100% ours |
| Optional 3D later | Mesh2Motion CC0 assets + MakeHuman base mesh | CC0 / MIT: OK (verify MakeHuman asset terms) |
| Local DB / app structure | Room + Compose + Hilt (InFit Apache-2.0 for ideas; GymRoutines GPL ideas only) | Own code |
| Logging UX reference | FitNotes-style flows, openGym, Flexify (MIT) | Reference only |
| Program/progression | Own rules engine; semantics informed by Liftosaur/openGym | No code copying |
| Rep counting (v2) | MediaPipe Tasks Pose Landmarker (Apache-2.0) via CameraX; ML Kit sample k-NN counter as reference | Apache-2.0: OK |
| Health sync | Health Connect `connect-client` + android/health-samples | Apache-2.0: OK |
| Animation runtime if Lottie/Rive wanted | lottie-android (Apache-2.0), rive-android (MIT) | OK |
| Paid shortcut if time-boxed | ExerciseDB or RepDB commercial package for media | Paid license, get written terms |

## 8. Do NOT use

| Item | Reason |
|---|---|
| hasaneyldrm/exercises-dataset GIFs/thumbnails; any GitHub-hosted ExerciseDB/Gym visual GIF copies (exercisedb-pro/exercisedb-dataset, azilRababe/Exercises_Dataset, mohamedatef90/exercise-library, WorkoutX GIFs) | Redistributed, rights unresolved/contradictory (Gym visual vs ExerciseDB), non-transferable permission. Fatal for commercialization. |
| ExerciseDB hosted API as a runtime dependency | Paid, rate-limited, v2 playground flagged unstable; raw redistribution forbidden. Only with a purchased license. |
| wger / openGym / Liftosaur / LiftLog / GymRoutines / norrdev OpenGym / Open-GYM / FitoTrack / RunnerUp code | AGPL/GPL: copying or linking forces open-sourcing the whole app. Ideas only. (App-store exceptions cover the original authors' distribution, not us.) |
| Ultralytics YOLO pose | AGPL-3.0 (commercial license required). |
| Z-Anatomy kidney / inner-ear parts | CC BY-NC / NC-SA. Rest is CC BY-SA 4.0 (share-alike) and heavy. |
| wger exercise images without checking per-image license | Mixed CC licenses per record; some may be NC (unverified). |
| ExerciseDB/muscle-visualizer-api and other unlicensed repos (simonalveteg Workout-Tracker, StronGo, KraftLog) | No license = all rights reserved. |
| Mixamo raw files bundled as redistributable pack | Terms forbid redistributing raw files and ML training; only baked app output after legal review. |
| AI-generated exercise videos for form instruction | Form/anatomy errors = safety and liability risk; license varies by generator. |
| react-native-body-highlighter paths (until provenance confirmed) | MIT wrapper but artwork origin undocumented; prefer MuscleMap (MIT, documented). |

## 9. Open questions to verify before build
- Exact origin of free-exercise-db photos (check upstream wrkout/exercises.json issues).
- Per-image licenses across wger images.
- MakeHuman asset license text (current version).
- MediaPipe pose model files license per model card.
- Compare openGym/MuscleMap body-map completeness (front/back, sub-muscles) before choosing paths to convert.
