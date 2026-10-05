package com.nunna.fitnessgym

import android.content.Intent
import androidx.compose.ui.test.assertTextContains
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nunna.fitnessgym.core.Tracking
import com.nunna.fitnessgym.core.Units
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ProgressSettingsE2ETest : E2EBase() {

    /** Onboards, logs one weighted set (20 × 12) and finishes. Returns the exercise id. */
    private fun oneWorkout(weight: String = "20"): String {
        launch(); onboard()
        click("go"); waitFor("workout_title")
        val ordered = active()!!.ordered
        val pos = ordered.indexOfFirst { it.ex.tracking == Tracking.WEIGHT_REPS.name }
        type("w_${pos}_0", weight); type("r_${pos}_0", "12"); click("check_${pos}_0")
        advanceMinutes(30)
        click("finish"); click("confirm_finish"); waitFor("summary_title"); click("summary_done"); waitFor("go")
        return ordered[pos].ex.exerciseId
    }

    @Test fun calendarDayLogAndMonthNavigation() {
        oneWorkout()
        click("tab_progress")
        mtag("total_workouts").assertTextContains("1", substring = true)
        assertTrue(exists("cal_2026-10-05"))
        assertTrue(exists("daylog"))
        assertTrue(textExists("20 kg × 12"))
        click("prev_month")
        tag("month_title").assertTextContains("September 2026")
        click("cal_2026-09-10")
        assertTrue(exists("day_empty"))
        click("next_month"); click("next_month")
        tag("month_title").assertTextContains("November 2026")
    }

    @Test fun sessionDetailCanBeDeleted() {
        oneWorkout()
        click("tab_progress"); click("daylog")
        waitFor("delete_session")
        click("delete_session"); click("confirm_delete")
        waitFor("total_workouts")
        mtag("total_workouts").assertTextContains("0", substring = true)
        assertTrue(exists("day_empty"))
    }

    @Test fun streakAndWeekCountOnToday() {
        oneWorkout()
        mtag("week_count").assertTextContains("1 / 3", substring = true)
        mtag("streak").assertTextContains("1", substring = true)
        assertTrue(textExists("Lv 1"))
    }

    @Test fun editingGoalsRebuildsThePlanAndKeepsHistory() {
        oneWorkout()
        click("tab_me"); click("edit_goals")
        waitFor("onb_next")
        assertTrue(textExists("What's your main goal?"))   // edit starts at the goal step
        click("onb_next"); click("onb_next")
        click("days_4"); click("onb_next"); click("onb_next"); click("onb_next"); click("onb_next"); click("onb_next")
        assertTrue(textExists("Save & rebuild"))
        click("build_plan")
        waitFor("edit_goals")
        assertEquals(4, settings().daysPerWeek)
        assertEquals("Upper A", plan()[0].day.name)
        click("tab_progress")
        mtag("total_workouts").assertTextContains("1", substring = true)
    }

    @Test fun newPlanVariationKeepsGoalAndChangesSeed() {
        launch(); onboard()
        val before = kotlinx.coroutines.runBlocking { db.dao().profile()!!.planSeed }
        click("tab_me"); click("regen")
        tag("me_info").assertTextContains("New plan variation", substring = true)
        val after = kotlinx.coroutines.runBlocking { db.dao().profile()!!.planSeed }
        assertTrue(before != after)
        assertEquals(3, plan().size)
    }

    @Test fun switchingToPoundsShowsLbEverywhere() {
        launch(); onboard()
        click("tab_me"); click("units_LB")
        assertEquals(Units.LB, settings().units)
        click("tab_today"); click("go"); waitFor("workout_title")
        assertTrue(textExists("lb"))
        assertEquals("LB", active()!!.ordered.first().sets.first().unit)
    }

    @Test fun effortLoggingAddsAnRirBox() {
        launch(); onboard()
        click("tab_me"); click("rir_switch")
        assertTrue(settings().logEffort)
        click("tab_today"); click("go"); waitFor("workout_title")
        val pos = active()!!.ordered.indexOfFirst { it.ex.tracking == Tracking.WEIGHT_REPS.name }
        type("w_${pos}_0", "20"); type("rir_${pos}_0", "2"); click("check_${pos}_0")
        assertEquals(2, active()!!.ordered[pos].orderedSets[0].rir)
    }

    @Test fun hiddenExercisesCanBeAllowedAgain() {
        launch(); onboard()
        click("go"); waitFor("workout_title")
        val id = active()!!.ordered[0].ex.exerciseId
        click("menu_0"); click("menu_never")
        click("discard"); click("confirm_discard"); waitFor("go")
        click("tab_me")
        click("unhide_$id")
        assertFalse(id in settings().excluded)
    }

    @Test fun exportSharesACsvOfEveryLoggedSet() {
        oneWorkout()
        click("tab_me"); click("export")
        val started = shadowOf(androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()).nextStartedActivity
        assertNotNull(started)
        assertEquals(Intent.ACTION_CHOOSER, started.action)
        @Suppress("DEPRECATION") val inner = started.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        val csv = inner.getStringExtra(Intent.EXTRA_TEXT)!!
        assertTrue(csv.startsWith("date,workout,exercise,set,weight,unit,reps,seconds,rir"))
        assertTrue(csv.contains("2026-10-05"))
        assertTrue(csv.contains(",20.0,KG,12,"))
    }

    @Test fun resetEverythingStartsSetupAgain() {
        oneWorkout()
        click("tab_me"); click("reset"); click("confirm_reset")
        waitFor("onb_next")
        assertTrue(textExists("Welcome to Fitness Gym"))
        assertEquals(0, kotlinx.coroutines.runBlocking { db.dao().doneStarts() }.size)
        onboard()
        mtag("week_count").assertTextContains("0 / 3", substring = true)
    }

    @Test fun exerciseDetailShowsYourHistoryAndCanAddToWorkout() {
        val id = oneWorkout("25")
        click("tab_library")
        type("library_search", repo.exercise(id)!!.name)
        click("lib_$id")
        waitFor("best_set")
        tag("best_set").assertTextContains("25 kg × 12", substring = true)
        assertTrue(exists("history_line"))
        // Start a quick workout, then add this exercise from its page.
        pressBack(); click("tab_today"); click("quick"); waitFor("workout_title")
        click("add_exercise"); type("pick_search", "Plank"); click("pick_plank"); waitFor("workout_title")
        click("ex_name_0")
        waitFor("add_to_workout")
        click("add_to_workout")
        waitFor("workout_title")
        assertTrue(active()!!.exercises.any { it.ex.exerciseId == "plank" })
        assertEquals(2, active()!!.exercises.size)
    }

    @Test fun libraryFiltersAndMuscleMap() {
        launch(); onboard()
        click("tab_library")
        click("libtag_pilates")
        assertTrue(textExists("Pilates Hundred"))
        assertFalse(textExists("Leg Press"))
        click("tab_muscles")
        click("muscle_quadriceps")
        assertTrue(textExists("exercises for Quads"))
    }
}
