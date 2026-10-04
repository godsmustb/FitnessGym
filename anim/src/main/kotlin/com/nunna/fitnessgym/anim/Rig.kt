package com.nunna.fitnessgym.anim

import kotlin.math.sqrt

/** Body proportions in metres for a ~1.80 m figure. Standing: pelvis centre at y = 0.97. */
const val STAND_PELVIS_Y = 0.97
object Dim {
    val HIP = Vec3(0.09, -0.06, 0.0)          // hip joint from pelvis centre (left side)
    const val THIGH = 0.43
    const val SHIN = 0.40
    const val LUMBAR = 0.20                    // pelvis centre -> waist
    const val THORAX = 0.32                    // waist -> neck base
    val SHOULDER = Vec3(0.19, -0.04, 0.0)     // shoulder joint from neck base (left side)
    const val UPPER_ARM = 0.30
    const val FOREARM = 0.26
    const val HAND = 0.08
    const val NECK = 0.08
    const val HEAD_R = 0.105
}

/** A two-bone limb after IK. [front] vectors point at the muscle "front" (biceps, quads). */
data class Limb(
    val root: Vec3, val mid: Vec3, val end: Vec3,
    val upperFront: Vec3, val lowerFront: Vec3,
)

/** All joint positions and frames for one pose. */
class Skeleton(
    val pelvis: Vec3, val pelvisR: Mat3,
    val waist: Vec3, val waistR: Mat3,
    val neckBase: Vec3, val chestR: Mat3,
    val neckTop: Vec3, val headR: Mat3,
    val armL: Limb, val armR: Limb, val legL: Limb, val legR: Limb,
    val handTipL: Vec3, val handTipR: Vec3,
    val footL: Mat3, val footR: Mat3,
) {
    val headCenter get() = neckTop + headR * Vec3(0.0, 0.10, 0.015)
    fun heel(left: Boolean) = (if (left) legL else legR).end + (if (left) footL else footR) * Vec3(0.0, -0.045, -0.04)
    fun toe(left: Boolean) = (if (left) legL else legR).end + (if (left) footL else footR) * Vec3(0.0, -0.045, 0.17)
    fun allPoints(): List<Vec3> = listOf(
        pelvis, waist, neckBase, neckTop, headCenter + Vec3(0.0, Dim.HEAD_R, 0.0), headCenter - Vec3(0.0, Dim.HEAD_R, 0.0),
        armL.mid, armL.end, armR.mid, armR.end, handTipL, handTipR,
        legL.mid, legL.end, legR.mid, legR.end, heel(true), toe(true), heel(false), toe(false),
    )
}

object Rig {
    /** Analytic two-bone IK. The middle joint bends toward [pole]. Unreachable targets clamp. */
    fun twoBone(root: Vec3, target: Vec3, l1: Double, l2: Double, pole: Vec3, armSign: Double): Limb {
        val toT = target - root
        val dist = toT.length().coerceIn(kotlin.math.abs(l1 - l2) + 1e-4, l1 + l2 - 1e-4)
        val u = toT.normalized(Vec3(0.0, -1.0, 0.0))
        var pp = pole - u * (pole dot u)
        if (pp.length() < 1e-6) {
            pp = Vec3.FWD - u * (Vec3.FWD dot u)
            if (pp.length() < 1e-6) pp = Vec3.UP - u * (Vec3.UP dot u)
        }
        val v = pp.normalized()
        val a = (l1 * l1 - l2 * l2 + dist * dist) / (2 * dist)
        val h = sqrt((l1 * l1 - a * a).coerceAtLeast(0.0))
        val mid = root + u * a + v * h
        val end = root + u * dist
        val n = (u cross v).normalized(Vec3.X)                // bend axis
        val upperDir = (mid - root).normalized()
        val lowerDir = (end - mid).normalized()
        // Legs: the knee points at the pole, so the thigh front faces the pole side.
        // Arms: the elbow points at the pole (behind), so the biceps face away from it.
        val upperFront = (n cross upperDir).normalized() * armSign
        val lowerFront = (n cross lowerDir).normalized() * armSign
        return Limb(root, mid, end, upperFront, lowerFront)
    }

    fun solve(p: Pose): Skeleton {
        val pelvisR = Mat3.body(p.pitch, p.roll, p.yaw)
        fun spineR(f: Double) = pelvisR * Mat3.ry(p.spineYaw * f) * Mat3.rz(-p.spineRoll * f) * Mat3.rx(p.spineFlex * f)
        val waistR = spineR(0.5)
        val chestR = spineR(1.0)
        val waist = p.pelvis + waistR * Vec3(0.0, Dim.LUMBAR, 0.0)
        val neckBase = waist + chestR * Vec3(0.0, Dim.THORAX, 0.0)
        val headR = chestR * Mat3.rx(p.neck)
        val neckTop = neckBase + headR * Vec3(0.0, Dim.NECK, 0.0)

        fun frameOf(name: String): Pair<Vec3, Mat3> = when (name) {
            "chest" -> neckBase to chestR
            "pelvis" -> p.pelvis to pelvisR
            else -> Vec3.ZERO to Mat3.I
        }
        val (hO, hR) = frameOf(p.handFrame)
        val (fO, fR) = frameOf(p.footFrame)

        val shL = neckBase + chestR * Dim.SHOULDER
        val shR = neckBase + chestR * Vec3(-Dim.SHOULDER.x, Dim.SHOULDER.y, Dim.SHOULDER.z)
        val armLen = Dim.UPPER_ARM + Dim.FOREARM
        val armL = twoBone(shL, hO + hR * p.lh, Dim.UPPER_ARM, Dim.FOREARM, chestR * p.lep, -1.0)
        val armR = twoBone(shR, hO + hR * p.rh, Dim.UPPER_ARM, Dim.FOREARM, chestR * p.rep, -1.0)

        val hipL = p.pelvis + pelvisR * Dim.HIP
        val hipR = p.pelvis + pelvisR * Vec3(-Dim.HIP.x, Dim.HIP.y, Dim.HIP.z)
        val legL = twoBone(hipL, fO + fR * p.lf, Dim.THIGH, Dim.SHIN, pelvisR * p.lkp, 1.0)
        val legR = twoBone(hipR, fO + fR * p.rf, Dim.THIGH, Dim.SHIN, pelvisR * p.rkp, 1.0)

        // Feet: world-frame feet follow only the body's yaw; pelvis-frame feet follow the pelvis.
        val footBase = if (p.footFrame == "world") Mat3.ry(p.yaw) else pelvisR
        val footL = footBase * Mat3.ry(p.lfy) * Mat3.rx(-p.lfa)
        val footR = footBase * Mat3.ry(-p.rfy) * Mat3.rx(-p.rfa)

        fun tip(l: Limb) = l.end + (l.end - l.mid).normalized() * Dim.HAND
        check(armLen > 0)
        return Skeleton(
            p.pelvis, pelvisR, waist, waistR, neckBase, chestR, neckTop, headR,
            armL, armR, legL, legR, tip(armL), tip(armR), footL, footR,
        )
    }
}
