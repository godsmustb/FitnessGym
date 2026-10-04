package com.nunna.fitnessgym.anim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Validates the real content/ folder: every rule in docs/CONTENT_SCHEMA.md and every motion file. */
class ContentTest {
    private val dir = File(System.getProperty("contentDir") ?: "../content")
    private val motions: Map<String, Motion> by lazy {
        File(dir, "motions").listFiles { f -> f.extension == "json" }!!.associate { f ->
            val m = try { Motion.parse(f.readText()) } catch (e: Exception) { throw AssertionError("${f.name}: ${e.message}") }
            assertEquals("file name must match id", f.nameWithoutExtension, m.template.id)
            m.template.id to m
        }
    }
    private val exercises by lazy { Library.parseExercises(File(dir, "exercises.json").readText()) }

    @Test fun exercisesFollowSchema() {
        val errs = Library.validate(exercises, motions.keys)
        assertTrue(errs.take(30).joinToString("\n"), errs.isEmpty())
    }

    @Test fun everyLibraryTagHasExercises() {
        for (t in Library.TAGS) assertTrue("no exercises tagged $t", exercises.count { t in it.tags } >= 10)
    }

    @Test fun everyMotionRendersCleanly() {
        for ((id, m) in motions) {
            val scene = FigureScene(m, Activation(setOf("chest", "quadriceps"), setOf("triceps"), "dumbbell"))
            for (i in 0 until 12) {
                val prims = scene.frame(i / 12.0, 400f, 500f)
                assertTrue("$id: no shapes", prims.size > 40)
                for (p in prims) for (v in p.xy) assertTrue("$id: NaN/inf at phase ${i / 12.0}", v.isFinite())
            }
        }
    }

    @Test fun motionsKeepEffortInRange() {
        for ((id, m) in motions) for (k in m.keys) {
            for (e in listOfNotNull(k.e, k.el, k.er)) assertTrue("$id t=${k.t}: effort $e", e in 0.0..1.0)
        }
    }
}
