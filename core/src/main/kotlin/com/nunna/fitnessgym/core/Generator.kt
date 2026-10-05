package com.nunna.fitnessgym.core

import com.nunna.fitnessgym.anim.Exercise
import kotlin.random.Random

/** One prescribed exercise in a plan day. [seconds] is the target for timed / cardio work. */
data class PlanSlot(
    val exerciseId: String,
    val sets: Int,
    val repMin: Int,
    val repMax: Int,
    val rir: Int,
    val restSec: Int,
    val seconds: Int = 0,
    val role: Role,
    val tracking: Tracking,
)

data class PlanDay(val name: String, val focus: String, val slots: List<PlanSlot>)
data class Plan(val days: List<PlanDay>, val seed: Long)

enum class Role { MAIN, ACCESSORY, ISOLATION, CORE, FINISHER }

/** A movement "job" in a workout. Exercises fill slots by their motion template. */
enum class Slot(val motions: Set<String>, val role: Role, val fallback: List<String> = emptyList()) {
    KNEE(setOf("squat", "leg_press"), Role.MAIN, listOf("KNEE_UNI")),
    KNEE_UNI(setOf("lunge", "bulgarian_split_squat", "step_up"), Role.ACCESSORY, listOf("KNEE")),
    QUAD_ISO(setOf("leg_extension"), Role.ISOLATION, listOf("KNEE_UNI", "KNEE")),
    HINGE(setOf("hinge"), Role.MAIN, listOf("GLUTE", "HAM_ISO")),
    GLUTE(setOf("glute_bridge", "cable_kickback", "quadruped_kickback", "hip_abduction_machine"), Role.ACCESSORY, listOf("HINGE")),
    HAM_ISO(setOf("leg_curl", "back_extension"), Role.ISOLATION, listOf("GLUTE", "HINGE")),
    H_PUSH(setOf("bench_press", "machine_chest_press", "pushup"), Role.MAIN, listOf("INCLINE_PUSH", "CHEST_ISO")),
    INCLINE_PUSH(setOf("incline_press"), Role.ACCESSORY, listOf("H_PUSH")),
    CHEST_ISO(setOf("chest_fly", "pec_deck", "cable_crossover"), Role.ISOLATION, listOf("H_PUSH")),
    V_PUSH(setOf("standing_overhead_press", "seated_overhead_press", "machine_shoulder_press"), Role.MAIN, listOf("LATERAL", "H_PUSH")),
    LATERAL(setOf("lateral_raise"), Role.ISOLATION, listOf("REAR_DELT", "V_PUSH")),
    H_PULL(setOf("bent_over_row", "one_arm_row", "seated_cable_row", "inverted_row"), Role.MAIN, listOf("V_PULL", "REAR_DELT", "BACK_BW")),
    V_PULL(setOf("lat_pulldown", "pullup"), Role.MAIN, listOf("H_PULL", "REAR_DELT", "BACK_BW")),
    REAR_DELT(setOf("rear_delt_fly", "face_pull"), Role.ISOLATION, listOf("H_PULL", "BACK_BW")),
    BACK_BW(setOf("superman", "bird_dog"), Role.ACCESSORY),
    BICEPS(setOf("curl", "machine_curl", "concentration_curl"), Role.ISOLATION, listOf("H_PULL")),
    TRICEPS(setOf("triceps_pushdown", "overhead_triceps_extension", "triceps_kickback", "lying_triceps_extension", "bench_dip", "parallel_dip"), Role.ISOLATION, listOf("H_PUSH")),
    CALVES(setOf("calf_raise", "seated_calf_raise"), Role.ISOLATION),
    TRAPS(setOf("shrug"), Role.ISOLATION, listOf("REAR_DELT")),
    CORE(setOf("plank", "side_plank", "crunch", "dead_bug", "bird_dog", "leg_raise", "hanging_knee_raise", "ab_crunch_machine", "woodchop", "bicycle_crunch"), Role.CORE),
}

object Generator {
    /** Staple lifts a coach would actually program; picked ahead of odd variants. */
    val STAPLES = setOf(
        "goblet-squat", "dumbbell-squat", "bodyweight-squat", "leg-press", "hack-squat", "smith-machine-squat",
        "reverse-lunge", "dumbbell-lunges", "split-squat-with-dumbbells", "bodyweight-walking-lunge",
        "bulgarian-split-squat-bodyweight", "dumbbell-step-ups", "stiff-legged-dumbbell-deadlift", "pull-through",
        "glute-bridge", "dumbbell-hip-thrust", "single-leg-glute-bridge", "lying-leg-curls", "seated-leg-curl",
        "leg-extensions", "back-extension-bench", "dumbbell-bench-press", "pushups", "incline-push-up",
        "chest-press-machine", "machine-bench-press", "incline-dumbbell-press", "leverage-incline-chest-press",
        "dumbbell-flyes", "pec-deck-machine", "cable-crossover", "butterfly", "standing-dumbbell-press",
        "seated-dumbbell-press", "shoulder-press-machine", "side-lateral-raise", "cable-seated-lateral-raise",
        "one-arm-dumbbell-row", "bent-over-two-dumbbell-row", "seated-cable-rows", "seated-row-machine",
        "inverted-row", "lat-pulldown", "wide-grip-lat-pulldown", "pullups", "chin-up", "assisted-pull-up-machine",
        "reverse-flyes", "face-pull", "cable-rear-delt-fly", "dumbbell-bicep-curl", "hammer-curls",
        "machine-bicep-curl", "triceps-pushdown", "triceps-pushdown-rope-attachment",
        "standing-dumbbell-triceps-extension", "tricep-dumbbell-kickback", "bench-dips", "dips-triceps-version",
        "standing-calf-raises", "bodyweight-calf-raise", "seated-calf-raise", "dumbbell-shrug", "plank",
        "crunches", "dead-bug", "bird-dog", "reverse-crunch", "side-bridge", "hanging-leg-raise", "cable-crunch",
        "cable-woodchop", "dumbbell-woodchop", "thigh-abductor", "superman", "donkey-kick",
    )

    /** Motions removed for each area the owner wants to protect. */
    val AVOID: Map<AvoidArea, Set<String>> = mapOf(
        AvoidArea.SHOULDERS to setOf(
            "standing_overhead_press", "seated_overhead_press", "machine_shoulder_press", "upright_row",
            "parallel_dip", "bench_dip", "pullover", "overhead_triceps_extension", "jumping_jack",
        ),
        AvoidArea.KNEES to setOf(
            "lunge", "bulgarian_split_squat", "step_up", "jump_squat", "skater_hop", "leg_extension", "burpee",
            "high_knees", "jump_rope", "knee_strike",
        ),
        AvoidArea.LOWER_BACK to setOf(
            "hinge", "bent_over_row", "superman", "back_extension", "situp", "russian_twist", "swan",
            "rolling_like_a_ball", "teaser", "roll_up", "rowing_machine", "woodchop",
        ),
        AvoidArea.WRISTS to setOf("pushup", "bench_dip", "wrist_curl", "mountain_climber", "burpee", "leg_pull_front", "side_plank"),
        AvoidArea.NECK to setOf("shrug", "situp", "crunch", "bicycle_crunch", "hundred"),
    )

    private val S = Slot.entries.associateBy { it.name }

    private fun day(name: String, focus: String, vararg s: Slot) = Triple(name, focus, s.toList())

    private val FULL_A = day("Full Body A", "Squat · push · pull · hinge", Slot.KNEE, Slot.H_PUSH, Slot.H_PULL, Slot.HINGE, Slot.LATERAL, Slot.CORE)
    private val FULL_B = day("Full Body B", "Hinge · overhead · pull-down · lunge", Slot.HINGE, Slot.V_PUSH, Slot.V_PULL, Slot.KNEE_UNI, Slot.BICEPS, Slot.CORE)
    private val FULL_C = day("Full Body C", "Squat · incline · row · glutes", Slot.KNEE, Slot.INCLINE_PUSH, Slot.H_PULL, Slot.GLUTE, Slot.TRICEPS, Slot.CALVES, Slot.CORE)
    private val UPPER_A = day("Upper A", "Chest · back · shoulders · arms", Slot.H_PUSH, Slot.H_PULL, Slot.V_PUSH, Slot.V_PULL, Slot.BICEPS, Slot.TRICEPS)
    private val UPPER_B = day("Upper B", "Back · incline · delts · arms", Slot.V_PULL, Slot.INCLINE_PUSH, Slot.H_PULL, Slot.LATERAL, Slot.REAR_DELT, Slot.TRICEPS, Slot.BICEPS)
    private val LOWER_A = day("Lower A", "Quads · hamstrings · calves", Slot.KNEE, Slot.HINGE, Slot.KNEE_UNI, Slot.HAM_ISO, Slot.CALVES, Slot.CORE)
    private val LOWER_B = day("Lower B", "Hinge · squat · glutes", Slot.HINGE, Slot.KNEE, Slot.GLUTE, Slot.QUAD_ISO, Slot.CALVES, Slot.CORE)
    private val PUSH = day("Push", "Chest · shoulders · triceps", Slot.H_PUSH, Slot.V_PUSH, Slot.INCLINE_PUSH, Slot.CHEST_ISO, Slot.LATERAL, Slot.TRICEPS)
    private val PULL = day("Pull", "Back · rear delts · biceps", Slot.V_PULL, Slot.H_PULL, Slot.REAR_DELT, Slot.BICEPS, Slot.TRAPS, Slot.CORE)
    private val LEGS = day("Legs", "Quads · hamstrings · glutes · calves", Slot.KNEE, Slot.HINGE, Slot.KNEE_UNI, Slot.HAM_ISO, Slot.QUAD_ISO, Slot.CALVES)
    private val RECOVERY = day("Active Recovery", "Core · mobility · easy cardio", Slot.CORE, Slot.BACK_BW, Slot.GLUTE)

    fun split(days: Int) = when (days.coerceIn(1, 7)) {
        1 -> listOf(FULL_A)
        2 -> listOf(FULL_A, FULL_B)
        3 -> listOf(FULL_A, FULL_B, FULL_C)
        4 -> listOf(UPPER_A, LOWER_A, UPPER_B, LOWER_B)
        5 -> listOf(UPPER_A, LOWER_A, PUSH, PULL, LEGS)
        6 -> listOf(PUSH, PULL, LEGS, PUSH.copy(first = "Push B"), PULL.copy(first = "Pull B"), LEGS.copy(first = "Legs B"))
        else -> listOf(PUSH, PULL, LEGS, PUSH.copy(first = "Push B"), PULL.copy(first = "Pull B"), LEGS.copy(first = "Legs B"), RECOVERY)
    }

    private fun levelOk(ex: Exercise, x: Experience) = when (x) {
        Experience.BEGINNER -> ex.level == "beginner"
        Experience.INTERMEDIATE -> ex.level != "expert"
        Experience.ADVANCED -> true
    }

    /** Exercises this owner may be given at all (equipment, avoid areas, exclusions). */
    fun allowed(ex: Exercise, s: UserSettings): Boolean =
        Classify.available(ex, s.profile) && ex.id !in s.excluded &&
            s.avoid.none { ex.motion in (AVOID[it] ?: emptySet()) }

    fun generate(s0: UserSettings, library: List<Exercise>, seed: Long = 1L): Plan {
        val s = s0.validated()
        val rnd = Random(seed)
        val pool = library.filter { allowed(it, s) }
        val usedInPlan = HashMap<String, Int>()
        val interests = buildList {
            addAll(s.interests.sortedBy { it.ordinal })
            if (s.goal == Goal.LOSE_FAT && Interest.CARDIO !in s.interests) add(0, Interest.CARDIO)
        }
        val days = split(s.daysPerWeek).mapIndexed { di, (name, focus, slots) ->
            val usedToday = HashSet<String>()
            val motionsToday = HashSet<String>()
            val out = ArrayList<PlanSlot>()
            for ((si, slot) in slots.withIndex()) {
                val ex = pick(slot, pool, s, rnd, usedToday, motionsToday, usedInPlan) ?: continue
                usedToday += ex.id; motionsToday += ex.motion
                usedInPlan[ex.id] = (usedInPlan[ex.id] ?: 0) + 1
                val role = if (si < 2 && slot.role == Role.MAIN) Role.MAIN else if (slot.role == Role.MAIN) Role.ACCESSORY else slot.role
                out += prescribe(ex, role, s)
            }
            // Finisher from the owner's interests (Pilates / kickboxing / cardio), rotating by day.
            if (interests.isNotEmpty()) {
                val interest = interests[di % interests.size]
                finisher(interest, pool, s, rnd, usedToday, usedInPlan)?.let { ex ->
                    usedInPlan[ex.id] = (usedInPlan[ex.id] ?: 0) + 1
                    out += prescribe(ex, Role.FINISHER, s)
                }
            }
            PlanDay(name, focus, fitToTime(out, s.sessionMinutes))
        }
        return Plan(days, seed)
    }

    private fun score(ex: Exercise, s: UserSettings, usedInPlan: Map<String, Int>, main: Boolean = false): Double {
        var sc = 0.0
        // Heavy strength work is safest and easiest to load on machines / Smith / leg press.
        if (main && s.goal == Goal.GET_STRONGER && ex.equipment == "machine") sc += 3
        if (ex.id in STAPLES) sc += 10
        if (ex.source == "fitnessgym") sc += 4
        if (ex.level == "beginner" && s.experience == Experience.BEGINNER) sc += 2
        if (ex.unilateral) sc -= 1.5
        sc -= 7.0 * (usedInPlan[ex.id] ?: 0)          // variety budget across the week
        return sc
    }

    private fun pick(
        slot: Slot, pool: List<Exercise>, s: UserSettings, rnd: Random,
        usedToday: Set<String>, motionsToday: Set<String>, usedInPlan: Map<String, Int>,
    ): Exercise? {
        val chain = listOf(slot) + slot.fallback.mapNotNull { S[it] }
        for (sl in chain) {
            for (strictLevel in listOf(true, false)) {
                val cands = pool.filter {
                    it.motion in sl.motions && it.id !in usedToday && it.motion !in motionsToday &&
                        it.category in setOf("strength", "plyometrics", "pilates") && (!strictLevel || levelOk(it, s.experience))
                }
                if (cands.isEmpty()) continue
                val scored = cands.map { it to score(it, s, usedInPlan, sl.role == Role.MAIN) + rnd.nextDouble() * 1.5 }
                return scored.maxBy { it.second }.first
            }
        }
        return null
    }

    private fun finisher(
        i: Interest, pool: List<Exercise>, s: UserSettings, rnd: Random, usedToday: Set<String>, usedInPlan: Map<String, Int>,
    ): Exercise? {
        val cands = pool.filter { i.tag in it.tags && it.id !in usedToday && it.category in setOf("cardio", "pilates", "kickboxing") && levelOk(it, s.experience) }
            .ifEmpty { pool.filter { i.tag in it.tags && it.id !in usedToday } }
        if (cands.isEmpty()) return null
        return cands.maxBy { score(it, s, usedInPlan) + rnd.nextDouble() * 3 }
    }

    /** Sets, reps, effort and rest from the evidence table (docs/RESEARCH.md §5). */
    fun prescribe(ex: Exercise, role: Role, s: UserSettings): PlanSlot {
        val tr = Classify.tracking(ex)
        val beg = s.experience == Experience.BEGINNER
        val adv = s.experience == Experience.ADVANCED
        fun slot(sets: Int, lo: Int, hi: Int, rir: Int, rest: Int, sec: Int = 0) =
            PlanSlot(ex.id, sets, lo, hi, rir, rest, sec, role, tr)

        if (tr == Tracking.CARDIO) return slot(1, 0, 0, 0, 0, sec = if (s.goal == Goal.LOSE_FAT) 15 * 60 else 10 * 60)
        if (role == Role.FINISHER) return when {
            "kickboxing" in ex.tags -> slot(3, 0, 0, 0, 45, sec = if (beg) 60 else 90)
            "pilates" in ex.tags && tr == Tracking.REPS -> slot(2, 8, 10, 2, 30)
            tr == Tracking.TIME || tr == Tracking.REPS && "cardio" in ex.tags -> PlanSlot(ex.id, 4, 0, 0, 0, 20, if (beg) 30 else 40, role, Tracking.TIME)
            else -> slot(2, 8, 12, 2, 30, sec = 45)
        }
        if (tr == Tracking.TIME || tr == Tracking.WEIGHT_TIME) {
            val sec = if (beg) 30 else if (adv) 60 else 45
            return slot(if (beg) 2 else 3, 0, 0, 2, 45, sec)
        }
        if (role == Role.CORE) return slot(if (beg) 2 else 3, 10, 15, 2, 45)

        val bw = tr == Tracking.REPS
        val (sets, lo, hi, rir, rest) = when (s.goal) {
            Goal.BUILD_MUSCLE -> when (role) {
                Role.MAIN -> Quint(if (adv) 4 else 3, if (beg) 8 else 6, if (beg) 12 else 10, 2, if (beg) 90 else 120)
                Role.ACCESSORY -> Quint(3, 8, 12, 2, 90)
                else -> Quint(if (beg) 2 else 3, 10, 15, if (beg) 2 else 1, 60)
            }
            Goal.GET_STRONGER -> when (role) {
                Role.MAIN -> Quint(if (beg) 3 else if (adv) 5 else 4, if (beg) 6 else 4, if (beg) 8 else 6, 2, 180)
                Role.ACCESSORY -> Quint(3, 6, 10, 2, 120)
                else -> Quint(if (beg) 2 else 3, 10, 12, 2, 75)
            }
            Goal.LOSE_FAT -> when (role) {
                Role.MAIN -> Quint(3, 8, 12, 2, 90)
                Role.ACCESSORY -> Quint(3, 10, 15, 2, 60)
                else -> Quint(if (beg) 2 else 3, 12, 15, 2, 45)
            }
            Goal.GENERAL_FITNESS -> when (role) {
                Role.MAIN -> Quint(if (beg) 2 else 3, 8, 12, 3, 90)
                Role.ACCESSORY -> Quint(if (beg) 2 else 3, 10, 12, 2, 75)
                else -> Quint(2, 12, 15, 2, 60)
            }
        }
        // Bodyweight moves sit higher in the rep range (no load to add).
        return if (bw) slot(sets, lo + 2, hi + 5, rir, rest) else slot(sets, lo, hi, rir, rest)
    }

    private data class Quint(val a: Int, val b: Int, val c: Int, val d: Int, val e: Int)

    /** Rough minutes: warm-up 5 + each set's work and rest. */
    fun estimateMinutes(slots: List<PlanSlot>): Int {
        var sec = 5 * 60
        for (p in slots) {
            val work = if (p.seconds > 0) p.seconds else 45
            sec += p.sets * (work + p.restSec) + 60 // +1 min to set up each exercise
        }
        return (sec + 59) / 60
    }

    /** Trim to the session length: first drop sets from isolation/core work, then drop the last accessories. */
    fun fitToTime(slots: List<PlanSlot>, minutes: Int): List<PlanSlot> {
        var out = slots.toMutableList()
        val limit = minutes + 5
        if (estimateMinutes(out) <= limit) return out
        out = out.map { if (it.role in setOf(Role.ISOLATION, Role.CORE, Role.ACCESSORY) && it.sets > 2) it.copy(sets = 2) else it }.toMutableList()
        while (estimateMinutes(out) > limit && out.size > 2) {
            val idx = out.indexOfLast { it.role == Role.ISOLATION || it.role == Role.CORE }
                .takeIf { it >= 0 } ?: out.indexOfLast { it.role == Role.ACCESSORY }.takeIf { it >= 0 } ?: out.indexOfLast { it.role != Role.MAIN }
            if (idx < 0) break
            out.removeAt(idx)
        }
        while (estimateMinutes(out) > limit && out.any { it.sets > 1 }) {
            val i = out.indexOfFirst { it.sets == out.maxOf { o -> o.sets } }
            out[i] = out[i].copy(sets = out[i].sets - 1)
        }
        return out
    }

    /** Swap options: same motion first, then the same movement slot, then the same main muscle. */
    fun substitutes(ex: Exercise, library: List<Exercise>, s: UserSettings, limit: Int = 12): List<Exercise> {
        val slots = Slot.entries.filter { ex.motion in it.motions }
        val pool = library.filter { it.id != ex.id && allowed(it, s) }
        val sameMotion = pool.filter { it.motion == ex.motion }
        val sameSlot = pool.filter { p -> p.motion != ex.motion && slots.any { p.motion in it.motions } }
        val sameMuscle = pool.filter { it.primary.firstOrNull() == ex.primary.firstOrNull() && it !in sameMotion && it !in sameSlot && it.category == ex.category }
        fun rank(l: List<Exercise>) = l.sortedByDescending { (if (it.id in STAPLES) 10 else 0) + (if (it.level == "beginner") 1 else 0) }
        return (rank(sameMotion) + rank(sameSlot) + rank(sameMuscle)).take(limit)
    }
}
