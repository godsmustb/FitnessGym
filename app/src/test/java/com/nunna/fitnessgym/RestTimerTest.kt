package com.nunna.fitnessgym

import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nunna.fitnessgym.data.AppClock
import com.nunna.fitnessgym.timer.RestTimer
import com.nunna.fitnessgym.timer.RestTimerService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/** The lock-screen rest timer: foreground countdown, then a "rest over" alert, then it stops itself. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class RestTimerTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private var now = 1_000_000L

    @Before fun setUp() {
        AppClock.now = { now }
        RestTimer.useService = false
        shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    @After fun tearDown() { RestTimer.stop(app) }

    @Test fun stateCountsDownAndCanBeAdjusted() {
        RestTimer.start(app, 90, "Leg Press")
        assertEquals(90, RestTimer.remainingSec())
        now += 30_000
        assertEquals(60, RestTimer.remainingSec())
        RestTimer.add(app, 15)
        assertEquals(75, RestTimer.remainingSec())
        RestTimer.add(app, -200)                 // shortening past zero ends the rest
        assertNull(RestTimer.state.value)
        RestTimer.start(app, 0, null)            // zero rest = no timer
        assertNull(RestTimer.state.value)
    }

    @Test fun serviceShowsCountdownThenAlertsAndStops() {
        RestTimer.start(app, 60, "Lat Pulldown")
        val controller = Robolectric.buildService(RestTimerService::class.java, Intent(app, RestTimerService::class.java)).create()
        controller.startCommand(0, 1)
        val service = controller.get()
        val shadow = shadowOf(service)
        assertNotNull("countdown notification is shown", shadow.lastForegroundNotification)
        assertEquals("Resting", shadow.lastForegroundNotification.extras.getString("android.title"))

        now += 60_000
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(61))
        assertNull("timer cleared when rest is over", RestTimer.state.value)
        val nm = app.getSystemService(NotificationManager::class.java)
        val posted = shadowOf(nm).allNotifications
        assertTrue(posted.any { it.extras.getString("android.title") == "Rest over. Time to lift" })
        assertTrue(shadow.isStoppedBySelf)
    }

    @Test fun serviceWithNoRestStopsImmediately() {
        RestTimer.stop(app)
        val controller = Robolectric.buildService(RestTimerService::class.java).create()
        controller.startCommand(0, 1)
        assertTrue(shadowOf(controller.get()).isStoppedBySelf)
    }
}
