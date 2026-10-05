package com.nunna.fitnessgym.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.nunna.fitnessgym.AppGraph
import com.nunna.fitnessgym.anim.Camera
import com.nunna.fitnessgym.anim.FigureScene
import com.nunna.fitnessgym.anim.Prim

/**
 * The animated anatomical figure. [speed] 1.0 = the template's tempo; [playing] false freezes it.
 * Tapping a muscle reports its group id (e.g. "quadriceps") through [onTapMuscle].
 */
@Composable
fun FigureView(
    scene: FigureScene,
    camera: Camera,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
    speed: Double = 1.0,
    staticPhase: Double = 0.0,
    onTapMuscle: ((String) -> Unit)? = null,
) {
    var phase by remember(scene) { mutableDoubleStateOf(staticPhase) }
    val last = remember { arrayOf<List<Prim>>(emptyList()) }
    val duration = scene.motion.template.duration.coerceAtLeast(0.3)

    LaunchedEffect(scene, playing, speed) {
        if (!playing || !AppGraph.animations) return@LaunchedEffect
        var prev = 0L
        while (true) {
            withFrameNanos { now ->
                if (prev != 0L) phase = (phase + (now - prev) / 1e9 / duration * speed) % 1.0
                prev = now
            }
        }
    }

    val tap = if (onTapMuscle != null) Modifier.pointerInput(scene) {
        detectTapGestures { pos -> hitTest(last[0], pos)?.let(onTapMuscle) }
    } else Modifier

    Canvas(modifier.then(tap)) {
        val prims = scene.frame(phase, size.width, size.height, camera)
        last[0] = prims
        for (p in prims) {
            if (p.xy.size < 4) continue
            val path = Path().apply {
                moveTo(p.xy[0], p.xy[1])
                var i = 2
                while (i < p.xy.size) { lineTo(p.xy[i], p.xy[i + 1]); i += 2 }
                if (p.closed) close()
            }
            if (p.fill != 0 && p.closed) drawPath(path, Color(p.fill), style = Fill)
            if (p.stroke != 0 && p.strokeW > 0f) {
                drawPath(path, Color(p.stroke), style = Stroke(p.strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

/** Topmost tagged muscle polygon under the finger. */
private fun hitTest(prims: List<Prim>, pos: Offset): String? {
    for (p in prims.asReversed()) {
        val tag = p.tag ?: continue
        if (tag.startsWith("_") || !p.closed || p.fill == 0) continue
        if (inside(p.xy, pos.x, pos.y)) return tag
    }
    return null
}

private fun inside(xy: FloatArray, x: Float, y: Float): Boolean {
    var c = false
    val n = xy.size / 2
    var j = n - 1
    for (i in 0 until n) {
        val xi = xy[i * 2]; val yi = xy[i * 2 + 1]; val xj = xy[j * 2]; val yj = xy[j * 2 + 1]
        if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) c = !c
        j = i
    }
    return c
}
