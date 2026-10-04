package com.nunna.fitnessgym.anim

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/** An elliptical cross-section. [lat] points to the figure's left-ish side, [front] to the muscle front. */
data class Ring(val c: Vec3, val front: Vec3, val lat: Vec3, val rF: Double, val rL: Double)

/** A body part: a chain of rings, drawn as the union of hulls between neighbouring rings. */
class Segment(val id: String, val side: Int, val rings: List<Ring>) {
    /** Interpolated ring at s in [0,1] along the chain. */
    fun ringAt(s: Double): Ring {
        val f = (s.coerceIn(0.0, 1.0)) * (rings.size - 1)
        val i = f.toInt().coerceAtMost(rings.size - 2)
        val u = f - i
        val a = rings[i]; val b = rings[i + 1]
        return Ring(
            Vec3.lerp(a.c, b.c, u), Vec3.lerp(a.front, b.front, u).normalized(a.front),
            Vec3.lerp(a.lat, b.lat, u).normalized(a.lat), a.rF + (b.rF - a.rF) * u, a.rL + (b.rL - a.rL) * u,
        )
    }

    /** Surface point at s and angle theta (degrees from the front, positive toward [out] side). */
    fun surface(s: Double, thetaDeg: Double, out: Int, k: Double = 1.0): Vec3 {
        val r = ringAt(s)
        val t = Mat3.rad(thetaDeg)
        return r.c + r.front * (r.rF * cos(t) * k) + r.lat * (out * r.rL * sin(t) * k)
    }

    fun normal(s: Double, thetaDeg: Double, out: Int): Vec3 {
        val r = ringAt(s)
        val t = Mat3.rad(thetaDeg)
        return (r.front * (cos(t) / r.rF) + r.lat * (out * sin(t) / r.rL)).normalized()
    }
}

/**
 * A muscle drawn as a spindle-shaped patch on a segment surface.
 * [seg] names a segment ("thorax", or "upperArm" which expands to the left and right limbs).
 * [theta] is the centre angle from the front, positive toward the outside of the body.
 */
data class MusclePatch(
    val group: String, val seg: String, val s0: Double, val s1: Double,
    val theta: Double, val width: Double, val shape: Double = 0.6, val crossFibers: Boolean = false,
)

object Anatomy {
    val GROUPS = listOf(
        "abdominals", "obliques", "abductors", "adductors", "biceps", "calves", "chest", "forearms", "glutes",
        "hamstrings", "hip_flexors", "lats", "lower_back", "middle_back", "neck", "quadriceps", "shoulders", "traps", "triceps",
    )

    val LABELS = mapOf(
        "abdominals" to "Abs", "obliques" to "Obliques", "abductors" to "Outer hips", "adductors" to "Inner thighs",
        "biceps" to "Biceps", "calves" to "Calves", "chest" to "Chest", "forearms" to "Forearms", "glutes" to "Glutes",
        "hamstrings" to "Hamstrings", "hip_flexors" to "Hip flexors", "lats" to "Lats", "lower_back" to "Lower back",
        "middle_back" to "Mid back", "neck" to "Neck", "quadriceps" to "Quads", "shoulders" to "Shoulders",
        "traps" to "Traps", "triceps" to "Triceps",
    )

    /** Torso and neck patches are drawn on both sides. Limb patches expand to L and R. */
    val PATCHES = listOf(
        // thorax: rings chest(0) -> shoulder line(0.5) -> trap slope(1)
        MusclePatch("chest", "thorax", 0.0, 0.66, 42.0, 40.0, 0.35),
        MusclePatch("lats", "thorax", 0.0, 0.58, 118.0, 34.0, 0.45),
        MusclePatch("lats", "abdomen", 0.35, 1.0, 112.0, 26.0, 0.5),
        MusclePatch("middle_back", "thorax", 0.04, 0.52, 162.0, 16.0, 0.4),
        MusclePatch("traps", "thorax", 0.34, 1.0, 152.0, 34.0, 0.4),
        MusclePatch("traps", "neck", 0.0, 0.85, 150.0, 30.0, 0.45),
        MusclePatch("neck", "neck", 0.0, 1.0, 34.0, 28.0, 0.6),
        // abdomen: waist(0) -> chest(1)
        MusclePatch("abdominals", "abdomen", 0.0, 0.98, 10.0, 12.0, 0.12, crossFibers = true),
        MusclePatch("obliques", "abdomen", 0.0, 0.88, 58.0, 30.0, 0.4),
        MusclePatch("lower_back", "abdomen", 0.0, 0.92, 166.0, 15.0, 0.3),
        // pelvis: bottom(0) -> hip line(0.5) -> waist(1)
        MusclePatch("abdominals", "pelvis", 0.6, 1.0, 10.0, 11.0, 0.25, crossFibers = true),
        MusclePatch("obliques", "pelvis", 0.62, 1.0, 58.0, 26.0, 0.5),
        MusclePatch("lower_back", "pelvis", 0.6, 1.0, 166.0, 14.0, 0.4),
        MusclePatch("glutes", "pelvis", 0.0, 0.66, 140.0, 44.0, 0.35),
        MusclePatch("abductors", "pelvis", 0.32, 0.82, 96.0, 22.0, 0.5),
        MusclePatch("hip_flexors", "pelvis", 0.12, 0.6, 32.0, 16.0, 0.5),
        // arms
        MusclePatch("shoulders", "upperArm", 0.0, 0.42, 30.0, 46.0, 0.35),
        MusclePatch("shoulders", "upperArm", 0.0, 0.44, 100.0, 44.0, 0.35),
        MusclePatch("shoulders", "upperArm", 0.0, 0.42, 165.0, 42.0, 0.35),
        MusclePatch("biceps", "upperArm", 0.3, 0.95, 0.0, 56.0, 0.45),
        MusclePatch("triceps", "upperArm", 0.22, 0.95, 180.0, 66.0, 0.45),
        MusclePatch("forearms", "forearm", 0.0, 0.78, 10.0, 70.0, 0.4),
        MusclePatch("forearms", "forearm", 0.0, 0.78, 185.0, 64.0, 0.4),
        // legs
        MusclePatch("quadriceps", "thigh", 0.04, 0.96, 6.0, 76.0, 0.35),
        MusclePatch("hamstrings", "thigh", 0.08, 0.94, 180.0, 70.0, 0.4),
        MusclePatch("adductors", "thigh", 0.0, 0.64, -82.0, 40.0, 0.45),
        MusclePatch("abductors", "thigh", 0.0, 0.38, 94.0, 28.0, 0.5),
        MusclePatch("calves", "shin", 0.02, 0.66, 180.0, 82.0, 0.4),
        MusclePatch("_tibialis", "shin", 0.05, 0.85, 22.0, 34.0, 0.4),
    )

    private val LIMB_SEGS = setOf("upperArm", "forearm", "hand", "thigh", "shin", "foot")
    fun isLimb(seg: String) = seg in LIMB_SEGS
}
