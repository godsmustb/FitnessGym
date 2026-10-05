package com.nunna.fitnessgym.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlin.math.roundToInt

/** One completed set from history. */
data class SetPerf(val weight: Double? = null, val reps: Int? = null, val seconds: Int? = null, val rir: Int? = null)

/** One past session's working sets for an exercise, with the target it was given. */
data class Performance(
    val sets: List<SetPerf>,
    val targetSets: Int,
    val repMin: Int,
    val repMax: Int,
    val targetSeconds: Int = 0,
    val targetRir: Int = 2,
    val unit: Units = Units.KG,
)

enum class Change { FIRST, UP, SAME, DOWN }

/** What to do next time, with a plain-English reason (shown under the exercise). */
data class Suggestion(val weight: Double?, val reps: Int?, val seconds: Int?, val change: Change, val note: String)

/** Available loads for an exercise, so suggestions land on weights that exist. */
class Ladder(private val values: List<Double>) {
    fun up(w: Double) = values.firstOrNull { it > w + 1e-6 }
    fun down(w: Double) = values.lastOrNull { it < w - 1e-6 }
    fun nearest(w: Double) = values.minByOrNull { abs(it - w) } ?: w
    val max get() = values.lastOrNull()

    companion object {
        fun forExercise(equipment: String, profile: EquipmentProfile, units: Units): Ladder = when (equipment) {
            "dumbbell" -> Ladder(
                profile.dumbbells.filter { it > 0 }.distinct().sorted().ifEmpty {
                    if (units == Units.KG) (1..20).map { it * 2.5 } else (1..24).map { it * 5.0 }
                },
            )
            "machine", "cable" -> {
                val step = profile.machineStep.takeIf { it > 0 } ?: units.machineStep
                Ladder((1..60).map { it * step })
            }
            else -> Ladder(emptyList())
        }
    }
}

object Progression {
    fun fmt(w: Double): String = if (abs(w - w.roundToInt()) < 0.05) w.roundToInt().toString() else "%.1f".format(w)

    /**
     * Double progression (Plotkin 2022): earn the top of the rep range on every set at the target effort,
     * then add the smallest load step and restart at the bottom of the range. Two sessions in a row below
     * the range at the same load trigger a small drop (reactive deload). [history] is newest first.
     */
    fun suggest(tracking: Tracking, history: List<Performance>, target: Performance, ladder: Ladder, units: Units): Suggestion {
        val past = history.filter { it.sets.isNotEmpty() }
        val last = past.firstOrNull()
        val u = units.label
        if (last == null) return when (tracking) {
            Tracking.WEIGHT_REPS, Tracking.WEIGHT_TIME -> Suggestion(null, target.repMax.takeIf { it > 0 }, target.targetSeconds.takeIf { it > 0 }, Change.FIRST,
                "First time: pick a weight you could lift about 2 more reps than the target. The app learns from there.")
            Tracking.REPS -> Suggestion(null, target.repMin, null, Change.FIRST, "First time: aim for ${target.repMin}–${target.repMax} clean reps.")
            Tracking.TIME, Tracking.CARDIO -> Suggestion(null, null, target.targetSeconds, Change.FIRST, "First time: aim for ${secText(target.targetSeconds)}.")
        }

        return when (tracking) {
            Tracking.WEIGHT_REPS -> weightReps(last, past, target, ladder, units)
            Tracking.WEIGHT_TIME -> {
                val w = convert(last.sets.mapNotNull { it.weight }.maxOrNull(), last.unit, units)
                val allHeld = last.sets.size >= last.targetSets && last.sets.all { (it.seconds ?: 0) >= last.targetSeconds }
                if (w != null && allHeld) {
                    val up = ladder.up(w)
                    if (up != null) Suggestion(up, null, target.targetSeconds, Change.UP, "Up to ${fmt(up)} $u: you held every set for ${secText(last.targetSeconds)} at ${fmt(w)} $u.")
                    else Suggestion(w, null, target.targetSeconds + 10, Change.UP, "You're at your heaviest pair. Go 10 s longer.")
                } else Suggestion(w, null, target.targetSeconds, Change.SAME, "Same load. Hold every set for the full ${secText(target.targetSeconds)}.")
            }
            Tracking.REPS -> {
                val reps = last.sets.mapNotNull { it.reps }
                val hitTop = last.sets.size >= last.targetSets && reps.isNotEmpty() && reps.all { it >= last.repMax }
                if (hitTop) Suggestion(null, last.repMax + 2, null, Change.UP,
                    "You hit ${last.repMax}+ reps on every set. Aim for ${last.repMax + 2}, or try a harder variation.")
                else {
                    val next = ((reps.minOrNull() ?: target.repMin) + 1).coerceIn(target.repMin, maxOf(target.repMax, target.repMin))
                    Suggestion(null, next, null, Change.SAME, "Build reps: aim for $next on every set (last: ${reps.joinToString(", ")}).")
                }
            }
            Tracking.TIME, Tracking.CARDIO -> {
                val secs = last.sets.mapNotNull { it.seconds }
                val tgt = maxOf(target.targetSeconds, last.targetSeconds)
                val held = secs.isNotEmpty() && last.sets.size >= last.targetSets && secs.all { it >= tgt }
                val step = if (tracking == Tracking.CARDIO) 60 else if (tgt >= 60) 10 else 5
                val cap = if (tracking == Tracking.CARDIO) 45 * 60 else 180
                if (held && tgt < cap) Suggestion(null, null, tgt + step, Change.UP, "You completed ${secText(tgt)}. Next: ${secText(tgt + step)}.")
                else Suggestion(null, null, tgt, Change.SAME, "Same target: ${secText(tgt)}.")
            }
        }
    }

    private fun weightReps(last: Performance, past: List<Performance>, target: Performance, ladder: Ladder, units: Units): Suggestion {
        val u = units.label
        val w = convert(last.sets.mapNotNull { it.weight }.maxOrNull(), last.unit, units)
            ?: return Suggestion(null, target.repMax, null, Change.FIRST, "No weight was logged last time. Enter the weight you use.")
        val reps = last.sets.mapNotNull { it.reps }
        val rirs = last.sets.mapNotNull { it.rir }
        val hitTop = last.sets.size >= last.targetSets && reps.isNotEmpty() && reps.all { it >= last.repMax }
        val tooHard = rirs.isNotEmpty() && rirs.all { it == 0 } && last.targetRir >= 2
        val below = reps.any { it < last.repMin }
        val prev = past.getOrNull(1)
        val prevW = convert(prev?.sets?.mapNotNull { it.weight }?.maxOrNull(), prev?.unit ?: units, units)
        val prevBelow = prev != null && prevW != null && abs(prevW - w) < 0.01 && prev.sets.mapNotNull { it.reps }.any { it < prev.repMin }

        return when {
            hitTop && !tooHard -> {
                val up = ladder.up(w)
                if (up != null) Suggestion(up, target.repMin, null, Change.UP,
                    "+${fmt(up - w)} $u: you hit ${last.sets.size}×${last.repMax} at ${fmt(w)} $u. Restart at ${target.repMin} reps.")
                else Suggestion(w, target.repMax, null, Change.SAME,
                    "You've topped out at ${fmt(w)} $u. Add a set, or slow the lowering to 3 seconds.")
            }
            below && prevBelow -> {
                val down = ladder.down(w) ?: w
                Suggestion(down, target.repMin, null, Change.DOWN,
                    "Two tough sessions in a row at ${fmt(w)} $u. Drop to ${fmt(down)} $u and rebuild. This is normal.")
            }
            else -> {
                val goal = ((reps.minOrNull() ?: target.repMin) + 1).coerceIn(target.repMin, target.repMax)
                Suggestion(w, goal, null, Change.SAME,
                    "Same ${fmt(w)} $u. Aim for $goal reps on every set (last: ${reps.joinToString(", ")}). At ${target.repMax} on all sets, it goes up.")
            }
        }
    }

    private fun convert(w: Double?, from: Units, to: Units): Double? = when {
        w == null -> null
        from == to -> w
        to == Units.KG -> w / 2.20462
        else -> w * 2.20462
    }

    fun secText(s: Int): String = when {
        s <= 0 -> "0 s"
        s % 60 == 0 && s >= 60 -> "${s / 60} min"
        s > 60 -> "${s / 60} min ${s % 60} s"
        else -> "$s s"
    }
}

object Records {
    /** Epley estimated one-rep max. */
    fun e1rm(weight: Double, reps: Int): Double = if (reps <= 0) 0.0 else if (reps == 1) weight else weight * (1 + reps / 30.0)

    /** Which personal records a new set beats, compared with all earlier sets of the same exercise. */
    fun newRecords(set: SetPerf, earlier: List<SetPerf>, tracking: Tracking): List<String> {
        val out = ArrayList<String>()
        when (tracking) {
            Tracking.WEIGHT_REPS -> {
                val w = set.weight ?: return out
                val r = set.reps ?: return out
                if (r <= 0 || w <= 0) return out
                val prevW = earlier.mapNotNull { it.weight }.maxOrNull()
                val prevE = earlier.filter { it.weight != null && it.reps != null }.maxOfOrNull { e1rm(it.weight!!, it.reps!!) }
                if (prevW == null) return out // first time is a baseline, not a PR
                if (w > prevW + 1e-6) out += "Heaviest weight"
                if (prevE != null && e1rm(w, r) > prevE + 1e-6) out += "Best estimated 1RM"
            }
            Tracking.REPS -> {
                val r = set.reps ?: return out
                val prev = earlier.mapNotNull { it.reps }.maxOrNull() ?: return out
                if (r > prev) out += "Most reps"
            }
            Tracking.TIME, Tracking.CARDIO, Tracking.WEIGHT_TIME -> {
                val s = set.seconds ?: return out
                val prev = earlier.mapNotNull { it.seconds }.maxOrNull() ?: return out
                if (s > prev) out += "Longest"
            }
        }
        return out
    }
}

object Gamification {
    const val XP_SET = 10
    const val XP_WORKOUT = 50
    const val XP_PR = 25

    /** Total XP needed to reach [level] (level 1 = 0 XP). Same curve as GuitarNoodle. */
    fun xpForLevel(level: Int): Int = if (level <= 1) 0 else Math.round(100 * Math.pow((level - 1).toDouble(), 1.6)).toInt()

    fun level(xp: Int): Int {
        var l = 1
        while (xpForLevel(l + 1) <= xp) l++
        return l
    }

    /** Fraction of the way to the next level, 0..1. */
    fun progress(xp: Int): Double {
        val l = level(xp)
        val a = xpForLevel(l); val b = xpForLevel(l + 1)
        return ((xp - a).toDouble() / (b - a)).coerceIn(0.0, 1.0)
    }

    /**
     * Weekly streak: consecutive weeks (Mon–Sun) with at least one workout. Rest days never break it,
     * and the current week counts as "still open", so the streak survives until a whole week is missed.
     */
    fun weeklyStreak(workoutDays: Collection<LocalDate>, today: LocalDate): Int {
        if (workoutDays.isEmpty()) return 0
        val weeks = workoutDays.map { it.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }.toSet()
        var wk = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        if (wk !in weeks) wk = wk.minusWeeks(1)
        var n = 0
        while (wk in weeks) { n++; wk = wk.minusWeeks(1) }
        return n
    }
}
