package com.nunna.fitnessgym.anim

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.PI
import kotlin.math.cos

/** Shared JSON settings for all content files. */
val ContentJson = Json { ignoreUnknownKeys = true; isLenient = true; allowTrailingComma = true; allowComments = true }

/**
 * One keyframe. Every field except [t] is optional. A missing field carries forward from the
 * previous key. A right-side field that is never given anywhere in the template mirrors the
 * left side, so symmetric moves only need the left limbs. See docs/MOTION_AUTHORING.md.
 */
@Serializable
data class Key(
    val t: Double,
    val e: Double? = null, val el: Double? = null, val er: Double? = null,
    val pelvis: List<Double>? = null,
    val pitch: Double? = null, val roll: Double? = null, val yaw: Double? = null,
    val spine: List<Double>? = null,
    val neck: Double? = null,
    val lh: List<Double>? = null, val rh: List<Double>? = null,
    val lf: List<Double>? = null, val rf: List<Double>? = null,
    val lep: List<Double>? = null, val rep: List<Double>? = null,
    val lkp: List<Double>? = null, val rkp: List<Double>? = null,
    val lfa: Double? = null, val rfa: Double? = null,
    val lfy: Double? = null, val rfy: Double? = null,
)

@Serializable
data class View(val yaw: Double = -35.0, val pitch: Double = 10.0)

/**
 * A scenery or attached prop.
 * type = mat | box | cable | bar | disc
 *  - box: [min]..[max] world corners; color = frame | pad | steel | dark | accent
 *  - cable: from [from] (world point) to [to] (lh | rh | hands | lf | rf)
 *  - bar: a straight handle between both hands
 *  - disc: a flat round plate (flywheel, foot plate) at [at], facing along [axis], [radius]
 */
@Serializable
data class Prop(
    val type: String,
    val min: List<Double>? = null, val max: List<Double>? = null,
    val color: String? = null,
    val from: List<Double>? = null, val to: String? = null,
    val at: List<Double>? = null, val axis: List<Double>? = null, val radius: Double? = null,
)

@Serializable
data class MotionTemplate(
    val id: String,
    val name: String,
    val duration: Double = 3.0,
    val handFrame: String = "chest",
    val footFrame: String = "world",
    val view: View = View(),
    val props: List<Prop> = emptyList(),
    val keys: List<Key>,
    val notes: String? = null,
)

/** Every value resolved for one moment in the cycle. */
data class Pose(
    val pelvis: Vec3, val pitch: Double, val roll: Double, val yaw: Double,
    val spineFlex: Double, val spineRoll: Double, val spineYaw: Double, val neck: Double,
    val lh: Vec3, val rh: Vec3, val lf: Vec3, val rf: Vec3,
    val lep: Vec3, val rep: Vec3, val lkp: Vec3, val rkp: Vec3,
    val lfa: Double, val rfa: Double, val lfy: Double, val rfy: Double,
    val effortL: Double, val effortR: Double,
    val handFrame: String, val footFrame: String,
)

private fun mirror(v: List<Double>) = listOf(-v[0], v[1], v[2])

/** A template with every key filled in, ready to sample quickly every frame. */
class Motion(val template: MotionTemplate) {
    val keys: List<Key>

    init {
        require(template.keys.isNotEmpty()) { "motion ${template.id}: no keys" }
        require(template.handFrame in FRAMES) { "motion ${template.id}: handFrame ${template.handFrame}" }
        require(template.footFrame in FRAMES) { "motion ${template.id}: footFrame ${template.footFrame}" }
        val k = template.keys.sortedBy { it.t }
        require(k.first().t >= 0.0 && k.last().t < 1.0) { "motion ${template.id}: key t must be in [0,1)" }
        val any = { f: (Key) -> Any? -> k.any { f(it) != null } }
        val mirRh = !any { it.rh }; val mirRf = !any { it.rf }
        val mirRep = !any { it.rep }; val mirRkp = !any { it.rkp }
        val mirRfa = !any { it.rfa }; val mirRfy = !any { it.rfy }

        val defHand = if (template.handFrame == "chest") listOf(0.20, -0.56, 0.02) else listOf(0.20, 0.9, 0.02)
        val defFoot = if (template.footFrame == "world") listOf(0.10, 0.08, 0.0) else listOf(0.10, -0.89, 0.0)
        var prev = Key(
            t = 0.0, e = 0.0, pelvis = listOf(0.0, STAND_PELVIS_Y, 0.0), pitch = 0.0, roll = 0.0, yaw = 0.0,
            spine = listOf(0.0, 0.0, 0.0), neck = 0.0,
            lh = defHand, rh = mirror(defHand), lf = defFoot, rf = mirror(defFoot),
            lep = listOf(0.35, -0.2, -1.0), rep = listOf(-0.35, -0.2, -1.0),
            lkp = listOf(0.15, 0.0, 1.0), rkp = listOf(-0.15, 0.0, 1.0),
            lfa = 0.0, rfa = 0.0, lfy = 8.0, rfy = 8.0,
        )
        keys = k.map { c ->
            val lh = c.lh ?: prev.lh; val lf = c.lf ?: prev.lf
            val lep = c.lep ?: prev.lep; val lkp = c.lkp ?: prev.lkp
            val lfa = c.lfa ?: prev.lfa; val lfy = c.lfy ?: prev.lfy
            val e = c.e ?: prev.e
            val full = Key(
                t = c.t, e = e,
                el = c.el ?: (if (c.e != null) null else prev.el),
                er = c.er ?: (if (c.e != null) null else prev.er),
                pelvis = c.pelvis ?: prev.pelvis, pitch = c.pitch ?: prev.pitch,
                roll = c.roll ?: prev.roll, yaw = c.yaw ?: prev.yaw,
                spine = c.spine ?: prev.spine, neck = c.neck ?: prev.neck,
                lh = lh, rh = if (mirRh) mirror(lh!!) else c.rh ?: prev.rh,
                lf = lf, rf = if (mirRf) mirror(lf!!) else c.rf ?: prev.rf,
                lep = lep, rep = if (mirRep) mirror(lep!!) else c.rep ?: prev.rep,
                lkp = lkp, rkp = if (mirRkp) mirror(lkp!!) else c.rkp ?: prev.rkp,
                lfa = lfa, rfa = if (mirRfa) lfa else c.rfa ?: prev.rfa,
                lfy = lfy, rfy = if (mirRfy) lfy else c.rfy ?: prev.rfy,
            )
            for (v in listOf(full.pelvis, full.spine, full.lh, full.rh, full.lf, full.rf, full.lep, full.rep, full.lkp, full.rkp)) {
                require(v!!.size == 3) { "motion ${template.id}: vectors need 3 numbers (key t=${c.t})" }
            }
            prev = full
            full
        }
    }

    /** Pose at cycle phase [phase] in [0,1). Keys loop: the last key blends back into the first. */
    fun sample(phase: Double): Pose {
        val p = ((phase % 1.0) + 1.0) % 1.0
        val n = keys.size
        var i = n - 1
        for (j in 0 until n) if (keys[j].t <= p) i = j
        val a = keys[i]
        val b = keys[(i + 1) % n]
        val ta = a.t
        var tb = b.t
        if (tb <= ta) tb += 1.0
        var pp = p
        if (pp < ta) pp += 1.0
        val u = if (tb - ta < 1e-9) 0.0 else ((pp - ta) / (tb - ta)).coerceIn(0.0, 1.0)
        val s = 0.5 - 0.5 * cos(PI * u) // ease in-out
        fun d(x: Double?, y: Double?) = x!! + (y!! - x) * s
        fun v(x: List<Double>?, y: List<Double>?) = Vec3.lerp(Vec3.of(x!!), Vec3.of(y!!), s)
        val eA = a.e!!; val eB = b.e!!
        return Pose(
            pelvis = v(a.pelvis, b.pelvis), pitch = d(a.pitch, b.pitch), roll = d(a.roll, b.roll), yaw = d(a.yaw, b.yaw),
            spineFlex = d(a.spine!![0], b.spine!![0]), spineRoll = d(a.spine[1], b.spine[1]), spineYaw = d(a.spine[2], b.spine[2]),
            neck = d(a.neck, b.neck),
            lh = v(a.lh, b.lh), rh = v(a.rh, b.rh), lf = v(a.lf, b.lf), rf = v(a.rf, b.rf),
            lep = v(a.lep, b.lep), rep = v(a.rep, b.rep), lkp = v(a.lkp, b.lkp), rkp = v(a.rkp, b.rkp),
            lfa = d(a.lfa, b.lfa), rfa = d(a.rfa, b.rfa), lfy = d(a.lfy, b.lfy), rfy = d(a.rfy, b.rfy),
            effortL = d(a.el ?: eA, b.el ?: eB), effortR = d(a.er ?: eA, b.er ?: eB),
            handFrame = template.handFrame, footFrame = template.footFrame,
        )
    }

    companion object {
        val FRAMES = setOf("world", "chest", "pelvis")
        fun parse(json: String) = Motion(ContentJson.decodeFromString(MotionTemplate.serializer(), json))
    }
}
