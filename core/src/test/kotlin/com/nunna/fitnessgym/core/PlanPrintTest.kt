package com.nunna.fitnessgym.core

import com.nunna.fitnessgym.anim.Library
import org.junit.Test
import java.io.File

/** Not an assertion test: writes sample plans to build/sample-plans.txt for human review. */
class PlanPrintTest {
    @Test fun printSamples() {
        val lib = Library.parseExercises(File(System.getProperty("contentDir"), "exercises.json").readText())
        val byId = lib.associateBy { it.id }
        val sb = StringBuilder()
        val cases = listOf(
            UserSettings(goal = Goal.BUILD_MUSCLE, experience = Experience.BEGINNER, daysPerWeek = 3, sessionMinutes = 60, interests = setOf(Interest.PILATES)),
            UserSettings(goal = Goal.GET_STRONGER, experience = Experience.INTERMEDIATE, daysPerWeek = 4, sessionMinutes = 60),
            UserSettings(goal = Goal.LOSE_FAT, experience = Experience.BEGINNER, daysPerWeek = 3, sessionMinutes = 45, profiles = listOf(EquipmentProfile.home(listOf(5.0, 10.0, 15.0))), interests = setOf(Interest.KICKBOXING)),
        )
        for (s in cases) {
            sb.appendLine("=== ${s.goal} ${s.experience} ${s.daysPerWeek}d ${s.sessionMinutes}min ${s.profile.name}")
            for (d in Generator.generate(s, lib, 1).days) {
                sb.appendLine("  ${d.name} (~${Generator.estimateMinutes(d.slots)} min)")
                for (x in d.slots) sb.appendLine("    ${byId[x.exerciseId]!!.name} | ${x.sets}x${x.repMin}-${x.repMax} ${if (x.seconds > 0) "${x.seconds}s" else ""} rest ${x.restSec} ${x.role} ${x.tracking}")
            }
        }
        File("build/sample-plans.txt").writeText(sb.toString())
    }
}
