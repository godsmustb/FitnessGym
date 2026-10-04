# Content schema: exercises and motions

All content is JSON under `content/`. Adding an exercise means editing data, never code. Unit tests in `anim/src/test` check every rule below.

## content/exercises.json

A JSON array of exercise objects.

```json
{
  "id": "dumbbell-bench-press",
  "name": "Dumbbell Bench Press",
  "tags": ["dumbbell"],
  "equipment": "dumbbell",
  "category": "strength",
  "level": "beginner",
  "mechanic": "compound",
  "force": "push",
  "primary": ["chest"],
  "secondary": ["shoulders", "triceps"],
  "motion": "bench_press",
  "implement": "dumbbell",
  "unilateral": false,
  "instructions": ["Sit on the end of a flat bench...", "..."],
  "cues": ["Shoulder blades pinned back", "Lower for 2-3 seconds", "Press up and slightly in"],
  "mistakes": ["Flaring elbows to 90 degrees", "Bouncing the weights"],
  "source": "free-exercise-db"
}
```

| Field | Rule |
|---|---|
| `id` | kebab-case and unique |
| `tags` | One or more of the **6 library tags**: `bodyweight`, `dumbbell`, `machine`, `pilates`, `kickboxing`, `cardio`. Add `mat` when the exercise is done on the floor (the owner has a yoga mat). |
| `equipment` | `bodyweight`, `dumbbell`, `machine`, `cable`, `cardio_machine` or `mat`. `cable` counts as a gym machine (it gets the `machine` tag). |
| `category` | `strength`, `plyometrics`, `cardio`, `pilates`, `kickboxing` or `stretching` |
| `level` | `beginner`, `intermediate` or `expert` |
| `mechanic` and `force` | Free-exercise-db values. Either may be `null`. |
| `primary` and `secondary` | Muscle group ids from the list below. `primary` must not be empty. |
| `motion` | A motion template id from the catalog below |
| `implement` | `none`, `dumbbell`, `gloves`, `cable`, `handle` or `rope`. This is what is drawn in the figure's hands. |
| `unilateral` | true when one side works at a time |
| `cues` | 2–4 short coaching cues. Use an external focus for technique ("push the floor away"), plus one muscle-focus cue for isolation moves. |
| `mistakes` | 2–3 common mistakes |
| `source` | `free-exercise-db` (Unlicense) or `fitnessgym` (written by us) |

### Muscle group ids (19)

`abdominals, obliques, abductors, adductors, biceps, calves, chest, forearms, glutes, hamstrings, hip_flexors, lats, lower_back, middle_back, neck, quadriceps, shoulders, traps, triceps`

These are the free-exercise-db names, with spaces replaced by `_`. We added `obliques` and `hip_flexors`.

## content/motions/<id>.json: motion templates

Each animation is a looping keyframe file for one parametric anatomical figure. Many exercises share one template: a dumbbell curl and a cable curl both use `curl`, with a different `implement`.

See `docs/MOTION_AUTHORING.md` for the keyframe format and coordinate conventions.

### Motion catalog (template ids)

**Standing, free weight and bodyweight:**
- `squat`: also goblet squat, dumbbell squat, Smith machine squat, sumo squat
- `wall_sit`
- `lunge`: forward, reverse, walking and static split squat
- `bulgarian_split_squat`
- `step_up`
- `hinge`: Romanian or stiff-leg deadlift, dumbbell deadlift, good morning, dumbbell swing-style
- `standing_overhead_press`
- `seated_overhead_press`: seated on a bench with dumbbells
- `lateral_raise`
- `front_raise`
- `rear_delt_fly`: bent-over reverse fly
- `upright_row`
- `shrug`
- `curl`: standing, with biceps, hammer and cable variants
- `concentration_curl`
- `overhead_triceps_extension`
- `triceps_kickback`
- `bent_over_row`: two arms
- `one_arm_row`: supported on a bench
- `calf_raise`
- `woodchop`
- `farmer_walk`
- `wrist_curl`: seated, forearms on thighs
- `side_bend`

**Floor and bench:**
- `pushup`: also knee push-up, incline and decline
- `plank`
- `side_plank`
- `crunch`
- `situp`
- `bicycle_crunch`
- `russian_twist`
- `leg_raise`: lying
- `hanging_knee_raise`: bar or captain's chair
- `glute_bridge`: also hip thrust
- `bird_dog`
- `superman`: prone back extension
- `dead_bug`
- `quadruped_kickback`: donkey kick, fire hydrant
- `clamshell`
- `side_leg_raise`: side-lying
- `mountain_climber`
- `bench_dip`
- `parallel_dip`
- `pullup`: also chin-up and assisted pull-up
- `inverted_row`
- `bench_press`: flat dumbbell press, floor press
- `incline_press`
- `chest_fly`: lying dumbbell fly
- `pullover`
- `lying_triceps_extension`

**Gym machines (GoodLife / LA Fitness style):**
- `machine_chest_press`
- `pec_deck`
- `cable_crossover`
- `machine_shoulder_press`
- `lat_pulldown`
- `seated_cable_row`: also machine row
- `face_pull`
- `triceps_pushdown`
- `machine_curl`: preacher-style
- `leg_press`
- `leg_extension`
- `leg_curl`: lying or seated
- `hip_abduction_machine`
- `hip_adduction_machine`
- `seated_calf_raise`
- `cable_kickback`
- `back_extension`: hyperextension bench
- `ab_crunch_machine`

**Cardio:**
- `jumping_jack`
- `high_knees`
- `butt_kicks`
- `run_in_place`
- `burpee`
- `jump_squat`
- `jump_rope`
- `skater_hop`
- `treadmill_walk`
- `treadmill_run`
- `stationary_bike`
- `elliptical`
- `rowing_machine`
- `stair_climber`

**Kickboxing (orthodox stance, left foot forward):**
- `boxing_stance`: bounce and shuffle
- `jab`
- `cross`
- `hook`
- `uppercut`
- `jab_cross`
- `front_kick`
- `roundhouse_kick`
- `side_kick`
- `knee_strike`
- `bob_and_weave`

**Pilates (on the mat):**
- `hundred`
- `roll_up`
- `single_leg_stretch`
- `double_leg_stretch`
- `single_leg_circle`
- `rolling_like_a_ball`
- `spine_stretch_forward`
- `saw`
- `swan`
- `swimming`
- `teaser`
- `scissors`
- `leg_pull_front`
- `spine_twist`

`criss_cross` reuses `bicycle_crunch`, `shoulder_bridge` reuses `glute_bridge`, and the side-kick series reuses `side_leg_raise`.
