package com.nunna.fitnessgym

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nunna.fitnessgym.core.Change
import com.nunna.fitnessgym.core.Experience
import com.nunna.fitnessgym.core.Goal
import com.nunna.fitnessgym.core.Tracking
import com.nunna.fitnessgym.core.UserSettings
import com.nunna.fitnessgym.data.AppClock
import com.nunna.fitnessgym.data.GymRepo
import com.nunna.fitnessgym.data.db.GymDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Database-level edge cases for every workout operation (no UI). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class RepoTest {
    private lateinit var db: GymDatabase
    private lateinit var repo: GymRepo
    private var now = 1_791_200_000_000L // 2026-10-05

    @Before fun setUp() {
        AppClock.now = { now }
        db = AppGraph.useTestDatabase(ApplicationProvider.getApplicationContext())
        repo = AppGraph.repo(ApplicationProvider.getApplicationContext())
        runBlocking { repo.completeOnboarding(UserSettings(goal = Goal.BUILD_MUSCLE, experience = Experience.BEGINNER, daysPerWeek = 3)) }
    }

    @After fun tearDown() = db.close()

    private fun weightedPos(sid: Long) = runBlocking { db.dao().session(sid)!!.ordered.indexOfFirst { it.ex.tracking == Tracking.WEIGHT_REPS.name } }
    private fun full(sid: Long) = runBlocking { db.dao().session(sid)!! }

    @Test fun onboardingStoresSettingsAndPlan() = runBlocking {
        val p = db.dao().profile()!!
        assertTrue(p.onboarded)
        assertEquals(3, db.dao().plan().size)
        assertEquals(Goal.BUILD_MUSCLE, repo.settings()!!.goal)
    }

    @Test fun startingTwiceResumesTheSameWorkout() = runBlocking {
        val a = repo.startPlanDay(0)
        assertEquals(a, repo.startPlanDay(1))
        assertEquals(a, repo.startEmpty())
        val f = full(a)
        assertEquals(db.dao().plan()[0].slots.size, f.exercises.size)
        assertTrue(f.exercises.all { e -> e.sets.size == e.ex.targetSets })
    }

    @Test fun completeSetValidatesInput() = runBlocking {
        val sid = repo.startPlanDay(0)
        val set = full(sid).ordered[weightedPos(sid)].orderedSets[0]
        assertFalse(repo.completeSet(set.id, null, 10, null, null).ok)
        assertFalse(repo.completeSet(set.id, -5.0, 10, null, null).ok)
        assertFalse(repo.completeSet(set.id, 20.0, 0, null, null).ok)
        assertFalse(repo.completeSet(set.id, 20.0, 999, null, null).ok)
        assertFalse(repo.completeSet(set.id, 1500.0, 10, null, null).ok)
        val ok = repo.completeSet(set.id, 20.0, 10, null, 2)
        assertTrue(ok.ok); assertTrue(ok.restSec > 0); assertNotNull(ok.nextLabel)
        assertTrue(db.dao().set(set.id)!!.done)
    }

    @Test fun loggedWeightCarriesIntoTheNextEmptySets() = runBlocking {
        val sid = repo.startEmpty()
        val se = repo.addExercise(sid, "dumbbell-bench-press")!!
        val sets = db.dao().setsOf(se)
        assertTrue(sets.all { it.weight == null })            // first time: no weight yet
        repo.completeSet(sets[0].id, 17.5, 11, null, null)
        assertTrue(db.dao().setsOf(se).drop(1).all { it.weight == 17.5 && !it.done })
    }

    @Test fun lastSetOfTheWorkoutHasNoRest() = runBlocking {
        val sid = repo.startEmpty()
        val seId = repo.addExercise(sid, "dumbbell-bench-press")!!
        val sets = db.dao().setsOf(seId)
        for ((i, s) in sets.withIndex()) {
            val r = repo.completeSet(s.id, 20.0, 10, null, null)
            if (i < sets.size - 1) assertTrue(r.restSec > 0) else { assertEquals(0, r.restSec); assertNull(r.nextLabel) }
        }
    }

    @Test fun swapIsRefusedAfterALoggedSetAndRewritesUnloggedSets() = runBlocking {
        val sid = repo.startEmpty()
        val seId = repo.addExercise(sid, "dumbbell-bench-press")!!
        assertTrue(repo.swapExercise(seId, "plank"))
        val se = db.dao().exercise(seId)!!
        assertEquals("plank", se.exerciseId); assertEquals(Tracking.TIME.name, se.tracking)
        assertTrue(db.dao().setsOf(seId).all { it.seconds != null && it.reps == null })
        repo.completeSet(db.dao().setsOf(seId)[0].id, null, null, 30, null)
        assertFalse(repo.swapExercise(seId, "pushups"))
        assertFalse(repo.swapExercise(9999, "pushups"))
        assertFalse(repo.swapExercise(seId, "no-such-exercise"))
    }

    @Test fun setsCanBeAddedAndRemovedButNeverTheLast() = runBlocking {
        val sid = repo.startEmpty()
        val seId = repo.addExercise(sid, "pushups")!!
        val n = db.dao().setsOf(seId).size
        repo.addSet(seId)
        assertEquals(n + 1, db.dao().setsOf(seId).size)
        while (db.dao().setsOf(seId).size > 1) assertTrue(repo.removeSet(db.dao().setsOf(seId).first().id))
        assertFalse(repo.removeSet(db.dao().setsOf(seId).first().id))
        assertEquals(listOf(0), db.dao().setsOf(seId).map { it.setIndex })
    }

    @Test fun moveSkipRemoveExercise() = runBlocking {
        val sid = repo.startPlanDay(0)
        val ids = full(sid).ordered.map { it.ex.id }
        repo.moveExercise(ids[2], -2)
        assertEquals(ids[2], full(sid).ordered[0].ex.id)
        repo.moveExercise(ids[2], -1) // already first: no change
        assertEquals(ids[2], full(sid).ordered[0].ex.id)
        repo.setSkipped(ids[1], true)
        assertTrue(full(sid).exercises.first { it.ex.id == ids[1] }.ex.skipped)
        repo.removeExercise(ids[3])
        assertEquals(ids.size - 1, full(sid).exercises.size)
    }

    @Test fun finishKeepsOnlyLoggedWorkAndAdvancesTheRotation() = runBlocking {
        val sid = repo.startPlanDay(0)
        val pos = weightedPos(sid)
        val set = full(sid).ordered[pos].orderedSets[0]
        repo.completeSet(set.id, 20.0, 12, null, null)
        now += 45 * 60_000
        val sum = repo.finish(sid)!!
        assertEquals(1, sum.sets); assertEquals(45, sum.minutes); assertEquals(240.0, sum.volume, 1e-9)
        assertEquals(10 + 50, sum.xp)
        val f = full(sid)
        assertEquals("done", f.session.status)
        assertEquals(1, f.exercises.size); assertEquals(1, f.exercises[0].sets.size)
        assertEquals(1, db.dao().profile()!!.nextDay)
        assertEquals(60, db.dao().profile()!!.xp)
        assertNull(db.dao().active())
    }

    @Test fun finishWithNothingLoggedDiscards() = runBlocking {
        val sid = repo.startPlanDay(0)
        assertNull(repo.finish(sid))
        assertNull(db.dao().session(sid))
        assertEquals(0, db.dao().profile()!!.nextDay)
    }

    @Test fun nextWorkoutIsPrefilledFromHistory() = runBlocking {
        val sid = repo.startEmpty()
        val seId = repo.addExercise(sid, "dumbbell-bench-press")!!
        for (s in db.dao().setsOf(seId)) repo.completeSet(s.id, 20.0, 12, null, null)
        repo.finish(sid)
        now += 86_400_000
        val sid2 = repo.startEmpty()
        val se2 = repo.addExercise(sid2, "dumbbell-bench-press")!!
        val sets = db.dao().setsOf(se2)
        assertTrue(sets.all { it.weight == 22.5 })
        assertTrue(db.dao().exercise(se2)!!.note.startsWith("+2.5 kg"))
        val sug = repo.suggestion(repo.exercise("dumbbell-bench-press")!!, Tracking.WEIGHT_REPS, 3, 8, 12, 2, 0, repo.settings()!!)
        assertEquals(Change.UP, sug.change)
    }

    @Test fun personalRecordsAreDetectedAgainstEarlierWorkouts() = runBlocking {
        val sid = repo.startEmpty()
        val se = repo.addExercise(sid, "dumbbell-bench-press")!!
        assertTrue(repo.completeSet(db.dao().setsOf(se)[0].id, 20.0, 10, null, null).records.isEmpty()) // first ever: baseline
        repo.finish(sid)
        val sid2 = repo.startEmpty()
        val se2 = repo.addExercise(sid2, "dumbbell-bench-press")!!
        val r = repo.completeSet(db.dao().setsOf(se2)[0].id, 25.0, 8, null, null)
        assertTrue(r.records.contains("Heaviest weight"))
        assertEquals("Heaviest weight", db.dao().setsOf(se2)[0].pr)
        val sum = repo.finish(sid2)!!
        assertEquals(1, sum.records.size)
        assertEquals(10 + 50 + 25, sum.xp)
    }

    @Test fun deletingAWorkoutCascadesToItsSets() = runBlocking {
        val sid = repo.startEmpty()
        val se = repo.addExercise(sid, "pushups")!!
        repo.completeSet(db.dao().setsOf(se)[0].id, null, 15, null, null)
        repo.finish(sid)
        repo.deleteSession(sid)
        assertNull(db.dao().session(sid))
        assertTrue(db.dao().setsOf(se).isEmpty())
        assertTrue(db.dao().history("pushups").isEmpty())
    }

    @Test fun excludingRebuildsThePlanWithoutIt() = runBlocking {
        val id = db.dao().plan()[0].ordered[0].exerciseId
        repo.exclude(id, true)
        assertTrue(db.dao().plan().flatMap { it.slots }.none { it.exerciseId == id })
        repo.exclude(id, false)
        assertFalse(id in repo.settings()!!.excluded)
    }

    @Test fun csvExportListsEveryLoggedSet() = runBlocking {
        val sid = repo.startEmpty()
        val se = repo.addExercise(sid, "plank")!!
        repo.completeSet(db.dao().setsOf(se)[0].id, null, null, 45, null)
        repo.finish(sid)
        val csv = repo.csv(db.dao().session(sid)!!.let { listOf(it) })
        val lines = csv.trim().lines()
        assertEquals(2, lines.size)
        assertTrue(lines[1].contains("\"Plank\",1,,KG,,45,"))
    }

    @Test fun resetClearsEverythingButKeepsSettingsForReview() = runBlocking {
        val sid = repo.startEmpty()
        val se = repo.addExercise(sid, "pushups")!!
        repo.completeSet(db.dao().setsOf(se)[0].id, null, 12, null, null)
        repo.finish(sid)
        repo.resetAll()
        val p = db.dao().profile()!!
        assertFalse(p.onboarded); assertEquals(0, p.xp)
        assertTrue(db.dao().plan().isEmpty())
        assertTrue(db.dao().doneStarts().isEmpty())
    }

    @Test fun cardioIsLoggedInSecondsAndProgressesByAMinute() = runBlocking {
        val treadmill = repo.exercises.first { it.motion == "treadmill_run" }
        val sid = repo.startEmpty()
        val se = repo.addExercise(sid, treadmill.id)!!
        val set = db.dao().setsOf(se)[0]
        assertEquals(600, set.seconds)
        repo.completeSet(set.id, null, null, 600, null)
        repo.finish(sid)
        val se2 = repo.addExercise(repo.startEmpty(), treadmill.id)!!
        assertEquals(660, db.dao().setsOf(se2)[0].seconds)
    }
}
