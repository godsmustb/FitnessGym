package com.nunna.fitnessgym

import android.content.Context
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.nunna.fitnessgym.data.AppClock
import com.nunna.fitnessgym.data.GymRepo
import com.nunna.fitnessgym.data.db.GymDatabase
import com.nunna.fitnessgym.timer.RestTimer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * End-to-end harness: the real app (all screens, navigation, Room, content) on the JVM via Robolectric,
 * with an in-memory database, a fixed clock, no endless animations and no Android services.
 */
abstract class E2EBase {
    @get:Rule val compose = createEmptyComposeRule()
    protected lateinit var db: GymDatabase
    protected lateinit var repo: GymRepo
    protected var scenario: ActivityScenario<MainActivity>? = null
    protected val ctx: Context get() = ApplicationProvider.getApplicationContext()
    private val zone = ZoneId.of("America/Toronto")
    protected var nowMs: Long = LocalDateTime.of(2026, 10, 5, 8, 0).atZone(zone).toInstant().toEpochMilli()

    @Before fun setUpApp() {
        AppClock.zone = zone
        AppClock.now = { nowMs }
        RestTimer.useService = false
        RestTimer.stop(ctx)
        db = AppGraph.useTestDatabase(ctx)
        repo = AppGraph.repo(ctx)
    }

    @After fun tearDownApp() {
        scenario?.close()
        RestTimer.stop(ctx)
        db.close()
    }

    protected fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitForIdle()
    }

    protected fun pressBack() {
        scenario!!.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    protected fun advanceMinutes(m: Long) { nowMs += m * 60_000 }

    /** Node by tag in the unmerged tree (works for children of clickable rows). */
    protected fun tag(t: String) = compose.onNodeWithTag(t, useUnmergedTree = true)
    /** Node by tag in the merged tree (for tiles whose texts are merged into one node). */
    protected fun mtag(t: String) = compose.onNodeWithTag(t)
    protected fun text(t: String) = compose.onNodeWithText(t, useUnmergedTree = true)
    protected fun exists(t: String) = compose.onAllNodesWithTag(t, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    protected fun textExists(t: String, substring: Boolean = true) =
        compose.onAllNodes(hasText(t, substring = substring), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    /** Finds a tagged node, scrolling a lazy list to it if needed, waiting briefly for it to appear. */
    protected fun find(t: String): SemanticsNodeInteraction {
        for (list in listOf("workout_list", "pick_list", "library_list")) {
            if (!exists(t) && exists(list)) runCatching { tag(list).performScrollToNode(hasTestTag(t)); compose.waitForIdle() }
        }
        if (!exists(t)) waitFor(t, 3_000)
        return tag(t).apply { runCatching { performScrollTo() } }
    }

    protected fun click(t: String) { find(t).performClick(); compose.waitForIdle() }

    protected fun clickText(t: String) {
        compose.waitUntil(3_000) { textExists(t, substring = false) }
        text(t).apply { runCatching { performScrollTo() } }.performClick(); compose.waitForIdle()
    }

    protected fun type(t: String, value: String) { find(t).performTextReplacement(value); compose.waitForIdle() }

    protected fun waitFor(t: String, timeoutMs: Long = 5_000) {
        try {
            compose.waitUntil(timeoutMs) { exists(t) }
        } catch (e: Throwable) {
            throw AssertionError("Timed out waiting for '$t'. Screen:\n" + screenDump().take(8000), e)
        }
    }

    protected fun waitText(t: String, timeoutMs: Long = 5_000) {
        try { compose.waitUntil(timeoutMs) { textExists(t) } } catch (e: Throwable) {
            throw AssertionError("Timed out waiting for text '$t'. Screen:\n" + screenDump().take(8000), e)
        }
    }

    protected fun screenDump(): String = runCatching {
        compose.onAllNodes(isRoot(), useUnmergedTree = true).fetchSemanticsNodes().joinToString("\n") { dump(it, 0) }
    }.getOrElse { "tree unavailable: $it" }

    private fun dump(n: SemanticsNode, d: Int): String {
        val keep = setOf("Text", "EditableText", "TestTag", "ContentDescription")
        val parts = n.config.filter { (k, _) -> k.name in keep }.map { (k, v) -> "${k.name}=$v" }
        val me = if (parts.isEmpty()) "" else "  ".repeat(d) + parts.joinToString(" ").take(150) + "\n"
        return me + n.children.joinToString("") { dump(it, d + 1) }
    }

    /** Completes onboarding through the real UI. */
    protected fun onboard(
        goal: String = "Build muscle", experience: String = "Beginner", days: Int = 3, minutes: Int = 45,
        where: String = "A gym", configure: () -> Unit = {}, interests: List<String> = emptyList(), protect: List<String> = emptyList(),
        name: String = "Nunna",
    ) {
        waitFor("onb_next")
        if (name.isNotEmpty()) type("name_field", name)
        click("onb_next")                                   // welcome -> goal
        clickText(goal); click("onb_next")                  // -> experience
        clickText(experience); click("onb_next")            // -> schedule
        click("days_$days"); click("mins_$minutes"); click("onb_next") // -> where
        clickText(where); configure(); click("onb_next")    // -> extras
        for (i in interests) clickText(i)
        click("onb_next")                                   // -> protect
        for (p in protect) clickText(p)
        click("onb_next")                                   // -> safety
        click("onb_next")                                   // -> summary
        click("build_plan")
        waitFor("go")
        tag("go").assertIsDisplayed()
    }

    protected fun settings() = runBlocking { repo.settings()!! }
    protected fun plan() = runBlocking { db.dao().plan() }
    protected fun active() = runBlocking { db.dao().active() }
}
