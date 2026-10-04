# Motion authoring guide

Each file in `content/motions/<id>.json` is one looping animation of the anatomical figure. The engine is in `anim/src/main/kotlin/com/nunna/fitnessgym/anim/`:
- `Rig.kt`: inverse kinematics (IK)
- `Scene.kt`: drawing
- `Motion.kt`: keyframes

**Worked examples to copy:** `squat.json`, `pushup.json`, `curl.json`, `leg_extension.json` and `jab.json`.

## Coordinates (metres, ~1.80 m figure)

- **y is up and the floor is y = 0.**
- **The figure faces +z when yaw = 0. The figure's LEFT is +x and its RIGHT is -x.**

Standing reference values:

| Point | Position |
|---|---|
| Pelvis centre | `[0, 0.97, 0]` |
| Hip joints | pelvis + `(±0.09, -0.06, 0)` |
| Neck base | 0.52 above the pelvis |
| Shoulder joints | neck base + `(±0.19, -0.04, 0)` |
| Head top | about 1.80 |

Segment lengths:

| Segment | Length |
|---|---|
| Thigh | 0.43 |
| Shin | 0.40 |
| Ankle height when standing | 0.08 |
| Upper arm | 0.30 |
| Forearm | 0.26 |
| Hand | 0.08 (past the wrist) |
| Foot | heel 0.04 behind the ankle, toe 0.17 ahead |

**Reach limits:**
- Hip to ankle is at most 0.83.
- Shoulder to wrist is at most 0.56.

Targets beyond reach clamp to a straight limb, which is fine for straight arms and legs.

## Body orientation (in degrees, per key)

| Field | Meaning |
|---|---|
| `pelvis` | `[x, y, z]` world position of the pelvis centre |
| `pitch` | + leans forward. 90 = horizontal face-down (prone, head toward +z). -90 = lying on the back (supine, head toward -z). 0 = upright. |
| `roll` | + tips toward the figure's LEFT. 90 = lying on the left side, head toward +x. |
| `yaw` | + turns to the figure's left. For example, yaw -30 turns the body 30° to its right (boxing stance). |
| `spine` | `[flex, roll, yaw]` of the chest relative to the pelvis, spread over the lumbar and thoracic spine. flex + = crunch forward, flex - = arch back. yaw = twist. |
| `neck` | + = chin down / look down. - = look up. |

Useful body orientations:
- **Seated upright:** `pitch` 0 to -10, with the pelvis at seat height (seat top + 0.08).
- **Lying supine on the floor:** `pitch -90`, pelvis y ≈ 0.12.
- **Supine on a bench** (bench top y ≈ 0.45): pelvis y ≈ 0.57.
- **Prone:** `pitch 90`, pelvis y ≈ 0.12.

## Limbs: targets plus IK

Hands and feet are **targets for the wrist and ankle**. The IK places the elbows and knees.

| Field | Meaning |
|---|---|
| `lh` / `rh` | left/right wrist target, in the template's `handFrame` |
| `lf` / `rf` | left/right ankle target, in the template's `footFrame` |
| `lep` / `rep` | elbow pole: the direction the elbow points, **always in the chest frame**. Default `[0.35, -0.2, -1]`, which is back, slightly down and out. |
| `lkp` / `rkp` | knee pole: the direction the knee points, **always in the pelvis frame**. Default `[0.15, 0, 1]`, which is forward. |
| `lfa` / `rfa` | foot pitch. + = toes up. - = pointed toes (calf raise, plank toes -75). |
| `lfy` / `rfy` | toe-out in degrees, default 8 |

**Frames** (template level):

| Frame | Origin and axes | Use it when |
|---|---|---|
| `"world"` | Absolute coordinates | Hands or feet are fixed on the floor, a bar or a handle |
| `"chest"` | Origin at the neck base. Axes rotate with the chest: x = the figure's left, y = toward the head, z = out of the chest. | Arms move with the torso: presses, curls, raises, punches. One set of hand values works standing, seated or lying. |
| `"pelvis"` | Origin at the pelvis centre, rotating with the pelvis | Legs move relative to the hips: seated machines, leg raises, kicks while lying |

**Mirroring:** if a right-side field (`rh`, `rf`, `rep`, `rkp`, `rfa`, `rfy`) never appears in any key, it mirrors the left side automatically (x → -x). For symmetric moves, give only the left side. For asymmetric moves (lunges, punches, kicks, walking), give both sides in the first key. After that, any field you leave out of a key carries forward from the previous key.

## Keys and effort

- `keys`: a list of keys with `t` in [0, 1) inside the loop. The last key blends back into the first, so don't repeat t = 1.
- `duration`: the seconds per loop.
- `e`: effort from 0 to 1. It drives how brightly the exercise's muscles glow:
  - about 0.15 at rest or the start
  - 0.4–0.6 while lowering (eccentric)
  - **1.0 at the hardest point of the lift (concentric)**
- `el` / `er`: separate left/right effort for one-sided moves. A jab is `el` 1.0 and `er` 0.1.

Typical lift timing:
- lower over about 0.45 of the loop
- a short bottom pause
- lift by about 0.85
- hold at the top

Cardio and kickboxing loops are faster (duration 0.8–1.6 s).

## Camera and props

- `view: { "yaw": -40, "pitch": 8 }` is the default camera.
  - yaw is the camera orbit: 0 = front, -90 = the figure's right side, so the figure faces screen-right.
  - Pick the view that shows the movement best. Use about -70 to -90 for anything moving front-to-back (punches, rows, presses, lying exercises), and about -20 to -35 for side-to-side movement (lateral raise, jumping jack).
- `props` (scenery, in world coordinates):
  - `{"type":"mat","min":[...],"max":[...]}`: the yoga mat. The default runs along z (-1 to 1) and is 0.66 wide. Set min/max so it lies under the body.
  - `{"type":"box","min":[x,y,z],"max":[x,y,z],"color":"frame|pad|steel|dark|accent"}`: benches, seats, back pads, machine frames, steps, treadmill decks. Benches are pads about 0.45 high. A flat bench is about 0.3 wide and 1.2 long.
  - `{"type":"cable","from":[x,y,z],"to":"hands|lh|rh|handsMid|lf|rf"}`: a cable from a pulley to the hands.
  - `{"type":"bar","to":"hands|feet|heels|knees","color":"steel|pad","radius":0.01}`: a bar or handle between both hands, or a roller pad on the ankles or knees.
  - `{"type":"disc","at":[x,y,z],"axis":[1,0,0],"radius":0.25,"color":"frame"}`: a flywheel or wheel.
- **Things the exercise draws itself, so don't add them:** dumbbells, gloves, cable handles and jump ropes come from the exercise's `implement`.

## Workflow (do this for every template)

1. Write the JSON, then render it:
   ```
   cd "C:\Users\scnun\Projects\Fitness Gym"
   preview\build\install\preview\bin\preview.bat <id> [<id> ...]
   ```
   This renders 6 phases × 2 views to `preview-out/<id>.png`. Open the PNG with the Read tool and **look at it**.
   - To check a pose more closely, use `big:<id>`.
   - To check the glow, use `<id>@quadriceps,glutes/hamstrings`. The default glow comes from the first exercise that uses the template.
2. Check:
   - Feet on the floor, not floating or sinking (unless jumping).
   - Hands on the handles, bar or floor where they should be.
   - Elbows and knees bend the anatomically correct way.
   - The motion reads clearly in the default view.
   - The body doesn't pass through the bench, seat or floor.
   - The glow peaks on the lifting phase.
3. Fix it and render again, until it looks like a coach demonstrating good form.
