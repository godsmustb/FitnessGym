package com.nunna.fitnessgym.anim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RigTest {
    private val eps = 1e-6

    @Test fun ikReachesReachableTargetWithExactBoneLengths() {
        val root = Vec3(0.0, 1.0, 0.0)
        val target = Vec3(0.1, 0.6, 0.25)
        val l = Rig.twoBone(root, target, 0.3, 0.26, Vec3(0.0, 0.0, -1.0), -1.0)
        assertEquals(0.0, (l.end - target).length(), eps)
        assertEquals(0.3, (l.mid - root).length(), eps)
        assertEquals(0.26, (l.end - l.mid).length(), eps)
    }

    @Test fun ikClampsUnreachableTargetToStraightLimb() {
        val l = Rig.twoBone(Vec3.ZERO, Vec3(0.0, -5.0, 0.0), 0.43, 0.40, Vec3.FWD, 1.0)
        assertEquals(0.83, l.end.length(), 1e-3)
    }

    @Test fun kneeBendsTowardPole() {
        val l = Rig.twoBone(Vec3(0.0, 0.9, 0.0), Vec3(0.0, 0.3, 0.0), 0.43, 0.40, Vec3.FWD, 1.0)
        assertTrue("knee should move forward (+z)", l.mid.z > 0.1)
        assertTrue("thigh front faces forward", l.upperFront.z > 0.5)
    }

    @Test fun standingPoseHasFeetOnFloorAndHeadAboutOneEighty() {
        val m = Motion(MotionTemplate("t", "t", keys = listOf(Key(t = 0.0))))
        val sk = Rig.solve(m.sample(0.0))
        assertEquals(0.08, sk.legL.end.y, 0.02)
        val top = sk.headCenter.y + Dim.HEAD_R
        assertTrue("head top $top", top in 1.72..1.86)
        assertTrue("left side is +x", sk.armL.root.x > 0 && sk.legR.root.x < 0)
    }

    @Test fun bodyOrientationConventions() {
        // pitch +90 = prone: chest (z) faces down, head (y) toward +z
        val prone = Mat3.body(90.0, 0.0, 0.0)
        assertEquals(-1.0, prone.zAxis.y, eps); assertEquals(1.0, prone.yAxis.z, eps)
        // pitch -90 = supine: chest faces up
        assertEquals(1.0, Mat3.body(-90.0, 0.0, 0.0).zAxis.y, eps)
        // roll +90 = lying on the left side: left (x) faces down
        assertEquals(-1.0, Mat3.body(0.0, 90.0, 0.0).xAxis.y, eps)
        // yaw +90 = facing the figure's left (+x)
        assertEquals(1.0, Mat3.body(0.0, 0.0, 90.0).zAxis.x, eps)
    }

    @Test fun rightSideMirrorsWhenNotGiven() {
        val m = Motion(MotionTemplate("t", "t", keys = listOf(Key(t = 0.0, lh = listOf(0.3, -0.2, 0.4)))))
        val p = m.sample(0.0)
        assertEquals(-0.3, p.rh.x, eps); assertEquals(0.4, p.rh.z, eps)
    }

    @Test fun samplingLoopsSmoothly() {
        val m = Motion(MotionTemplate("t", "t", keys = listOf(
            Key(t = 0.0, e = 0.0, pelvis = listOf(0.0, 0.97, 0.0)),
            Key(t = 0.5, e = 1.0, pelvis = listOf(0.0, 0.6, 0.0)),
        )))
        assertEquals(0.97, m.sample(0.0).pelvis.y, eps)
        assertEquals(0.6, m.sample(0.5).pelvis.y, eps)
        assertEquals(0.97, m.sample(0.9999).pelvis.y, 1e-3)
        assertEquals(1.0, m.sample(0.5).effortL, eps)
    }
}
