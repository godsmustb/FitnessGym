# Third-party content and code

Fitness Gym is licensed **AGPL-3.0** (see LICENSE). It is open source and for personal use.

| Item | Use | License | Notes |
|---|---|---|---|
| free-exercise-db (yuhonas) https://github.com/yuhonas/free-exercise-db | Exercise names, muscles, equipment, level and instructions for 337 imported exercises | Unlicense (public domain) | Photos **not** used (provenance undocumented) |
| Jetpack Compose, AndroidX, Material 3, Navigation | UI | Apache-2.0 | |
| kotlinx.serialization | JSON | Apache-2.0 | |
| JUnit 4 | Tests | EPL-1.0 | test only |

## Our own work (AGPL-3.0)
- The anatomical figure, rig/IK, muscle model, and all motion templates in `content/motions/`.
- All cues and mistakes, and every Pilates, kickboxing, cardio and gap-filler exercise in `tools/seed/authored.json`.

## Deliberately NOT used
- ExerciseDB / "Gym visual" GIFs and their GitHub copies: ownership is disputed, and committing them to a public repo would redistribute them.
- GPL/AGPL app code from openGym, wger, Liftosaur and others. We could legally reuse it under AGPL, but nothing has been copied so far. Credit it here if that changes.
