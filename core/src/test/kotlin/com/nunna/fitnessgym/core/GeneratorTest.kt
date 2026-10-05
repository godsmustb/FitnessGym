package com.nunna.fitnessgym.core

import com.nunna.fitnessgym.anim.Exercise
import com.nunna.fitnessgym.anim.Library
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GeneratorTest {
    private val lib: List<Exercise> = Library.parseExercises(File(System.getProperty("contentDir"), "exercises.json").readText())
    private val byId = lib.associateBy { it.id }
    private val home = EquipmentProfile.home(listOf(5.0, 10.0, 15.0))
    private val bareHome = EquipmentProfile("Home", setOf(Equip.MAT))

    private fun settings(
        goal: Goal = Goal.BUILD_MUSCLE, x: Experience = Experience.BEGINNER, days: Int = 3, mins: Int = 60,
        p: EquipmentProfile = EquipmentProfile.gym(), interests: Set<Interest> = emptySet(), avoid: Set<AvoidArea> = emptySet(),
        excluded: Set<String> = emptySet(),
    ) = UserSettings(
        goal = goal, experience = x, daysPerWeek = days, sessionMinutes = mins, profiles = listOf(p),
        interests = interests, avoid = avoid, excluded = excluded,
    )

    @Test fun everyCombinationProducesAValidPlan() {
        for (g in Goal.entries) for (x in Experience.entries) for (d in 1..7) for (p in listOf(EquipmentProfile.gym(), home, bareHome)) {
            val s = settings(g, x, d, 45, p, setOf(Interest.PILATES, Interest.KICKBOXING))
            val plan = Generator.generate(s, lib, seed = 7)
            val tag = "$g/$x/$d/${p.name}${p.items}"
            assertEquals(tag, d, plan.days.size)
            for (day in plan.days) {
                assertTrue("$tag ${day.name} empty", day.slots.size >= 2)
                assertEquals("$tag ${day.name} duplicate exercise", day.slots.size, day.slots.map { it.exerciseId }.toSet().size)
                for (sl in day.slots) {
                    val ex = byId.getValue(sl.exerciseId)
                    assertTrue("$tag ${ex.id} not available", Classify.available(ex, p))
                    assertTrue("$tag ${ex.id} sets", sl.sets in 1..6)
                    if (sl.tracking == Tracking.WEIGHT_REPS || sl.tracking == Tracking.REPS) assertTrue("$tag ${ex.id} reps", sl.repMin in 1..sl.repMax)
                    if (sl.tracking == Tracking.TIME || sl.tracking == Tracking.CARDIO) assertTrue("$tag ${ex.id} seconds", sl.seconds > 0)
                }
                val est = Generator.estimateMinutes(day.slots)
                assertTrue("$tag ${day.name} too long: $est", est <= 45 + 5 || day.slots.size <= 2)
            }
        }
    }

    @Test fun beginnersOnlyGetBeginnerMovesWhenPossible() {
        val plan = Generator.generate(settings(x = Experience.BEGINNER, days = 4), lib)
        val nonBeg = plan.days.flatMap { it.slots }.map { byId.getValue(it.exerciseId) }.filter { it.level != "beginner" }
        assertTrue(nonBeg.map { it.id }.toString(), nonBeg.size <= 1)
    }

    @Test fun avoidAreasRemoveTheirMotions() {
        for (a in AvoidArea.entries) {
            val plan = Generator.generate(settings(days = 6, avoid = setOf(a), interests = Interest.entries.toSet()), lib)
            for (sl in plan.days.flatMap { it.slots }) {
                assertFalse("$a got ${sl.exerciseId}", byId.getValue(sl.exerciseId).motion in Generator.AVOID.getValue(a))
            }
        }
    }

    @Test fun excludedExercisesNeverAppear() {
        val first = Generator.generate(settings(days = 3), lib)
        val banned = first.days.flatMap { it.slots }.map { it.exerciseId }.toSet()
        val again = Generator.generate(settings(days = 3, excluded = banned), lib)
        assertTrue(again.days.flatMap { it.slots }.none { it.exerciseId in banned })
        assertTrue(again.days.all { it.slots.size >= 3 })
    }

    @Test fun bodyweightOnlyHomeStillTrainsEveryPattern() {
        val plan = Generator.generate(settings(days = 3, p = bareHome), lib)
        val exs = plan.days.flatMap { it.slots }.map { byId.getValue(it.exerciseId) }
        assertTrue(exs.all { it.equipment in setOf("bodyweight", "mat") })
        assertTrue("needs a push", exs.any { it.motion == "pushup" })
        assertTrue("needs a squat pattern", exs.any { it.motion in setOf("squat", "lunge", "bulgarian_split_squat", "step_up") })
    }

    @Test fun noBenchMeansNoBenchVariants() {
        for (seed in 1L..5L) {
            val plan = Generator.generate(settings(days = 6, p = home, x = Experience.ADVANCED), lib, seed)
            for (sl in plan.days.flatMap { it.slots }) {
                val ex = byId.getValue(sl.exerciseId)
                assertFalse(ex.name, Regex("(?i)\\b(incline|decline|bench|preacher)\\b").containsMatchIn(ex.name) && ex.motion != "pushup")
            }
        }
    }

    @Test fun gymPlanUsesMachinesAndFreeWeights() {
        val eq = Generator.generate(settings(days = 4, x = Experience.INTERMEDIATE), lib).days.flatMap { it.slots }
            .map { byId.getValue(it.exerciseId).equipment }.toSet()
        assertTrue(eq.toString(), "machine" in eq || "cable" in eq)
        assertTrue(eq.toString(), "dumbbell" in eq)
    }

    @Test fun loseFatAddsCardioFinisherEveryDay() {
        val plan = Generator.generate(settings(goal = Goal.LOSE_FAT, days = 3), lib)
        for (d in plan.days) assertEquals(d.name, Role.FINISHER, d.slots.last().role)
        assertTrue(plan.days.all { "cardio" in byId.getValue(it.slots.last().exerciseId).tags })
    }

    @Test fun interestsRotateAcrossDays() {
        val plan = Generator.generate(settings(days = 3, mins = 90, interests = setOf(Interest.PILATES, Interest.KICKBOXING, Interest.CARDIO)), lib)
        val tags = plan.days.map { d -> byId.getValue(d.slots.last { it.role == Role.FINISHER }.exerciseId).tags }
        assertTrue("cardio" in tags[0]); assertTrue("pilates" in tags[1]); assertTrue("kickboxing" in tags[2])
    }

    @Test fun strengthGoalUsesLowRepsAndLongRest() {
        val main = Generator.generate(settings(goal = Goal.GET_STRONGER, x = Experience.INTERMEDIATE, mins = 90), lib).days[0].slots[0]
        assertEquals(Role.MAIN, main.role)
        assertTrue(main.repMax <= 8); assertTrue(main.restSec >= 180)
    }

    @Test fun shortSessionsAreTrimmed() {
        val plan = Generator.generate(settings(days = 4, mins = 20), lib)
        for (d in plan.days) assertTrue("${d.name}: ${Generator.estimateMinutes(d.slots)}", Generator.estimateMinutes(d.slots) <= 25 || d.slots.size <= 2)
        assertTrue(plan.days.all { d -> d.slots.any { it.role == Role.MAIN } })
    }

    @Test fun weeklyVolumeIsInTheEvidenceRange() {
        // Fractional sets (primary 1, secondary 0.5) for big muscles on a 4-day intermediate plan.
        val plan = Generator.generate(settings(days = 4, x = Experience.INTERMEDIATE, mins = 75), lib)
        val vol = HashMap<String, Double>()
        for (sl in plan.days.flatMap { it.slots }) {
            val ex = byId.getValue(sl.exerciseId)
            ex.primary.forEach { vol[it] = (vol[it] ?: 0.0) + sl.sets }
            ex.secondary.forEach { vol[it] = (vol[it] ?: 0.0) + sl.sets * 0.5 }
        }
        for (m in listOf("chest", "quadriceps", "shoulders")) assertTrue("$m ${vol[m]}", (vol[m] ?: 0.0) in 4.0..24.0)
    }

    @Test fun sameSeedSamePlanNewSeedVaries() {
        val s = settings(days = 3)
        assertEquals(Generator.generate(s, lib, 3), Generator.generate(s, lib, 3))
        val variants = (1L..6L).map { Generator.generate(s, lib, it).days.flatMap { d -> d.slots.map { x -> x.exerciseId } } }.toSet()
        assertTrue("regenerating should offer variety", variants.size > 1)
    }

    @Test fun substitutesRespectEquipmentAndPreferSameMotion() {
        val ex = byId.getValue("dumbbell-bench-press")
        val withBench = home.copy(items = home.items + Equip.BENCH)
        val subs = Generator.substitutes(ex, lib, settings(p = withBench))
        assertTrue(subs.isNotEmpty())
        assertEquals("bench_press", subs.first().motion)
        assertTrue(subs.all { Classify.available(it, withBench) })
        assertTrue(subs.none { it.id == ex.id })
        val bare = Generator.substitutes(ex, lib, settings(p = bareHome))
        assertTrue(bare.isNotEmpty()); assertTrue(bare.all { it.equipment in setOf("bodyweight", "mat") })
    }

    @Test fun settingsValidationClamps() {
        val s = UserSettings(daysPerWeek = 12, sessionMinutes = 1, profiles = emptyList(), activeProfile = 5).validated()
        assertEquals(7, s.daysPerWeek); assertEquals(15, s.sessionMinutes); assertEquals(1, s.profiles.size); assertEquals(0, s.activeProfile)
        val round = UserSettings.fromJson(settings(interests = setOf(Interest.CARDIO)).toJson())
        assertEquals(setOf(Interest.CARDIO), round.interests)
        assertNotEquals("", round.toJson())
    }

    @Test fun everyExerciseHasATrackingKind() {
        val kinds = lib.groupBy { Classify.tracking(it) }
        assertTrue(kinds.keys.containsAll(listOf(Tracking.WEIGHT_REPS, Tracking.REPS, Tracking.TIME, Tracking.CARDIO)))
        assertEquals(Tracking.CARDIO, Classify.tracking(lib.first { it.motion == "treadmill_run" }))
        assertEquals(Tracking.TIME, Classify.tracking(byId.getValue("plank")))
        assertEquals(Tracking.WEIGHT_REPS, Classify.tracking(byId.getValue("dumbbell-bench-press")))
        assertEquals(Tracking.REPS, Classify.tracking(byId.getValue("pushups")))
    }
}
