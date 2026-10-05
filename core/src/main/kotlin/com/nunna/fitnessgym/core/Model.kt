package com.nunna.fitnessgym.core

import com.nunna.fitnessgym.anim.Exercise
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class Goal(val label: String, val blurb: String) {
    BUILD_MUSCLE("Build muscle", "Grow and shape muscle (hypertrophy)"),
    GET_STRONGER("Get stronger", "Lift heavier on the big moves"),
    LOSE_FAT("Lose fat", "Strength to keep muscle, plus cardio finishers"),
    GENERAL_FITNESS("General fitness", "Feel fit, move well, all-round health"),
}

enum class Experience(val label: String, val blurb: String) {
    BEGINNER("Beginner", "New to lifting, or less than ~6 months"),
    INTERMEDIATE("Intermediate", "6 months to 2 years of regular training"),
    ADVANCED("Advanced", "2+ years, comfortable with all the main lifts"),
}

/** What the owner can use. BODYWEIGHT is always available. GYM = a GoodLife / LA Fitness style gym. */
enum class Equip(val label: String) {
    BODYWEIGHT("Bodyweight"), MAT("Yoga mat"), DUMBBELLS("Dumbbells"), BENCH("Bench"),
    PULLUP_BAR("Pull-up bar"), GYM("Gym machines & cables"), CARDIO_MACHINES("Cardio machines"),
}

enum class Interest(val label: String, val tag: String) {
    CARDIO("Cardio", "cardio"), PILATES("Pilates", "pilates"), KICKBOXING("Kickboxing", "kickboxing"),
}

/** Areas to protect. Each one removes the motions that load it (see [Generator.AVOID]). */
enum class AvoidArea(val label: String) {
    SHOULDERS("Shoulders / overhead"), KNEES("Knees"), LOWER_BACK("Lower back"), WRISTS("Wrists"), NECK("Neck"),
}

enum class Units(val label: String, val defaultStep: Double, val machineStep: Double) {
    KG("kg", 2.5, 5.0), LB("lb", 5.0, 10.0);
    fun toKg(v: Double) = if (this == KG) v else v / 2.20462
}

@Serializable
data class EquipmentProfile(
    val name: String,
    val items: Set<Equip>,
    /** Dumbbell weights owned (each = one pair), in the user's units. Empty = any (gym rack). */
    val dumbbells: List<Double> = emptyList(),
    val machineStep: Double = 5.0,
) {
    /** A gym has everything (mats, dumbbells, benches, bars, cardio machines). */
    fun has(e: Equip) = e == Equip.BODYWEIGHT || e in items || Equip.GYM in items

    companion object {
        fun home(dumbbells: List<Double> = emptyList()) = EquipmentProfile(
            "Home", buildSet { add(Equip.MAT); if (dumbbells.isNotEmpty()) add(Equip.DUMBBELLS) }, dumbbells,
        )
        fun gym(units: Units = Units.KG) = EquipmentProfile(
            "Gym", setOf(Equip.GYM, Equip.CARDIO_MACHINES, Equip.DUMBBELLS, Equip.BENCH, Equip.PULLUP_BAR, Equip.MAT),
            emptyList(), units.machineStep,
        )
    }
}

/** Everything the onboarding asks. Stored as JSON in the database. */
@Serializable
data class UserSettings(
    val name: String = "",
    val goal: Goal = Goal.BUILD_MUSCLE,
    val experience: Experience = Experience.BEGINNER,
    val daysPerWeek: Int = 3,
    val sessionMinutes: Int = 45,
    val profiles: List<EquipmentProfile> = listOf(EquipmentProfile.gym()),
    val activeProfile: Int = 0,
    val interests: Set<Interest> = emptySet(),
    val avoid: Set<AvoidArea> = emptySet(),
    val excluded: Set<String> = emptySet(),
    val units: Units = Units.KG,
    val parqFlags: Int = 0,
    /** Show an optional "reps in reserve" box on each set. */
    val logEffort: Boolean = false,
) {
    val profile: EquipmentProfile get() = profiles.getOrElse(activeProfile) { profiles.first() }

    fun validated(): UserSettings = copy(
        daysPerWeek = daysPerWeek.coerceIn(1, 7),
        sessionMinutes = sessionMinutes.coerceIn(15, 120),
        profiles = profiles.ifEmpty { listOf(EquipmentProfile.home()) },
        activeProfile = activeProfile.coerceIn(0, (profiles.size - 1).coerceAtLeast(0)),
    )

    fun toJson() = SettingsJson.encodeToString(serializer(), this)

    companion object {
        val SettingsJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        fun fromJson(s: String): UserSettings = SettingsJson.decodeFromString(serializer(), s).validated()
    }
}

/** How a set is recorded. */
enum class Tracking { WEIGHT_REPS, REPS, TIME, WEIGHT_TIME, CARDIO }

object Classify {
    private val TIMED = setOf("plank", "side_plank", "wall_sit", "hundred", "boxing_stance", "bob_and_weave", "leg_pull_front")
    private val NEEDS_BENCH = setOf(
        "bench_press", "incline_press", "chest_fly", "pullover", "lying_triceps_extension", "one_arm_row",
        "concentration_curl", "bulgarian_split_squat", "seated_overhead_press", "bench_dip", "step_up",
    )
    private val NEEDS_BAR = setOf("pullup", "hanging_knee_raise", "inverted_row")
    private val NEEDS_GYM = setOf("parallel_dip")
    private val BENCH_WORDS = Regex("(?i)\\b(incline|decline|bench|preacher)\\b")

    fun tracking(e: Exercise): Tracking = when {
        e.equipment == "cardio_machine" -> Tracking.CARDIO
        e.motion == "farmer_walk" -> Tracking.WEIGHT_TIME
        e.category == "cardio" || e.category == "kickboxing" || e.motion in TIMED -> Tracking.TIME
        e.equipment in setOf("dumbbell", "machine", "cable") -> Tracking.WEIGHT_REPS
        else -> Tracking.REPS
    }

    /** Equipment an exercise needs, beyond bodyweight. */
    fun needs(e: Exercise): Set<Equip> = buildSet {
        when (e.equipment) {
            "dumbbell" -> add(Equip.DUMBBELLS)
            "machine", "cable" -> add(Equip.GYM)
            "cardio_machine" -> add(Equip.CARDIO_MACHINES)
            "mat" -> add(Equip.MAT)
        }
        val freeWeight = e.equipment == "dumbbell" || e.equipment == "bodyweight"
        if (freeWeight && e.motion in NEEDS_BENCH) add(Equip.BENCH)
        // Named bench variants (incline row, decline press, preacher curl...) need a bench too.
        if (freeWeight && e.motion != "pushup" && BENCH_WORDS.containsMatchIn(e.name)) add(Equip.BENCH)
        if (e.equipment == "bodyweight" && e.motion in NEEDS_BAR) add(Equip.PULLUP_BAR)
        if (e.motion in NEEDS_GYM) add(Equip.GYM)
    }

    fun available(e: Exercise, p: EquipmentProfile) = needs(e).all { p.has(it) }
}
