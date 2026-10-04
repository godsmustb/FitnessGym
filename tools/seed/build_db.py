#!/usr/bin/env python3
"""Build content/exercises.json from free-exercise-db plus our authored exercises.

Inputs  (tools/seed):  free-exercise-db.json, motion_map.json, cues.json, authored.json
Outputs:               content/exercises.json, tools/seed/skipped.txt
Run:                   python tools/seed/build_db.py
"""
import json
import re
import sys
from collections import Counter
from pathlib import Path

SEED = Path(__file__).resolve().parent
ROOT = SEED.parent.parent
OUT = ROOT / "content" / "exercises.json"

MUSCLES = set("abdominals obliques abductors adductors biceps calves chest forearms glutes hamstrings "
              "hip_flexors lats lower_back middle_back neck quadriceps shoulders traps triceps".split())
MOTIONS = set("""squat wall_sit lunge bulgarian_split_squat step_up hinge standing_overhead_press seated_overhead_press
lateral_raise front_raise rear_delt_fly upright_row shrug curl concentration_curl overhead_triceps_extension
triceps_kickback bent_over_row one_arm_row calf_raise woodchop farmer_walk wrist_curl side_bend pushup plank side_plank
crunch situp bicycle_crunch russian_twist leg_raise hanging_knee_raise glute_bridge bird_dog superman dead_bug
quadruped_kickback clamshell side_leg_raise mountain_climber bench_dip parallel_dip pullup inverted_row bench_press
incline_press chest_fly pullover lying_triceps_extension machine_chest_press pec_deck cable_crossover
machine_shoulder_press lat_pulldown seated_cable_row face_pull triceps_pushdown machine_curl leg_press leg_extension
leg_curl hip_abduction_machine hip_adduction_machine seated_calf_raise cable_kickback back_extension ab_crunch_machine
jumping_jack high_knees butt_kicks run_in_place burpee jump_squat jump_rope skater_hop treadmill_walk treadmill_run
stationary_bike elliptical rowing_machine stair_climber boxing_stance jab cross hook uppercut jab_cross front_kick
roundhouse_kick side_kick knee_strike bob_and_weave hundred roll_up single_leg_stretch double_leg_stretch
single_leg_circle rolling_like_a_ball spine_stretch_forward saw swan swimming teaser scissors leg_pull_front
spine_twist""".split())
LIBRARY_TAGS = {"bodyweight", "dumbbell", "machine", "pilates", "kickboxing", "cardio"}
EQUIPMENT = {"bodyweight", "dumbbell", "machine", "cable", "cardio_machine", "mat"}
CATEGORIES = {"strength", "plyometrics", "cardio", "pilates", "kickboxing", "stretching"}
LEVELS = {"beginner", "intermediate", "expert"}
IMPLEMENTS = {"none", "dumbbell", "gloves", "cable", "handle", "rope"}

IMPORT_EQUIPMENT = {"body only", "dumbbell", "machine", "cable", None}
IMPORT_CATEGORIES = {"strength", "plyometrics", "cardio", "powerlifting", "strongman"}
EQUIP_OUT = {"body only": "bodyweight", None: "bodyweight", "dumbbell": "dumbbell", "machine": "machine", "cable": "cable"}


def kebab(s):
    return re.sub(r"[^a-z0-9]+", "-", s.lower()).strip("-")


def muscle(m):
    return m.strip().lower().replace(" ", "_")


def dedupe(seq):
    seen, out = set(), []
    for x in seq:
        if x not in seen:
            seen.add(x)
            out.append(x)
    return out


def clean(s):
    return re.sub(r"\s+", " ", s.replace("�", "")).strip()


def category_out(c):
    return "strength" if c in ("powerlifting", "strongman") else c


SOURCE_URL = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist/exercises.json"


def main():
    srcf = SEED / "free-exercise-db.json"
    if not srcf.exists():  # not committed (1 MB); fetch the public-domain source on first run
        import urllib.request
        urllib.request.urlretrieve(SOURCE_URL, srcf)
    src = json.loads(srcf.read_text(encoding="utf8"))
    mm = json.loads((SEED / "motion_map.json").read_text(encoding="utf8"))
    cmap, skipmap = mm["map"], mm["skip"]
    cues = json.loads((SEED / "cues.json").read_text(encoding="utf8"))
    authored = json.loads((SEED / "authored.json").read_text(encoding="utf8"))

    skipped = []        # (id, name, reason)
    auto_skip = Counter()
    result = []
    for x in src:
        eq, cat = x["equipment"], x["category"]
        sid = x["id"]
        if cat == "stretching":
            auto_skip["stretching (skipped for now)"] += 1
            continue
        if eq not in IMPORT_EQUIPMENT:
            auto_skip["equipment not available (barbell, EZ bar, kettlebell, bands, balls, foam roll, sled, other, etc.)"] += 1
            continue
        if cat not in IMPORT_CATEGORIES:
            auto_skip["category not imported (" + cat + ")"] += 1
            continue
        if sid in skipmap:
            skipped.append((sid, x["name"], skipmap[sid]))
            continue
        if sid not in cmap:
            sys.exit("UNMAPPED candidate: " + sid)
        e = cmap[sid]
        if sid not in cues:
            sys.exit("no cues for " + sid)
        prim = [muscle(m) for m in e.get("primary", x["primaryMuscles"])]
        sec = [muscle(m) for m in e.get("secondary", x["secondaryMuscles"])]
        sec = dedupe(sec + e.get("add_secondary", []))
        sec = [m for m in sec if m not in prim]
        eid = kebab(sid)
        result.append({
            "id": eid,
            "name": x["name"],
            "tags": dedupe(e["tags"]),
            "equipment": EQUIP_OUT[eq],
            "category": category_out(cat),
            "level": x["level"],
            "mechanic": x["mechanic"],
            "force": x["force"],
            "primary": prim,
            "secondary": sec,
            "motion": e["motion"],
            "implement": e["implement"],
            "unilateral": bool(e["unilateral"]),
            "instructions": e.get("instructions") or [clean(s) for s in x["instructions"]],
            "cues": cues[sid]["cues"],
            "mistakes": cues[sid]["mistakes"],
            "source": "free-exercise-db",
        })
    for a in authored:
        result.append(a)

    result.sort(key=lambda r: (",".join(sorted(r["tags"])), r["name"].lower()))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n", encoding="utf8")

    lines = ["# Exercises from free-exercise-db that were not imported", "",
             "## Skipped because no motion template fits, needs missing equipment, or replaced by an authored entry", ""]
    for sid, name, reason in sorted(skipped):
        lines.append(f"{sid} | {name} | {reason}")
    lines += ["", "## Bulk filters (counts)", ""]
    for k, v in sorted(auto_skip.items()):
        lines.append(f"{v:4d} | {k}")
    (SEED / "skipped.txt").write_text("\n".join(lines) + "\n", encoding="utf8")

    errors = validate(result)
    report(result, len(skipped), sum(auto_skip.values()))
    if errors:
        print("\nVALIDATION ERRORS (%d):" % len(errors))
        for e in errors[:60]:
            print(" -", e)
        sys.exit(1)
    print("\nValidation: OK")


def validate(items):
    errs = []
    ids = Counter(i["id"] for i in items)
    for k, v in ids.items():
        if v > 1:
            errs.append("duplicate id " + k)
    for i in items:
        p = i["id"]
        if not re.fullmatch(r"[a-z0-9]+(-[a-z0-9]+)*", p):
            errs.append(f"{p}: id not kebab-case")
        if not i["tags"] or not (set(i["tags"]) & LIBRARY_TAGS):
            errs.append(f"{p}: needs a library tag")
        for t in i["tags"]:
            if t not in LIBRARY_TAGS and t != "mat":
                errs.append(f"{p}: bad tag {t}")
        if i["equipment"] not in EQUIPMENT:
            errs.append(f"{p}: bad equipment {i['equipment']}")
        if i["equipment"] in ("cable", "machine", "cardio_machine") and "machine" not in i["tags"]:
            errs.append(f"{p}: gym equipment missing machine tag")
        if i["category"] not in CATEGORIES:
            errs.append(f"{p}: bad category {i['category']}")
        if i["level"] not in LEVELS:
            errs.append(f"{p}: bad level {i['level']}")
        if not i["primary"]:
            errs.append(f"{p}: empty primary")
        for m in i["primary"] + i["secondary"]:
            if m not in MUSCLES:
                errs.append(f"{p}: unknown muscle {m}")
        if set(i["primary"]) & set(i["secondary"]):
            errs.append(f"{p}: muscle in both primary and secondary")
        if i["motion"] not in MOTIONS:
            errs.append(f"{p}: unknown motion {i['motion']}")
        if i["implement"] not in IMPLEMENTS:
            errs.append(f"{p}: bad implement {i['implement']}")
        if not isinstance(i["unilateral"], bool):
            errs.append(f"{p}: unilateral not bool")
        if not (2 <= len(i["cues"]) <= 4):
            errs.append(f"{p}: cues count {len(i['cues'])}")
        if not (2 <= len(i["mistakes"]) <= 3):
            errs.append(f"{p}: mistakes count {len(i['mistakes'])}")
        if not i["instructions"]:
            errs.append(f"{p}: no instructions")
        if i["source"] not in ("free-exercise-db", "fitnessgym"):
            errs.append(f"{p}: bad source")
    return errs


def report(items, n_skip_listed, n_skip_bulk):
    tags = Counter(t for i in items for t in i["tags"])
    print("Total exercises:", len(items))
    print("Skipped: %d listed in skipped.txt + %d by bulk filters" % (n_skip_listed, n_skip_bulk))
    print("\nCounts per tag:")
    for t, c in sorted(tags.items()):
        print(f"  {t:12s} {c}")
    motions = Counter(i["motion"] for i in items)
    print("\nCounts per motion:")
    for m in sorted(MOTIONS):
        print(f"  {m:28s} {motions.get(m, 0)}")
    zero = sorted(m for m in MOTIONS if not motions.get(m))
    print("\nMotions with zero exercises:", zero or "none")
    unknown = sorted(set(motions) - MOTIONS)
    if unknown:
        print("Unknown motions used:", unknown)


if __name__ == "__main__":
    main()
