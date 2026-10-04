package com.nunna.fitnessgym.anim

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

/** One library exercise. Schema and rules: docs/CONTENT_SCHEMA.md. */
@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val tags: List<String>,
    val equipment: String,
    val category: String,
    val level: String,
    val mechanic: String? = null,
    val force: String? = null,
    val primary: List<String>,
    val secondary: List<String> = emptyList(),
    val motion: String,
    val implement: String = "none",
    val unilateral: Boolean = false,
    val instructions: List<String> = emptyList(),
    val cues: List<String> = emptyList(),
    val mistakes: List<String> = emptyList(),
    val source: String = "fitnessgym",
) {
    fun activation() = Activation(primary.toSet(), secondary.toSet() - primary.toSet(), implement, unilateral)
}

object Library {
    /** The six library tabs, in display order. */
    val TAGS = listOf("bodyweight", "dumbbell", "machine", "pilates", "kickboxing", "cardio")
    val TAG_LABELS = mapOf(
        "bodyweight" to "Bodyweight", "dumbbell" to "Dumbbells", "machine" to "Machines",
        "pilates" to "Pilates", "kickboxing" to "Kickboxing", "cardio" to "Cardio", "mat" to "Mat",
    )
    val EQUIPMENT = setOf("bodyweight", "dumbbell", "machine", "cable", "cardio_machine", "mat")
    val CATEGORIES = setOf("strength", "plyometrics", "cardio", "pilates", "kickboxing", "stretching")
    val LEVELS = setOf("beginner", "intermediate", "expert")
    val IMPLEMENTS = setOf("none", "dumbbell", "gloves", "cable", "handle", "rope")

    fun parseExercises(json: String): List<Exercise> =
        ContentJson.decodeFromString(ListSerializer(Exercise.serializer()), json)

    /** Returns human-readable problems; empty means the content is valid. */
    fun validate(exercises: List<Exercise>, motionIds: Set<String>): List<String> {
        val errs = ArrayList<String>()
        val seen = HashSet<String>()
        for (e in exercises) {
            val w = "exercise ${e.id}"
            if (!seen.add(e.id)) errs += "$w: duplicate id"
            if (!Regex("^[a-z0-9]+(-[a-z0-9]+)*$").matches(e.id)) errs += "$w: id must be kebab-case"
            if (e.tags.none { it in TAGS }) errs += "$w: needs one of $TAGS"
            for (t in e.tags) if (t !in TAGS && t != "mat") errs += "$w: unknown tag $t"
            if (e.equipment !in EQUIPMENT) errs += "$w: unknown equipment ${e.equipment}"
            if (e.category !in CATEGORIES) errs += "$w: unknown category ${e.category}"
            if (e.level !in LEVELS) errs += "$w: unknown level ${e.level}"
            if (e.implement !in IMPLEMENTS) errs += "$w: unknown implement ${e.implement}"
            if (e.primary.isEmpty()) errs += "$w: primary is empty"
            for (m in e.primary + e.secondary) if (m !in Anatomy.GROUPS) errs += "$w: unknown muscle $m"
            if (e.motion !in motionIds) errs += "$w: unknown motion ${e.motion}"
            if (e.cues.size !in 2..4) errs += "$w: needs 2-4 cues (has ${e.cues.size})"
        }
        return errs
    }
}
