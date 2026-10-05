package com.nunna.fitnessgym

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nunna.fitnessgym.core.AvoidArea
import com.nunna.fitnessgym.core.Classify
import com.nunna.fitnessgym.core.Equip
import com.nunna.fitnessgym.core.Experience
import com.nunna.fitnessgym.core.Generator
import com.nunna.fitnessgym.core.Goal
import com.nunna.fitnessgym.core.Interest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class OnboardingE2ETest : E2EBase() {

    @Test fun firstLaunchAsksQuestionsAndNextWaitsForAnAnswer() {
        launch()
        waitFor("onb_next")
        assertTrue(textExists("Welcome to Fitness Gym"))
        click("onb_next")
        assertTrue(textExists("What's your main goal?"))
        tag("onb_next").assertIsNotEnabled()
        clickText("Lose fat")
        tag("onb_next").assertIsEnabled()
        click("onb_next")
        tag("onb_next").assertIsNotEnabled()            // experience not chosen yet
        click("onb_back")                                // back keeps the earlier answer
        tag("onb_next").assertIsEnabled()
        click("onb_next"); clickText("Intermediate"); click("onb_next")
        assertTrue(textExists("How many days a week will you train?"))
    }

    @Test fun answersAreSavedAndDriveThePlan() {
        launch()
        onboard(goal = "Get stronger", experience = "Intermediate", days = 4, minutes = 60, name = "Sam")
        val s = settings()
        assertEquals(Goal.GET_STRONGER, s.goal); assertEquals(Experience.INTERMEDIATE, s.experience)
        assertEquals(4, s.daysPerWeek); assertEquals(60, s.sessionMinutes); assertEquals("Sam", s.name)
        assertEquals(listOf("Upper A", "Lower A", "Upper B", "Lower B"), plan().map { it.day.name })
        assertTrue(textExists("Good morning, Sam"))
        assertTrue(textExists("Upper A"))
    }

    @Test fun homeDumbbellsNeedAtLeastOneWeight() {
        launch()
        onboard(where = "At home", configure = {
            clickText("Dumbbells")
            tag("onb_next").assertIsNotEnabled()
            assertTrue(exists("db_hint"))
            click("db_10")
            tag("onb_next").assertIsEnabled()
            type("db_custom", "13"); click("db_add")
            assertTrue(exists("db_13"))
        })
        val p = settings().profile
        assertEquals("Home", p.name)
        assertEquals(listOf(10.0, 13.0), p.dumbbells)
        for (sl in plan().flatMap { it.slots }) {
            val ex = repo.exercise(sl.exerciseId)!!
            assertTrue("${ex.id} needs ${Classify.needs(ex)}", Classify.available(ex, p))
            assertFalse(ex.id, ex.equipment == "machine" || ex.equipment == "cable")
        }
    }

    @Test fun bodyweightOnlyHomeStillGetsAFullPlan() {
        launch()
        onboard(where = "At home", days = 2)
        val p = settings().profile
        assertEquals(setOf(Equip.MAT), p.items)
        val exs = plan().flatMap { it.slots }.map { repo.exercise(it.exerciseId)!! }
        assertTrue(exs.size >= 8)
        assertTrue(exs.all { it.equipment == "bodyweight" || it.equipment == "mat" })
    }

    @Test fun protectedAreasAndExtrasShapeThePlan() {
        launch()
        onboard(interests = listOf("Pilates"), protect = listOf("Knees", "Shoulders / overhead"), minutes = 75)
        val s = settings()
        assertEquals(setOf(Interest.PILATES), s.interests)
        assertEquals(setOf(AvoidArea.KNEES, AvoidArea.SHOULDERS), s.avoid)
        val banned = Generator.AVOID.getValue(AvoidArea.KNEES) + Generator.AVOID.getValue(AvoidArea.SHOULDERS)
        val exs = plan().flatMap { it.ordered }
        assertTrue(exs.none { repo.exercise(it.exerciseId)!!.motion in banned })
        for (d in plan()) assertTrue(d.day.name, "pilates" in repo.exercise(d.ordered.last().exerciseId)!!.tags)
    }

    @Test fun safetyYesShowsAdviceButNeverBlocks() {
        launch()
        waitFor("onb_next")
        click("onb_next"); clickText("General fitness"); click("onb_next")
        clickText("Beginner"); click("onb_next"); click("onb_next")
        clickText("A gym"); click("onb_next"); click("onb_next"); click("onb_next")
        click("parq_yes_0")
        assertTrue(exists("parq_warning"))
        tag("onb_next").assertIsEnabled()
        click("onb_next"); click("build_plan")
        waitFor("go")
        assertEquals(1, settings().parqFlags)
    }

    @Test fun oneAndSevenDayPlans() {
        launch()
        onboard(days = 1)
        assertEquals(1, plan().size)
        assertFalse("no day picker for a single day", exists("day_0"))
        runBlockingUpdate { it.copy(daysPerWeek = 7) }
        assertEquals(7, plan().size)
        assertEquals("Active Recovery", plan().last().day.name)
        waitFor("day_6")
    }

    @Test fun trainingInBothPlacesShowsASwitchThatRebuildsThePlan() {
        launch()
        onboard(where = "Both", configure = { clickText("Dumbbells"); click("db_10"); click("db_12") })
        assertEquals(listOf("Gym", "Home"), settings().profiles.map { it.name })
        assertTrue(exists("profile_Gym") && exists("profile_Home"))
        click("profile_Home")
        compose.waitUntil(5000) { settings().activeProfile == 1 }
        val home = settings().profile
        assertTrue(plan().flatMap { it.slots }.all { Classify.available(repo.exercise(it.exerciseId)!!, home) })
    }

    private fun runBlockingUpdate(f: (com.nunna.fitnessgym.core.UserSettings) -> com.nunna.fitnessgym.core.UserSettings) {
        kotlinx.coroutines.runBlocking { repo.updateSettings(f(settings())) }
        compose.waitForIdle()
    }
}
