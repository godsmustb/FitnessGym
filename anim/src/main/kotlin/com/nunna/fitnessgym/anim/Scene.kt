package com.nunna.fitnessgym.anim

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** One flat 2D shape in screen pixels. Colours are ARGB; 0 means "none". [tag] = muscle group for tap hit-tests. */
class Prim(val xy: FloatArray, val fill: Int, val stroke: Int = 0, val strokeW: Float = 0f, val closed: Boolean = true, val tag: String? = null)

object Palette {
    const val BG = 0xFF10131A.toInt()
    const val FLOOR = 0xFF1A1F29.toInt()
    const val FLOOR_LINE = 0xFF2A3140.toInt()
    const val MAT = 0xFF2C6E6A.toInt()
    const val BODY = 0xFF353C4B.toInt()
    const val BODY_DARK = 0xFF3A4252.toInt()
    const val OUTLINE = 0xFF1A1E27.toInt()
    const val FACE = 0xFF8E97AA.toInt()
    const val MUSCLE = 0xFF8A4E55.toInt()
    const val FIBER = 0xFFA86A73.toInt()
    const val PRIME = 0xFFFF4528.toInt()
    const val PRIME_FIBER = 0xFFFFD2B8.toInt()
    const val SYNERGIST = 0xFFFFB020.toInt()
    const val SYN_FIBER = 0xFFFFE7B0.toInt()
    const val STEEL = 0xFFA7B0C2.toInt()
    const val DARK = 0xFF232833.toInt()
    const val FRAME = 0xFF566178.toInt()
    const val PAD = 0xFF2E3442.toInt()
    const val ACCENT = 0xFF3FA7D6.toInt()
    const val GLOVE = 0xFFD7263D.toInt()
    const val CABLE = 0xFFD0D5DF.toInt()

    fun named(n: String?) = when (n) {
        "pad" -> PAD; "steel" -> STEEL; "dark" -> DARK; "accent" -> ACCENT; "mat" -> MAT; else -> FRAME
    }

    fun lerp(a: Int, b: Int, t: Double): Int {
        val u = t.coerceIn(0.0, 1.0)
        fun ch(sh: Int) = (((a ushr sh) and 0xFF) + (((b ushr sh) and 0xFF) - ((a ushr sh) and 0xFF)) * u).toInt() and 0xFF
        return (ch(24) shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    fun alpha(c: Int, a: Double) = (((a.coerceIn(0.0, 1.0) * 255).toInt()) shl 24) or (c and 0xFFFFFF)
    fun shade(c: Int, f: Double) = lerp(c, if (f < 1) 0xFF000000.toInt() else 0xFFFFFFFF.toInt(), abs1(f - 1))
    private fun abs1(x: Double) = if (x < 0) -x else x
}

/** Which muscles glow, and how strongly. */
data class Activation(val primary: Set<String>, val secondary: Set<String>, val implement: String = "none", val unilateral: Boolean = false) {
    companion object { val NONE = Activation(emptySet(), emptySet()) }
}

data class Camera(val yaw: Double, val pitch: Double) {
    private val r = Mat3.rx(pitch) * Mat3.ry(-yaw)
    fun view(p: Vec3) = r * p
    /** World-space direction from the figure toward the viewer. */
    val toViewer: Vec3 = run {
        // inverse rotation = transpose
        val m = r.m
        Vec3(m[6], m[7], m[8])
    }
}

/**
 * Builds the 2D drawing for one frame. Create once per (motion, exercise); call [frame] every tick.
 * The fit (scale/centre) is computed over the whole cycle so the figure never jumps.
 */
class FigureScene(val motion: Motion, val act: Activation = Activation.NONE) {
    private val fits = HashMap<Pair<Double, Double>, DoubleArray>()
    private val workingLeft: Boolean
    private val workingRight: Boolean

    init {
        var l = 0.0; var r = 0.0
        for (i in 0 until 24) { val p = motion.sample(i / 24.0); l += p.effortL; r += p.effortR }
        // One-sided moves with equal effort on both sides work the RIGHT side (nearest the default camera).
        workingLeft = !act.unilateral || l > r + 1e-6
        workingRight = !act.unilateral || !workingLeft
    }

    fun defaultCamera() = Camera(motion.template.view.yaw, motion.template.view.pitch)

    /** Bounds in view space over the cycle: minX, maxX, minY, maxY. */
    private fun fit(cam: Camera): DoubleArray = fits.getOrPut(cam.yaw to cam.pitch) {
        var x0 = Double.MAX_VALUE; var x1 = -Double.MAX_VALUE; var y0 = Double.MAX_VALUE; var y1 = -Double.MAX_VALUE
        fun add(p: Vec3, pad: Double) {
            val q = cam.view(p)
            x0 = min(x0, q.x - pad); x1 = max(x1, q.x + pad); y0 = min(y0, q.y - pad); y1 = max(y1, q.y + pad)
        }
        for (i in 0 until 32) {
            val sk = Rig.solve(motion.sample(i / 32.0))
            for (p in sk.allPoints()) add(p, 0.11)
        }
        for (pr in motion.template.props) when (pr.type) {
            "box" -> { add(Vec3.of(pr.min!!), 0.0); add(Vec3.of(pr.max!!), 0.0) }
            "disc" -> add(Vec3.of(pr.at!!), pr.radius ?: 0.2)
        }
        add(Vec3(0.0, 0.0, 0.0), 0.05)
        doubleArrayOf(x0, x1, y0, y1)
    }

    fun frame(phase: Double, width: Float, height: Float, cam: Camera = defaultCamera()): List<Prim> {
        val pose = motion.sample(phase)
        val sk = Rig.solve(pose)
        val b = fit(cam)
        val scale = min(width / (b[1] - b[0]), height / (b[3] - b[2])) * 0.9
        val cx = (b[0] + b[1]) / 2; val cy = (b[2] + b[3]) / 2
        val px = { p: Vec3 ->
            val q = cam.view(p)
            floatArrayOf((width / 2 + (q.x - cx) * scale).toFloat(), (height / 2 - (q.y - cy) * scale).toFloat())
        }
        val out = ArrayList<Prim>()
        val w = scale.toFloat()

        // Floor, then scenery props that sit behind the figure.
        floor(out, px, cam)
        val sorted = ArrayList<Pair<Double, () -> Unit>>()
        for (pr in motion.template.props) {
            when (pr.type) {
                "mat" -> box(out, px, cam, Vec3.of(pr.min ?: listOf(-0.33, -0.005, -1.0)), Vec3.of(pr.max ?: listOf(0.33, 0.008, 1.0)), Palette.MAT)
                "box" -> {
                    val lo = Vec3.of(pr.min!!); val hi = Vec3.of(pr.max!!)
                    if (pr.color == "front") sorted += cam.view((lo + hi) * 0.5).z to { box(out, px, cam, lo, hi, Palette.PAD) }
                    else box(out, px, cam, lo, hi, Palette.named(pr.color))
                }
                "disc" -> disc(out, px, Vec3.of(pr.at!!), Vec3.of(pr.axis ?: listOf(1.0, 0.0, 0.0)), pr.radius ?: 0.2, Palette.named(pr.color))
            }
        }

        // Body segments, painter-sorted far -> near.
        for (seg in segments(sk)) {
            val depth = seg.rings.map { cam.view(it.c).z }.average()
            sorted += depth to { drawSegment(out, seg, px, cam, pose, w) }
        }
        // Attached props (dumbbells, gloves, cables, handles).
        attachments(out, sk, pose, phase, cam, px, w, sorted)

        sorted.sortBy { it.first }
        for ((_, draw) in sorted) draw()
        return out
    }

    // ---------- body ----------

    private fun limbRings(a: Vec3, b: Vec3, front: Vec3, rA: Double, rB: Double, extraMid: Double? = null): List<Ring> {
        val dir = (b - a).normalized()
        val lat = (front cross dir).normalized(Vec3.X)
        val f = (dir cross lat).normalized(front) * -1.0
        val fr = if ((f dot front) < 0) f * -1.0 else f
        val rs = ArrayList<Ring>()
        rs += Ring(a, fr, lat, rA, rA)
        if (extraMid != null) rs += Ring(Vec3.lerp(a, b, 0.3), fr, lat, extraMid, extraMid)
        rs += Ring(b, fr, lat, rB, rB)
        return rs
    }

    fun segments(sk: Skeleton): List<Segment> {
        val pR = sk.pelvisR; val wR = sk.waistR; val cR = sk.chestR; val hR = sk.headR
        fun ring(c: Vec3, r: Mat3, rx: Double, rz: Double) = Ring(c, r.zAxis, r.xAxis, rz, rx)
        val segs = ArrayList<Segment>()
        val chestC = sk.waist + cR * Vec3(0.0, 0.17, 0.005)
        segs += Segment("pelvis", 0, listOf(
            ring(sk.pelvis + pR * Vec3(0.0, -0.13, -0.01), pR, 0.13, 0.095),
            ring(sk.pelvis + pR * Vec3(0.0, -0.02, 0.0), pR, 0.165, 0.112),
            ring(sk.waist, wR, 0.135, 0.095),
        ))
        segs += Segment("abdomen", 0, listOf(ring(sk.waist, wR, 0.135, 0.095), ring(chestC, cR, 0.16, 0.115)))
        segs += Segment("thorax", 0, listOf(
            ring(chestC, cR, 0.16, 0.115),
            ring(sk.neckBase + cR * Vec3(0.0, -0.045, -0.01), cR, 0.175, 0.085),
            ring(sk.neckBase + cR * Vec3(0.0, 0.015, -0.012), cR, 0.075, 0.055),
        ))
        segs += Segment("neck", 0, listOf(ring(sk.neckBase, hR, 0.05, 0.05), ring(sk.neckTop, hR, 0.048, 0.048)))
        val hc = sk.headCenter
        // Head: an ellipsoid sampled as rings (rx 0.092, ry 0.118, rz 0.104).
        segs += Segment("head", 0, listOf(-72.0, -45.0, -15.0, 15.0, 45.0, 72.0).map { a ->
            val ca = kotlin.math.cos(Mat3.rad(a)); val sa = kotlin.math.sin(Mat3.rad(a))
            ring(hc + hR * Vec3(0.0, 0.118 * sa, 0.0), hR, 0.092 * ca, 0.104 * ca)
        })
        for ((side, arm) in listOf(1 to sk.armL, -1 to sk.armR)) {
            val dirU = (arm.mid - arm.root).normalized()
            segs += Segment("upperArm", side, limbRings(arm.root - dirU * 0.035, arm.mid, arm.upperFront, 0.058, 0.04))
            segs += Segment("forearm", side, limbRings(arm.mid, arm.end, arm.lowerFront, 0.043, 0.028))
            val tip = if (side == 1) sk.handTipL else sk.handTipR
            segs += Segment("hand", side, limbRings(arm.end, tip, arm.lowerFront, 0.03, 0.024))
        }
        for ((side, leg) in listOf(1 to sk.legL, -1 to sk.legR)) {
            segs += Segment("thigh", side, limbRings(leg.root - (leg.mid - leg.root).normalized() * 0.04, leg.mid, leg.upperFront, 0.09, 0.056))
            segs += Segment("shin", side, limbRings(leg.mid, leg.end, leg.lowerFront, 0.054, 0.034, extraMid = 0.06))
            val fm = if (side == 1) sk.footL else sk.footR
            val heel = sk.heel(side == 1); val toe = sk.toe(side == 1)
            segs += Segment("foot", side, listOf(
                Ring(heel, fm.yAxis, fm.xAxis, 0.036, 0.036),
                Ring(toe, fm.yAxis, fm.xAxis, 0.022, 0.03),
            ))
        }
        return segs
    }

    private fun roleOf(group: String) = when (group) {
        in act.primary -> 2
        in act.secondary -> 1
        else -> 0
    }

    private fun drawSegment(out: MutableList<Prim>, seg: Segment, px: (Vec3) -> FloatArray, cam: Camera, pose: Pose, w: Float) {
        // Outline pass then fill pass, so the seams between hull pieces disappear.
        val pieces = (0 until seg.rings.size - 1).map { i ->
            hull2d(ringPoints(seg.rings[i]).map(px) + ringPoints(seg.rings[i + 1]).map(px))
        }
        val base = if (seg.side == -1 || seg.id == "head") Palette.BODY else Palette.BODY
        for (p in pieces) out += Prim(p, 0, Palette.OUTLINE, w * 0.012f)
        for (p in pieces) out += Prim(p, base)

        if (seg.id == "head") {
            face(out, seg, px, cam)
            return
        }
        // Muscles on this segment, drawn if they face the viewer.
        val patches = Anatomy.PATCHES.filter { it.seg == seg.id }
        val sides = if (seg.side == 0) listOf(1, -1) else listOf(seg.side)
        val vis = ArrayList<Pair<Double, () -> Unit>>()
        for (mp in patches) for (side in sides) {
            val sMid = (mp.s0 + mp.s1) / 2
            val facing = seg.normal(sMid, mp.theta, side) dot cam.toViewer
            if (facing < 0.05) continue
            vis += facing to { patch(out, seg, mp, side, px, pose, facing, w) }
        }
        vis.sortBy { it.first }
        for ((_, d) in vis) d()
    }

    private fun patch(out: MutableList<Prim>, seg: Segment, mp: MusclePatch, side: Int, px: (Vec3) -> FloatArray, pose: Pose, facing: Double, w: Float) {
        val role = roleOf(mp.group)
        val sideWorks = if (side == 1) workingLeft else workingRight
        val effort = if (side == 1) pose.effortL else pose.effortR
        val inten = when {
            role == 0 || !sideWorks -> 0.0
            role == 2 -> 0.35 + 0.65 * effort
            else -> 0.22 + 0.45 * effort
        }
        val n = 9
        val k = { u: Double -> 1.0 + 0.07 * sin(PI * u) * (1.0 + 0.6 * inten) }
        val halfW = { u: Double -> mp.width * sin(PI * u).coerceAtLeast(0.0).pow(mp.shape) }
        val sAt = { u: Double -> mp.s0 + (mp.s1 - mp.s0) * u }
        val left = (0..n).map { i -> val u = i / n.toDouble(); seg.surface(sAt(u), mp.theta - halfW(u), side, k(u)) }
        val right = (0..n).map { i -> val u = 1 - i / n.toDouble(); seg.surface(sAt(u), mp.theta + halfW(u), side, k(u)) }
        val poly = flat((left + right).map(px))

        val fade = ((facing - 0.05) / 0.3).coerceIn(0.0, 1.0)
        val active = if (role == 2) Palette.PRIME else Palette.SYNERGIST
        val fill = Palette.lerp(Palette.MUSCLE, active, inten)
        val fillA = Palette.alpha(fill, 0.35 + 0.65 * fade)
        if (inten > 0.3) out += Prim(poly, 0, Palette.alpha(active, 0.28 * inten * fade), w * (0.010f + 0.018f * inten.toFloat()), tag = mp.group)
        out += Prim(poly, fillA, Palette.alpha(Palette.OUTLINE, 0.55 * fade), w * 0.004f, tag = mp.group)

        // Fibres: lighter lines along the muscle (or across it, for the abs).
        val fib = Palette.alpha(Palette.lerp(Palette.FIBER, if (role == 2) Palette.PRIME_FIBER else Palette.SYN_FIBER, inten), 0.55 * fade)
        if (mp.crossFibers) {
            for (u in listOf(0.22, 0.45, 0.68)) {
                val a = seg.surface(sAt(u), mp.theta - halfW(u) * 0.9, side, k(u))
                val c = seg.surface(sAt(u), mp.theta + halfW(u) * 0.9, side, k(u))
                out += Prim(flat(listOf(px(a), px(c))), 0, fib, w * 0.004f, closed = false)
            }
        } else {
            for (f in listOf(-0.45, 0.0, 0.45)) {
                val line = (1 until n).map { i -> val u = i / n.toDouble(); px(seg.surface(sAt(u), mp.theta + halfW(u) * f, side, k(u))) }
                out += Prim(flat(line), 0, fib, w * 0.0035f, closed = false)
            }
        }
    }

    private fun face(out: MutableList<Prim>, seg: Segment, px: (Vec3) -> FloatArray, cam: Camera) {
        val facing = seg.normal(0.5, 0.0, 1) dot cam.toViewer
        if (facing < -0.35) return
        val pts = (0 until 16).map { i ->
            val a = 2 * PI * i / 16
            seg.surface(0.52 + 0.2 * sin(a), 40.0 * cos(a), 1, 1.0)
        }
        // Clip the visor to the half of the head facing us, so it shows as a profile in side view.
        val vis = pts.filter { p -> ((p - seg.ringAt(0.5).c).normalized() dot cam.toViewer) > -0.05 }
        if (vis.size < 3) return
        out += Prim(hull2d(vis.map(px)), Palette.alpha(Palette.FACE, 0.55 + 0.4 * ((facing + 0.35) / 0.8).coerceIn(0.0, 1.0)))
    }

    private fun ringPoints(r: Ring, n: Int = 20) = (0 until n).map { i ->
        val a = 2 * PI * i / n
        r.c + r.front * (r.rF * cos(a)) + r.lat * (r.rL * sin(a))
    }

    // ---------- props ----------

    private fun floor(out: MutableList<Prim>, px: (Vec3) -> FloatArray, cam: Camera) {
        val c = listOf(Vec3(-1.4, 0.0, -1.4), Vec3(1.4, 0.0, -1.4), Vec3(1.4, 0.0, 1.4), Vec3(-1.4, 0.0, 1.4))
        out += Prim(flat(c.map(px)), Palette.FLOOR, Palette.FLOOR_LINE, 1.5f)
    }

    private fun box(out: MutableList<Prim>, px: (Vec3) -> FloatArray, cam: Camera, lo: Vec3, hi: Vec3, color: Int) {
        val v = { x: Double, y: Double, z: Double -> Vec3(x, y, z) }
        val faces = listOf(
            Vec3(0.0, 1.0, 0.0) to listOf(v(lo.x, hi.y, lo.z), v(hi.x, hi.y, lo.z), v(hi.x, hi.y, hi.z), v(lo.x, hi.y, hi.z)),
            Vec3(0.0, -1.0, 0.0) to listOf(v(lo.x, lo.y, lo.z), v(hi.x, lo.y, lo.z), v(hi.x, lo.y, hi.z), v(lo.x, lo.y, hi.z)),
            Vec3(1.0, 0.0, 0.0) to listOf(v(hi.x, lo.y, lo.z), v(hi.x, hi.y, lo.z), v(hi.x, hi.y, hi.z), v(hi.x, lo.y, hi.z)),
            Vec3(-1.0, 0.0, 0.0) to listOf(v(lo.x, lo.y, lo.z), v(lo.x, hi.y, lo.z), v(lo.x, hi.y, hi.z), v(lo.x, lo.y, hi.z)),
            Vec3(0.0, 0.0, 1.0) to listOf(v(lo.x, lo.y, hi.z), v(hi.x, lo.y, hi.z), v(hi.x, hi.y, hi.z), v(lo.x, hi.y, hi.z)),
            Vec3(0.0, 0.0, -1.0) to listOf(v(lo.x, lo.y, lo.z), v(hi.x, lo.y, lo.z), v(hi.x, hi.y, lo.z), v(lo.x, hi.y, lo.z)),
        )
        for ((n, q) in faces) {
            if ((n dot cam.toViewer) <= 0.0) continue
            val shade = if (n.y > 0) 1.18 else if (n.y < 0) 0.7 else 0.88 + 0.1 * n.x
            out += Prim(flat(q.map(px)), Palette.shade(color, shade), Palette.OUTLINE, 1.2f)
        }
    }

    private fun disc(out: MutableList<Prim>, px: (Vec3) -> FloatArray, c: Vec3, axis: Vec3, r: Double, color: Int) {
        val a = axis.normalized()
        val u = (if (kotlin.math.abs(a.y) < 0.9) Vec3.UP else Vec3.X).let { (it cross a).normalized() }
        val v = (a cross u).normalized()
        val pts = (0 until 20).map { i -> val t = 2 * PI * i / 20; c + u * (r * cos(t)) + v * (r * sin(t)) }
        out += Prim(flat(pts.map(px)), color, Palette.OUTLINE, 1.2f)
    }

    private fun attachments(
        out: MutableList<Prim>, sk: Skeleton, pose: Pose, phase: Double, cam: Camera, px: (Vec3) -> FloatArray, w: Float,
        sorted: MutableList<Pair<Double, () -> Unit>>,
    ) {
        val t = motion.template
        // Cables from the template (machines) are always drawn.
        for (pr in t.props) when (pr.type) {
            "cable" -> {
                val from = Vec3.of(pr.from!!)
                val ends = when (pr.to) {
                    "lh" -> listOf(sk.armL.end); "rh" -> listOf(sk.armR.end)
                    "lf" -> listOf(sk.legL.end); "rf" -> listOf(sk.legR.end)
                    "handsMid" -> listOf((sk.armL.end + sk.armR.end) * 0.5)
                    else -> listOf(sk.armL.end, sk.armR.end)
                }
                for (e in ends) sorted += cam.view((from + e) * 0.5).z - 0.3 to {
                    out += Prim(flat(listOf(px(from), px(e))), 0, Palette.CABLE, w * 0.007f, closed = false)
                }
            }
            "bar" -> {
                // A straight bar or pad between both hands / ankles / knees (e.g. leg extension pad).
                val (a, b) = when (pr.to) {
                    "feet" -> (sk.legL.end + sk.legL.lowerFront * 0.06) to (sk.legR.end + sk.legR.lowerFront * 0.06)
                    "heels" -> (sk.legL.end - sk.legL.lowerFront * 0.06) to (sk.legR.end - sk.legR.lowerFront * 0.06)
                    "knees" -> sk.legL.mid to sk.legR.mid
                    else -> sk.armL.end to sk.armR.end
                }
                val ext = (a - b).normalized() * 0.08
                val col = if (pr.color == null) Palette.STEEL else Palette.named(pr.color)
                val thick = ((pr.radius ?: 0.009) * 2).toFloat()
                sorted += cam.view((a + b) * 0.5).z + 0.02 to {
                    out += Prim(flat(listOf(px(a + ext), px(b - ext))), 0, col, w * thick, closed = false)
                }
            }
        }
        val hands = buildList {
            if (workingLeft) add(Triple(sk.armL, sk.handTipL, 1))
            if (workingRight) add(Triple(sk.armR, sk.handTipR, -1))
        }
        when (act.implement) {
            "dumbbell" -> for ((arm, tip, _) in hands) {
                val grip = Vec3.lerp(arm.end, tip, 0.45)
                val axis = (arm.lowerFront cross (arm.end - arm.mid)).normalized(Vec3.X)
                sorted += cam.view(grip).z + 0.04 to {
                    val a = grip + axis * 0.1; val b = grip - axis * 0.1
                    out += Prim(flat(listOf(px(a), px(b))), 0, Palette.STEEL, w * 0.022f, closed = false)
                    for (end in listOf(a, b)) {
                        val pts = ringPoints(Ring(end, (arm.end - arm.mid).normalized(), (axis cross (arm.end - arm.mid)).normalized(), 0.055, 0.055), 14)
                        val e2 = end + axis * (if (end == a) 0.035 else -0.035)
                        val pts2 = ringPoints(Ring(e2, (arm.end - arm.mid).normalized(), (axis cross (arm.end - arm.mid)).normalized(), 0.055, 0.055), 14)
                        out += Prim(hull2d((pts + pts2).map(px)), Palette.STEEL, Palette.OUTLINE, w * 0.006f)
                    }
                }
            }
            "gloves" -> for ((arm, tip, _) in hands) {
                val c = Vec3.lerp(arm.end, tip, 0.5)
                sorted += cam.view(c).z + 0.05 to {
                    val r = Ring(c, arm.lowerFront, (arm.lowerFront cross (tip - arm.end)).normalized(), 0.06, 0.055)
                    val r2 = Ring(arm.end - (tip - arm.end).normalized() * 0.03, arm.lowerFront, r.lat, 0.045, 0.045)
                    out += Prim(hull2d((ringPoints(r) + ringPoints(r2)).map(px)), Palette.GLOVE, Palette.OUTLINE, w * 0.008f)
                }
            }
            "cable", "handle" -> {
                if (act.implement == "cable" && t.props.none { it.type == "cable" }) {
                    val anchor = Vec3(0.0, 0.15, 0.75)
                    for ((arm, _, _) in hands) sorted += cam.view((anchor + arm.end) * 0.5).z - 0.3 to {
                        out += Prim(flat(listOf(px(anchor), px(arm.end))), 0, Palette.CABLE, w * 0.007f, closed = false)
                    }
                }
                for ((arm, tip, _) in hands) {
                    val g = Vec3.lerp(arm.end, tip, 0.45)
                    val ax = (arm.lowerFront cross (arm.end - arm.mid)).normalized(Vec3.X)
                    sorted += cam.view(g).z + 0.03 to {
                        out += Prim(flat(listOf(px(g + ax * 0.06), px(g - ax * 0.06))), 0, Palette.STEEL, w * 0.016f, closed = false)
                    }
                }
            }
            "rope" -> {
                val mid = (sk.armL.end + sk.armR.end) * 0.5
                val ang = 2 * PI * phase * 2 // two turns per cycle
                val sweep = mid + Vec3(0.0, -0.55 * cos(ang) - 0.35, 0.55 * sin(ang))
                sorted += cam.view(sweep).z to {
                    val pts = ArrayList<FloatArray>()
                    for (i in 0..16) {
                        val u = i / 16.0
                        val a = Vec3.lerp(sk.armL.end, sk.armR.end, u)
                        val bulge = sin(PI * u)
                        pts += px(a + (sweep - mid) * bulge)
                    }
                    out += Prim(flat(pts), 0, Palette.ACCENT, w * 0.008f, closed = false)
                }
            }
        }
    }

    companion object {
        fun flat(pts: List<FloatArray>): FloatArray {
            val r = FloatArray(pts.size * 2)
            for ((i, p) in pts.withIndex()) { r[i * 2] = p[0]; r[i * 2 + 1] = p[1] }
            return r
        }

        /** Andrew's monotone chain convex hull. */
        fun hull2d(pts: List<FloatArray>): FloatArray {
            val p = pts.sortedWith(compareBy({ it[0] }, { it[1] }))
            if (p.size < 3) return flat(p)
            fun cross(o: FloatArray, a: FloatArray, b: FloatArray) = (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0])
            val lower = ArrayList<FloatArray>()
            for (q in p) { while (lower.size >= 2 && cross(lower[lower.size - 2], lower.last(), q) <= 0) lower.removeAt(lower.size - 1); lower += q }
            val upper = ArrayList<FloatArray>()
            for (q in p.asReversed()) { while (upper.size >= 2 && cross(upper[upper.size - 2], upper.last(), q) <= 0) upper.removeAt(upper.size - 1); upper += q }
            lower.removeAt(lower.size - 1); upper.removeAt(upper.size - 1)
            return flat(lower + upper)
        }
    }
}
