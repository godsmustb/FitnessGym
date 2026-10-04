package com.nunna.fitnessgym.anim

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** World: y up, the figure faces +z, the figure's LEFT is +x. Units are metres. */
data class Vec3(val x: Double, val y: Double, val z: Double) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Double) = Vec3(x * s, y * s, z * s)
    operator fun unaryMinus() = Vec3(-x, -y, -z)
    infix fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    infix fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun length() = sqrt(this dot this)
    fun normalized(fallback: Vec3 = UP): Vec3 {
        val l = length()
        return if (l < 1e-9) fallback else this * (1.0 / l)
    }
    fun isFinite() = x.isFinite() && y.isFinite() && z.isFinite()

    companion object {
        val ZERO = Vec3(0.0, 0.0, 0.0)
        val X = Vec3(1.0, 0.0, 0.0)
        val UP = Vec3(0.0, 1.0, 0.0)
        val FWD = Vec3(0.0, 0.0, 1.0)
        fun of(a: List<Double>) = Vec3(a[0], a[1], a[2])
        fun lerp(a: Vec3, b: Vec3, t: Double) = a + (b - a) * t
    }
}

/** Row-major 3x3 rotation matrix. Columns are the local x (left), y (up), z (front) axes. */
class Mat3(val m: DoubleArray) {
    operator fun times(v: Vec3) = Vec3(
        m[0] * v.x + m[1] * v.y + m[2] * v.z,
        m[3] * v.x + m[4] * v.y + m[5] * v.z,
        m[6] * v.x + m[7] * v.y + m[8] * v.z,
    )

    operator fun times(o: Mat3): Mat3 {
        val r = DoubleArray(9)
        for (i in 0..2) for (j in 0..2) {
            r[i * 3 + j] = m[i * 3] * o.m[j] + m[i * 3 + 1] * o.m[3 + j] + m[i * 3 + 2] * o.m[6 + j]
        }
        return Mat3(r)
    }

    val xAxis get() = Vec3(m[0], m[3], m[6])
    val yAxis get() = Vec3(m[1], m[4], m[7])
    val zAxis get() = Vec3(m[2], m[5], m[8])

    companion object {
        val I = Mat3(doubleArrayOf(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0))
        fun rad(deg: Double) = deg * PI / 180.0

        /** Rotation about x. Positive tips +y toward +z (an upright torso leans forward). */
        fun rx(deg: Double): Mat3 {
            val c = cos(rad(deg)); val s = sin(rad(deg))
            return Mat3(doubleArrayOf(1.0, 0.0, 0.0, 0.0, c, -s, 0.0, s, c))
        }

        /** Rotation about y. Positive turns +z toward +x (the figure turns to its left). */
        fun ry(deg: Double): Mat3 {
            val c = cos(rad(deg)); val s = sin(rad(deg))
            return Mat3(doubleArrayOf(c, 0.0, s, 0.0, 1.0, 0.0, -s, 0.0, c))
        }

        /** Rotation about z. Positive tips +y toward -x. */
        fun rz(deg: Double): Mat3 {
            val c = cos(rad(deg)); val s = sin(rad(deg))
            return Mat3(doubleArrayOf(c, -s, 0.0, s, c, 0.0, 0.0, 0.0, 1.0))
        }

        /**
         * Body orientation from authoring angles.
         * pitch +: lean forward (90 = lying face down), -90 = lying on the back.
         * roll +: tip toward the figure's LEFT side. yaw +: turn to the figure's left.
         */
        fun body(pitch: Double, roll: Double, yaw: Double): Mat3 = ry(yaw) * rz(-roll) * rx(pitch)
    }
}
