package com.nunna.fitnessgym.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProgressionTest {
    private val kg = Units.KG
    private val dbLadder = Ladder.forExercise("dumbbell", EquipmentProfile.gym(), kg)
    private val target = Performance(emptyList(), 3, 8, 12, targetRir = 2)
    private fun perf(w: Double?, vararg reps: Int, rir: Int? = null, unit: Units = kg) =
        Performance(reps.map { SetPerf(w, it, null, rir) }, 3, 8, 12, unit = unit)

    @Test fun firstTimeHasNoWeight() {
        val s = Progression.suggest(Tracking.WEIGHT_REPS, emptyList(), target, dbLadder, kg)
        assertEquals(Change.FIRST, s.change); assertNull(s.weight)
    }

    @Test fun topOfRangeOnAllSetsAddsSmallestStep() {
        val s = Progression.suggest(Tracking.WEIGHT_REPS, listOf(perf(20.0, 12, 12, 12)), target, dbLadder, kg)
        assertEquals(Change.UP, s.change); assertEquals(22.5, s.weight!!, 1e-9); assertEquals(8, s.reps)
        assertTrue(s.note, s.note.contains("3×12") && s.note.contains("20"))
    }

    @Test fun missingASetMeansNoIncrease() {
        val s = Progression.suggest(Tracking.WEIGHT_REPS, listOf(perf(20.0, 12, 12)), target, dbLadder, kg)
        assertEquals(Change.SAME, s.change); assertEquals(20.0, s.weight!!, 1e-9)
    }

    @Test fun inRangeAimsOneMoreRep() {
        val s = Progression.suggest(Tracking.WEIGHT_REPS, listOf(perf(20.0, 10, 9, 8)), target, dbLadder, kg)
        assertEquals(Change.SAME, s.change); assertEquals(9, s.reps)
    }

    @Test fun allSetsToFailureHoldsEvenAtTop() {
        val s = Progression.suggest(Tracking.WEIGHT_REPS, listOf(perf(20.0, 12, 12, 12, rir = 0)), target, dbLadder, kg)
        assertEquals(Change.SAME, s.change)
    }

    @Test fun twoBadSessionsInARowDeload() {
        val s = Progression.suggest(Tracking.WEIGHT_REPS, listOf(perf(20.0, 7, 6, 6), perf(20.0, 7, 7, 6)), target, dbLadder, kg)
        assertEquals(Change.DOWN, s.change); assertEquals(17.5, s.weight!!, 1e-9)
    }

    @Test fun oneBadSessionHolds() {
        val s = Progression.suggest(Tracking.WEIGHT_REPS, listOf(perf(20.0, 7, 6, 6), perf(17.5, 12, 12, 12)), target, dbLadder, kg)
        assertEquals(Change.SAME, s.change); assertEquals(20.0, s.weight!!, 1e-9)
    }

    @Test fun homeDumbbellsJumpToNextPairOwned() {
        val home = Ladder.forExercise("dumbbell", EquipmentProfile.home(listOf(5.0, 10.0, 15.0)), kg)
        val s = Progression.suggest(Tracking.WEIGHT_REPS, listOf(perf(10.0, 12, 12, 12)), target, home, kg)
        assertEquals(15.0, s.weight!!, 1e-9)
        val top = Progression.suggest(Tracking.WEIGHT_REPS, listOf(perf(15.0, 12, 12, 12)), target, home, kg)
        assertEquals(Change.SAME, top.change); assertTrue(top.note.contains("topped out"))
    }

    @Test fun machineUsesStackStep() {
        val m = Ladder.forExercise("machine", EquipmentProfile.gym().copy(machineStep = 5.0), kg)
        val s = Progression.suggest(Tracking.WEIGHT_REPS, listOf(perf(45.0, 12, 12, 12)), target, m, kg)
        assertEquals(50.0, s.weight!!, 1e-9)
    }

    @Test fun poundsLadderAndConversion() {
        val lb = Ladder.forExercise("dumbbell", EquipmentProfile.gym(Units.LB), Units.LB)
        val s = Progression.suggest(Tracking.WEIGHT_REPS, listOf(perf(40.0, 12, 12, 12, unit = Units.LB)), target, lb, Units.LB)
        assertEquals(45.0, s.weight!!, 1e-9); assertTrue(s.note.contains("lb"))
        // History logged in kg, now displayed in lb: 20 kg is about 44.1 lb, so the next step up is 45.
        val conv = Progression.suggest(Tracking.WEIGHT_REPS, listOf(perf(20.0, 12, 12, 12)), target, lb, Units.LB)
        assertEquals(45.0, conv.weight!!, 1e-9)
    }

    @Test fun bodyweightRepsProgress() {
        val t = Performance(emptyList(), 3, 10, 17)
        val up = Progression.suggest(Tracking.REPS, listOf(Performance(List(3) { SetPerf(reps = 17) }, 3, 10, 17)), t, Ladder(emptyList()), kg)
        assertEquals(Change.UP, up.change); assertEquals(19, up.reps)
        val same = Progression.suggest(Tracking.REPS, listOf(Performance(listOf(SetPerf(reps = 12), SetPerf(reps = 10)), 3, 10, 17)), t, Ladder(emptyList()), kg)
        assertEquals(11, same.reps)
    }

    @Test fun timedHoldsGrow() {
        val t = Performance(emptyList(), 3, 0, 0, targetSeconds = 30)
        val up = Progression.suggest(Tracking.TIME, listOf(Performance(List(3) { SetPerf(seconds = 30) }, 3, 0, 0, 30)), t, Ladder(emptyList()), kg)
        assertEquals(35, up.seconds)
        val hold = Progression.suggest(Tracking.TIME, listOf(Performance(listOf(SetPerf(seconds = 20)), 3, 0, 0, 30)), t, Ladder(emptyList()), kg)
        assertEquals(30, hold.seconds); assertEquals(Change.SAME, hold.change)
        val cardio = Progression.suggest(Tracking.CARDIO, listOf(Performance(listOf(SetPerf(seconds = 600)), 1, 0, 0, 600)), t.copy(targetSeconds = 600), Ladder(emptyList()), kg)
        assertEquals(660, cardio.seconds)
    }

    @Test fun recordsDetectHeavierAndBetterE1rmButNotFirstTime() {
        val earlier = listOf(SetPerf(20.0, 10), SetPerf(22.5, 6))
        assertEquals(listOf("Heaviest weight", "Best estimated 1RM"), Records.newRecords(SetPerf(25.0, 6), earlier, Tracking.WEIGHT_REPS))
        assertEquals(listOf("Best estimated 1RM"), Records.newRecords(SetPerf(20.0, 14), earlier, Tracking.WEIGHT_REPS))
        assertTrue(Records.newRecords(SetPerf(20.0, 5), earlier, Tracking.WEIGHT_REPS).isEmpty())
        assertTrue(Records.newRecords(SetPerf(30.0, 5), emptyList(), Tracking.WEIGHT_REPS).isEmpty())
        assertEquals(listOf("Most reps"), Records.newRecords(SetPerf(reps = 21), listOf(SetPerf(reps = 20)), Tracking.REPS))
        assertEquals(listOf("Longest"), Records.newRecords(SetPerf(seconds = 61), listOf(SetPerf(seconds = 60)), Tracking.TIME))
        assertEquals(26.67, Records.e1rm(20.0, 10), 0.01)
    }

    @Test fun levelsAndStreaks() {
        assertEquals(1, Gamification.level(0)); assertEquals(2, Gamification.level(100)); assertEquals(1, Gamification.level(99))
        assertEquals(3, Gamification.level(Gamification.xpForLevel(3)))
        val mon = LocalDate.of(2026, 10, 5) // a Monday
        assertEquals(0, Gamification.weeklyStreak(emptyList(), mon))
        // Trained last week only: the streak is still alive on Monday.
        assertEquals(1, Gamification.weeklyStreak(listOf(mon.minusDays(3)), mon))
        assertEquals(3, Gamification.weeklyStreak(listOf(mon, mon.minusWeeks(1), mon.minusWeeks(2).plusDays(4)), mon.plusDays(2)))
        // A fully missed week breaks it.
        assertEquals(1, Gamification.weeklyStreak(listOf(mon, mon.minusWeeks(2)), mon))
    }
}
