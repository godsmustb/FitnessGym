package com.nunna.fitnessgym.data

import android.content.Context
import com.nunna.fitnessgym.anim.Exercise
import com.nunna.fitnessgym.anim.Library
import com.nunna.fitnessgym.anim.Motion

/**
 * Loads the exercise library and motion templates from assets (the repo-root content/ folder).
 * Loading never throws to the UI: problems are collected in [errors] with an error code,
 * and the Status screen shows them.
 */
class ContentRepo private constructor(ctx: Context) {
    val exercises: List<Exercise>
    val motions: Map<String, Motion>
    val errors: List<String>

    init {
        val errs = ArrayList<String>()
        val am = ctx.assets
        val ms = HashMap<String, Motion>()
        for (f in am.list("motions").orEmpty().filter { it.endsWith(".json") }) {
            try {
                val m = Motion.parse(am.open("motions/$f").bufferedReader().readText())
                ms[m.template.id] = m
            } catch (e: Exception) {
                errs += "CNT-002 motions/$f: ${e.message?.take(160)}"
            }
        }
        motions = ms
        exercises = try {
            Library.parseExercises(am.open("exercises.json").bufferedReader().readText()).sortedBy { it.name.lowercase() }
        } catch (e: Exception) {
            errs += "CNT-001 exercises.json: ${e.message?.take(200)}"
            emptyList()
        }
        val missing = exercises.map { it.motion }.toSet() - ms.keys
        if (missing.isNotEmpty()) errs += "CNT-003 ${missing.size} animations not built yet (shown with the standing figure): ${missing.sorted().joinToString()}"
        errors = errs
    }

    fun exercise(id: String) = exercises.firstOrNull { it.id == id }

    /** The exercise's animation, or the standing figure if its template is missing. */
    fun motionFor(e: Exercise): Motion = motions[e.motion] ?: motions.getValue("stand")

    companion object {
        @Volatile private var inst: ContentRepo? = null
        fun get(ctx: Context): ContentRepo = inst ?: synchronized(this) {
            inst ?: ContentRepo(ctx.applicationContext).also { inst = it }
        }
    }
}
