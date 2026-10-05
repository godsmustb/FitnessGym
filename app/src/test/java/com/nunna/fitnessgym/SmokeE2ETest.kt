package com.nunna.fitnessgym

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class SmokeE2ETest : E2EBase() {
    @Test fun onboardThenStartWorkout() {
        launch()
        onboard()
        assertEquals(3, plan().size)
        click("go")
        waitFor("workout_title")
        assertTrue(active() != null)
        assertTrue(textExists("Full Body A"))
    }
}
