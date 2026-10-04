# Fitness Gym: research synthesis (Oct 4, 2026)

This is the one-page "why" behind every product decision. The full sources are in `docs/research/`:
- `01_OPEN_SOURCE.md`: repos, exercise datasets, media licensing, body maps, pose/rep counting, Health Connect
- `02_RESEARCH_PAPERS.md`: about 90 papers on programming, exercise selection, AI generation, computer vision, instruction media, adherence and safety
- `03_MARKET_AND_COMPLAINTS.md`: 25-app competitor matrix, top-10 complaints, practitioner needs, monetization, white space

Known gaps in the research:
- The search tools blocked Reddit, so the complaint evidence comes from Trustpilot, app-store aggregators and review blogs.
- Influencer views are inferred from their products, such as Stronger by Science, RP and Nippard, not from direct quotes.
- Paper metadata was checked from abstracts only.

Items tagged "(unverified)" in the source files need a manual pass.

---

## 1. What "OpenGym" is

The most likely match is **openGym by Duarte Santos**: https://github.com/DuarteSantos8/openGym
- It has about 2.7k stars and was created in July 2026.
- It is licensed **AGPL-3.0**.
- It is a React/Vite + Node PWA, wrapped as an APK with Capacitor.

Its features are almost exactly our brief:
- 1,324 exercises with animated demos and a muscle body map
- filtering by the equipment you own
- weekly routines with progression rules
- logging with PR detection, plus a muscle volume/recovery map
- an optional AI coach

What this means for us:
- **Update (owner decision, Oct 4): our repo is AGPL-3.0, open source and personal-use, so openGym's AGPL logic and data mappings can be reused with credit.** Its GIFs still can't, because their ownership is disputed.
- Its own NOTICE says the ownership of its exercise GIFs is **"unresolved"**. Those GIFs are the same ExerciseDB / Gym visual media that fills most of GitHub.
- "Building something like OpenGym" therefore means two things:
  - the same feature scope
  - a clean-room implementation with media we own

Other projects share the name: norrdev/OpenGym (Flutter, GPL) and AssiamahS/opengym3d (MIT). opengym3d is a 3D exercise-animation pipeline, and its idea is useful to us.

## 2. Competitive landscape (short)

| Cluster | Apps | What they nail | What they miss |
|---|---|---|---|
| Fast loggers | Hevy, Strong, FitNotes | Speed and history | Core features behind the paywall (Hevy limits charts to 3 months and allows 4 routines; Strong allows 3 routines); weak "what next" |
| AI generators | Fitbod, JEFIT, Freeletics | Equipment-aware generated sessions | Repetitive or illogical picks, bad warm-ups, opaque logic, billing traps |
| Evidence-based programs | RP Hypertrophy, Alpha Progression, Boostcamp, Liftosaur, StrongLifts | Progression and mesocycles | RP costs $34.99/mo; little visual form teaching; steep for beginners |
| Video and class apps | Nike Training Club, Apple Fitness+, Gymshark, Ladder | Production quality, motivation | Little real logging or progression; not tied to your equipment |
| Human coaching | Future, Caliber | Accountability | Future costs $199/mo |
| Exercise reference | MuscleWiki | Muscle map, then exercise, then video | Weak plan and logging |

**Nobody combines all three of these:** equipment-aware generation, an animated form demo with muscle highlight *inside the logging row*, and evidence-based, explainable progression. That gap is our product.

## 3. Top 10 user complaints, ranked, and our answer

| # | Complaint | Seen in | Our answer |
|---|---|---|---|
| 1 | Trial-to-annual billing traps, hard cancellation, no refunds | Fitbod, JEFIT and others | No paywall in v1. Any future billing has no-card trials, renewal reminders and one-tap cancel. |
| 2 | Core features and history held hostage behind the paywall | Hevy, Strong | Unlimited routines, history, charts and export, always free |
| 3 | Repetitive or illogical generated workouts, bad warm-ups | Fitbod | A rules engine with movement-pattern balance, a variety budget, and auto warm-up ramps. **Every pick shows "why this exercise"**. |
| 4 | Data loss, crashes, watch sync failures | Many | Offline-first Room DB, write-ahead set logging (every tap is saved immediately), auto backup, CSV/JSON export |
| 5 | Logging friction: no "next set" cursor, can't edit a routine mid-workout | Hevy, Strong | A focus cursor that jumps to the next set. One-tap "same as last time". Swap, add or reorder mid-workout. |
| 6 | Rest timer silent outside the app; weak watch support | Many | A foreground-service rest timer with notification, vibration and lock-screen controls. Wear OS in a later phase. |
| 7 | AI and coaching overpriced for the value | Fitbod, RP, Future | On-device logic. The optional AI explainer runs on your laptop over Tailscale for $0. |
| 8 | Unresponsive support | Many | For you, a health screen plus error codes with a plain-English fix (GuitarNoodle pattern) |
| 9 | Equipment and exercise gaps; can't exclude exercises; settings reset | Many | Multiple gym profiles with exact equipment and increments, "never show me this" exclusions, and custom exercises |
| 10 | No readiness awareness; linear plans plateau | StrongLifts and others | A 10-second pre-workout check-in (sleep, soreness, energy). Reactive deloads. Double progression plus RIR. |

## 4. What lifters, coaches and influencers say a great app needs

These are the table stakes. If any one is missing, serious lifters leave.
- Logging a set in **3 taps or fewer**, with values pre-filled from last time.
- A rest timer that works on the lock screen.
- Warm-up set calculator and plate calculator.
- Supersets and circuits.
- Optional RPE/RIR per set.
- Automatic PRs and estimated 1RM.
- Substitution when the gym is busy.
- Notes per exercise, such as seat height or grip.
- Full history and charts.
- Offline use.
- Data export.
- Watch support.

These are differentiators:
- Explainable progression ("+2.5 kg because you hit 3×12 at RIR 2")
- Weekly volume per muscle against a target zone
- Form cues at the moment they're needed
- Program templates from known coaches' published methods, such as 5/3/1, GZCL, PPL and full-body

## 5. Evidence-based defaults for the generator

Full table with citations: `02_RESEARCH_PAPERS.md` §(a).

**Weekly volume per muscle**
- Count direct sets as 1 and indirect sets as 0.5.
- Targets by level:

  | Level | Sets/muscle/week |
  |---|---|
  | Beginner | 4–8 |
  | Intermediate | 10–16 |
  | Advanced | 12–20 |

- Cap at about 6–11 sets per muscle per session (Pelland 2025; Remmert).

**Session structure**
- Train each muscle twice a week.
- Hypertrophy works across a wide range: 6–20 reps at 0–3 RIR.
- Beginners stay at 2–4 RIR with no failure.
- Rest 60–120 s, and 2–3 min for heavy compounds.

**Progression**
- Use double progression (Plotkin 2022): top of the rep range on all sets at the target RIR, then add the smallest load step.
- Deloads are **reactive**, not scheduled. Evidence for scheduled deloads is thin.

**Equipment and exercise choice**
- Machines, free weights, bands and bodyweight are interchangeable for hypertrophy. Swap within the movement pattern.
- For a strength goal, keep the lift you are testing.
- Favour a variant that loads the muscle at a long length (stretch-position benefit).

**Cues**
- Use external cues for technique, such as "push the floor away".
- Use internal "feel the muscle" cues on light isolation sets (Schoenfeld 2018).

**Safety and AI**
- Screen with PAR-Q+ at onboarding.
- Never let an LLM write the plan. Expert studies find LLM plans plausible but weak on progression and safety. **Rules engine plans; LLM only explains.**

**Adherence**
- Habit formation takes a median of about 66 days, so plan a 12-week onboarding arc.
- Use forgiving streaks and a "minimum session" option.

## 6. Media and licensing: the key build decision

- **Do NOT ship any of the following:**
  - the ExerciseDB / Gym visual GIF copies on GitHub (rights are contradictory; the problem is fatal if we commercialize)
  - YOLO pose (AGPL)
  - any GPL/AGPL tracker code
- **Safe metadata seed:** free-exercise-db (Unlicense, 800+ exercises with equipment and primary/secondary muscles).
  - We rewrite the instructions in our own words.
  - We don't ship its photos until their source is cleared.
- **Body map:** MuscleMap SVG paths (MIT), converted to Compose vector paths, as openGym did.
- **Animations:** we make our own. The plan proposes procedural mannequin animation in Compose Canvas, driven by joint-angle keyframes. Keyframes can be captured from your own videos with MediaPipe on the laptop. The result is 100% owned, small, recolourable, and highlights the target muscle on the figure.
- **Rep counting (later):** MediaPipe Pose Landmarker (Apache-2.0) with CameraX. Phone pose estimation works for standing lifts and degrades with occlusion, so start with rep counting plus a few angle cues and label the scores as estimates.
- **Health sync:** Health Connect 1.1 (Apache-2.0).
