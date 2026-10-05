package com.nunna.fitnessgym

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nunna.fitnessgym.core.Tracking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders real app screens to build/screens/<n>.png (Robolectric native graphics) so the UI can be
 * reviewed without a phone. Not an assertion test; it fails only if a screen can't be drawn.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest : E2EBase() {
    private val out = File("build/screens").apply { mkdirs() }

    private fun shot(name: String) {
        compose.waitForIdle()
        scenario!!.onActivity { act ->
            // Draw the whole window (dialogs and menus live in other windows, so include those too).
            val views = listOf(act.window.decorView) + rootViews().filter { it !== act.window.decorView }
            val main = views.first()
            val bmp = Bitmap.createBitmap(main.width, main.height, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            for (v in views) {
                val loc = IntArray(2); v.getLocationOnScreen(loc)
                c.save(); c.translate(loc[0].toFloat(), loc[1].toFloat()); v.draw(c); c.restore()
            }
            File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun rootViews(): List<View> = runCatching {
        val wmg = Class.forName("android.view.WindowManagerGlobal")
        val inst = wmg.getMethod("getInstance").invoke(null)
        val f = wmg.getDeclaredField("mViews").apply { isAccessible = true }
        (f.get(inst) as List<View>).toList()
    }.getOrDefault(emptyList())

    @Test fun captureMainScreens() {
        launch()
        waitFor("onb_next"); shot("01_welcome")
        click("onb_next"); clickText("Build muscle"); shot("02_goal")
        click("onb_next"); clickText("Beginner"); click("onb_next"); click("days_3"); shot("03_schedule")
        click("onb_next"); clickText("Both"); clickText("Dumbbells"); click("db_10"); click("db_15"); shot("04_where")
        click("onb_next"); clickText("Pilates"); click("onb_next"); click("onb_next"); click("parq_yes_5"); shot("05_safety")
        click("onb_next"); shot("06_summary")
        click("build_plan"); waitFor("go"); shot("07_today")
        click("go"); waitFor("workout_title")
        val pos = active()!!.ordered.indexOfFirst { it.ex.tracking == Tracking.WEIGHT_REPS.name }
        type("w_${pos}_0", "20"); type("r_${pos}_0", "12"); click("check_${pos}_0")
        shot("08_workout")
        click("menu_0"); shot("09_menu"); click("menu_addset")
        advanceMinutes(42)
        click("finish"); shot("10_finish_dialog"); click("confirm_finish"); waitFor("summary_title"); shot("11_summary")
        click("summary_done"); waitFor("go")
        click("tab_progress"); shot("12_progress")
        click("tab_me"); shot("13_me")
        click("tab_library"); shot("14_library")
        click("tab_muscles"); shot("15_muscles")
    }
}
