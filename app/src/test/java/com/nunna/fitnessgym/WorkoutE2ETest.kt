package com.nunna.fitnessgym

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nunna.fitnessgym.core.Ladder
import com.nunna.fitnessgym.core.Tracking
import com.nunna.fitnessgym.timer.RestTimer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class WorkoutE2ETest : E2EBase() {

    private fun startWorkout() { launch(); onboard(); click("go"); waitFor("workout_title") }
    private fun ordered() = active()!!.ordered
    private fun tracking(pos: Int) = Tracking.valueOf(ordered()[pos].ex.tracking)
    private fun firstWeighted() = ordered().indexOfFirst { it.ex.tracking == "WEIGHT_REPS" }.also { assertTrue("plan needs a weighted lift", it >= 0) }

    /** Fills a set row the way the owner would and taps ✓. */
    private fun logSet(pos: Int, i: Int, w: String? = "20", r: String? = "12", s: String? = "30") {
        when (tracking(pos)) {
            Tracking.WEIGHT_REPS -> { if (w != null) type("w_${pos}_$i", w); if (r != null) type("r_${pos}_$i", r) }
            Tracking.REPS -> if (r != null) type("r_${pos}_$i", r)
            Tracking.TIME, Tracking.CARDIO -> if (s != null) type("s_${pos}_$i", s)
            Tracking.WEIGHT_TIME -> { if (w != null) type("w_${pos}_$i", w); if (s != null) type("s_${pos}_$i", s) }
        }
        click("check_${pos}_$i")
    }

    private fun finishWorkout() { click("finish"); click("confirm_finish") }

    @Test fun happyPathLogFinishReviewAndProgress() {
        startWorkout()
        val pos = firstWeighted()
        val ex = repo.exercise(ordered()[pos].ex.exerciseId)!!
        assertTrue("first time has guidance", textExists("First time"))
        val n = ordered()[pos].sets.size
        for (i in 0 until n) logSet(pos, i, "20", "12")
        assertTrue(ordered()[pos].sets.all { it.done && it.weight == 20.0 && it.reps == 12 })
        assertTrue(exists("rest_banner"))
        tag("progress_line").assertTextContains("$n /", substring = true)

        advanceMinutes(40)
        finishWorkout()
        waitFor("summary_title")
        mtag("summary_sets").assertTextContains("$n", substring = true)
        mtag("summary_xp").assertTextContains("+${n * 10 + 50}", substring = true)
        click("summary_done")

        waitFor("go")
        mtag("week_count").assertTextContains("1 / 3", substring = true)
        mtag("streak").assertTextContains("1", substring = true)
        tag("day_title").assertTextContains("Full Body B")

        click("tab_progress")
        mtag("total_workouts").assertTextContains("1", substring = true)
        assertTrue(textExists("20 kg × 12"))

        // Next time on the same day, the weight goes up by the smallest step and the reason is shown.
        click("tab_today"); click("day_0"); click("go"); waitFor("workout_title")
        val up = Ladder.forExercise(ex.equipment, settings().profile, settings().units).up(20.0)!!
        find("w_${pos}_0").assertTextContains(com.nunna.fitnessgym.ui.Fmt.w(up))
        find("note_$pos").assertTextContains("you hit", substring = true)
    }

    @Test fun invalidEntriesAreRejectedWithAPlainMessage() {
        startWorkout()
        val pos = firstWeighted()
        type("w_${pos}_0", ""); click("check_${pos}_0")
        tag("set_error").assertTextContains("Enter the weight", substring = true)
        type("w_${pos}_0", "5000"); click("check_${pos}_0")
        tag("set_error").assertTextContains("between 0 and 1000", substring = true)
        type("w_${pos}_0", "20"); type("r_${pos}_0", "0"); click("check_${pos}_0")
        tag("set_error").assertTextContains("Enter reps", substring = true)
        type("r_${pos}_0", "abc")                         // letters are filtered out
        assertTrue(ordered()[pos].orderedSets[0].reps == null)
        type("r_${pos}_0", "10"); click("check_${pos}_0")
        assertTrue(ordered()[pos].orderedSets[0].done)
        assertFalse(exists("set_error"))
    }

    @Test fun stepperUsesAvailableWeights() {
        startWorkout()
        val pos = firstWeighted()
        type("w_${pos}_0", "20")
        click("wplus_${pos}_0")
        val ex = repo.exercise(ordered()[pos].ex.exerciseId)!!
        val up = Ladder.forExercise(ex.equipment, settings().profile, settings().units).up(20.0)!!
        assertEquals(up, ordered()[pos].orderedSets[0].weight!!, 1e-9)
        click("wminus_${pos}_0"); click("wminus_${pos}_0")
        assertTrue(ordered()[pos].orderedSets[0].weight!! < 20.0)
    }

    @Test fun tickingTwiceUndoesASet() {
        startWorkout()
        val pos = firstWeighted()
        logSet(pos, 0)
        assertTrue(ordered()[pos].orderedSets[0].done)
        click("check_${pos}_0")
        assertFalse(ordered()[pos].orderedSets[0].done)
    }

    @Test fun restTimerCanBeExtendedShortenedAndSkipped() {
        startWorkout()
        val pos = firstWeighted()
        logSet(pos, 0)
        val rest = ordered()[pos].ex.restSec
        tag("rest_left").assertTextContains("Rest " + com.nunna.fitnessgym.ui.Fmt.clock(rest))
        click("rest_plus")
        tag("rest_left").assertTextContains("Rest " + com.nunna.fitnessgym.ui.Fmt.clock(rest + 15))
        click("rest_minus"); click("rest_minus")
        tag("rest_left").assertTextContains("Rest " + com.nunna.fitnessgym.ui.Fmt.clock(rest - 15))
        click("rest_skip")
        assertFalse(exists("rest_banner"))
        assertNull(RestTimer.state.value)
    }

    @Test fun skipSwapMoveRemoveAndAddExercises() {
        startWorkout()
        val before = ordered().map { it.ex.exerciseId }
        // Skip the first exercise.
        click("menu_0"); click("menu_skip")
        assertTrue(ordered()[0].ex.skipped); assertTrue(textExists("Skipped today"))
        // Swap the second exercise for the top suggestion.
        click("menu_1"); click("menu_swap")
        waitFor("pick_list")
        waitText("Best swaps")
        val pickTag = compose.onAllNodes(tagPrefix("pick_"), useUnmergedTree = true).fetchSemanticsNodes()
            .map { it.config[SemanticsProperties.TestTag] }.first { it !in setOf("pick_list", "pick_search", "pick_mine", "pick_empty") }
        click(pickTag)
        waitFor("workout_title")
        assertEquals(pickTag.removePrefix("pick_"), ordered()[1].ex.exerciseId)
        assertNotEquals(before[1], ordered()[1].ex.exerciseId)
        // Move it up, then remove the (skipped) exercise now in second place.
        click("menu_1"); click("menu_up")
        assertEquals(pickTag.removePrefix("pick_"), ordered()[0].ex.exerciseId)
        val count = ordered().size
        click("menu_1"); click("menu_remove")
        assertEquals(count - 1, ordered().size)
        // Add a timed exercise (plank) from the picker.
        click("add_exercise"); waitFor("pick_search")
        type("pick_search", "Plank"); click("pick_plank")
        waitFor("workout_title")
        val p = ordered().indexOfFirst { it.ex.exerciseId == "plank" }
        assertTrue(p >= 0); assertEquals(Tracking.TIME, tracking(p))
        logSet(p, 0, s = "45")
        assertEquals(45, ordered()[p].orderedSets[0].seconds)
    }

    @Test fun swapIsBlockedOnceASetIsLogged() {
        startWorkout()
        val pos = firstWeighted()
        logSet(pos, 0)
        click("menu_$pos")
        assertTrue(textExists("Swap (no logged sets only)"))
    }

    @Test fun addSetAndLongPressToRemoveButNeverTheLastOne() {
        startWorkout()
        val pos = firstWeighted()
        val n = ordered()[pos].sets.size
        click("add_set_$pos")
        assertEquals(n + 1, ordered()[pos].sets.size)
        find("setnum_${pos}_$n").performTouchInput { longClick() }; compose.waitForIdle()
        assertEquals(n, ordered()[pos].sets.size)
        repeat(n - 1) { find("setnum_${pos}_0").performTouchInput { longClick() }; compose.waitForIdle() }
        assertEquals(1, ordered()[pos].sets.size)
        find("setnum_${pos}_0").performTouchInput { longClick() }; compose.waitForIdle()
        assertEquals(1, ordered()[pos].sets.size)
        tag("set_error").assertTextContains("at least one set", substring = true)
    }

    @Test fun finishingWithNothingLoggedSavesNothing() {
        startWorkout()
        click("finish")
        assertTrue(textExists("Nothing logged yet"))
        click("confirm_finish")
        waitFor("summary_empty")
        click("summary_done")
        waitFor("go")
        assertNull(active())
        assertEquals(0, kotlinx.coroutines.runBlocking { db.dao().doneStarts() }.size)
    }

    @Test fun keepGoingClosesTheFinishDialog() {
        startWorkout()
        logSet(firstWeighted(), 0)
        click("finish"); click("keep_going")
        assertTrue(active() != null)
        assertTrue(exists("workout_title"))
    }

    @Test fun unloggedSetsAreDroppedOnFinish() {
        startWorkout()
        val pos = firstWeighted()
        logSet(pos, 0)
        val sid = active()!!.session.id
        finishWorkout(); waitFor("summary_title")
        val saved = kotlinx.coroutines.runBlocking { db.dao().session(sid)!! }
        assertEquals("done", saved.session.status)
        assertEquals(1, saved.exercises.size)
        assertEquals(1, saved.exercises[0].sets.size)
    }

    @Test fun discardDeletesTheWorkout() {
        startWorkout()
        logSet(firstWeighted(), 0)
        click("discard"); click("confirm_discard")
        waitFor("go")
        assertNull(active())
        click("tab_progress")
        mtag("total_workouts").assertTextContains("0", substring = true)
    }

    @Test fun workoutSurvivesTheAppBeingClosedAndReopened() {
        startWorkout()
        val pos = firstWeighted()
        logSet(pos, 0, "17.5", "9")
        // Activity recreated (rotation, low memory): Android restores the workout screen itself.
        scenario!!.recreate(); compose.waitForIdle()
        waitFor("workout_title")
        assertTrue(ordered()[pos].orderedSets[0].done)
        // Cold start (app swiped away / process killed): Today offers to resume.
        scenario!!.close(); launch()
        waitFor("resume")
        assertTrue(textExists("Workout in progress"))
        click("resume"); waitFor("workout_title")
        assertTrue(ordered()[pos].orderedSets[0].done)
        assertEquals(17.5, ordered()[pos].orderedSets[0].weight!!, 1e-9)
        assertTrue(compose.onAllNodes(hasContentDescription("Logged. Tap to undo"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
    }

    @Test fun startingTwiceNeverCreatesTwoWorkouts() {
        startWorkout()
        pressBack()
        waitFor("resume")
        assertFalse(exists("go"))
        val first = active()!!.session.id
        kotlinx.coroutines.runBlocking { assertEquals(first, repo.startPlanDay(1)); assertEquals(first, repo.startEmpty()) }
    }

    @Test fun quickWorkoutFromScratch() {
        launch(); onboard()
        click("quick"); waitFor("workout_title")
        assertTrue(textExists("Empty workout"))
        click("add_exercise"); type("pick_search", "Pushups"); click("pick_pushups")
        waitFor("workout_title")
        logSet(0, 0, r = "15")
        finishWorkout(); waitFor("summary_title")
        click("summary_done")
        waitFor("go")
        // A quick workout doesn't move the plan rotation.
        tag("day_title").assertTextContains("Full Body A")
    }

    @Test fun neverSuggestHidesAnExerciseFromThePlan() {
        startWorkout()
        val id = ordered()[0].ex.exerciseId
        click("menu_0"); click("menu_never")
        assertTrue(id in settings().excluded)
        assertTrue(plan().flatMap { it.slots }.none { it.exerciseId == id })
        assertTrue("today's workout is left alone", ordered().any { it.ex.exerciseId == id })
    }

    @Test fun heavierSecondWorkoutCelebratesAPersonalRecord() {
        startWorkout()
        val pos = firstWeighted()
        logSet(pos, 0, "20", "10")
        finishWorkout(); waitFor("summary_title"); click("summary_done")
        advanceMinutes(60 * 24)
        click("day_0"); click("go"); waitFor("workout_title")
        logSet(pos, 0, "30", "8")
        assertTrue(exists("pr_banner"))
        tag("pr_banner").assertTextContains("Heaviest weight", substring = true)
        finishWorkout(); waitFor("summary_title")
        assertTrue(exists("summary_pr"))
    }

    private fun tagPrefix(p: String) = SemanticsMatcher("tag starts with $p") { (if (SemanticsProperties.TestTag in it.config) it.config[SemanticsProperties.TestTag] else "").startsWith(p) }

}
